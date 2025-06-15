package Config;

import com.zeroc.Ice.Properties;
import com.zeroc.Ice.Util;

/**
 * 🔧 Configuración para ReliableMessageQueue
 * Lee propiedades desde archivos de configuración de Ice
 */
public class ReliableQueueConfig {
    
    // Valores por defecto
    private static final String DEFAULT_BASE_DIR = "reliable_queue";
    private static final int DEFAULT_PROCESSING_INTERVAL = 5000;
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_RETRY_DELAY = 10000;
    private static final int DEFAULT_PROCESSING_THREADS = 5;
    private static final int DEFAULT_SCHEDULER_THREADS = 2;
    private static final boolean DEFAULT_PERSISTENCE_ENABLED = true;
    private static final boolean DEFAULT_AUTO_CLEANUP = true;
    private static final int DEFAULT_CLEANUP_INTERVAL = 3600000; // 1 hora
    private static final int DEFAULT_MAX_PROCESSED_FILES = 1000;
    private static final String DEFAULT_LOG_LEVEL = "INFO";
    private static final boolean DEFAULT_LOG_STATISTICS = true;
    private static final int DEFAULT_STATISTICS_INTERVAL = 30000; // 30 segundos
    
    // Propiedades de configuración
    private final String baseDir;
    private final int processingInterval;
    private final int batchSize;
    private final int maxRetries;
    private final int retryDelay;
    private final int processingThreads;
    private final int schedulerThreads;
    private final boolean persistenceEnabled;
    private final boolean autoCleanup;
    private final int cleanupInterval;
    private final int maxProcessedFiles;
    private final String logLevel;
    private final boolean logStatistics;
    private final int statisticsInterval;
    
    // Propiedades de base de datos
    private final int dbConnectionTimeout;
    private final int dbMaxRetries;
    private final int dbRetryInterval;
    
    public ReliableQueueConfig() {
        this(null);
    }
    
    public ReliableQueueConfig(Properties properties) {
        if (properties == null) {
            // Intentar obtener propiedades del contexto actual de Ice
            try {
                properties = Util.createProperties();
            } catch (Exception e) {
                System.err.println("⚠️ No se pudieron cargar propiedades de Ice, usando valores por defecto");
                properties = null;
            }
        }
        
        // Cargar configuración del ReliableMessageQueue
        this.baseDir = getProperty(properties, "ReliableQueue.BaseDir", DEFAULT_BASE_DIR);
        this.processingInterval = getIntProperty(properties, "ReliableQueue.ProcessingInterval", DEFAULT_PROCESSING_INTERVAL);
        this.batchSize = getIntProperty(properties, "ReliableQueue.BatchSize", DEFAULT_BATCH_SIZE);
        this.maxRetries = getIntProperty(properties, "ReliableQueue.MaxRetries", DEFAULT_MAX_RETRIES);
        this.retryDelay = getIntProperty(properties, "ReliableQueue.RetryDelay", DEFAULT_RETRY_DELAY);
        this.processingThreads = getIntProperty(properties, "ReliableQueue.ProcessingThreads", DEFAULT_PROCESSING_THREADS);
        this.schedulerThreads = getIntProperty(properties, "ReliableQueue.SchedulerThreads", DEFAULT_SCHEDULER_THREADS);
        this.persistenceEnabled = getBooleanProperty(properties, "ReliableQueue.PersistenceEnabled", DEFAULT_PERSISTENCE_ENABLED);
        this.autoCleanup = getBooleanProperty(properties, "ReliableQueue.AutoCleanup", DEFAULT_AUTO_CLEANUP);
        this.cleanupInterval = getIntProperty(properties, "ReliableQueue.CleanupInterval", DEFAULT_CLEANUP_INTERVAL);
        this.maxProcessedFiles = getIntProperty(properties, "ReliableQueue.MaxProcessedFiles", DEFAULT_MAX_PROCESSED_FILES);
        this.logLevel = getProperty(properties, "ReliableQueue.LogLevel", DEFAULT_LOG_LEVEL);
        this.logStatistics = getBooleanProperty(properties, "ReliableQueue.LogStatistics", DEFAULT_LOG_STATISTICS);
        this.statisticsInterval = getIntProperty(properties, "ReliableQueue.StatisticsInterval", DEFAULT_STATISTICS_INTERVAL);
        
        // Cargar configuración de base de datos
        this.dbConnectionTimeout = getIntProperty(properties, "VotosDB.ConnectionTimeout", 5000);
        this.dbMaxRetries = getIntProperty(properties, "VotosDB.MaxRetries", 3);
        this.dbRetryInterval = getIntProperty(properties, "VotosDB.RetryInterval", 2000);
        
        System.out.println("🔧 ReliableQueueConfig inicializada:");
        System.out.println("   📁 Base Dir: " + baseDir);
        System.out.println("   ⏱️ Processing Interval: " + processingInterval + "ms");
        System.out.println("   📦 Batch Size: " + batchSize);
        System.out.println("   🔄 Max Retries: " + maxRetries);
        System.out.println("   🧵 Processing Threads: " + processingThreads);
        System.out.println("   💾 Persistence: " + (persistenceEnabled ? "Enabled" : "Disabled"));
        System.out.println("   🧹 Auto Cleanup: " + (autoCleanup ? "Enabled" : "Disabled"));
    }
    
    private String getProperty(Properties properties, String key, String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        return properties.getPropertyWithDefault(key, defaultValue);
    }
    
    private int getIntProperty(Properties properties, String key, int defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        return properties.getPropertyAsIntWithDefault(key, defaultValue);
    }
    
    private boolean getBooleanProperty(Properties properties, String key, boolean defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        String value = properties.getPropertyWithDefault(key, String.valueOf(defaultValue));
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }
    
    // Getters
    public String getBaseDir() { return baseDir; }
    public int getProcessingInterval() { return processingInterval; }
    public int getBatchSize() { return batchSize; }
    public int getMaxRetries() { return maxRetries; }
    public int getRetryDelay() { return retryDelay; }
    public int getProcessingThreads() { return processingThreads; }
    public int getSchedulerThreads() { return schedulerThreads; }
    public boolean isPersistenceEnabled() { return persistenceEnabled; }
    public boolean isAutoCleanup() { return autoCleanup; }
    public int getCleanupInterval() { return cleanupInterval; }
    public int getMaxProcessedFiles() { return maxProcessedFiles; }
    public String getLogLevel() { return logLevel; }
    public boolean isLogStatistics() { return logStatistics; }
    public int getStatisticsInterval() { return statisticsInterval; }
    public int getDbConnectionTimeout() { return dbConnectionTimeout; }
    public int getDbMaxRetries() { return dbMaxRetries; }
    public int getDbRetryInterval() { return dbRetryInterval; }
    
    @Override
    public String toString() {
        return "ReliableQueueConfig{" +
                "baseDir='" + baseDir + '\'' +
                ", processingInterval=" + processingInterval +
                ", batchSize=" + batchSize +
                ", maxRetries=" + maxRetries +
                ", processingThreads=" + processingThreads +
                ", persistenceEnabled=" + persistenceEnabled +
                ", autoCleanup=" + autoCleanup +
                '}';
    }
} 