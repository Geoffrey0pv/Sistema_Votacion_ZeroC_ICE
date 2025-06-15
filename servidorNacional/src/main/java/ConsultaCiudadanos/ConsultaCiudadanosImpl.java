package ConsultaCiudadanos;

import Demo.*;
import Database.DatabaseConnection;
import com.zeroc.Ice.Current;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SERVICIO SÚPER OPTIMIZADO de consulta de ciudadanos por departamentos
 * Diseñado para ser una MÁQUINA DE PROCESAMIENTO SÚPER VELOZ
 * Optimizado para resistir miles de peticiones simultáneas
 */
public class ConsultaCiudadanosImpl implements IConsultaCiudadanos {
    
    private final DatabaseConnection dbConnection;
    
    // CONFIGURACIÓN SÚPER AGRESIVA PARA ALTO RENDIMIENTO
    private static final int LIMITE_POR_DEFECTO = 10000; // Aumentado para mejor rendimiento
    private static final int TAMAÑO_PAGINA_DEFECTO = 1000; // Páginas más grandes
    private static final int FETCH_SIZE_OPTIMIZADO = 1000; // Fetch size optimizado
    
    // CACHE DE PREPARED STATEMENTS PARA SÚPER VELOCIDAD
    private final ConcurrentHashMap<String, String> queryCache;
    
    // MÉTRICAS DE RENDIMIENTO EN TIEMPO REAL
    private final AtomicLong totalConsultasServidas;
    private final AtomicLong totalCiudadanosRetornados;
    private final AtomicLong tiempoTotalProcesamiento;
    private final AtomicInteger consultasSimultaneas;
    private final AtomicLong consultasMasRapida;
    private final AtomicLong consultasMasLenta;
    
    public ConsultaCiudadanosImpl() {
        this.dbConnection = new DatabaseConnection();
        this.queryCache = new ConcurrentHashMap<>();
        
        // Inicializar métricas
        this.totalConsultasServidas = new AtomicLong(0);
        this.totalCiudadanosRetornados = new AtomicLong(0);
        this.tiempoTotalProcesamiento = new AtomicLong(0);
        this.consultasSimultaneas = new AtomicInteger(0);
        this.consultasMasRapida = new AtomicLong(Long.MAX_VALUE);
        this.consultasMasLenta = new AtomicLong(0);
        
        // Pre-cargar queries en cache
        precargarQueriesEnCache();
        
        System.out.println("🚀 ConsultaCiudadanos iniciado:");
        System.out.println("   ⚡ Límite por defecto: " + LIMITE_POR_DEFECTO);
        System.out.println("   📄 Tamaño página: " + TAMAÑO_PAGINA_DEFECTO);
        System.out.println("   🔥 Fetch size: " + FETCH_SIZE_OPTIMIZADO);
        System.out.println("   💾 Cache de queries: ACTIVADO");
        System.out.println("   📊 Métricas en tiempo real: ACTIVADAS");
        System.out.println("   🏁 LISTO PARA PROCESAMIENTO MASIVO PARALELO");
    }
    
    private void precargarQueriesEnCache() {
        // Pre-cargar las queries más comunes en cache para súper velocidad
        String baseQuery = "SELECT " +
            "c.id AS ciudadano_id, " +
            "c.documento, " +
            "c.nombre AS nombre_ciudadano, " +
            "c.apellido, " +
            "mv.consecutive AS mesa_consecutiva, " +
            "pv.nombre AS nombre_puesto, " +
            "m.nombre AS nombre_municipio, " +
            "d.nombre AS nombre_departamento " +
            "FROM ciudadano c " +
            "JOIN mesa_votacion mv ON c.mesa_id = mv.id " +
            "JOIN puesto_votacion pv ON mv.puesto_id = pv.id " +
            "JOIN municipio m ON pv.municipio_id = m.id " +
            "JOIN departamento d ON m.departamento_id = d.id " +
            "WHERE d.nombre IN ";
        
        String countQuery = "SELECT COUNT(*) FROM ciudadano c " +
            "JOIN mesa_votacion mv ON c.mesa_id = mv.id " +
            "JOIN puesto_votacion pv ON mv.puesto_id = pv.id " +
            "JOIN municipio m ON pv.municipio_id = m.id " +
            "JOIN departamento d ON m.departamento_id = d.id " +
            "WHERE d.nombre IN ";
        
        queryCache.put("BASE_QUERY", baseQuery);
        queryCache.put("COUNT_QUERY", countQuery);
        
        System.out.println("💾 Cache de queries precargado con " + queryCache.size() + " queries");
    }
    
