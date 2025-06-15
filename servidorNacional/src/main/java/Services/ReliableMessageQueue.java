package Services;

import Models.VotoModel;
import Config.ReliableQueueConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Sistema de cola confiable para votos
 * Persiste los votos en disco cuando la BD no está disponible
 * Los procesa automáticamente cuando la BD se recupera
 */
public class ReliableMessageQueue {
    
    // Configuración
    private final ReliableQueueConfig config;
    
    // Directorios dinámicos basados en configuración
    private final String queueDir;
    private final String pendingDir;
    private final String processingDir;
    private final String failedDir;
    private final String processedDir;
    
    private final BlockingQueue<VotoModel> memoryQueue;
    private final AtomicLong messageCounter;
    private final AtomicBoolean isProcessing;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService processingExecutor;
    private final Gson gson;
    
    // Callback para procesar votos
    private VotoProcessor votoProcessor;
    
    // Estadísticas
    private final AtomicLong totalReceived;
    private final AtomicLong totalProcessed;
    private final AtomicLong totalFailed;
    private final AtomicLong totalInQueue;
    
    public interface VotoProcessor {
        boolean processVoto(VotoModel voto);
        boolean isDatabaseAvailable();
    }
    
    public ReliableMessageQueue() {
        this(new ReliableQueueConfig());
    }
    
    public ReliableMessageQueue(ReliableQueueConfig config) {
        this.config = config;
        
        // Configurar directorios basados en configuración
        this.queueDir = config.getBaseDir();
        this.pendingDir = queueDir + "/pending";
        this.processingDir = queueDir + "/processing";
        this.failedDir = queueDir + "/failed";
        this.processedDir = queueDir + "/processed";
        
        this.memoryQueue = new LinkedBlockingQueue<>();
        this.messageCounter = new AtomicLong(0);
        this.isProcessing = new AtomicBoolean(false);
        
        // Configurar thread pools basados en configuración
        this.scheduler = Executors.newScheduledThreadPool(config.getSchedulerThreads());
        this.processingExecutor = Executors.newFixedThreadPool(config.getProcessingThreads());
        
        // Configurar Gson para manejar LocalDateTime
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
                context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> 
                LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .setPrettyPrinting()
            .create();
        
        // Estadísticas
        this.totalReceived = new AtomicLong(0);
        this.totalProcessed = new AtomicLong(0);
        this.totalFailed = new AtomicLong(0);
        this.totalInQueue = new AtomicLong(0);
        
        // Inicializar directorios
        initializeDirectories();
        
        // Cargar votos pendientes del disco si la persistencia está habilitada
        if (config.isPersistenceEnabled()) {
            loadPendingVotesFromDisk();
        }
        
        // Iniciar procesamiento automático
        startProcessing();
        
        System.out.println("🔄 ReliableMessageQueue inicializada");
        System.out.println("   📁 Directorio: " + queueDir);
        System.out.println("   📊 Votos en cola: " + totalInQueue.get());
        System.out.println("   ⚙️ Configuración: " + config.toString());
    }
    
    private void initializeDirectories() {
        if (!config.isPersistenceEnabled()) {
            System.out.println("💾 Persistencia deshabilitada, omitiendo creación de directorios");
            return;
        }
        
        try {
            Files.createDirectories(Paths.get(pendingDir));
            Files.createDirectories(Paths.get(processingDir));
            Files.createDirectories(Paths.get(failedDir));
            Files.createDirectories(Paths.get(processedDir));
            System.out.println("✅ Directorios de cola confiable creados");
        } catch (IOException e) {
            System.err.println("❌ Error creando directorios: " + e.getMessage());
        }
    }
    
    public void setVotoProcessor(VotoProcessor processor) {
        this.votoProcessor = processor;
        System.out.println("✅ VotoProcessor configurado");
    }
    
