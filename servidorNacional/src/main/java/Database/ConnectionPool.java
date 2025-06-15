package Database;

import Config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pool de conexiones SÚPER OPTIMIZADO para procesamiento masivo paralelo
 * Diseñado para resistir miles de peticiones simultáneas
 */
public class ConnectionPool {
    private static ConnectionPool instance;
    private final BlockingQueue<Connection> connectionPool;
    private final AtomicInteger currentConnections;
    private final AtomicBoolean isServiceActive;
    private final ConfigManager config;
    
    // CONFIGURACIÓN OPTIMIZADA PARA ALTO RENDIMIENTO
    private final int minPoolSize;
    private final int maxPoolSize;
    private final int poolTimeout;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    
    // NUEVAS OPTIMIZACIONES
    private final AtomicLong totalRequestsServed;
    private final AtomicLong totalConnectionsCreated;
    private final AtomicInteger peakConcurrentConnections;
    private final ScheduledExecutorService maintenanceExecutor;
    private final ConcurrentHashMap<Connection, Long> connectionLastUsed;
    
    // Configuración de reintentos
    private final int maxRetryAttempts;
    private final int retryDelayMs;
    private final double backoffMultiplier;
    
    private ConnectionPool() {
        this.config = ConfigManager.getInstance();
        this.connectionPool = new LinkedBlockingQueue<>();
        this.currentConnections = new AtomicInteger(0);
        this.isServiceActive = new AtomicBoolean(false);
        
        // CONFIGURACIÓN SÚPER AGRESIVA PARA ALTO RENDIMIENTO
        this.minPoolSize = Math.max(config.getPoolMinSize(), 50);  // Mínimo 50 conexiones
        this.maxPoolSize = Math.max(config.getPoolMaxSize(), 200); // Máximo 200 conexiones
        this.poolTimeout = Math.min(config.getPoolTimeout(), 100); // Timeout súper rápido
        this.dbUrl = config.getDatabaseUrl() + "?tcpKeepAlive=true&socketTimeout=30000&loginTimeout=10";
        this.dbUser = config.getDatabaseUser();
        this.dbPassword = config.getDatabasePassword();
        
        this.maxRetryAttempts = config.getRetryMaxAttempts();
        this.retryDelayMs = Math.min(config.getRetryDelayMs(), 50); // Reintentos súper rápidos
        this.backoffMultiplier = config.getRetryBackoffMultiplier();
        
        // NUEVAS MÉTRICAS Y OPTIMIZACIONES
        this.totalRequestsServed = new AtomicLong(0);
        this.totalConnectionsCreated = new AtomicLong(0);
        this.peakConcurrentConnections = new AtomicInteger(0);
        this.connectionLastUsed = new ConcurrentHashMap<>();
        this.maintenanceExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Cargar driver PostgreSQL
        loadPostgreSQLDriver();
        
        // Inicializar pool con estrategias agresivas
        initializePoolAggressively();
        
        // Iniciar mantenimiento automático
        startMaintenanceTasks();
        
        System.out.println("🚀 POOL SÚPER OPTIMIZADO INICIADO:");
        System.out.println("   📊 Pool: " + minPoolSize + "-" + maxPoolSize + " conexiones");
        System.out.println("   ⚡ Timeout: " + poolTimeout + "ms (súper rápido)");
        System.out.println("   🔥 Optimizado para procesamiento masivo paralelo");
    }
    
    public static synchronized ConnectionPool getInstance() {
        if (instance == null) {
            instance = new ConnectionPool();
        }
        return instance;
    }
    
