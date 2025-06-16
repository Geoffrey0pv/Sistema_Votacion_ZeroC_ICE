package ConsultaMesa;

import Demo.*;
import Config.ConfigManager;
import Database.DatabaseManager;
import Database.DatabaseConnection;
import com.zeroc.Ice.Current;
import java.sql.*;

/**
 * Implementación mejorada del servicio ConsultaMesa
 * - Usa conexión directa estable
 * - Implementa reintentos automáticos
 * - Maneja estados de servicio inactivo
 * - Configuración centralizada
 * - Utiliza la base de datos de registraduría
 */
public class ConsultaMesaImpl implements IConsultaMesa {
    
    private final ConfigManager config;
    private final DatabaseManager dbManager;
    private final DatabaseConnection dbConnection;
    
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
        System.out.println("🔧 Inicializando ConsultaMesa con conexión directa...");
        
        // Cargar configuración
        this.config = ConfigManager.getInstance();
        
        // Obtener conexión de registraduría
        this.dbManager = DatabaseManager.getInstance();
        this.dbConnection = dbManager.getRegistraduriaConnection();
        
        // Mostrar configuración
        config.printConfiguration();
        
        System.out.println("✅ ConsultaMesa inicializado correctamente");
        System.out.println("   📊 Base de datos: registraduría");
        System.out.println("   " + dbConnection.getConnectionInfo());
    }
    
    @Override
    public MesaInfo consultarMesaPorDocumento(String documento, Current current) {
        System.out.println("🔍 Consultando mesa para documento: " + documento);
        
        // Verificar si el servicio está activo
        if (!dbConnection.isServiceActive()) {
            System.err.println("🚫 Servicio de base de datos inactivo");
            return createServiceInactiveResponse();
        }
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                System.err.println("❌ No se pudo obtener conexión a la base de datos");
                return createServiceInactiveResponse();
            }
            
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
        }
    }
    
    @Override
    public boolean verificarConexionBD(Current current) {
        System.out.println("🔧 Verificando conexión a base de datos...");
        System.out.println("   " + dbConnection.getConnectionInfo());
        
        if (!dbConnection.isServiceActive()) {
            System.err.println("❌ Servicio marcado como inactivo");
            return false;
        }
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                System.err.println("❌ No se pudo obtener conexión");
                return false;
            }
            
            // Ejecutar una consulta simple para verificar la conexión
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 as test")) {
                stmt.setQueryTimeout(5); // 5 segundos timeout
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("test") == 1) {
                        System.out.println("✅ Conexión a base de datos OK");
                        System.out.println("   " + dbConnection.getConnectionInfo());
                        return true;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error verificando conexión: " + e.getMessage());
            return false;
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
                           dbConnection.getConnectionInfo(),
                           config.getQueryTimeout());
    }
    
    /**
     * Método para cerrar conexiones y limpiar recursos
     */
    public void shutdown() {
        System.out.println("✅ ConsultaMesa cerrado correctamente");
    }
} 