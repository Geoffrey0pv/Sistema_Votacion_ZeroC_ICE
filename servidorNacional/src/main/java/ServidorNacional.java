import Demo.*;
import Broker.BrokerNacional;
import ConsultaMesa.ConsultaMesaImpl;
import ConsultaCiudadanos.ConsultaCiudadanosImpl;
import ConsultaCandidatos.ConsultaCandidatosImpl;
import RegistroVotos.RegistroVotosImpl;
import ReplicaInfo.ReplicaInfoImpl;
import ServidorNacionalUI.ServidorNacionalUI;
import Config.ConfigManager;
import Services.ProcesadorLoteVotosImpl;
import Services.ElectoralReportService;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Util;
import Config.HostConfig;

import java.lang.Exception;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

public class ServidorNacional {
    private static BrokerNacional brokerNacional;
    private static ConsultaMesaImpl consultaMesa;
    private static ConsultaCiudadanosImpl consultaCiudadanos;
    private static ConsultaCandidatosImpl consultaCandidatos;
    private static RegistroVotosImpl registroVotos;
    private static ProcesadorLoteVotosImpl procesadorLoteVotos;
    private static ReplicaInfoImpl replicaInfo;
    private static Communicator communicator;
    private static ObjectAdapter adapter;
    private static ConfigManager configManager;
    private static ServidorNacionalUI ui;
    private static ElectoralReportService reportService;
    private static boolean useUI = false;
    private static ExecutorService commandExecutor;
    private static volatile boolean serverRunning = true;
    private static HostConfig hostConfig;

