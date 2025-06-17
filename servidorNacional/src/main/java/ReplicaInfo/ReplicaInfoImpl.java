package ReplicaInfo;

import Demo.*;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Properties;
import Config.HostConfig;

/**
 * Implementación del servicio de información de réplicas
 * Proporciona información sobre el puerto de ejecución y estado de la réplica
 */
public class ReplicaInfoImpl implements IReplicaInfo {
    
    private final String replicaId;
    private final String nodeId;
    private final int puerto;
    private final String host;
    private final String endpoint;
    private final long tiempoInicio;
    private boolean activa;
    private final HostConfig hostConfig;
    
    public ReplicaInfoImpl(Properties properties) {
        // Obtener configuración centralizada de hosts
        this.hostConfig = HostConfig.getInstance();
        
        // Obtener información de las propiedades de ICE
        this.replicaId = properties.getProperty("Replica.Id");
        this.nodeId = "nodeNacional" + replicaId;
        this.puerto = Integer.parseInt(properties.getPropertyWithDefault("Replica.Port", "9090"));
        this.host = hostConfig.getReplicaHost(); // Usar configuración centralizada
        this.endpoint = "tcp -h " + host + " -p " + puerto;
        this.tiempoInicio = System.currentTimeMillis();
        this.activa = true;
        
        System.out.println("🔧 ReplicaInfo inicializado:");
        System.out.println("   📍 Réplica ID: " + replicaId);
        System.out.println("   🌐 Node ID: " + nodeId);
        System.out.println("   🔌 Puerto: " + puerto);
        System.out.println("   🏠 Host: " + host + " (desde configuración)");
        System.out.println("   📡 Endpoint: " + endpoint);
    }
    
    @Override
    public InfoEjecucionReplica obtenerInfoReplica(Current current) {
        System.out.println("📊 Consultando información de réplica: " + replicaId);
        
        InfoEjecucionReplica info = new InfoEjecucionReplica();
        info.replicaId = this.replicaId;
        info.nodeId = this.nodeId;
        info.puerto = this.puerto;
        info.host = this.host;
        info.endpoint = this.endpoint;
        info.activa = this.activa;
        info.tiempoInicio = this.tiempoInicio;
        
        // Obtener métricas básicas
        info.metricas = obtenerMetricasBasicas();
        
        return info;
    }
    
    @Override
    public int obtenerPuertoEjecucion(Current current) {
        System.out.println("🔌 Consultando puerto de ejecución: " + puerto);
        return this.puerto;
    }
    
    @Override
    public String obtenerEndpoint(Current current) {
        System.out.println("📡 Consultando endpoint: " + endpoint);
        return this.endpoint;
    }
    
    @Override
    public String obtenerReplicaId(Current current) {
        System.out.println("🆔 Consultando ID de réplica: " + replicaId);
        return this.replicaId;
    }
    
    @Override
    public boolean estaActiva(Current current) {
        System.out.println("✅ Consultando estado de réplica: " + (activa ? "ACTIVA" : "INACTIVA"));
        return this.activa;
    }
    
    @Override
    public long obtenerTiempoActividad(Current current) {
        long tiempoActividad = System.currentTimeMillis() - tiempoInicio;
        System.out.println("⏱️ Tiempo de actividad: " + tiempoActividad + "ms");
        return tiempoActividad;
    }
    
    /**
     * Obtiene métricas básicas del sistema
     */
    private MetricasRecursos obtenerMetricasBasicas() {
        MetricasRecursos metricas = new MetricasRecursos();
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        metricas.cpuUsage = 0.0; // Simplificado
        metricas.memoryUsage = (double) usedMemory / totalMemory * 100.0;
        metricas.networkUsage = 0.0; // Simplificado
        metricas.requestCount = 0; // Simplificado
        metricas.timestamp = System.currentTimeMillis();
        metricas.nodeId = this.nodeId;
        
        return metricas;
    }
    
    /**
     * Método para desactivar la réplica
     */
    public void desactivar() {
        this.activa = false;
        System.out.println("🛑 Réplica " + replicaId + " desactivada");
    }
    
    /**
     * Método para activar la réplica
     */
    public void activar() {
        this.activa = true;
        System.out.println("✅ Réplica " + replicaId + " activada");
    }
    
    /**
     * Obtener información resumida para logs
     */
    public String obtenerInfoResumida() {
        return String.format("Réplica[ID=%s, Puerto=%d, Activa=%s, Tiempo=%dms]", 
            replicaId, puerto, activa, System.currentTimeMillis() - tiempoInicio);
    }
} 