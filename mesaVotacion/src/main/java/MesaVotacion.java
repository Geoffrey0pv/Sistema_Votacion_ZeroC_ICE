import Demo.IRegistrarVotoPrx;
import Demo.Voto;
import Demo.IConfirmacionVotoPrx;
import GestorVotos.VotoImp;
import GestorVotos.ConfirmacionVotoI;
import com.zeroc.Ice.ObjectAdapter;

public class MesaVotacion {

    public static void main(String[] args) {
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try(com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "mesa.cfg", extraArgs)) {
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
            // Continuar sin adaptador para callbacks síncronos
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

        if(registrarVoto == null) {
            System.err.println("No se pudo encontrar el objeto IRegistrarVoto");
            System.err.println("Asegúrate de que el servidor regional esté ejecutándose");
            return 1;
        }

        System.out.println(" Conectado al servidor regional exitosamente");
        menu();

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

    // Método para obtener servidor regional con retry
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

    // Método helper para reconexión
    private static IRegistrarVotoPrx reconectarServidor(com.zeroc.Ice.Communicator communicator, 
                                                        com.zeroc.IceGrid.QueryPrx query) {
        System.out.println(" Servidor no disponible, buscando otro servidor...");
        
        try {
            if (query != null) {
                IRegistrarVotoPrx nuevoServidor = IRegistrarVotoPrx.checkedCast(
                        query.findObjectByType("::Demo::IRegistrarVoto"));
                if (nuevoServidor != null) {
                    System.out.println(" Conectado a nuevo servidor regional");
                    return nuevoServidor;
                }
            }
        } catch (Exception e) {
            System.err.println(" Error intentando reconectar: " + e.getMessage());
        }
        
        return null;
    }

    private static IRegistrarVotoPrx enviarVoto(IRegistrarVotoPrx registrarVoto, ObjectAdapter adapter,
                                               com.zeroc.Ice.Communicator communicator, 
                                               com.zeroc.IceGrid.QueryPrx query) {
        try {
            // Crear voto interactivo
            VotoImp votoImpl = VotoImp.crearVotoInteractivo();

            if (!votoImpl.esValido()) {
                System.err.println(" El voto no es válido. Verifique los datos ingresados.");
                return registrarVoto;
            }

            // Crear el callback
            IConfirmacionVotoPrx callback = crearCallback(adapter, communicator);

            // Enviar el voto
            System.out.println(" Enviando voto al servidor regional...");
            registrarVoto.enviarVoto(votoImpl, callback);

            System.out.println(" Voto enviado. Esperando confirmación...");

        } catch (com.zeroc.Ice.NoEndpointException e) {
            System.err.println(" Error: " + e.getMessage());
            IRegistrarVotoPrx nuevoServidor = reconectarServidor(communicator, query);
            if (nuevoServidor != null) {
                return enviarVoto(nuevoServidor, adapter, communicator, query); // Retry
            }
        } catch (com.zeroc.Ice.LocalException ex) {
            System.err.println(" Error de comunicación ICE: " + ex.getMessage());
            IRegistrarVotoPrx nuevoServidor = reconectarServidor(communicator, query);
            if (nuevoServidor != null) {
                return nuevoServidor;
            }
        } catch (Exception e) {
            System.err.println(" Error enviando voto: " + e.getMessage());
            e.printStackTrace();
        }
        
        return registrarVoto;
    }

    private static IRegistrarVotoPrx enviarVotoPrueba(IRegistrarVotoPrx registrarVoto, ObjectAdapter adapter,
                                                     com.zeroc.Ice.Communicator communicator,
                                                     com.zeroc.IceGrid.QueryPrx query) {
        try {
            // Crear voto de prueba
            VotoImp votoImpl = VotoImp.crearVotoPrueba();

            System.out.println(" Voto de prueba generado:");
            System.out.println(votoImpl.toString());

            // Crear el callback
            IConfirmacionVotoPrx callback = crearCallback(adapter, communicator);

            // Enviar el voto
            System.out.println(" Enviando voto de prueba al servidor regional...");
            registrarVoto.enviarVoto(votoImpl, callback);

            System.out.println(" Voto de prueba enviado. Esperando confirmación...");

        } catch (com.zeroc.Ice.NoEndpointException e) {
            System.err.println(" Error: " + e.getMessage());
            IRegistrarVotoPrx nuevoServidor = reconectarServidor(communicator, query);
            if (nuevoServidor != null) {
                return enviarVotoPrueba(nuevoServidor, adapter, communicator, query); // Retry
            }
        } catch (com.zeroc.Ice.LocalException ex) {
            System.err.println(" Error de comunicación ICE: " + ex.getMessage());
            IRegistrarVotoPrx nuevoServidor = reconectarServidor(communicator, query);
            if (nuevoServidor != null) {
                return nuevoServidor;  
            }
        } catch (Exception e) {
            System.err.println(" Error enviando voto de prueba: " + e.getMessage());
            e.printStackTrace();
        }
        
        return registrarVoto;
    }

    private static IConfirmacionVotoPrx crearCallback(ObjectAdapter adapter,
                                                      com.zeroc.Ice.Communicator communicator) {
        try {
            if (adapter != null) {
                // Crear callback con adaptador (asíncrono)
                ConfirmacionVotoI confirmacionImpl = new ConfirmacionVotoI();
                com.zeroc.Ice.ObjectPrx obj = adapter.addWithUUID(confirmacionImpl);
                return IConfirmacionVotoPrx.uncheckedCast(obj);
            } else {
                // Callback nulo para modo síncrono
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
                        "x: salir\n" +
                        "?: mostrar este menú\n" +
                        "==========================");
    }
}