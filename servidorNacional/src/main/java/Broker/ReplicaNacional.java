package Broker;

import Demo.*;
import AdministradorCandidatos.AdministradorCandidatos;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Communicator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReplicaNacional implements IReplicaNacional {
    
    private final String nodeId;
    private final AdministradorCandidatos administradorCandidatos;
    private final MonitorRecursos monitorRecursos;
    private final BalanceadorCarga balanceador;
    private final ScheduledExecutorService scheduler;
    private volatile boolean activa;
    
    public ReplicaNacional(String nodeId, Communicator communicator, BalanceadorCarga balanceador) {
        this.nodeId = nodeId;
        this.administradorCandidatos = new AdministradorCandidatos(communicator);
        this.monitorRecursos = new MonitorRecursos(nodeId);
        this.balanceador = balanceador;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.activa = true;
        
        // Iniciar reporte periódico de métricas al balanceador
        iniciarReporteMetricas();
        
        System.out.printf("🔄 Réplica Nacional iniciada: %s%n", nodeId);
    }
    
    private void iniciarReporteMetricas() {
        // Reportar métricas al balanceador cada 10 segundos
        scheduler.scheduleAtFixedRate(() -> {
            if (activa && balanceador != null) {
                try {
                    MetricasRecursos metricas = monitorRecursos.obtenerMetricas(null);
                    balanceador.actualizarMetricas(nodeId, metricas, null);
                } catch (Exception e) {
                    System.err.printf("❌ Error reportando métricas de %s: %s%n", nodeId, e.getMessage());
                }
            }
        }, 5, 10, TimeUnit.SECONDS);
    }
    
    // ========== MÉTODOS DE IAdministradorCandidatos ==========
    
    @Override
    public boolean cargarCandidatosDesdeCSV(String rutaArchivo, Current current) {
        monitorRecursos.incrementarRequests();
        return administradorCandidatos.cargarCandidatosDesdeCSV(rutaArchivo, current);
    }
    
    @Override
    public boolean cargarCandidatosDesdeArray(Candidato[] candidatos, Current current) {
        monitorRecursos.incrementarRequests();
        return administradorCandidatos.cargarCandidatosDesdeArray(candidatos, current);
    }
    
    @Override
    public int obtenerCantidadCandidatos(Current current) {
        monitorRecursos.incrementarRequests();
        return administradorCandidatos.obtenerCantidadCandidatos(current);
    }
    
    @Override
    public Candidato[] obtenerTodosCandidatos(Current current) {
        monitorRecursos.incrementarRequests();
        return administradorCandidatos.obtenerTodosCandidatos(current);
    }
    
    @Override
    public boolean limpiarCandidatos(Current current) {
        monitorRecursos.incrementarRequests();
        return administradorCandidatos.limpiarCandidatos(current);
    }
    
    @Override
    public boolean enviarCandidatosARegional(String endpointRegional, Current current) {
        monitorRecursos.incrementarRequests();
        return administradorCandidatos.enviarCandidatosARegional(endpointRegional, current);
    }
    
    @Override
    public boolean enviarCandidatosATodosRegionales(Current current) {
        monitorRecursos.incrementarRequests();
        return administradorCandidatos.enviarCandidatosATodosRegionales(current);
    }
    
    // ========== MÉTODOS DE IMonitorRecursos ==========
    
    @Override
    public MetricasRecursos obtenerMetricas(Current current) {
        return monitorRecursos.obtenerMetricas(current);
    }
    
    @Override
    public boolean estaDisponible(Current current) {
        return activa && monitorRecursos.estaDisponible(current);
    }
    
    @Override
    public void notificarCarga(double carga, Current current) {
        monitorRecursos.notificarCarga(carga, current);
    }
    
    // ========== MÉTODOS DE IReplicaNacional ==========
    
    @Override
    public boolean sincronizarConMaster(Candidato[] candidatos, Current current) {
        try {
            System.out.printf("🔄 [%s] Sincronizando %d candidatos desde master%n", nodeId, candidatos.length);
            
            // Limpiar datos actuales
            administradorCandidatos.limpiarCandidatos(current);
            
            // Cargar nuevos datos
            boolean resultado = administradorCandidatos.cargarCandidatosDesdeArray(candidatos, current);
            
            if (resultado) {
                System.out.printf("✅ [%s] Sincronización completada exitosamente%n", nodeId);
            } else {
                System.err.printf("❌ [%s] Error en sincronización%n", nodeId);
            }
            
            return resultado;
            
        } catch (Exception e) {
            System.err.printf("❌ [%s] Error sincronizando: %s%n", nodeId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean notificarEstado(MetricasRecursos metricas, Current current) {
        try {
            // Actualizar métricas en el balanceador si está disponible
            if (balanceador != null) {
                balanceador.actualizarMetricas(nodeId, metricas, null);
            }
            
            // Log periódico del estado
            if (metricas.requestCount % 50 == 0) {
                System.out.printf("📊 [%s] Estado: CPU=%.1f%%, MEM=%.1f%%, REQ=%d%n",
                    nodeId, metricas.cpuUsage, metricas.memoryUsage, metricas.requestCount);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.printf("❌ [%s] Error notificando estado: %s%n", nodeId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public String obtenerNodeId(Current current) {
        return nodeId;
    }
    
    // ========== MÉTODOS PÚBLICOS ADICIONALES ==========
    
    public boolean necesitaEscalado() {
        return monitorRecursos.necesitaEscalado();
    }
    
    public boolean puedeReducirse() {
        return monitorRecursos.puedeReducirse();
    }
    
    public double getCargaTotal() {
        return monitorRecursos.getCargaTotal();
    }
    
    public AdministradorCandidatos getAdministradorCandidatos() {
        return administradorCandidatos;
    }
    
    public MonitorRecursos getMonitorRecursos() {
        return monitorRecursos;
    }
    
    public boolean isActiva() {
        return activa;
    }
    
    public void setActiva(boolean activa) {
        this.activa = activa;
        System.out.printf("🔄 [%s] Estado cambiado a: %s%n", nodeId, activa ? "ACTIVA" : "INACTIVA");
    }
    
    public void shutdown() {
        System.out.printf("🔄 [%s] Deteniendo réplica...%n", nodeId);
        
        activa = false;
        
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
        
        // Detener monitor de recursos
        if (monitorRecursos != null) {
            monitorRecursos.shutdown();
        }
        
        System.out.printf("✅ [%s] Réplica detenida%n", nodeId);
    }
    
    @Override
    public String toString() {
        return String.format("ReplicaNacional{nodeId='%s', activa=%s, carga=%.1f%%}", 
            nodeId, activa, getCargaTotal());
    }
} 