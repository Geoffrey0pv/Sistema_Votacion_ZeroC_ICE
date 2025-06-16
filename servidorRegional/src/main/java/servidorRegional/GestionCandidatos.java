package servidorRegional;

import Demo.*;
import com.zeroc.Ice.*;
import java.io.InputStream;
import java.util.Properties;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class GestionCandidatos implements ICargarCandidatos {
    private final Communicator communicator;
    private final List<Candidato> candidatos;
    private final List<String> endpointsMesas;
    private IAdministradorCandidatosPrx adminCandidatosNacional;
    private ObjectAdapter callbackAdapter;
    private Properties config;
    private final AtomicBoolean actualizandoCandidatos = new AtomicBoolean(false);

    public GestionCandidatos(Communicator communicator) {
        this.communicator = communicator;
        this.candidatos = new CopyOnWriteArrayList<>();
        this.endpointsMesas = new CopyOnWriteArrayList<>();
        cargarConfiguracion();

        // Crear adaptador para callbacks una sola vez
        try {
            this.callbackAdapter = communicator.createObjectAdapter("");
            this.callbackAdapter.activate();
        } catch (Exception e) {
            System.err.println("❌ Error creando adaptador para callbacks: " + e.getMessage());
        }

        conectarYNuevoIntento();

        // Cargar endpoints conocidos de las mesas (esto podría venir de configuración)
        cargarEndpointsMesas();
    }

    private void cargarConfiguracion() {
        config = new Properties();
        try (InputStream input = GestionCandidatos.class.getClassLoader().getResourceAsStream("regional.properties")) {
            if (input == null) {
                System.err.println("No se pudo encontrar regional.properties");
                return;
            }
            config.load(input);
            System.out.println("Configuración cargada correctamente");
        } catch (Exception e) {
            System.err.println("Error cargando configuración: " + e.getMessage());
        }
    }

    private void conectarYNuevoIntento() {
        new Thread(() -> {
            try {
                ObjectPrx base = communicator.stringToProxy("AdministradorCandidatosNacional@ServidorNacionalAdapter");
                adminCandidatosNacional = IAdministradorCandidatosPrx.checkedCast(base);
                
                if (adminCandidatosNacional == null) {
                    throw new Error("Proxy nulo para Administrador de Candidatos Nacional");
                }
                
                System.out.println("✅ Conexión establecida con el Administrador de Candidatos Nacional");
                actualizarCandidatosDesdeNacional();
                
            } catch (Exception e) {
                System.err.println("❌ Error conectando con el Servidor Nacional: " + e.getMessage());
                
                try {
                    System.out.println("🔄 Reintentando conexión con Servidor Nacional en 5 segundos...");
                    Thread.sleep(5000);
                    conectarYNuevoIntento();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void actualizarCandidatosDesdeNacional() {
        if (!actualizandoCandidatos.compareAndSet(false, true)) {
            System.out.println("🔄 Actualización de candidatos ya en progreso.");
            return;
        }

        try {
            if (adminCandidatosNacional != null) {
                System.out.println("🔄 Solicitando candidatos al Servidor Nacional...");
                Candidato[] candidatosNacionales = adminCandidatosNacional.obtenerTodosCandidatos();
                if (candidatosNacionales != null && candidatosNacionales.length > 0) {
                    this.candidatos.clear();
                    for (Candidato c : candidatosNacionales) {
                        this.candidatos.add(c);
                    }
                    System.out.println("✅ Candidatos actualizados desde el Servidor Nacional: " + this.candidatos.size());
                } else {
                    System.err.println("⚠️ No se obtuvieron candidatos del Servidor Nacional (lista vacía).");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo candidatos del Servidor Nacional: " + e.getMessage());
        } finally {
            actualizandoCandidatos.set(false);
        }
    }

    private void cargarEndpointsMesas() {
        // Aquí cargarías los endpoints desde configuración o registro
        // Por ejemplo:
        endpointsMesas.add("Mesa1:tcp -h 192.168.1.10 -p 10001");
        endpointsMesas.add("Mesa2:tcp -h 192.168.1.11 -p 10001");
        endpointsMesas.add("Mesa3:tcp -h 192.168.1.12 -p 10001");
    }

    @Override
    public boolean enviarCandidatosATodasMesas(Current current) {
        if (candidatos.isEmpty()) {
            System.err.println("⚠️ No hay candidatos para enviar, intentando actualizar desde el nacional...");
            actualizarCandidatosDesdeNacional();
            return false;
        }
        
        System.out.println("Enviando candidatos a todas las mesas registradas (lógica no implementada).");
        return true;
    }

    @Override
    public boolean enviarCandidatosAMesas(String endpointMesa, Current current) {
        System.out.println("Enviando candidatos a mesa específica: " + endpointMesa);

        if (candidatos.isEmpty()) {
            System.err.println("⚠️ No hay candidatos locales, solicitando al servidor nacional antes de continuar...");
            actualizarCandidatosDesdeNacional();
            
            try {
                // Damos un momento para que la actualización asíncrona termine
                Thread.sleep(3000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (candidatos.isEmpty()) {
                System.err.println("❌ Fallo definitivo: No se pudieron obtener candidatos del servidor nacional.");
                return false;
            }
        }

        Candidato[] arrayCandidatos = candidatos.toArray(new Candidato[0]);
        return enviarCandidatosAMesa(endpointMesa, arrayCandidatos);
    }

    private boolean enviarCandidatosAMesa(String endpoint, Candidato[] candidatos) {
        try {
            System.out.println("Enviando " + candidatos.length + " candidatos a: " + endpoint);
            
            ObjectPrx mesaBase = communicator.stringToProxy(endpoint);
            IRecibirCandidatosPrx mesa = IRecibirCandidatosPrx.checkedCast(mesaBase);

            if (mesa == null) {
                System.err.println("❌ No se pudo obtener un proxy para la mesa en el endpoint: " + endpoint);
                return false;
            }

            ConfirmacionCallback callback = new ConfirmacionCallback();
            IConfirmacionCandidatosPrx callbackProxy = IConfirmacionCandidatosPrx.uncheckedCast(
                callbackAdapter.addWithUUID(callback)
            );

            mesa.recibirCandidatos(candidatos, callbackProxy);
            System.out.println("✅ Petición de candidatos enviada a " + endpoint);
            
            return true; 
        } catch (Exception e) {
            System.err.println("❌ Error enviando candidatos a mesa " + endpoint + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Métodos para gestionar la lista de candidatos
    public void actualizarCandidatos(Candidato[] nuevosCandidatos) {
        candidatos.clear();
        for (Candidato candidato : nuevosCandidatos) {
            candidatos.add(candidato);
        }
        System.out.println("Lista de candidatos actualizada. Total: " + candidatos.size());
    }

    public void agregarEndpointMesa(String endpoint) {
        if (!endpointsMesas.contains(endpoint)) {
            endpointsMesas.add(endpoint);
            System.out.println("Nuevo endpoint de mesa agregado: " + endpoint);
        }
    }

    public void removerEndpointMesa(String endpoint) {
        endpointsMesas.remove(endpoint);
        System.out.println("Endpoint de mesa removido: " + endpoint);
    }

    public List<String> obtenerEndpointsMesas() {
        return new ArrayList<>(endpointsMesas);
    }

    public int obtenerCantidadCandidatos() {
        return candidatos.size();
    }

    // Clase interna para manejar callbacks de confirmación
    private static class ConfirmacionCallback implements IConfirmacionCandidatos {
        private boolean exitoso = false;
        private String mensaje = "";

        @Override
        public void recibirConfirmacion(boolean ok, String mensaje, Current current) {
            this.exitoso = ok;
            this.mensaje = mensaje;
            System.out.println("Confirmación recibida - Éxito: " + ok + ", Mensaje: " + mensaje);
        }

        public boolean isExitoso() {
            return exitoso;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}