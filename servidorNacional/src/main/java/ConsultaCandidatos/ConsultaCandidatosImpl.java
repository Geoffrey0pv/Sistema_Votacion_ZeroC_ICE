package ConsultaCandidatos;

import Demo.*;
import com.zeroc.Ice.Current;
import Database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de consulta de candidatos electorales
 * Utiliza la base de datos nacional configurada
 */
public class ConsultaCandidatosImpl implements IConsultaCandidatos {
    
    private final DatabaseConnection dbConnection;
    
    public ConsultaCandidatosImpl() {
        this.dbConnection = new DatabaseConnection("nacional");
        
        System.out.println("🗳️  ConsultaCandidatos inicializado");
        System.out.println("   📊 Base de datos: nacional");
        
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
        
        String sql = "SELECT id, nombre, partido, fecha_creacion, activo FROM candidato ORDER BY id";
        List<CandidatoElectoral> candidatos = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                System.err.println("❌ No se pudo obtener conexión a la base de datos");
                return new CandidatoElectoral[0];
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    CandidatoElectoral candidato = new CandidatoElectoral();
                    candidato.id = rs.getLong("id");
                    candidato.nombre = rs.getString("nombre");
                    candidato.partido = rs.getString("partido");
                    
                    // Manejar fecha_creacion que puede ser null
                    Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
                    candidato.fechaCreacion = fechaCreacion != null ? fechaCreacion.toString() : "N/A";
                    
                    candidato.activo = rs.getBoolean("activo");
                    
                    candidatos.add(candidato);
                }
                
                long endTime = System.currentTimeMillis();
                System.out.println("✅ Candidatos consultados: " + candidatos.size() + 
                                 " en " + (endTime - startTime) + "ms");
                
            }
            
        } catch (SQLException e) {
            long endTime = System.currentTimeMillis();
            System.err.println("❌ Error consultando candidatos (" + (endTime - startTime) + "ms): " + e.getMessage());
            e.printStackTrace();
        }
        
        return candidatos.toArray(new CandidatoElectoral[0]);
    }
    
    @Override
    public CandidatoElectoral[] obtenerCandidatosPorPartido(String partido, Current current) {
        System.out.println("🔍 Consultando candidatos del partido: " + partido);
        
        if (partido == null || partido.trim().isEmpty()) {
            System.err.println("❌ Partido no puede estar vacío");
            return new CandidatoElectoral[0];
        }
        
        String sql = "SELECT id, nombre, partido, fecha_creacion, activo FROM candidato " +
                    "WHERE LOWER(partido) LIKE LOWER(?) ORDER BY id";
        List<CandidatoElectoral> candidatos = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                System.err.println("❌ No se pudo obtener conexión a la base de datos");
                return new CandidatoElectoral[0];
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + partido.trim() + "%");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        CandidatoElectoral candidato = new CandidatoElectoral();
                        candidato.id = rs.getLong("id");
                        candidato.nombre = rs.getString("nombre");
                        candidato.partido = rs.getString("partido");
                        
                        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
                        candidato.fechaCreacion = fechaCreacion != null ? fechaCreacion.toString() : "N/A";
                        
                        candidato.activo = rs.getBoolean("activo");
                        
                        candidatos.add(candidato);
                    }
                    
                    long endTime = System.currentTimeMillis();
                    System.out.println("✅ Candidatos del partido '" + partido + "': " + candidatos.size() + 
                                     " en " + (endTime - startTime) + "ms");
                }
            }
            
        } catch (SQLException e) {
            long endTime = System.currentTimeMillis();
            System.err.println("❌ Error consultando candidatos por partido (" + (endTime - startTime) + "ms): " + e.getMessage());
            e.printStackTrace();
        }
        
        return candidatos.toArray(new CandidatoElectoral[0]);
    }
    
    @Override
    public long contarCandidatos(Current current) {
        System.out.println("🔢 Contando total de candidatos...");
        
        String sql = "SELECT COUNT(*) as total FROM candidato WHERE activo = true";
        long total = 0;
        
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                System.err.println("❌ No se pudo obtener conexión a la base de datos");
                return 0;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    total = rs.getLong("total");
                }
                
                long endTime = System.currentTimeMillis();
                System.out.println("✅ Total de candidatos activos: " + total + 
                                 " en " + (endTime - startTime) + "ms");
                
            }
            
        } catch (SQLException e) {
            long endTime = System.currentTimeMillis();
            System.err.println("❌ Error contando candidatos (" + (endTime - startTime) + "ms): " + e.getMessage());
            e.printStackTrace();
        }
        
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
        return "BD Candidatos: " + dbConnection.getPoolStats();
    }
    
    /**
     * Método para cerrar el servicio de forma limpia
     */
    public void shutdown() {
        System.out.println("🛑 ConsultaCandidatos: Cerrando servicio...");
        // DatabaseConnection no tiene método close(), el pool se maneja automáticamente
        System.out.println("✅ ConsultaCandidatos: Servicio cerrado");
    }
} 