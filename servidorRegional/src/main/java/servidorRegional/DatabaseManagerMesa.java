package servidorRegional;

import Demo.CiudadanoInfo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gestor de base de datos SQLite específico para una Mesa de Votación
 * Permite persistir localmente solo los votantes asignados a esta mesa
 */
public class DatabaseManagerMesa {
    
    private final String mesaId;
    private final String DB_PATH;
    private final String DB_URL;
    
    public DatabaseManagerMesa(String mesaId) {
        this.mesaId = mesaId;
        this.DB_PATH = "data/mesa_" + mesaId.replaceAll("[^a-zA-Z0-9]", "_") + ".db";
        this.DB_URL = "jdbc:sqlite:" + DB_PATH;
        
        try {
            // Cargar el driver JDBC de SQLite
            Class.forName("org.sqlite.JDBC");
            inicializarBaseDatos();
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: Driver SQLite no encontrado. Agregue sqlite-jdbc a las dependencias.");
            throw new RuntimeException(e);
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando base de datos de mesa: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Inicializa la base de datos específica de la mesa
     */
    private void inicializarBaseDatos() throws SQLException {
        // Crear directorio si no existe
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Crear tabla de votantes de la mesa
            String createVotantesTable = "CREATE TABLE IF NOT EXISTS votantes_mesa (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ciudadano_id INTEGER," +
                "documento TEXT NOT NULL UNIQUE," +
                "nombre TEXT NOT NULL," +
                "apellido TEXT NOT NULL," +
                "mesa TEXT NOT NULL," +
                "puesto TEXT," +
                "municipio TEXT," +
                "departamento TEXT NOT NULL," +
                "fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "verificado INTEGER DEFAULT 0," +
                "fecha_verificacion TIMESTAMP" +
                ")";
            
            // Crear tabla de estadísticas de la mesa
            String createEstadisticasTable = "CREATE TABLE IF NOT EXISTS estadisticas_mesa (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "mesa_id TEXT NOT NULL," +
                "departamento TEXT," +
                "municipio TEXT," +
                "puesto TEXT," +
                "total_votantes INTEGER DEFAULT 0," +
                "votantes_verificados INTEGER DEFAULT 0," +
                "mesa_activa INTEGER DEFAULT 1," +
                "fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            
            // Crear tabla de log de verificaciones
            String createLogTable = "CREATE TABLE IF NOT EXISTS log_verificaciones (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "documento TEXT NOT NULL," +
                "accion TEXT NOT NULL," +
                "resultado TEXT," +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            
            // Crear índices para mejorar rendimiento
            String createIndex1 = "CREATE INDEX IF NOT EXISTS idx_votantes_documento ON votantes_mesa(documento)";
            String createIndex2 = "CREATE INDEX IF NOT EXISTS idx_votantes_mesa ON votantes_mesa(mesa)";
            String createIndex3 = "CREATE INDEX IF NOT EXISTS idx_log_documento ON log_verificaciones(documento)";
            
            Statement stmt = conn.createStatement();
            stmt.execute(createVotantesTable);
            stmt.execute(createEstadisticasTable);
            stmt.execute(createLogTable);
            stmt.execute(createIndex1);
            stmt.execute(createIndex2);
            stmt.execute(createIndex3);
            
            System.out.println("✅ Base de datos SQLite de mesa inicializada: " + DB_PATH);
            
            // Inicializar estadísticas de la mesa si no existen
            inicializarEstadisticasMesa();
        }
    }
    
    /**
     * Inicializa las estadísticas de la mesa
     */
    private void inicializarEstadisticasMesa() throws SQLException {
        String checkSQL = "SELECT COUNT(*) FROM estadisticas_mesa WHERE mesa_id = ?";
        String insertSQL = "INSERT INTO estadisticas_mesa (mesa_id) VALUES (?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
            
            checkStmt.setString(1, mesaId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) == 0) {
                // No existe, crear registro inicial
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                    insertStmt.setString(1, mesaId);
                    insertStmt.executeUpdate();
                    System.out.println("📊 Estadísticas de mesa inicializadas para: " + mesaId);
                }
            }
        }
    }
    
    /**
     * Guarda votantes asignados a esta mesa
     */
    public int guardarVotantesMesa(List<CiudadanoInfo> votantes, String departamento) {
        if (votantes == null || votantes.isEmpty()) {
            return 0;
        }
        
        String insertSQL = "INSERT OR REPLACE INTO votantes_mesa " +
            "(ciudadano_id, documento, nombre, apellido, mesa, puesto, municipio, departamento, fecha_asignacion) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int guardados = 0;
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            conn.setAutoCommit(false); // Transacción para mejor rendimiento
            
            for (CiudadanoInfo votante : votantes) {
                // Solo guardar si el votante pertenece a esta mesa
                if (mesaId.equals(votante.mesa)) {
                    pstmt.setLong(1, votante.id);
                    pstmt.setString(2, votante.documento);
                    pstmt.setString(3, votante.nombre);
                    pstmt.setString(4, votante.apellido);
                    pstmt.setString(5, votante.mesa);
                    pstmt.setString(6, votante.puesto);
                    pstmt.setString(7, votante.municipio);
                    pstmt.setString(8, departamento);
                    pstmt.setString(9, fechaActual);
                    
                    pstmt.addBatch();
                    guardados++;
                }
            }
            
            if (guardados > 0) {
                pstmt.executeBatch();
                conn.commit();
                
                // Actualizar estadísticas
                actualizarEstadisticas();
                
                System.out.println("💾 Guardados " + guardados + " votantes en mesa " + mesaId);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando votantes de mesa: " + e.getMessage());
        }
        
        return guardados;
    }
    
    /**
     * Verifica si un votante pertenece a esta mesa
     */
    public boolean verificarVotanteEnMesa(String documento) {
        String selectSQL = "SELECT COUNT(*) FROM votantes_mesa WHERE documento = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setString(1, documento);
            ResultSet rs = pstmt.executeQuery();
            
            boolean existe = rs.next() && rs.getInt(1) > 0;
            
            // Registrar la verificación
            registrarVerificacion(documento, "VERIFICAR_DOCUMENTO", existe ? "ENCONTRADO" : "NO_ENCONTRADO");
            
            return existe;
            
        } catch (SQLException e) {
            System.err.println("❌ Error verificando votante: " + e.getMessage());
            registrarVerificacion(documento, "VERIFICAR_DOCUMENTO", "ERROR: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene información de un votante de la mesa
     */
    public CiudadanoInfo obtenerVotanteDeMesa(String documento) {
        String selectSQL = "SELECT ciudadano_id, documento, nombre, apellido, mesa, puesto, municipio, departamento " +
            "FROM votantes_mesa WHERE documento = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setString(1, documento);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                CiudadanoInfo votante = new CiudadanoInfo();
                votante.id = rs.getLong("ciudadano_id");
                votante.documento = rs.getString("documento");
                votante.nombre = rs.getString("nombre");
                votante.apellido = rs.getString("apellido");
                votante.mesa = rs.getString("mesa");
                votante.puesto = rs.getString("puesto");
                votante.municipio = rs.getString("municipio");
                votante.departamento = rs.getString("departamento");
                
                registrarVerificacion(documento, "OBTENER_INFO", "EXITOSO");
                return votante;
            }
            
            registrarVerificacion(documento, "OBTENER_INFO", "NO_ENCONTRADO");
            return null;
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo votante: " + e.getMessage());
            registrarVerificacion(documento, "OBTENER_INFO", "ERROR: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Cuenta los votantes asignados a esta mesa
     */
    public int contarVotantesEnMesa() {
        String countSQL = "SELECT COUNT(*) FROM votantes_mesa";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSQL)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error contando votantes de mesa: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Obtiene estadísticas de la mesa
     */
    public String obtenerEstadisticasMesa() {
        String selectSQL = "SELECT mesa_id, departamento, municipio, puesto, total_votantes, " +
            "votantes_verificados, mesa_activa, ultima_actualizacion " +
            "FROM estadisticas_mesa WHERE mesa_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setString(1, mesaId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String estadisticas = "📊 Estadísticas Mesa " + rs.getString("mesa_id") + ":\n";
                estadisticas += "   Departamento: " + rs.getString("departamento") + "\n";
                estadisticas += "   Municipio: " + rs.getString("municipio") + "\n";
                estadisticas += "   Puesto: " + rs.getString("puesto") + "\n";
                estadisticas += "   Votantes asignados: " + rs.getInt("total_votantes") + "\n";
                estadisticas += "   Votantes verificados: " + rs.getInt("votantes_verificados") + "\n";
                estadisticas += "   Mesa activa: " + (rs.getInt("mesa_activa") == 1 ? "Sí" : "No") + "\n";
                estadisticas += "   Última actualización: " + new java.util.Date(rs.getLong("ultima_actualizacion"));
                
                return estadisticas;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo estadísticas: " + e.getMessage());
        }
        
        return "❌ No se encontraron estadísticas para la mesa " + mesaId;
    }
    
    /**
     * Actualiza las estadísticas de la mesa
     */
    private void actualizarEstadisticas() {
        String updateSQL = "UPDATE estadisticas_mesa SET " +
            "total_votantes = (SELECT COUNT(*) FROM votantes_mesa), " +
            "votantes_verificados = (SELECT COUNT(*) FROM votantes_mesa WHERE verificado = 1), " +
            "ultima_actualizacion = ? " +
            "WHERE mesa_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setString(2, mesaId);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando estadísticas: " + e.getMessage());
        }
    }
    
    /**
     * Registra una verificación en el log
     */
    private void registrarVerificacion(String documento, String accion, String resultado) {
        String insertSQL = "INSERT INTO log_verificaciones (documento, accion, resultado) VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            pstmt.setString(1, documento);
            pstmt.setString(2, accion);
            pstmt.setString(3, resultado);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            // No es crítico si falla el log
            System.err.println("⚠️ Error registrando verificación: " + e.getMessage());
        }
    }
    
    /**
     * Limpia todos los datos de la mesa
     */
    public boolean limpiarDatosMesa() {
        String deleteVotantes = "DELETE FROM votantes_mesa";
        String deleteLog = "DELETE FROM log_verificaciones";
        String resetStats = "UPDATE estadisticas_mesa SET total_votantes = 0, votantes_verificados = 0, " +
            "ultima_actualizacion = ? WHERE mesa_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(deleteVotantes);
                stmt.executeUpdate(deleteLog);
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(resetStats)) {
                pstmt.setLong(1, System.currentTimeMillis());
                pstmt.setString(2, mesaId);
                pstmt.executeUpdate();
            }
            
            conn.commit();
            System.out.println("🗑️ Datos de mesa " + mesaId + " limpiados");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error limpiando datos de mesa: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica si la base de datos está disponible
     */
    public boolean verificarConexion() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Obtiene el ID de la mesa
     */
    public String getMesaId() {
        return mesaId;
    }
    
    /**
     * Obtiene la ruta de la base de datos
     */
    public String getDbPath() {
        return DB_PATH;
    }
} 