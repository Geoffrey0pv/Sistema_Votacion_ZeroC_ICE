package servidorRegional;

import Demo.*;
import com.zeroc.Ice.Current;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Paths;
import java.nio.file.Files;

public class ConsultaMesaSQLiteImpl implements IConsultaMesaSQLite {
    
    private static final String DB_PATH_PREFIX = "data/mesa_";
    private static final String DB_PATH_SUFFIX = ".db";
    
    public ConsultaMesaSQLiteImpl() {
        System.out.println("🗄️  Servicio de Consulta de Mesas SQLite inicializado");
    }

    @Override
    public boolean existeMesaSQLite(String mesaId, Current current) {
        String dbPath = DB_PATH_PREFIX + mesaId + DB_PATH_SUFFIX;
        File dbFile = new File(dbPath);
        boolean existe = dbFile.exists() && dbFile.isFile();
        System.out.println("🔍 Verificando mesa " + mesaId + ": " + (existe ? "EXISTE" : "NO EXISTE"));
        return existe;
    }

    @Override
    public EstadisticasMesaSQLite obtenerEstadisticasMesa(String mesaId, Current current) {
        String dbPath = DB_PATH_PREFIX + mesaId + DB_PATH_SUFFIX;
        
        if (!existeMesaSQLite(mesaId, current)) {
            System.err.println("❌ Base de datos no encontrada para mesa: " + mesaId);
            return null;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String sql = "SELECT mesa_id, departamento, municipio, puesto, total_votantes, " +
                        "votantes_verificados, mesa_activa, fecha_creacion, ultima_actualizacion " +
                        "FROM estadisticas_mesa WHERE mesa_id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, mesaId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    EstadisticasMesaSQLite stats = new EstadisticasMesaSQLite();
                    stats.mesaId = rs.getString("mesa_id");
                    stats.departamento = rs.getString("departamento") != null ? rs.getString("departamento") : "";
                    stats.municipio = rs.getString("municipio") != null ? rs.getString("municipio") : "";
                    stats.puesto = rs.getString("puesto") != null ? rs.getString("puesto") : "";
                    stats.totalVotantes = rs.getInt("total_votantes");
                    stats.votantesVerificados = rs.getInt("votantes_verificados");
                    stats.mesaActiva = rs.getInt("mesa_activa");
                    stats.fechaCreacion = rs.getString("fecha_creacion") != null ? rs.getString("fecha_creacion") : "";
                    stats.ultimaActualizacion = rs.getLong("ultima_actualizacion");
                    
                    System.out.println("📊 Estadísticas obtenidas para mesa " + mesaId + 
                                     " - Votantes: " + stats.totalVotantes);
                    return stats;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error consultando estadísticas de mesa " + mesaId + ": " + e.getMessage());
        }
        
        return null;
    }

    @Override
    public VotanteMesa[] obtenerVotantesDeMesa(String mesaId, Current current) {
        return obtenerVotantesPaginados(mesaId, 1, Integer.MAX_VALUE, current);
    }