    @Override
    public CiudadanoInfo[] consultarCiudadanosPorDepartamentos(String[] departamentos, Current current) {
        long inicioConsulta = System.nanoTime();
        consultasSimultaneas.incrementAndGet();
        
        try {
            System.out.printf("🔍 [SÚPER RÁPIDO] Consultando ciudadanos para %d departamentos (LÍMITE: %d): %s%n", 
                departamentos.length, LIMITE_POR_DEFECTO, String.join(", ", departamentos));
            
            // Usar el método optimizado con límite
            return consultarCiudadanosConLimiteOptimizado(departamentos, LIMITE_POR_DEFECTO, current);
            
        } finally {
            long tiempoConsulta = System.nanoTime() - inicioConsulta;
            actualizarMetricas(tiempoConsulta);
            consultasSimultaneas.decrementAndGet();
        }
    }
    
    @Override
    public CiudadanoInfo[] consultarCiudadanosConLimite(String[] departamentos, int limite, Current current) {
        long inicioConsulta = System.nanoTime();
        consultasSimultaneas.incrementAndGet();
        
        try {
            return consultarCiudadanosConLimiteOptimizado(departamentos, limite, current);
        } finally {
            long tiempoConsulta = System.nanoTime() - inicioConsulta;
            actualizarMetricas(tiempoConsulta);
            consultasSimultaneas.decrementAndGet();
        }
    }
    