    public static void main(String[] args) {
        int status = 0;

        try {
            // Verificar si se debe usar la interfaz gráfica
            useUI = checkForUIParameter(args);
            
            // Inicializar configuración
            configManager = ConfigManager.getInstance();
            System.out.println("✅ Configuración cargada correctamente");
            
            // Inicializar servicio de reportes electorales
            reportService = new ElectoralReportService();
            System.out.println("✅ Servicio de reportes electorales inicializado");
            
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Configurar propiedades de réplica para ReplicaInfo
            com.zeroc.Ice.Properties properties = communicator.getProperties();
            
            // Leer propiedades del sistema y configurarlas en ICE
            String replicaId = System.getProperty("Replica.Id");
            String replicaPort = System.getProperty("Replica.Port");
            
            if (replicaId != null && !replicaId.isEmpty()) {
                properties.setProperty("Replica.Id", replicaId);
                System.out.println("🔧 Réplica ID configurado: " + replicaId);
            } else if (properties.getProperty("Replica.Id").isEmpty()) {
                properties.setProperty("Replica.Id", "nacional-master");
                System.out.println("🔧 Réplica ID por defecto: nacional-master");
            }
            
            if (replicaPort != null && !replicaPort.isEmpty()) {
                properties.setProperty("Replica.Port", replicaPort);
                System.out.println("🔧 Puerto de réplica configurado: " + replicaPort);
            } else if (properties.getProperty("Replica.Port").isEmpty()) {
                properties.setProperty("Replica.Port", "9090");
                System.out.println("🔧 Puerto de réplica por defecto: 9090");
            }
            
            // Configurar otras propiedades del sistema en ICE
            String[] systemProps = {
                "ReliableQueue.BaseDir", "ReliableQueue.ProcessingInterval", "ReliableQueue.BatchSize",
                "ReliableQueue.MaxRetries", "ReliableQueue.RetryDelay", "ReliableQueue.ProcessingThreads",
                "ReliableQueue.SchedulerThreads", "ReliableQueue.PersistenceEnabled", "ReliableQueue.AutoCleanup",
                "ReliableQueue.CleanupInterval", "ReliableQueue.MaxProcessedFiles", "ReliableQueue.LogLevel",
                "ReliableQueue.LogStatistics", "ReliableQueue.StatisticsInterval", "VotosDB.ConnectionTimeout",
                "VotosDB.MaxRetries", "VotosDB.RetryInterval"
            };
            
            for (String prop : systemProps) {
                String value = System.getProperty(prop);
                if (value != null && !value.isEmpty()) {
                    properties.setProperty(prop, value);
                }
            }
            
            // Inicializar configuración de hosts
            hostConfig = HostConfig.getInstance();
            hostConfig.printConfiguration();
            
            // Determinar el puerto del adaptador basado en la configuración
            String adapterPort = properties.getProperty("Replica.Port");
            String adapterEndpoint = hostConfig.getNacionalAdapterEndpoints();
            
            // Si se especifica un puerto específico, usarlo
            if (adapterPort != null && !adapterPort.isEmpty()) {
                adapterEndpoint = "tcp -h " + hostConfig.getNacionalHost() + " -p " + adapterPort;
            }
            
            // Crear adaptador
            adapter = communicator.createObjectAdapterWithEndpoints(
                "ServidorNacionalAdapter", adapterEndpoint
            );
            
            // Crear e inicializar el Broker Nacional
            brokerNacional = new BrokerNacional(communicator);
            
            
            // Crear e inicializar ConsultaMesa con configuración
            consultaMesa = new ConsultaMesaImpl();
            
            // Crear e inicializar ConsultaCiudadanos con configuración
            consultaCiudadanos = new ConsultaCiudadanosImpl();
            
            // Crear e inicializar ConsultaCandidatos con configuración
            consultaCandidatos = new ConsultaCandidatosImpl();
            
            // Crear e inicializar RegistroVotos con configuración
            registroVotos = new RegistroVotosImpl();
            
            // Crear e inicializar ProcesadorLoteVotos
            procesadorLoteVotos = new ProcesadorLoteVotosImpl();
            
            // Crear e inicializar ReplicaInfo
            replicaInfo = new ReplicaInfoImpl(communicator.getProperties());
            
            // Registrar el Broker como IAdministradorCandidatos (compatibilidad hacia atrás)
            Identity candidatosId = Util.stringToIdentity("AdministradorCandidatos");
            adapter.add(brokerNacional, candidatosId);
            
            // Registrar el Broker como IBrokerNacional (nueva funcionalidad)
            Identity brokerId = Util.stringToIdentity("BrokerNacional");
            adapter.add(brokerNacional, brokerId);
            
            
            // Registrar ConsultaMesa endpoint
            Identity consultaMesaId = Util.stringToIdentity("ConsultaMesa");
            adapter.add(consultaMesa, consultaMesaId);
            
            // Registrar ConsultaCiudadanos endpoint
            Identity consultaCiudadanosId = Util.stringToIdentity("ConsultaCiudadanos");
            adapter.add(consultaCiudadanos, consultaCiudadanosId);
            
            // Registrar ConsultaCandidatos endpoint
            Identity consultaCandidatosId = Util.stringToIdentity("ConsultaCandidatos");
            adapter.add(consultaCandidatos, consultaCandidatosId);
            
            // Registrar RegistroVotos endpoint
            Identity registroVotosId = Util.stringToIdentity("RegistroVotos");
            adapter.add(registroVotos, registroVotosId);
            
            // Registrar ProcesadorLoteVotos endpoint
            Identity procesadorLoteVotosId = Util.stringToIdentity("ProcesadorLoteVotos");
            adapter.add(procesadorLoteVotos, procesadorLoteVotosId);
            
            // Registrar ReplicaInfo endpoint
            Identity replicaInfoId = Util.stringToIdentity("ReplicaInfo");
            adapter.add(replicaInfo, replicaInfoId);
            
            // Activar adaptador
            adapter.activate();
            
            System.out.println("🎯 ===== SERVIDOR NACIONAL CON BROKER =====");
            System.out.println("   🚀 Servidor iniciado con patrón Broker");
            System.out.println("   📡 Endpoint: " + hostConfig.getNacionalEndpoint());
            System.out.println("   🔄 Escalado automático: ACTIVADO (50%)");
            System.out.println("   ⚖️  Balanceador de carga: ACTIVADO");
            System.out.println("   📊 Monitor de recursos: ACTIVADO");
            System.out.println("   🗄️  Base de datos: " + configManager.getDatabaseUrl());
            System.out.println("==========================================");
            System.out.println("   🎮 Servicios disponibles:");
            System.out.println("   • AdministradorCandidatos (compatibilidad)");
            System.out.println("   • BrokerNacional (nueva funcionalidad)");
            System.out.println("   • ConsultaMesa (consulta por documento) 🔍");
            System.out.println("   • ConsultaCiudadanos (consulta por ciudadano) 🌍");
            System.out.println("   • ConsultaCandidatos (consulta candidatos electorales) 🗳️");
            System.out.println("   • RegistroVotos (registro de votos) 📝");
            System.out.println("   • ProcesadorLoteVotos (procesamiento de votos) 🗳️");
            System.out.println("   • ReplicaInfo (información de ejecución de réplica) 📋");
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
            System.out.println("📝 ===== COMANDOS DISPONIBLES =====");
            System.out.println("   • CERRAR REPORTES Y GENERAR - Cierra jornada y genera reportes");
            System.out.println("   • CERRAR JORNADA - Solo cierra la jornada electoral");
            System.out.println("   • GENERAR REPORTES - Solo genera reportes (requiere jornada cerrada)");
            System.out.println("   • ESTADISTICAS - Muestra estadísticas actuales");
            System.out.println("   • AYUDA - Muestra esta lista de comandos");
            System.out.println("   • SALIR - Detiene el servidor");
            System.out.println("=====================================");
            System.out.println();
            
            // Test de conexiones de base de datos
            testDatabaseConnections();
            
            // Inicializar executor para comandos
            commandExecutor = Executors.newSingleThreadExecutor();
            
            // Configurar shutdown hook para limpieza
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Deteniendo servidor...");
                serverRunning = false;
                shutdown();
            }));
            
            // Iniciar el procesador de comandos por consola
            startConsoleCommandProcessor();
            
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
     * Inicia el procesador de comandos por consola
     */
    private static void startConsoleCommandProcessor() {
        Thread consoleThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            
            while (serverRunning) {
                try {
                    System.out.print("🎯 ServidorNacional> ");
                    
                    if (scanner.hasNextLine()) {
                        String command = scanner.nextLine().trim().toUpperCase();
                        
                        if (!command.isEmpty()) {
                            processCommand(command);
                        }
                    }
                    
                    // Pequeña pausa para evitar consumo excesivo de CPU
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    if (serverRunning) {
                        System.err.println("❌ Error procesando comando: " + e.getMessage());
                    }
                }
            }
            
            scanner.close();
        });
        
        consoleThread.setDaemon(true);
        consoleThread.setName("ConsoleCommandProcessor");
        consoleThread.start();
        
        System.out.println("✅ Procesador de comandos por consola iniciado");
        System.out.println("💡 Escriba 'AYUDA' para ver los comandos disponibles");
    }
    
    /**
     * Procesa un comando ingresado por consola
     */
    private static void processCommand(String command) {
        commandExecutor.submit(() -> {
            try {
                switch (command) {
                    case "CERRAR REPORTES Y GENERAR":
                    case "CERRAR Y GENERAR":
                        executeCloseAndGenerateReports();
                        break;
                        
                    case "CERRAR JORNADA":
                    case "CERRAR":
                        executeCloseElectoralDay();
                        break;
                        
                    case "GENERAR REPORTES":
                    case "REPORTES":
                        executeGenerateReports();
                        break;
                        
                    case "ESTADISTICAS":
                    case "STATS":
                        showStatistics();
                        break;
                        
                    case "AYUDA":
                    case "HELP":
                        showHelp();
                        break;
                        
                    case "SALIR":
                    case "EXIT":
                    case "QUIT":
                        executeExit();
                        break;
                        
                    default:
                        System.out.println("❌ Comando no reconocido: " + command);
                        System.out.println("💡 Escriba 'AYUDA' para ver los comandos disponibles");
                        break;
                }
            } catch (Exception e) {
                System.err.println("❌ Error ejecutando comando '" + command + "': " + e.getMessage());
            }
        });
    }
    
    /**
     * Ejecuta el comando para cerrar jornada y generar reportes
     */
    private static void executeCloseAndGenerateReports() {
        System.out.println("\n🚀 ===== CERRANDO JORNADA Y GENERANDO REPORTES =====");
        
        try {
            // Primero cerrar la jornada si no está cerrada
            if (!reportService.isJornadaCerrada()) {
                System.out.println("🔒 Cerrando jornada electoral...");
                boolean closed = reportService.cerrarJornada();
                if (!closed) {
                    System.err.println("❌ No se pudo cerrar la jornada electoral");
                    return;
                }
                System.out.println("✅ Jornada cerrada exitosamente");
            } else {
                System.out.println("ℹ️  La jornada ya estaba cerrada");
            }
            
            // Luego generar reportes
            System.out.println("📄 Generando reportes CSV...");
            ElectoralReportService.ReportResult result = reportService.generateAllReports();
            
            if (result.success) {
                System.out.println("\n🎉 ¡PROCESO COMPLETADO EXITOSAMENTE!");
                System.out.println("✅ Jornada electoral cerrada");
                System.out.println("✅ Reportes generados");
                System.out.println("📁 Directorio: " + result.reportDirectory);
                System.out.println("📄 Archivos generados: " + result.filesGenerated);
                System.out.println("\nArchivos disponibles:");
                System.out.println("• resume.csv - Reporte general con todos los resultados");
                System.out.println("• partial-{mesaId}.csv - Reportes individuales por mesa");
                System.out.println("Formato: candidateId,candidateName,totalVotes");
            } else {
                System.err.println("❌ Error generando reportes: " + result.message);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en el proceso: " + e.getMessage());
        }
        
        System.out.println("====================================================\n");
    }
    
    /**
     * Ejecuta el comando para cerrar solo la jornada electoral
     */
    private static void executeCloseElectoralDay() {
        System.out.println("\n🔒 ===== CERRANDO JORNADA ELECTORAL =====");
        
        try {
            if (reportService.isJornadaCerrada()) {
                System.out.println("ℹ️  La jornada electoral ya está cerrada");
                System.out.println("📅 Fecha de cierre: " + reportService.getFechaCierre());
            } else {
                boolean closed = reportService.cerrarJornada();
                if (closed) {
                    System.out.println("✅ Jornada electoral cerrada exitosamente");
                    System.out.println("📅 Fecha de cierre: " + reportService.getFechaCierre());
                } else {
                    System.err.println("❌ No se pudo cerrar la jornada electoral");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error cerrando jornada: " + e.getMessage());
        }
        
        System.out.println("========================================\n");
    }
    
    /**
     * Ejecuta el comando para generar solo los reportes
     */
    private static void executeGenerateReports() {
        System.out.println("\n📄 ===== GENERANDO REPORTES =====");
        
        try {
            if (!reportService.isJornadaCerrada()) {
                System.err.println("❌ La jornada electoral debe estar cerrada para generar reportes");
                System.out.println("💡 Use 'CERRAR JORNADA' primero o 'CERRAR REPORTES Y GENERAR'");
                return;
            }
            
            ElectoralReportService.ReportResult result = reportService.generateAllReports();
            
            if (result.success) {
                System.out.println("✅ Reportes generados exitosamente");
                System.out.println("📁 Directorio: " + result.reportDirectory);
                System.out.println("📄 Archivos generados: " + result.filesGenerated);
                System.out.println("\nArchivos disponibles:");
                System.out.println("• resume.csv - Reporte general");
                System.out.println("• partial-{mesaId}.csv - Reportes por mesa");
            } else {
                System.err.println("❌ Error generando reportes: " + result.message);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error generando reportes: " + e.getMessage());
        }
        
        System.out.println("=================================\n");
    }
    
    /**
     * Muestra las estadísticas actuales
     */
    private static void showStatistics() {
        System.out.println("\n📊 ===== ESTADÍSTICAS ACTUALES =====");
        
        try {
            ElectoralReportService.JornadaStats stats = reportService.getJornadaStats();
            
            System.out.println("🗳️  Total de votos: " + stats.totalVotos);
            System.out.println("🏛️  Total de mesas: " + stats.totalMesas);
            System.out.println("👥 Total de candidatos: " + stats.totalCandidatos);
            System.out.println("📅 Estado de jornada: " + (stats.jornadaCerrada ? "🔒 CERRADA" : "🔓 ABIERTA"));
            
            if (stats.primerVoto != null) {
                System.out.println("⏰ Primer voto: " + stats.primerVoto);
            }
            if (stats.ultimoVoto != null) {
                System.out.println("⏰ Último voto: " + stats.ultimoVoto);
            }
            if (stats.fechaCierre != null) {
                System.out.println("🔒 Fecha de cierre: " + stats.fechaCierre);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo estadísticas: " + e.getMessage());
        }
        
        System.out.println("====================================\n");
    }
    
    /**
     * Muestra la ayuda con los comandos disponibles
     */
    private static void showHelp() {
        System.out.println("\n📝 ===== COMANDOS DISPONIBLES =====");
        System.out.println("• CERRAR REPORTES Y GENERAR - Cierra jornada y genera reportes");
        System.out.println("• CERRAR JORNADA - Solo cierra la jornada electoral");
        System.out.println("• GENERAR REPORTES - Solo genera reportes (requiere jornada cerrada)");
        System.out.println("• ESTADISTICAS - Muestra estadísticas actuales");
        System.out.println("• AYUDA - Muestra esta lista de comandos");
        System.out.println("• SALIR - Detiene el servidor");
        System.out.println("\n💡 Aliases disponibles:");
        System.out.println("• CERRAR Y GENERAR = CERRAR REPORTES Y GENERAR");
        System.out.println("• CERRAR = CERRAR JORNADA");
        System.out.println("• REPORTES = GENERAR REPORTES");
        System.out.println("• STATS = ESTADISTICAS");
        System.out.println("• HELP = AYUDA");
        System.out.println("• EXIT, QUIT = SALIR");
        System.out.println("===================================\n");
    }
    
    /**
     * Ejecuta el comando para salir del servidor
     */
    private static void executeExit() {
        System.out.println("\n🛑 Iniciando cierre del servidor...");
        serverRunning = false;
        
        if (communicator != null) {
            communicator.shutdown();
        }
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
        
        // Test ConsultaCandidatos (debe usar BD de votos)
        if (consultaCandidatos != null) {
            System.out.println("🗳️  Probando ConsultaCandidatos...");
            boolean candidatosOk = consultaCandidatos.verificarConexionBD(null);
            System.out.println("   Estado: " + (candidatosOk ? "✅ CONECTADO" : "❌ DESCONECTADO"));
        }
        
        // Test RegistroVotos (debe usar BD de votos)
        if (registroVotos != null) {
            System.out.println("📝 Probando RegistroVotos...");
            boolean registroOk = registroVotos.verificarConexionBD(null);
            System.out.println("   Estado: " + (registroOk ? "✅ CONECTADO" : "❌ DESCONECTADO"));
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
            // Detener el procesador de comandos
            if (commandExecutor != null && !commandExecutor.isShutdown()) {
                commandExecutor.shutdown();
            }
            
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
            
            // Cerrar servicio de consulta de candidatos
            if (consultaCandidatos != null) {
                consultaCandidatos.shutdown();
            }
            
            // Cerrar servicio de registro de votos
            if (registroVotos != null) {
                registroVotos.shutdown();
            }
            
            // Cerrar procesador de lote de votos
            if (procesadorLoteVotos != null) {
                procesadorLoteVotos.shutdown();
            }
            
            // ReplicaInfo no necesita shutdown explícito
            
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
        
    // Método para obtener ConsultaMesa (útil para testing)
    public static ConsultaMesaImpl getConsultaMesa() {
        return consultaMesa;
    }
    
    // Método para obtener ConsultaCiudadanos (útil para testing)
    public static ConsultaCiudadanosImpl getConsultaCiudadanos() {
        return consultaCiudadanos;
    }
    
    // Método para obtener ConsultaCandidatos (útil para testing)
    public static ConsultaCandidatosImpl getConsultaCandidatos() {
        return consultaCandidatos;
    }
    
    // Método para obtener RegistroVotos (útil para testing)
    public static RegistroVotosImpl getRegistroVotos() {
        return registroVotos;
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