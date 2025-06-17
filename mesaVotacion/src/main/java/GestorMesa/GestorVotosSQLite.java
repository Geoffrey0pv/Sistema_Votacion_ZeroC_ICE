package GestorMesa;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
 * Gestor de votos y control de votantes en SQLite local
 * Maneja dos bases de datos:
 * 1. Registro de votos emitidos
 * 2. Control de votantes que ya han votado
 */
public class GestorVotosSQLite {
    private final String mesaId;
    private final String dbVotosPath;
    private final String dbControlPath;
    private Connection conexionVotos;
    private Connection conexionControl;
    
    public GestorVotosSQLite(String mesaId) {
        this.mesaId = mesaId;
        this.dbVotosPath = "data/votos_mesa_" + mesaId + ".sqlite";
        this.dbControlPath = "data/control_votantes_mesa_" + mesaId + ".sqlite";
        
        // Crear directorio data si no existe
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            System.out.println("📁 Directorio 'data' creado para votos");
        }
        
        inicializarBaseDatos();
    }
    
    /**
     * Inicializa las bases de datos SQLite
     */
    private void inicializarBaseDatos() {
        try {
            // Cargar driver SQLite
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ Driver SQLite cargado para Gestor de Votos");
            
            // Inicializar base de datos de votos
            inicializarBDVotos();
            
            // Inicializar base de datos de control de votantes
            inicializarBDControl();
            
        } catch (Exception e) {
            System.err.println("❌ Error inicializando bases de datos de votos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inicializa la base de datos de votos registrados
     */
    private void inicializarBDVotos() throws SQLException {
        conexionVotos = DriverManager.getConnection("jdbc:sqlite:" + dbVotosPath);
        
        // Crear tabla de votos si no existe
        String sqlCrearTablaVotos = "CREATE TABLE IF NOT EXISTS votos_registrados (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "mesa_id TEXT NOT NULL," +
            "timestamp INTEGER NOT NULL," +
            "candidato_id INTEGER NOT NULL," +
            "hash_verificacion TEXT NOT NULL," +
            "municipio TEXT NOT NULL," +
            "departamento TEXT NOT NULL," +
            "fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        try (Statement stmt = conexionVotos.createStatement()) {
            stmt.execute(sqlCrearTablaVotos);
            System.out.println("✅ Tabla de votos registrados inicializada");
        }
        
        // Crear índices para mejorar rendimiento
        String sqlIndices = "CREATE INDEX IF NOT EXISTS idx_votos_mesa ON votos_registrados(mesa_id);" +
            "CREATE INDEX IF NOT EXISTS idx_votos_timestamp ON votos_registrados(timestamp);" +
            "CREATE INDEX IF NOT EXISTS idx_votos_candidato ON votos_registrados(candidato_id);";
        
        try (Statement stmt = conexionVotos.createStatement()) {
            stmt.execute(sqlIndices);
        }
    }
    
    /**
     * Inicializa la base de datos de control de votantes
     */
    private void inicializarBDControl() throws SQLException {
        conexionControl = DriverManager.getConnection("jdbc:sqlite:" + dbControlPath);
        
        // Crear tabla de control de votantes
        String sqlCrearTablaControl = "CREATE TABLE IF NOT EXISTS votantes_ya_votaron (" +
            "documento TEXT PRIMARY KEY," +
            "mesa_id TEXT NOT NULL," +
            "hash_verificacion TEXT NOT NULL," +
            "timestamp_voto INTEGER NOT NULL," +
            "municipio TEXT NOT NULL," +
            "departamento TEXT NOT NULL," +
            "fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        try (Statement stmt = conexionControl.createStatement()) {
            stmt.execute(sqlCrearTablaControl);
            System.out.println("✅ Tabla de control de votantes inicializada");
        }
        
        // Crear índices
        String sqlIndices = "CREATE INDEX IF NOT EXISTS idx_control_mesa ON votantes_ya_votaron(mesa_id);" +
            "CREATE INDEX IF NOT EXISTS idx_control_timestamp ON votantes_ya_votaron(timestamp_voto);";
        
        try (Statement stmt = conexionControl.createStatement()) {
            stmt.execute(sqlIndices);
        }
    }
    
    /**
     * Registra un voto en la base de datos
     */
    public boolean registrarVoto(VotoRegistro voto) {
        if (conexionVotos == null) {
            System.err.println("❌ No hay conexión a la base de datos de votos");
            return false;
        }
        
        String sql = "INSERT INTO votos_registrados " +
            "(mesa_id, timestamp, candidato_id, hash_verificacion, municipio, departamento) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conexionVotos.prepareStatement(sql)) {
            pstmt.setString(1, voto.mesaId);
            pstmt.setLong(2, voto.timestamp);
            pstmt.setLong(3, voto.candidatoId);
            pstmt.setString(4, voto.hashVerificacion);
            pstmt.setString(5, voto.municipio);
            pstmt.setString(6, voto.departamento);
            
            int filasAfectadas = pstmt.executeUpdate();
            
            if (filasAfectadas > 0) {
                // Obtener el ID del último registro insertado usando SQLite specific method
                try (Statement stmt = conexionVotos.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        voto.id = rs.getLong(1);
                    }
                } catch (SQLException e) {
                    System.err.println("⚠️ No se pudo obtener ID del voto, pero se registró correctamente");
                    voto.id = System.currentTimeMillis(); // ID temporal
                }
                
                System.out.println("✅ Voto registrado en BD local: ID=" + voto.id);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error registrando voto en BD: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Marca un votante como que ya votó
     */
    public boolean marcarVotanteComoVotado(String documento, String hashVerificacion, 
                                          String municipio, String departamento) {
        if (conexionControl == null) {
            System.err.println("❌ No hay conexión a la base de datos de control");
            return false;
        }
        
        String sql = "INSERT INTO votantes_ya_votaron " +
            "(documento, mesa_id, hash_verificacion, timestamp_voto, municipio, departamento) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conexionControl.prepareStatement(sql)) {
            pstmt.setString(1, documento);
            pstmt.setString(2, mesaId);
            pstmt.setString(3, hashVerificacion);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.setString(5, municipio);
            pstmt.setString(6, departamento);
            
            int filasAfectadas = pstmt.executeUpdate();
            
            if (filasAfectadas > 0) {
                System.out.println("✅ Votante marcado como que ya votó: " + documento);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error marcando votante como votado: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Verifica si un votante ya ha votado
     */
    public boolean yaVoto(String documento) {
        if (conexionControl == null) {
            System.err.println("❌ No hay conexión a la base de datos de control");
            return false;
        }
        
        String sql = "SELECT COUNT(*) FROM votantes_ya_votaron WHERE documento = ? AND mesa_id = ?";
        
        try (PreparedStatement pstmt = conexionControl.prepareStatement(sql)) {
            pstmt.setString(1, documento);
            pstmt.setString(2, mesaId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error verificando si ya votó: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Obtiene todos los votos registrados
     */
    public List<VotoRegistro> obtenerTodosLosVotos() {
        List<VotoRegistro> votos = new ArrayList<>();
        
        if (conexionVotos == null) {
            System.err.println("❌ No hay conexión a la base de datos de votos");
            return votos;
        }
        
        String sql = "SELECT id, mesa_id, timestamp, candidato_id, hash_verificacion, municipio, departamento " +
            "FROM votos_registrados " +
            "WHERE mesa_id = ? " +
            "ORDER BY timestamp DESC";
        
        try (PreparedStatement pstmt = conexionVotos.prepareStatement(sql)) {
            pstmt.setString(1, mesaId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    VotoRegistro voto = new VotoRegistro(
                        rs.getLong("id"),
                        rs.getString("mesa_id"),
                        rs.getLong("timestamp"),
                        rs.getLong("candidato_id"),
                        rs.getString("hash_verificacion"),
                        rs.getString("municipio"),
                        rs.getString("departamento")
                    );
                    votos.add(voto);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo votos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return votos;
    }
    
    /**
     * Obtiene estadísticas de votación
     */
    public void mostrarEstadisticas() {
        System.out.println("\n📊 === ESTADÍSTICAS DE VOTACIÓN MESA " + mesaId + " ===");
        
        // Contar votos totales
        int totalVotos = contarVotos();
        System.out.println("🗳️ Total de votos registrados: " + totalVotos);
        
        // Contar votantes que ya votaron
        int votantesQueYaVotaron = contarVotantesQueYaVotaron();
        System.out.println("👥 Votantes que ya votaron: " + votantesQueYaVotaron);
        
        // Mostrar votos por candidato
        mostrarVotosPorCandidato();
        
        System.out.println("📁 BD Votos: " + dbVotosPath);
        System.out.println("📁 BD Control: " + dbControlPath);
        System.out.println("═".repeat(50));
    }
    
    private int contarVotos() {
        if (conexionVotos == null) return 0;
        
        String sql = "SELECT COUNT(*) FROM votos_registrados WHERE mesa_id = ?";
        try (PreparedStatement pstmt = conexionVotos.prepareStatement(sql)) {
            pstmt.setString(1, mesaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error contando votos: " + e.getMessage());
        }
        return 0;
    }
    
    private int contarVotantesQueYaVotaron() {
        if (conexionControl == null) return 0;
        
        String sql = "SELECT COUNT(*) FROM votantes_ya_votaron WHERE mesa_id = ?";
        try (PreparedStatement pstmt = conexionControl.prepareStatement(sql)) {
            pstmt.setString(1, mesaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error contando votantes: " + e.getMessage());
        }
        return 0;
    }
    
    private void mostrarVotosPorCandidato() {
        if (conexionVotos == null) return;
        
        String sql = "SELECT candidato_id, COUNT(*) as total_votos " +
            "FROM votos_registrados " +
            "WHERE mesa_id = ? " +
            "GROUP BY candidato_id " +
            "ORDER BY total_votos DESC";
        
        try (PreparedStatement pstmt = conexionVotos.prepareStatement(sql)) {
            pstmt.setString(1, mesaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("📊 Votos por candidato:");
                while (rs.next()) {
                    long candidatoId = rs.getLong("candidato_id");
                    int totalVotos = rs.getInt("total_votos");
                    System.out.println("  • Candidato " + candidatoId + ": " + totalVotos + " votos");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo votos por candidato: " + e.getMessage());
        }
    }
    
    /**
     * Cierra las conexiones a las bases de datos
     */
    public void cerrarConexiones() {
        try {
            if (conexionVotos != null && !conexionVotos.isClosed()) {
                conexionVotos.close();
                System.out.println("✅ Conexión a BD de votos cerrada");
            }
            if (conexionControl != null && !conexionControl.isClosed()) {
                conexionControl.close();
                System.out.println("✅ Conexión a BD de control cerrada");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error cerrando conexiones: " + e.getMessage());
        }
    }
    
    // Getters
    public String getMesaId() { return mesaId; }
    public String getDbVotosPath() { return dbVotosPath; }
    public String getDbControlPath() { return dbControlPath; }
} 