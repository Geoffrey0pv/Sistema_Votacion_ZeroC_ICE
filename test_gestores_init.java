import java.io.File;

public class test_gestores_init {
    public static void main(String[] args) {
        System.out.println("🧪 Prueba de inicialización de gestores");
        
        // Limpiar datos previos
        File dataDir = new File("data");
        if (dataDir.exists()) {
            File[] files = dataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        
        try {
            System.out.println("🔄 Probando GestorVotantesSQLite...");
            
            // Crear instancia de GestorVotantesSQLite
            // Esto debería llamar al constructor que inicializa la base de datos
            Object gestorVotantes = Class.forName("GestorMesa.GestorVotantesSQLite")
                .getConstructor(String.class, String.class)
                .newInstance("MESA001", "tcp -h localhost -p 8080");
            
            System.out.println("✅ GestorVotantesSQLite inicializado");
            
            // Verificar que el archivo se creó
            File votantesDB = new File("data/mesa_MESA001.sqlite");
            if (votantesDB.exists() && votantesDB.length() > 0) {
                System.out.println("✅ Base de datos de votantes creada: " + votantesDB.length() + " bytes");
            } else {
                System.out.println("❌ Base de datos de votantes no creada o vacía");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error inicializando GestorVotantesSQLite: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            System.out.println("🔄 Probando GestorCandidatosMesa...");
            
            // Crear instancia de GestorCandidatosMesa
            Object gestorCandidatos = Class.forName("GestorMesa.GestorCandidatosMesa")
                .getConstructor(String.class, String.class)
                .newInstance("MESA001", "tcp -h localhost -p 8080");
            
            System.out.println("✅ GestorCandidatosMesa inicializado");
            
            // Verificar que el archivo se creó
            File candidatosDB = new File("data/candidatos_mesa_MESA001.sqlite");
            if (candidatosDB.exists() && candidatosDB.length() > 0) {
                System.out.println("✅ Base de datos de candidatos creada: " + candidatosDB.length() + " bytes");
            } else {
                System.out.println("❌ Base de datos de candidatos no creada o vacía");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error inicializando GestorCandidatosMesa: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("✅ Prueba completada");
    }
} 