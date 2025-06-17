package servidorRegional;

import Demo.*;
import com.zeroc.Ice.*;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

/**
 * Componente del Servidor Regional que distribuye votantes por mesas
 * VERSIÓN DISTRIBUIDA: Soporta distribución local Y remota
 * - Local: Crea archivos SQLite localmente
 * - Remota: Envía archivos SQLite a mesas en equipos remotos
 */
public class DistribuidorMesas {
    
    private final Communicator communicator;
    private final DatabaseManager databaseManager;
    private final Map<String, String> registroMesas; // mesaId -> endpoint
    
    public DistribuidorMesas(Communicator communicator, DatabaseManager databaseManager) {
        this.communicator = communicator;
        this.databaseManager = databaseManager;
        this.registroMesas = new HashMap<>();
        
        System.out.println("🗳️ Distribuidor de Mesas inicializado (Local + Distribuido)");
    }
    
    /**
     * MÉTODO EXISTENTE: Distribución local (crear archivos SQLite localmente)
     * AHORA CON CONCURRENCIA MEJORADA
     * @param departamento Nombre del departamento
     * @return Resultado de la distribución local
     */
    public boolean distribuirVotantesPorDepartamento(String departamento) {
        System.out.println("🗳️ Iniciando distribución LOCAL CONCURRENTE de votantes para: " + departamento);
        
        List<CiudadanoInfo> votantesDepartamento = databaseManager.consultarVotantesLocales(departamento);
        
        if (votantesDepartamento.isEmpty()) {
            System.out.println("⚠️ No hay votantes locales para " + departamento + ". Ejecute 'guardar " + departamento + "' primero.");
            return false;
        }
        
        System.out.println("📊 Total votantes a distribuir: " + votantesDepartamento.size());
        
        Map<String, List<CiudadanoInfo>> votantesPorMesa = agruparVotantesPorMesa(votantesDepartamento);
        System.out.println("🗳️ Mesas identificadas: " + votantesPorMesa.size());
        
        // Crear ExecutorService para procesamiento concurrente
        int numThreads = Math.min(votantesPorMesa.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("⚡ Usando " + numThreads + " hilos para procesamiento concurrente");
        
        try {
            // Crear lista de tareas concurrentes
            List<CompletableFuture<ResultadoMesa>> tareas = new ArrayList<>();
            
            for (Map.Entry<String, List<CiudadanoInfo>> entry : votantesPorMesa.entrySet()) {
                String mesaId = entry.getKey();
                List<CiudadanoInfo> votantesMesa = entry.getValue();
                
                System.out.println("🗳️ Mesa: " + mesaId + " - Votantes: " + votantesMesa.size());
                
                // Crear tarea asíncrona para cada mesa
                CompletableFuture<ResultadoMesa> tarea = CompletableFuture.supplyAsync(() -> {
                    long startTime = System.currentTimeMillis();
                    System.out.println("📤 [" + Thread.currentThread().getName() + "] Procesando mesa " + mesaId + " con " + votantesMesa.size() + " votantes");
                    
                    boolean exito = crearArchivoMesa(mesaId, votantesMesa, departamento);
                    long endTime = System.currentTimeMillis();
                    
                    ResultadoMesa resultado = new ResultadoMesa(mesaId, exito, votantesMesa.size(), endTime - startTime);
                    
                    if (exito) {
                        System.out.println("✅ [" + Thread.currentThread().getName() + "] Mesa " + mesaId + " completada en " + (endTime - startTime) + "ms");
                    } else {
                        System.out.println("❌ [" + Thread.currentThread().getName() + "] Error en mesa " + mesaId);
                    }
                    
                    return resultado;
                }, executor);
                
                tareas.add(tarea);
            }
            
            // Esperar a que todas las tareas terminen
            System.out.println("⏳ Esperando finalización de " + tareas.size() + " tareas concurrentes...");
            long startTimeTotal = System.currentTimeMillis();
            
            CompletableFuture<Void> todasLasTareas = CompletableFuture.allOf(
                tareas.toArray(new CompletableFuture[0])
            );
            
            // Esperar con timeout de 5 minutos
            todasLasTareas.get(5, TimeUnit.MINUTES);
            
            long endTimeTotal = System.currentTimeMillis();
            
            // Recopilar resultados
            int mesasExitosas = 0;
            int totalVotantesDistribuidos = 0;
            long tiempoMaximo = 0;
            
            for (CompletableFuture<ResultadoMesa> tarea : tareas) {
                try {
                    ResultadoMesa resultado = tarea.get();
                    if (resultado.exito) {
                        mesasExitosas++;
                        totalVotantesDistribuidos += resultado.votantesGuardados;
                    }
                    tiempoMaximo = Math.max(tiempoMaximo, resultado.tiempoProcesamiento);
                } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
                    System.err.println("❌ Error obteniendo resultado de tarea: " + e.getMessage());
                }
            }
            
            System.out.println("📈 === RESUMEN DISTRIBUCIÓN LOCAL CONCURRENTE ===");
            System.out.println("   Departamento: " + departamento);
            System.out.println("   Mesas creadas: " + mesasExitosas + "/" + votantesPorMesa.size());
            System.out.println("   Votantes distribuidos: " + totalVotantesDistribuidos + "/" + votantesDepartamento.size());
            System.out.println("   Tiempo total: " + (endTimeTotal - startTimeTotal) + "ms");
            System.out.println("   Tiempo mesa más lenta: " + tiempoMaximo + "ms");
            System.out.println("   Hilos utilizados: " + numThreads);
            System.out.println("   Archivos creados en: data/mesa_*.db");
            System.out.println("   Ganancia de velocidad: ~" + String.format("%.1fx", (double) tiempoMaximo * votantesPorMesa.size() / (endTimeTotal - startTimeTotal)));
            System.out.println("===============================================");
            
            return mesasExitosas > 0;
            
        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("❌ Timeout: Algunas tareas tardaron más de 5 minutos");
            return false;
        } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
            System.err.println("❌ Error en procesamiento concurrente: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Cerrar ExecutorService
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * NUEVO: Distribución remota a mesas registradas
     * @param departamento Nombre del departamento
     * @return Resultado de la distribución remota
     */
    public boolean distribuirVotantesRemotamente(String departamento) {
        System.out.println("🌐 Iniciando distribución REMOTA de votantes para: " + departamento);
        
        // Primero crear archivos localmente
        if (!distribuirVotantesPorDepartamento(departamento)) {
            System.out.println("❌ Error en distribución local. No se puede proceder con distribución remota.");
            return false;
        }
        
        // Obtener mesas del departamento
        Set<String> mesas = obtenerMesasDelDepartamento(departamento);
        
        if (registroMesas.isEmpty()) {
            System.out.println("⚠️ No hay mesas registradas para distribución remota.");
            System.out.println("💡 Use 'registrar <mesaId> <endpoint>' para registrar mesas remotas");
            return false;
        }
        
        int mesasEnviadas = 0;
        
        for (String mesaId : mesas) {
            String endpoint = registroMesas.get(mesaId);
            
            if (endpoint != null) {
                System.out.println("📡 Enviando archivo a Mesa " + mesaId + " en " + endpoint);
                
                if (enviarArchivoAMesaEspecifica(mesaId, endpoint)) {
                    mesasEnviadas++;
                    System.out.println("✅ Mesa " + mesaId + " actualizada remotamente");
                } else {
                    System.out.println("❌ Error enviando a Mesa " + mesaId);
                }
            } else {
                System.out.println("⚠️ Mesa " + mesaId + " no tiene endpoint registrado");
            }
        }
        
        System.out.println("📈 === RESUMEN DISTRIBUCIÓN REMOTA ===");
        System.out.println("   Departamento: " + departamento);
        System.out.println("   Mesas registradas: " + registroMesas.size());
        System.out.println("   Mesas enviadas: " + mesasEnviadas + "/" + mesas.size());
        System.out.println("   Estado: " + (mesasEnviadas > 0 ? "EXITOSO" : "FALLIDO"));
        System.out.println("=====================================");
        
        return mesasEnviadas > 0;
    }
    
    /**
     * NUEVO: Enviar archivo SQLite a una mesa específica
     * @param mesaId ID de la mesa
     * @param endpoint Endpoint de la mesa remota
     * @return true si el envío fue exitoso
     */
    private boolean enviarArchivoAMesaEspecifica(String mesaId, String endpoint) {
        try {
            // 1. Leer archivo SQLite local
            String archivoPath = "data/mesa_" + mesaId.replaceAll("[^a-zA-Z0-9]", "_") + ".db";
            java.io.File archivo = new java.io.File(archivoPath);
            
            if (!archivo.exists()) {
                System.err.println("❌ Archivo no existe: " + archivoPath);
                return false;
            }
            
            byte[] datosArchivo = Files.readAllBytes(Paths.get(archivoPath));
            System.out.println("📁 Archivo leído: " + archivoPath + " (" + datosArchivo.length + " bytes)");
            
            // 2. Conectar a la mesa remota
            ObjectPrx base = communicator.stringToProxy(endpoint);
            IMesaVotacionPrx mesaProxy = IMesaVotacionPrx.checkedCast(base);
            
            if (mesaProxy == null) {
                System.err.println("❌ No se pudo conectar a la mesa: " + endpoint);
                return false;
            }
            
            // 3. Verificar si la mesa está lista
            if (!mesaProxy.estaListaParaRecibir()) {
                System.err.println("❌ Mesa " + mesaId + " no está lista para recibir datos");
                return false;
            }
            
            // 4. Enviar archivo
            String nombreArchivo = "mesa_" + mesaId + ".db";
            boolean exito = mesaProxy.recibirArchivoSQLite(datosArchivo, nombreArchivo);
            
            if (exito) {
                System.out.println("✅ Archivo enviado exitosamente a Mesa " + mesaId);
            } else {
                System.err.println("❌ Mesa " + mesaId + " rechazó el archivo");
            }
            
            return exito;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error enviando archivo a Mesa " + mesaId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * NUEVO: Registrar una mesa remota
     * @param mesaId ID de la mesa
     * @param endpoint Endpoint ICE de la mesa
     */
    public void registrarMesaRemota(String mesaId, String endpoint) {
        registroMesas.put(mesaId, endpoint);
        System.out.println("📝 Mesa remota registrada: " + mesaId + " -> " + endpoint);
    }
    
    /**
     * NUEVO: Desregistrar una mesa remota
     * @param mesaId ID de la mesa
     */
    public boolean desregistrarMesaRemota(String mesaId) {
        String endpoint = registroMesas.remove(mesaId);
        if (endpoint != null) {
            System.out.println("🗑️ Mesa remota desregistrada: " + mesaId);
            return true;
        } else {
            System.out.println("⚠️ Mesa " + mesaId + " no estaba registrada");
            return false;
        }
    }
    
    /**
     * NUEVO: Listar mesas registradas
     * @return Mapa de mesaId -> endpoint
     */
    public Map<String, String> obtenerMesasRegistradas() {
        return new HashMap<>(registroMesas);
    }
    
    /**
     * NUEVO: Verificar conectividad con mesas registradas
     * @return Número de mesas conectadas
     */
    public int verificarConectividadMesas() {
        System.out.println("🔗 Verificando conectividad con mesas registradas...");
        
        int mesasConectadas = 0;
        
        for (Map.Entry<String, String> entry : registroMesas.entrySet()) {
            String mesaId = entry.getKey();
            String endpoint = entry.getValue();
            
            try {
                ObjectPrx base = communicator.stringToProxy(endpoint);
                IMesaVotacionPrx mesaProxy = IMesaVotacionPrx.checkedCast(base);
                
                if (mesaProxy != null && mesaProxy.verificarEstadoMesa()) {
                    System.out.println("✅ Mesa " + mesaId + " conectada");
                    mesasConectadas++;
                } else {
                    System.out.println("❌ Mesa " + mesaId + " no responde");
                }
                
            } catch (java.lang.Exception e) {
                System.out.println("❌ Mesa " + mesaId + " error: " + e.getMessage());
            }
        }
        
        System.out.println("📊 Mesas conectadas: " + mesasConectadas + "/" + registroMesas.size());
        return mesasConectadas;
    }
    
    /**
     * Crea un archivo SQLite específico para una mesa
     * OPTIMIZADO PARA CONCURRENCIA
     */
    private boolean crearArchivoMesa(String mesaId, List<CiudadanoInfo> votantes, String departamento) {
        try {
            // Crear DatabaseManager específico para esta mesa
            DatabaseManagerMesa dbMesa = new DatabaseManagerMesa(mesaId);
            int guardados = dbMesa.guardarVotantesMesa(votantes, departamento);
            
            System.out.println("💾 [" + Thread.currentThread().getName() + "] Archivo SQLite creado: " + dbMesa.getDbPath() + " (" + guardados + " votantes)");
            return guardados > 0;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ [" + Thread.currentThread().getName() + "] Error creando archivo para mesa " + mesaId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Agrupa votantes por mesa
     * @param votantes Lista de votantes
     * @return Mapa de mesa -> lista de votantes
     */
    private Map<String, List<CiudadanoInfo>> agruparVotantesPorMesa(List<CiudadanoInfo> votantes) {
        Map<String, List<CiudadanoInfo>> grupos = new HashMap<>();
        
        for (CiudadanoInfo votante : votantes) {
            String mesaId = votante.mesaId;
            
            if (mesaId == null || mesaId.trim().isEmpty()) {
                System.err.println("⚠️ Votante sin mesa asignada: " + votante.documento + " - " + votante.nombre);
                continue;
            }
            
            grupos.computeIfAbsent(mesaId, k -> new ArrayList<>()).add(votante);
        }
        
        // Mostrar estadísticas de agrupación
        System.out.println("📊 Agrupación por mesas:");
        for (Map.Entry<String, List<CiudadanoInfo>> entry : grupos.entrySet()) {
            System.out.println("   Mesa " + entry.getKey() + ": " + entry.getValue().size() + " votantes");
        }
        
        return grupos;
    }
    
    /**
     * Lista todas las mesas únicas de un departamento
     * @param departamento Nombre del departamento
     * @return Set de IDs de mesa
     */
    public Set<String> obtenerMesasDelDepartamento(String departamento) {
        List<CiudadanoInfo> votantes = databaseManager.consultarVotantesLocales(departamento);
        
        return votantes.stream()
                .map(v -> v.mesa)
                .filter(Objects::nonNull)
                .filter(mesa -> !mesa.trim().isEmpty())
                .collect(Collectors.toSet());
    }
    
    /**
     * Obtiene estadísticas básicas de distribución
     * @return Lista de estadísticas por departamento
     */
    public List<String> obtenerEstadisticasDistribucion() {
        List<String> estadisticas = new ArrayList<>();
        
        estadisticas.add("📊 === ESTADÍSTICAS DE DISTRIBUCIÓN ===");
        estadisticas.add("   Función: Creación local + Distribución remota");
        estadisticas.add("   Ubicación archivos: data/mesa_*.db");
        estadisticas.add("   Mesas remotas registradas: " + registroMesas.size());
        
        if (!registroMesas.isEmpty()) {
            estadisticas.add("   Mesas registradas:");
            for (Map.Entry<String, String> entry : registroMesas.entrySet()) {
                estadisticas.add("   • Mesa " + entry.getKey() + " -> " + entry.getValue());
            }
        }
        
        // Verificar archivos existentes en directorio data
        java.io.File dataDir = new java.io.File("data");
        if (dataDir.exists() && dataDir.isDirectory()) {
            java.io.File[] archivos = dataDir.listFiles((dir, name) -> name.startsWith("mesa_") && name.endsWith(".db"));
            if (archivos != null) {
                estadisticas.add("   Archivos locales creados: " + archivos.length);
                for (java.io.File archivo : archivos) {
                    long sizeKB = archivo.length() / 1024;
                    estadisticas.add("   • " + archivo.getName() + " (" + sizeKB + " KB)");
                }
            }
        } else {
            estadisticas.add("   No hay archivos de mesa creados aún");
        }
        
        estadisticas.add("=====================================");
        return estadisticas;
    }
    
    /**
     * Limpia archivos de distribución de un departamento
     * @param departamento Nombre del departamento
     * @return número de archivos eliminados
     */
    public int limpiarDistribucionDepartamento(String departamento) {
        Set<String> mesas = obtenerMesasDelDepartamento(departamento);
        int archivosEliminados = 0;
        
        System.out.println("🧹 Limpiando archivos de distribución de " + departamento + " (" + mesas.size() + " mesas)...");
        
        for (String mesaId : mesas) {
            try {
                String archivoPath = "data/mesa_" + mesaId.replaceAll("[^a-zA-Z0-9]", "_") + ".db";
                java.io.File archivo = new java.io.File(archivoPath);
                
                if (archivo.exists() && archivo.delete()) {
                    archivosEliminados++;
                    System.out.println("🗑️ Eliminado: " + archivoPath);
                }
                
            } catch (java.lang.Exception e) {
                System.err.println("❌ Error eliminando archivo de mesa " + mesaId + ": " + e.getMessage());
            }
        }
        
        System.out.println("✅ Eliminados " + archivosEliminados + "/" + mesas.size() + " archivos de " + departamento);
        return archivosEliminados;
    }
    
    /**
     * Clase interna para almacenar resultados de procesamiento de mesa
     */
    private static class ResultadoMesa {
        final String mesaId;
        final boolean exito;
        final int votantesGuardados;
        final long tiempoProcesamiento;
        
        ResultadoMesa(String mesaId, boolean exito, int votantesGuardados, long tiempoProcesamiento) {
            this.mesaId = mesaId;
            this.exito = exito;
            this.votantesGuardados = votantesGuardados;
            this.tiempoProcesamiento = tiempoProcesamiento;
        }
    }
} 