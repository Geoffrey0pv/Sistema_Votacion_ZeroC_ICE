package servidorRegional;

import Demo.*;
import com.zeroc.Ice.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestor de Candidatos SQLite para Servidor Regional
 * Consulta candidatos del Servidor Nacional (localhost) y los persiste localmente
 */
public class GestorCandidatosSQLite implements IConsultaCandidatos {
    
    private final String DB_PATH = "data/candidatos_regional.sqlite";
    private final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    private final String endpointServidorNacional = "tcp -h localhost -p 9090";
    private final Communicator communicator;
    private IConsultaCandidatosPrx consultaCandidatosNacional;
    private final AtomicBoolean sincronizando = new AtomicBoolean(false);
    
    public GestorCandidatosSQLite(Communicator communicator) {
        this.communicator = communicator;
        
        try {
            // Cargar el driver JDBC de SQLite
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ Driver SQLite cargado para Gestor de Candidatos");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: Driver SQLite no encontrado");
            throw new RuntimeException(e);
        }
        
        // Crear directorio data si no existe
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            boolean creado = dataDir.mkdirs();
            if (creado) {
                System.out.println("📁 Directorio 'data' creado");
            }
        }
        
        // Inicializar base de datos
        try {
            inicializarBaseDatosCandidatos();
            System.out.println("✅ Base de datos de candidatos inicializada");
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando base de datos de candidatos: " + e.getMessage());
            throw new RuntimeException(e);
        }
        
        // Verificar si hay candidatos locales
        if (verificarCandidatosLocalesExisten()) {
            System.out.println("✅ Candidatos encontrados en base de datos local");
        } else {
            System.out.println("⚠️ No hay candidatos locales, se consultará servidor nacional");
            conectarYSincronizarConServidorNacional();
        }
    }
    
    /**
     * Inicializa la estructura de base de datos SQLite para candidatos
     */
    private void inicializarBaseDatosCandidatos() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Tabla de candidatos electorales
            String createCandidatosTable = 
                "CREATE TABLE IF NOT EXISTS candidatos_electorales (" +
                "    id INTEGER PRIMARY KEY," +
                "    nombre TEXT NOT NULL," +
                "    partido TEXT NOT NULL," +
                "    fecha_creacion TEXT," +
                "    activo INTEGER DEFAULT 1," +
                "    fecha_sincronizacion TEXT NOT NULL" +
                ")";
            
            // Tabla de log de sincronización
            String createLogTable = 
                "CREATE TABLE IF NOT EXISTS log_sincronizacion_candidatos (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    accion TEXT NOT NULL," +
                "    resultado TEXT NOT NULL," +
                "    cantidad INTEGER DEFAULT 0," +
                "    detalles TEXT," +
                "    timestamp TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))" +
                ")";
            
            Statement stmt = conn.createStatement();
            stmt.execute(createCandidatosTable);
            stmt.execute(createLogTable);
        }
    }
    
    /**
     * Verifica si existen candidatos en la base de datos local
     */
    private boolean verificarCandidatosLocalesExisten() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String countSQL = "SELECT COUNT(*) FROM candidatos_electorales WHERE activo = 1";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSQL)) {
                
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error verificando candidatos locales: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Conecta al servidor nacional y sincroniza candidatos
     */
    public boolean conectarYSincronizarConServidorNacional() {
        if (sincronizando.get()) {
            System.out.println("⚠️ Sincronización ya en progreso");
            return false;
        }
        
        sincronizando.set(true);
        
        try {
            System.out.println("🔌 Conectando a Servidor Nacional para candidatos...");
            System.out.println("   Endpoint: " + endpointServidorNacional);
            
            // Conectar al servicio de candidatos del servidor nacional
            ObjectPrx base = communicator.stringToProxy("ConsultaCandidatos:" + endpointServidorNacional);
            consultaCandidatosNacional = IConsultaCandidatosPrx.checkedCast(base);
            
            if (consultaCandidatosNacional == null) {
                System.err.println("❌ No se pudo conectar al servicio de candidatos del Servidor Nacional");
                registrarEnLog("CONECTAR_NACIONAL", "ERROR", 0, "No se pudo conectar al servicio");
                return false;
            }
            
            // Verificar conectividad
            consultaCandidatosNacional.ice_ping();
            System.out.println("✅ Conexión exitosa al Servidor Nacional");
            
            // Sincronizar candidatos
            return sincronizarCandidatosDesdeServidor();
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error conectando al Servidor Nacional: " + e.getMessage());
            registrarEnLog("CONECTAR_NACIONAL", "ERROR", 0, e.getMessage());
            return false;
        } finally {
            sincronizando.set(false);
        }
    }
    
    /**
     * Sincroniza candidatos desde el servidor nacional
     */
    private boolean sincronizarCandidatosDesdeServidor() {
        try {
            System.out.println("🔄 Sincronizando candidatos desde Servidor Nacional...");
            
            CandidatoElectoral[] candidatos = consultaCandidatosNacional.obtenerTodosCandidatosElectorales();
            
            if (candidatos == null || candidatos.length == 0) {
                System.out.println("⚠️ No hay candidatos disponibles en el Servidor Nacional");
                registrarEnLog("SINCRONIZAR_CANDIDATOS", "SIN_DATOS", 0, "No hay candidatos en servidor nacional");
                return false;
            }
            
            // Guardar candidatos en base de datos local
            boolean guardado = guardarCandidatosEnSQLite(candidatos);
            
            if (guardado) {
                System.out.println("✅ " + candidatos.length + " candidatos sincronizados correctamente");
                registrarEnLog("SINCRONIZAR_CANDIDATOS", "EXITOSO", candidatos.length, 
                    "Candidatos sincronizados desde " + endpointServidorNacional);
                return true;
            } else {
                System.err.println("❌ Error guardando candidatos en base de datos local");
                return false;
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error sincronizando candidatos: " + e.getMessage());
            registrarEnLog("SINCRONIZAR_CANDIDATOS", "ERROR", 0, e.getMessage());
            return false;
        }
    }
    
    /**
     * Guarda candidatos en la base de datos SQLite local
     */
    private boolean guardarCandidatosEnSQLite(CandidatoElectoral[] candidatos) {
        String insertCandidato = 
            "INSERT OR REPLACE INTO candidatos_electorales " +
            "(id, nombre, partido, fecha_creacion, activo, fecha_sincronizacion) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            
            // Limpiar candidatos existentes
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM candidatos_electorales");
            }
            
            // Insertar nuevos candidatos
            try (PreparedStatement pstmt = conn.prepareStatement(insertCandidato)) {
                String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                for (CandidatoElectoral candidato : candidatos) {
                    pstmt.setLong(1, candidato.id);
                    pstmt.setString(2, candidato.nombre);
                    pstmt.setString(3, candidato.partido);
                    pstmt.setString(4, candidato.fechaCreacion);
                    pstmt.setInt(5, candidato.activo ? 1 : 0);
                    pstmt.setString(6, fechaActual);
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                conn.commit();
                
                System.out.println("✅ " + candidatos.length + " candidatos guardados en SQLite");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando candidatos en SQLite: " + e.getMessage());
            registrarEnLog("GUARDAR_CANDIDATOS", "ERROR", 0, e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene todos los candidatos desde la base de datos local
     */
    @Override
    public CandidatoElectoral[] obtenerTodosCandidatosElectorales(Current current) {
        System.out.println("🔍 Consultando candidatos desde base de datos local...");
        return obtenerCandidatosLocales();
    }
    
    /**
     * Obtiene candidatos por partido desde la base de datos local
     */
    @Override
    public CandidatoElectoral[] obtenerCandidatosPorPartido(String partido, Current current) {
        System.out.println("🔍 Consultando candidatos del partido: " + partido);
        
        if (partido == null || partido.trim().isEmpty()) {
            System.err.println("❌ Partido no puede estar vacío");
            return new CandidatoElectoral[0];
        }
        
        List<CandidatoElectoral> candidatos = new ArrayList<>();
        String selectSQL = 
            "SELECT id, nombre, partido, fecha_creacion, activo " +
            "FROM candidatos_electorales " +
            "WHERE partido = ? AND activo = 1 " +
            "ORDER BY id";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setString(1, partido);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                CandidatoElectoral candidato = new CandidatoElectoral();
                candidato.id = rs.getLong("id");
                candidato.nombre = rs.getString("nombre");
                candidato.partido = rs.getString("partido");
                candidato.fechaCreacion = rs.getString("fecha_creacion");
                candidato.activo = rs.getInt("activo") == 1;
                
                candidatos.add(candidato);
            }
            
            System.out.println("✅ " + candidatos.size() + " candidatos encontrados para partido: " + partido);
            
        } catch (SQLException e) {
            System.err.println("❌ Error consultando candidatos por partido: " + e.getMessage());
        }
        
        return candidatos.toArray(new CandidatoElectoral[0]);
    }
    
    /**
     * Cuenta total de candidatos activos
     */
    @Override
    public long contarCandidatos(Current current) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String countSQL = "SELECT COUNT(*) FROM candidatos_electorales WHERE activo = 1";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSQL)) {
                
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error contando candidatos: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Verifica conexión a base de datos
     */
    @Override
    public boolean verificarConexionBD(Current current) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("❌ Error verificando conexión BD: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene candidatos desde la base de datos local
     */
    public CandidatoElectoral[] obtenerCandidatosLocales() {
        List<CandidatoElectoral> candidatos = new ArrayList<>();
        String selectSQL = 
            "SELECT id, nombre, partido, fecha_creacion, activo " +
            "FROM candidatos_electorales " +
            "WHERE activo = 1 " +
            "ORDER BY id";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                CandidatoElectoral candidato = new CandidatoElectoral();
                candidato.id = rs.getLong("id");
                candidato.nombre = rs.getString("nombre");
                candidato.partido = rs.getString("partido");
                candidato.fechaCreacion = rs.getString("fecha_creacion");
                candidato.activo = rs.getInt("activo") == 1;
                
                candidatos.add(candidato);
            }
            
            if (candidatos.isEmpty()) {
                System.out.println("⚠️ No hay candidatos locales, intentando sincronizar...");
                if (conectarYSincronizarConServidorNacional()) {
                    // Recursar para obtener los candidatos recién sincronizados
                    return obtenerCandidatosLocales();
                }
            } else {
                System.out.println("✅ " + candidatos.size() + " candidatos obtenidos desde base de datos local");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo candidatos locales: " + e.getMessage());
        }
        
        return candidatos.toArray(new CandidatoElectoral[0]);
    }
    
    /**
     * Convierte candidatos electorales a formato simple para mesas
     */
    public Candidato[] obtenerCandidatosParaMesas() {
        CandidatoElectoral[] candidatosElectorales = obtenerCandidatosLocales();
        List<Candidato> candidatos = new ArrayList<>();
        
        for (CandidatoElectoral ce : candidatosElectorales) {
            if (ce.activo) {
                candidatos.add(new Candidato(ce.id, ce.nombre, ce.partido));
            }
        }
        
        System.out.println("✅ " + candidatos.size() + " candidatos preparados para envío a mesas");
        return candidatos.toArray(new Candidato[0]);
    }
    
    /**
     * Fuerza una nueva sincronización con el servidor nacional
     */
    public boolean forzarSincronizacion() {
        System.out.println("🔄 Forzando sincronización con Servidor Nacional...");
        return conectarYSincronizarConServidorNacional();
    }
    
    /**
     * Registra eventos en el log
     */
    private void registrarEnLog(String accion, String resultado, int cantidad, String detalles) {
        String insertLog = 
            "INSERT INTO log_sincronizacion_candidatos (accion, resultado, cantidad, detalles) " +
            "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertLog)) {
            
            pstmt.setString(1, accion);
            pstmt.setString(2, resultado);
            pstmt.setInt(3, cantidad);
            pstmt.setString(4, detalles);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("⚠️ Error registrando en log: " + e.getMessage());
        }
    }
    
    /**
     * Muestra estadísticas de candidatos
     */
    public void mostrarEstadisticas() {
        System.out.println("\n📊 === ESTADÍSTICAS DE CANDIDATOS ===");
        System.out.println("📁 Base de datos: " + DB_PATH);
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Contar candidatos por partido
            String statsSQL = 
                "SELECT partido, COUNT(*) as cantidad " +
                "FROM candidatos_electorales " +
                "WHERE activo = 1 " +
                "GROUP BY partido " +
                "ORDER BY cantidad DESC";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(statsSQL)) {
                
                int totalCandidatos = 0;
                System.out.println("🗳️ Candidatos por partido:");
                
                while (rs.next()) {
                    String partido = rs.getString("partido");
                    int cantidad = rs.getInt("cantidad");
                    totalCandidatos += cantidad;
                    System.out.println("   • " + partido + ": " + cantidad + " candidatos");
                }
                
                System.out.println("📊 Total candidatos activos: " + totalCandidatos);
            }
            
            // Mostrar última sincronización
            String lastSyncSQL = 
                "SELECT timestamp FROM log_sincronizacion_candidatos " +
                "WHERE resultado = 'EXITOSO' " +
                "ORDER BY timestamp DESC LIMIT 1";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(lastSyncSQL)) {
                
                if (rs.next()) {
                    System.out.println("🔄 Última sincronización: " + rs.getString("timestamp"));
                } else {
                    System.out.println("⚠️ No hay registros de sincronización exitosa");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error mostrando estadísticas: " + e.getMessage());
        }
        
        System.out.println("═".repeat(50));
    }
    
    /**
     * Getters
     */
    public String getDbPath() {
        return DB_PATH;
    }
    
    public boolean isConectado() {
        return consultaCandidatosNacional != null;
    }
    
    public boolean estaDisponible() {
        return isConectado() && verificarCandidatosLocalesExisten();
    }
    
    // ========== MÉTODOS ADICIONALES DE LA INTERFAZ ACTUALIZADA ==========
    
    @Override
    public CandidatoElectoral buscarCandidatoPorId(long idCandidato, Current current) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT id, nombre, partido, fecha_creacion, activo FROM candidatos_electorales WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, idCandidato);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        CandidatoElectoral candidato = new CandidatoElectoral();
                        candidato.id = rs.getLong("id");
                        candidato.nombre = rs.getString("nombre");
                        candidato.partido = rs.getString("partido");
                        candidato.fechaCreacion = rs.getString("fecha_creacion");
                        candidato.activo = rs.getBoolean("activo");
                        return candidato;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error buscando candidato por ID: " + e.getMessage());
        }
        return null;
    }
    
    @Override
    public CandidatoElectoral[] buscarCandidatosPorNombre(String nombre, Current current) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            List<CandidatoElectoral> candidatos = new ArrayList<>();
            String sql = "SELECT id, nombre, partido, fecha_creacion, activo FROM candidatos_electorales " +
                        "WHERE nombre LIKE ? AND activo = 1 ORDER BY nombre";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + nombre + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        CandidatoElectoral candidato = new CandidatoElectoral();
                        candidato.id = rs.getLong("id");
                        candidato.nombre = rs.getString("nombre");
                        candidato.partido = rs.getString("partido");
                        candidato.fechaCreacion = rs.getString("fecha_creacion");
                        candidato.activo = rs.getBoolean("activo");
                        candidatos.add(candidato);
                    }
                }
            }
            
            return candidatos.toArray(new CandidatoElectoral[0]);
            
        } catch (SQLException e) {
            System.err.println("❌ Error buscando candidatos por nombre: " + e.getMessage());
            return new CandidatoElectoral[0];
        }
    }
    
    @Override
    public String[] obtenerPartidosDisponibles(Current current) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Set<String> partidos = new HashSet<>();
            String sql = "SELECT DISTINCT partido FROM candidatos_electorales WHERE activo = 1 ORDER BY partido";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String partido = rs.getString("partido");
                    if (partido != null && !partido.trim().isEmpty()) {
                        partidos.add(partido);
                    }
                }
            }
            
            return partidos.toArray(new String[0]);
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo partidos: " + e.getMessage());
            return new String[0];
        }
    }
    
    @Override
    public boolean sincronizarCandidatos(Current current) {
        return forzarSincronizacion();
    }
    
    @Override
    public boolean validarCandidato(long idCandidato, Current current) {
        CandidatoElectoral candidato = buscarCandidatoPorId(idCandidato, current);
        return candidato != null && candidato.activo;
    }
    
    @Override
    public CandidatoElectoral[] obtenerCandidatosParaMesa(String mesaId, Current current) {
        // Por ahora retornamos todos los candidatos activos
        return obtenerTodosCandidatosElectorales(current);
    }
    
    @Override
    public boolean verificarServicio(Current current) {
        return verificarConexionBD(current) && estaDisponible();
    }
} 