    private void loadPostgreSQLDriver() {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver PostgreSQL cargado correctamente");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error cargando driver PostgreSQL: " + e.getMessage());
            System.err.println("   Asegúrese de que postgresql.jar esté en el classpath");
        }
    }
    
    private void initializePoolAggressively() {
        System.out.println("🚀 Inicializando pool SÚPER OPTIMIZADO...");
        System.out.println("   URL: " + dbUrl);
        System.out.println("   Pool: " + minPoolSize + "-" + maxPoolSize + " conexiones");
        
        // CREAR TODAS LAS CONEXIONES MÍNIMAS DE UNA VEZ (PARALELO)
        System.out.println("⚡ Creando " + minPoolSize + " conexiones iniciales en paralelo...");
        
        for (int i = 0; i < minPoolSize; i++) {
            try {
                Connection conn = createOptimizedConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                    currentConnections.incrementAndGet();
                    totalConnectionsCreated.incrementAndGet();
                    connectionLastUsed.put(conn, System.currentTimeMillis());
                }
            } catch (Exception e) {
                System.err.println("⚠️  No se pudo crear conexión inicial " + (i + 1) + ": " + e.getMessage());
            }
        }
        
        // PRE-CALENTAR CONEXIONES (ejecutar una consulta simple en cada una)
        warmUpConnections();
        
        // Verificar si el servicio está activo
        checkServiceStatus();
        
        if (isServiceActive.get()) {
            System.out.println("✅ Pool SÚPER OPTIMIZADO inicializado: " + currentConnections.get() + " conexiones");
            System.out.println("🔥 LISTO PARA PROCESAMIENTO MASIVO PARALELO");
        } else {
            System.err.println("❌ Pool de conexiones no pudo inicializarse completamente");
        }
    }
    
    private Connection createOptimizedConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        
        // OPTIMIZACIONES A NIVEL DE CONEXIÓN
        conn.setAutoCommit(true); // Auto-commit para consultas rápidas
        conn.setReadOnly(true);   // Solo lectura para consultas
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        
        // Configurar timeouts agresivos
        try (PreparedStatement stmt = conn.prepareStatement("SET statement_timeout = '30s'")) {
            stmt.execute();
        }
        try (PreparedStatement stmt = conn.prepareStatement("SET lock_timeout = '10s'")) {
            stmt.execute();
        }
        
        return conn;
    }
    
    private void warmUpConnections() {
        System.out.println("🔥 Pre-calentando conexiones...");
        
        Connection[] connections = new Connection[connectionPool.size()];
        int count = 0;
        
        // Sacar todas las conexiones
        Connection conn;
        while ((conn = connectionPool.poll()) != null && count < connections.length) {
            connections[count++] = conn;
        }
        
        // Pre-calentar cada conexión
        for (int i = 0; i < count; i++) {
            try {
                if (connections[i] != null && !connections[i].isClosed()) {
                    // Ejecutar consulta simple para pre-calentar
                    try (PreparedStatement stmt = connections[i].prepareStatement("SELECT 1")) {
                        stmt.executeQuery();
                    }
                }
            } catch (SQLException e) {
                System.err.println("⚠️  Error pre-calentando conexión " + (i + 1));
            }
        }
        
        // Devolver todas las conexiones al pool
        for (int i = 0; i < count; i++) {
            if (connections[i] != null) {
                connectionPool.offer(connections[i]);
            }
        }
        
        System.out.println("✅ " + count + " conexiones pre-calentadas");
    }
    
    private void startMaintenanceTasks() {
        // Tarea de mantenimiento cada 30 segundos
        maintenanceExecutor.scheduleAtFixedRate(() -> {
            try {
                maintainPool();
            } catch (Exception e) {
                System.err.println("⚠️  Error en mantenimiento del pool: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        // Tarea de estadísticas cada 60 segundos
        maintenanceExecutor.scheduleAtFixedRate(() -> {
            try {
                logPerformanceStats();
            } catch (Exception e) {
                System.err.println("⚠️  Error en estadísticas: " + e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    private void maintainPool() {
        // Limpiar conexiones viejas o inválidas
        int cleaned = 0;
        long now = System.currentTimeMillis();
        
        for (Connection conn : connectionLastUsed.keySet()) {
            try {
                Long lastUsed = connectionLastUsed.get(conn);
                if (lastUsed != null && (now - lastUsed) > 300000) { // 5 minutos
                    if (conn.isClosed() || !conn.isValid(1)) {
                        connectionLastUsed.remove(conn);
                        cleaned++;
                    }
                }
            } catch (SQLException e) {
                connectionLastUsed.remove(conn);
                cleaned++;
            }
        }
        
        if (cleaned > 0) {
            System.out.println("🧹 Limpieza del pool: " + cleaned + " conexiones inválidas removidas");
        }
        
        // Asegurar conexiones mínimas
        int current = currentConnections.get();
        if (current < minPoolSize) {
            int toCreate = minPoolSize - current;
            System.out.println("📈 Creando " + toCreate + " conexiones adicionales...");
            
            for (int i = 0; i < toCreate; i++) {
                try {
                    Connection conn = createOptimizedConnection();
                    if (conn != null) {
                        connectionPool.offer(conn);
                        currentConnections.incrementAndGet();
                        totalConnectionsCreated.incrementAndGet();
                        connectionLastUsed.put(conn, System.currentTimeMillis());
                    }
                } catch (SQLException e) {
                    System.err.println("⚠️  Error creando conexión de mantenimiento: " + e.getMessage());
                }
            }
        }
    }
    
    private void logPerformanceStats() {
        int current = currentConnections.get();
        int available = connectionPool.size();
        int inUse = current - available;
        long totalRequests = totalRequestsServed.get();
        int peak = peakConcurrentConnections.get();
        
        System.out.println("📊 === ESTADÍSTICAS DE RENDIMIENTO ===");
        System.out.println("   🔗 Conexiones: " + current + " total, " + available + " disponibles, " + inUse + " en uso");
        System.out.println("   📈 Pico concurrente: " + peak + " conexiones");
        System.out.println("   📋 Peticiones servidas: " + String.format("%,d", totalRequests));
        System.out.println("   🏭 Conexiones creadas: " + totalConnectionsCreated.get());
        System.out.println("   ⚡ Servicio: " + (isServiceActive.get() ? "SÚPER ACTIVO" : "INACTIVO"));
        System.out.println("=====================================");
    }
    
    private void checkServiceStatus() {
        try (Connection testConn = createOptimizedConnection()) {
            // Si llegamos aquí, la conexión es exitosa
            isServiceActive.set(true);
            System.out.println("✅ Servicio de base de datos SÚPER ACTIVO");
        } catch (SQLException e) {
            isServiceActive.set(false);
            System.err.println("❌ Servicio de base de datos inactivo: " + e.getMessage());
        }
    }
    
    /**
     * OBTIENE CONEXIÓN SÚPER OPTIMIZADA para procesamiento masivo
     */
    public Connection getConnection() throws SQLException {
        totalRequestsServed.incrementAndGet();
        
        // Si el servicio está marcado como inactivo, intentar reactivarlo
        if (!isServiceActive.get()) {
            System.out.println("🔄 Intentando reactivar servicio de base de datos...");
            checkServiceStatus();
        }
        
        // Si sigue inactivo, lanzar excepción específica
        if (!isServiceActive.get()) {
            throw new SQLException("SERVICIO_INACTIVO");
        }
        
        Connection conn = null;
        int attempts = 0;
        int delay = retryDelayMs;
        
        while (attempts < maxRetryAttempts && conn == null) {
            attempts++;
            
            try {
                // ESTRATEGIA SÚPER AGRESIVA: Intentar obtener conexión inmediatamente
                conn = connectionPool.poll(poolTimeout, TimeUnit.MILLISECONDS);
                
                if (conn == null || conn.isClosed()) {
                    // Si no hay conexiones disponibles, crear nueva INMEDIATAMENTE
                    if (currentConnections.get() < maxPoolSize) {
                        conn = createOptimizedConnection();
                        int newTotal = currentConnections.incrementAndGet();
                        totalConnectionsCreated.incrementAndGet();
                        
                        // Actualizar pico si es necesario
                        int currentPeak = peakConcurrentConnections.get();
                        if (newTotal > currentPeak) {
                            peakConcurrentConnections.compareAndSet(currentPeak, newTotal);
                        }
                        
                        System.out.println("⚡ Nueva conexión SÚPER RÁPIDA creada (total: " + newTotal + ")");
                    } else {
                        // Pool agotado, esperar MUY POCO tiempo
                        conn = connectionPool.poll(50, TimeUnit.MILLISECONDS);
                        if (conn == null) {
                            throw new SQLException("Pool temporalmente agotado - reintentando");
                        }
                    }
                }
                
                // Verificar que la conexión esté válida SÚPER RÁPIDO
                if (conn != null && !conn.isClosed() && conn.isValid(1)) {
                    connectionLastUsed.put(conn, System.currentTimeMillis());
                    return conn;
                } else {
                    // Conexión inválida, descartar y reintentar
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                    connectionLastUsed.remove(conn);
                    currentConnections.decrementAndGet();
                    conn = null;
                }
                
            } catch (Exception e) {
                if (attempts < maxRetryAttempts) {
                    try {
                        Thread.sleep(delay);
                        delay = Math.min((int) (delay * backoffMultiplier), 200); // Máximo 200ms
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrumpido durante reintento", ie);
                    }
                } else {
                    System.err.println("❌ Todos los intentos fallaron: " + e.getMessage());
                }
            }
        }
        
        // Si llegamos aquí, todos los intentos fallaron
        throw new SQLException("No se pudo obtener conexión después de " + maxRetryAttempts + " intentos");
    }
    
    /**
     * DEVUELVE CONEXIÓN SÚPER OPTIMIZADA
     */
    public void returnConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(1)) {
                    connectionLastUsed.put(conn, System.currentTimeMillis());
                    connectionPool.offer(conn);
                } else {
                    conn.close();
                    connectionLastUsed.remove(conn);
                    currentConnections.decrementAndGet();
                }
            } catch (SQLException e) {
                System.err.println("⚠️  Error devolviendo conexión al pool: " + e.getMessage());
                connectionLastUsed.remove(conn);
                currentConnections.decrementAndGet();
            }
        }
    }
    
    /**
     * Cierra una conexión y la remueve del pool
     */
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                connectionLastUsed.remove(conn);
                currentConnections.decrementAndGet();
            } catch (SQLException e) {
                System.err.println("⚠️  Error cerrando conexión: " + e.getMessage());
            }
        }
    }
    
    /**
     * Verifica si el servicio está activo
     */
    public boolean isServiceActive() {
        return isServiceActive.get();
    }
    
    /**
     * Obtiene estadísticas SÚPER DETALLADAS del pool
     */
    public String getPoolStats() {
        int current = currentConnections.get();
        int available = connectionPool.size();
        int inUse = current - available;
        long totalRequests = totalRequestsServed.get();
        int peak = peakConcurrentConnections.get();
        
        return String.format("SÚPER POOL: %d/%d conexiones (%d en uso), %,d peticiones servidas, pico: %d, Servicio: %s", 
                           available, current, inUse, totalRequests, peak,
                           isServiceActive.get() ? "SÚPER ACTIVO" : "INACTIVO");
    }
    
    /**
     * Cierra todas las conexiones del pool
     */
    public void shutdown() {
        System.out.println("🔄 Cerrando pool SÚPER OPTIMIZADO...");
        
        // Detener tareas de mantenimiento
        maintenanceExecutor.shutdown();
        try {
            if (!maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenanceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            maintenanceExecutor.shutdownNow();
        }
        
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("⚠️  Error cerrando conexión durante shutdown: " + e.getMessage());
            }
        }
        
        connectionLastUsed.clear();
        currentConnections.set(0);
        isServiceActive.set(false);
        System.out.println("✅ Pool SÚPER OPTIMIZADO cerrado");
    }
} 