package servidorRegional;

import Demo.CiudadanoInfo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gestor de base de datos SQLite para el Servidor Regional
 * Permite persistir localmente los votantes consultados del servidor nacional
 */
public class DatabaseManager {
    
    private static final String DB_PATH = "data/regional_votantes.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    
    public DatabaseManager() {
        try {
            // Cargar el driver JDBC de SQLite
            Class.forName("org.sqlite.JDBC");
            inicializarBaseDatos();
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: Driver SQLite no encontrado. Agregue sqlite-jdbc a las dependencias.");
            throw new RuntimeException(e);
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando base de datos: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Inicializa la base de datos y crea las tablas necesarias
     */
    private void inicializarBaseDatos() throws SQLException {
        // Crear directorio si no existe
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Crear tabla de votantes
            String createVotantesTable = "CREATE TABLE IF NOT EXISTS votantes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ciudadano_id INTEGER," +
                "documento TEXT NOT NULL," +
                "nombre TEXT NOT NULL," +
                "apellido TEXT NOT NULL," +
                "mesa TEXT," +
                "mesa_id TEXT," +
                "puesto TEXT," +
                "municipio TEXT," +
                "departamento TEXT NOT NULL," +
                "fecha_consulta TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE(documento, departamento)" +
                ")";
            
            // Crear tabla de consultas (para estadísticas)
            String createConsultasTable = "CREATE TABLE IF NOT EXISTS consultas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "departamento TEXT NOT NULL," +
                "tipo_consulta TEXT NOT NULL," +
                "total_registros INTEGER," +
                "fecha_consulta TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
            
            // Crear índices para mejorar rendimiento
            String createIndex1 = "CREATE INDEX IF NOT EXISTS idx_votantes_departamento ON votantes(departamento)";
            String createIndex2 = "CREATE INDEX IF NOT EXISTS idx_votantes_documento ON votantes(documento)";
            String createIndex3 = "CREATE INDEX IF NOT EXISTS idx_votantes_municipio ON votantes(municipio)";
            
            Statement stmt = conn.createStatement();
            stmt.execute(createVotantesTable);
            stmt.execute(createConsultasTable);
            stmt.execute(createIndex1);
            stmt.execute(createIndex2);
            stmt.execute(createIndex3);
            
            // Migración: Agregar campo mesa_id si no existe
            try {
                stmt.execute("ALTER TABLE votantes ADD COLUMN mesa_id TEXT");
                System.out.println("🔄 Migración aplicada: campo mesa_id agregado");
            } catch (SQLException e) {
                // El campo ya existe, no es un error
                if (!e.getMessage().contains("duplicate column name")) {
                    System.err.println("⚠️ Error en migración: " + e.getMessage());
                }
            }
            
            System.out.println("✅ Base de datos SQLite inicializada: " + DB_PATH);
        }
    }
    
    /**
     * Guarda una lista de votantes en la base de datos
     */
    public int guardarVotantes(List<CiudadanoInfo> votantes, String departamento) {
        if (votantes == null || votantes.isEmpty()) {
            return 0;
        }
        
        String insertSQL = "INSERT OR REPLACE INTO votantes " +
            "(ciudadano_id, documento, nombre, apellido, mesa, mesa_id, puesto, municipio, departamento, fecha_consulta) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        int guardados = 0;
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            conn.setAutoCommit(false); // Transacción para mejor rendimiento
            
            for (CiudadanoInfo votante : votantes) {
                pstmt.setLong(1, votante.id);
                pstmt.setString(2, votante.documento);
                pstmt.setString(3, votante.nombre);
                pstmt.setString(4, votante.apellido);
                pstmt.setString(5, votante.mesa);
                pstmt.setString(6, votante.mesaId);
                pstmt.setString(7, votante.puesto);
                pstmt.setString(8, votante.municipio);
                pstmt.setString(9, votante.departamento);
                pstmt.setString(10, fechaActual);
                
                pstmt.addBatch();
                guardados++;
            }
            
            pstmt.executeBatch();
            conn.commit();
            
            // Registrar la consulta
            registrarConsulta(conn, departamento, "GUARDAR_VOTANTES", guardados);
            
            System.out.println("💾 Guardados " + guardados + " votantes de " + departamento + " en SQLite");
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando votantes: " + e.getMessage());
        }
        
        return guardados;
    }
    
