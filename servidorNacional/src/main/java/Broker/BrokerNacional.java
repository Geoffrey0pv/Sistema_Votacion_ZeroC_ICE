package Broker;

import Demo.*;
import AdministradorCandidatos.AdministradorCandidatos;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Communicator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BrokerNacional implements IBrokerNacional {
    
    private final AdministradorCandidatos masterCandidatos;
    private final MonitorRecursos monitorMaster;
    private final BalanceadorCarga balanceador;
    private final GestorReplicas gestorReplicas;
    private final ScheduledExecutorService scheduler;
    private final Communicator communicator;
    
    // Configuración de escalado
    private static final double UMBRAL_ESCALADO = 50.0; // 50% como solicitado
    private static final double UMBRAL_REDUCCION = 20.0; // 20% para reducir réplicas
    private static final int MIN_REPLICAS = 0;
    private static final int MAX_REPLICAS = 10;
    private static final long INTERVALO_ESCALADO_MS = 30000; // 30 segundos
    
    // Control de escalado
    private final AtomicBoolean escaladoEnProceso = new AtomicBoolean(false);
    private long ultimoEscalado = 0;
    private long ultimaReduccion = 0;
    
    public BrokerNacional(Communicator communicator) {
        this.communicator = communicator;
        this.masterCandidatos = new AdministradorCandidatos(communicator);
        this.monitorMaster = new MonitorRecursos("master");
        this.balanceador = new BalanceadorCarga();
        this.gestorReplicas = new GestorReplicas(communicator, balanceador);
        this.scheduler = Executors.newScheduledThreadPool(4);
        
        // Iniciar monitoreo automático y escalado
        iniciarEscaladoAutomatico();
        
        System.out.println("🎯 Broker Nacional iniciado con escalado automático");
        System.out.printf("   📊 Umbral de escalado: %.1f%%\n", UMBRAL_ESCALADO);
        System.out.printf("   📉 Umbral de reducción: %.1f%%\n", UMBRAL_REDUCCION);
    }
    
    private void iniciarEscaladoAutomatico() {
        // Monitoreo y escalado automático cada 15 segundos
        scheduler.scheduleAtFixedRate(this::evaluarEscalado, 10, 15, TimeUnit.SECONDS);
        
        // Sincronización de datos cada 60 segundos
        scheduler.scheduleAtFixedRate(this::sincronizarReplicas, 30, 60, TimeUnit.SECONDS);
        
        // Reporte de estado cada 2 minutos
        scheduler.scheduleAtFixedRate(this::reportarEstadoCluster, 60, 120, TimeUnit.SECONDS);
    }
    
    private void evaluarEscalado() {
        if (escaladoEnProceso.get()) {
            return; // Ya hay un escalado en proceso
        }
        
        try {
            MetricasRecursos metricasMaster = monitorMaster.obtenerMetricas(null);
            double cargaPromedio = balanceador.getCargaPromedioCluster();
            int replicasActivas = gestorReplicas.getCantidadReplicasActivas();
            
            // Combinar carga del master con promedio del cluster
            double cargaTotal = (metricasMaster.cpuUsage + metricasMaster.memoryUsage + cargaPromedio) / 3.0;
            
            System.out.printf("📊 Evaluación escalado: Master=%.1f%%, Cluster=%.1f%%, Total=%.1f%%, Réplicas=%d%n",
                (metricasMaster.cpuUsage + metricasMaster.memoryUsage) / 2.0, cargaPromedio, cargaTotal, replicasActivas);
            
            long tiempoActual = System.currentTimeMillis();
            
            // Decidir si escalar hacia arriba
            if (cargaTotal > UMBRAL_ESCALADO && 
                replicasActivas < MAX_REPLICAS && 
                gestorReplicas.puedeCrearMasReplicas() &&
                (tiempoActual - ultimoEscalado) > INTERVALO_ESCALADO_MS) {
                
                System.out.printf("🚀 Iniciando escalado automático (Carga: %.1f%% > %.1f%%)%n", 
                    cargaTotal, UMBRAL_ESCALADO);
                escalarAutomaticamente();
                ultimoEscalado = tiempoActual;
            }
            // Decidir si reducir réplicas
            else if (cargaTotal < UMBRAL_REDUCCION && 
                     replicasActivas > MIN_REPLICAS &&
                     (tiempoActual - ultimaReduccion) > INTERVALO_ESCALADO_MS * 2) { // Más conservador para reducir
                
                System.out.printf("📉 Iniciando reducción automática (Carga: %.1f%% < %.1f%%)%n", 
                    cargaTotal, UMBRAL_REDUCCION);
                reducirReplicas();
                ultimaReduccion = tiempoActual;
            }
            
        } catch (Exception e) {
            System.err.printf("❌ Error evaluando escalado: %s%n", e.getMessage());
        }
    }
    
    private void sincronizarReplicas() {
        try {
            Candidato[] candidatos = masterCandidatos.obtenerTodosCandidatos(null);
            if (candidatos.length > 0) {
                gestorReplicas.sincronizarTodasReplicas(candidatos);
            }
        } catch (Exception e) {
            System.err.printf("❌ Error sincronizando réplicas: %s%n", e.getMessage());
        }
    }
    
    private void reportarEstadoCluster() {
        try {
            int totalReplicas = gestorReplicas.getCantidadReplicas();
            int replicasActivas = gestorReplicas.getCantidadReplicasActivas();
            double cargaPromedio = balanceador.getCargaPromedioCluster();
            MetricasRecursos metricasMaster = monitorMaster.obtenerMetricas(null);
            
            System.out.println("\n🎯 ===== ESTADO DEL BROKER NACIONAL =====");
            System.out.printf("   🖥️  Master: CPU=%.1f%%, MEM=%.1f%%, REQ=%d%n",
                metricasMaster.cpuUsage, metricasMaster.memoryUsage, metricasMaster.requestCount);
            System.out.printf("   🔄 Réplicas: %d total, %d activas%n", totalReplicas, replicasActivas);
            System.out.printf("   📊 Carga promedio cluster: %.1f%%%n", cargaPromedio);
            System.out.printf("   ⚖️  Algoritmo balanceo: %s%n", balanceador.getAlgoritmoActual());
            System.out.println("==========================================\n");
            
        } catch (Exception e) {
            System.err.printf("❌ Error reportando estado: %s%n", e.getMessage());
        }
    }
    
    // ========== MÉTODOS DE IAdministradorCandidatos ==========
    
    @Override
    public boolean cargarCandidatosDesdeCSV(String rutaArchivo, Current current) {
        monitorMaster.incrementarRequests();
        
        boolean resultado = masterCandidatos.cargarCandidatosDesdeCSV(rutaArchivo, current);
        if (resultado) {
            // Sincronizar con réplicas de forma asíncrona
            scheduler.execute(() -> sincronizarReplicas());
        }
        return resultado;
    }
    
    @Override
    public boolean cargarCandidatosDesdeArray(Candidato[] candidatos, Current current) {
        monitorMaster.incrementarRequests();
        
        boolean resultado = masterCandidatos.cargarCandidatosDesdeArray(candidatos, current);
        if (resultado) {
            // Sincronizar con réplicas de forma asíncrona
            scheduler.execute(() -> sincronizarReplicas());
        }
        return resultado;
    }
    
    @Override
    public int obtenerCantidadCandidatos(Current current) {
        // Delegar a la mejor réplica disponible o al master
        String mejorReplica = balanceador.obtenerMejorReplica(null);
        if (mejorReplica != null) {
            // TODO: Implementar llamada a réplica específica
            // Por ahora usar el master
        }
        
        monitorMaster.incrementarRequests();
        return masterCandidatos.obtenerCantidadCandidatos(current);
    }
    
    @Override
    public Candidato[] obtenerTodosCandidatos(Current current) {
        // Delegar a la mejor réplica disponible o al master
        String mejorReplica = balanceador.obtenerMejorReplica(null);
        if (mejorReplica != null) {
            // TODO: Implementar llamada a réplica específica
            // Por ahora usar el master
        }
        
        monitorMaster.incrementarRequests();
        return masterCandidatos.obtenerTodosCandidatos(current);
    }
    
    @Override
    public boolean limpiarCandidatos(Current current) {
        monitorMaster.incrementarRequests();
        
        boolean resultado = masterCandidatos.limpiarCandidatos(current);
        if (resultado) {
            // Sincronizar con réplicas
            scheduler.execute(() -> sincronizarReplicas());
        }
        return resultado;
    }
    
    @Override
    public boolean enviarCandidatosARegional(String endpointRegional, Current current) {
        monitorMaster.incrementarRequests();
        return masterCandidatos.enviarCandidatosARegional(endpointRegional, current);
    }
    
    @Override
    public boolean enviarCandidatosATodosRegionales(Current current) {
        monitorMaster.incrementarRequests();
        return masterCandidatos.enviarCandidatosATodosRegionales(current);
    }
    
    // ========== MÉTODOS DE IBrokerNacional ==========
    
    @Override
    public boolean registrarReplica(String nodeId, String endpoint, Current current) {
        return gestorReplicas.crearReplica(nodeId, endpoint, current);
    }
    
    @Override
    public boolean desregistrarReplica(String nodeId, Current current) {
        return gestorReplicas.eliminarReplica(nodeId, current);
    }
    
    @Override
    public MetricasRecursos obtenerMetricasGlobales(Current current) {
        MetricasRecursos metricasGlobales = monitorMaster.obtenerMetricas(null);
        
        // Agregar información del cluster
        InfoReplica[] replicas = balanceador.obtenerEstadoReplicas(null);
        long totalRequests = metricasGlobales.requestCount;
        double cpuPromedio = metricasGlobales.cpuUsage;
        double memPromedio = metricasGlobales.memoryUsage;
        
        if (replicas.length > 0) {
            for (InfoReplica replica : replicas) {
                if (replica.activa) {
                    totalRequests += replica.metricas.requestCount;
                    cpuPromedio += replica.metricas.cpuUsage;
                    memPromedio += replica.metricas.memoryUsage;
                }
            }
            
            int nodos = replicas.length + 1; // +1 por el master
            cpuPromedio /= nodos;
            memPromedio /= nodos;
        }
        
        MetricasRecursos globales = new MetricasRecursos();
        globales.nodeId = "cluster-global";
        globales.cpuUsage = cpuPromedio;
        globales.memoryUsage = memPromedio;
        globales.networkUsage = balanceador.getCargaPromedioCluster();
        globales.requestCount = totalRequests;
        globales.timestamp = System.currentTimeMillis();
        
        return globales;
    }
    
    @Override
    public InfoReplica[] obtenerReplicasDisponibles(Current current) {
        return gestorReplicas.obtenerReplicasActivas(current);
    }
    
    @Override
    public String obtenerEndpointOptimo(Current current) {
        return balanceador.obtenerMejorReplica(current);
    }
    
    @Override
    public boolean escalarAutomaticamente(Current current) {
        return escalarAutomaticamente();
    }
    
    @Override
    public boolean reducirReplicas(Current current) {
        return reducirReplicas();
    }
    
    // ========== MÉTODOS PÚBLICOS ADICIONALES ==========
    
    public boolean escalarAutomaticamente() {
        if (!escaladoEnProceso.compareAndSet(false, true)) {
            System.out.println("⚠️ Escalado ya en proceso, omitiendo...");
            return false;
        }
        
        try {
            if (!gestorReplicas.puedeCrearMasReplicas()) {
                System.out.printf("⚠️ Límite máximo de réplicas alcanzado (%d)%n", MAX_REPLICAS);
                return false;
            }
            
            boolean resultado = gestorReplicas.crearReplicaAutomatica();
            
            if (resultado) {
                System.out.println("✅ Escalado automático completado");
                
                // Sincronizar la nueva réplica
                scheduler.schedule(() -> sincronizarReplicas(), 5, TimeUnit.SECONDS);
            } else {
                System.err.println("❌ Error en escalado automático");
            }
            
            return resultado;
            
        } finally {
            escaladoEnProceso.set(false);
        }
    }
    
    public boolean reducirReplicas() {
        if (!escaladoEnProceso.compareAndSet(false, true)) {
            System.out.println("⚠️ Operación de escalado ya en proceso, omitiendo reducción...");
            return false;
        }
        
        try {
            InfoReplica[] replicas = gestorReplicas.obtenerReplicasActivas(null);
            
            if (replicas.length <= MIN_REPLICAS) {
                System.out.printf("⚠️ Número mínimo de réplicas alcanzado (%d)%n", MIN_REPLICAS);
                return false;
            }
            
            // Encontrar la réplica con menor carga para eliminar
            InfoReplica replicaAEliminar = null;
            double menorCarga = Double.MAX_VALUE;
            
            for (InfoReplica replica : replicas) {
                if (replica.activa) {
                    double carga = (replica.metricas.cpuUsage + replica.metricas.memoryUsage) / 2.0;
                    if (carga < menorCarga) {
                        menorCarga = carga;
                        replicaAEliminar = replica;
                    }
                }
            }
            
            if (replicaAEliminar != null) {
                boolean resultado = gestorReplicas.eliminarReplica(replicaAEliminar.nodeId, null);
                
                if (resultado) {
                    System.out.printf("✅ Réplica eliminada: %s (Carga: %.1f%%)%n", 
                        replicaAEliminar.nodeId, menorCarga);
                } else {
                    System.err.printf("❌ Error eliminando réplica: %s%n", replicaAEliminar.nodeId);
                }
                
                return resultado;
            }
            
            return false;
            
        } finally {
            escaladoEnProceso.set(false);
        }
    }
    
    public AdministradorCandidatos getMasterCandidatos() {
        return masterCandidatos;
    }
    
    public BalanceadorCarga getBalanceador() {
        return balanceador;
    }
    
    public GestorReplicas getGestorReplicas() {
        return gestorReplicas;
    }
    
    public MonitorRecursos getMonitorMaster() {
        return monitorMaster;
    }
    
    public void shutdown() {
        System.out.println("🎯 Deteniendo Broker Nacional...");
        
        // Detener scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Detener componentes
        if (gestorReplicas != null) {
            gestorReplicas.shutdown();
        }
        
        if (balanceador != null) {
            balanceador.shutdown();
        }
        
        if (monitorMaster != null) {
            monitorMaster.shutdown();
        }
        
        System.out.println("✅ Broker Nacional detenido");
    }
} 