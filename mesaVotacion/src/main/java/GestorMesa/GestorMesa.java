package GestorMesa;

import Demo.*;
import GestorVotos.VotoImp;
import ReliableMessageManager.ReliableMessageManager;
import GestorVotos.GestorVotos;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Communicator;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GestorMesa implements IRecibirCandidatos {
    private final ReliableMessageManager messageManager;
    private IRegistrarVotoPrx servidorRegional;
    private ICargarCandidatosPrx gestionCandidatos;
    private ObjectAdapter adapter;
    private Communicator communicator;
    private com.zeroc.IceGrid.QueryPrx query;
    private String idMesa;
    private List<Candidato> candidatosDisponibles;
    private List<String> electoresYaVotaron;
    private boolean candidatosCargados = false;

    public GestorMesa(String idMesa) {
        this.idMesa = idMesa;
        this.candidatosDisponibles = new ArrayList<>();
        this.electoresYaVotaron = new ArrayList<>();
        this.messageManager = new ReliableMessageManager();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n Guardando mensajes pendientes...");
            if (messageManager != null) {
                messageManager.shutdown();
            }
        }));
    }

    public boolean inicializar(Communicator communicator) {
        this.communicator = communicator;

        try {
            this.adapter = communicator.createObjectAdapter("MesaCallbackAdapter");

            // Registrar esta mesa como receptor de candidatos
            com.zeroc.Ice.Identity mesaIdentity = new com.zeroc.Ice.Identity();
            mesaIdentity.name = "Mesa-" + idMesa;
            mesaIdentity.category = "RecibirCandidatos";
            adapter.add(this, mesaIdentity);

            adapter.activate();

            this.query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                    communicator.stringToProxy("DemoIceGrid/Query"));

            this.servidorRegional = obtenerServidorRegional(communicator);
            this.gestionCandidatos = obtenerGestionCandidatos(communicator);

            if (servidorRegional != null) {
                System.out.println("Gestor de Mesa inicializado correctamente");
                System.out.println("  Mesa ID: " + idMesa);
                System.out.println("  Conectado al servidor regional");

                // Procesar mensajes pendientes si los hay
                if (messageManager.hayMensajesPendientes()) {
                    System.out.println("📤 Procesando mensajes pendientes...");
                    messageManager.procesarMensajesPendientes(servidorRegional, adapter, communicator);
                }

                solicitarCandidatos();

                return true;
            } else {
                System.err.println("⚠️  No hay servidor regional disponible");
                System.err.println("   📤 Los votos se guardarán para envío posterior");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Error inicializando gestor de mesa: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void recibirCandidatos(Candidato[] candidatos, IConfirmacionCandidatosPrx callback,
                                  com.zeroc.Ice.Current current) {
        try {
            System.out.println("📋 Recibiendo lista de candidatos del servidor regional...");

            candidatosDisponibles.clear();
            for (Candidato candidato : candidatos) {
                candidatosDisponibles.add(candidato);
                System.out.println("   ✓ " + candidato.idCandidato + " - " + candidato.nombre +
                        " (" + candidato.partido + ")");
            }

            candidatosCargados = true;
            System.out.println(" Candidatos cargados exitosamente. Total: " + candidatos.length);

            if (callback != null) {
                try {
                    callback.recibirConfirmacion(true,
                            "Candidatos recibidos correctamente en mesa " + idMesa);
                } catch (Exception callbackEx) {
                    System.err.println(" Error enviando confirmación: " + callbackEx.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println(" Error procesando candidatos: " + e.getMessage());
            e.printStackTrace();

            if (callback != null) {
                try {
                    callback.recibirConfirmacion(false,
                            "Error procesando candidatos en mesa " + idMesa + ": " + e.getMessage());
                } catch (Exception callbackEx) {
                    System.err.println("Error enviando confirmación de error: " + callbackEx.getMessage());
                }
            }
        }
    }

    private void solicitarCandidatos() {
        if (gestionCandidatos != null) {
            try {
                System.out.println(" Solicitando candidatos al servidor regional...");

                String endpointMesa = "Mesa-" + idMesa + ":tcp -h localhost -p " +
                        (10000 + Integer.parseInt(idMesa.replaceAll("\\D+", "")));

                CompletableFuture.supplyAsync(() -> {
                            try {
                                return gestionCandidatos.enviarCandidatosAMesas(endpointMesa);
                            } catch (Exception e) {
                                System.err.println("Error en solicitud asíncrona: " + e.getMessage());
                                return false;
                            }
                        }).orTimeout(5, TimeUnit.SECONDS)
                        .thenAccept(resultado -> {
                            if (resultado) {
                                System.out.println(" Solicitud de candidatos enviada exitosamente");
                            } else {
                                System.err.println("  Error en la solicitud de candidatos");
                                cargarCandidatosPorDefecto();
                            }
                        }).exceptionally(ex -> {
                            System.err.println("  Timeout o error en solicitud de candidatos: " + ex.getMessage());
                            cargarCandidatosPorDefecto();
                            return null;
                        });

            } catch (Exception e) {
                System.err.println(" Error solicitando candidatos: " + e.getMessage());
                cargarCandidatosPorDefecto();
            }
        } else {
            System.err.println("  No hay conexión con GestionCandidatos, cargando candidatos por defecto");
            cargarCandidatosPorDefecto();
        }
    }

    public void cargarCandidatos() {
        if (!candidatosCargados) {
            solicitarCandidatos();

            // Esperar un momento para que lleguen los candidatos del servidor
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Si aún no se han cargado, usar candidatos por defecto
            if (!candidatosCargados) {
                cargarCandidatosPorDefecto();
            }
        }
    }

    private void cargarCandidatosPorDefecto() {
        System.out.println("📋 Cargando candidatos por defecto...");
        candidatosDisponibles.clear();
        candidatosDisponibles.add(new Candidato(1, "Juan Pérez", "Partido A"));
        candidatosDisponibles.add(new Candidato(2, "María García", "Partido B"));
        candidatosDisponibles.add(new Candidato(3, "Carlos López", "Partido C"));
        candidatosDisponibles.add(new Candidato(4, "Ana Martínez", "Partido D"));
        candidatosDisponibles.add(new Candidato(5, "Luis Rodríguez", "Partido E"));

        candidatosCargados = true;
        System.out.println("✅ Candidatos por defecto cargados: " + candidatosDisponibles.size());
    }

    public boolean validarElector(String documentoIdentidad) {
        String hashElector = generarHashElector(documentoIdentidad);
        if (electoresYaVotaron.contains(hashElector)) {
            System.err.println("  El elector ya votó en esta mesa");
            return false;
        }

        boolean valido = documentoIdentidad != null && !documentoIdentidad.trim().isEmpty();
        if (valido) {
            System.out.println(" Elector validado: " + documentoIdentidad.substring(0,
                    Math.min(3, documentoIdentidad.length())) + "***");
        }
        return valido;
    }

    public boolean validarCandidato(long idCandidato) {
        return candidatosDisponibles.stream()
                .anyMatch(c -> c.idCandidato == idCandidato);
    }

    public boolean registrarVoto(String documentoIdentidad, long idCandidato) {
        try {
            if (!validarElector(documentoIdentidad)) {
                System.err.println(" Elector no válido o ya votó");
                return false;
            }

            if (!validarCandidato(idCandidato)) {
                System.err.println(" Candidato no válido: " + idCandidato);
                return false;
            }

            String hashElector = generarHashElector(documentoIdentidad);
            long idVoto = System.currentTimeMillis() + (int)(Math.random() * 1000);
            long timestamp = java.time.Instant.now().getEpochSecond();

            VotoImp voto = new VotoImp(idVoto, idMesa, hashElector, idCandidato, timestamp);

            if (!voto.esValido()) {
                System.err.println(" El voto generado no es válido");
                return false;
            }

            // Marcar elector como que ya votó
            electoresYaVotaron.add(hashElector);

            // Obtener nombre del candidato para mostrar
            String nombreCandidato = candidatosDisponibles.stream()
                    .filter(c -> c.idCandidato == idCandidato)
                    .map(c -> c.nombre)
                    .findFirst()
                    .orElse("Candidato " + idCandidato);

            System.out.println("🗳️  Registrando voto:");
            System.out.println("     Voto ID: " + idVoto);
            System.out.println("     Candidato: " + nombreCandidato);
            System.out.println("     Mesa: " + idMesa);

            boolean enviado = enviarVoto(voto);

            if (!enviado) {
                // Si no se pudo enviar, quitar al elector de la lista para que pueda intentar de nuevo
                electoresYaVotaron.remove(hashElector);
                System.err.println(" No se pudo procesar el voto");
            } else {
                System.out.println(" Voto procesado exitosamente");
            }

            return enviado;

        } catch (Exception e) {
            System.err.println(" Error registrando voto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean enviarVoto(VotoImp voto) {
        try {
            if (servidorRegional == null) {
                System.err.println("  No hay servidor disponible. Guardando voto para envío posterior...");
                messageManager.guardarVotoPendiente(voto);
                return true;
            }

            IConfirmacionVotoPrx callback = crearCallback();

            System.out.println(" Enviando voto al servidor regional...");
            servidorRegional.enviarVoto(voto, callback);

            System.out.println(" Voto enviado. Esperando confirmación...");
            return true;

        } catch (com.zeroc.Ice.NoEndpointException | com.zeroc.Ice.ConnectFailedException e) {
            System.err.println(" Servidor no disponible: " + e.getMessage());
            messageManager.guardarVotoPendiente(voto);
            reconectarServidor();
            return true;

        } catch (com.zeroc.Ice.LocalException ex) {
            System.err.println(" Error de comunicación ICE: " + ex.getMessage());
            messageManager.guardarVotoPendiente(voto);
            reconectarServidor();
            return true;

        } catch (Exception e) {
            System.err.println(" Error enviando voto: " + e.getMessage());
            messageManager.guardarVotoPendiente(voto);
            return false;
        }
    }

    public boolean reconectarServidor() {
        System.out.println(" Intentando reconectar al servidor...");

        IRegistrarVotoPrx nuevoServidorVotos = obtenerServidorRegional(communicator);
        ICargarCandidatosPrx nuevaGestionCandidatos = obtenerGestionCandidatos(communicator);

        if (nuevoServidorVotos != null) {
            this.servidorRegional = nuevoServidorVotos;
            this.gestionCandidatos = nuevaGestionCandidatos;

            System.out.println(" Reconectado al servidor regional");

            if (messageManager.hayMensajesPendientes()) {
                System.out.println(" Procesando mensajes pendientes...");
                messageManager.procesarMensajesPendientes(servidorRegional, adapter, communicator);
            }

            // Solicitar candidatos actualizados
            if (!candidatosCargados || candidatosDisponibles.isEmpty()) {
                solicitarCandidatos();
            }

            return true;
        } else {
            System.err.println(" No se pudo establecer conexión con ningún servidor");
            return false;
        }
    }

    private static IRegistrarVotoPrx obtenerServidorRegional(com.zeroc.Ice.Communicator communicator) {
        IRegistrarVotoPrx registrarVoto = null;
        
        try {
            // Primero intentar conectar directamente usando IceGrid con la identidad específica
            System.out.println("🔗 Conectando al servidor regional (receptorVotos)...");
            
            // Conectar usando IceGrid Query para encontrar el objeto por identidad
            com.zeroc.IceGrid.QueryPrx query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                    communicator.stringToProxy("DemoIceGrid/Query"));
            
            if (query != null) {
                try {
                    // Buscar el objeto por identidad específica
                    com.zeroc.Ice.ObjectPrx obj = query.findObjectByType("::Demo::IRegistrarVoto");
                    if (obj != null) {
                        registrarVoto = IRegistrarVotoPrx.checkedCast(obj);
                        if (registrarVoto != null) {
                            System.out.println("✅ Conectado al servidor regional via IceGrid Query");
                            return registrarVoto;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error buscando por tipo: " + e.getMessage());
                }
                
                // Si no funciona por tipo, intentar por identidad directa
                try {
                    com.zeroc.Ice.ObjectPrx objById = query.findObjectById(
                            com.zeroc.Ice.Util.stringToIdentity("receptorVotos"));
                    if (objById != null) {
                        registrarVoto = IRegistrarVotoPrx.checkedCast(objById);
                        if (registrarVoto != null) {
                            System.out.println("✅ Conectado al servidor regional via identidad");
                            return registrarVoto;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error buscando por identidad: " + e.getMessage());
                }
            }
            
            // Método alternativo: conectar directamente al adaptador
            try {
                System.out.println("🔄 Intentando conexión directa al adaptador...");
                String proxyString = "receptorVotos@RegionalAdapter";
                com.zeroc.Ice.ObjectPrx directObj = communicator.stringToProxy(proxyString);
                if (directObj != null) {
                    registrarVoto = IRegistrarVotoPrx.checkedCast(directObj);
                    if (registrarVoto != null) {
                        System.out.println("✅ Conectado directamente al adaptador regional");
                        return registrarVoto;
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️  Error en conexión directa: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error general conectando al servidor regional: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.err.println("❌ No se pudo establecer conexión con el servidor regional");
        return null;
    }

    private ICargarCandidatosPrx obtenerGestionCandidatos(com.zeroc.Ice.Communicator communicator) {
        ICargarCandidatosPrx cargarCandidatos = null;
        
        try {
            // Conectar al servicio de gestión de candidatos
            System.out.println("🔗 Conectando al servicio de gestión de candidatos...");
            
            // Usar IceGrid Query para encontrar el servicio
            com.zeroc.IceGrid.QueryPrx query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                    communicator.stringToProxy("DemoIceGrid/Query"));
            
            if (query != null) {
                try {
                    // Buscar por tipo de interfaz
                    com.zeroc.Ice.ObjectPrx obj = query.findObjectByType("::Demo::ICargarCandidatos");
                    if (obj != null) {
                        cargarCandidatos = ICargarCandidatosPrx.checkedCast(obj);
                        if (cargarCandidatos != null) {
                            System.out.println("✅ Conectado al servicio de candidatos via IceGrid Query");
                            return cargarCandidatos;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error buscando gestión candidatos por tipo: " + e.getMessage());
                }
                
                // Intentar por identidad específica
                try {
                    com.zeroc.Ice.ObjectPrx objById = query.findObjectById(
                            com.zeroc.Ice.Util.stringToIdentity("gestionCandidatos"));
                    if (objById != null) {
                        cargarCandidatos = ICargarCandidatosPrx.checkedCast(objById);
                        if (cargarCandidatos != null) {
                            System.out.println("✅ Conectado al servicio de candidatos via identidad");
                            return cargarCandidatos;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error buscando gestión candidatos por identidad: " + e.getMessage());
                }
            }
            
            // Método alternativo: conexión directa
            try {
                System.out.println("🔄 Intentando conexión directa al servicio de candidatos...");
                String proxyString = "gestionCandidatos@RegionalAdapter";
                com.zeroc.Ice.ObjectPrx directObj = communicator.stringToProxy(proxyString);
                if (directObj != null) {
                    cargarCandidatos = ICargarCandidatosPrx.checkedCast(directObj);
                    if (cargarCandidatos != null) {
                        System.out.println("✅ Conectado directamente al servicio de candidatos");
                        return cargarCandidatos;
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️  Error en conexión directa a candidatos: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error general conectando al servicio de candidatos: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.err.println("❌ No se pudo establecer conexión con el servicio de gestión de candidatos");
        return null;
    }

    private IConfirmacionVotoPrx crearCallback() {
        try {
            if (adapter != null) {
                GestorVotos confirmacionImpl = new GestorVotos();
                com.zeroc.Ice.ObjectPrx obj = adapter.addWithUUID(confirmacionImpl);
                return IConfirmacionVotoPrx.uncheckedCast(obj);
            }
        } catch (Exception e) {
            System.err.println(" Error creando callback: " + e.getMessage());
        }
        return null;
    }

    private String generarHashElector(String documentoIdentidad) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((documentoIdentidad + idMesa).getBytes()); // Incluir ID mesa para unicidad
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
            return "HASH_" + Math.abs((documentoIdentidad + idMesa).hashCode());
        }
    }

    // Getters y métodos de utilidad
    public List<Candidato> getCandidatosDisponibles() {
        return new ArrayList<>(candidatosDisponibles);
    }

    public String getIdMesa() {
        return idMesa;
    }

    public boolean hayMensajesPendientes() {
        return messageManager.hayMensajesPendientes();
    }

    public boolean candidatosCargados() {
        return candidatosCargados;
    }

    public void mostrarEstadisticas() {
        System.out.println("\n ESTADÍSTICAS DE LA MESA " + idMesa);
        System.out.println("    Electores que votaron: " + electoresYaVotaron.size());
        System.out.println("     Candidatos disponibles: " + candidatosDisponibles.size());
        System.out.println("   Candidatos cargados desde servidor: " +
                (candidatosCargados ? "Sí" : "No"));
        messageManager.mostrarEstadisticas();
    }

    public void shutdown() {
        System.out.println("Cerrando gestor de mesa...");
        if (messageManager != null) {
            messageManager.shutdown();
        }
        if (adapter != null) {
            adapter.destroy();
        }
    }
}