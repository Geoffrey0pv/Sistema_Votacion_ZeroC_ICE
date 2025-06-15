package servidorRegional;

import Demo.*;
import com.zeroc.Ice.*;

import java.lang.Exception;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GestionCandidatos implements ICargarCandidatos {
    private final Communicator communicator;
    private final List<Candidato> candidatos;
    private final List<String> endpointsMesas;

    public GestionCandidatos(Communicator communicator) {
        this.communicator = communicator;
        this.candidatos = new CopyOnWriteArrayList<>();
        this.endpointsMesas = new CopyOnWriteArrayList<>();

        // Cargar endpoints conocidos de las mesas (esto podría venir de configuración)
        cargarEndpointsMesas();
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
        System.out.println("Enviando candidatos a todas las mesas de votación...");

        if (candidatos.isEmpty()) {
            System.err.println("No hay candidatos para enviar");
            return false;
        }

        boolean todosExitosos = true;
        Candidato[] arrayCandidatos = candidatos.toArray(new Candidato[0]);

        for (String endpoint : endpointsMesas) {
            try {
                boolean resultado = enviarCandidatosAMesa(endpoint, arrayCandidatos);
                if (!resultado) {
                    todosExitosos = false;
                    System.err.println("Falló el envío a mesa con endpoint: " + endpoint);
                }
            } catch (Exception e) {
                System.err.println("Error enviando a mesa " + endpoint + ": " + e.getMessage());
                todosExitosos = false;
            }
        }

        System.out.println("Envío completado. Éxito: " + todosExitosos);
        return todosExitosos;
    }

    @Override
    public boolean enviarCandidatosAMesas(String endpointMesa, Current current) {
        System.out.println("Enviando candidatos a mesa específica: " + endpointMesa);

        if (candidatos.isEmpty()) {
            System.err.println("No hay candidatos para enviar");
            return false;
        }

        Candidato[] arrayCandidatos = candidatos.toArray(new Candidato[0]);
        return enviarCandidatosAMesa(endpointMesa, arrayCandidatos);
    }

    private boolean enviarCandidatosAMesa(String endpoint, Candidato[] candidatos) {
        try {
            // Crear proxy para la mesa de votación
            ObjectPrx base = communicator.stringToProxy("IceGrid/Query:tcp -h localhost -p 4061");

            // Obtener proxy de la mesa usando el endpoint
            ObjectPrx mesaProxy = communicator.stringToProxy(endpoint);
            IRecibirCandidatosPrx mesa = IRecibirCandidatosPrx.checkedCast(mesaProxy);

            if (mesa == null) {
                System.err.println("No se pudo conectar con la mesa: " + endpoint);
                return false;
            }

            // Crear callback para recibir confirmación
            ConfirmacionCallback callback = new ConfirmacionCallback();
            IConfirmacionCandidatosPrx callbackProxy =
                    IConfirmacionCandidatosPrx.uncheckedCast(
                            communicator.createObjectAdapter("CallbackAdapter").addWithUUID(callback)
                    );

            // Enviar candidatos a la mesa
            mesa.recibirCandidatos(candidatos, callbackProxy);

            // Esperar confirmación (implementación simplificada)
            Thread.sleep(1000);

            return callback.isExitoso();

        } catch (Exception e) {
            System.err.println("Error enviando candidatos a mesa " + endpoint + ": " + e.getMessage());
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