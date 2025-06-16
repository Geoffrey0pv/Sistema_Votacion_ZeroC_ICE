import java.sql.*;

public class SimpleVoteCount {
    
    private static final String DB_URL = "jdbc:postgresql://10.147.17.110:5432/votos_elecciones_grajj";
    private static final String DB_USER = "votaciones_grajj";
    private static final String DB_PASSWORD = "votaciones_grajj";
    
    public static void main(String[] args) {
        System.out.println("🔍 === CONTEO SIMPLE DE VOTOS ===");
        
        try {
            Class.forName("org.postgresql.Driver");
            
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                System.out.println("✅ Conectado a la base de datos");
                
                // Contar todos los votos
                String countSQL = "SELECT COUNT(*) as total FROM votos";
                try (PreparedStatement stmt = conn.prepareStatement(countSQL);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt("total");
                        System.out.println("📊 Total votos en BD: " + total);
                    }
                }
                
                // Mostrar últimos 10 votos
                String recentSQL = "SELECT mesa_id, candidato_id, municipio, fecha_recepcion " +
                                  "FROM votos ORDER BY fecha_recepcion DESC LIMIT 10";
                try (PreparedStatement stmt = conn.prepareStatement(recentSQL);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    System.out.println("\n📋 Últimos 10 votos:");
                    int i = 1;
                    while (rs.next()) {
                        System.out.printf("   %d. Mesa: %s, Candidato: %s, Municipio: %s, Fecha: %s%n",
                            i++, rs.getString("mesa_id"), rs.getString("candidato_id"), 
                            rs.getString("municipio"), rs.getTimestamp("fecha_recepcion"));
                    }
                }
                
                // Contar por mesa
                String mesaSQL = "SELECT mesa_id, COUNT(*) as votos FROM votos GROUP BY mesa_id ORDER BY votos DESC";
                try (PreparedStatement stmt = conn.prepareStatement(mesaSQL);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    System.out.println("\n📊 Votos por mesa:");
                    while (rs.next()) {
                        System.out.printf("   Mesa %s: %d votos%n",
                            rs.getString("mesa_id"), rs.getInt("votos"));
                    }
                }
                
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n================================");
    }
} 