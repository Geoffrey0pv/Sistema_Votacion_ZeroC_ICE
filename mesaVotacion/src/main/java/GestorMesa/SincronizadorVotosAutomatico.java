package GestorMesa;

import Demo.*;
import com.zeroc.Ice.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sincronizador Automático de Votos
 * Envía votos nuevos al servidor regional cada 10 segundos
 */
public class SincronizadorVotosAutomatico {
    
    private final String mesaId;
    private final GestorVotosSQLite gestorVotosSQLite;
    private final Communicator communicator;
    private final ScheduledExecutorService scheduler;
    private IReceptorVotosRegionalPrx receptorVotosRegional;
    
    // Control de votos ya enviados
    private final AtomicLong ultimoVotoEnviado = new AtomicLong(0);
    private boolean activo = false;
    
    // Estadísticas
    private long totalVotosEnviados = 0;
    private long totalIntentosSincronizacion = 0;
    private long ultimaSincronizacionExitosa = 0;
    
    public SincronizadorVotosAutomatico(String mesaId, GestorVotosSQLite gestorVotosSQLite, Communicator communicator) {
        this.mesaId = mesaId;
        this.gestorVotosSQLite = gestorVotosSQLite;
        this.communicator = communicator;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SincronizadorVotos-Mesa-" + mesaId);
            t.setDaemon(true);
            return t;
        });
        
        System.out.println("🔄 Sincronizador Automático inicializado para Mesa " + mesaId);
    }
    
    /**
     * Inicia la sincronización automática cada 10 segundos
     */
    public boolean iniciar() {
        if (activo) {
            System.out.println("⚠️ El sincronizador ya está activo");
            return false;
        }
        
        // Conectar al servidor regional
        if (!conectarServidorRegional()) {
            System.err.println("❌ No se pudo conectar al servidor regional. Sincronización deshabilitada.");
            return false;
        }
        
        // Programar tarea de sincronización cada 10 segundos
        scheduler.scheduleAtFixedRate(this::sincronizarVotosNuevos, 5, 10, TimeUnit.SECONDS);
        activo = true;
        
        System.out.println("✅ Sincronización automática iniciada - cada 10 segundos");
        System.out.println("📡 Conectado a servidor regional para envío de votos");
        return true;
    }
    
    /**
     * Detiene la sincronización automática
     */
    public void detener() {
        if (!activo) {
            return;
        }
        
        activo = false;
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("🛑 Sincronización automática detenida");
        mostrarEstadisticasFinales();
    }
    
    /**
     * Conecta al servidor regional para envío de votos
     */
    private boolean conectarServidorRegional() {
        try {
            System.out.println("🔗 Conectando al servidor regional (receptorVotosRegional)...");
            
            // Intentar conexión directa al servidor regional
            String proxyString = "receptorVotosRegional:tcp -h localhost -p 8080";
            
            ObjectPrx base = communicator.stringToProxy(proxyString);
            if (base != null) {
                receptorVotosRegional = IReceptorVotosRegionalPrx.checkedCast(base);
                if (receptorVotosRegional != null) {
                    // Verificar conectividad
                    boolean servicio = receptorVotosRegional.verificarServicio();
                    if (servicio) {
                        System.out.println("✅ Conectado al servidor regional para sincronización");
                        return true;
                    }
                }
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error conectando al servidor regional: " + e.getMessage());
        }
        
        System.err.println("❌ No se pudo establecer conexión con el servidor regional");
        return false;
    }
    
    /**
     * Sincroniza solo los votos nuevos al servidor regional
     */
    private void sincronizarVotosNuevos() {
        if (!activo || receptorVotosRegional == null) {
            return;
        }
        
        try {
            totalIntentosSincronizacion++;
            
            // Obtener solo votos nuevos desde el último ID enviado
            long ultimoIdEnviado = ultimoVotoEnviado.get();
            List<VotoRegistro> votosNuevos = gestorVotosSQLite.obtenerVotosNuevosDesde(ultimoIdEnviado);
            
            if (votosNuevos.isEmpty()) {
                // No hay votos nuevos, no hacer nada
                return;
            }
            
            // Convertir a formato regional
            VotoRegional[] votosRegionales = convertirAVotosRegionales(votosNuevos);
            
            // Enviar votos al servidor regional
            System.out.println("📤 Sincronizando " + votosNuevos.size() + " votos nuevos...");
            
            ResultadoRecepcionVotos resultado = receptorVotosRegional.recibirListaVotos(votosRegionales);
            
            if (resultado.exito) {
                // Actualizar último voto enviado
                long maxId = votosNuevos.stream().mapToLong(v -> v.id).max().orElse(ultimoIdEnviado);
                ultimoVotoEnviado.set(maxId);
                
                totalVotosEnviados += resultado.votosGuardados;
                ultimaSincronizacionExitosa = System.currentTimeMillis();
                
                System.out.println("✅ Sincronización exitosa: " + resultado.votosGuardados + " votos enviados");
                
                if (resultado.votosRechazados > 0) {
                    System.out.println("⚠️ " + resultado.votosRechazados + " votos rechazados");
                }
                
            } else {
                System.err.println("❌ Error en sincronización: " + resultado.mensaje);
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error durante sincronización: " + e.getMessage());
            
            // Intentar reconectar si hay error de comunicación
            if (e instanceof LocalException) {
                System.out.println("🔄 Intentando reconectar al servidor regional...");
                conectarServidorRegional();
            }
        }
    }
    
    /**
     * Convierte votos locales a formato regional
     */
    private VotoRegional[] convertirAVotosRegionales(List<VotoRegistro> votosLocales) {
        VotoRegional[] votosRegionales = new VotoRegional[votosLocales.size()];
        
        for (int i = 0; i < votosLocales.size(); i++) {
            VotoRegistro votoLocal = votosLocales.get(i);
            
            VotoRegional votoRegional = new VotoRegional();
            votoRegional.idVoto = votoLocal.id;
            votoRegional.mesaId = votoLocal.mesaId;
            votoRegional.timestamp = votoLocal.timestamp;
            votoRegional.candidatoId = votoLocal.candidatoId;
            votoRegional.hashElector = votoLocal.hashVerificacion;
            votoRegional.municipio = votoLocal.municipio;
            votoRegional.departamento = votoLocal.departamento;
            votoRegional.estadoRegistro = "NUEVO";
            
            votosRegionales[i] = votoRegional;
        }
        
        return votosRegionales;
    }
    
    /**
     * Fuerza una sincronización inmediata
     */
    public boolean sincronizarAhora() {
        if (!activo || receptorVotosRegional == null) {
            System.err.println("❌ Sincronizador no está activo o no hay conexión");
            return false;
        }
        
        System.out.println("🔄 Forzando sincronización inmediata...");
        sincronizarVotosNuevos();
        return true;
    }
    
    /**
     * Obtiene estadísticas del sincronizador
     */
    public void mostrarEstadisticas() {
        System.out.println("\n📊 === ESTADÍSTICAS SINCRONIZACIÓN AUTOMÁTICA ===");
        System.out.println("📍 Mesa: " + mesaId);
        System.out.println("🔄 Estado: " + (activo ? "ACTIVO" : "INACTIVO"));
        System.out.println("📤 Total votos enviados: " + totalVotosEnviados);
        System.out.println("🔄 Total intentos sincronización: " + totalIntentosSincronizacion);
        System.out.println("📊 Último voto enviado ID: " + ultimoVotoEnviado.get());
        
        if (ultimaSincronizacionExitosa > 0) {
            LocalDateTime ultimaSync = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(ultimaSincronizacionExitosa),
                java.time.ZoneId.systemDefault());
            System.out.println("⏰ Última sincronización exitosa: " + 
                ultimaSync.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        } else {
            System.out.println("⏰ Última sincronización exitosa: Nunca");
        }
        
        System.out.println("🔗 Conexión servidor regional: " + (receptorVotosRegional != null ? "CONECTADO" : "DESCONECTADO"));
        System.out.println("═".repeat(50));
    }
    
    private void mostrarEstadisticasFinales() {
        System.out.println("\n📊 === RESUMEN FINAL SINCRONIZACIÓN ===");
        mostrarEstadisticas();
    }
    
    // Getters para monitoreo
    public boolean estaActivo() { return activo; }
    public long getTotalVotosEnviados() { return totalVotosEnviados; }
    public long getTotalIntentosSincronizacion() { return totalIntentosSincronizacion; }
    public long getUltimoVotoEnviado() { return ultimoVotoEnviado.get(); }
    public boolean estaConectado() { return receptorVotosRegional != null; }
} 