package ConsultaMesa;

import Demo.*;
import Config.ConfigManager;
import Database.ConnectionPool;
import com.zeroc.Ice.Current;
import java.sql.*;

/**
 * Implementación mejorada del servicio ConsultaMesa
 * - Usa pool de conexiones persistentes
 * - Implementa reintentos automáticos
 * - Maneja estados de servicio inactivo
 * - Configuración centralizada
 */
public class ConsultaMesaImpl implements IConsultaMesa {
    
    private final ConfigManager config;
    private final ConnectionPool connectionPool;
    
    // Query SQL para consultar mesa por documento
    private static final String QUERY_MESA_POR_DOCUMENTO = 
        "SELECT d.nombre AS departamento, " +
        "       m.nombre AS municipio, " +
        "       pv.nombre AS puesto, " +
        "       mv.consecutive AS mesa " +
        "FROM ciudadano c " +
        "JOIN mesa_votacion mv ON mv.id = c.mesa_id " +
        "JOIN puesto_votacion pv ON pv.id = mv.puesto_id " +
        "JOIN municipio m ON m.id = pv.municipio_id " +
        "JOIN departamento d ON d.id = m.departamento_id " +
        "WHERE c.documento = ?";
    
    public ConsultaMesaImpl() {
        System.out.println("🔧 Inicializando ConsultaMesa con pool de conexiones...");
        
        // Cargar configuración
        this.config = ConfigManager.getInstance();
        
        // Obtener pool de conexiones
        this.connectionPool = ConnectionPool.getInstance();
        
        // Mostrar configuración
        config.printConfiguration();
        
        System.out.println("✅ ConsultaMesa inicializado correctamente");
        System.out.println("   " + connectionPool.getPoolStats());
    }
    
    @Override
    public MesaInfo consultarMesaPorDocumento(String documento, Current current) {
        System.out.println("🔍 Consultando mesa para documento: " + documento);
        
        // Verificar si el servicio está activo
        if (!connectionPool.isServiceActive()) {
            System.err.println("🚫 Servicio de base de datos inactivo");
            return createServiceInactiveResponse();
        }
        
        Connection conn = null;
        try {
            // Obtener conexión del pool (con reintentos automáticos)
            conn = connectionPool.getConnection();
            
            // Configurar timeout para la consulta
            try (PreparedStatement stmt = conn.prepareStatement(QUERY_MESA_POR_DOCUMENTO)) {
                stmt.setQueryTimeout(config.getQueryTimeout() / 1000); // Convertir a segundos
                stmt.setString(1, documento);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        MesaInfo mesaInfo = new MesaInfo();
                        mesaInfo.departamento = rs.getString("departamento");
                        mesaInfo.municipio = rs.getString("municipio");
                        mesaInfo.puesto = rs.getString("puesto");
                        mesaInfo.mesa = rs.getString("mesa");
                        
                        System.out.println("✅ Mesa encontrada: " + 
                            mesaInfo.departamento + " - " + 
                            mesaInfo.municipio + " - " + 
                            mesaInfo.puesto + " - Mesa: " + 
                            mesaInfo.mesa);
                        
                        return mesaInfo;
                    } else {
                        System.out.println("⚠️  No se encontró mesa para el documento: " + documento);
                        return createNotFoundResponse();
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error consultando base de datos: " + e.getMessage());
            
            // Verificar si es un error de servicio inactivo
            if ("SERVICIO_INACTIVO".equals(e.getMessage())) {
                return createServiceInactiveResponse();
            }
            
            // Para otros errores SQL, devolver error específico
            return createErrorResponse("Error de consulta: " + e.getMessage());
            
        } finally {
            // Devolver conexión al pool
            if (conn != null) {
                connectionPool.returnConnection(conn);
            }
        }
    }
    
    @Override
    public boolean verificarConexionBD(Current current) {
        System.out.println("🔧 Verificando conexión a base de datos...");
        System.out.println("   " + connectionPool.getPoolStats());
        
        if (!connectionPool.isServiceActive()) {
            System.err.println("❌ Servicio marcado como inactivo");
            return false;
        }
        
        Connection conn = null;
        try {
            // Intentar obtener conexión del pool
            conn = connectionPool.getConnection();
            
            // Ejecutar una consulta simple para verificar la conexión
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 as test")) {
                stmt.setQueryTimeout(5); // 5 segundos timeout
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("test") == 1) {
                        System.out.println("✅ Conexión a base de datos OK");
                        System.out.println("   " + connectionPool.getPoolStats());
                        return true;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error verificando conexión: " + e.getMessage());
            
            if ("SERVICIO_INACTIVO".equals(e.getMessage())) {
                System.err.println("🚫 Servicio de base de datos inactivo");
            }
            
            return false;
            
        } finally {
            // Devolver conexión al pool
            if (conn != null) {
                connectionPool.returnConnection(conn);
            }
        }
        
        return false;
    }
    
    /**
     * Crea una respuesta para cuando el servicio está inactivo
     */
    private MesaInfo createServiceInactiveResponse() {
        MesaInfo mesaInfo = new MesaInfo();
        mesaInfo.departamento = "SERVICIO_INACTIVO";
        mesaInfo.municipio = config.getServiceInactiveMessage();
        mesaInfo.puesto = "";
        mesaInfo.mesa = "";
        return mesaInfo;
    }
    
    /**
     * Crea una respuesta para cuando no se encuentra el documento
     */
    private MesaInfo createNotFoundResponse() {
        MesaInfo mesaInfo = new MesaInfo();
        mesaInfo.departamento = "";
        mesaInfo.municipio = "";
        mesaInfo.puesto = "";
        mesaInfo.mesa = "";
        return mesaInfo;
    }
    
    /**
     * Crea una respuesta de error específico
     */
    private MesaInfo createErrorResponse(String errorMessage) {
        MesaInfo mesaInfo = new MesaInfo();
        mesaInfo.departamento = "ERROR";
        mesaInfo.municipio = errorMessage;
        mesaInfo.puesto = "";
        mesaInfo.mesa = "";
        return mesaInfo;
    }
    
    /**
     * Método para obtener estadísticas del servicio
     */
    public String getServiceStats() {
        return String.format("ConsultaMesa - %s, Timeout: %dms", 
                           connectionPool.getPoolStats(),
                           config.getQueryTimeout());
    }
    
    /**
     * Método para cerrar conexiones y limpiar recursos
     */
    public void shutdown() {
        if (connectionPool != null) {
            connectionPool.shutdown();
            System.out.println("✅ Pool de conexiones cerrado correctamente");
        }
    }
} 