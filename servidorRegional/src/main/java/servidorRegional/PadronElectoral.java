package servidorRegional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Clase de utilidad para generar y manejar el padrón electoral en bases de datos SQLite.
 */
public class PadronElectoral {

    /**
     * Crea una base de datos de padrón electoral para un departamento específico.
     * La base de datos contiene una tabla de electores con IDs generados aleatoriamente.
     *
     * @param departamento El nombre del departamento para el cual se genera el padrón.
     * @param cantidadElectores El número de electores de prueba a generar.
     * @return Un array de bytes que representa el archivo de la base de datos SQLite.
     * @throws SQLException Si ocurre un error de SQL durante la creación de la base de datos.
     * @throws IOException Si ocurre un error al leer el archivo de la base de datos.
     */
    public static byte[] generarPadron(String departamento, int cantidadElectores) throws SQLException, IOException {
        // Crear un nombre de archivo único para la base de datos temporal
        String dbFileName = "padron_" + departamento.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis() + ".db";
        File dbFile = new File(dbFileName);
        String dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            if (conn != null) {
                System.out.println("✅ Conexión establecida a la base de datos temporal: " + dbFile.getName());

                // Crear tabla de electores
                String sqlCreateTable = "CREATE TABLE IF NOT EXISTS electores (id_elector TEXT PRIMARY KEY, ha_votado INTEGER NOT NULL DEFAULT 0);";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sqlCreateTable);
                    System.out.println("✅ Tabla 'electores' creada o ya existente.");
                }

                // Insertar electores de prueba
                String sqlInsert = "INSERT INTO electores(id_elector) VALUES(?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                    System.out.println("Insertando " + cantidadElectores + " electores de prueba...");
                    for (int i = 0; i < cantidadElectores; i++) {
                        // Generar un ID de elector aleatorio (formato UUID)
                        pstmt.setString(1, UUID.randomUUID().toString());
                        pstmt.executeUpdate();
                    }
                    System.out.println("✅ " + cantidadElectores + " electores insertados.");
                }
            }
        } finally {
            // Asegurarse de que la conexión se cierre para poder leer el archivo
            // El try-with-resources ya se encarga de esto.
        }

        // Leer el archivo de la base de datos a un array de bytes
        byte[] dbBytes = Files.readAllBytes(dbFile.toPath());
        System.out.println("✅ Archivo de base de datos leído en memoria (" + dbBytes.length + " bytes).");

        // Eliminar el archivo temporal
        if (dbFile.delete()) {
            System.out.println("✅ Archivo de base de datos temporal eliminado: " + dbFile.getName());
        } else {
            System.err.println("⚠️ No se pudo eliminar el archivo de base de datos temporal: " + dbFile.getName());
        }

        return dbBytes;
    }
} 