    @Override
    public VotanteMesa[] obtenerVotantesPaginados(String mesaId, int pagina, int tamanoPagina, Current current) {
        String dbPath = DB_PATH_PREFIX + mesaId + DB_PATH_SUFFIX;
        
        if (!existeMesaSQLite(mesaId, current)) {
            System.err.println("❌ Base de datos no encontrada para mesa: " + mesaId);
            return new VotanteMesa[0];
        }

        List<VotanteMesa> votantes = new ArrayList<>();
        int offset = (pagina - 1) * tamanoPagina;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String sql = "SELECT id, ciudadano_id, documento, nombre, apellido, mesa, mesa_id, " +
                        "puesto, municipio, departamento, fecha_asignacion, verificado, fecha_verificacion " +
                        "FROM votantes_mesa ORDER BY id LIMIT ? OFFSET ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, tamanoPagina);
                pstmt.setInt(2, offset);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    VotanteMesa votante = new VotanteMesa();
                    votante.id = rs.getInt("id");
                    votante.ciudadanoId = rs.getLong("ciudadano_id");
                    votante.documento = rs.getString("documento");
                    votante.nombre = rs.getString("nombre");
                    votante.apellido = rs.getString("apellido");
                    votante.mesa = rs.getString("mesa");
                    votante.mesaId = rs.getString("mesa_id");
                    votante.puesto = rs.getString("puesto") != null ? rs.getString("puesto") : "";
                    votante.municipio = rs.getString("municipio") != null ? rs.getString("municipio") : "";
                    votante.departamento = rs.getString("departamento");
                    votante.fechaAsignacion = rs.getString("fecha_asignacion") != null ? rs.getString("fecha_asignacion") : "";
                    votante.verificado = rs.getInt("verificado");
                    votante.fechaVerificacion = rs.getString("fecha_verificacion") != null ? rs.getString("fecha_verificacion") : "";
                    
                    votantes.add(votante);
                }
            }
            
            System.out.println("📋 Obtenidos " + votantes.size() + " votantes de mesa " + mesaId +
                             " (página " + pagina + ", tamaño " + tamanoPagina + ")");
            
        } catch (SQLException e) {
            System.err.println("❌ Error consultando votantes de mesa " + mesaId + ": " + e.getMessage());
        }
        
        return votantes.toArray(new VotanteMesa[0]);
    }

    @Override
    public VotanteMesa buscarVotantePorDocumento(String mesaId, String documento, Current current) {
        String dbPath = DB_PATH_PREFIX + mesaId + DB_PATH_SUFFIX;
        
        if (!existeMesaSQLite(mesaId, current)) {
            System.err.println("❌ Base de datos no encontrada para mesa: " + mesaId);
            return null;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String sql = "SELECT id, ciudadano_id, documento, nombre, apellido, mesa, mesa_id, " +
                        "puesto, municipio, departamento, fecha_asignacion, verificado, fecha_verificacion " +
                        "FROM votantes_mesa WHERE documento = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, documento);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    VotanteMesa votante = new VotanteMesa();
                    votante.id = rs.getInt("id");
                    votante.ciudadanoId = rs.getLong("ciudadano_id");
                    votante.documento = rs.getString("documento");
                    votante.nombre = rs.getString("nombre");
                    votante.apellido = rs.getString("apellido");
                    votante.mesa = rs.getString("mesa");
                    votante.mesaId = rs.getString("mesa_id");
                    votante.puesto = rs.getString("puesto") != null ? rs.getString("puesto") : "";
                    votante.municipio = rs.getString("municipio") != null ? rs.getString("municipio") : "";
                    votante.departamento = rs.getString("departamento");
                    votante.fechaAsignacion = rs.getString("fecha_asignacion") != null ? rs.getString("fecha_asignacion") : "";
                    votante.verificado = rs.getInt("verificado");
                    votante.fechaVerificacion = rs.getString("fecha_verificacion") != null ? rs.getString("fecha_verificacion") : "";
                    
                    System.out.println("🔍 Votante encontrado en mesa " + mesaId + ": " + votante.nombre + " " + votante.apellido);
                    return votante;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error buscando votante " + documento + " en mesa " + mesaId + ": " + e.getMessage());
        }
        
        System.out.println("⚠️ Votante con documento " + documento + " no encontrado en mesa " + mesaId);
        return null;
    }

    @Override
    public LogVerificacion[] obtenerLogsVerificacion(String mesaId, Current current) {
        String dbPath = DB_PATH_PREFIX + mesaId + DB_PATH_SUFFIX;
        
        if (!existeMesaSQLite(mesaId, current)) {
            System.err.println("❌ Base de datos no encontrada para mesa: " + mesaId);
            return new LogVerificacion[0];
        }

        List<LogVerificacion> logs = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String sql = "SELECT id, documento, accion, resultado, timestamp " +
                        "FROM log_verificaciones ORDER BY timestamp DESC";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    LogVerificacion log = new LogVerificacion();
                    log.id = rs.getInt("id");
                    log.documento = rs.getString("documento");
                    log.accion = rs.getString("accion");
                    log.resultado = rs.getString("resultado") != null ? rs.getString("resultado") : "";
                    log.timestamp = rs.getString("timestamp") != null ? rs.getString("timestamp") : "";
                    
                    logs.add(log);
                }
            }
            
            System.out.println("📝 Obtenidos " + logs.size() + " logs de verificación de mesa " + mesaId);
            
        } catch (SQLException e) {
            System.err.println("❌ Error consultando logs de mesa " + mesaId + ": " + e.getMessage());
        }
        
        return logs.toArray(new LogVerificacion[0]);
    }

    @Override
    public InfoCompletaMesa obtenerInfoCompletaMesa(String mesaId, Current current) {
        InfoCompletaMesa info = new InfoCompletaMesa();
        String dbPath = DB_PATH_PREFIX + mesaId + DB_PATH_SUFFIX;
        
        info.archivoExiste = existeMesaSQLite(mesaId, current);
        info.rutaArchivo = new File(dbPath).getAbsolutePath();
        
        if (!info.archivoExiste) {
            System.err.println("❌ Base de datos no encontrada para mesa: " + mesaId);
            info.estadisticas = new EstadisticasMesaSQLite();
            info.votantes = new VotanteMesa[0];
            info.logs = new LogVerificacion[0];
            return info;
        }
        
        System.out.println("📊 Obteniendo información completa de mesa " + mesaId);
        
        // Obtener estadísticas
        info.estadisticas = obtenerEstadisticasMesa(mesaId, current);
        if (info.estadisticas == null) {
            info.estadisticas = new EstadisticasMesaSQLite();
        }
        
        // Obtener votantes (limitado a 1000 para evitar sobrecarga)
        info.votantes = obtenerVotantesPaginados(mesaId, 1, 1000, current);
        
        // Obtener logs
        info.logs = obtenerLogsVerificacion(mesaId, current);
        
        System.out.println("✅ Información completa obtenida para mesa " + mesaId);
        return info;
    }

    @Override
    public int contarVotantesMesa(String mesaId, Current current) {
        String dbPath = DB_PATH_PREFIX + mesaId + DB_PATH_SUFFIX;
        
        if (!existeMesaSQLite(mesaId, current)) {
            System.err.println("❌ Base de datos no encontrada para mesa: " + mesaId);
            return 0;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String sql = "SELECT COUNT(*) as total FROM votantes_mesa";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    int total = rs.getInt("total");
                    System.out.println("🔢 Total votantes en mesa " + mesaId + ": " + total);
                    return total;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error contando votantes de mesa " + mesaId + ": " + e.getMessage());
        }
        
        return 0;
    }

    @Override
    public int contarVotantesVerificados(String mesaId, Current current) {
        String dbPath = DB_PATH_PREFIX + mesaId + DB_PATH_SUFFIX;
        
        if (!existeMesaSQLite(mesaId, current)) {
            System.err.println("❌ Base de datos no encontrada para mesa: " + mesaId);
            return 0;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String sql = "SELECT COUNT(*) as total FROM votantes_mesa WHERE verificado = 1";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    int total = rs.getInt("total");
                    System.out.println("✅ Votantes verificados en mesa " + mesaId + ": " + total);
                    return total;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error contando votantes verificados de mesa " + mesaId + ": " + e.getMessage());
        }
        
        return 0;
    }

    @Override
    public String[] listarMesasDisponibles(Current current) {
        List<String> mesas = new ArrayList<>();
        File dataDir = new File("data");
        
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            System.err.println("❌ Directorio data no encontrado");
            return new String[0];
        }
        
        File[] archivos = dataDir.listFiles((dir, name) -> 
            name.startsWith("mesa_") && name.endsWith(".db"));
        
        if (archivos != null) {
            for (File archivo : archivos) {
                String nombre = archivo.getName();
                // Extraer el ID de mesa del nombre del archivo
                String mesaId = nombre.substring(5, nombre.length() - 3); // Quitar "mesa_" y ".db"
                mesas.add(mesaId);
            }
        }
        
        System.out.println("📋 Mesas SQLite disponibles: " + mesas.size());
        return mesas.toArray(new String[0]);
    }

    @Override
    public boolean verificarServicio(Current current) {
        File dataDir = new File("data");
        boolean servicioOK = dataDir.exists() && dataDir.isDirectory();
        
        if (servicioOK) {
            System.out.println("✅ Servicio de Consulta Mesa SQLite operativo");
        } else {
            System.err.println("❌ Servicio de Consulta Mesa SQLite con problemas - directorio data no encontrado");
        }
        
        return servicioOK;
    }
} 