    private CiudadanoInfo[] consultarCiudadanosConLimiteOptimizado(String[] departamentos, int limite, Current current) {
        System.out.printf("⚡ [SÚPER OPTIMIZADO] Consultando ciudadanos con límite %d para %d departamentos: %s%n", 
            limite, departamentos.length, String.join(", ", departamentos));
        
        if (departamentos == null || departamentos.length == 0) {
            System.err.println("❌ Lista de departamentos vacía");
            return new CiudadanoInfo[0];
        }
        
        if (limite <= 0 || limite > 50000) { // Límite más alto para mejor rendimiento
            System.err.println("❌ Límite inválido: " + limite + " (debe estar entre 1 y 50000)");
            return crearRespuestaError("ERROR_LIMITE", "Límite debe estar entre 1 y 50000");
        }
        
        List<CiudadanoInfo> ciudadanos = new ArrayList<>(limite); // Pre-dimensionar para eficiencia
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbConnection.getConnection();
            
            if (conn == null) {
                System.err.println("❌ No se pudo establecer conexión con la base de datos");
                return crearRespuestaError("SERVICIO_INACTIVO", "Base de datos no disponible");
            }
            
            // CONSTRUIR QUERY SÚPER OPTIMIZADA USANDO CACHE
            String baseQuery = queryCache.get("BASE_QUERY");
            StringBuilder sqlBuilder = new StringBuilder(baseQuery);
            
            // Agregar placeholders para cada departamento
            sqlBuilder.append("(");
            for (int i = 0; i < departamentos.length; i++) {
                if (i > 0) sqlBuilder.append(", ");
                sqlBuilder.append("?");
            }
            sqlBuilder.append(") ORDER BY d.nombre, c.apellido, c.nombre LIMIT ?");
            
            String sql = sqlBuilder.toString();
            System.out.println("📋 [SÚPER RÁPIDO] Ejecutando consulta SQL optimizada con límite: " + limite);
            
            stmt = conn.prepareStatement(sql);
            
            // OPTIMIZACIONES DE RENDIMIENTO A NIVEL DE STATEMENT
            stmt.setFetchSize(FETCH_SIZE_OPTIMIZADO); // Fetch size optimizado
            stmt.setQueryTimeout(30); // Timeout de 30 segundos
            
            // Establecer los parámetros
            for (int i = 0; i < departamentos.length; i++) {
                stmt.setString(i + 1, departamentos[i].trim().toUpperCase());
            }
            stmt.setInt(departamentos.length + 1, limite);
            
            long startTime = System.currentTimeMillis();
            rs = stmt.executeQuery();
            long queryTime = System.currentTimeMillis() - startTime;
            
            // PROCESAMIENTO SÚPER OPTIMIZADO DE RESULTADOS
            int count = 0;
            while (rs.next() && count < limite) {
                CiudadanoInfo ciudadano = new CiudadanoInfo();
                
                // Acceso optimizado a columnas por índice (más rápido que por nombre)
                ciudadano.id = rs.getLong(1);
                ciudadano.documento = rs.getString(2);
                ciudadano.nombre = rs.getString(3);
                ciudadano.apellido = rs.getString(4);
                ciudadano.mesa = rs.getString(5);
                ciudadano.puesto = rs.getString(6);
                ciudadano.municipio = rs.getString(7);
                ciudadano.departamento = rs.getString(8);
                
                ciudadanos.add(ciudadano);
                count++;
            }
            
            // Actualizar métricas
            totalCiudadanosRetornados.addAndGet(count);
            
            System.out.printf("✅ [SÚPER RÁPIDO] Consulta exitosa: %d ciudadanos encontrados en %dms (límite: %d)%n", 
                count, queryTime, limite);
            
            if (count == limite) {
                System.out.println("⚠️ ADVERTENCIA: Se alcanzó el límite. Puede haber más registros disponibles.");
                System.out.println("   Use consultarCiudadanosPaginado() para obtener todos los resultados.");
            }
            
        } catch (SQLException e) {
            System.err.printf("❌ Error SQL consultando ciudadanos: %s%n", e.getMessage());
            return crearRespuestaError("ERROR_SQL", "Error en consulta: " + e.getMessage());
            
        } catch (Exception e) {
            System.err.printf("❌ Error general consultando ciudadanos: %s%n", e.getMessage());
            return crearRespuestaError("ERROR_GENERAL", "Error interno: " + e.getMessage());
            
        } finally {
            cerrarRecursos(rs, stmt, conn);
        }
        
