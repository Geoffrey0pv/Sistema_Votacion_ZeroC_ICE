package Services;

import Models.VotoModel;
import Database.DatabaseManager;
import Database.DatabaseConnection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSyntaxException;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar votos con reliable messaging
 * Utiliza la base de datos de registraduría
 */
public class VotosService implements ReliableMessageQueue.VotoProcessor {
    
    private final DatabaseManager dbManager;
    private final DatabaseConnection dbConnection;
    private final Gson gson;
    private final ReliableMessageQueue messageQueue;
    
    public VotosService() {
        this.dbManager = DatabaseManager.getInstance();
        this.dbConnection = dbManager.getRegistraduriaConnection();
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> 
                LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_DATE_TIME))
            .create();
        
        // Inicializar cola confiable
        this.messageQueue = new ReliableMessageQueue();
        this.messageQueue.setVotoProcessor(this);
        
        // Crear tabla si no existe
        createVotosTableIfNotExists();
        
        System.out.println("✅ VotosService inicializado con reliable messaging");
        System.out.println("   📊 Base de datos: registraduría");
    }
    
    private void createVotosTableIfNotExists() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS votos (" +
            "id BIGSERIAL PRIMARY KEY," +
            "mesa_id VARCHAR(50) NOT NULL," +
            "candidato_id VARCHAR(50) NOT NULL," +
            "timestamp TIMESTAMP NOT NULL," +
            "municipio VARCHAR(100) NOT NULL," +
            "departamento VARCHAR(100) NOT NULL," +
            "hash_verificacion VARCHAR(255) NOT NULL," +
            "firma_mesa TEXT NOT NULL," +
            "fecha_recepcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "estado VARCHAR(20) DEFAULT 'PROCESADO'," +
            "UNIQUE(mesa_id, candidato_id, timestamp, hash_verificacion)" +
            ")";
        
        String createIndexSQL1 = "CREATE INDEX IF NOT EXISTS idx_votos_mesa ON votos(mesa_id)";
        String createIndexSQL2 = "CREATE INDEX IF NOT EXISTS idx_votos_candidato ON votos(candidato_id)";
        String createIndexSQL3 = "CREATE INDEX IF NOT EXISTS idx_votos_municipio ON votos(municipio)";
        String createIndexSQL4 = "CREATE INDEX IF NOT EXISTS idx_votos_timestamp ON votos(timestamp)";
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn != null) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableSQL);
                    stmt.execute(createIndexSQL1);
                    stmt.execute(createIndexSQL2);
                    stmt.execute(createIndexSQL3);
                    stmt.execute(createIndexSQL4);
                    System.out.println("✅ Tabla 'votos' verificada/creada con índices");
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ No se pudo crear tabla votos: " + e.getMessage());
        }
    }
    
    /**
     * Recibe un paquete de votos en formato JSON y los procesa
     */
    public ProcessingResult receiveVotesPackage(String jsonPackage) {
        if (jsonPackage == null || jsonPackage.trim().isEmpty()) {
            return new ProcessingResult(false, 0, 0, "JSON vacío");
        }
        
        try {
            // Parsear JSON
            VotoModel[] votosArray = gson.fromJson(jsonPackage, VotoModel[].class);
            List<VotoModel> votos = new ArrayList<>();
            
            // Validar y convertir
            for (VotoModel voto : votosArray) {
                if (voto != null && voto.isValid()) {
                    votos.add(voto);
                } else {
                    System.err.println("⚠️ Voto inválido ignorado: " + (voto != null ? voto.toString() : "null"));
                }
            }
            
            if (votos.isEmpty()) {
                return new ProcessingResult(false, 0, 0, "No hay votos válidos en el paquete");
            }
            
            // Encolar votos para procesamiento confiable
            int enqueued = messageQueue.enqueueBatch(votos);
            
            System.out.println("📦 Paquete recibido: " + enqueued + "/" + votos.size() + " votos encolados");
            
            return new ProcessingResult(true, votos.size(), enqueued, 
                "Paquete recibido y encolado para procesamiento");
            
        } catch (JsonSyntaxException e) {
            System.err.println("❌ Error parseando JSON: " + e.getMessage());
            return new ProcessingResult(false, 0, 0, "Error en formato JSON: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error procesando paquete: " + e.getMessage());
            return new ProcessingResult(false, 0, 0, "Error interno: " + e.getMessage());
        }
    }
    
    /**
     * Implementación de VotoProcessor - procesa un voto individual
     */
    @Override
    public boolean processVoto(VotoModel voto) {
        if (voto == null || !voto.isValid()) {
            return false;
        }
        
        String insertSQL = "INSERT INTO votos (mesa_id, candidato_id, timestamp, municipio, departamento, " +
                          "hash_verificacion, firma_mesa, fecha_recepcion, estado) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                          "ON CONFLICT (mesa_id, candidato_id, timestamp, hash_verificacion) " +
                          "DO NOTHING";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            
            if (conn == null) {
                return false;
            }
            
            stmt.setString(1, voto.getMesaId());
            stmt.setString(2, voto.getCandidatoId());
            stmt.setTimestamp(3, Timestamp.valueOf(voto.getTimestamp()));
            stmt.setString(4, voto.getMunicipio());
            stmt.setString(5, voto.getDepartamento());
            stmt.setString(6, voto.getHashVerificacion());
            stmt.setTimestamp(7, Timestamp.valueOf(voto.getFechaRecepcion()));
            stmt.setString(8, "PROCESADO");
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Voto guardado: " + voto.getMesaId() + " -> " + voto.getCandidatoId());
                return true;
            } else {
                System.out.println("ℹ️ Voto duplicado ignorado: " + voto.getMesaId() + " -> " + voto.getCandidatoId());
                return true; // Consideramos éxito porque el voto ya existe
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando voto: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Implementación de VotoProcessor - verifica disponibilidad de BD
     */
    @Override
    public boolean isDatabaseAvailable() {
        return dbConnection.isServiceActive();
    }
    
    /**
     * Obtiene estadísticas de votos
     */
    public VotingStats getVotingStats() {
        String statsSQL = "SELECT " +
                         "COUNT(*) as total_votos, " +
                         "COUNT(DISTINCT mesa_id) as total_mesas, " +
                         "COUNT(DISTINCT candidato_id) as total_candidatos, " +
                         "COUNT(DISTINCT municipio) as total_municipios, " +
                         "MIN(timestamp) as primer_voto, " +
                         "MAX(timestamp) as ultimo_voto " +
                         "FROM votos";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(statsSQL);
             ResultSet rs = stmt.executeQuery()) {
            
            if (conn == null) {
                return new VotingStats(0, 0, 0, 0, null, null);
            }
            
            if (rs.next()) {
                return new VotingStats(
                    rs.getLong("total_votos"),
                    rs.getInt("total_mesas"),
                    rs.getInt("total_candidatos"),
                    rs.getInt("total_municipios"),
                    rs.getTimestamp("primer_voto"),
                    rs.getTimestamp("ultimo_voto")
                );
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo estadísticas: " + e.getMessage());
        }
        
        return new VotingStats(0, 0, 0, 0, null, null);
    }
    
    /**
     * Obtiene votos por candidato
     */
    public List<CandidateVoteCount> getVotesByCandidato() {
        String sql = "SELECT candidato_id, COUNT(*) as votos " +
                    "FROM votos " +
                    "GROUP BY candidato_id " +
                    "ORDER BY votos DESC";
        
        List<CandidateVoteCount> results = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (conn == null) {
                return results;
            }
            
            while (rs.next()) {
                results.add(new CandidateVoteCount(
                    rs.getString("candidato_id"),
                    rs.getLong("votos")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo votos por candidato: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Obtiene estadísticas de la cola confiable
     */
    public ReliableMessageQueue.QueueStats getQueueStats() {
        return messageQueue.getStats();
    }
    
    /**
     * Verifica si el servicio está disponible
     */
    public boolean isServiceAvailable() {
        return dbConnection.isServiceActive();
    }
    
    /**
     * Método de prueba para verificar la conexión y mostrar información de la BD
     */
    public void testDatabaseConnection() {
        System.out.println("🧪 === TEST CONEXIÓN VOTOS SERVICE ===");
        try (Connection conn = dbConnection.getConnection()) {
            if (conn != null) {
                String url = conn.getMetaData().getURL();
                String user = conn.getMetaData().getUserName();
                System.out.println("✅ Conexión exitosa a BD de VOTOS:");
                System.out.println("   📍 URL: " + url);
                System.out.println("   👤 Usuario: " + user);
                System.out.println("   📊 Connection Info: " + dbConnection.getConnectionInfo());
            } else {
                System.err.println("❌ No se pudo conectar a la BD de votos");
            }
        } catch (Exception e) {
            System.err.println("❌ Error en test de conexión: " + e.getMessage());
        }
        System.out.println("=====================================");
    }
    
    /**
     * Cierra el servicio
     */
    public void shutdown() {
        System.out.println("🛑 Cerrando VotosService...");
        messageQueue.shutdown();
        System.out.println("✅ VotosService cerrado");
    }
    
    // Clases de resultado
    public static class ProcessingResult {
        public final boolean success;
        public final int totalVotes;
        public final int enqueuedVotes;
        public final String message;
        
        public ProcessingResult(boolean success, int totalVotes, int enqueuedVotes, String message) {
            this.success = success;
            this.totalVotes = totalVotes;
            this.enqueuedVotes = enqueuedVotes;
            this.message = message;
        }
        
        @Override
        public String toString() {
            return String.format("ProcessingResult{success=%s, total=%d, enqueued=%d, message='%s'}", 
                success, totalVotes, enqueuedVotes, message);
        }
    }
    
    public static class VotingStats {
        public final long totalVotos;
        public final int totalMesas;
        public final int totalCandidatos;
        public final int totalMunicipios;
        public final Timestamp primerVoto;
        public final Timestamp ultimoVoto;
        
        public VotingStats(long totalVotos, int totalMesas, int totalCandidatos, 
                          int totalMunicipios, Timestamp primerVoto, Timestamp ultimoVoto) {
            this.totalVotos = totalVotos;
            this.totalMesas = totalMesas;
            this.totalCandidatos = totalCandidatos;
            this.totalMunicipios = totalMunicipios;
            this.primerVoto = primerVoto;
            this.ultimoVoto = ultimoVoto;
        }
        
        @Override
        public String toString() {
            return String.format("VotingStats{votos=%d, mesas=%d, candidatos=%d, municipios=%d}", 
                totalVotos, totalMesas, totalCandidatos, totalMunicipios);
        }
    }
    
    public static class CandidateVoteCount {
        public final String candidatoId;
        public final long votos;
        
        public CandidateVoteCount(String candidatoId, long votos) {
            this.candidatoId = candidatoId;
            this.votos = votos;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %d votos", candidatoId, votos);
        }
    }
} 