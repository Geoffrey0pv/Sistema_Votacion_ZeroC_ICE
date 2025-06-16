package Database;

/**
 * Gestor centralizado para las conexiones de base de datos
 * Maneja tanto la base de datos de Registraduría como la de Votos
 */
public class DatabaseManager {
    
    private static DatabaseManager instance;
    private final DatabaseConnection registraduriaConnection;
    private final VotosDatabaseConnection votosConnection;
    
    private DatabaseManager() {
        System.out.println("🚀 Inicializando DatabaseManager...");
        
        // Inicializar conexiones
        this.registraduriaConnection = new DatabaseConnection();
        this.votosConnection = new VotosDatabaseConnection();
        
        System.out.println("✅ DatabaseManager inicializado correctamente");
        System.out.println("📊 Estado de conexiones:");
        System.out.println("   " + registraduriaConnection.getConnectionInfo());
        System.out.println("   " + votosConnection.getConnectionInfo());
    }
    
    /**
     * Obtiene la instancia singleton del DatabaseManager
     * @return Instancia única del DatabaseManager
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * Obtiene la conexión a la base de datos de Registraduría
     * @return DatabaseConnection para consultas de registraduría
     */
    public DatabaseConnection getRegistraduriaConnection() {
        return registraduriaConnection;
    }
    
    /**
     * Obtiene la conexión a la base de datos de Votos
     * @return VotosDatabaseConnection para operaciones de votación
     */
    public VotosDatabaseConnection getVotosConnection() {
        return votosConnection;
    }
    
    /**
     * Verifica el estado de todas las conexiones
     * @return true si ambas conexiones están activas
     */
    public boolean areAllConnectionsActive() {
        return registraduriaConnection.isServiceActive() && 
               votosConnection.isServiceActive();
    }
    
    /**
     * Fuerza la reconexión de todas las bases de datos
     */
    public void reconnectAll() {
        System.out.println("🔄 Reconectando todas las bases de datos...");
        registraduriaConnection.reconnect();
        votosConnection.reconnect();
        
        System.out.println("📊 Estado después de reconexión:");
        System.out.println("   " + registraduriaConnection.getConnectionInfo());
        System.out.println("   " + votosConnection.getConnectionInfo());
    }
    
    /**
     * Obtiene un reporte completo del estado de las conexiones
     * @return String con información detallada de todas las conexiones
     */
    public String getConnectionsReport() {
        StringBuilder report = new StringBuilder();
        report.append("📊 REPORTE DE CONEXIONES DE BASE DE DATOS\n");
        report.append("==========================================\n");
        report.append(registraduriaConnection.getConnectionInfo()).append("\n");
        report.append(votosConnection.getConnectionInfo()).append("\n");
        report.append("==========================================\n");
        report.append("Estado General: ");
        
        if (areAllConnectionsActive()) {
            report.append("✅ TODAS LAS CONEXIONES ACTIVAS");
        } else {
            report.append("⚠️ ALGUNAS CONEXIONES INACTIVAS");
        }
        
        return report.toString();
    }
    
    /**
     * Cierra todas las conexiones de base de datos
     */
    public void closeAllConnections() {
        System.out.println("🔒 Cerrando todas las conexiones de base de datos...");
        registraduriaConnection.close();
        votosConnection.close();
        System.out.println("✅ Todas las conexiones cerradas");
    }
    
    /**
     * Método para limpiar recursos al cerrar la aplicación
     */
    public void shutdown() {
        closeAllConnections();
        instance = null;
        System.out.println("🛑 DatabaseManager cerrado");
    }
} 