    /**
     * Consulta votantes por departamento desde la base de datos local
     */
    public List<CiudadanoInfo> consultarVotantesLocales(String departamentoNoNormalizado) {
        String departamento = departamentoNoNormalizado.trim().toUpperCase();
        List<CiudadanoInfo> votantes = new ArrayList<>();
        
        String selectSQL = "SELECT ciudadano_id, documento, nombre, apellido, mesa, mesa_id, puesto, municipio, departamento " +
            "FROM votantes " +
            "WHERE departamento = ? " +
            "ORDER BY apellido, nombre";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setString(1, departamento);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                CiudadanoInfo votante = new CiudadanoInfo();
                votante.id = rs.getLong("ciudadano_id");
                votante.documento = rs.getString("documento");
                votante.nombre = rs.getString("nombre");
                votante.apellido = rs.getString("apellido");
                votante.mesa = rs.getString("mesa");
                votante.mesaId = rs.getString("mesa_id");
                votante.puesto = rs.getString("puesto");
                votante.municipio = rs.getString("municipio");
                votante.departamento = rs.getString("departamento");
                
                votantes.add(votante);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error consultando votantes locales: " + e.getMessage());
        }
        
        return votantes;
    }
    
    /**
     * Cuenta votantes por departamento en la base de datos local
     */
    public long contarVotantesLocales(String departamento) {
        String countSQL = "SELECT COUNT(*) FROM votantes WHERE departamento = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(countSQL)) {
            
            pstmt.setString(1, departamento);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error contando votantes locales: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Obtiene estadísticas de la base de datos local
     */
    public void mostrarEstadisticas() {
        String statsSQL = "SELECT " +
            "departamento, " +
            "COUNT(*) as total_votantes, " +
            "MAX(fecha_consulta) as ultima_consulta " +
            "FROM votantes " +
            "GROUP BY departamento " +
            "ORDER BY total_votantes DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(statsSQL)) {
            
            System.out.println("📊 === ESTADÍSTICAS BASE DE DATOS LOCAL ===");
            System.out.println("   Base de datos: " + DB_PATH);
            
            boolean hayDatos = false;
            while (rs.next()) {
                if (!hayDatos) {
                    System.out.println("   Departamentos con datos:");
                    hayDatos = true;
                }
                
                String depto = rs.getString("departamento");
                long total = rs.getLong("total_votantes");
                String ultimaConsulta = rs.getString("ultima_consulta");
                
                System.out.println(String.format("   • %s: %,d votantes (Última: %s)", 
                    depto, total, ultimaConsulta.substring(0, 19)));
            }
            
            if (!hayDatos) {
                System.out.println("   No hay datos almacenados localmente");
            }
            
            // Mostrar tamaño de archivo
            java.io.File dbFile = new java.io.File(DB_PATH);
            if (dbFile.exists()) {
                long sizeKB = dbFile.length() / 1024;
                System.out.println("   Tamaño archivo: " + sizeKB + " KB");
            }
            
            System.out.println("=======================================");
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo estadísticas: " + e.getMessage());
        }
    }
    
    /**
     * Limpia todos los datos de un departamento específico
     */
    public int limpiarDepartamento(String departamento) {
        String deleteSQL = "DELETE FROM votantes WHERE departamento = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            
            pstmt.setString(1, departamento);
            int eliminados = pstmt.executeUpdate();
            
            System.out.println("🗑️ Eliminados " + eliminados + " registros de " + departamento);
            return eliminados;
            
        } catch (SQLException e) {
            System.err.println("❌ Error limpiando departamento: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Registra una consulta en el log de consultas
     */
    private void registrarConsulta(Connection conn, String departamento, String tipoConsulta, int totalRegistros) {
        String insertSQL = "INSERT INTO consultas (departamento, tipo_consulta, total_registros) " +
            "VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, departamento);
            pstmt.setString(2, tipoConsulta);
            pstmt.setInt(3, totalRegistros);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // No es crítico si falla el log
            System.err.println("⚠️ Error registrando consulta: " + e.getMessage());
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
} 