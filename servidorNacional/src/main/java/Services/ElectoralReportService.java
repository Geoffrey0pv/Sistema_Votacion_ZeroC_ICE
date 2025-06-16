package Services;

import Models.VotoModel;
import Database.DatabaseManager;
import Database.VotosDatabaseConnection;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio para generar reportes electorales y cerrar jornada
 */
public class ElectoralReportService {
    
    private final DatabaseManager dbManager;
    private final VotosDatabaseConnection votosConnection;
    private boolean jornadaCerrada = false;
    private LocalDateTime fechaCierre = null;
    
    public ElectoralReportService() {
        this.dbManager = DatabaseManager.getInstance();
        this.votosConnection = dbManager.getVotosConnection();
        
        System.out.println("✅ ElectoralReportService inicializado");
    }
    
    /**
     * Cierra la jornada electoral
     */
    public boolean cerrarJornada() {
        if (jornadaCerrada) {
            System.out.println("⚠️ La jornada electoral ya está cerrada");
            return false;
        }
        
        try {
            fechaCierre = LocalDateTime.now();
            jornadaCerrada = true;
            
            System.out.println("🔒 JORNADA ELECTORAL CERRADA");
            System.out.println("   📅 Fecha de cierre: " + fechaCierre.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error cerrando jornada: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Genera todos los reportes CSV
     */
    public ReportResult generateAllReports() {
        if (!jornadaCerrada) {
            return new ReportResult(false, "La jornada electoral debe estar cerrada para generar reportes");
        }
        
        try {
            // Crear directorio de reportes
            String reportDir = "reportes_electorales_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Files.createDirectories(Paths.get(reportDir));
            
            System.out.println("📊 Generando reportes electorales...");
            System.out.println("   📁 Directorio: " + reportDir);
            
            // Generar reporte general
            boolean resumeOk = generateResumeReport(reportDir);
            
            // Generar reportes por mesa
            List<String> mesasReports = generateMesaReports(reportDir);
            
            ReportResult result = new ReportResult(
                resumeOk && !mesasReports.isEmpty(),
                resumeOk ? "Reportes generados exitosamente" : "Error generando reportes",
                reportDir,
                mesasReports.size()
            );
            
            if (result.success) {
                System.out.println("✅ Reportes generados exitosamente:");
                System.out.println("   📄 Reporte general: " + reportDir + "/resume.csv");
                System.out.println("   📄 Reportes por mesa: " + mesasReports.size() + " archivos");
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error generando reportes: " + e.getMessage());
            return new ReportResult(false, "Error interno: " + e.getMessage());
        }
    }
    
    /**
     * Genera el reporte general resume.csv
     */
    private boolean generateResumeReport(String reportDir) {
        String sql = "SELECT " +
                    "v.candidato_id, " +
                    "v.candidato_id as candidato_nombre, " +
                    "COUNT(*) as total_votos " +
                    "FROM votos v " +
                    "GROUP BY v.candidato_id " +
                    "ORDER BY total_votos DESC";
        
        try (Connection conn = votosConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (conn == null) {
                System.err.println("❌ No se pudo conectar a la BD de votos");
                return false;
            }
            
            Path filePath = Paths.get(reportDir, "resume.csv");
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
                
                // Escribir encabezado
                writer.println("candidateId,candidateName,totalVotes");
                
                int totalCandidatos = 0;
                long totalVotos = 0;
                
                while (rs.next()) {
                    String candidatoId = rs.getString("candidato_id");
                    String candidatoNombre = "Candidato " + candidatoId; // Nombre genérico
                    long votos = rs.getLong("total_votos");
                    
                    writer.printf("%s,\"%s\",%d%n", candidatoId, candidatoNombre, votos);
                    
                    totalCandidatos++;
                    totalVotos += votos;
                }
                
                System.out.println("📄 Reporte general generado:");
                System.out.println("   👥 Candidatos: " + totalCandidatos);
                System.out.println("   🗳️ Total votos: " + totalVotos);
                
                return true;
            }
            
        } catch (SQLException | IOException e) {
            System.err.println("❌ Error generando reporte general: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Genera reportes por mesa partial-{mesaId}.csv
     */
    private List<String> generateMesaReports(String reportDir) {
        List<String> generatedFiles = new ArrayList<>();
        
        // Obtener todas las mesas
        String mesasSQL = "SELECT DISTINCT mesa_id FROM votos ORDER BY mesa_id";
        
        try (Connection conn = votosConnection.getConnection();
             PreparedStatement mesasStmt = conn.prepareStatement(mesasSQL);
             ResultSet mesasRs = mesasStmt.executeQuery()) {
            
            if (conn == null) {
                System.err.println("❌ No se pudo conectar a la BD de votos");
                return generatedFiles;
            }
            
            while (mesasRs.next()) {
                String mesaId = mesasRs.getString("mesa_id");
                
                if (generateSingleMesaReport(conn, reportDir, mesaId)) {
                    generatedFiles.add("partial-" + mesaId + ".csv");
                }
            }
            
            System.out.println("📄 Reportes por mesa generados: " + generatedFiles.size());
            
        } catch (SQLException e) {
            System.err.println("❌ Error generando reportes por mesa: " + e.getMessage());
        }
        
        return generatedFiles;
    }
    
    /**
     * Genera reporte para una mesa específica
     */
    private boolean generateSingleMesaReport(Connection conn, String reportDir, String mesaId) {
        String sql = "SELECT " +
                    "v.candidato_id, " +
                    "v.candidato_id as candidato_nombre, " +
                    "COUNT(*) as total_votos " +
                    "FROM votos v " +
                    "WHERE v.mesa_id = ? " +
                    "GROUP BY v.candidato_id " +
                    "ORDER BY total_votos DESC";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mesaId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                Path filePath = Paths.get(reportDir, "partial-" + mesaId + ".csv");
                
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
                    
                    // Escribir encabezado
                    writer.println("candidateId,candidateName,totalVotes");
                    
                    int votosEnMesa = 0;
                    
                    while (rs.next()) {
                        String candidatoId = rs.getString("candidato_id");
                        String candidatoNombre = "Candidato " + candidatoId; // Nombre genérico
                        long votos = rs.getLong("total_votos");
                        
                        writer.printf("%s,\"%s\",%d%n", candidatoId, candidatoNombre, votos);
                        votosEnMesa += votos;
                    }
                    
                    if (votosEnMesa > 0) {
                        System.out.println("   📊 Mesa " + mesaId + ": " + votosEnMesa + " votos");
                        return true;
                    }
                }
            }
            
        } catch (SQLException | IOException e) {
            System.err.println("❌ Error generando reporte para mesa " + mesaId + ": " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Obtiene estadísticas de la jornada
     */
    public JornadaStats getJornadaStats() {
        String sql = "SELECT " +
                    "COUNT(*) as total_votos, " +
                    "COUNT(DISTINCT mesa_id) as total_mesas, " +
                    "COUNT(DISTINCT candidato_id) as total_candidatos, " +
                    "MIN(timestamp) as primer_voto, " +
                    "MAX(timestamp) as ultimo_voto " +
                    "FROM votos";
        
        try (Connection conn = votosConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (conn != null && rs.next()) {
                return new JornadaStats(
                    rs.getLong("total_votos"),
                    rs.getInt("total_mesas"),
                    rs.getInt("total_candidatos"),
                    rs.getTimestamp("primer_voto"),
                    rs.getTimestamp("ultimo_voto"),
                    jornadaCerrada,
                    fechaCierre
                );
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo estadísticas: " + e.getMessage());
        }
        
        return new JornadaStats(0, 0, 0, null, null, jornadaCerrada, fechaCierre);
    }
    
    public boolean isJornadaCerrada() {
        return jornadaCerrada;
    }
    
    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }
    
    // Clases de resultado
    public static class ReportResult {
        public final boolean success;
        public final String message;
        public final String reportDirectory;
        public final int filesGenerated;
        
        public ReportResult(boolean success, String message) {
            this(success, message, null, 0);
        }
        
        public ReportResult(boolean success, String message, String reportDirectory, int filesGenerated) {
            this.success = success;
            this.message = message;
            this.reportDirectory = reportDirectory;
            this.filesGenerated = filesGenerated;
        }
        
        @Override
        public String toString() {
            return String.format("ReportResult{success=%s, message='%s', directory='%s', files=%d}", 
                success, message, reportDirectory, filesGenerated);
        }
    }
    
    public static class JornadaStats {
        public final long totalVotos;
        public final int totalMesas;
        public final int totalCandidatos;
        public final Timestamp primerVoto;
        public final Timestamp ultimoVoto;
        public final boolean jornadaCerrada;
        public final LocalDateTime fechaCierre;
        
        public JornadaStats(long totalVotos, int totalMesas, int totalCandidatos, 
                           Timestamp primerVoto, Timestamp ultimoVoto, 
                           boolean jornadaCerrada, LocalDateTime fechaCierre) {
            this.totalVotos = totalVotos;
            this.totalMesas = totalMesas;
            this.totalCandidatos = totalCandidatos;
            this.primerVoto = primerVoto;
            this.ultimoVoto = ultimoVoto;
            this.jornadaCerrada = jornadaCerrada;
            this.fechaCierre = fechaCierre;
        }
        
        @Override
        public String toString() {
            return String.format("JornadaStats{votos=%d, mesas=%d, candidatos=%d, cerrada=%s}", 
                totalVotos, totalMesas, totalCandidatos, jornadaCerrada);
        }
    }
} 