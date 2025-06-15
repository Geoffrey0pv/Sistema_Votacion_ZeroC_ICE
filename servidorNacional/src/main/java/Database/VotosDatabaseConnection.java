package Database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wrapper para el pool de conexiones de la base de datos de votos
 * Simplifica el uso del VotosConnectionPool para los servicios de votación
 */
public class VotosDatabaseConnection {
    
    private final VotosConnectionPool connectionPool;
    
    public VotosDatabaseConnection() {
        this.connectionPool = VotosConnectionPool.getInstance();
        System.out.println("🗳️  VotosDatabaseConnection inicializada");
    }
    
    /**
     * Obtiene una conexión de la base de datos de votos
     * @return Connection o null si el servicio está inactivo
     */
    public Connection getConnection() {
        try {
            return connectionPool.getConnection();
        } catch (SQLException e) {
            if ("SERVICIO_VOTOS_INACTIVO".equals(e.getMessage())) {
                System.err.println("❌ Servicio de base de datos de votos inactivo");
                return null;
            } else {
                System.err.println("❌ Error obteniendo conexión de votos: " + e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Devuelve una conexión al pool de votos
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
     * Verifica si el servicio de base de datos de votos está activo
     * @return true si está activo, false si no
     */
    public boolean isServiceActive() {
        return connectionPool.isServiceActive();
    }
    
    /**
     * Obtiene estadísticas del pool de conexiones de votos
     * @return String con estadísticas
     */
    public String getPoolStats() {
        return connectionPool.getPoolStats();
    }
} 