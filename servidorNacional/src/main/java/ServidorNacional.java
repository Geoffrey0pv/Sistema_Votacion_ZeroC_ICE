import Demo.*;
import AdministradorCandidatos.AdministradorCandidatos;
import Broker.BrokerNacional;
import HelloWorld.HelloWorldImpl;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Util;

import java.lang.Exception;
import java.util.Properties;

import javax.swing.SwingUtilities;

public class ServidorNacional {
    private static BrokerNacional brokerNacional;
    private static HelloWorldImpl helloWorld;
    private static Communicator communicator;
    private static ObjectAdapter adapter;

    public static void main(String[] args) {
        int status = 0;

        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Crear adaptador
            adapter = communicator.createObjectAdapterWithEndpoints(
                "ServidorNacionalAdapter", "tcp -h localhost -p 9090"
            );
            
            // Crear e inicializar el Broker Nacional
            brokerNacional = new BrokerNacional(communicator);
            
            // Crear e inicializar Hello World
            helloWorld = new HelloWorldImpl("Servidor Nacional - Sistema de Votación", "1.0.0");
            
            // Registrar el Broker como IAdministradorCandidatos (compatibilidad hacia atrás)
            Identity candidatosId = Util.stringToIdentity("AdministradorCandidatos");
            adapter.add(brokerNacional, candidatosId);
            
            // Registrar el Broker como IBrokerNacional (nueva funcionalidad)
            Identity brokerId = Util.stringToIdentity("BrokerNacional");
            adapter.add(brokerNacional, brokerId);
            
            // Registrar Hello World endpoint
            Identity helloWorldId = Util.stringToIdentity("HelloWorld");
            adapter.add(helloWorld, helloWorldId);
            
            // Activar adaptador
            adapter.activate();
            
            System.out.println("🎯 ===== SERVIDOR NACIONAL CON BROKER =====");
            System.out.println("   🚀 Servidor iniciado con patrón Broker");
            System.out.println("   📡 Endpoint: tcp -h localhost -p 9090");
            System.out.println("   🔄 Escalado automático: ACTIVADO (50%)");
            System.out.println("   ⚖️  Balanceador de carga: ACTIVADO");
            System.out.println("   📊 Monitor de recursos: ACTIVADO");
            System.out.println("==========================================");
            System.out.println("   🎮 Servicios disponibles:");
            System.out.println("   • AdministradorCandidatos (compatibilidad)");
            System.out.println("   • BrokerNacional (nueva funcionalidad)");
            System.out.println("   • HelloWorld (endpoint de prueba) 🌍");
            System.out.println("==========================================");
            System.out.println("   ⏹️  Presiona Ctrl+C para detener");
            System.out.println();
            
            // Configurar shutdown hook para limpieza
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Deteniendo servidor...");
                shutdown();
            }));
            
            // Esperar hasta que se detenga
            communicator.waitForShutdown();
            
        } catch (Exception e) {
            System.err.println("❌ Error en servidor nacional: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        } finally {
            shutdown();
        }
        
        System.exit(status);
    }
    
    private static void shutdown() {
        try {
            // Detener el Broker Nacional
            if (brokerNacional != null) {
                brokerNacional.shutdown();
            }
            
            // Desactivar adaptador
            if (adapter != null) {
                adapter.deactivate();
            }
            
            // Destruir communicator
            if (communicator != null) {
                communicator.destroy();
            }
            
            System.out.println("✅ Servidor nacional detenido correctamente");
            
        } catch (Exception e) {
            System.err.println("❌ Error durante shutdown: " + e.getMessage());
        }
    }
    
    // Método para obtener el Broker (útil para testing o acceso externo)
    public static BrokerNacional getBroker() {
        return brokerNacional;
    }
    
    // Método para obtener Hello World (útil para testing)
    public static HelloWorldImpl getHelloWorld() {
        return helloWorld;
    }
    
    // Método para obtener el communicator (útil para testing)
    public static Communicator getCommunicator() {
        return communicator;
    }
}