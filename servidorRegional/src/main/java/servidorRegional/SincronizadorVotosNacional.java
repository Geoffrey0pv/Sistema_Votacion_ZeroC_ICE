package servidorRegional;

import com.zeroc.Ice.Communicator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import Demo.*;

/**
 * Clase que maneja la sincronización automática de votos desde el servidor regional 
 * hacia el servidor nacional cada 10 segundos.
 */
public class SincronizadorVotosNacional {
    
    private final GestorVotosRegionalSQLite gestorVotos;
    private final Communicator communicator;
    private final ScheduledExecutorService scheduler;
    
    // Variables de control
    private Future<?> tareaSync;
    private volatile boolean sincronizacionActiva = false;
    
    // Estadísticas
    private final AtomicInteger totalSincronizaciones = new AtomicInteger(0);
    private final AtomicInteger sincronizacionesExitosas = new AtomicInteger(0);
    private final AtomicInteger sincronizacionesFallidas = new AtomicInteger(0);
    private final AtomicLong totalVotosSincronizados = new AtomicLong(0);
    private volatile String ultimaSincronizacion = "Nunca";
    private volatile String ultimoError = "Ninguno";
    
    // Configuración
    private static final int INTERVALO_SEGUNDOS = 10;
    private static final String ENDPOINT_SERVIDOR_NACIONAL = "RegistroVotos:tcp -h 10.147.17.113 -p 9090";
    
