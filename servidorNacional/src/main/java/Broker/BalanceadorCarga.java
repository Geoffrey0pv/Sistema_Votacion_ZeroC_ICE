package Broker;

import Demo.*;
import com.zeroc.Ice.Current;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BalanceadorCarga implements IBalanceadorCarga {
    
    private final ConcurrentHashMap<String, InfoReplica> replicas;
    private final ReadWriteLock lock;
    private final AtomicInteger roundRobinIndex;
    
    // Algoritmos de balanceo disponibles
    public enum AlgoritmoBalanceo {
        ROUND_ROBIN,
        LEAST_CONNECTIONS,
        WEIGHTED_RESPONSE_TIME,
        LEAST_CPU_USAGE
    }
    
    private AlgoritmoBalanceo algoritmoActual;
    
    public BalanceadorCarga() {
        this.replicas = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.roundRobinIndex = new AtomicInteger(0);
        this.algoritmoActual = AlgoritmoBalanceo.LEAST_CPU_USAGE;
        
        System.out.println("⚖️ Balanceador de Carga iniciado con algoritmo: " + algoritmoActual);
    }
    
    @Override
    public String obtenerMejorReplica(Current current) {
        lock.readLock().lock();
        try {
            List<InfoReplica> replicasActivas = obtenerReplicasActivasInterno();
            
            if (replicasActivas.isEmpty()) {
                System.err.println("⚠️ No hay réplicas activas disponibles");
                return null;
            }
            
            InfoReplica mejorReplica = seleccionarReplica(replicasActivas);
            
            if (mejorReplica != null) {
                System.out.printf("🎯 Seleccionada réplica: %s (CPU: %.1f%%, MEM: %.1f%%)%n",
                    mejorReplica.nodeId, 
                    mejorReplica.metricas.cpuUsage,
                    mejorReplica.metricas.memoryUsage);
                return mejorReplica.endpoint;
            }
            
            return null;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private InfoReplica seleccionarReplica(List<InfoReplica> replicasActivas) {
        switch (algoritmoActual) {
            case ROUND_ROBIN:
                return seleccionarRoundRobin(replicasActivas);
            case LEAST_CONNECTIONS:
                return seleccionarMenosConexiones(replicasActivas);
            case WEIGHTED_RESPONSE_TIME:
                return seleccionarMenorTiempoRespuesta(replicasActivas);
            case LEAST_CPU_USAGE:
            default:
                return seleccionarMenorCPU(replicasActivas);
        }
    }
    
    private InfoReplica seleccionarRoundRobin(List<InfoReplica> replicas) {
        if (replicas.isEmpty()) return null;
        
        int index = roundRobinIndex.getAndIncrement() % replicas.size();
        return replicas.get(index);
    }
    
    private InfoReplica seleccionarMenosConexiones(List<InfoReplica> replicas) {
        return replicas.stream()
            .min(Comparator.comparingLong(r -> r.metricas.requestCount))
            .orElse(null);
    }
    
    private InfoReplica seleccionarMenorTiempoRespuesta(List<InfoReplica> replicas) {
        // Simulamos tiempo de respuesta basado en carga total
        return replicas.stream()
            .min(Comparator.comparingDouble(r -> 
                (r.metricas.cpuUsage + r.metricas.memoryUsage + r.metricas.networkUsage) / 3.0))
            .orElse(null);
    }
    
    private InfoReplica seleccionarMenorCPU(List<InfoReplica> replicas) {
        return replicas.stream()
            .filter(r -> r.metricas.cpuUsage < 80.0) // Filtrar réplicas sobrecargadas
            .min(Comparator.comparingDouble(r -> r.metricas.cpuUsage))
            .orElse(replicas.stream()
                .min(Comparator.comparingDouble(r -> r.metricas.cpuUsage))
                .orElse(null));
    }
    
    @Override
    public void registrarReplica(String nodeId, String endpoint, Current current) {
        lock.writeLock().lock();
        try {
            InfoReplica replica = new InfoReplica();
            replica.nodeId = nodeId;
            replica.endpoint = endpoint;
            replica.activa = true;
            replica.tiempoCreacion = System.currentTimeMillis();
            
            // Inicializar métricas por defecto
            replica.metricas = new MetricasRecursos();
            replica.metricas.nodeId = nodeId;
            replica.metricas.cpuUsage = 0.0;
            replica.metricas.memoryUsage = 0.0;
            replica.metricas.networkUsage = 0.0;
            replica.metricas.requestCount = 0;
            replica.metricas.timestamp = System.currentTimeMillis();
            
            replicas.put(nodeId, replica);
            
            System.out.printf("✅ Réplica registrada: %s -> %s%n", nodeId, endpoint);
            mostrarEstadoReplicas();
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void desregistrarReplica(String nodeId, Current current) {
        lock.writeLock().lock();
        try {
            InfoReplica replica = replicas.remove(nodeId);
            if (replica != null) {
                System.out.printf("❌ Réplica desregistrada: %s%n", nodeId);
                mostrarEstadoReplicas();
            } else {
                System.err.printf("⚠️ Intento de desregistrar réplica inexistente: %s%n", nodeId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void actualizarMetricas(String nodeId, MetricasRecursos metricas, Current current) {
        lock.writeLock().lock();
        try {
            InfoReplica replica = replicas.get(nodeId);
            if (replica != null) {
                replica.metricas = metricas;
                
                // Log detallado cada cierto tiempo
                if (metricas.requestCount % 10 == 0) {
                    System.out.printf("📊 Métricas actualizadas [%s]: CPU=%.1f%%, MEM=%.1f%%, REQ=%d%n",
                        nodeId, metricas.cpuUsage, metricas.memoryUsage, metricas.requestCount);
                }
            } else {
                System.err.printf("⚠️ Intento de actualizar métricas de réplica inexistente: %s%n", nodeId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public InfoReplica[] obtenerEstadoReplicas(Current current) {
        lock.readLock().lock();
        try {
            return replicas.values().toArray(new InfoReplica[0]);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Métodos públicos para uso interno
    public List<InfoReplica> obtenerReplicasActivasInterno() {
        return replicas.values().stream()
            .filter(r -> r.activa)
            .sorted(Comparator.comparing(r -> r.nodeId))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public int getCantidadReplicasActivas() {
        return (int) replicas.values().stream().filter(r -> r.activa).count();
    }
    
    public boolean tieneReplicasDisponibles() {
        return replicas.values().stream().anyMatch(r -> r.activa && r.metricas.cpuUsage < 90.0);
    }
    
    public double getCargaPromedioCluster() {
        List<InfoReplica> activas = obtenerReplicasActivasInterno();
        if (activas.isEmpty()) return 0.0;
        
        return activas.stream()
            .mapToDouble(r -> (r.metricas.cpuUsage + r.metricas.memoryUsage) / 2.0)
            .average()
            .orElse(0.0);
    }
    
    public List<String> obtenerReplicasSobrecargadas() {
        return replicas.values().stream()
            .filter(r -> r.activa && (r.metricas.cpuUsage > 80.0 || r.metricas.memoryUsage > 80.0))
            .map(r -> r.nodeId)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public void cambiarAlgoritmo(AlgoritmoBalanceo nuevoAlgoritmo) {
        this.algoritmoActual = nuevoAlgoritmo;
        System.out.println("⚖️ Algoritmo de balanceo cambiado a: " + nuevoAlgoritmo);
    }
    
    public AlgoritmoBalanceo getAlgoritmoActual() {
        return algoritmoActual;
    }
    
    private void mostrarEstadoReplicas() {
        System.out.println("\n📋 Estado actual del cluster:");
        System.out.println("┌─────────────────┬──────────────────────────────┬────────┬─────────┬─────────┐");
        System.out.println("│ Node ID         │ Endpoint                     │ Estado │ CPU %   │ MEM %   │");
        System.out.println("├─────────────────┼──────────────────────────────┼────────┼─────────┼─────────┤");
        
        replicas.values().stream()
            .sorted(Comparator.comparing(r -> r.nodeId))
            .forEach(r -> {
                String estado = r.activa ? "✅ ACT" : "❌ INA";
                System.out.printf("│ %-15s │ %-28s │ %-6s │ %6.1f  │ %6.1f  │%n",
                    r.nodeId, 
                    r.endpoint.length() > 28 ? r.endpoint.substring(0, 25) + "..." : r.endpoint,
                    estado,
                    r.metricas.cpuUsage,
                    r.metricas.memoryUsage);
            });
        
        System.out.println("└─────────────────┴──────────────────────────────┴────────┴─────────┴─────────┘");
        System.out.printf("Total réplicas: %d | Activas: %d | Carga promedio: %.1f%%%n%n",
            replicas.size(), getCantidadReplicasActivas(), getCargaPromedioCluster());
    }
    
    public void shutdown() {
        lock.writeLock().lock();
        try {
            replicas.clear();
            System.out.println("⚖️ Balanceador de Carga detenido");
        } finally {
            lock.writeLock().unlock();
        }
    }
} 