package Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestor de configuración para el Servidor Nacional
 * Carga y proporciona acceso a las propiedades de configuración
 */
public class ConfigManager implements IConfig {
    private static ConfigManager instance;
    private Properties properties;
    private static final String CONFIG_FILE = "/servidorNacional.cfg";
    
    private ConfigManager() {
        loadConfiguration();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        properties = new Properties();
        
        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("❌ No se pudo encontrar el archivo de configuración: " + CONFIG_FILE);
                System.err.println("   Usando valores por defecto");
                loadDefaultProperties();
                return;
            }
            
            properties.load(input);
            System.out.println("✅ Configuración cargada desde: " + CONFIG_FILE);
            
        } catch (IOException e) {
            System.err.println("❌ Error cargando configuración: " + e.getMessage());
            System.err.println("   Usando valores por defecto");
            loadDefaultProperties();
        }
    }
    
    private void loadDefaultProperties() {
        // Valores por defecto para base de datos (usando la configuración de cfg)
        properties.setProperty("db.host", "10.147.17.101");
        properties.setProperty("db.port", "5432");
        properties.setProperty("db.name", "votos_elecciones_grajj");
        properties.setProperty("db.user", "votaciones_grajj");
        properties.setProperty("db.password", "votaciones_grajj");
        
        // Pool de conexiones por defecto
        properties.setProperty("db.pool.minSize", "5");
        properties.setProperty("db.pool.maxSize", "50");
        properties.setProperty("db.pool.timeout", "30000");
        
        // Reintentos por defecto
        properties.setProperty("db.retry.maxAttempts", "3");
        properties.setProperty("db.retry.delayMs", "2000");
        properties.setProperty("db.retry.backoffMultiplier", "2.0");
        
        // ConsultaMesa por defecto
        properties.setProperty("consultaMesa.queryTimeout", "15000");
        properties.setProperty("consultaMesa.serviceInactiveMessage", 
            "🚫 SERVICIO TEMPORALMENTE INACTIVO\\n   📞 Contacte a soporte técnico");
    }
    
    // ========== MÉTODOS DE ACCESO A CONFIGURACIÓN ==========
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Valor inválido para " + key + ": " + properties.getProperty(key));
            return defaultValue;
        }
    }
    
    public double getDoubleProperty(String key, double defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Valor inválido para " + key + ": " + properties.getProperty(key));
            return defaultValue;
        }
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    // ========== MÉTODOS ESPECÍFICOS PARA BASE DE DATOS ==========
    
    public String getDatabaseUrl() {
        String host = getProperty("db.host", "10.147.17.101");
        int port = getIntProperty("db.port", 5432);
        String dbName = getProperty("db.name", "votos_elecciones_grajj");
        return "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    }
    
    public String getDatabaseUser() {
        return getProperty("db.user", "votaciones_grajj");
    }
    
    public String getDatabasePassword() {
        return getProperty("db.password", "votaciones_grajj");
    }
    
    public int getPoolMinSize() {
        return getIntProperty("db.pool.minSize", 5);
    }
    
    public int getPoolMaxSize() {
        return getIntProperty("db.pool.maxSize", 50);
    }
    
    public int getPoolTimeout() {
        return getIntProperty("db.pool.timeout", 30000);
    }
    
    // ========== MÉTODOS ESPECÍFICOS PARA REINTENTOS ==========
    
    public int getRetryMaxAttempts() {
        return getIntProperty("db.retry.maxAttempts", 3);
    }
    
    public int getRetryDelayMs() {
        return getIntProperty("db.retry.delayMs", 2000);
    }
    
    public double getRetryBackoffMultiplier() {
        return getDoubleProperty("db.retry.backoffMultiplier", 2.0);
    }
    
    // ========== MÉTODOS ESPECÍFICOS PARA CONSULTA MESA ==========
    
    public int getQueryTimeout() {
        return getIntProperty("consultaMesa.queryTimeout", 15000);
    }
    
    public String getServiceInactiveMessage() {
        String message = getProperty("consultaMesa.serviceInactiveMessage", 
            "🚫 SERVICIO TEMPORALMENTE INACTIVO\\n   📞 Contacte a soporte técnico");
        // Reemplazar \\n con saltos de línea reales
        return message.replace("\\n", "\n");
    }
    
    // ========== MÉTODOS DE UTILIDAD ==========
    
    public void printConfiguration() {
        System.out.println("📋 ===== CONFIGURACIÓN SERVIDOR NACIONAL =====");
        System.out.println("🗄️  Base de datos:");
        System.out.println("   URL: " + getDatabaseUrl());
        System.out.println("   Usuario: " + getDatabaseUser());
        System.out.println("   Pool: " + getPoolMinSize() + "-" + getPoolMaxSize() + " conexiones");
        System.out.println("🔄 Reintentos:");
        System.out.println("   Máximo: " + getRetryMaxAttempts() + " intentos");
        System.out.println("   Delay: " + getRetryDelayMs() + "ms");
        System.out.println("⏱️  Timeouts:");
        System.out.println("   Query: " + getQueryTimeout() + "ms");
        System.out.println("   Pool: " + getPoolTimeout() + "ms");
        System.out.println("===============================================");
    }
} 