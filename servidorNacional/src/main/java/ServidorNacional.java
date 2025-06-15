import Demo.*;
import AdministradorCandidatos.AdministradorCandidatos;
import ServidorNacionalUI.ServidorNacionalUI;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Util;

import java.lang.Exception;
import java.util.Properties;

import javax.swing.SwingUtilities;

public class ServidorNacional {
    private static Communicator communicator;
    private static ObjectAdapter adapter;
    private static AdministradorCandidatos administradorCandidatos;
    private static ServidorNacionalUI ui;

    public static void main(String[] args) {
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<String>();

        try {

            com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, extraArgs);
            communicator.getProperties().setProperty("Ice.Default.Package", "Demo");

            adapter = communicator.createObjectAdapterWithEndpoints(
                    "ServidorNacionalAdapter",
                    "tcp -h localhost -p 9999"
            );

            administradorCandidatos = new AdministradorCandidatos(communicator);

            Identity identity = Util.stringToIdentity("AdministradorCandidatos");
            adapter.add(administradorCandidatos, identity);

            adapter.activate();

            System.out.println("Servidor Nacional iniciado en puerto 9999");
            System.out.println("Endpoint: AdministradorCandidatos:tcp -h localhost -p 9999");
            System.out.println("Iniciando interfaz gráfica...");

            SwingUtilities.invokeLater(() -> {
                try {
                    ui = new ServidorNacionalUI(administradorCandidatos);
                    ui.setVisible(true);
                } catch (Exception e) {
                    System.err.println("Error iniciando UI: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Cerrando Servidor Nacional...");
                if (ui != null) {
                    ui.dispose();
                }
                if (communicator != null) {
                    communicator.destroy();
                }
            }));

            if (!extraArgs.isEmpty()) {
                System.err.println("Argumentos adicionales no reconocidos");
                status = 1;
            } else {
                communicator.waitForShutdown();
            }

        } catch (Exception e) {
            System.err.println("Error en Servidor Nacional: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        } finally {
            if (communicator != null) {
                try {
                    communicator.destroy();
                } catch (Exception e) {
                    System.err.println("Error cerrando comunicador: " + e.getMessage());
                }
            }
        }

        System.exit(status);
    }

    public static AdministradorCandidatos getAdministradorCandidatos() {
        return administradorCandidatos;
    }

    public static Communicator getCommunicator() {
        return communicator;
    }
}