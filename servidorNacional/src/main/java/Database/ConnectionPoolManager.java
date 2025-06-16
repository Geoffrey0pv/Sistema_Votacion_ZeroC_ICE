package Database;

import Config.ConfigManager;
import Config.VotosConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool de Conexiones Simplificado y Rápido
 * Sin monitoreo complejo, solo conexiones básicas y eficientes
 */
public class ConnectionPoolManager {
    
    private static ConnectionPoolManager instance;
    private static final Object lock = new Object();
    
    // Pools simples
    private final ConcurrentLinkedQueue<Connection> registraduriaPool;
    private final ConcurrentLinkedQueue<Connection> votosPool;
    
    // Configuraciones básicas
    private final String registraduriaUrl;
    private final String registraduriaUser;
    private final String registraduriaPassword;
    private final String votosUrl;
    private final String votosUser;
    private final String votosPassword;
    
    // Contadores simples
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    
    private ConnectionPoolManager() {
        // Cargar configuraciones básicas
        ConfigManager config = ConfigManager.getInstance();
        VotosConfigManager votosConfig = VotosConfigManager.getInstance();
        
        // URLs simples sin parámetros complejos
        this.registraduriaUrl = config.getDatabaseUrl();
        this.registraduriaUser = config.getDatabaseUser();
        this.registraduriaPassword = config.getDatabasePassword();
        
        this.votosUrl = votosConfig.getVotosDatabaseUrl();
        this.votosUser = votosConfig.getVotosDatabaseUser();
        this.votosPassword = votosConfig.getVotosDatabasePassword();
        
        // Pools simples
        this.registraduriaPool = new ConcurrentLinkedQueue<>();
        this.votosPool = new ConcurrentLinkedQueue<>();
        
        // Cargar driver una sola vez
        loadDriver();
        
        System.out.println("🚀 ConnectionPool RÁPIDO inicializado");
    }
    
    public static ConnectionPoolManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ConnectionPoolManager();
                }
            }
        }
        return instance;
    }
    
    private void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver PostgreSQL no encontrado", e);
        }
    }
    
    /**
     * Obtiene conexión de registraduría - RÁPIDO
     */
    public Connection getRegistraduriaConnection() throws SQLException {
        Connection conn = registraduriaPool.poll();
        
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(registraduriaUrl, registraduriaUser, registraduriaPassword);
            conn.setAutoCommit(true);
            activeConnections.incrementAndGet();
            totalCreated.incrementAndGet();
        }
        
        return conn;
    }
    
    /**
     * Obtiene conexión de votos - RÁPIDO
     */
    public Connection getVotosConnection() throws SQLException {
        Connection conn = votosPool.poll();
        
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(votosUrl, votosUser, votosPassword);
            conn.setAutoCommit(false);
            activeConnections.incrementAndGet();
            totalCreated.incrementAndGet();
        }
        
        return conn;
    }
    
    /**
     * Devuelve conexión al pool - SIMPLE
     */
    public void returnRegistraduriaConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    registraduriaPool.offer(conn);
                }
            } catch (SQLException e) {
                // Ignorar errores al devolver
            }
        }
    }
    
    /**
     * Devuelve conexión al pool - SIMPLE
     */
    public void returnVotosConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    votosPool.offer(conn);
                }
            } catch (SQLException e) {
                // Ignorar errores al devolver
            }
        }
    }
    
    /**
     * Estadísticas básicas
     */
    public String getPoolStatistics() {
        return String.format(
            "📊 Pool: %d activas, %d creadas, Reg:%d, Votos:%d",
            activeConnections.get(),
            totalCreated.get(),
            registraduriaPool.size(),
            votosPool.size()
        );
    }
    
    /**
     * Cierre simple
     */
    public void shutdown() {
        // Cerrar conexiones en pools
        Connection conn;
        while ((conn = registraduriaPool.poll()) != null) {
            try { conn.close(); } catch (SQLException e) { /* ignorar */ }
        }
        while ((conn = votosPool.poll()) != null) {
            try { conn.close(); } catch (SQLException e) { /* ignorar */ }
        }
        
        System.out.println("🛑 ConnectionPool cerrado");
    }
} 