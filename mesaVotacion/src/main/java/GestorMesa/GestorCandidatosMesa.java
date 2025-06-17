package GestorMesa;

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
import java.util.Arrays;
import java.util.List;

/**
 * Gestor de Candidatos SQLite para Mesa de Votación
 * Consulta candidatos del Servidor Regional y los persiste localmente
 */
public class GestorCandidatosMesa {
    
    private final String mesaId;
    private final String DB_PATH;
    private final String DB_URL;
    private final String endpointServidorRegional;
    private Communicator communicator;
    private IConsultaCandidatosPrx consultaCandidatosProxy;
    
    public GestorCandidatosMesa(String mesaId, String endpointServidorRegional) {
        this.mesaId = mesaId;
        this.endpointServidorRegional = endpointServidorRegional;
        this.DB_PATH = "data/candidatos_mesa_" + mesaId + ".sqlite";
        this.DB_URL = "jdbc:sqlite:" + DB_PATH;
        this.communicator = Util.initialize();

        try {
            // Cargar el driver JDBC de SQLite
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ Driver SQLite cargado para Gestor de Candidatos Mesa");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: Driver SQLite no encontrado");
            throw new RuntimeException(e);
        }

        // Verificar si ya existe base de datos local con candidatos
        if (verificarCandidatosLocalesExisten()) {
            System.out.println("✅ Candidatos encontrados en base de datos local: " + DB_PATH);
            System.out.println("📊 Usando candidatos almacenados localmente");
        } else {
            System.out.println("⚠️ No hay candidatos locales o base de datos está vacía");
            System.out.println("🔄 Se consultará el servidor regional para obtener candidatos");
        }
    }
    
    /**
     * Verifica si existe la base de datos local con candidatos
     */
    private boolean verificarCandidatosLocalesExisten() {
        java.io.File dbFile = new java.io.File(DB_PATH);
        if (!dbFile.exists()) {
            System.out.println("📁 Archivo " + DB_PATH + " no existe");
            return false;
        }

        // Verificar si tiene datos
        try {
            inicializarBaseDatosCandidatos(); // Crear tablas si no existen
            
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                String countSQL = "SELECT COUNT(*) FROM candidatos_mesa WHERE activo = 1";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(countSQL)) {
                    
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count > 0) {
                            System.out.println("📊 Base de datos local tiene " + count + " candidatos");
                            return true;
                        } else {
                            System.out.println("📊 Base de datos local existe pero está vacía");
                            return false;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error verificando base de datos local de candidatos: " + e.getMessage());
            return false;
        }
        
        return false;
    }
    
    /**
     * Inicializa la conexión ICE al servidor regional solo cuando sea necesario
     */
    public boolean inicializarConexionICE() {
        if (consultaCandidatosProxy != null) {
            return true; // Ya está conectado
        }
        
        try {
            System.out.println("🔌 Conectando a Servidor Regional para consultar candidatos...");
            System.out.println("   Endpoint: " + endpointServidorRegional);
            
            // Conectar al servicio de candidatos del Servidor Regional
            ObjectPrx base = communicator.stringToProxy("consultaCandidatos:" + endpointServidorRegional);
            consultaCandidatosProxy = IConsultaCandidatosPrx.checkedCast(base);
            
            if (consultaCandidatosProxy == null) {
                System.err.println("❌ No se pudo conectar al servicio de candidatos del Servidor Regional");
                return false;
            }
            
            // Verificar conectividad
            consultaCandidatosProxy.ice_ping();
            System.out.println("✅ Conexión exitosa al Servidor Regional (Candidatos)");
            return true;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error conectando al Servidor Regional: " + e.getMessage());
            System.err.println("💡 Verifique que el Servidor Regional esté ejecutándose en " + endpointServidorRegional);
            return false;
        }
    }
    
    /**
     * Sincroniza candidatos desde el servidor regional
     */
    public boolean sincronizarCandidatos() {
        try {
            if (!inicializarConexionICE()) {
                System.err.println("❌ No se pudo conectar al Servidor Regional para candidatos");
                return false;
            }
            
            System.out.println("🔄 Sincronizando candidatos desde Servidor Regional...");
            
            CandidatoElectoral[] candidatos = consultaCandidatosProxy.obtenerTodosCandidatosElectorales();
            
            if (candidatos == null || candidatos.length == 0) {
                System.out.println("⚠️ No hay candidatos disponibles en el Servidor Regional");
                registrarEnLog("SINCRONIZAR_CANDIDATOS", "SIN_DATOS", "No hay candidatos en servidor regional");
                return false;
            }
            
            // Guardar candidatos en base de datos local
            if (guardarCandidatosEnSQLite(candidatos)) {
                System.out.println("✅ " + candidatos.length + " candidatos sincronizados para Mesa " + mesaId);
                System.out.println("📁 Próximas ejecuciones usarán datos locales: " + DB_PATH);
                registrarEnLog("SINCRONIZAR_CANDIDATOS", "EXITOSO", 
                    candidatos.length + " candidatos sincronizados desde " + endpointServidorRegional);
                return true;
            }
            
            return false;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error sincronizando candidatos: " + e.getMessage());
            e.printStackTrace();
            registrarEnLog("SINCRONIZAR_CANDIDATOS", "ERROR", e.getMessage());
            return false;
        }
    }
    
    /**
     * Crea la estructura de base de datos SQLite para candidatos
     */
    private void inicializarBaseDatosCandidatos() throws SQLException {
        // Crear directorio data si no existe
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            System.out.println("📁 Directorio 'data' creado");
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Tabla de candidatos para la mesa
            String createCandidatosTable = 
                "CREATE TABLE IF NOT EXISTS candidatos_mesa (" +
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
                "    detalles TEXT," +
                "    timestamp TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))" +
                ")";
            
            Statement stmt = conn.createStatement();
            stmt.execute(createCandidatosTable);
            stmt.execute(createLogTable);
            
            System.out.println("✅ Base de datos de candidatos inicializada");
            
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando base de datos candidatos: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Guarda los candidatos en la base de datos SQLite local
     */
    private boolean guardarCandidatosEnSQLite(CandidatoElectoral[] candidatos) {
        String insertCandidato = 
            "INSERT OR REPLACE INTO candidatos_mesa " +
            "(id, nombre, partido, fecha_creacion, activo, fecha_sincronizacion) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            
            // Limpiar candidatos existentes
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM candidatos_mesa");
            }
            
            // Guardar candidatos
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
                
                int[] resultados = pstmt.executeBatch();
                System.out.println("✅ " + resultados.length + " candidatos guardados en SQLite");
            }
            
            // Registrar en log
            registrarEnLog("GUARDAR_CANDIDATOS", "EXITOSO", 
                candidatos.length + " candidatos guardados para Mesa " + mesaId);
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando candidatos en SQLite: " + e.getMessage());
            registrarEnLog("GUARDAR_CANDIDATOS", "ERROR", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene candidatos: primero verifica local, luego servidor regional si es necesario
     */
    public List<Candidato> obtenerCandidatosLocales() {
        System.out.println("🔄 Obteniendo candidatos para Mesa " + mesaId + "...");
        
        // 1. Verificar si tenemos datos locales
        if (verificarCandidatosLocalesExisten()) {
            System.out.println("📊 Usando candidatos de base de datos local");
            return obtenerCandidatosDesdeBaseDatosLocal();
        }
        
        // 2. Si no hay datos locales, buscar en servidor regional
        System.out.println("🌐 Consultando candidatos en Servidor Regional...");
        
        if (!inicializarConexionICE()) {
            System.err.println("❌ No se pudo conectar al Servidor Regional");
            return new ArrayList<>();
        }
        
        try {
            CandidatoElectoral[] candidatos = consultaCandidatosProxy.obtenerTodosCandidatosElectorales();
            
            if (candidatos == null || candidatos.length == 0) {
                System.out.println("⚠️ No hay candidatos disponibles en el servidor regional");
                return new ArrayList<>();
            }
            
            // Guardar en base de datos local para futuras ejecuciones
            if (guardarCandidatosEnSQLite(candidatos)) {
                System.out.println("✅ " + candidatos.length + " candidatos guardados en base de datos local");
                System.out.println("📁 Próximas ejecuciones usarán datos locales: " + DB_PATH);
            }
            
            // Convertir a formato simple para la mesa
            List<Candidato> candidatosSimples = new ArrayList<>();
            for (CandidatoElectoral ce : candidatos) {
                if (ce.activo) {
                    candidatosSimples.add(new Candidato(ce.id, ce.nombre, ce.partido));
                }
            }
            
            return candidatosSimples;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error obteniendo candidatos desde Servidor Regional: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Obtiene candidatos desde la base de datos SQLite local
     */
    private List<Candidato> obtenerCandidatosDesdeBaseDatosLocal() {
        List<Candidato> candidatos = new ArrayList<>();
        String selectSQL = "SELECT id, nombre, partido FROM candidatos_mesa WHERE activo = 1 ORDER BY id";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                Candidato candidato = new Candidato();
                candidato.idCandidato = rs.getLong("id");
                candidato.nombre = rs.getString("nombre");
                candidato.partido = rs.getString("partido");
                
                candidatos.add(candidato);
            }
            
            System.out.println("✅ " + candidatos.size() + " candidatos cargados desde base de datos local");
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando candidatos desde base de datos local: " + e.getMessage());
        }
        
        return candidatos;
    }
    
    /**
     * Fuerza una nueva sincronización con el servidor regional
     */
    public boolean forzarSincronizacion() {
        System.out.println("🔄 Forzando sincronización de candidatos con Servidor Regional...");
        return sincronizarCandidatos();
    }
    
    /**
     * Muestra estadísticas de candidatos
     */
    public void mostrarEstadisticas() {
        System.out.println("\n📊 === ESTADÍSTICAS DE CANDIDATOS MESA " + mesaId + " ===");
        System.out.println("📁 Base de datos: " + DB_PATH);
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Contar candidatos por partido
            String statsSQL = 
                "SELECT partido, COUNT(*) as cantidad " +
                "FROM candidatos_mesa " +
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
                
                System.out.println("📊 Total candidatos: " + totalCandidatos);
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
     * Registra eventos en el log
     */
    private void registrarEnLog(String accion, String resultado, String detalles) {
        String insertLog = 
            "INSERT INTO log_sincronizacion_candidatos (accion, resultado, detalles) " +
            "VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertLog)) {
            
            pstmt.setString(1, accion);
            pstmt.setString(2, resultado);
            pstmt.setString(3, detalles);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("⚠️ Error registrando en log candidatos: " + e.getMessage());
        }
    }
    
    /**
     * Getters
     */
    public String getMesaId() {
        return mesaId;
    }
    
    public String getDbPath() {
        return DB_PATH;
    }
    
    public boolean isConectado() {
        return consultaCandidatosProxy != null;
    }
    
    public boolean estaDisponible() {
        return verificarCandidatosLocalesExisten();
    }
} 