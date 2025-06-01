package messaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zeroc.Ice.ObjectAdapter;
import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import GestorVotos.VotoImp;
import Demo.IConfirmacionVotoPrx;
import Demo.IRegistrarVotoPrx;
import messaging.utils.LocalDateTimeAdapter;

public class ReliableMessageManager {
    private static final String MENSAJES_FILE = "mensajes_pendientes.json";
    private final Gson gson;
    private final List<MensajePendiente> mensajesPendientes;
    private final ScheduledExecutorService scheduler;
    private final Object lock = new Object();
    
    public ReliableMessageManager() {
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();
        this.mensajesPendientes = new ArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        cargarMensajesPendientes();
        iniciarMonitoreoReenvio();
    }
    
    /**
     * Guarda un voto para envío posterior cuando haya conectividad
     */
    public void guardarVotoPendiente(VotoImp voto) {
        synchronized (lock) {
            MensajePendiente mensaje = new MensajePendiente(voto);
            mensajesPendientes.add(mensaje);
            persistirMensajes();
            
            System.out.println("✓ Voto guardado para envío posterior - ID: " + mensaje.getId());
        }
    }
    
    /**
     * Intenta enviar todos los mensajes pendientes
     */
    public void procesarMensajesPendientes(IRegistrarVotoPrx servidor, 
                                         ObjectAdapter adapter, 
                                         com.zeroc.Ice.Communicator communicator) {
        if (servidor == null) return;
        
        synchronized (lock) {
            Iterator<MensajePendiente> iterator = mensajesPendientes.iterator();
            
            while (iterator.hasNext()) {
                MensajePendiente mensaje = iterator.next();
                
                if (!mensaje.puedeReintentar()) {
                    if (mensaje.getIntentos() >= mensaje.getMaxIntentos()) {
                        mensaje.setEstado("FALLIDO");
                        System.err.println("✗ Mensaje " + mensaje.getId() + 
                                         " falló después de " + mensaje.getMaxIntentos() + " intentos");
                    }
                    iterator.remove();
                    continue;
                }
                
                try {
                    mensaje.incrementarIntentos();
                    
                    // Crear callback para el reenvío
                    IConfirmacionVotoPrx callback = crearCallbackReenvio(adapter, communicator, mensaje.getId());
                    
                    // Intentar enviar el voto
                    servidor.enviarVoto(mensaje.getVoto(), callback);
                    
                    mensaje.setEstado("ENVIADO");
                    System.out.println("✓ Voto reenviado exitosamente - ID: " + mensaje.getId() + 
                                     " (Intento " + mensaje.getIntentos() + ")");
                    
                    iterator.remove(); // Remover mensaje enviado exitosamente
                    
                } catch (Exception e) {
                    System.err.println("✗ Error reenviando mensaje " + mensaje.getId() + 
                                     " (Intento " + mensaje.getIntentos() + "): " + e.getMessage());
                    
                    if (!mensaje.puedeReintentar()) {
                        mensaje.setEstado("FALLIDO");
                        iterator.remove();
                    }
                }
            }
            
            persistirMensajes();
        }
    }
    
    /**
     * Verifica si hay mensajes pendientes
     */
    public boolean hayMensajesPendientes() {
        synchronized (lock) {
            return mensajesPendientes.stream()
                    .anyMatch(m -> "PENDIENTE".equals(m.getEstado()));
        }
    }
    
    /**
     * Obtiene estadísticas de mensajes
     */
    public void mostrarEstadisticas() {
        synchronized (lock) {
            long pendientes = mensajesPendientes.stream()
                    .filter(m -> "PENDIENTE".equals(m.getEstado())).count();
            
            if (pendientes > 0) {
                System.out.println("\n📊 MENSAJES PENDIENTES: " + pendientes);
                System.out.println("   (Se reenviarán automáticamente cuando haya conectividad)");
            }
        }
    }
    
    private void cargarMensajesPendientes() {
        File file = new File(MENSAJES_FILE);
        if (!file.exists()) return;
        
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<MensajePendiente>>(){}.getType();
            List<MensajePendiente> mensajes = gson.fromJson(reader, listType);
            
            if (mensajes != null) {
                synchronized (lock) {
                    mensajesPendientes.addAll(mensajes);
                    System.out.println("📂 Cargados " + mensajes.size() + " mensajes pendientes");
                }
            }
        } catch (Exception e) {
            System.err.println("Error cargando mensajes pendientes: " + e.getMessage());
        }
    }
    
    private void persistirMensajes() {
        try (FileWriter writer = new FileWriter(MENSAJES_FILE)) {
            gson.toJson(mensajesPendientes, writer);
        } catch (IOException e) {
            System.err.println("Error persistiendo mensajes: " + e.getMessage());
        }
    }
    
    private void iniciarMonitoreoReenvio() {
        // Verificar cada 30 segundos si hay mensajes para reenviar
        scheduler.scheduleWithFixedDelay(() -> {
            if (hayMensajesPendientes()) {
                System.out.println("🔄 Verificando mensajes pendientes...");
                // Aquí podrías intentar reconectar y reenviar automáticamente
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    private IConfirmacionVotoPrx crearCallbackReenvio(ObjectAdapter adapter,
                                                     com.zeroc.Ice.Communicator communicator,
                                                     String mensajeId) {
        try {
            if (adapter != null) {
                ConfirmacionVotoReenvio confirmacionImpl = new ConfirmacionVotoReenvio(mensajeId);
                com.zeroc.Ice.ObjectPrx obj = adapter.addWithUUID(confirmacionImpl);
                return IConfirmacionVotoPrx.uncheckedCast(obj);
            }
        } catch (Exception e) {
            System.err.println("Error creando callback de reenvío: " + e.getMessage());
        }
        return null;
    }
    
    public void shutdown() {
        scheduler.shutdown();
        persistirMensajes();
    }
}