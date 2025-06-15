package Broker;

import Demo.*;
import com.zeroc.Ice.Current;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorRecursos implements IMonitorRecursos {
    
    private final String nodeId;
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final AtomicLong requestCount;
    private final ScheduledExecutorService scheduler;
    
    // Métricas actuales
    private volatile double cpuUsage = 0.0;
    private volatile double memoryUsage = 0.0;
    private volatile double networkUsage = 0.0;
    private volatile boolean disponible = true;
    
    // Configuración de umbrales
    private static final double CPU_THRESHOLD = 50.0;
    private static final double MEMORY_THRESHOLD = 50.0;
    private static final double NETWORK_THRESHOLD = 50.0;
    
    public MonitorRecursos(String nodeId) {
        this.nodeId = nodeId;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.requestCount = new AtomicLong(0);
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        iniciarMonitoreo();
        System.out.println("🔍 Monitor de Recursos iniciado para nodo: " + nodeId);
    }
    
    private void iniciarMonitoreo() {
        // Monitoreo cada 5 segundos
        scheduler.scheduleAtFixedRate(this::actualizarMetricas, 0, 5, TimeUnit.SECONDS);
        
        // Log de métricas cada 30 segundos
        scheduler.scheduleAtFixedRate(this::logMetricas, 10, 30, TimeUnit.SECONDS);
    }
    
    private void actualizarMetricas() {
        try {
            // CPU Usage
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                cpuUsage = sunOsBean.getProcessCpuLoad() * 100.0;
                if (cpuUsage < 0) cpuUsage = 0.0; // Valor no disponible
            } else {
                // Fallback: usar load average como aproximación
                double loadAverage = osBean.getSystemLoadAverage();
                if (loadAverage >= 0) {
                    cpuUsage = Math.min(loadAverage * 25.0, 100.0); // Aproximación
                }
            }
            
            // Memory Usage
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            if (maxMemory > 0) {
                memoryUsage = (double) usedMemory / maxMemory * 100.0;
            }
            
            // Network Usage (simulado basado en requests)
            long currentRequests = requestCount.get();
            networkUsage = Math.min(currentRequests * 0.1, 100.0); // Simulación
            
            // Determinar disponibilidad
            disponible = cpuUsage < 90.0 && memoryUsage < 90.0;
            
        } catch (Exception e) {
            System.err.println("❌ Error actualizando métricas: " + e.getMessage());
        }
    }
    
    private void logMetricas() {
        System.out.printf("📊 [%s] CPU: %.1f%% | MEM: %.1f%% | NET: %.1f%% | REQ: %d | DISP: %s%n",
            nodeId, cpuUsage, memoryUsage, networkUsage, requestCount.get(), 
            disponible ? "✅" : "❌");
    }
    
    @Override
    public MetricasRecursos obtenerMetricas(Current current) {
        requestCount.incrementAndGet();
        
        MetricasRecursos metricas = new MetricasRecursos();
        metricas.cpuUsage = cpuUsage;
        metricas.memoryUsage = memoryUsage;
        metricas.networkUsage = networkUsage;
        metricas.requestCount = requestCount.get();
        metricas.timestamp = System.currentTimeMillis();
        metricas.nodeId = nodeId;
        
        return metricas;
    }
    
    @Override
    public boolean estaDisponible(Current current) {
        requestCount.incrementAndGet();
        return disponible;
    }
    
    @Override
    public void notificarCarga(double carga, Current current) {
        requestCount.incrementAndGet();
        
        // Ajustar métricas basado en carga externa
        if (carga > 0) {
            networkUsage = Math.min(networkUsage + carga, 100.0);
        }
        
        System.out.printf("📈 [%s] Carga notificada: %.1f%% (Total NET: %.1f%%)%n", 
            nodeId, carga, networkUsage);
    }
    
    // Métodos públicos para uso interno
    public boolean necesitaEscalado() {
        return cpuUsage > CPU_THRESHOLD || 
               memoryUsage > MEMORY_THRESHOLD || 
               networkUsage > NETWORK_THRESHOLD;
    }
    
    public boolean puedeReducirse() {
        return cpuUsage < CPU_THRESHOLD * 0.3 && 
               memoryUsage < MEMORY_THRESHOLD * 0.3 && 
               networkUsage < NETWORK_THRESHOLD * 0.3;
    }
    
    public double getCargaTotal() {
        return (cpuUsage + memoryUsage + networkUsage) / 3.0;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void incrementarRequests() {
        requestCount.incrementAndGet();
    }
    
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("🔍 Monitor de Recursos detenido para nodo: " + nodeId);
    }
} 