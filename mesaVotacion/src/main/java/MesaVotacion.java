import GestorMesa.GestorMesa;
import InterfazGrafica.MesaVotacionUI;
import GestorVotos.VotoImp;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;

public class MesaVotacion {
    private static GestorMesa gestorMesa;
    private static MesaVotacionUI interfazUsuario;
    private static final String ID_MESA_DEFAULT = "MESA_001";

    public static void main(String[] args) {
        int status = 0;
        List<String> extraArgs = new ArrayList<>();

        try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "mesa.cfg", extraArgs)) {
            if (!extraArgs.isEmpty()) {
                System.err.println("Demasiados argumentos");
                status = 1;
            } else {
                status = ejecutar(communicator);
                communicator.waitForShutdown();
            }
        }
        System.exit(status);
    }

    private static int ejecutar(com.zeroc.Ice.Communicator communicator) {

        String idMesa = obtenerIdMesa();

        gestorMesa = new GestorMesa(idMesa);

        boolean inicializado = gestorMesa.inicializar(communicator);

        if (!inicializado) {
            System.err.println("Gestor de mesa inicializado sin conexión al servidor");
            System.err.println(" Los votos se guardarán para envío posterior");
        }

        mostrarMenuInicial();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Seleccione modo de operación (1-2): ");

        try {
            int opcion = scanner.nextInt();

            switch (opcion) {
                case 1:
                    iniciarModoGrafico();
                    break;
                case 2:
                    iniciarModoConsola(scanner);
                    break;
                default:
                    System.out.println("Opción inválida. Iniciando modo gráfico por defecto.");
                    iniciarModoGrafico();
                    break;
            }
        } catch (Exception e) {
            System.out.println("Entrada inválida. Iniciando modo gráfico por defecto.");
            iniciarModoGrafico();
        }

        return 0;
    }

    private static String obtenerIdMesa() {
        // TODO: Implementar lectura desde configuración o argumentos
        // Por ahora retorna el valor por defecto
        String idMesa = System.getProperty("mesa.id", ID_MESA_DEFAULT);
        System.out.println("Inicializando Mesa de Votación: " + idMesa);
        return idMesa;
    }

    private static void mostrarMenuInicial() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("BIENVENIDO AL SISTEMA DE VOTACIÓN ELECTRÓNICA");
        System.out.println("=".repeat(50));
        System.out.println("Mesa: " + gestorMesa.getIdMesa());

        if (gestorMesa.hayMensajesPendientes()) {
            System.out.println("Estado: Hay mensajes pendientes de envío");
        } else {
            System.out.println("Estado: Conexión normal");
        }

        System.out.println("\nModos de operación disponibles:");
        System.out.println("  1. Modo Gráfico (Interfaz visual)");
        System.out.println("  2. Modo Consola (Línea de comandos)");
        System.out.println("=".repeat(50));
    }

    private static void iniciarModoGrafico() {
        System.out.println(" Iniciando modo gráfico...");

        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("No se pudo configurar el Look and Feel del sistema");
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            interfazUsuario = new MesaVotacionUI(gestorMesa);
            interfazUsuario.mostrar();

            System.out.println("Interfaz gráfica iniciada correctamente");
            System.out.println("Para cerrar el sistema, use la opción cerrar ventana (X)");
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Cerrando sistema...");
            if (gestorMesa != null) {
                gestorMesa.shutdown();
            }
        }));
    }

    private static void iniciarModoConsola(Scanner scanner) {
        System.out.println("Iniciando modo consola...");

        boolean continuar = true;

        while (continuar) {
            mostrarMenuConsola();
            System.out.print("==> ");

            try {
                int opcion = scanner.nextInt();
                scanner.nextLine();

                switch (opcion) {
                    case 1:
                        procesarVotoConsola(scanner);
                        break;
                    case 2:
                        mostrarEstadisticas();
                        break;
                    case 3:
                        intentarReconexion();
                        break;
                    case 4:
                        crearVotoPrueba();
                        break;
                    case 5:
                        mostrarCandidatos();
                        break;
                    case 0:
                        continuar = false;
                        break;
                    default:
                        System.out.println("Opción inválida");
                        break;
                }
            } catch (Exception e) {
                System.out.println("Entrada inválida. Intente nuevamente.");
                scanner.nextLine(); // Limpiar buffer
            }
        }

        System.out.println("Cerrando sistema de votación...");
        gestorMesa.shutdown();
    }

    private static void mostrarMenuConsola() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("MENÚ DE OPCIONES");
        System.out.println("-".repeat(40));
        System.out.println("1. Procesar voto");
        System.out.println("2. Ver estadísticas");
        System.out.println("3. Intentar reconexión");
        System.out.println("4. Crear voto de prueba");
        System.out.println("5. Mostrar candidatos");
        System.out.println("0. Salir");
        System.out.println("-".repeat(40));
    }


    private static void procesarVotoConsola(Scanner scanner) {
        System.out.println("\n=== PROCESAR VOTO ===");

        System.out.print("Documento de identidad: ");
        String documento = scanner.nextLine().trim();

        if (documento.isEmpty()) {
            System.out.println("Documento no puede estar vacío");
            return;
        }

        if (!gestorMesa.validarElector(documento)) {
            System.out.println("Elector no válido o ya votó en esta mesa");
            return;
        }

        System.out.println("Elector validado correctamente");

        mostrarCandidatos();

        System.out.print("Seleccione ID del candidato: ");
        try {
            long idCandidato = scanner.nextLong();
            scanner.nextLine();

            System.out.print("¿Confirma su voto por el candidato " + idCandidato + "? (s/n): ");
            String confirmacion = scanner.nextLine().trim().toLowerCase();

            if (confirmacion.equals("s") || confirmacion.equals("si") || confirmacion.equals("y") || confirmacion.equals("yes")) {
                boolean exito = gestorMesa.registrarVoto(documento, idCandidato);

                if (exito) {
                    System.out.println("¡Voto registrado exitosamente!");
                } else {
                    System.out.println("Error registrando el voto");
                }
            } else {
                System.out.println("Voto cancelado");
            }

        } catch (Exception e) {
            System.out.println("ID de candidato inválido");
            scanner.nextLine();
        }
    }

    private static void mostrarEstadisticas() {
        System.out.println("\n=== ESTADÍSTICAS DEL SISTEMA ===");
        System.out.println("Mesa: " + gestorMesa.getIdMesa());

        if (gestorMesa.hayMensajesPendientes()) {
            System.out.println("Estado: Hay mensajes pendientes");
        } else {
            System.out.println("Estado: Sistema operativo");
        }

        gestorMesa.mostrarEstadisticas();
        System.out.println("===============================");
    }

    private static void intentarReconexion() {
        System.out.println("\nIntentando reconectar...");
        boolean exito = gestorMesa.reconectarServidor();

        if (exito) {
            System.out.println("Reconexión exitosa");
        } else {
            System.out.println("No se pudo establecer conexión");
        }
    }

    private static void crearVotoPrueba() {
        System.out.println("\n Creando voto de prueba...");
        VotoImp votoPrueba = VotoImp.crearVotoPrueba();
        System.out.println("Voto creado: " + votoPrueba);

        String documentoPrueba = "TEST_" + System.currentTimeMillis();
        boolean exito = gestorMesa.registrarVoto(documentoPrueba, votoPrueba.idCandidato);

        if (exito) {
            System.out.println(" Voto de prueba procesado");
        } else {
            System.out.println(" Error procesando voto de prueba");
        }
    }

    private static void mostrarCandidatos() {
        System.out.println("\n Candidatos disponibles:");
        System.out.println("-".repeat(30));

        gestorMesa.getCandidatosDisponibles().forEach(candidato -> {
            System.out.println("  " + candidato.idCandidato + " - " + candidato.nombre);
        });

        System.out.println("-".repeat(30));
    }
}