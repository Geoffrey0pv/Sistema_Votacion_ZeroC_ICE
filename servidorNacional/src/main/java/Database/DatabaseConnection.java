package Database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wrapper para el pool de conexiones de base de datos
 * Simplifica el uso del ConnectionPool para los servicios
 */
public class DatabaseConnection {
    
    private final ConnectionPool connectionPool;
    
    public DatabaseConnection() {
        this.connectionPool = ConnectionPool.getInstance();
        System.out.println("🔗 DatabaseConnection inicializada");
    }
    
    /**
     * Obtiene una conexión de la base de datos
     * @return Connection o null si el servicio está inactivo
     */
    public Connection getConnection() {
        try {
            return connectionPool.getConnection();
        } catch (SQLException e) {
            if ("SERVICIO_INACTIVO".equals(e.getMessage())) {
                System.err.println("❌ Servicio de base de datos inactivo");
                return null;
            } else {
                System.err.println("❌ Error obteniendo conexión: " + e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Devuelve una conexión al pool
     * @param conn Conexión a devolver
     */
    public void returnConnection(Connection conn) {
        if (conn != null) {
            connectionPool.returnConnection(conn);
        }
    }
    
    /**
     * Cierra una conexión definitivamente
     * @param conn Conexión a cerrar
     */
    public void closeConnection(Connection conn) {
        if (conn != null) {
            connectionPool.closeConnection(conn);
        }
    }
    
    /**
     * Verifica si el servicio de base de datos está activo
     * @return true si está activo, false si no
     */
    public boolean isServiceActive() {
        return connectionPool.isServiceActive();
    }
    
    /**
     * Obtiene estadísticas del pool de conexiones
     * @return String con estadísticas
     */
    public String getPoolStats() {
        return connectionPool.getPoolStats();
    }
} 