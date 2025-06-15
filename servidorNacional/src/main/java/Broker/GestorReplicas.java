package Broker;

import Demo.*;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Identity;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GestorReplicas implements IGestorReplicas {
    
    private final Communicator communicator;
    private final BalanceadorCarga balanceador;
    private final ConcurrentHashMap<String, ReplicaInfo> replicasGestionadas;
    private final AtomicInteger contadorReplicas;
    private final ScheduledExecutorService scheduler;
    
    // Configuración
    private static final int PUERTO_BASE_REPLICAS = 10000;
    private static final int MAX_REPLICAS = 10;
    private static final String HOST_REPLICAS = "localhost";
    
    // Información interna de réplicas
    private static class ReplicaInfo {
        String nodeId;
        String endpoint;
        int puerto;
        ObjectAdapter adapter;
        ReplicaNacional instancia;
        boolean activa;
        long tiempoCreacion;
        Process proceso; // Para réplicas en procesos separados (futuro)
        
        ReplicaInfo(String nodeId, String endpoint, int puerto) {
            this.nodeId = nodeId;
            this.endpoint = endpoint;
            this.puerto = puerto;
            this.activa = false;
            this.tiempoCreacion = System.currentTimeMillis();
        }
    }
    
    public GestorReplicas(Communicator communicator, BalanceadorCarga balanceador) {
        this.communicator = communicator;
        this.balanceador = balanceador;
        this.replicasGestionadas = new ConcurrentHashMap<>();
        this.contadorReplicas = new AtomicInteger(1);
        this.scheduler = Executors.newScheduledThreadPool(3);
        
        // Monitoreo periódico de réplicas
        iniciarMonitoreoReplicas();
        
        System.out.println("🏭 Gestor de Réplicas iniciado");
    }
    
    private void iniciarMonitoreoReplicas() {
        // Verificar salud de réplicas cada 30 segundos
        scheduler.scheduleAtFixedRate(this::verificarSaludReplicas, 30, 30, TimeUnit.SECONDS);
        
        // Limpiar réplicas inactivas cada 5 minutos
        scheduler.scheduleAtFixedRate(this::limpiarReplicasInactivas, 300, 300, TimeUnit.SECONDS);
    }
    
    @Override
    public boolean crearReplica(String nodeId, String endpoint, Current current) {
        try {
            if (replicasGestionadas.size() >= MAX_REPLICAS) {
                System.err.printf("❌ Límite máximo de réplicas alcanzado (%d)%n", MAX_REPLICAS);
                return false;
            }
            
            if (replicasGestionadas.containsKey(nodeId)) {
                System.err.printf("⚠️ Ya existe una réplica con ID: %s%n", nodeId);
                return false;
            }
            
            // Si no se especifica endpoint, generar uno automáticamente
            if (endpoint == null || endpoint.trim().isEmpty()) {
                int puerto = PUERTO_BASE_REPLICAS + contadorReplicas.getAndIncrement();
                endpoint = String.format("tcp -h %s -p %d", HOST_REPLICAS, puerto);
            }
            
            System.out.printf("🚀 Creando réplica: %s -> %s%n", nodeId, endpoint);
            
            // Crear información de la réplica
            int puerto = extraerPuertoDeEndpoint(endpoint);
            ReplicaInfo replicaInfo = new ReplicaInfo(nodeId, endpoint, puerto);
            
            // Crear adapter para la réplica
            String adapterName = "ReplicaAdapter_" + nodeId;
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(adapterName, endpoint);
            
            // Crear instancia de la réplica
            ReplicaNacional replica = new ReplicaNacional(nodeId, communicator, balanceador);
            
            // Registrar la réplica en el adapter
            Identity identity = new Identity();
            identity.name = "AdministradorCandidatos_" + nodeId;
            identity.category = "Replica";
            adapter.add(replica, identity);
            
            // Activar el adapter
            adapter.activate();
            
            // Guardar información
            replicaInfo.adapter = adapter;
            replicaInfo.instancia = replica;
            replicaInfo.activa = true;
            
            replicasGestionadas.put(nodeId, replicaInfo);
            
            // Registrar en el balanceador
            String endpointCompleto = identity.name + ":" + endpoint;
            balanceador.registrarReplica(nodeId, endpointCompleto, null);
            
            System.out.printf("✅ Réplica creada exitosamente: %s%n", nodeId);
            mostrarEstadoReplicas();
            
            return true;
            
        } catch (Exception e) {
            System.err.printf("❌ Error creando réplica %s: %s%n", nodeId, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean eliminarReplica(String nodeId, Current current) {
        try {
            ReplicaInfo replicaInfo = replicasGestionadas.remove(nodeId);
            
            if (replicaInfo == null) {
                System.err.printf("⚠️ No se encontró réplica para eliminar: %s%n", nodeId);
                return false;
            }
            
            System.out.printf("🗑️ Eliminando réplica: %s%n", nodeId);
            
            // Desactivar la réplica
            if (replicaInfo.instancia != null) {
                replicaInfo.instancia.shutdown();
            }
            
            // Destruir el adapter
            if (replicaInfo.adapter != null) {
                try {
                    replicaInfo.adapter.destroy();
                } catch (Exception e) {
                    System.err.printf("⚠️ Error destruyendo adapter de %s: %s%n", nodeId, e.getMessage());
                }
            }
            
            // Desregistrar del balanceador
            balanceador.desregistrarReplica(nodeId, null);
            
            System.out.printf("✅ Réplica eliminada: %s%n", nodeId);
            mostrarEstadoReplicas();
            
            return true;
            
        } catch (Exception e) {
            System.err.printf("❌ Error eliminando réplica %s: %s%n", nodeId, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public InfoReplica[] obtenerReplicasActivas(Current current) {
        List<InfoReplica> replicas = new ArrayList<>();
        
        for (ReplicaInfo info : replicasGestionadas.values()) {
            if (info.activa && info.instancia != null) {
                InfoReplica replica = new InfoReplica();
                replica.nodeId = info.nodeId;
                replica.endpoint = info.endpoint;
                replica.activa = info.activa;
                replica.tiempoCreacion = info.tiempoCreacion;
                
                // Obtener métricas actuales
                try {
                    replica.metricas = info.instancia.obtenerMetricas(null);
                } catch (Exception e) {
                    // Crear métricas por defecto si hay error
                    replica.metricas = new MetricasRecursos();
                    replica.metricas.nodeId = info.nodeId;
                    replica.metricas.timestamp = System.currentTimeMillis();
                }
                
                replicas.add(replica);
            }
        }
        
        return replicas.toArray(new InfoReplica[0]);
    }
    
    @Override
    public boolean activarReplica(String nodeId, Current current) {
        ReplicaInfo info = replicasGestionadas.get(nodeId);
        if (info != null && !info.activa) {
            info.activa = true;
            balanceador.registrarReplica(nodeId, info.endpoint, null);
            System.out.printf("✅ Réplica activada: %s%n", nodeId);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean desactivarReplica(String nodeId, Current current) {
        ReplicaInfo info = replicasGestionadas.get(nodeId);
        if (info != null && info.activa) {
            info.activa = false;
            balanceador.desregistrarReplica(nodeId, null);
            System.out.printf("⏸️ Réplica desactivada: %s%n", nodeId);
            return true;
        }
        return false;
    }
    
    @Override
    public InfoReplica obtenerInfoReplica(String nodeId, Current current) {
        ReplicaInfo info = replicasGestionadas.get(nodeId);
        if (info == null) return null;
        
        InfoReplica replica = new InfoReplica();
        replica.nodeId = info.nodeId;
        replica.endpoint = info.endpoint;
        replica.activa = info.activa;
        replica.tiempoCreacion = info.tiempoCreacion;
        
        if (info.instancia != null) {
            try {
                replica.metricas = info.instancia.obtenerMetricas(null);
            } catch (Exception e) {
                replica.metricas = new MetricasRecursos();
                replica.metricas.nodeId = nodeId;
                replica.metricas.timestamp = System.currentTimeMillis();
            }
        }
        
        return replica;
    }
    
    // Métodos públicos para uso interno
    public boolean crearReplicaAutomatica() {
        String nodeId = "replica-auto-" + System.currentTimeMillis();
        return crearReplica(nodeId, null, null);
    }
    
    public int getCantidadReplicas() {
        return replicasGestionadas.size();
    }
    
    public int getCantidadReplicasActivas() {
        return (int) replicasGestionadas.values().stream().filter(r -> r.activa).count();
    }
    
    public boolean puedeCrearMasReplicas() {
        return replicasGestionadas.size() < MAX_REPLICAS;
    }
    
    public void sincronizarTodasReplicas(Candidato[] candidatos) {
        System.out.printf("🔄 Sincronizando %d candidatos con %d réplicas%n", 
            candidatos.length, replicasGestionadas.size());
        
        CompletableFuture<Void>[] futures = replicasGestionadas.values().stream()
            .filter(info -> info.activa && info.instancia != null)
            .map(info -> CompletableFuture.runAsync(() -> {
                try {
                    info.instancia.sincronizarConMaster(candidatos, null);
                    System.out.printf("✅ Sincronizada réplica: %s%n", info.nodeId);
                } catch (Exception e) {
                    System.err.printf("❌ Error sincronizando %s: %s%n", info.nodeId, e.getMessage());
                }
            }))
            .toArray(CompletableFuture[]::new);
        
        // Esperar a que todas las sincronizaciones terminen
        CompletableFuture.allOf(futures).join();
        System.out.println("🔄 Sincronización completada");
    }
    
    private void verificarSaludReplicas() {
        System.out.println("🏥 Verificando salud de réplicas...");
        
        for (ReplicaInfo info : replicasGestionadas.values()) {
            if (info.activa && info.instancia != null) {
                try {
                    boolean disponible = info.instancia.estaDisponible(null);
                    if (!disponible) {
                        System.err.printf("⚠️ Réplica %s no responde, desactivando...%n", info.nodeId);
                        desactivarReplica(info.nodeId, null);
                    }
                } catch (Exception e) {
                    System.err.printf("❌ Error verificando %s: %s%n", info.nodeId, e.getMessage());
                    desactivarReplica(info.nodeId, null);
                }
            }
        }
    }
    
    private void limpiarReplicasInactivas() {
        long tiempoLimite = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        
        List<String> paraEliminar = new ArrayList<>();
        for (ReplicaInfo info : replicasGestionadas.values()) {
            if (!info.activa && info.tiempoCreacion < tiempoLimite) {
                paraEliminar.add(info.nodeId);
            }
        }
        
        for (String nodeId : paraEliminar) {
            System.out.printf("🧹 Limpiando réplica inactiva: %s%n", nodeId);
            eliminarReplica(nodeId, null);
        }
    }
    
    private int extraerPuertoDeEndpoint(String endpoint) {
        try {
            String[] partes = endpoint.split("-p");
            if (partes.length > 1) {
                return Integer.parseInt(partes[1].trim().split("\\s+")[0]);
            }
        } catch (Exception e) {
            // Ignorar errores de parsing
        }
        return PUERTO_BASE_REPLICAS + contadorReplicas.get();
    }
    
    private void mostrarEstadoReplicas() {
        System.out.println("\n🏭 Estado del Gestor de Réplicas:");
        System.out.printf("   Total: %d | Activas: %d | Máximo: %d%n",
            getCantidadReplicas(), getCantidadReplicasActivas(), MAX_REPLICAS);
        
        replicasGestionadas.values().forEach(info -> {
            String estado = info.activa ? "🟢" : "🔴";
            long tiempoVida = (System.currentTimeMillis() - info.tiempoCreacion) / 1000;
            System.out.printf("   %s %s (Puerto: %d, Vida: %ds)%n",
                estado, info.nodeId, info.puerto, tiempoVida);
        });
        System.out.println();
    }
    
    public void shutdown() {
        System.out.println("🏭 Deteniendo Gestor de Réplicas...");
        
        // Detener scheduler
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
        
        // Eliminar todas las réplicas
        List<String> nodeIds = new ArrayList<>(replicasGestionadas.keySet());
        for (String nodeId : nodeIds) {
            eliminarReplica(nodeId, null);
        }
        
        System.out.println("🏭 Gestor de Réplicas detenido");
    }
} 