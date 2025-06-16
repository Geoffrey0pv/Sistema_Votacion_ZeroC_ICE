package ConsultaCandidatos;

import Demo.*;
import com.zeroc.Ice.Current;
import Database.DatabaseManager;
import Database.VotosDatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de consulta de candidatos electorales
 * Utiliza la base de datos de votos
 */
public class ConsultaCandidatosImpl implements IConsultaCandidatos {
    
    private final DatabaseManager dbManager;
    private final VotosDatabaseConnection dbConnection;
    
    public ConsultaCandidatosImpl() {
        this.dbManager = DatabaseManager.getInstance();
        this.dbConnection = dbManager.getVotosConnection();
        
        System.out.println("🗳️  ConsultaCandidatos inicializado");
        System.out.println("   📊 Base de datos: votos");
        
        // Verificar conexión inicial
        if (verificarConexionBD(null)) {
            System.out.println("   ✅ Conexión a BD de candidatos: OK");
        } else {
            System.out.println("   ⚠️  Conexión a BD de candidatos: FALLO");
        }
    }
    
    @Override
    public CandidatoElectoral[] obtenerTodosCandidatosElectorales(Current current) {
        System.out.println("🔍 Consultando todos los candidatos electorales...");
        
        List<CandidatoElectoral> candidatos = obtenerTodosCandidatosElectorales();
        
        return candidatos.toArray(new CandidatoElectoral[0]);
    }
    
    @Override
    public CandidatoElectoral[] obtenerCandidatosPorPartido(String partido, Current current) {
        System.out.println("🔍 Consultando candidatos del partido: " + partido);
        
        if (partido == null || partido.trim().isEmpty()) {
            System.err.println("❌ Partido no puede estar vacío");
            return new CandidatoElectoral[0];
        }
        
        List<CandidatoElectoral> candidatos = obtenerCandidatosPorPartido(partido);
        
        return candidatos.toArray(new CandidatoElectoral[0]);
    }
    
    @Override
    public long contarCandidatos(Current current) {
        System.out.println("🔢 Contando total de candidatos...");
        
        int total = obtenerTotalCandidatos();
        
        return total;
    }
    
    @Override
    public boolean verificarConexionBD(Current current) {
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                return false;
            }
            
            // Verificar que la tabla candidato existe
            String sql = "SELECT COUNT(*) FROM candidato LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("⚠️  Error verificando conexión BD candidatos: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Método de utilidad para obtener información de la base de datos
     */
    public String obtenerInfoBaseDatos() {
        return "BD Candidatos: " + dbConnection.getConnectionInfo();
    }
    
    /**
     * Método para cerrar el servicio de forma limpia
     */
    public void shutdown() {
        System.out.println("🛑 ConsultaCandidatos: Cerrando servicio...");
        System.out.println("✅ ConsultaCandidatos: Servicio cerrado");
    }
    
    public List<CandidatoElectoral> obtenerTodosCandidatosElectorales() {
        List<CandidatoElectoral> candidatos = new ArrayList<>();
        
        // SQL para el schema corregido: id BIGINT, nombre, partido
        String sql = "SELECT id, nombre, partido FROM candidato ORDER BY id";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                CandidatoElectoral candidato = new CandidatoElectoral();
                // Ahora el ID es BIGINT, no necesita conversión
                candidato.id = rs.getLong("id");
                candidato.nombre = rs.getString("nombre");
                candidato.partido = rs.getString("partido");
                
                // Valores por defecto para campos que no existen en el schema real
                candidato.fechaCreacion = "N/A"; // Campo requerido por ICE pero no existe en DB
                candidato.activo = true; // Asumimos que todos están activos
                
                candidatos.add(candidato);
            }
            
            System.out.println("✅ Candidatos obtenidos: " + candidatos.size());
            
        } catch (SQLException e) {
            System.err.println("❌ Error consultando candidatos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return candidatos;
    }
    
    public List<CandidatoElectoral> obtenerCandidatosPorPartido(String partido) {
        List<CandidatoElectoral> candidatos = new ArrayList<>();
        
        // SQL para el schema corregido
        String sql = "SELECT id, nombre, partido FROM candidato " +
                    "WHERE LOWER(partido) LIKE LOWER(?) ORDER BY id";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + partido + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CandidatoElectoral candidato = new CandidatoElectoral();
                    // Ahora el ID es BIGINT, no necesita conversión
                    candidato.id = rs.getLong("id");
                    candidato.nombre = rs.getString("nombre");
                    candidato.partido = rs.getString("partido");
                    
                    // Valores por defecto para campos que no existen en el schema real
                    candidato.fechaCreacion = "N/A";
                    candidato.activo = true;
                    
                    candidatos.add(candidato);
                }
            }
            
            System.out.println("✅ Candidatos por partido '" + partido + "': " + candidatos.size());
            
        } catch (SQLException e) {
            System.err.println("❌ Error consultando candidatos por partido: " + e.getMessage());
            e.printStackTrace();
        }
        
        return candidatos;
    }
    
    public int obtenerTotalCandidatos() {
        // SQL corregido para el schema real - contar todos los candidatos
        String sql = "SELECT COUNT(*) as total FROM candidato";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                int total = rs.getInt("total");
                System.out.println("✅ Total de candidatos: " + total +
                    " (consultado en " + (System.currentTimeMillis() - System.currentTimeMillis()) + "ms)");
                return total;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo total de candidatos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
} 