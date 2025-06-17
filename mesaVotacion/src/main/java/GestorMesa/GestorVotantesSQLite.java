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
 * Gestor de Votantes SQLite para Mesa de Votación
 * Consume votantes del Servidor Regional via IConsultaMesaSQLite
 * y los almacena en base de datos SQLite local
 */
public class GestorVotantesSQLite {
    
    private final String mesaId;
    private final String DB_PATH;
    private final String DB_URL;
    private final String endpointServidorRegional;
    private Communicator communicator;
    private IConsultaMesaSQLitePrx consultaMesaSQLiteProxy;
    
    public GestorVotantesSQLite(String mesaId, String endpointServidorRegional) {
        this.mesaId = mesaId;
        this.endpointServidorRegional = endpointServidorRegional;
        this.DB_PATH = "data/mesa_" + mesaId + ".sqlite";
        this.DB_URL = "jdbc:sqlite:" + DB_PATH;
        this.communicator = Util.initialize();

        try {
            // Cargar el driver JDBC de SQLite
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ Driver SQLite cargado para Gestor de Votantes");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: Driver SQLite no encontrado. Agregue sqlite-jdbc a las dependencias.");
            throw new RuntimeException(e);
        }

        // Verificar si ya existe base de datos local con datos
        if (verificarBaseDatosLocalExiste()) {
            System.out.println("✅ Base de datos local encontrada: " + DB_PATH);
            System.out.println("📊 Usando votantes almacenados localmente");
        } else {
            System.out.println("⚠️ No hay base de datos local o está vacía");
            System.out.println("🔄 Se buscará en el servidor regional al inicializar");
        }
    }
    
    /**
     * Verifica si existe la base de datos local con votantes
     */
    private boolean verificarBaseDatosLocalExiste() {
        java.io.File dbFile = new java.io.File(DB_PATH);
        if (!dbFile.exists()) {
            System.out.println("📁 Archivo " + DB_PATH + " no existe");
            return false;
        }

        // Verificar si tiene datos
        try {
            inicializarBaseDatosVotantes(); // Crear tablas si no existen
            
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                String countSQL = "SELECT COUNT(*) FROM votantes_mesa";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(countSQL)) {
                    
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count > 0) {
                            System.out.println("📊 Base de datos local tiene " + count + " votantes");
                            return true;
                        } else {
                            System.out.println("📊 Base de datos local existe pero está vacía");
                            return false;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error verificando base de datos local: " + e.getMessage());
            return false;
        }
        
        return false;
    }
    
    /**
     * Inicializa la conexión ICE solo cuando sea necesario
     */
    public boolean inicializarConexionICE() {
        if (consultaMesaSQLiteProxy != null) {
            return true; // Ya está conectado
        }
        
        try {
            System.out.println("🔌 Conectando a Servidor Regional para consultar votantes...");
            System.out.println("   Endpoint: " + endpointServidorRegional);
            
            // Conectar al servicio del Servidor Regional usando endpoint con IConsultaMesaSQLitePrx
            ObjectPrx base = communicator.stringToProxy("consultaMesaSQLite:" + endpointServidorRegional);
            consultaMesaSQLiteProxy = IConsultaMesaSQLitePrx.checkedCast(base);
            
            if (consultaMesaSQLiteProxy == null) {
                System.err.println("❌ No se pudo conectar al servicio IConsultaMesaSQLite del Servidor Regional");
                return false;
            }
            
            // Verificar conectividad
            consultaMesaSQLiteProxy.ice_ping();
            System.out.println("✅ Conexión exitosa al Servidor Regional (IConsultaMesaSQLite)");
            return true;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error conectando al Servidor Regional: " + e.getMessage());
            System.err.println("💡 Verifique que el Servidor Regional esté ejecutándose en " + endpointServidorRegional);
            return false;
        }
    }
    
    /**
     * Consulta votantes del Servidor Regional y los guarda en SQLite local
     */
    public boolean sincronizarVotantes() {
        if (consultaMesaSQLiteProxy == null) {
            System.err.println("❌ No hay conexión al Servidor Regional");
            return false;
        }
        
        try {
            System.out.println("🔄 Consultando votantes desde Servidor Regional para Mesa " + mesaId + "...");
            
            // Crear base de datos local para votantes
            inicializarBaseDatosVotantes();
            
            // Obtener votantes desde el Servidor Regional
            long startTime = System.currentTimeMillis();
            VotanteMesa[] votantes = consultaMesaSQLiteProxy.obtenerVotantesDeMesa(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Votantes obtenidos en " + (endTime - startTime) + "ms");
            
            if (votantes == null || votantes.length == 0) {
                System.out.println("⚠️ No hay votantes asignados a la Mesa " + mesaId);
                registrarEnLog("SINCRONIZAR_VOTANTES", "SIN_DATOS", "No hay votantes para Mesa " + mesaId);
                return true; // No es error, simplemente no hay votantes
            }
            
            // Guardar votantes en SQLite local
            boolean guardadoExitoso = guardarVotantesEnSQLite(votantes);
            
            if (guardadoExitoso) {
                System.out.println("✅ " + votantes.length + " votantes sincronizados exitosamente");
                System.out.println("📁 Base de datos local: " + DB_PATH);
                mostrarResumenVotantes();
                return true;
            } else {
                System.err.println("❌ Error guardando votantes en SQLite");
                return false;
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error sincronizando votantes: " + e.getMessage());
            e.printStackTrace();
            registrarEnLog("SINCRONIZAR_VOTANTES", "ERROR", e.getMessage());
            return false;
        }
    }
    
    /**
     * Crea la estructura de base de datos SQLite para votantes
     */
    private void inicializarBaseDatosVotantes() throws SQLException {
        // Crear directorio data si no existe
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            System.out.println("📁 Directorio 'data' creado");
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Tabla de votantes para la mesa
            String createVotantesTable = 
                "CREATE TABLE IF NOT EXISTS votantes_mesa (" +
                "    documento TEXT PRIMARY KEY," +
                "    nombre TEXT NOT NULL," +
                "    apellido TEXT NOT NULL," +
                "    mesa TEXT NOT NULL," +
                "    verificado INTEGER DEFAULT 0," +
                "    fecha_asignacion TEXT," +
                "    fecha_sincronizacion TEXT NOT NULL," +
                "    ha_votado INTEGER DEFAULT 0," +
                "    fecha_voto TEXT" +
                ")";
            
            // Tabla de estadísticas de la mesa
            String createEstadisticasTable = 
                "CREATE TABLE IF NOT EXISTS estadisticas_mesa (" +
                "    mesa_id TEXT PRIMARY KEY," +
                "    departamento TEXT," +
                "    municipio TEXT," +
                "    puesto TEXT," +
                "    total_votantes INTEGER DEFAULT 0," +
                "    votantes_verificados INTEGER DEFAULT 0," +
                "    mesa_activa INTEGER DEFAULT 1," +
                "    fecha_creacion TEXT," +
                "    ultima_actualizacion TEXT," +
                "    fecha_sincronizacion TEXT NOT NULL" +
                ")";
            
            // Tabla de log de sincronización
            String createLogTable = 
                "CREATE TABLE IF NOT EXISTS log_sincronizacion (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    accion TEXT NOT NULL," +
                "    resultado TEXT NOT NULL," +
                "    detalles TEXT," +
                "    timestamp TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))" +
                ")";
            
            Statement stmt = conn.createStatement();
            stmt.execute(createVotantesTable);
            stmt.execute(createEstadisticasTable);
            stmt.execute(createLogTable);
            
            System.out.println("✅ Base de datos de votantes inicializada");
            
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando base de datos: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Guarda los votantes en la base de datos SQLite local
     */
    private boolean guardarVotantesEnSQLite(VotanteMesa[] votantes) {
        String insertVotante = 
            "INSERT OR REPLACE INTO votantes_mesa " +
            "(documento, nombre, apellido, mesa, verificado, fecha_asignacion, fecha_sincronizacion) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            
            // Guardar votantes
            try (PreparedStatement pstmt = conn.prepareStatement(insertVotante)) {
                String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                for (VotanteMesa votante : votantes) {
                    pstmt.setString(1, votante.documento);
                    pstmt.setString(2, votante.nombre);
                    pstmt.setString(3, votante.apellido);
                    pstmt.setString(4, votante.mesa);
                    pstmt.setInt(5, votante.verificado);
                    pstmt.setString(6, votante.fechaAsignacion);
                    pstmt.setString(7, fechaActual);
                    pstmt.addBatch();
                }
                
                int[] resultados = pstmt.executeBatch();
                System.out.println("✅ " + resultados.length + " votantes guardados en SQLite");
            }
            
            // Obtener y guardar estadísticas de la mesa
            try {
                EstadisticasMesaSQLite stats = consultaMesaSQLiteProxy.obtenerEstadisticasMesa(mesaId);
                guardarEstadisticasMesa(conn, stats);
            } catch (java.lang.Exception e) {
                System.err.println("⚠️ No se pudieron obtener estadísticas de la mesa: " + e.getMessage());
            }
            
            // Registrar en log
            registrarEnLog("SINCRONIZAR_VOTANTES", "EXITOSO", 
                votantes.length + " votantes guardados para Mesa " + mesaId);
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando en SQLite: " + e.getMessage());
            registrarEnLog("SINCRONIZAR_VOTANTES", "ERROR", e.getMessage());
            return false;
        }
    }
    
    /**
     * Guarda las estadísticas de la mesa
     */
    private void guardarEstadisticasMesa(Connection conn, EstadisticasMesaSQLite stats) throws SQLException {
        String insertStats = 
            "INSERT OR REPLACE INTO estadisticas_mesa " +
            "(mesa_id, departamento, municipio, puesto, total_votantes, votantes_verificados, " +
            " mesa_activa, fecha_creacion, ultima_actualizacion, fecha_sincronizacion) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertStats)) {
            String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            pstmt.setString(1, stats.mesaId);
            pstmt.setString(2, stats.departamento);
            pstmt.setString(3, stats.municipio);
            pstmt.setString(4, stats.puesto);
            pstmt.setInt(5, stats.totalVotantes);
            pstmt.setInt(6, stats.votantesVerificados);
            pstmt.setInt(7, stats.mesaActiva);
            pstmt.setString(8, stats.fechaCreacion);
            pstmt.setString(9, stats.ultimaActualizacion > 0 ? String.valueOf(stats.ultimaActualizacion) : null);
            pstmt.setString(10, fechaActual);
            
            pstmt.executeUpdate();
            System.out.println("✅ Estadísticas de mesa guardadas");
        }
    }
    
    /**
     * Obtiene votantes: primero verifica local, luego servidor regional si es necesario
     */
    public List<VotanteMesa> obtenerVotantesLocales() {
        System.out.println("🔄 Obteniendo votantes para Mesa " + mesaId + "...");
        
        // 1. Verificar si tenemos datos locales
        if (verificarBaseDatosLocalExiste()) {
            System.out.println("📊 Usando votantes de base de datos local");
            return obtenerVotantesDesdeBaseDatosLocal();
        }
        
        // 2. Si no hay datos locales, buscar en servidor regional
        System.out.println("🌐 Buscando votantes en Servidor Regional...");
        
        if (!inicializarConexionICE()) {
            System.err.println("❌ No se pudo conectar al Servidor Regional");
            return new ArrayList<>();
        }
        
        try {
            VotanteMesa[] votantes = consultaMesaSQLiteProxy.obtenerVotantesDeMesa(mesaId);
            
            if (votantes == null || votantes.length == 0) {
                System.out.println("⚠️ No hay votantes asignados a la Mesa " + mesaId + " en el servidor regional");
                return new ArrayList<>();
            }
            
            // Guardar en base de datos local para futuras ejecuciones
            if (guardarVotantesEnSQLite(votantes)) {
                System.out.println("✅ " + votantes.length + " votantes guardados en base de datos local");
                System.out.println("📁 Próximas ejecuciones usarán datos locales: " + DB_PATH);
            }
            
            return Arrays.asList(votantes);
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error obteniendo votantes desde Servidor Regional: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Obtiene votantes desde la base de datos SQLite local
     */
    private List<VotanteMesa> obtenerVotantesDesdeBaseDatosLocal() {
        List<VotanteMesa> votantes = new ArrayList<>();
        String selectSQL = "SELECT documento, nombre, apellido, mesa, verificado, fecha_asignacion FROM votantes_mesa";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                VotanteMesa votante = new VotanteMesa();
                votante.documento = rs.getString("documento");
                votante.nombre = rs.getString("nombre");
                votante.apellido = rs.getString("apellido");
                votante.mesa = rs.getString("mesa");
                votante.verificado = rs.getInt("verificado");
                votante.fechaAsignacion = rs.getString("fecha_asignacion");
                
                votantes.add(votante);
            }
            
            System.out.println("✅ " + votantes.size() + " votantes cargados desde base de datos local");
            
        } catch (SQLException e) {
            System.err.println("❌ Error cargando votantes desde base de datos local: " + e.getMessage());
        }
        
        return votantes;
    }
    
    /**
     * Verifica si un votante puede votar (está en la lista y no ha votado)
     */
    public boolean verificarVotante(String documento) {
        String selectSQL = "SELECT documento, ha_votado FROM votantes_mesa WHERE documento = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setString(1, documento);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                boolean haVotado = rs.getInt("ha_votado") == 1;
                if (haVotado) {
                    System.out.println("⚠️ El votante " + documento + " ya ejerció su derecho al voto");
                    return false;
                } else {
                    System.out.println("✅ Votante " + documento + " autorizado para votar");
                    return true;
                }
            } else {
                System.out.println("❌ Votante " + documento + " no está asignado a esta mesa");
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error verificando votante: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Registra que un votante ha ejercido su voto
     */
    public boolean registrarVoto(String documento) {
        String updateSQL = "UPDATE votantes_mesa SET ha_votado = 1, fecha_voto = ? WHERE documento = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            pstmt.setString(1, fechaActual);
            pstmt.setString(2, documento);
            
            int filasActualizadas = pstmt.executeUpdate();
            
            if (filasActualizadas > 0) {
                System.out.println("✅ Voto registrado para " + documento);
                registrarEnLog("REGISTRAR_VOTO", "EXITOSO", "Votante: " + documento);
                return true;
            } else {
                System.err.println("❌ No se pudo registrar voto para " + documento);
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error registrando voto: " + e.getMessage());
            registrarEnLog("REGISTRAR_VOTO", "ERROR", e.getMessage());
            return false;
        }
    }
    
    /**
     * Muestra resumen de votantes y estadísticas
     */
    public void mostrarResumenVotantes() {
        String selectSQL = 
            "SELECT COUNT(*) as total, " +
            "COUNT(CASE WHEN verificado = 1 THEN 1 END) as verificados, " +
            "COUNT(CASE WHEN ha_votado = 1 THEN 1 END) as han_votado " +
            "FROM votantes_mesa";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            if (rs.next()) {
                int total = rs.getInt("total");
                int verificados = rs.getInt("verificados");
                int hanVotado = rs.getInt("han_votado");
                
                System.out.println("\n📊 === RESUMEN DE VOTANTES MESA " + mesaId + " ===");
                System.out.println("📁 Base de datos: " + DB_PATH);
                System.out.println("═".repeat(50));
                System.out.println("👥 Total Votantes: " + total);
                System.out.println("✅ Verificados: " + verificados);
                System.out.println("🗳️ Han Votado: " + hanVotado);
                System.out.println("⏳ Pendientes: " + (total - hanVotado));
                if (total > 0) {
                    double porcentajeParticipacion = (hanVotado * 100.0) / total;
                    System.out.printf("📈 Participación: %.1f%%\n", porcentajeParticipacion);
                }
                System.out.println("═".repeat(50));
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error mostrando resumen: " + e.getMessage());
        }
    }
    
    /**
     * Verifica la disponibilidad de la base de datos
     */
    public boolean verificarBaseDatos() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            System.out.println("✅ Base de datos de votantes disponible: " + DB_PATH);
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Error verificando base de datos: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Registra eventos en el log
     */
    private void registrarEnLog(String accion, String resultado, String detalles) {
        String insertSQL = "INSERT INTO log_sincronizacion (accion, resultado, detalles) VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            pstmt.setString(1, accion);
            pstmt.setString(2, resultado);
            pstmt.setString(3, detalles);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            // No es crítico si falla el log
            System.err.println("⚠️ Error registrando en log: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene estadísticas desde la base de datos local
     */
    public EstadisticasMesaSQLite obtenerEstadisticasLocales() {
        String selectSQL = "SELECT * FROM estadisticas_mesa WHERE mesa_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setString(1, mesaId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                EstadisticasMesaSQLite stats = new EstadisticasMesaSQLite();
                stats.mesaId = rs.getString("mesa_id");
                stats.departamento = rs.getString("departamento");
                stats.municipio = rs.getString("municipio");
                stats.puesto = rs.getString("puesto");
                stats.totalVotantes = rs.getInt("total_votantes");
                stats.votantesVerificados = rs.getInt("votantes_verificados");
                stats.mesaActiva = rs.getInt("mesa_activa");
                stats.fechaCreacion = rs.getString("fecha_creacion");
                
                String ultimaActStr = rs.getString("ultima_actualizacion");
                stats.ultimaActualizacion = ultimaActStr != null ? Long.parseLong(ultimaActStr) : 0;
                
                return stats;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo estadísticas locales: " + e.getMessage());
        }
        
        return null;
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
        return consultaMesaSQLiteProxy != null;
    }
    
    public IConsultaMesaSQLitePrx getProxy() {
        return consultaMesaSQLiteProxy;
    }
} 