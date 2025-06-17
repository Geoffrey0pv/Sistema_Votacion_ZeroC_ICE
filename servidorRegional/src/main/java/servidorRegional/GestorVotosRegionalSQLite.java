package servidorRegional;

import Demo.*;
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
 * Gestor SQLite para Votos en el Servidor Regional
 * Maneja el almacenamiento y consulta de votos recibidos de las mesas de votación
 */
public class GestorVotosRegionalSQLite {
    
    private final String DB_PATH = "data/votos_regional.sqlite";
    private final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    
    public GestorVotosRegionalSQLite() {
        try {
            // Cargar el driver JDBC de SQLite
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ Driver SQLite cargado para Gestor de Votos Regional");
            
            // Crear directorio data si no existe
            java.io.File dataDir = new java.io.File("data");
            if (!dataDir.exists()) {
                boolean creado = dataDir.mkdirs();
                if (creado) {
                    System.out.println("📁 Directorio 'data' creado para votos regionales");
                }
            }
            
            // Inicializar base de datos
            inicializarBaseDatos();
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: Driver SQLite no encontrado para votos regionales");
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Inicializa la base de datos y crea las tablas necesarias
     */
    private void inicializarBaseDatos() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS votos_regionales (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "id_voto INTEGER UNIQUE NOT NULL," +
            "mesa_id TEXT NOT NULL," +
            "timestamp INTEGER NOT NULL," +
            "candidato_id INTEGER NOT NULL," +
            "hash_elector TEXT NOT NULL," +
            "municipio TEXT," +
            "departamento TEXT," +
            "estado_registro TEXT DEFAULT 'NUEVO'," +
            "fecha_recepcion TEXT NOT NULL," +
            "fecha_procesamiento TEXT," +
            "sincronizado BOOLEAN DEFAULT FALSE," +
            "fecha_sincronizacion TEXT," +
            "intentos_sincronizacion INTEGER DEFAULT 0," +
            "UNIQUE(hash_elector, mesa_id)" +
            ")";
        
        // Añadir columnas de sincronización si no existen (para compatibilidad con DBs existentes)
        String addSincronizadoSQL = "ALTER TABLE votos_regionales ADD COLUMN sincronizado BOOLEAN DEFAULT FALSE";
        String addFechaSincronizacionSQL = "ALTER TABLE votos_regionales ADD COLUMN fecha_sincronizacion TEXT";
        String addIntentosSincronizacionSQL = "ALTER TABLE votos_regionales ADD COLUMN intentos_sincronizacion INTEGER DEFAULT 0";
        
        String createIndexSQL1 = "CREATE INDEX IF NOT EXISTS idx_votos_mesa ON votos_regionales(mesa_id)";
        String createIndexSQL2 = "CREATE INDEX IF NOT EXISTS idx_votos_candidato ON votos_regionales(candidato_id)";
        String createIndexSQL3 = "CREATE INDEX IF NOT EXISTS idx_votos_hash ON votos_regionales(hash_elector)";
        String createIndexSQL4 = "CREATE INDEX IF NOT EXISTS idx_votos_timestamp ON votos_regionales(timestamp)";
        String createIndexSQL5 = "CREATE INDEX IF NOT EXISTS idx_votos_sincronizado ON votos_regionales(sincronizado)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createTableSQL);
            
            // Intentar añadir columnas de sincronización (ignorar errores si ya existen)
            try { stmt.execute(addSincronizadoSQL); } catch (SQLException e) { /* Columna ya existe */ }
            try { stmt.execute(addFechaSincronizacionSQL); } catch (SQLException e) { /* Columna ya existe */ }
            try { stmt.execute(addIntentosSincronizacionSQL); } catch (SQLException e) { /* Columna ya existe */ }
            
            stmt.execute(createIndexSQL1);
            stmt.execute(createIndexSQL2);
            stmt.execute(createIndexSQL3);
            stmt.execute(createIndexSQL4);
            stmt.execute(createIndexSQL5);
            
            System.out.println("✅ Base de datos de votos regionales inicializada: " + DB_PATH);
            
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando base de datos de votos: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Guarda un voto en la base de datos
     */
    public boolean guardarVoto(VotoRegional voto) {
        String insertSQL = "INSERT OR REPLACE INTO votos_regionales " +
            "(id_voto, mesa_id, timestamp, candidato_id, hash_elector, municipio, departamento, estado_registro, fecha_recepcion) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        String fechaRecepcion = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            pstmt.setLong(1, voto.idVoto);
            pstmt.setString(2, voto.mesaId);
            pstmt.setLong(3, voto.timestamp);
            pstmt.setLong(4, voto.candidatoId);
            pstmt.setString(5, voto.hashElector);
            pstmt.setString(6, voto.municipio);
            pstmt.setString(7, voto.departamento);
            pstmt.setString(8, voto.estadoRegistro != null ? voto.estadoRegistro : "NUEVO");
            pstmt.setString(9, fechaRecepcion);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Voto guardado: ID=" + voto.idVoto + ", Mesa=" + voto.mesaId + ", Candidato=" + voto.candidatoId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando voto " + voto.idVoto + ": " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Guarda múltiples votos en una transacción
     */
    public ResultadoRecepcionVotos guardarVotosLote(VotoRegional[] votos) {
        long inicioTiempo = System.currentTimeMillis();
        int votosGuardados = 0;
        int votosRechazados = 0;
        List<String> errores = new ArrayList<>();
        
        String insertSQL = "INSERT OR REPLACE INTO votos_regionales " +
            "(id_voto, mesa_id, timestamp, candidato_id, hash_elector, municipio, departamento, estado_registro, fecha_recepcion) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        String fechaRecepcion = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false); // Iniciar transacción
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                
                for (VotoRegional voto : votos) {
                    try {
                        // Validar voto antes de guardarlo
                        if (!validarVoto(voto)) {
                            votosRechazados++;
                            errores.add("Voto inválido ID: " + voto.idVoto);
                            continue;
                        }
                        
                        pstmt.setLong(1, voto.idVoto);
                        pstmt.setString(2, voto.mesaId);
                        pstmt.setLong(3, voto.timestamp);
                        pstmt.setLong(4, voto.candidatoId);
                        pstmt.setString(5, voto.hashElector);
                        pstmt.setString(6, voto.municipio);
                        pstmt.setString(7, voto.departamento);
                        pstmt.setString(8, voto.estadoRegistro != null ? voto.estadoRegistro : "NUEVO");
                        pstmt.setString(9, fechaRecepcion);
                        
                        pstmt.addBatch();
                        votosGuardados++;
                        
                    } catch (Exception e) {
                        votosRechazados++;
                        errores.add("Error voto ID " + voto.idVoto + ": " + e.getMessage());
                    }
                }
                
                // Ejecutar el lote
                pstmt.executeBatch();
                conn.commit();
                
                long tiempoProcessamiento = System.currentTimeMillis() - inicioTiempo;
                
                System.out.println("✅ Lote de votos procesado:");
                System.out.println("   Total recibidos: " + votos.length);
                System.out.println("   Votos guardados: " + votosGuardados);
                System.out.println("   Votos rechazados: " + votosRechazados);
                System.out.println("   Tiempo: " + tiempoProcessamiento + "ms");
                
                // Crear resultado
                ResultadoRecepcionVotos resultado = new ResultadoRecepcionVotos();
                resultado.exito = true;
                resultado.totalRecibidos = votos.length;
                resultado.votosGuardados = votosGuardados;
                resultado.votosRechazados = votosRechazados;
                resultado.mensaje = "Lote procesado exitosamente";
                resultado.tiempoProcessamiento = tiempoProcessamiento;
                resultado.errores = errores.toArray(new String[0]);
                
                return resultado;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error procesando lote de votos: " + e.getMessage());
            
            long tiempoProcessamiento = System.currentTimeMillis() - inicioTiempo;
            ResultadoRecepcionVotos resultado = new ResultadoRecepcionVotos();
            resultado.exito = false;
            resultado.totalRecibidos = votos.length;
            resultado.votosGuardados = 0;
            resultado.votosRechazados = votos.length;
            resultado.mensaje = "Error procesando lote: " + e.getMessage();
            resultado.tiempoProcessamiento = tiempoProcessamiento;
            resultado.errores = new String[]{"Error de base de datos: " + e.getMessage()};
            
            return resultado;
        }
    }
    
    /**
     * Valida un voto antes de guardarlo
     */
    private boolean validarVoto(VotoRegional voto) {
        if (voto.idVoto <= 0) {
            return false;
        }
        if (voto.mesaId == null || voto.mesaId.trim().isEmpty()) {
            return false;
        }
        if (voto.candidatoId <= 0) {
            return false;
        }
        if (voto.hashElector == null || voto.hashElector.trim().isEmpty()) {
            return false;
        }
        if (voto.timestamp <= 0) {
            return false;
        }
        return true;
    }
    
    /**
     * Verifica si un voto ya existe por ID
     */
    public boolean existeVoto(long idVoto) {
        String selectSQL = "SELECT COUNT(*) FROM votos_regionales WHERE id_voto = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setLong(1, idVoto);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error verificando existencia de voto: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Verifica si ya existe un voto con el mismo hash y mesa
     */
    public boolean existeVotoPorHash(String hashElector, String mesaId) {
        String selectSQL = "SELECT COUNT(*) FROM votos_regionales WHERE hash_elector = ? AND mesa_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setString(1, hashElector);
            pstmt.setString(2, mesaId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error verificando voto por hash: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Cuenta el total de votos almacenados
     */
    public long contarVotosAlmacenados() {
        String countSQL = "SELECT COUNT(*) FROM votos_regionales";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(countSQL)) {
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error contando votos: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Cuenta votos por mesa
     */
    public long contarVotosPorMesa(String mesaId) {
        String countSQL = "SELECT COUNT(*) FROM votos_regionales WHERE mesa_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(countSQL)) {
            
            pstmt.setString(1, mesaId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error contando votos por mesa: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Cuenta votos por candidato
     */
    public long contarVotosPorCandidato(long candidatoId) {
        String countSQL = "SELECT COUNT(*) FROM votos_regionales WHERE candidato_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(countSQL)) {
            
            pstmt.setLong(1, candidatoId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error contando votos por candidato: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Obtiene votos por mesa
     */
    public List<VotoRegional> obtenerVotosPorMesa(String mesaId) {
        List<VotoRegional> votos = new ArrayList<>();
        String selectSQL = "SELECT id_voto, mesa_id, timestamp, candidato_id, hash_elector, municipio, departamento, estado_registro " +
            "FROM votos_regionales " +
            "WHERE mesa_id = ? " +
            "ORDER BY timestamp DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setString(1, mesaId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                VotoRegional voto = new VotoRegional();
                voto.idVoto = rs.getLong("id_voto");
                voto.mesaId = rs.getString("mesa_id");
                voto.timestamp = rs.getLong("timestamp");
                voto.candidatoId = rs.getLong("candidato_id");
                voto.hashElector = rs.getString("hash_elector");
                voto.municipio = rs.getString("municipio");
                voto.departamento = rs.getString("departamento");
                voto.estadoRegistro = rs.getString("estado_registro");
                
                votos.add(voto);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo votos por mesa: " + e.getMessage());
        }
        
        return votos;
    }
    
    /**
     * Obtiene votos por candidato
     */
    public List<VotoRegional> obtenerVotosPorCandidato(long candidatoId) {
        List<VotoRegional> votos = new ArrayList<>();
        String selectSQL = "SELECT id_voto, mesa_id, timestamp, candidato_id, hash_elector, municipio, departamento, estado_registro " +
            "FROM votos_regionales " +
            "WHERE candidato_id = ? " +
            "ORDER BY timestamp DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setLong(1, candidatoId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                VotoRegional voto = new VotoRegional();
                voto.idVoto = rs.getLong("id_voto");
                voto.mesaId = rs.getString("mesa_id");
                voto.timestamp = rs.getLong("timestamp");
                voto.candidatoId = rs.getLong("candidato_id");
                voto.hashElector = rs.getString("hash_elector");
                voto.municipio = rs.getString("municipio");
                voto.departamento = rs.getString("departamento");
                voto.estadoRegistro = rs.getString("estado_registro");
                
                votos.add(voto);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo votos por candidato: " + e.getMessage());
        }
        
        return votos;
    }
    
    /**
     * Limpia todos los votos (para testing)
     */
    public boolean limpiarVotos() {
        String deleteSQL = "DELETE FROM votos_regionales";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            
            int rowsDeleted = pstmt.executeUpdate();
            System.out.println("🧹 Eliminados " + rowsDeleted + " votos de la base de datos");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error limpiando votos: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Limpia votos de una mesa específica
     */
    public boolean limpiarVotosMesa(String mesaId) {
        String deleteSQL = "DELETE FROM votos_regionales WHERE mesa_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            
            pstmt.setString(1, mesaId);
            int rowsDeleted = pstmt.executeUpdate();
            System.out.println("🧹 Eliminados " + rowsDeleted + " votos de la mesa " + mesaId);
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error limpiando votos de mesa: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene estadísticas detalladas
     */
    public String obtenerEstadisticasDetalladas() {
        StringBuilder stats = new StringBuilder();
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            
            // Estadísticas generales
            String generalSQL = "SELECT " +
                "COUNT(*) as total_votos," +
                "COUNT(DISTINCT mesa_id) as total_mesas," +
                "COUNT(DISTINCT candidato_id) as total_candidatos," +
                "MIN(timestamp) as primer_voto," +
                "MAX(timestamp) as ultimo_voto " +
                "FROM votos_regionales";
            
            try (PreparedStatement pstmt = conn.prepareStatement(generalSQL)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    stats.append("📊 ESTADÍSTICAS GENERALES:\n");
                    stats.append("   Total votos: ").append(rs.getLong("total_votos")).append("\n");
                    stats.append("   Total mesas: ").append(rs.getInt("total_mesas")).append("\n");
                    stats.append("   Total candidatos: ").append(rs.getInt("total_candidatos")).append("\n");
                    
                    long primerVoto = rs.getLong("primer_voto");
                    long ultimoVoto = rs.getLong("ultimo_voto");
                    if (primerVoto > 0) {
                        stats.append("   Primer voto: ").append(new java.util.Date(primerVoto)).append("\n");
                        stats.append("   Último voto: ").append(new java.util.Date(ultimoVoto)).append("\n");
                    }
                }
            }
            
            // Votos por mesa
            String mesaSQL = "SELECT mesa_id, COUNT(*) as votos FROM votos_regionales GROUP BY mesa_id ORDER BY votos DESC LIMIT 10";
            try (PreparedStatement pstmt = conn.prepareStatement(mesaSQL)) {
                ResultSet rs = pstmt.executeQuery();
                stats.append("\n🗳️ TOP 10 MESAS:\n");
                while (rs.next()) {
                    stats.append("   Mesa ").append(rs.getString("mesa_id"))
                         .append(": ").append(rs.getInt("votos")).append(" votos\n");
                }
            }
            
            // Votos por candidato
            String candidatoSQL = "SELECT candidato_id, COUNT(*) as votos FROM votos_regionales GROUP BY candidato_id ORDER BY votos DESC";
            try (PreparedStatement pstmt = conn.prepareStatement(candidatoSQL)) {
                ResultSet rs = pstmt.executeQuery();
                stats.append("\n🏆 VOTOS POR CANDIDATO:\n");
                while (rs.next()) {
                    stats.append("   Candidato ").append(rs.getLong("candidato_id"))
                         .append(": ").append(rs.getInt("votos")).append(" votos\n");
                }
            }
            
        } catch (SQLException e) {
            stats.append("❌ Error obteniendo estadísticas: ").append(e.getMessage());
        }
        
        return stats.toString();
    }
    
    /**
     * Verifica la conectividad del servicio
     */
    public boolean verificarServicio() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            return conn != null;
        } catch (SQLException e) {
            System.err.println("❌ Error verificando servicio: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene la ruta de la base de datos
     */
    public String getDbPath() {
        return DB_PATH;
    }
    
    // ========== MÉTODOS DE SINCRONIZACIÓN ==========
    
    /**
     * Obtiene los votos que no han sido sincronizados con el servidor nacional
     */
    public List<VotoRegional> obtenerVotosNoSincronizados(int limite) {
        List<VotoRegional> votos = new ArrayList<>();
        String selectSQL = "SELECT id_voto, mesa_id, timestamp, candidato_id, hash_elector, municipio, departamento, estado_registro " +
            "FROM votos_regionales " +
            "WHERE (sincronizado = FALSE OR sincronizado IS NULL) " +
            "AND intentos_sincronizacion < 3 " +
            "ORDER BY timestamp ASC " +
            "LIMIT ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            
            pstmt.setInt(1, limite);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                VotoRegional voto = new VotoRegional();
                voto.idVoto = rs.getLong("id_voto");
                voto.mesaId = rs.getString("mesa_id");
                voto.timestamp = rs.getLong("timestamp");
                voto.candidatoId = rs.getLong("candidato_id");
                voto.hashElector = rs.getString("hash_elector");
                voto.municipio = rs.getString("municipio");
                voto.departamento = rs.getString("departamento");
                voto.estadoRegistro = rs.getString("estado_registro");
                
                votos.add(voto);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo votos no sincronizados: " + e.getMessage());
        }
        
        return votos;
    }
    
    /**
     * Marca votos como sincronizados exitosamente
     */
    public boolean marcarVotosSincronizados(List<Long> idsVotos) {
        if (idsVotos.isEmpty()) {
            return true;
        }
        
        String updateSQL = "UPDATE votos_regionales SET sincronizado = TRUE, fecha_sincronizacion = ? WHERE id_voto = ?";
        String fechaSincronizacion = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            conn.setAutoCommit(false);
            
            for (Long idVoto : idsVotos) {
                pstmt.setString(1, fechaSincronizacion);
                pstmt.setLong(2, idVoto);
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            conn.commit();
            
            System.out.println("✅ Marcados " + idsVotos.size() + " votos como sincronizados");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error marcando votos como sincronizados: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Incrementa el contador de intentos de sincronización para votos fallidos
     */
    public boolean incrementarIntentosSincronizacion(List<Long> idsVotos) {
        if (idsVotos.isEmpty()) {
            return true;
        }
        
        String updateSQL = "UPDATE votos_regionales SET intentos_sincronizacion = intentos_sincronizacion + 1 WHERE id_voto = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            conn.setAutoCommit(false);
            
            for (Long idVoto : idsVotos) {
                pstmt.setLong(1, idVoto);
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            conn.commit();
            
            System.out.println("⚠️ Incrementados intentos de sincronización para " + idsVotos.size() + " votos");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error incrementando intentos de sincronización: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene estadísticas de sincronización
     */
    public String obtenerEstadisticasSincronizacion() {
        StringBuilder stats = new StringBuilder();
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            
            // Estadísticas de sincronización
            String sincronizacionSQL = "SELECT " +
                "COUNT(*) as total_votos," +
                "SUM(CASE WHEN sincronizado = TRUE THEN 1 ELSE 0 END) as votos_sincronizados," +
                "SUM(CASE WHEN (sincronizado = FALSE OR sincronizado IS NULL) THEN 1 ELSE 0 END) as votos_pendientes," +
                "SUM(CASE WHEN intentos_sincronizacion >= 3 THEN 1 ELSE 0 END) as votos_fallidos " +
                "FROM votos_regionales";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sincronizacionSQL)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    stats.append("🔄 ESTADÍSTICAS DE SINCRONIZACIÓN:\n");
                    stats.append("   Total votos: ").append(rs.getLong("total_votos")).append("\n");
                    stats.append("   Votos sincronizados: ").append(rs.getInt("votos_sincronizados")).append("\n");
                    stats.append("   Votos pendientes: ").append(rs.getInt("votos_pendientes")).append("\n");
                    stats.append("   Votos fallidos: ").append(rs.getInt("votos_fallidos")).append("\n");
                }
            }
            
        } catch (SQLException e) {
            stats.append("❌ Error obteniendo estadísticas de sincronización: ").append(e.getMessage());
        }
        
        return stats.toString();
    }
    
    /**
     * Actualiza los votos que tienen municipio y departamento vacíos
     * con información por defecto basada en el servidor regional
     */
    public int actualizarInformacionGeograficaVotos() {
        String updateSQL = "UPDATE votos_regionales " +
            "SET municipio = CASE " +
            "    WHEN municipio IS NULL OR municipio = '' THEN 'REGIONAL_' || SUBSTR(mesa_id, 1, 3) " +
            "    ELSE municipio " +
            "END, " +
            "departamento = CASE " +
            "    WHEN departamento IS NULL OR departamento = '' THEN 'CUNDINAMARCA' " +
            "    ELSE departamento " +
            "END " +
            "WHERE (municipio IS NULL OR municipio = '') OR (departamento IS NULL OR departamento = '')";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            int filasActualizadas = pstmt.executeUpdate();
            
            if (filasActualizadas > 0) {
                System.out.println("✅ Actualizada información geográfica de " + filasActualizadas + " votos");
            }
            
            return filasActualizadas;
            
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando información geográfica: " + e.getMessage());
            return 0;
        }
    }
} 