package Database;

import Config.VotosConfigManager;
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
 * Pool de conexiones optimizado para la Base de Datos de Votos y Candidatos
 * Maneja conexiones independientes para operaciones de votación
 */
public class VotosConnectionPool {
    private static VotosConnectionPool instance;
    private final BlockingQueue<Connection> connectionPool;
    private final AtomicInteger currentConnections;
    private final AtomicBoolean isServiceActive;
    private final VotosConfigManager config;
    
    // CONFIGURACIÓN OPTIMIZADA
    private final int minPoolSize;
    private final int maxPoolSize;
    private final int poolTimeout;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    
    // MÉTRICAS Y OPTIMIZACIONES
    private final AtomicLong totalRequestsServed;
    private final AtomicLong totalConnectionsCreated;
    private final AtomicInteger peakConcurrentConnections;
    private final ScheduledExecutorService maintenanceExecutor;
    private final ConcurrentHashMap<Connection, Long> connectionLastUsed;
    
    // Configuración de reintentos
    private final int maxRetryAttempts;
    private final int retryDelayMs;
    private final double backoffMultiplier;
    
    private VotosConnectionPool() {
        this.config = VotosConfigManager.getInstance();
        this.connectionPool = new LinkedBlockingQueue<>();
        this.currentConnections = new AtomicInteger(0);
        this.isServiceActive = new AtomicBoolean(false);
        
        // CONFIGURACIÓN ESPECÍFICA PARA VOTOS
        this.minPoolSize = config.getVotosPoolMinSize();
        this.maxPoolSize = config.getVotosPoolMaxSize();
        this.poolTimeout = config.getVotosPoolTimeout();
        this.dbUrl = config.getVotosDatabaseUrl() + "?tcpKeepAlive=true&socketTimeout=30000&loginTimeout=10";
        this.dbUser = config.getVotosDatabaseUser();
        this.dbPassword = config.getVotosDatabasePassword();
        
        this.maxRetryAttempts = config.getVotosRetryMaxAttempts();
        this.retryDelayMs = config.getVotosRetryDelayMs();
        this.backoffMultiplier = config.getVotosRetryBackoffMultiplier();
        
        // MÉTRICAS
        this.totalRequestsServed = new AtomicLong(0);
        this.totalConnectionsCreated = new AtomicLong(0);
        this.peakConcurrentConnections = new AtomicInteger(0);
        this.connectionLastUsed = new ConcurrentHashMap<>();
        this.maintenanceExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Cargar driver PostgreSQL
        loadPostgreSQLDriver();
        
        // Inicializar pool
        initializePool();
        
        // Iniciar mantenimiento automático
        startMaintenanceTasks();
        
        System.out.println("🗳️  POOL DE VOTOS INICIADO:");
        System.out.println("   📊 Pool: " + minPoolSize + "-" + maxPoolSize + " conexiones");
        System.out.println("   ⚡ Timeout: " + poolTimeout + "ms");
        System.out.println("   🔗 URL: " + config.getVotosDatabaseUrl());
    }
    
    public static synchronized VotosConnectionPool getInstance() {
        if (instance == null) {
            instance = new VotosConnectionPool();
        }
        return instance;
    }
    
