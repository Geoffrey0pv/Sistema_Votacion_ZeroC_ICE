package Database;

import Config.VotosConfigManager;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Conexión RÁPIDA y SIMPLE para la base de datos de Votos
 */
public class VotosDatabaseConnection {
    
    private final ConnectionPoolManager poolManager;
    private boolean isServiceActive = true;
    
    public VotosDatabaseConnection() {
        this.poolManager = ConnectionPoolManager.getInstance();
        System.out.println("🗳️ VotosDatabaseConnection RÁPIDA inicializada");
    }
    
    /**
     * Obtiene una conexión válida - RÁPIDO
     */
    public Connection getConnection() {
        if (!isServiceActive) {
            return null;
        }
        
        try {
            return poolManager.getVotosConnection();
        } catch (SQLException e) {
            System.err.println("❌ Error conexión votos: " + e.getMessage());
            return null;
        }
    }
    
    public void commit() {
        // Usar connection.commit() directamente
    }
    
    public void rollback() {
        // Usar connection.rollback() directamente
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
        return isServiceActive ? "🗳️ Votos: ACTIVA" : "❌ Votos: INACTIVA";
    }
} 