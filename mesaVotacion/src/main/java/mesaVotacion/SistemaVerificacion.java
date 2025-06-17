package mesaVotacion;

import Demo.CiudadanoInfo;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sistema de Verificación de Mesa de Votación
 * Componente independiente que accede al SQLite específico de una mesa
 * para verificar votantes sin necesidad de consultar servidores centrales
 */
public class SistemaVerificacion {

    private final String mesaId;
    private final String DB_PATH;
    private final String DB_URL;

    public SistemaVerificacion(String mesaId) {
        this.mesaId = mesaId;
        this.DB_PATH = "data/mesa_" + mesaId.replaceAll("[^a-zA-Z0-9]", "_") + ".db";
        this.DB_URL = "jdbc:sqlite:" + DB_PATH;

        try {
            // Cargar el driver JDBC de SQLite
            Class.forName("org.sqlite.JDBC");
            verificarBaseDatos();
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error: Driver SQLite no encontrado. Agregue sqlite-jdbc a las dependencias.");
            throw new RuntimeException(e);
        } catch (java.lang.Exception e) {

        }
    }

    /**
     * Verifica que la base de datos de la mesa exista
     */
    private void verificarBaseDatos() {
        java.io.File dbFile = new java.io.File(DB_PATH);
        if (!dbFile.exists()) {
            System.err.println("❌ Base de datos de mesa no encontrada: " + DB_PATH);
            crearBaseDeDatos();
        }

        System.out.println("✅ Sistema de Verificación inicializado para Mesa " + mesaId);
        System.out.println("📁 Base de datos: " + DB_PATH);
    }
    private void crearBaseDeDatos() {

        System.out.println("💡 Creando base de datos de mesa: " + DB_PATH);
        
        String createTableSQL = "CREATE TABLE IF NOT EXISTS votantes_mesa ("
                + "documento TEXT PRIMARY KEY,"
                + "nombre TEXT,"
                + "apellido TEXT," 
                + "mesa TEXT,"
                + "mesa_id TEXT,"
                + "puesto TEXT,"
                + "municipio TEXT,"
                + "departamento TEXT"
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createTableSQL);
            System.out.println("✅ Base de datos creada y lista: " + DB_PATH);

        } catch (SQLException e) {
            System.err.println("❌ Error al crear la base de datos:");
            e.printStackTrace();
        }

    }

    /**
     * Verifica si un votante pertenece a esta mesa
     * 
     * @param documento Documento del votante
     * @return true si el votante pertenece a esta mesa
     */
    public boolean verificarVotante(String documento) {
        String selectSQL = "SELECT COUNT(*) FROM votantes_mesa WHERE documento = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, documento);
            ResultSet rs = pstmt.executeQuery();

            boolean existe = rs.next() && rs.getInt(1) > 0;

            // Registrar la verificación
            registrarVerificacion(documento, "VERIFICAR_DOCUMENTO", existe ? "ENCONTRADO" : "NO_ENCONTRADO");

            if (existe) {
                System.out.println("✅ Votante " + documento + " AUTORIZADO en Mesa " + mesaId);
            } else {
                System.out.println("❌ Votante " + documento + " NO pertenece a Mesa " + mesaId);
            }

            return existe;

        } catch (SQLException e) {
            System.err.println("❌ Error verificando votante: " + e.getMessage());
            registrarVerificacion(documento, "VERIFICAR_DOCUMENTO", "ERROR: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene información completa de un votante de la mesa
     * 
     * @param documento Documento del votante
     * @return Información del votante o null si no existe
     */
    public CiudadanoInfo obtenerInformacionVotante(String documento) {
        String selectSQL = "SELECT ciudadano_id, documento, nombre, apellido, mesa, mesa_id, puesto, municipio, departamento "
                +
                "FROM votantes_mesa WHERE documento = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, documento);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                CiudadanoInfo votante = new CiudadanoInfo();
                votante.id = rs.getLong("ciudadano_id");
                votante.documento = rs.getString("documento");
                votante.nombre = rs.getString("nombre");
                votante.apellido = rs.getString("apellido");
                votante.mesa = rs.getString("mesa");
                votante.mesaId = rs.getString("mesa_id");
                votante.puesto = rs.getString("puesto");
                votante.municipio = rs.getString("municipio");
                votante.departamento = rs.getString("departamento");

                registrarVerificacion(documento, "OBTENER_INFO", "EXITOSO");

                System.out.println("📋 Información del votante:");
                System.out.println("   Nombre: " + votante.nombre + " " + votante.apellido);
                System.out.println("   Documento: " + votante.documento);
                System.out.println("   Mesa: " + votante.mesa);
                System.out.println("   Puesto: " + votante.puesto);
                System.out.println("   Municipio: " + votante.municipio);

                return votante;
            }

            registrarVerificacion(documento, "OBTENER_INFO", "NO_ENCONTRADO");
            System.out.println("❌ Votante " + documento + " no encontrado en Mesa " + mesaId);
            return null;

        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo información: " + e.getMessage());
            registrarVerificacion(documento, "OBTENER_INFO", "ERROR: " + e.getMessage());
            return null;
        }
    }

    /**
     * Marca un votante como verificado (para control interno)
     * 
     * @param documento Documento del votante
     * @return true si se marcó exitosamente
     */
    public boolean marcarVotanteVerificado(String documento) {
        String updateSQL = "UPDATE votantes_mesa SET verificado = 1, fecha_verificacion = ? WHERE documento = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

            String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            pstmt.setString(1, fechaActual);
            pstmt.setString(2, documento);

            int filasActualizadas = pstmt.executeUpdate();
            boolean exitoso = filasActualizadas > 0;

            registrarVerificacion(documento, "MARCAR_VERIFICADO", exitoso ? "EXITOSO" : "NO_ENCONTRADO");

            if (exitoso) {
                System.out.println("✅ Votante " + documento + " marcado como verificado");
            } else {
                System.out.println("❌ No se pudo marcar votante " + documento);
            }

            return exitoso;

        } catch (SQLException e) {
            System.err.println("❌ Error marcando votante: " + e.getMessage());
            registrarVerificacion(documento, "MARCAR_VERIFICADO", "ERROR: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene estadísticas de la mesa
     * 
     * @return String con estadísticas formateadas
     */
    public String obtenerEstadisticasMesa() {
        String statsSQL = "SELECT " +
                "(SELECT COUNT(*) FROM votantes_mesa) as total_votantes, " +
                "(SELECT COUNT(*) FROM votantes_mesa WHERE verificado = 1) as votantes_verificados, " +
                "(SELECT COUNT(DISTINCT documento) FROM log_verificaciones WHERE DATE(timestamp) = DATE('now')) as verificaciones_hoy";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(statsSQL)) {

            if (rs.next()) {
                int totalVotantes = rs.getInt("total_votantes");
                int votantesVerificados = rs.getInt("votantes_verificados");
                int verificacionesHoy = rs.getInt("verificaciones_hoy");

                String estadisticas = "📊 === ESTADÍSTICAS MESA " + mesaId + " ===\n";
                estadisticas += "   📁 Base de datos: " + DB_PATH + "\n";
                estadisticas += "   👥 Total votantes asignados: " + totalVotantes + "\n";
                estadisticas += "   ✅ Votantes verificados: " + votantesVerificados + "\n";
                estadisticas += "   🔍 Verificaciones hoy: " + verificacionesHoy + "\n";
                estadisticas += "   📈 Porcentaje verificado: " +
                        (totalVotantes > 0 ? String.format("%.2f%%", (double) votantesVerificados / totalVotantes * 100)
                                : "0%")
                        + "\n";
                estadisticas += "=====================================";

                return estadisticas;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo estadísticas: " + e.getMessage());
        }

        return "❌ No se pudieron obtener estadísticas de Mesa " + mesaId;
    }

    /**
     * Lista todos los votantes asignados a la mesa
     * 
     * @param limite Número máximo de votantes a mostrar (0 = todos)
     * @return Número de votantes listados
     */
    public int listarVotantesMesa(int limite) {
        String selectSQL = "SELECT documento, nombre, apellido, verificado " +
                "FROM votantes_mesa ORDER BY apellido, nombre" +
                (limite > 0 ? " LIMIT " + limite : "");

        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {

            System.out.println("👥 === VOTANTES ASIGNADOS A MESA " + mesaId + " ===");

            int contador = 0;
            while (rs.next()) {
                contador++;
                String documento = rs.getString("documento");
                String nombre = rs.getString("nombre");
                String apellido = rs.getString("apellido");
                boolean verificado = rs.getInt("verificado") == 1;

                String estado = verificado ? "✅" : "⏳";
                System.out.println(String.format("   %3d. %s %s %s (Doc: %s)",
                        contador, estado, nombre, apellido, documento));
            }

            if (contador == 0) {
                System.out.println("   No hay votantes asignados a esta mesa");
            } else {
                System.out.println("================================================");
                System.out.println("Total mostrados: " + contador +
                        (limite > 0 && contador == limite ? " (limitado)" : ""));
            }

            return contador;

        } catch (SQLException e) {
            System.err.println("❌ Error listando votantes: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Registra una verificación en el log
     */
    private void registrarVerificacion(String documento, String accion, String resultado) {
        String insertSQL = "INSERT INTO log_verificaciones (documento, accion, resultado) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, documento);
            pstmt.setString(2, accion);
            pstmt.setString(3, resultado);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            // No es crítico si falla el log
            System.err.println("⚠️ Error registrando verificación: " + e.getMessage());
        }
    }

    /**
     * Verifica si la base de datos está disponible
     * 
     * @return true si la conexión es exitosa
     */
    public boolean verificarConexion() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el ID de la mesa
     * 
     * @return ID de la mesa
     */
    public String getMesaId() {
        return mesaId;
    }

    /**
     * Obtiene la ruta de la base de datos
     * 
     * @return Ruta del archivo SQLite
     */
    public String getDbPath() {
        return DB_PATH;
    }
}