        return ciudadanos.toArray(new CiudadanoInfo[0]);
    }
    
    @Override
    public ResultadoPaginado consultarCiudadanosPaginado(String[] departamentos, int pagina, int tamanoPagina, Current current) {
        long inicioConsulta = System.nanoTime();
        consultasSimultaneas.incrementAndGet();
        
        try {
            System.out.printf("🔍 [SÚPER PAGINADO] Consultando ciudadanos paginados - Página %d, Tamaño %d, Departamentos: %s%n", 
                pagina, tamanoPagina, String.join(", ", departamentos));
            
            if (departamentos == null || departamentos.length == 0) {
                System.err.println("❌ Lista de departamentos vacía");
                return crearResultadoPaginadoVacio();
            }
            
            if (pagina < 1) {
                System.err.println("❌ Número de página inválido: " + pagina);
                return crearResultadoPaginadoVacio();
            }
            
            if (tamanoPagina <= 0 || tamanoPagina > 5000) { // Páginas más grandes permitidas
                System.err.println("❌ Tamaño de página inválido: " + tamanoPagina + " (debe estar entre 1 y 5000)");
                return crearResultadoPaginadoVacio();
            }
            
            Connection conn = null;
            
            try {
                conn = dbConnection.getConnection();
                
                if (conn == null) {
                    System.err.println("❌ No se pudo establecer conexión con la base de datos");
                    return crearResultadoPaginadoVacio();
                }
                
                // OPTIMIZACIÓN: Obtener total de registros SÚPER RÁPIDO
                long totalRegistros = contarCiudadanosPorDepartamentosOptimizado(conn, departamentos);
                
                if (totalRegistros == 0) {
                    System.out.println("⚠️ No se encontraron ciudadanos para los departamentos especificados");
                    return crearResultadoPaginadoVacio();
                }
                
                // Calcular información de paginación
                int totalPaginas = (int) Math.ceil((double) totalRegistros / tamanoPagina);
                int offset = (pagina - 1) * tamanoPagina;
                
                if (pagina > totalPaginas) {
                    System.err.printf("❌ Página %d no existe (total páginas: %d)%n", pagina, totalPaginas);
                    return crearResultadoPaginadoVacio();
                }
                
                // OBTENER CIUDADANOS DE LA PÁGINA SÚPER OPTIMIZADO
                CiudadanoInfo[] ciudadanos = obtenerCiudadanosPaginaOptimizado(conn, departamentos, offset, tamanoPagina);
                
                // Actualizar métricas
                totalCiudadanosRetornados.addAndGet(ciudadanos.length);
                
                // Crear resultado paginado
                ResultadoPaginado resultado = new ResultadoPaginado();
                resultado.ciudadanos = ciudadanos;
                resultado.totalRegistros = totalRegistros;
                resultado.paginaActual = pagina;
                resultado.totalPaginas = totalPaginas;
                resultado.hayMasPaginas = pagina < totalPaginas;
                
                System.out.printf("✅ [SÚPER PAGINADO] Página %d/%d obtenida: %d ciudadanos (total: %d)%n", 
                    pagina, totalPaginas, ciudadanos.length, totalRegistros);
                
                return resultado;
                
            } catch (SQLException e) {
                System.err.printf("❌ Error SQL en consulta paginada: %s%n", e.getMessage());
                return crearResultadoPaginadoVacio();
                
            } catch (Exception e) {
                System.err.printf("❌ Error general en consulta paginada: %s%n", e.getMessage());
                return crearResultadoPaginadoVacio();
                
            } finally {
                if (conn != null) {
                    dbConnection.returnConnection(conn);
                }
            }
            
        } finally {
            long tiempoConsulta = System.nanoTime() - inicioConsulta;
            actualizarMetricas(tiempoConsulta);
            consultasSimultaneas.decrementAndGet();
        }
    }
    
    @Override
    public long contarCiudadanosPorDepartamentos(String[] departamentos, Current current) {
        long inicioConsulta = System.nanoTime();
        consultasSimultaneas.incrementAndGet();
        
        try {
            System.out.printf("🔢 [SÚPER CONTADOR] Contando ciudadanos para %d departamentos: %s%n", 
                departamentos.length, String.join(", ", departamentos));
            
            if (departamentos == null || departamentos.length == 0) {
                return 0;
            }
            
            Connection conn = null;
            
            try {
                conn = dbConnection.getConnection();
                
                if (conn == null) {
                    System.err.println("❌ No se pudo establecer conexión con la base de datos");
                    return -1;
                }
                
                return contarCiudadanosPorDepartamentosOptimizado(conn, departamentos);
                
            } catch (Exception e) {
                System.err.printf("❌ Error contando ciudadanos: %s%n", e.getMessage());
                return -1;
                
            } finally {
                if (conn != null) {
                    dbConnection.returnConnection(conn);
                }
            }
            
        } finally {
            long tiempoConsulta = System.nanoTime() - inicioConsulta;
            actualizarMetricas(tiempoConsulta);
            consultasSimultaneas.decrementAndGet();
        }
    }
    
    // MÉTODOS AUXILIARES SÚPER OPTIMIZADOS
    
    private long contarCiudadanosPorDepartamentosOptimizado(Connection conn, String[] departamentos) throws SQLException {
        String countQuery = queryCache.get("COUNT_QUERY");
        StringBuilder sqlBuilder = new StringBuilder(countQuery);
        
        sqlBuilder.append("(");
        for (int i = 0; i < departamentos.length; i++) {
            if (i > 0) sqlBuilder.append(", ");
            sqlBuilder.append("?");
        }
        sqlBuilder.append(")");
        
        try (PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            // OPTIMIZACIONES DE RENDIMIENTO
            stmt.setQueryTimeout(15); // Timeout más corto para conteos
            
            for (int i = 0; i < departamentos.length; i++) {
                stmt.setString(i + 1, departamentos[i].trim().toUpperCase());
            }
            
            long startTime = System.currentTimeMillis();
            try (ResultSet rs = stmt.executeQuery()) {
                long queryTime = System.currentTimeMillis() - startTime;
                
                if (rs.next()) {
                    long total = rs.getLong(1);
                    System.out.printf("📊 [SÚPER CONTADOR] Total ciudadanos encontrados: %,d (consulta en %dms)%n", total, queryTime);
                    return total;
                }
            }
        }
        
        return 0;
    }
    
    private CiudadanoInfo[] obtenerCiudadanosPaginaOptimizado(Connection conn, String[] departamentos, int offset, int limite) throws SQLException {
        String baseQuery = queryCache.get("BASE_QUERY");
        StringBuilder sqlBuilder = new StringBuilder(baseQuery);
        
        sqlBuilder.append("(");
        for (int i = 0; i < departamentos.length; i++) {
            if (i > 0) sqlBuilder.append(", ");
            sqlBuilder.append("?");
        }
        sqlBuilder.append(") ORDER BY d.nombre, c.apellido, c.nombre LIMIT ? OFFSET ?");
        
        List<CiudadanoInfo> ciudadanos = new ArrayList<>(limite);
        
        try (PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            // OPTIMIZACIONES SÚPER AGRESIVAS
            stmt.setFetchSize(Math.min(limite, FETCH_SIZE_OPTIMIZADO));
            stmt.setQueryTimeout(30);
            
            for (int i = 0; i < departamentos.length; i++) {
                stmt.setString(i + 1, departamentos[i].trim().toUpperCase());
            }
            stmt.setInt(departamentos.length + 1, limite);
            stmt.setInt(departamentos.length + 2, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CiudadanoInfo ciudadano = new CiudadanoInfo();
                    
                    // Acceso optimizado por índice
                    ciudadano.id = rs.getLong(1);
                    ciudadano.documento = rs.getString(2);
                    ciudadano.nombre = rs.getString(3);
                    ciudadano.apellido = rs.getString(4);
                    ciudadano.mesa = rs.getString(5);
                    ciudadano.puesto = rs.getString(6);
                    ciudadano.municipio = rs.getString(7);
                    ciudadano.departamento = rs.getString(8);
                    
                    ciudadanos.add(ciudadano);
                }
            }
        }
        
        return ciudadanos.toArray(new CiudadanoInfo[0]);
    }
    
    private void actualizarMetricas(long tiempoNanos) {
        totalConsultasServidas.incrementAndGet();
        tiempoTotalProcesamiento.addAndGet(tiempoNanos);
        
        long tiempoMs = tiempoNanos / 1_000_000;
        
        // Actualizar tiempo más rápido
        long actualRapida = consultasMasRapida.get();
        if (tiempoMs < actualRapida) {
            consultasMasRapida.compareAndSet(actualRapida, tiempoMs);
        }
        
        // Actualizar tiempo más lento
        long actualLenta = consultasMasLenta.get();
        if (tiempoMs > actualLenta) {
            consultasMasLenta.compareAndSet(actualLenta, tiempoMs);
        }
    }
    
    private ResultadoPaginado crearResultadoPaginadoVacio() {
        ResultadoPaginado resultado = new ResultadoPaginado();
        resultado.ciudadanos = new CiudadanoInfo[0];
        resultado.totalRegistros = 0;
        resultado.paginaActual = 1;
        resultado.totalPaginas = 0;
        resultado.hayMasPaginas = false;
        return resultado;
    }
    
    private void cerrarRecursos(ResultSet rs, PreparedStatement stmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) {
                dbConnection.returnConnection(conn);
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error cerrando recursos: " + e.getMessage());
            if (conn != null) {
                dbConnection.closeConnection(conn);
            }
        }
    }
    
    @Override
    public boolean verificarConexionBD(Current current) {
        System.out.println("🔧 [SÚPER VERIFICADOR] Verificando conexión a base de datos...");
        
        // Usar el método del pool para verificar estado
        boolean activo = dbConnection.isServiceActive();
        
        if (activo) {
            // Hacer una prueba adicional con una consulta simple
            Connection conn = null;
            try {
                conn = dbConnection.getConnection();
                if (conn != null && !conn.isClosed()) {
                    // Ejecutar una consulta simple para verificar conectividad real
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
                        stmt.executeQuery();
                        System.out.println("✅ [SÚPER VERIFICADOR] Conexión a BD verificada correctamente");
                        return true;
                    }
                } else {
                    System.err.println("❌ Conexión a BD no disponible");
                    return false;
                }
            } catch (SQLException e) {
                System.err.printf("❌ Error verificando conexión BD: %s%n", e.getMessage());
                return false;
            } finally {
                if (conn != null) {
                    dbConnection.returnConnection(conn);
                }
            }
        } else {
            System.err.println("❌ Servicio de BD marcado como inactivo");
            return false;
        }
    }
    
    /**
     * Crea una respuesta de error con información específica
     */
    private CiudadanoInfo[] crearRespuestaError(String tipoError, String mensaje) {
        CiudadanoInfo error = new CiudadanoInfo();
        error.id = -1;
        error.documento = tipoError;
        error.nombre = mensaje;
        error.apellido = "";
        
        return new CiudadanoInfo[]{error};
    }
    
    /**
     * ESTADÍSTICAS SÚPER DETALLADAS DE RENDIMIENTO
     */
    public void mostrarEstadisticasSuperDetalladas() {
        long totalConsultas = totalConsultasServidas.get();
        long totalCiudadanos = totalCiudadanosRetornados.get();
        long tiempoTotal = tiempoTotalProcesamiento.get() / 1_000_000; // Convertir a ms
        int simultaneas = consultasSimultaneas.get();
        long masRapida = consultasMasRapida.get();
        long masLenta = consultasMasLenta.get();
        
        double promedioMs = totalConsultas > 0 ? (double) tiempoTotal / totalConsultas : 0;
        double ciudadanosPorSegundo = tiempoTotal > 0 ? (double) totalCiudadanos * 1000 / tiempoTotal : 0;
        
        System.out.println("\n🚀 ===== ESTADÍSTICAS SÚPER DETALLADAS =====");
        System.out.println("   🔍 Servicio: ConsultaCiudadanos SÚPER OPTIMIZADO");
        System.out.println("   📊 Total consultas servidas: " + String.format("%,d", totalConsultas));
        System.out.println("   👥 Total ciudadanos retornados: " + String.format("%,d", totalCiudadanos));
        System.out.println("   ⏱️ Tiempo total procesamiento: " + String.format("%,d", tiempoTotal) + "ms");
        System.out.println("   📈 Consultas simultáneas actuales: " + simultaneas);
        System.out.println("   ⚡ Consulta más rápida: " + masRapida + "ms");
        System.out.println("   🐌 Consulta más lenta: " + masLenta + "ms");
        System.out.println("   📊 Tiempo promedio por consulta: " + String.format("%.2f", promedioMs) + "ms");
        System.out.println("   🚀 Ciudadanos por segundo: " + String.format("%.0f", ciudadanosPorSegundo));
        System.out.println("   🗃️ Base de datos: " + (verificarConexionBD(null) ? "✅ SÚPER ACTIVA" : "❌ INACTIVA"));
        System.out.println("   📊 Pool stats: " + dbConnection.getPoolStats());
        System.out.println("===============================================\n");
    }
    
    /**
     * Método para cerrar recursos al finalizar el servicio
     */
    public void shutdown() {
        System.out.println("🛑 Cerrando servicio SÚPER OPTIMIZADO ConsultaCiudadanos...");
        mostrarEstadisticasSuperDetalladas();
        // El pool se encarga de cerrar las conexiones
    }
} 