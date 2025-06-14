package GestorMesa;

import Demo.*;
import GestorVotos.VotoImp;
import ReliableMessageManager.ReliableMessageManager;
import GestorVotos.GestorVotos;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Communicator;
import java.util.List;
import java.util.ArrayList;

public class GestorMesa {
    private ReliableMessageManager messageManager;
    private IRegistrarVotoPrx servidorRegional;
    private ICargarCandidatosPrx brokerCandidatos; // Simulado - vendrá de otro device
    private ObjectAdapter adapter;
    private Communicator communicator;
    private com.zeroc.IceGrid.QueryPrx query;
    private String idMesa;
    private List<Candidato> candidatosDisponibles;
    private List<String> electoresYaVotaron; // Simulado - vendrá de servicio de validación

    public GestorMesa(String idMesa) {
        this.idMesa = idMesa;
        this.candidatosDisponibles = new ArrayList<>();
        this.electoresYaVotaron = new ArrayList<>();
        this.messageManager = new ReliableMessageManager();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🔄 Guardando mensajes pendientes...");
            if (messageManager != null) {
                messageManager.shutdown();
            }
        }));
    }

    public boolean inicializar(Communicator communicator) {
        this.communicator = communicator;

        try {
            this.adapter = communicator.createObjectAdapter("MesaCallbackAdapter");
            adapter.activate();

            this.query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                    communicator.stringToProxy("DemoIceGrid/Query"));

            this.servidorRegional = obtenerServidorRegional();

            // TODO: Conectar al broker de candidatos (viene de otro device)
            // this.brokerCandidatos = obtenerBrokerCandidatos();

            if (servidorRegional != null) {
                System.out.println("Gestor de Mesa inicializado correctamente");

                if (messageManager.hayMensajesPendientes()) {
                    System.out.println("Procesando mensajes pendientes...");
                    messageManager.procesarMensajesPendientes(servidorRegional, adapter, communicator);
                }

                return true;
            } else {
                System.err.println("No hay servidor regional disponible");
                System.err.println("  Los votos se guardarán para envío posterior");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error inicializando gestor de mesa: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void cargarCandidatos() {
        // TODO: Implementar cuando esté disponible el broker de candidatos
        candidatosDisponibles.clear();
        candidatosDisponibles.add(new Candidato(1, "Juan Pérez", "Partido A"));
        candidatosDisponibles.add(new Candidato(2, "María García", "Partido B"));
        candidatosDisponibles.add(new Candidato(3, "Carlos López", "Partido C"));
        candidatosDisponibles.add(new Candidato(4, "Ana Martínez", "Partido D"));
        candidatosDisponibles.add(new Candidato(5, "Luis Rodríguez", "Partido E"));

        System.out.println("📋 Candidatos cargados: " + candidatosDisponibles.size());
    }

    public boolean validarElector(String documentoIdentidad) {
        // TODO: Implementar validación real con servicio externo

        String hashElector = generarHashElector(documentoIdentidad);
        if (electoresYaVotaron.contains(hashElector)) {
            return false;
        }

        return documentoIdentidad != null && !documentoIdentidad.trim().isEmpty();
    }

    public boolean registrarVoto(String documentoIdentidad, long idCandidato) {
        try {
            if (!validarElector(documentoIdentidad)) {
                System.err.println("Elector no válido o ya votó");
                return false;
            }

            String hashElector = generarHashElector(documentoIdentidad);
            long idVoto = System.currentTimeMillis() + (int)(Math.random() * 1000);
            long timestamp = java.time.Instant.now().getEpochSecond();

            VotoImp voto = new VotoImp(idVoto, idMesa, hashElector, idCandidato, timestamp);

            if (!voto.esValido()) {
                System.err.println("El voto generado no es válido");
                return false;
            }

            electoresYaVotaron.add(hashElector);

            boolean enviado = enviarVoto(voto);

            if (!enviado) {
                electoresYaVotaron.remove(hashElector);
            }

            return enviado;

        } catch (Exception e) {
            System.err.println("Error registrando voto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean enviarVoto(VotoImp voto) {
        try {

            if (servidorRegional == null) {
                System.err.println("No hay servidor disponible. Guardando voto para envío posterior...");
                messageManager.guardarVotoPendiente(voto);
                return true;
            }

            IConfirmacionVotoPrx callback = crearCallback();

            System.out.println("Enviando voto al servidor regional...");
            servidorRegional.enviarVoto(voto, callback);

            System.out.println("Voto enviado. Esperando confirmación...");
            return true;

        } catch (com.zeroc.Ice.NoEndpointException | com.zeroc.Ice.ConnectFailedException e) {
            System.err.println("Servidor no disponible: " + e.getMessage());
            messageManager.guardarVotoPendiente(voto);

            reconectarServidor();
            return true;

        } catch (com.zeroc.Ice.LocalException ex) {
            System.err.println("Error de comunicación ICE: " + ex.getMessage());
            messageManager.guardarVotoPendiente(voto);

            reconectarServidor();
            return true;

        } catch (Exception e) {
            System.err.println("Error enviando voto: " + e.getMessage());
            messageManager.guardarVotoPendiente(voto);
            return false;
        }
    }

    public boolean reconectarServidor() {
        System.out.println("Intentando reconectar al servidor...");

        IRegistrarVotoPrx nuevoServidor = obtenerServidorRegional();

        if (nuevoServidor != null) {
            this.servidorRegional = nuevoServidor;
            System.out.println("Reconectado al servidor regional");

            if (messageManager.hayMensajesPendientes()) {
                System.out.println("Procesando mensajes pendientes...");
                messageManager.procesarMensajesPendientes(servidorRegional, adapter, communicator);
            }

            return true;
        } else {
            System.err.println("No se pudo establecer conexion con ningun servidor");
            return false;
        }
    }

    private IRegistrarVotoPrx obtenerServidorRegional() {
        IRegistrarVotoPrx registrarVoto = null;

        try {
            registrarVoto = IRegistrarVotoPrx.checkedCast(
                    communicator.stringToProxy("regionalAdapter"));
        } catch(com.zeroc.Ice.NotRegisteredException ex) {
            if (query != null) {
                try {
                    registrarVoto = IRegistrarVotoPrx.checkedCast(
                            query.findObjectByType("::Demo::IRegistrarVoto"));
                } catch (Exception e) {
                    System.err.println("Error buscando objeto por tipo: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error conectando a regionalAdapter: " + e.getMessage());
        }

        return registrarVoto;
    }

    private IConfirmacionVotoPrx crearCallback() {
        try {
            if (adapter != null) {
                GestorVotos confirmacionImpl = new GestorVotos();
                com.zeroc.Ice.ObjectPrx obj = adapter.addWithUUID(confirmacionImpl);
                return IConfirmacionVotoPrx.uncheckedCast(obj);
            }
        } catch (Exception e) {
            System.err.println("Error creando callback: " + e.getMessage());
        }
        return null;
    }

    private String generarHashElector(String documentoIdentidad) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(documentoIdentidad.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);
        } catch (Exception e) {
            return "HASH_" + Math.abs(documentoIdentidad.hashCode());
        }
    }
    public List<Candidato> getCandidatosDisponibles() {
        return new ArrayList<>(candidatosDisponibles);
    }

    public String getIdMesa() {
        return idMesa;
    }

    public boolean hayMensajesPendientes() {
        return messageManager.hayMensajesPendientes();
    }

    public void mostrarEstadisticas() {
        messageManager.mostrarEstadisticas();
    }

    public void shutdown() {
        if (messageManager != null) {
            messageManager.shutdown();
        }
        if (adapter != null) {
            adapter.destroy();
        }
    }
}