    public SincronizadorVotosNacional(GestorVotosRegionalSQLite gestorVotos, Communicator communicator) {
        this.gestorVotos = gestorVotos;
        this.communicator = communicator;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SincronizadorVotos");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Inicia la sincronización automática cada 10 segundos
     */
    public void iniciarSincronizacionAutomatica() {
        if (sincronizacionActiva) {
            System.out.println("⚠️  La sincronización automática ya está activa");
            return;
        }
        
        sincronizacionActiva = true;
        tareaSync = scheduler.scheduleWithFixedDelay(
            this::ejecutarSincronizacion,
            0, // Retraso inicial (empezar inmediatamente)
            INTERVALO_SEGUNDOS,
            TimeUnit.SECONDS
        );
        
        System.out.println("🔄 Sincronización automática iniciada (cada " + INTERVALO_SEGUNDOS + " segundos)");
    }
    
    /**
     * Detiene la sincronización automática
     */
    public void detenerSincronizacionAutomatica() {
        if (!sincronizacionActiva) {
            System.out.println("⚠️  La sincronización automática no está activa");
            return;
        }
        
        sincronizacionActiva = false;
        if (tareaSync != null) {
            tareaSync.cancel(false);
            tareaSync = null;
        }
        
        System.out.println("🛑 Sincronización automática detenida");
    }
    
    /**
     * Ejecuta una sincronización manual inmediata
     */
    public void sincronizarAhora() {
        System.out.println("🔄 Ejecutando sincronización manual...");
        ejecutarSincronizacion();
    }
    
    /**
     * Obtiene el estado actual de la sincronización
     */
    public String obtenerEstado() {
        StringBuilder estado = new StringBuilder();
        estado.append("━━━ ESTADO DE SINCRONIZACIÓN ━━━\n");
        estado.append("Estado: ").append(sincronizacionActiva ? "🟢 ACTIVA" : "🔴 INACTIVA").append("\n");
        estado.append("Intervalo: ").append(INTERVALO_SEGUNDOS).append(" segundos\n");
        estado.append("Endpoint nacional: ").append(ENDPOINT_SERVIDOR_NACIONAL).append("\n");
        estado.append("━━━ ESTADÍSTICAS ━━━\n");
        estado.append("Total sincronizaciones: ").append(totalSincronizaciones.get()).append("\n");
        estado.append("Exitosas: ").append(sincronizacionesExitosas.get()).append("\n");
        estado.append("Fallidas: ").append(sincronizacionesFallidas.get()).append("\n");
        estado.append("Votos sincronizados: ").append(totalVotosSincronizados.get()).append("\n");
        estado.append("Última sincronización: ").append(ultimaSincronizacion).append("\n");
        estado.append("Último error: ").append(ultimoError).append("\n");
        
        return estado.toString();
    }
    
    /**
     * Lógica principal de sincronización
     */
    private void ejecutarSincronizacion() {
        try {
            totalSincronizaciones.incrementAndGet();
            
            // Obtener votos no sincronizados
            List<VotoRegional> votosPendientes = gestorVotos.obtenerVotosNoSincronizados(50);
            
            if (votosPendientes.isEmpty()) {
                // No hay votos pendientes, sincronización exitosa pero sin datos
                sincronizacionesExitosas.incrementAndGet();
                ultimaSincronizacion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                System.out.println("✅ Sincronización completada: No hay votos pendientes");
                return;
            }
            
            System.out.println("🔄 Sincronizando " + votosPendientes.size() + " votos al servidor nacional...");
            
            // Conectar al servidor nacional
            IRegistroVotosPrx registroVotos = null;
            try {
                registroVotos = IRegistroVotosPrx.checkedCast(
                    communicator.stringToProxy(ENDPOINT_SERVIDOR_NACIONAL)
                );
            } catch (Exception e) {
                throw new RuntimeException("Error al crear proxy: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
            
            if (registroVotos == null) {
                throw new RuntimeException("No se pudo conectar al servidor nacional en: " + ENDPOINT_SERVIDOR_NACIONAL);
            }
            
            // Convertir votos regionales a votos completos
            VotoCompleto[] votosCompletos = convertirVotosRegionalesACompletos(votosPendientes);
            
            // Enviar votos al servidor nacional
            ResultadoRegistroVotos resultado = null;
            try {
                resultado = registroVotos.registrarVotosLote(votosCompletos);
            } catch (Exception e) {
                throw new RuntimeException("Error al enviar votos: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
            
            if (resultado != null && resultado.exito) {
                // Marcar votos como sincronizados
                List<Long> idsVotos = votosPendientes.stream()
                    .map(v -> v.idVoto)
                    .collect(java.util.stream.Collectors.toList());
                gestorVotos.marcarVotosSincronizados(idsVotos);
                
                // Actualizar estadísticas
                totalVotosSincronizados.addAndGet(resultado.votosRegistrados);
                sincronizacionesExitosas.incrementAndGet();
                ultimaSincronizacion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                ultimoError = "Ninguno";
                
                System.out.println("✅ Sincronización completada: " + resultado.votosRegistrados + " votos enviados al servidor nacional");
                
                if (resultado.votosRechazados > 0) {
                    System.out.println("⚠️ " + resultado.votosRechazados + " votos rechazados");
                }
            } else {
                String mensajeError = "Respuesta inválida del servidor nacional";
                if (resultado != null && resultado.mensaje != null) {
                    mensajeError = resultado.mensaje;
                }
                throw new RuntimeException(mensajeError);
            }
            
        } catch (Exception e) {
            sincronizacionesFallidas.incrementAndGet();
            String mensajeError = e.getMessage();
            if (mensajeError == null || mensajeError.trim().isEmpty()) {
                mensajeError = "Error desconocido: " + e.getClass().getSimpleName();
            }
            ultimoError = mensajeError;
            System.err.println("❌ Error en sincronización: " + mensajeError);
            
            // Debug adicional para problemas de conexión
            if (e instanceof com.zeroc.Ice.ConnectFailedException) {
                System.err.println("🔌 Problema de conectividad con servidor nacional");
                System.err.println("   Endpoint: " + ENDPOINT_SERVIDOR_NACIONAL);
            } else if (e instanceof com.zeroc.Ice.ObjectNotExistException) {
                System.err.println("🚫 Servicio no encontrado en servidor nacional");
                System.err.println("   Verifique que IRegistroVotos esté disponible");
            }
        }
    }
    
    /**
     * Convierte votos regionales al formato del servidor nacional
     */
    private VotoCompleto[] convertirVotosRegionalesACompletos(List<VotoRegional> votosRegionales) {
        VotoCompleto[] votosCompletos = new VotoCompleto[votosRegionales.size()];
        
        for (int i = 0; i < votosRegionales.size(); i++) {
            VotoRegional votoRegional = votosRegionales.get(i);
            VotoCompleto votoCompleto = new VotoCompleto();
            
            // El ID será generado automáticamente por PostgreSQL BIGSERIAL
            // No necesitamos establecer votoCompleto.id
            votoCompleto.mesaId = votoRegional.mesaId;
            votoCompleto.timestamp = votoRegional.timestamp;
            votoCompleto.candidatoId = votoRegional.candidatoId;
            votoCompleto.hashVerificacion = votoRegional.hashElector;
            votoCompleto.municipio = votoRegional.municipio;
            votoCompleto.departamento = votoRegional.departamento;
            
            votosCompletos[i] = votoCompleto;
        }
        
        return votosCompletos;
    }
    
    /**
     * Verifica si la sincronización está activa
     */
    public boolean estaSincronizacionActiva() {
        return sincronizacionActiva;
    }
    
    /**
     * Cierra el sincronizador y libera recursos
     */
    public void cerrar() {
        detenerSincronizacionAutomatica();
        if (!scheduler.isShutdown()) {
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
    }
} 