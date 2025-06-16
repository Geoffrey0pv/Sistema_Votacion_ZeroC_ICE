import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestDatabaseConnection {
    
    private static final String DB_URL = "jdbc:postgresql://10.147.17.110:5432/votos_elecciones_grajj";
    private static final String DB_USER = "votaciones_grajj";
    private static final String DB_PASSWORD = "votaciones_grajj";
    
    public static void main(String[] args) {
        System.out.println("🧪 === TEST CONEXIÓN DIRECTA A BASE DE DATOS ===");
        
        try {
            // Cargar driver
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver PostgreSQL cargado");
            
            // Conectar a la base de datos
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                System.out.println("✅ Conexión exitosa a la base de datos");
                System.out.println("   📍 URL: " + DB_URL);
                System.out.println("   👤 Usuario: " + DB_USER);
                
                // Verificar si la tabla votos existe
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet tables = metaData.getTables(null, null, "votos", null)) {
                    if (tables.next()) {
                        System.out.println("✅ Tabla 'votos' existe");
                        
                        // Contar votos existentes
                        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM votos");
                             ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                int count = rs.getInt(1);
                                System.out.println("📊 Total votos en BD: " + count);
                            }
                        }
                        
                        // Mostrar últimos 5 votos
                        System.out.println("\n📋 Últimos 5 votos:");
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "SELECT mesa_id, candidato_id, timestamp, municipio, departamento " +
                                "FROM votos ORDER BY fecha_recepcion DESC LIMIT 5");
                             ResultSet rs = stmt.executeQuery()) {
                            
                            int i = 1;
                            while (rs.next()) {
                                System.out.printf("   %d. Mesa: %s, Candidato: %s, Municipio: %s%n",
                                    i++, rs.getString("mesa_id"), rs.getString("candidato_id"), 
                                    rs.getString("municipio"));
                            }
                            
                            if (i == 1) {
                                System.out.println("   (No hay votos en la base de datos)");
                            }
                        }
                        
                        // Intentar insertar un voto de prueba
                        System.out.println("\n🧪 Insertando voto de prueba...");
                        String insertSQL = "INSERT INTO votos (mesa_id, candidato_id, timestamp, municipio, " +
                                          "departamento, hash_verificacion, firma_mesa, fecha_recepcion, estado) " +
                                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                          "ON CONFLICT (mesa_id, candidato_id, timestamp, hash_verificacion) " +
                                          "DO NOTHING";
                        
                        try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                            LocalDateTime now = LocalDateTime.now();
                            stmt.setString(1, "TEST_MESA_999");
                            stmt.setString(2, "TEST_CANDIDATO_999");
                            stmt.setTimestamp(3, Timestamp.valueOf(now));
                            stmt.setString(4, "TEST_MUNICIPIO");
                            stmt.setString(5, "TEST_DEPARTAMENTO");
                            stmt.setString(6, "TEST_HASH_" + System.currentTimeMillis());
                            stmt.setString(7, "TEST_FIRMA_MESA");
                            stmt.setTimestamp(8, Timestamp.valueOf(now));
                            stmt.setString(9, "PROCESADO");
                            
                            int rowsAffected = stmt.executeUpdate();
                            if (rowsAffected > 0) {
                                System.out.println("✅ Voto de prueba insertado exitosamente");
                            } else {
                                System.out.println("ℹ️ Voto de prueba ya existía (duplicado)");
                            }
                        }
                        
                    } else {
                        System.out.println("❌ Tabla 'votos' NO existe");
                        
                        // Crear la tabla
                        System.out.println("🔧 Creando tabla 'votos'...");
                        String createTableSQL = "CREATE TABLE IF NOT EXISTS votos (" +
                            "id SERIAL PRIMARY KEY," +
                            "mesa_id VARCHAR(50) NOT NULL," +
                            "candidato_id VARCHAR(50) NOT NULL," +
                            "timestamp TIMESTAMP NOT NULL," +
                            "municipio VARCHAR(100) NOT NULL," +
                            "departamento VARCHAR(100) NOT NULL," +
                            "hash_verificacion VARCHAR(255) NOT NULL," +
                            "firma_mesa TEXT," +
                            "fecha_recepcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "estado VARCHAR(20) DEFAULT 'PENDIENTE'," +
                            "UNIQUE(mesa_id, candidato_id, timestamp, hash_verificacion)" +
                            ")";
                        
                        try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
                            stmt.executeUpdate();
                            System.out.println("✅ Tabla 'votos' creada exitosamente");
                        }
                    }
                }
                
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver PostgreSQL no encontrado: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Error de base de datos: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n==============================================");
    }
} 