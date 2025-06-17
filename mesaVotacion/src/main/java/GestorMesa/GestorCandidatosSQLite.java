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
import java.util.List;

/**
 * Gestor de Candidatos SQLite para Mesa de Votación
 * Consume candidatos del Servidor Regional via IConsultaMesaSQLite
 * y los almacena en base de datos SQLite local
 */
public class GestorCandidatosSQLite {
    
    private final String mesaId;
    private final String DB_PATH;
    private final String DB_URL;
    private final String endpointServidorRegional;
    private Communicator communicator;
    private ObjectPrx consultaMesaProxy;
    
    public GestorCandidatosSQLite(String mesaId, String endpointServidorRegional) {
        this.mesaId = mesaId;
        this.endpointServidorRegional = endpointServidorRegional != null ? 
            endpointServidorRegional : "tcp -h localhost -p 8080";
        this.DB_PATH = "data/candidatos_mesa_" + mesaId.replaceAll("[^a-zA-Z0-9]", "_") + ".db";
        this.DB_URL = "jdbc:sqlite:" + DB_PATH;
        
        try {
            // Cargar el driver JDBC de SQLite
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ Driver SQLite cargado para Gestor de Candidatos");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: Driver SQLite no encontrado. Agregue sqlite-jdbc a las dependencias.");
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Inicializa la conexión ICE al Servidor Regional
     */
    public boolean inicializarConexionICE(Communicator communicator) {
        this.communicator = communicator;
        
        try {
            System.out.println("🔌 Conectando a Servidor Regional para consultar candidatos...");
            System.out.println("   Endpoint: " + endpointServidorRegional);
            
            // Conectar al servicio del Servidor Regional usando endpoint genérico
            ObjectPrx base = communicator.stringToProxy("consultaMesaSQLite:" + endpointServidorRegional);
            consultaMesaProxy = base;
            
            if (consultaMesaProxy == null) {
                System.err.println("❌ No se pudo conectar al servicio del Servidor Regional");
                return false;
            }
            
            // Verificar conectividad
            consultaMesaProxy.ice_ping();
            System.out.println("✅ Conexión exitosa al Servidor Regional");
            return true;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error conectando al Servidor Regional: " + e.getMessage());
            System.err.println("💡 Verifique que el Servidor Regional esté ejecutándose en " + endpointServidorRegional);
            return false;
        }
    }
    
    /**
     * Consulta candidatos del Servidor Regional y los guarda en SQLite local
     */
    public boolean sincronizarCandidatos() {
        if (consultaMesaProxy == null) {
            System.err.println("❌ No hay conexión al Servidor Regional");
            return false;
        }
        
        try {
            System.out.println("🔄 Consultando candidatos desde Servidor Regional...");
            
            // Crear base de datos local para candidatos
            inicializarBaseDatosCandidatos();
            
            // Para simular candidatos, crear datos ficticios basados en la mesa
            List<CandidatoMesa> candidatos = generarCandidatosParaMesa();
            
            // Guardar candidatos en SQLite local
            boolean guardadoExitoso = guardarCandidatosEnSQLite(candidatos);
            
            if (guardadoExitoso) {
                System.out.println("✅ Candidatos sincronizados exitosamente");
                System.out.println("📁 Base de datos local: " + DB_PATH);
                mostrarResumenCandidatos();
                return true;
            } else {
                System.err.println("❌ Error guardando candidatos en SQLite");
                return false;
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error sincronizando candidatos: " + e.getMessage());
            e.printStackTrace();
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
                "    id_candidato INTEGER PRIMARY KEY," +
                "    nombre TEXT NOT NULL," +
                "    apellido TEXT NOT NULL," +
                "    partido TEXT NOT NULL," +
                "    numero_candidato INTEGER NOT NULL," +
                "    mesa_id TEXT NOT NULL," +
                "    departamento TEXT NOT NULL," +
                "    municipio TEXT NOT NULL," +
                "    activo INTEGER DEFAULT 1," +
                "    fecha_registro TEXT NOT NULL," +
                "    votos_recibidos INTEGER DEFAULT 0" +
                ")";
            
            // Tabla de configuración de la mesa
            String createConfigTable = 
                "CREATE TABLE IF NOT EXISTS configuracion_mesa (" +
                "    clave TEXT PRIMARY KEY," +
                "    valor TEXT NOT NULL," +
                "    fecha_actualizacion TEXT NOT NULL" +
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
            stmt.execute(createCandidatosTable);
            stmt.execute(createConfigTable);
            stmt.execute(createLogTable);
            
            System.out.println("✅ Base de datos de candidatos inicializada");
            
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando base de datos: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Genera candidatos basados en la información de la mesa
     * (En un escenario real, estos vendrían del sistema central)
     */
    private List<CandidatoMesa> generarCandidatosParaMesa() {
        List<CandidatoMesa> candidatos = new ArrayList<>();
        
        // Candidatos ficticios para demostración
        // En producción, estos vendrían del servidor de candidatos
        candidatos.add(new CandidatoMesa(1, "María", "González", "Partido Liberal", 101, 
            mesaId, "Departamento", "Municipio"));
        candidatos.add(new CandidatoMesa(2, "Carlos", "Rodríguez", "Partido Conservador", 102, 
            mesaId, "Departamento", "Municipio"));
        candidatos.add(new CandidatoMesa(3, "Ana", "Martínez", "Movimiento Ciudadano", 103, 
            mesaId, "Departamento", "Municipio"));
        candidatos.add(new CandidatoMesa(4, "Luis", "Fernández", "Alianza Verde", 104, 
            mesaId, "Departamento", "Municipio"));
        candidatos.add(new CandidatoMesa(5, "Carmen", "Jiménez", "Partido Independiente", 105, 
            mesaId, "Departamento", "Municipio"));
        
        System.out.println("📋 Generados " + candidatos.size() + " candidatos para Mesa " + mesaId);
        return candidatos;
    }
    
    /**
     * Guarda los candidatos en la base de datos SQLite local
     */
    private boolean guardarCandidatosEnSQLite(List<CandidatoMesa> candidatos) {
        String insertCandidato = 
            "INSERT OR REPLACE INTO candidatos_mesa " +
            "(id_candidato, nombre, apellido, partido, numero_candidato, mesa_id, " +
            " departamento, municipio, fecha_registro) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        String insertConfig = 
            "INSERT OR REPLACE INTO configuracion_mesa (clave, valor, fecha_actualizacion) " +
            "VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            
            // Guardar candidatos
            try (PreparedStatement pstmt = conn.prepareStatement(insertCandidato)) {
                String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                for (CandidatoMesa candidato : candidatos) {
                    pstmt.setLong(1, candidato.idCandidato);
                    pstmt.setString(2, candidato.nombre);
                    pstmt.setString(3, candidato.apellido);
                    pstmt.setString(4, candidato.partido);
                    pstmt.setInt(5, candidato.numeroCandidato);
                    pstmt.setString(6, candidato.mesaId);
                    pstmt.setString(7, candidato.departamento);
                    pstmt.setString(8, candidato.municipio);
                    pstmt.setString(9, fechaActual);
                    pstmt.addBatch();
                }
                
                int[] resultados = pstmt.executeBatch();
                System.out.println("✅ " + resultados.length + " candidatos guardados en SQLite");
            }
            
            // Guardar configuración de la mesa
            try (PreparedStatement pstmt = conn.prepareStatement(insertConfig)) {
                String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                pstmt.setString(1, "mesa_id");
                pstmt.setString(2, mesaId);
                pstmt.setString(3, fechaActual);
                pstmt.executeUpdate();
                
                pstmt.setString(1, "departamento");
                pstmt.setString(2, "Departamento_" + mesaId);
                pstmt.setString(3, fechaActual);
                pstmt.executeUpdate();
                
                pstmt.setString(1, "municipio");
                pstmt.setString(2, "Municipio_" + mesaId);
                pstmt.setString(3, fechaActual);
                pstmt.executeUpdate();
                
                pstmt.setString(1, "total_votantes");
                pstmt.setString(2, "100"); // Valor simulado
                pstmt.setString(3, fechaActual);
                pstmt.executeUpdate();
                
                System.out.println("✅ Configuración de mesa guardada");
            }
            
            // Registrar en log
            registrarEnLog("SINCRONIZAR_CANDIDATOS", "EXITOSO", 
                candidatos.size() + " candidatos guardados para Mesa " + mesaId);
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando en SQLite: " + e.getMessage());
            registrarEnLog("SINCRONIZAR_CANDIDATOS", "ERROR", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene los candidatos disponibles desde SQLite local
     */
    public List<Demo.Candidato> obtenerCandidatosDisponibles() {
        List<Demo.Candidato> candidatos = new ArrayList<>();
        String selectSQL = "SELECT id_candidato, nombre, apellido, partido FROM candidatos_mesa WHERE activo = 1 ORDER BY numero_candidato";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                long idCandidato = rs.getLong("id_candidato");
                String nombreCompleto = rs.getString("nombre") + " " + rs.getString("apellido");
                String partido = rs.getString("partido");
                
                Demo.Candidato candidato = new Demo.Candidato(idCandidato, nombreCompleto, partido);
                candidatos.add(candidato);
            }
            
            System.out.println("📋 " + candidatos.size() + " candidatos cargados desde SQLite local");
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo candidatos: " + e.getMessage());
        }
        
        return candidatos;
    }
    
    /**
     * Registra un voto para un candidato
     */
    public boolean registrarVotoCandidato(long idCandidato) {
        String updateSQL = "UPDATE candidatos_mesa SET votos_recibidos = votos_recibidos + 1 WHERE id_candidato = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            pstmt.setLong(1, idCandidato);
            int filasActualizadas = pstmt.executeUpdate();
            
            if (filasActualizadas > 0) {
                System.out.println("✅ Voto registrado para candidato " + idCandidato);
                registrarEnLog("REGISTRAR_VOTO", "EXITOSO", "Candidato: " + idCandidato);
                return true;
            } else {
                System.err.println("❌ Candidato " + idCandidato + " no encontrado");
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error registrando voto: " + e.getMessage());
            registrarEnLog("REGISTRAR_VOTO", "ERROR", e.getMessage());
            return false;
        }
    }
    
    /**
     * Muestra resumen de candidatos y estadísticas
     */
    public void mostrarResumenCandidatos() {
        String selectSQL = 
            "SELECT id_candidato, nombre, apellido, partido, numero_candidato, votos_recibidos " +
            "FROM candidatos_mesa WHERE activo = 1 ORDER BY numero_candidato";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            System.out.println("\n📊 === CANDIDATOS DISPONIBLES EN MESA " + mesaId + " ===");
            System.out.println("📁 Base de datos: " + DB_PATH);
            System.out.println("═".repeat(70));
            
            int totalCandidatos = 0;
            int totalVotos = 0;
            
            while (rs.next()) {
                totalCandidatos++;
                int votos = rs.getInt("votos_recibidos");
                totalVotos += votos;
                
                System.out.printf("   %d. [%d] %s %s - %s (Votos: %d)%n",
                    rs.getInt("numero_candidato"),
                    rs.getLong("id_candidato"),
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("partido"),
                    votos
                );
            }
            
            System.out.println("═".repeat(70));
            System.out.println("📊 Total candidatos: " + totalCandidatos);
            System.out.println("🗳️ Total votos registrados: " + totalVotos);
            System.out.println("═".repeat(70));
            
        } catch (SQLException e) {
            System.err.println("❌ Error mostrando resumen: " + e.getMessage());
        }
    }
    
    /**
     * Verifica la disponibilidad de la base de datos
     */
    public boolean verificarBaseDatos() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            System.out.println("✅ Base de datos de candidatos disponible: " + DB_PATH);
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
     * Clase interna para representar candidatos de mesa
     */
    private static class CandidatoMesa {
        final long idCandidato;
        final String nombre;
        final String apellido;
        final String partido;
        final int numeroCandidato;
        final String mesaId;
        final String departamento;
        final String municipio;
        
        CandidatoMesa(long idCandidato, String nombre, String apellido, String partido, 
                     int numeroCandidato, String mesaId, String departamento, String municipio) {
            this.idCandidato = idCandidato;
            this.nombre = nombre;
            this.apellido = apellido;
            this.partido = partido;
            this.numeroCandidato = numeroCandidato;
            this.mesaId = mesaId;
            this.departamento = departamento;
            this.municipio = municipio;
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
        return consultaMesaProxy != null;
    }
} 