    /**
     * Añade un voto a la cola confiable
     */
    public boolean enqueue(VotoModel voto) {
        if (voto == null || !voto.isValid()) {
            System.err.println("❌ Voto inválido rechazado");
            return false;
        }
        
        try {
            // Asignar ID único si no tiene
            if (voto.getId() == null) {
                voto.setId(messageCounter.incrementAndGet());
            }
            
            // Añadir a cola en memoria
            memoryQueue.offer(voto);
            totalReceived.incrementAndGet();
            totalInQueue.incrementAndGet();
            
            // Persistir en disco si está habilitado
            if (config.isPersistenceEnabled()) {
                persistVoteToDisk(voto, pendingDir);
            }
            
            if (config.getLogLevel().equals("DEBUG")) {
                System.out.println("📥 Voto encolado: " + voto.getMesaId() + " -> " + voto.getCandidatoId());
            }
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error encolando voto: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Añade múltiples votos a la cola
     */
    public int enqueueBatch(List<VotoModel> votos) {
        if (votos == null || votos.isEmpty()) {
            return 0;
        }
        
        int enqueued = 0;
        for (VotoModel voto : votos) {
            if (enqueue(voto)) {
                enqueued++;
            }
        }
        
        System.out.println("📦 Lote procesado: " + enqueued + "/" + votos.size() + " votos encolados");
        return enqueued;
    }
    
    private void persistVoteToDisk(VotoModel voto, String directory) {
        try {
            String filename = String.format("voto_%d_%s_%s.json", 
                voto.getId(), voto.getMesaId(), System.currentTimeMillis());
            Path filePath = Paths.get(directory, filename);
            
            String json = gson.toJson(voto);
            Files.write(filePath, json.getBytes(), StandardOpenOption.CREATE);
            
        } catch (IOException e) {
            System.err.println("❌ Error persistiendo voto: " + e.getMessage());
        }
    }
    
    private void loadPendingVotesFromDisk() {
        try {
            Path pendingPath = Paths.get(pendingDir);
            if (!Files.exists(pendingPath)) {
                return;
            }
            
            Files.list(pendingPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(this::loadVoteFromFile);
                
        } catch (IOException e) {
            System.err.println("❌ Error cargando votos pendientes: " + e.getMessage());
        }
    }
    
    private void loadVoteFromFile(Path filePath) {
        try {
            String json = new String(Files.readAllBytes(filePath));
            VotoModel voto = gson.fromJson(json, VotoModel.class);
            
            if (voto != null && voto.isValid()) {
                memoryQueue.offer(voto);
                totalInQueue.incrementAndGet();
                System.out.println("📂 Voto cargado desde disco: " + voto.getMesaId());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error cargando voto desde " + filePath + ": " + e.getMessage());
            // Mover archivo problemático a failed
            if (config.isPersistenceEnabled()) {
                moveFile(filePath, failedDir);
            }
        }
    }
    
    private void startProcessing() {
        // Procesador principal con intervalo configurable
        long intervalMs = config.getProcessingInterval();
        scheduler.scheduleWithFixedDelay(this::processQueue, 1000, intervalMs, TimeUnit.MILLISECONDS);
        
        // Monitor de estadísticas si está habilitado
        if (config.isLogStatistics()) {
            long statsIntervalMs = config.getStatisticsInterval();
            scheduler.scheduleWithFixedDelay(this::logStatistics, statsIntervalMs, statsIntervalMs, TimeUnit.MILLISECONDS);
        }
        
        // Limpieza automática si está habilitada
        if (config.isAutoCleanup()) {
            long cleanupIntervalMs = config.getCleanupInterval();
            scheduler.scheduleWithFixedDelay(this::performCleanup, cleanupIntervalMs, cleanupIntervalMs, TimeUnit.MILLISECONDS);
        }
        
        System.out.println("🚀 Procesamiento automático iniciado");
        System.out.println("   ⏱️ Intervalo de procesamiento: " + intervalMs + "ms");
        if (config.isLogStatistics()) {
            System.out.println("   📊 Intervalo de estadísticas: " + config.getStatisticsInterval() + "ms");
        }
    }
    
    private void processQueue() {
        if (isProcessing.get() || votoProcessor == null) {
            return;
        }
        
        if (!votoProcessor.isDatabaseAvailable()) {
            return; // BD no disponible, mantener votos en cola
        }
        
        isProcessing.set(true);
        
        try {
            List<VotoModel> batch = new ArrayList<>();
            int batchSize = config.getBatchSize();
            
            // Extraer lote de votos
            for (int i = 0; i < batchSize && !memoryQueue.isEmpty(); i++) {
                VotoModel voto = memoryQueue.poll();
                if (voto != null) {
                    batch.add(voto);
                }
            }
            
            if (!batch.isEmpty()) {
                processBatch(batch);
            }
            
        } finally {
            isProcessing.set(false);
        }
    }
    
    private void processBatch(List<VotoModel> batch) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (VotoModel voto : batch) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                boolean success = false;
                int retries = 0;
                int maxRetries = config.getMaxRetries();
                
                while (!success && retries < maxRetries) {
                    try {
                        if (config.isPersistenceEnabled()) {
                            moveVoteFile(voto, pendingDir, processingDir);
                        }
                        
                        success = votoProcessor.processVoto(voto);
                        
                        if (success) {
                            totalProcessed.incrementAndGet();
                            totalInQueue.decrementAndGet();
                            
                            if (config.isPersistenceEnabled()) {
                                moveVoteFile(voto, processingDir, processedDir);
                            }
                            
                            if (config.getLogLevel().equals("DEBUG")) {
                                System.out.println("✅ Voto procesado: " + voto.getMesaId());
                            }
                        } else {
                            retries++;
                            if (retries < maxRetries) {
                                Thread.sleep(config.getRetryDelay());
                            }
                        }
                        
                    } catch (Exception e) {
                        System.err.println("❌ Error procesando voto: " + e.getMessage());
                        retries++;
                        if (retries < maxRetries) {
                            try {
                                Thread.sleep(config.getRetryDelay());
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
                
                if (!success) {
                    totalFailed.incrementAndGet();
                    totalInQueue.decrementAndGet();
                    
                    if (config.isPersistenceEnabled()) {
                        moveVoteFile(voto, processingDir, failedDir);
                    }
                    
                    System.err.println("❌ Voto falló después de " + maxRetries + " intentos: " + voto.getMesaId());
                }
                
            }, processingExecutor);
            
            futures.add(future);
        }
        
        // Esperar que termine el lote
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
    
    private void moveVoteFile(VotoModel voto, String fromDir, String toDir) {
        try {
            String pattern = "voto_" + voto.getId() + "_" + voto.getMesaId() + "_*.json";
            Path fromPath = Paths.get(fromDir);
            
            Files.list(fromPath)
                .filter(path -> path.getFileName().toString().matches(pattern.replace("*", ".*")))
                .findFirst()
                .ifPresent(source -> moveFile(source, toDir));
                
        } catch (IOException e) {
            System.err.println("❌ Error moviendo archivo de voto: " + e.getMessage());
        }
    }
    
    private void moveFile(Path source, String targetDir) {
        try {
            Path target = Paths.get(targetDir, source.getFileName().toString());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("❌ Error moviendo archivo: " + e.getMessage());
        }
    }
    
    private void performCleanup() {
        if (!config.isAutoCleanup() || !config.isPersistenceEnabled()) {
            return;
        }
        
        try {
            Path processedPath = Paths.get(processedDir);
            if (!Files.exists(processedPath)) {
                return;
            }
            
            List<Path> processedFiles = Files.list(processedPath)
                .filter(path -> path.toString().endsWith(".json"))
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
            
            int maxFiles = config.getMaxProcessedFiles();
            if (processedFiles.size() > maxFiles) {
                int toDelete = processedFiles.size() - maxFiles;
                for (int i = 0; i < toDelete; i++) {
                    Files.deleteIfExists(processedFiles.get(i));
                }
                System.out.println("🧹 Limpieza automática: " + toDelete + " archivos eliminados");
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error en limpieza automática: " + e.getMessage());
        }
    }
    
    private void logStatistics() {
        if (!config.isLogStatistics()) {
            return;
        }
        
        QueueStats stats = getStats();
        System.out.println("📊 Estadísticas ReliableMessageQueue:");
        System.out.println("   " + stats.toString());
    }
    
    public QueueStats getStats() {
        return new QueueStats(
            totalReceived.get(),
            totalProcessed.get(),
            totalFailed.get(),
            totalInQueue.get(),
            memoryQueue.size()
        );
    }
    
    public void shutdown() {
        System.out.println("🛑 Cerrando ReliableMessageQueue...");
        
        scheduler.shutdown();
        processingExecutor.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!processingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                processingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            processingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ ReliableMessageQueue cerrada");
    }
    
    public static class QueueStats {
        public final long totalReceived;
        public final long totalProcessed;
        public final long totalFailed;
        public final long totalInQueue;
        public final int inMemory;
        
        public QueueStats(long received, long processed, long failed, long inQueue, int inMemory) {
            this.totalReceived = received;
            this.totalProcessed = processed;
            this.totalFailed = failed;
            this.totalInQueue = inQueue;
            this.inMemory = inMemory;
        }
        
        @Override
        public String toString() {
            return String.format("Recibidos: %d, Procesados: %d, Fallidos: %d, En cola: %d, En memoria: %d",
                totalReceived, totalProcessed, totalFailed, totalInQueue, inMemory);
        }
    }
} 