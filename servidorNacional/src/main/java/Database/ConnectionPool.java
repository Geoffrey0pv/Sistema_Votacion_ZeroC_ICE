package Database;

import Config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool de conexiones para la base de datos PostgreSQL
 * Maneja conexiones persistentes con reintentos automáticos
 */
public class ConnectionPool {
    private static ConnectionPool instance;
    private final BlockingQueue<Connection> connectionPool;
    private final AtomicInteger currentConnections;
    private final AtomicBoolean isServiceActive;
    private final ConfigManager config;
    
    // Configuración del pool
    private final int minPoolSize;
    private final int maxPoolSize;
    private final int poolTimeout;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    
    // Configuración de reintentos
    private final int maxRetryAttempts;
    private final int retryDelayMs;
    private final double backoffMultiplier;
    
    private ConnectionPool() {
        this.config = ConfigManager.getInstance();
        this.connectionPool = new LinkedBlockingQueue<>();
        this.currentConnections = new AtomicInteger(0);
        this.isServiceActive = new AtomicBoolean(false);
        
        // Cargar configuración
        this.minPoolSize = config.getPoolMinSize();
        this.maxPoolSize = config.getPoolMaxSize();
        this.poolTimeout = config.getPoolTimeout();
        this.dbUrl = config.getDatabaseUrl();
        this.dbUser = config.getDatabaseUser();
        this.dbPassword = config.getDatabasePassword();
        
        this.maxRetryAttempts = config.getRetryMaxAttempts();
        this.retryDelayMs = config.getRetryDelayMs();
        this.backoffMultiplier = config.getRetryBackoffMultiplier();
        
        // Cargar driver PostgreSQL
        loadPostgreSQLDriver();
        
        // Inicializar pool
        initializePool();
    }
    
    public static synchronized ConnectionPool getInstance() {
        if (instance == null) {
            instance = new ConnectionPool();
        }
        return instance;
    }
    
    private void loadPostgreSQLDriver() {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver PostgreSQL cargado correctamente");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error cargando driver PostgreSQL: " + e.getMessage());
            System.err.println("   Asegúrese de que postgresql.jar esté en el classpath");
        }
    }
    
    private void initializePool() {
        System.out.println("🔧 Inicializando pool de conexiones...");
        System.out.println("   URL: " + dbUrl);
        System.out.println("   Pool: " + minPoolSize + "-" + maxPoolSize + " conexiones");
        
        // Intentar crear conexiones iniciales
        for (int i = 0; i < minPoolSize; i++) {
            try {
                Connection conn = createNewConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                    currentConnections.incrementAndGet();
                }
            } catch (Exception e) {
                System.err.println("⚠️  No se pudo crear conexión inicial " + (i + 1) + ": " + e.getMessage());
            }
        }
        
        // Verificar si el servicio está activo
        checkServiceStatus();
        
        if (isServiceActive.get()) {
            System.out.println("✅ Pool de conexiones inicializado: " + currentConnections.get() + " conexiones");
        } else {
            System.err.println("❌ Pool de conexiones no pudo inicializarse completamente");
            System.err.println("   El servicio se marcará como inactivo");
        }
    }
    
    private Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
    
    private void checkServiceStatus() {
        try (Connection testConn = createNewConnection()) {
            // Si llegamos aquí, la conexión es exitosa
            isServiceActive.set(true);
            System.out.println("✅ Servicio de base de datos activo");
        } catch (SQLException e) {
            isServiceActive.set(false);
            System.err.println("❌ Servicio de base de datos inactivo: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene una conexión del pool con reintentos automáticos
     */
    public Connection getConnection() throws SQLException {
        // Si el servicio está marcado como inactivo, intentar reactivarlo
        if (!isServiceActive.get()) {
            System.out.println("🔄 Intentando reactivar servicio de base de datos...");
            checkServiceStatus();
        }
        
        // Si sigue inactivo, lanzar excepción específica
        if (!isServiceActive.get()) {
            throw new SQLException("SERVICIO_INACTIVO");
        }
        
        Connection conn = null;
        int attempts = 0;
        int delay = retryDelayMs;
        
        while (attempts < maxRetryAttempts && conn == null) {
            attempts++;
            
            try {
                // Intentar obtener conexión del pool
                conn = connectionPool.poll(poolTimeout, TimeUnit.MILLISECONDS);
                
                if (conn == null || conn.isClosed()) {
                    // Si no hay conexiones disponibles o está cerrada, crear nueva
                    if (currentConnections.get() < maxPoolSize) {
                        conn = createNewConnection();
                        currentConnections.incrementAndGet();
                        System.out.println("🔗 Nueva conexión creada (total: " + currentConnections.get() + ")");
                    } else {
                        throw new SQLException("Pool de conexiones agotado");
                    }
                }
                
                // Verificar que la conexión esté válida
                if (conn != null && !conn.isClosed() && conn.isValid(5)) {
                    return conn;
                } else {
                    // Conexión inválida, descartar y reintentar
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                    currentConnections.decrementAndGet();
                    conn = null;
                }
                
            } catch (Exception e) {
                System.err.println("⚠️  Intento " + attempts + "/" + maxRetryAttempts + 
                                 " falló: " + e.getMessage());
                
                if (attempts < maxRetryAttempts) {
                    try {
                        System.out.println("⏳ Esperando " + delay + "ms antes del siguiente intento...");
                        Thread.sleep(delay);
                        delay = (int) (delay * backoffMultiplier);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrumpido durante reintento", ie);
                    }
                }
            }
        }
        
        // Si llegamos aquí, todos los intentos fallaron
        isServiceActive.set(false);
        throw new SQLException("SERVICIO_INACTIVO");
    }
    
    /**
     * Devuelve una conexión al pool
     */
    public void returnConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(1)) {
                    connectionPool.offer(conn);
                } else {
                    conn.close();
                    currentConnections.decrementAndGet();
                }
            } catch (SQLException e) {
                System.err.println("⚠️  Error devolviendo conexión al pool: " + e.getMessage());
                currentConnections.decrementAndGet();
            }
        }
    }
    
    /**
     * Cierra una conexión y la remueve del pool
     */
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                currentConnections.decrementAndGet();
            } catch (SQLException e) {
                System.err.println("⚠️  Error cerrando conexión: " + e.getMessage());
            }
        }
    }
    
    /**
     * Verifica si el servicio está activo
     */
    public boolean isServiceActive() {
        return isServiceActive.get();
    }
    
    /**
     * Obtiene estadísticas del pool
     */
    public String getPoolStats() {
        return String.format("Pool: %d/%d conexiones, Servicio: %s", 
                           connectionPool.size(), 
                           currentConnections.get(),
                           isServiceActive.get() ? "ACTIVO" : "INACTIVO");
    }
    
    /**
     * Cierra todas las conexiones del pool
     */
    public void shutdown() {
        System.out.println("🔄 Cerrando pool de conexiones...");
        
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("⚠️  Error cerrando conexión durante shutdown: " + e.getMessage());
            }
        }
        
        currentConnections.set(0);
        isServiceActive.set(false);
        System.out.println("✅ Pool de conexiones cerrado");
    }
} 