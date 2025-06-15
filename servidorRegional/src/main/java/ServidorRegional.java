// ServidorRegional.java
import Demo.IRegistrarVotoPrx;
import servidorRegional.*;
import com.zeroc.Ice.*;
import java.lang.Exception;

public class ServidorRegional {
    public static void main(String[] args) {
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try(com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, extraArgs)) {
            communicator.getProperties().setProperty("Ice.Default.Package", "com.zeroc.demos.IceGrid.simple");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> communicator.destroy()));

            if(!extraArgs.isEmpty()) {
                System.err.println("too many arguments");
                status = 1;
            } else {
                // Crear adaptador para el servidor regional
                com.zeroc.Ice.ObjectAdapter adapter = communicator.createObjectAdapter("RegionalAdapter");
                com.zeroc.Ice.Properties properties = communicator.getProperties();

                // Registrar el componente ReceptorVotos
                com.zeroc.Ice.Identity idReceptor = com.zeroc.Ice.Util.stringToIdentity("ReceptorVotos");
                adapter.add(new ReceptorVotos(properties.getProperty("Ice.ProgramName")), idReceptor);

                // Registrar el componente GestionCandidatos
                com.zeroc.Ice.Identity idGestion = com.zeroc.Ice.Util.stringToIdentity("GestionCandidatos");
                GestionCandidatos gestionCandidatos = new GestionCandidatos(communicator);
                adapter.add(gestionCandidatos, idGestion);

                adapter.activate();
                System.out.println("Servidor Regional iniciado correctamente");
                System.out.println("- ReceptorVotos disponible en: " + idReceptor.name);
                System.out.println("- GestionCandidatos disponible en: " + idGestion.name);

                communicator.waitForShutdown();
            }
        } catch (Exception e) {
            System.err.println("Error en ServidorRegional: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        }

        System.exit(status);
    }
}