import java.sql.*;
import java.io.File;

public class test_debug_db {
    public static void main(String[] args) {
        System.out.println("🔍 Depurando inicialización de base de datos");
        
        // Limpiar cualquier archivo existente
        File dataDir = new File("data");
        if (dataDir.exists()) {
            File[] files = dataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().contains("9080")) {
                        file.delete();
                        System.out.println("🗑️ Eliminado: " + file.getName());
                    }
                }
            }
        }
        
        // Crear directorio si no existe
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            System.out.println("📁 Directorio data creado: " + created);
        }
        
        // Test 1: Probar creación directa de base de datos
        System.out.println("\n🧪 Test 1: Creación directa de base de datos");
        testCreacionDirecta();
        
        // Test 2: Probar usando el constructor
        System.out.println("\n🧪 Test 2: Usando constructor GestorVotantesSQLite");
        testConstructor();
    }
    
    private static void testCreacionDirecta() {
        String dbPath = "data/test_direct_9080.sqlite";
        String dbUrl = "jdbc:sqlite:" + dbPath;
        
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ Driver SQLite cargado");
            
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                System.out.println("✅ Conexión establecida");
                
                String createTable = "CREATE TABLE IF NOT EXISTS votantes_mesa (" +
                    "documento TEXT PRIMARY KEY," +
                    "nombre TEXT NOT NULL," +
                    "apellido TEXT NOT NULL," +
                    "mesa TEXT NOT NULL" +
                    ")";
                
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTable);
                    System.out.println("✅ Tabla creada");
                }
                
                // Verificar archivo
                File dbFile = new File(dbPath);
                if (dbFile.exists()) {
                    System.out.println("✅ Archivo creado: " + dbFile.length() + " bytes");
                } else {
                    System.out.println("❌ Archivo no creado");
                }
                
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en test directo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testConstructor() {
        try {
            // Usar reflexión para crear el gestor
            Class<?> gestorClass = Class.forName("GestorMesa.GestorVotantesSQLite");
            Object gestor = gestorClass.getConstructor(String.class, String.class)
                .newInstance("9080", "tcp -h localhost -p 8080");
            
            System.out.println("✅ Constructor ejecutado");
            
            // Verificar archivos creados
            File votantesFile = new File("data/mesa_9080.sqlite");
            if (votantesFile.exists()) {
                System.out.println("✅ Archivo votantes creado: " + votantesFile.length() + " bytes");
            } else {
                System.out.println("❌ Archivo votantes NO creado");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en constructor test: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 