    private void loadPostgreSQLDriver() {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Driver PostgreSQL cargado para DB de votos");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Error cargando driver PostgreSQL para votos: " + e.getMessage());
            System.err.println("   Asegúrese de que postgresql.jar esté en el classpath");
        }
    }
    
    private void initializePool() {
        System.out.println("🗳️  Inicializando pool de votos...");
        System.out.println("   URL: " + dbUrl);
        System.out.println("   Pool: " + minPoolSize + "-" + maxPoolSize + " conexiones");
        
        // Crear conexiones iniciales
        for (int i = 0; i < minPoolSize; i++) {
            try {
                Connection conn = createVotosConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                    currentConnections.incrementAndGet();
                    totalConnectionsCreated.incrementAndGet();
                    connectionLastUsed.put(conn, System.currentTimeMillis());
                }
            } catch (Exception e) {
                System.err.println("⚠️  No se pudo crear conexión de votos " + (i + 1) + ": " + e.getMessage());
            }
        }
        
        // Pre-calentar conexiones
        warmUpVotosConnections();
        
        // Verificar si el servicio está activo
        checkServiceStatus();
        
        if (isServiceActive.get()) {
            System.out.println("✅ Pool de votos inicializado: " + currentConnections.get() + " conexiones");
            System.out.println("🗳️  LISTO PARA OPERACIONES DE VOTACIÓN");
        } else {
            System.err.println("❌ Pool de votos no pudo inicializarse completamente");
        }
    }
    
    private Connection createVotosConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        
        // OPTIMIZACIONES PARA VOTOS
        conn.setAutoCommit(false); // Transacciones para votos
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        
        // Configurar timeouts
        try (PreparedStatement stmt = conn.prepareStatement("SET statement_timeout = '30s'")) {
            stmt.execute();
        }
        try (PreparedStatement stmt = conn.prepareStatement("SET lock_timeout = '10s'")) {
            stmt.execute();
        }
        
        return conn;
    }
    
    private void warmUpVotosConnections() {
        System.out.println("🔥 Pre-calentando conexiones de votos...");
        
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
                System.err.println("⚠️  Error pre-calentando conexión de votos " + (i + 1));
            }
        }
        
        // Devolver todas las conexiones al pool
        for (int i = 0; i < count; i++) {
            if (connections[i] != null) {
                connectionPool.offer(connections[i]);
            }
        }
        
        System.out.println("✅ " + count + " conexiones de votos pre-calentadas");
    }
    
    private void startMaintenanceTasks() {
        // Tarea de mantenimiento cada 30 segundos
        maintenanceExecutor.scheduleAtFixedRate(() -> {
            try {
                maintainPool();
                logPerformanceStats();
            } catch (Exception e) {
                System.err.println("⚠️  Error en mantenimiento del pool de votos: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    private void maintainPool() {
        // Eliminar conexiones viejas y crear nuevas si es necesario
        long now = System.currentTimeMillis();
        long maxAge = 300000; // 5 minutos
        
        connectionLastUsed.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > maxAge) {
                try {
                    entry.getKey().close();
                    currentConnections.decrementAndGet();
                } catch (SQLException e) {
                    // Ignorar errores al cerrar conexiones viejas
                }
                return true;
            }
            return false;
        });
        
        // Asegurar número mínimo de conexiones
        while (currentConnections.get() < minPoolSize) {
            try {
                Connection conn = createVotosConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                    currentConnections.incrementAndGet();
                    totalConnectionsCreated.incrementAndGet();
                    connectionLastUsed.put(conn, now);
                }
            } catch (SQLException e) {
                break; // No crear más si hay errores
            }
        }
    }
    
    private void logPerformanceStats() {
        int peak = peakConcurrentConnections.get();
        System.out.println("📊 STATS POOL VOTOS - Activas: " + currentConnections.get() + 
                         ", Pico: " + peak + ", Total servidas: " + totalRequestsServed.get());
    }
    
    private void checkServiceStatus() {
        try (Connection testConn = createVotosConnection()) {
            if (testConn != null && !testConn.isClosed()) {
                isServiceActive.set(true);
            }
        } catch (SQLException e) {
            isServiceActive.set(false);
            System.err.println("❌ Servicio de BD de votos inactivo: " + e.getMessage());
        }
    }
    
    public Connection getConnection() throws SQLException {
        if (!isServiceActive.get()) {
            throw new SQLException("SERVICIO_VOTOS_INACTIVO");
        }
        
        totalRequestsServed.incrementAndGet();
        
        // Intentar obtener conexión del pool
        Connection conn = connectionPool.poll();
        
        if (conn == null || conn.isClosed()) {
            // Crear nueva conexión si es necesario
            if (currentConnections.get() < maxPoolSize) {
                conn = createVotosConnection();
                currentConnections.incrementAndGet();
                totalConnectionsCreated.incrementAndGet();
            } else {
                // Esperar por una conexión disponible
                try {
                    conn = connectionPool.poll(poolTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Timeout esperando conexión de votos");
                }
            }
        }
        
        if (conn != null) {
            connectionLastUsed.put(conn, System.currentTimeMillis());
            
            // Actualizar pico de conexiones concurrentes
            int current = maxPoolSize - connectionPool.size();
            peakConcurrentConnections.updateAndGet(peak -> Math.max(peak, current));
        }
        
        return conn;
    }
    
    public void returnConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(5)) {
                    connectionLastUsed.put(conn, System.currentTimeMillis());
                    connectionPool.offer(conn);
                } else {
                    closeConnection(conn);
                }
            } catch (SQLException e) {
                closeConnection(conn);
            }
        }
    }
    
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                connectionLastUsed.remove(conn);
                currentConnections.decrementAndGet();
            } catch (SQLException e) {
                // Ignorar errores al cerrar
            }
        }
    }
    
    public boolean isServiceActive() {
        return isServiceActive.get();
    }
    
    public String getPoolStats() {
        return String.format("Pool Votos - Activas: %d/%d, En uso: %d, Total: %d, Pico: %d",
                currentConnections.get(), maxPoolSize,
                maxPoolSize - connectionPool.size(),
                totalRequestsServed.get(),
                peakConcurrentConnections.get());
    }
    
    public void shutdown() {
        isServiceActive.set(false);
        maintenanceExecutor.shutdown();
        
        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            closeConnection(conn);
        }
        
        System.out.println("🗳️  Pool de votos cerrado");
    }
} 