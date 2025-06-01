
import Demo.IRegistrarVotoPrx;
import Demo.Voto;
import Demo.IConfirmacionVotoPrx;
import GestorVotos.VotoImp;
import messaging.ReliableMessageManager;
import GestorVotos.ConfirmacionVotoI;
import com.zeroc.Ice.ObjectAdapter;


public class MesaVotacion {
    private static ReliableMessageManager messageManager;
    
    public static void main(String[] args) {
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try(com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "mesa.cfg", extraArgs)) {
            // Inicializar el gestor de mensajes confiables
            messageManager = new ReliableMessageManager();
            
            // Agregar shutdown hook para persistir mensajes al cerrar
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🔄 Guardando mensajes pendientes...");
                messageManager.shutdown();
            }));
            
            if(!extraArgs.isEmpty()) {
                System.err.println("too many arguments");
                status = 1;
            } else {
                status = run(communicator);
            }
        }

        System.exit(status);
    }

    private static int run(com.zeroc.Ice.Communicator communicator) {
        // Crear adaptador para callbacks
        ObjectAdapter adapter = null;
        try {
            adapter = communicator.createObjectAdapter("MesaCallbackAdapter");
            adapter.activate();
        } catch (Exception e) {
            System.err.println("Error creando adaptador: " + e.getMessage());
        }

        // Conectar al servidor regional
        IRegistrarVotoPrx registrarVoto = obtenerServidorRegional(communicator);
        com.zeroc.IceGrid.QueryPrx query = null;

        try {
            query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                    communicator.stringToProxy("DemoIceGrid/Query"));
        } catch (Exception e) {
            System.err.println("No se pudo conectar a IceGrid Query: " + e.getMessage());
        }

        // Verificar si hay conexión inicial
        if(registrarVoto != null) {
            System.out.println("✅ Conectado al servidor regional exitosamente");
            
            // Procesar mensajes pendientes si los hay
            if (messageManager.hayMensajesPendientes()) {
                System.out.println("🔄 Procesando mensajes pendientes...");
                messageManager.procesarMensajesPendientes(registrarVoto, adapter, communicator);
            }
        } else {
            System.err.println("⚠️  No hay servidor regional disponible");
            System.err.println("   Los votos se guardarán para envío posterior");
        }

        menu();
        messageManager.mostrarEstadisticas();

        java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(System.in));

        String line = null;
        do {
            try {
                System.out.print("==> ");
                System.out.flush();
                line = in.readLine();
                if(line == null) {
                    break;
                }

                switch (line.trim().toLowerCase()) {
                    case "t":
                        registrarVoto = enviarVoto(registrarVoto, adapter, communicator, query);
                        break;
                    case "p":
                        registrarVoto = enviarVotoPrueba(registrarVoto, adapter, communicator, query);
                        break;
                    case "r":
                        registrarVoto = reintentarConexion(registrarVoto, adapter, communicator, query);
                        break;
                    case "s":
                        messageManager.mostrarEstadisticas();
                        break;
                    case "x":
                        System.out.println("Saliendo...");
                        break;
                    case "?":
                        menu();
                        break;
                    default:
                        System.out.println("Comando desconocido: '" + line + "'");
                        menu();
                        break;
                }
            } catch(java.io.IOException ex) {
                ex.printStackTrace();
            }
        } while(!line.equals("x"));

        if (adapter != null) {
            adapter.destroy();
        }

        return 0;
    }

    private static IRegistrarVotoPrx enviarVoto(IRegistrarVotoPrx registrarVoto, ObjectAdapter adapter,
                                               com.zeroc.Ice.Communicator communicator, 
                                               com.zeroc.IceGrid.QueryPrx query) {
        try {
            // Crear voto interactivo
            VotoImp votoImpl = VotoImp.crearVotoInteractivo();

            if (!votoImpl.esValido()) {
                System.err.println("❌ El voto no es válido. Verifique los datos ingresados.");
                return registrarVoto;
            }

            // Verificar conexión antes de enviar
            if (registrarVoto == null) {
                System.err.println("⚠️  No hay servidor disponible. Guardando voto para envío posterior...");
                messageManager.guardarVotoPendiente(votoImpl);
                return registrarVoto;
            }

            // Crear el callback
            IConfirmacionVotoPrx callback = crearCallback(adapter, communicator);

            // Enviar el voto
            System.out.println("📤 Enviando voto al servidor regional...");
            registrarVoto.enviarVoto(votoImpl, callback);

            System.out.println("✅ Voto enviado. Esperando confirmación...");

        } catch (com.zeroc.Ice.NoEndpointException | com.zeroc.Ice.ConnectFailedException e) {
            System.err.println("⚠️  Servidor no disponible: " + e.getMessage());
            
            // Guardar el voto para envío posterior
            VotoImp votoImpl = VotoImp.crearVotoInteractivo();
            if (votoImpl.esValido()) {
                messageManager.guardarVotoPendiente(votoImpl);
            }
            
            // Intentar reconectar
            IRegistrarVotoPrx nuevoServidor = reconectarServidor(communicator, query);
            return nuevoServidor;
            
        } catch (com.zeroc.Ice.LocalException ex) {
            System.err.println("❌ Error de comunicación ICE: " + ex.getMessage());
            
            // Guardar voto para reintento
            VotoImp votoImpl = VotoImp.crearVotoInteractivo();
            if (votoImpl.esValido()) {
                messageManager.guardarVotoPendiente(votoImpl);
            }
            
            IRegistrarVotoPrx nuevoServidor = reconectarServidor(communicator, query);
            return nuevoServidor;
            
        } catch (Exception e) {
            System.err.println("❌ Error enviando voto: " + e.getMessage());
            e.printStackTrace();
        }
        
        return registrarVoto;
    }

    private static IRegistrarVotoPrx enviarVotoPrueba(IRegistrarVotoPrx registrarVoto, ObjectAdapter adapter,
                                                     com.zeroc.Ice.Communicator communicator,
                                                     com.zeroc.IceGrid.QueryPrx query) {
        // Crear voto de prueba
        VotoImp votoImpl = VotoImp.crearVotoPrueba();
        System.out.println("🧪 Voto de prueba generado:");
        System.out.println(votoImpl.toString());

        try {
            // Verificar conexión antes de enviar
            if (registrarVoto == null) {
                System.err.println("⚠️  No hay servidor disponible. Guardando voto de prueba para envío posterior...");
                messageManager.guardarVotoPendiente(votoImpl);
                return registrarVoto;
            }

            // Crear el callback
            IConfirmacionVotoPrx callback = crearCallback(adapter, communicator);

            // Enviar el voto
            System.out.println("📤 Enviando voto de prueba al servidor regional...");
            registrarVoto.enviarVoto(votoImpl, callback);

            System.out.println("✅ Voto de prueba enviado. Esperando confirmación...");

        } catch (com.zeroc.Ice.NoEndpointException | com.zeroc.Ice.ConnectFailedException e) {
            System.err.println("⚠️  Servidor no disponible: " + e.getMessage());
            messageManager.guardarVotoPendiente(votoImpl);
            
            IRegistrarVotoPrx nuevoServidor = reconectarServidor(communicator, query);
            return nuevoServidor;
            
        } catch (com.zeroc.Ice.LocalException ex) {
            System.err.println("❌ Error de comunicación ICE: " + ex.getMessage());
            messageManager.guardarVotoPendiente(votoImpl);
            
            IRegistrarVotoPrx nuevoServidor = reconectarServidor(communicator, query);
            return nuevoServidor;
            
        } catch (Exception e) {
            System.err.println("❌ Error enviando voto de prueba: " + e.getMessage());
            messageManager.guardarVotoPendiente(votoImpl);
            e.printStackTrace();
        }
        
        return registrarVoto;
    }

    // Nuevo método para reintentar conexión manualmente
    private static IRegistrarVotoPrx reintentarConexion(IRegistrarVotoPrx registrarVoto, ObjectAdapter adapter,
                                                       com.zeroc.Ice.Communicator communicator,
                                                       com.zeroc.IceGrid.QueryPrx query) {
        System.out.println("🔄 Intentando reconectar al servidor...");
        
        IRegistrarVotoPrx nuevoServidor = obtenerServidorRegional(communicator);
        
        if (nuevoServidor != null) {
            System.out.println("✅ Reconectado al servidor regional");
            
            // Procesar mensajes pendientes
            if (messageManager.hayMensajesPendientes()) {
                System.out.println("📤 Procesando mensajes pendientes...");
                messageManager.procesarMensajesPendientes(nuevoServidor, adapter, communicator);
            }
            
            return nuevoServidor;
        } else {
            System.err.println("❌ No se pudo establecer conexión con ningún servidor");
            messageManager.mostrarEstadisticas();
            return registrarVoto;
        }
    }

    // Métodos existentes (sin cambios)
    private static IRegistrarVotoPrx obtenerServidorRegional(com.zeroc.Ice.Communicator communicator) {
        IRegistrarVotoPrx registrarVoto = null;
        com.zeroc.IceGrid.QueryPrx query = null;

        try {
            query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                    communicator.stringToProxy("DemoIceGrid/Query"));
        } catch (Exception e) {
            System.err.println("No se pudo conectar a IceGrid Query: " + e.getMessage());
        }

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

    private static IRegistrarVotoPrx reconectarServidor(com.zeroc.Ice.Communicator communicator, 
                                                        com.zeroc.IceGrid.QueryPrx query) {
        System.out.println("🔄 Servidor no disponible, buscando otro servidor...");
        
        try {
            if (query != null) {
                IRegistrarVotoPrx nuevoServidor = IRegistrarVotoPrx.checkedCast(
                        query.findObjectByType("::Demo::IRegistrarVoto"));
                if (nuevoServidor != null) {
                    System.out.println("✅ Conectado a nuevo servidor regional");
                    
                    // Procesar mensajes pendientes automáticamente
                    if (messageManager.hayMensajesPendientes()) {
                        System.out.println("📤 Procesando mensajes pendientes automáticamente...");
                        messageManager.procesarMensajesPendientes(nuevoServidor, null, communicator);
                    }
                    
                    return nuevoServidor;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error intentando reconectar: " + e.getMessage());
        }
        
        return null;
    }

    private static IConfirmacionVotoPrx crearCallback(ObjectAdapter adapter,
                                                      com.zeroc.Ice.Communicator communicator) {
        try {
            if (adapter != null) {
                ConfirmacionVotoI confirmacionImpl = new ConfirmacionVotoI();
                com.zeroc.Ice.ObjectPrx obj = adapter.addWithUUID(confirmacionImpl);
                return IConfirmacionVotoPrx.uncheckedCast(obj);
            } else {
                System.out.println("  Usando modo síncrono (sin callback)");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error creando callback: " + e.getMessage());
            return null;
        }
    }

    private static void menu() {
        System.out.println(
                "\n=== SISTEMA DE VOTACIÓN ===\n" +
                        "t: enviar voto (interactivo)\n" +
                        "p: enviar voto de prueba\n" +
                        "r: reintentar conexión\n" +
                        "s: mostrar estadísticas de mensajes\n" +
                        "x: salir\n" +
                        "?: mostrar este menú\n" +
                        "==========================");
    }
}