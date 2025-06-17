package servidorRegional;

import com.zeroc.Ice.*;
import Demo.*;

public class ServidorRegionalSimple {
    public static void main(String[] args) {
        System.out.println("🎯 === SERVIDOR REGIONAL SIMPLE ===");
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try (Communicator communicator = Util.initialize(args, extraArgs)) {
            if (!extraArgs.isEmpty()) {
                System.err.println("too many arguments");
                status = 1;
            } else {
                // Crear el receptor de votos regional
                ReceptorVotosImpl receptorVotosRegional = new ReceptorVotosImpl("ServidorRegionalSimple");

                // Crear adaptador
                ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "RegionalAdapter", "tcp -h localhost -p 9091");

                // Registrar el receptor de votos regional
                Identity idReceptorRegional = Util.stringToIdentity("receptorVotosRegional");
                adapter.add(receptorVotosRegional, idReceptorRegional);

                adapter.activate();
                
                System.out.println("✅ Servidor Regional Simple iniciado correctamente");
                System.out.println("📊 Receptor de votos disponible en puerto 9091");
                System.out.println("🔗 Endpoint: tcp -h localhost -p 9091");
                System.out.println("🆔 Identity: receptorVotosRegional");
                
                // Mantener el servidor corriendo
                communicator.waitForShutdown();
            }
        } catch (java.lang.Exception e) {
            System.err.println("Error en ServidorRegionalSimple: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        }

        System.exit(status);
    }
} 