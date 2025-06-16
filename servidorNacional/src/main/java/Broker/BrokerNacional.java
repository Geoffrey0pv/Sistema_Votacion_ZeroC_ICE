package Broker;

import Demo.*;
import Services.CandidatosService;
import ConsultaCandidatos.ConsultaCandidatosImpl;
import Models.CandidatoModel;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Communicator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

public class BrokerNacional implements IBrokerNacional {
    
    private final MonitorRecursos monitorMaster;
    private final BalanceadorCarga balanceador;
    private final GestorReplicas gestorReplicas;
    private final ScheduledExecutorService scheduler;
    private final Communicator communicator;
    private final CandidatosService candidatosService;
    private final ConsultaCandidatosImpl consultaCandidatos;
    
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
        this.candidatosService = new CandidatosService();
        this.consultaCandidatos = new ConsultaCandidatosImpl();
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
                escaladoAutomatico();
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
            CandidatoElectoral[] candidatos = consultaCandidatos.obtenerTodosCandidatosElectorales(null);
            if (candidatos.length > 0) {
                // Convertir CandidatoElectoral[] a Candidato[] para sincronización
                Candidato[] candidatosParaSincronizar = new Candidato[candidatos.length];
                for (int i = 0; i < candidatos.length; i++) {
                    candidatosParaSincronizar[i] = new Candidato();
                    candidatosParaSincronizar[i].idCandidato = candidatos[i].id;
                    candidatosParaSincronizar[i].nombre = candidatos[i].nombre;
                    candidatosParaSincronizar[i].partido = candidatos[i].partido;
                }
                gestorReplicas.sincronizarTodasReplicas(candidatosParaSincronizar);
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
        
        try {
            java.io.File archivo = new java.io.File(rutaArchivo);
            if (!archivo.exists()) {
                System.err.println("❌ Archivo CSV no encontrado: " + rutaArchivo);
                return false;
            }
            
            List<CandidatoModel> candidatos = candidatosService.cargarCandidatosDesdeCSV(archivo);
            boolean resultado = candidatosService.guardarCandidatos(candidatos) > 0;
            
            if (resultado) {
                // Sincronizar con réplicas de forma asíncrona
                scheduler.execute(() -> sincronizarReplicas());
            }
            return resultado;
        } catch (Exception e) {
            System.err.println("❌ Error cargando candidatos desde CSV: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean cargarCandidatosDesdeArray(Candidato[] candidatos, Current current) {
        monitorMaster.incrementarRequests();
        
        try {
            // Convertir Candidato[] a CandidatoModel[]
            java.util.List<CandidatoModel> candidatosModel = new java.util.ArrayList<>();
            for (Candidato candidato : candidatos) {
                candidatosModel.add(new CandidatoModel(candidato.idCandidato, candidato.nombre, candidato.partido));
            }
            
            boolean resultado = candidatosService.guardarCandidatos(candidatosModel) > 0;
            if (resultado) {
                // Sincronizar con réplicas de forma asíncrona
                scheduler.execute(() -> sincronizarReplicas());
            }
            return resultado;
        } catch (Exception e) {
            System.err.println("❌ Error cargando candidatos desde array: " + e.getMessage());
            return false;
        }
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
        return (int) consultaCandidatos.contarCandidatos(current);
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
        CandidatoElectoral[] candidatosElectorales = consultaCandidatos.obtenerTodosCandidatosElectorales(current);
        
        // Convertir CandidatoElectoral[] a Candidato[]
        Candidato[] candidatos = new Candidato[candidatosElectorales.length];
        for (int i = 0; i < candidatosElectorales.length; i++) {
            candidatos[i] = new Candidato();
            candidatos[i].idCandidato = candidatosElectorales[i].id;
            candidatos[i].nombre = candidatosElectorales[i].nombre;
            candidatos[i].partido = candidatosElectorales[i].partido;
        }
        
        return candidatos;
    }
    
    @Override
    public boolean limpiarCandidatos(Current current) {
        monitorMaster.incrementarRequests();
        
        boolean resultado = candidatosService.eliminarTodosLosCandidatos();
        if (resultado) {
            // Sincronizar con réplicas
            scheduler.execute(() -> sincronizarReplicas());
        }
        return resultado;
    }
    
    @Override
    public boolean enviarCandidatosARegional(String endpointRegional, Current current) {
        monitorMaster.incrementarRequests();
        
        try {
            // Obtener candidatos
            Candidato[] candidatos = obtenerTodosCandidatos(current);
            if (candidatos.length == 0) {
                System.err.println("❌ No hay candidatos para enviar");
                return false;
            }
            
            // Crear proxy al servidor regional
            com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy(endpointRegional);
            ICargarCandidatosPrx cargarCandidatos = ICargarCandidatosPrx.checkedCast(base);
            
            if (cargarCandidatos == null) {
                System.err.println("❌ No se pudo conectar al servidor regional: " + endpointRegional);
                return false;
            }
            
            // Enviar candidatos
            boolean resultado = cargarCandidatos.enviarCandidatosATodasMesas();
            System.out.println(resultado ? "✅ Candidatos enviados al regional" : "❌ Error enviando candidatos al regional");
            return resultado;
            
        } catch (Exception e) {
            System.err.println("❌ Error enviando candidatos a regional: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean enviarCandidatosATodosRegionales(Current current) {
        monitorMaster.incrementarRequests();
        
        // TODO: Implementar envío a todos los regionales conocidos
        System.out.println("⚠️ enviarCandidatosATodosRegionales no implementado completamente");
        return false;
    }

    // ========== MÉTODOS DE IBrokerNacional ==========
    
    /**
     * Registra una nueva réplica en el sistema
     */
    @Override
    public boolean registrarReplica(String nodeId, String endpoint, Current current) {
        try {
            System.out.printf("📝 Registrando réplica: %s -> %s%n", nodeId, endpoint);
            
            // Crear la réplica usando el gestor
            boolean creada = gestorReplicas.crearReplica(nodeId, endpoint, current);
            
            if (creada) {
                System.out.printf("✅ Réplica registrada exitosamente: %s%n", nodeId);
                mostrarEstadoBroker();
                return true;
            } else {
                System.err.printf("❌ Error registrando réplica: %s%n", nodeId);
                return false;
            }
            
        } catch (Exception e) {
            System.err.printf("❌ Error en registro de réplica %s: %s%n", nodeId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Desregistra una réplica del sistema
     */
    @Override
    public boolean desregistrarReplica(String nodeId, Current current) {
        try {
            System.out.printf("🗑️ Desregistrando réplica: %s%n", nodeId);
            
            boolean eliminada = gestorReplicas.eliminarReplica(nodeId, current);
            
            if (eliminada) {
                System.out.printf("✅ Réplica desregistrada: %s%n", nodeId);
                mostrarEstadoBroker();
                return true;
            } else {
                System.err.printf("⚠️ Réplica no encontrada para desregistrar: %s%n", nodeId);
                return false;
            }
            
        } catch (Exception e) {
            System.err.printf("❌ Error desregistrando réplica %s: %s%n", nodeId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene métricas globales del broker
     */
    @Override
    public MetricasRecursos obtenerMetricasGlobales(Current current) {
        return monitorMaster.obtenerMetricas(current);
    }
    
    /**
     * Obtiene la lista de réplicas disponibles
     */
    @Override
    public InfoReplica[] obtenerReplicasDisponibles(Current current) {
        try {
            return gestorReplicas.obtenerReplicasActivas(current);
        } catch (Exception e) {
            System.err.printf("❌ Error obteniendo réplicas disponibles: %s%n", e.getMessage());
            return new InfoReplica[0];
        }
    }
    
    /**
     * Obtiene el endpoint óptimo para balanceo de carga
     */
    @Override
    public String obtenerEndpointOptimo(Current current) {
        return balanceador.obtenerMejorReplica(current);
    }
    
    /**
     * Ejecuta escalado automático
     */
    @Override
    public boolean escalarAutomaticamente(Current current) {
        return escaladoAutomatico();
    }
    
    /**
     * Reduce el número de réplicas
     */
    @Override
    public boolean reducirReplicas(Current current) {
        try {
            InfoReplica[] replicas = gestorReplicas.obtenerReplicasActivas(current);
            if (replicas.length <= 1) {
                System.out.println("⚠️ No se pueden reducir más réplicas (mínimo 1)");
                return false;
            }
            
            // Eliminar la réplica menos cargada
            eliminarReplicaMenosUsada();
            return true;
            
        } catch (Exception e) {
            System.err.printf("❌ Error reduciendo réplicas: %s%n", e.getMessage());
            return false;
        }
    }
    
    // Métodos internos de escalado
    public boolean escaladoAutomatico() {
        if (escaladoEnProceso.compareAndSet(false, true)) {
            try {
                System.out.println("🚀 Iniciando escalado automático...");
                
                // Determinar cuántas réplicas crear
                int replicasActuales = gestorReplicas.getCantidadReplicasActivas();
                int nuevasReplicas = Math.min(2, MAX_REPLICAS - replicasActuales); // Crear máximo 2 réplicas por vez
                
                if (nuevasReplicas <= 0) {
                    System.out.println("⚠️ No se pueden crear más réplicas (límite alcanzado)");
                    return false;
                }
                
                boolean exito = true;
                for (int i = 0; i < nuevasReplicas; i++) {
                    String nodeId = "replica-auto-" + System.currentTimeMillis() + "-" + i;
                    if (!gestorReplicas.crearReplica(nodeId, null, null)) {
                        System.err.println("❌ Error creando réplica: " + nodeId);
                        exito = false;
                    } else {
                        System.out.println("✅ Réplica creada: " + nodeId);
                    }
                }
                
                if (exito) {
                    // Sincronizar datos con las nuevas réplicas
                    scheduler.schedule(() -> sincronizarReplicas(), 5, TimeUnit.SECONDS);
                }
                
                System.out.printf("🎯 Escalado completado: %d réplicas creadas%n", nuevasReplicas);
                return exito;
                
            } finally {
                escaladoEnProceso.set(false);
            }
        } else {
            System.out.println("⚠️ Escalado ya en proceso, ignorando solicitud");
            return false;
        }
    }
    
    public boolean reducirReplicas() {
        if (escaladoEnProceso.compareAndSet(false, true)) {
            try {
                System.out.println("📉 Iniciando reducción de réplicas...");
                
                int replicasActuales = gestorReplicas.getCantidadReplicasActivas();
                int replicasAEliminar = Math.min(1, replicasActuales - MIN_REPLICAS); // Eliminar máximo 1 por vez
                
                if (replicasAEliminar <= 0) {
                    System.out.println("⚠️ No se pueden eliminar más réplicas (mínimo alcanzado)");
                    return false;
                }
                
                // Eliminar réplicas una por una
                boolean exito = true;
                for (int i = 0; i < replicasAEliminar; i++) {
                    eliminarReplicaMenosUsada();
                }
                
                System.out.printf("🎯 Reducción completada: %d réplicas eliminadas%n", replicasAEliminar);
                return exito;
                
            } finally {
                escaladoEnProceso.set(false);
            }
        } else {
            System.out.println("⚠️ Escalado en proceso, ignorando solicitud de reducción");
            return false;
        }
    }
    
    // Getters para acceso a componentes internos
    public CandidatosService getCandidatosService() {
        return candidatosService;
    }
    
    public ConsultaCandidatosImpl getConsultaCandidatos() {
        return consultaCandidatos;
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
        System.out.println("🛑 Cerrando Broker Nacional...");
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        if (gestorReplicas != null) {
            gestorReplicas.shutdown();
        }
        
        if (candidatosService != null) {
            // CandidatosService no tiene método shutdown, pero podríamos agregarlo si es necesario
        }
        
        if (consultaCandidatos != null) {
            consultaCandidatos.shutdown();
        }
        
        System.out.println("✅ Broker Nacional cerrado");
    }

    /**
     * Crea una nueva réplica automáticamente
     */
    private boolean crearReplica() {
        try {
            if (!gestorReplicas.puedeCrearMasReplicas()) {
                System.err.println("❌ No se pueden crear más réplicas (límite alcanzado)");
                return false;
            }
            
            boolean creada = gestorReplicas.crearReplicaAutomatica();
            
            if (creada) {
                System.out.println("✅ Nueva réplica creada automáticamente");
                mostrarEstadoBroker();
                return true;
            } else {
                System.err.println("❌ Error creando réplica automática");
                return false;
            }
            
        } catch (Exception e) {
            System.err.printf("❌ Error en creación automática de réplica: %s%n", e.getMessage());
            return false;
        }
    }

    /**
     * Elimina la réplica menos utilizada
     */
    private void eliminarReplicaMenosUsada() {
        try {
            InfoReplica[] replicas = gestorReplicas.obtenerReplicasActivas(null);
            
            if (replicas.length <= 1) {
                System.out.println("⚠️ No se puede eliminar más réplicas (mínimo 1)");
                return;
            }
            
            // Encontrar la réplica con menor carga
            InfoReplica menosUsada = replicas[0];
            double menorCarga = Double.MAX_VALUE;
            
            for (InfoReplica replica : replicas) {
                if (replica.metricas != null) {
                    double carga = replica.metricas.cpuUsage + replica.metricas.memoryUsage;
                    if (carga < menorCarga) {
                        menorCarga = carga;
                        menosUsada = replica;
                    }
                }
            }
            
            if (gestorReplicas.eliminarReplica(menosUsada.nodeId, null)) {
                System.out.printf("🗑️ Réplica menos usada eliminada: %s (carga: %.1f%%)%n", 
                    menosUsada.nodeId, menorCarga);
                mostrarEstadoBroker();
            }
            
        } catch (Exception e) {
            System.err.printf("❌ Error eliminando réplica menos usada: %s%n", e.getMessage());
        }
    }
    
    /**
     * Muestra el estado actual del broker
     */
    private void mostrarEstadoBroker() {
        try {
            System.out.println("\n🏢 Estado del Broker Nacional:");
            
            // Métricas generales
            MetricasRecursos metricas = monitorMaster.obtenerMetricas(null);
            System.out.printf("   CPU: %.1f%% | Memoria: %.1f%% | Requests: %d%n",
                metricas.cpuUsage, metricas.memoryUsage, metricas.requestCount);
            
            // Estado de réplicas
            InfoReplica[] replicas = gestorReplicas.obtenerReplicasActivas(null);
            System.out.printf("   Réplicas activas: %d | Total gestionadas: %d%n",
                replicas.length, gestorReplicas.getCantidadReplicas());
            
            // Endpoint óptimo
            String endpointOptimo = balanceador.obtenerMejorReplica(null);
            System.out.printf("   Endpoint óptimo: %s%n", endpointOptimo);
            
            System.out.println();
            
        } catch (Exception e) {
            System.err.printf("❌ Error mostrando estado del broker: %s%n", e.getMessage());
        }
    }
} 