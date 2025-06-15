import Demo.*;
import AdministradorCandidatos.AdministradorCandidatos;
import Broker.BrokerNacional;
import HelloWorld.HelloWorldImpl;
import ConsultaMesa.ConsultaMesaImpl;
import ConsultaCiudadanos.ConsultaCiudadanosImpl;
import ServidorNacionalUI.ServidorNacionalUI;
import Config.ConfigManager;
import Services.ProcesadorLoteVotosImpl;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Util;

import java.lang.Exception;
import java.util.Properties;

import javax.swing.SwingUtilities;

public class ServidorNacional {

    private static Communicator communicator;
    private static ObjectAdapter adapter;
    private static AdministradorCandidatos administradorCandidatos;
    private static ProcesadorLoteVotosImpl procesadorLoteVotos;
    private static Communicator communicator;
    private static ObjectAdapter adapter;
    private static ConfigManager configManager;
    private static ServidorNacionalUI ui;
    private static boolean useUI = false;

    public static void main(String[] args) {
        int status = 0;

        try {
            // Verificar si se debe usar la interfaz gráfica
            useUI = checkForUIParameter(args);
            
            // Inicializar configuración
            configManager = ConfigManager.getInstance();
            System.out.println("✅ Configuración cargada correctamente");
            
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
            
            // Crear e inicializar ConsultaMesa con configuración
            consultaMesa = new ConsultaMesaImpl();
            
            // Crear e inicializar ConsultaCiudadanos con configuración
            consultaCiudadanos = new ConsultaCiudadanosImpl();
            
            // Crear e inicializar ProcesadorLoteVotos
            procesadorLoteVotos = new ProcesadorLoteVotosImpl();
            
            // Registrar el Broker como IAdministradorCandidatos (compatibilidad hacia atrás)
            Identity candidatosId = Util.stringToIdentity("AdministradorCandidatos");
            adapter.add(brokerNacional, candidatosId);
            
            // Registrar el Broker como IBrokerNacional (nueva funcionalidad)
            Identity brokerId = Util.stringToIdentity("BrokerNacional");
            adapter.add(brokerNacional, brokerId);
            
            // Registrar Hello World endpoint
            Identity helloWorldId = Util.stringToIdentity("HelloWorld");
            adapter.add(helloWorld, helloWorldId);
            
            // Registrar ConsultaMesa endpoint
            Identity consultaMesaId = Util.stringToIdentity("ConsultaMesa");
            adapter.add(consultaMesa, consultaMesaId);
            
            // Registrar ConsultaCiudadanos endpoint
            Identity consultaCiudadanosId = Util.stringToIdentity("ConsultaCiudadanos");
            adapter.add(consultaCiudadanos, consultaCiudadanosId);
            
            // Registrar ProcesadorLoteVotos endpoint
            Identity procesadorLoteVotosId = Util.stringToIdentity("ProcesadorLoteVotos");
            adapter.add(procesadorLoteVotos, procesadorLoteVotosId);
            
            // Activar adaptador
            adapter.activate();
            
            System.out.println("🎯 ===== SERVIDOR NACIONAL CON BROKER =====");
            System.out.println("   🚀 Servidor iniciado con patrón Broker");
            System.out.println("   📡 Endpoint: tcp -h localhost -p 9090");
            System.out.println("   🔄 Escalado automático: ACTIVADO (50%)");
            System.out.println("   ⚖️  Balanceador de carga: ACTIVADO");
            System.out.println("   📊 Monitor de recursos: ACTIVADO");
            System.out.println("   🗄️  Base de datos: " + configManager.getDatabaseUrl());
            System.out.println("==========================================");
            System.out.println("   🎮 Servicios disponibles:");
            System.out.println("   • AdministradorCandidatos (compatibilidad)");
            System.out.println("   • BrokerNacional (nueva funcionalidad)");
            System.out.println("   • HelloWorld (endpoint de prueba) 🌍");
            System.out.println("   • ConsultaMesa (consulta por documento) 🔍");
            System.out.println("   • ConsultaCiudadanos (consulta por ciudadano) 🌍");
            System.out.println("   • ProcesadorLoteVotos (procesamiento de votos) 🗳️");
            System.out.println("==========================================");
            
            if (useUI) {
                System.out.println("   🖥️  Interfaz gráfica: HABILITADA");
                System.out.println("   📱 Abriendo ventana de administración...");
                
                // Lanzar la interfaz gráfica en el hilo de eventos de Swing
                SwingUtilities.invokeLater(() -> {
                    try {
                        ui = new ServidorNacionalUI();
                        ui.setVisible(true);
                        System.out.println("✅ Interfaz gráfica iniciada correctamente");
                    } catch (Exception e) {
                        System.err.println("❌ Error iniciando interfaz gráfica: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                System.out.println("   💻 Modo consola: ACTIVO");
                System.out.println("   💡 Para usar interfaz gráfica, ejecute con: --ui");
            }
            
            System.out.println("   ⏹️  Presiona Ctrl+C para detener");
            System.out.println();
            
            // Test de conexiones de base de datos
            testDatabaseConnections();
            
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
    
    /**
     * Verifica si se debe usar la interfaz gráfica
     */
    private static boolean checkForUIParameter(String[] args) {
        for (String arg : args) {
            if ("--ui".equals(arg) || "-ui".equals(arg) || "--gui".equals(arg)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Prueba las conexiones de base de datos de todos los servicios
     */
    private static void testDatabaseConnections() {
        System.out.println("\n🧪 ===== VERIFICACIÓN DE CONEXIONES BD =====");
        
        // Test ConsultaCiudadanos (debe usar BD de ciudadanos)
        if (consultaCiudadanos != null) {
            System.out.println("🔍 Probando ConsultaCiudadanos...");
            boolean ciudadanosOk = consultaCiudadanos.verificarConexionBD(null);
            System.out.println("   Estado: " + (ciudadanosOk ? "✅ CONECTADO" : "❌ DESCONECTADO"));
        }
        
        // Test ProcesadorLoteVotos (debe usar BD de votos)
        if (procesadorLoteVotos != null) {
            System.out.println("🗳️  Probando ProcesadorLoteVotos...");
            boolean votosOk = procesadorLoteVotos.verificarDisponibilidad(null);
            System.out.println("   Estado: " + (votosOk ? "✅ CONECTADO" : "❌ DESCONECTADO"));
        }
        
        System.out.println("============================================\n");
    }
    
    private static void shutdown() {
        try {
            // Cerrar interfaz gráfica si está activa
            if (ui != null) {
                SwingUtilities.invokeLater(() -> {
                    ui.dispose();
                    System.out.println("✅ Interfaz gráfica cerrada");
                });
            }
            
            // Detener el Broker Nacional
            if (brokerNacional != null) {
                brokerNacional.shutdown();
            }
            
            // Cerrar conexiones de base de datos
            if (consultaMesa != null) {
                consultaMesa.shutdown();
            }
            
            // Cerrar servicio de consulta de ciudadanos
            if (consultaCiudadanos != null) {
                consultaCiudadanos.shutdown();
            }
            
            // Cerrar procesador de lote de votos
            if (procesadorLoteVotos != null) {
                procesadorLoteVotos.shutdown();
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
    
    // Método para obtener ConsultaMesa (útil para testing)
    public static ConsultaMesaImpl getConsultaMesa() {
        return consultaMesa;
    }
    
    // Método para obtener ConsultaCiudadanos (útil para testing)
    public static ConsultaCiudadanosImpl getConsultaCiudadanos() {
        return consultaCiudadanos;
    }
    
    // Método para obtener el communicator (útil para testing)
    public static Communicator getCommunicator() {
        return communicator;
    }
    
    // Método para obtener el configuration manager (útil para testing)
    public static ConfigManager getConfigurationManager() {
        return configManager;
    }
    
    // Método para obtener la UI (útil para testing)
    public static ServidorNacionalUI getUI() {
        return ui;
    }
}