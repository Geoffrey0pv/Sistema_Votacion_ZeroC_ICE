package Database;

import Config.ConfigManager;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Conexión RÁPIDA y SIMPLE para la base de datos de Registraduría
 */
public class DatabaseConnection {
    
    private final ConnectionPoolManager poolManager;
    private boolean isServiceActive = true;
    
    public DatabaseConnection() {
        this.poolManager = ConnectionPoolManager.getInstance();
        System.out.println("🔗 DatabaseConnection RÁPIDA inicializada");
    }
    
    /**
     * Obtiene una conexión válida - RÁPIDO
     */
    public Connection getConnection() {
        if (!isServiceActive) {
            return null;
        }
        
        try {
            return poolManager.getRegistraduriaConnection();
        } catch (SQLException e) {
            System.err.println("❌ Error conexión registraduría: " + e.getMessage());
            return null;
        }
    }
    
    public boolean isServiceActive() {
        return isServiceActive;
    }
    
    public void reconnect() {
        // No hacer nada, el pool maneja esto
    }
    
    public void close() {
        isServiceActive = false;
    }
    
    public String getConnectionInfo() {
        return isServiceActive ? "🔗 Registraduría: ACTIVA" : "❌ Registraduría: INACTIVA";
    }
} 