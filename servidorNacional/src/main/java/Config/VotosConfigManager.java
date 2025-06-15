package Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestor de configuración para la Base de Datos de Votos
 * Maneja la conexión a la segunda base de datos dedicada a votos y candidatos
 */
public class VotosConfigManager {
    private static VotosConfigManager instance;
    private Properties properties;
    private static final String CONFIG_FILE = "/votos.cfg";
    
    private VotosConfigManager() {
        loadConfiguration();
    }
    
    public static synchronized VotosConfigManager getInstance() {
        if (instance == null) {
            instance = new VotosConfigManager();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        properties = new Properties();
        
        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("❌ No se pudo encontrar el archivo de configuración: " + CONFIG_FILE);
                System.err.println("   Usando valores por defecto para DB de votos");
                loadDefaultProperties();
                return;
            }
            
            properties.load(input);
            System.out.println("✅ Configuración de votos cargada desde: " + CONFIG_FILE);
            
        } catch (IOException e) {
            System.err.println("❌ Error cargando configuración de votos: " + e.getMessage());
            System.err.println("   Usando valores por defecto");
            loadDefaultProperties();
        }
    }
    
    private void loadDefaultProperties() {
        // Valores por defecto para base de datos de votos
        properties.setProperty("votos.db.host", "10.147.10.101");
        properties.setProperty("votos.db.port", "5432");
        properties.setProperty("votos.db.name", "votos_elecciones_grajj");
        properties.setProperty("votos.db.user", "votaciones_grajj");
        properties.setProperty("votos.db.password", "votaciones_grajj");
        
        // Pool de conexiones por defecto para votos
        properties.setProperty("votos.db.pool.minSize", "5");
        properties.setProperty("votos.db.pool.maxSize", "50");
        properties.setProperty("votos.db.pool.timeout", "30000");
        
        // Reintentos por defecto
        properties.setProperty("votos.db.retry.maxAttempts", "3");
        properties.setProperty("votos.db.retry.delayMs", "2000");
        properties.setProperty("votos.db.retry.backoffMultiplier", "2.0");
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
    
    // ========== MÉTODOS ESPECÍFICOS PARA BASE DE DATOS DE VOTOS ==========
    
    public String getVotosDatabaseUrl() {
        String host = getProperty("votos.db.host", "10.147.10.101");
        int port = getIntProperty("votos.db.port", 5432);
        String dbName = getProperty("votos.db.name", "votos_elecciones_grajj");
        return "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    }
    
    public String getVotosDatabaseUser() {
        return getProperty("votos.db.user", "votaciones_grajj");
    }
    
    public String getVotosDatabasePassword() {
        return getProperty("votos.db.password", "votaciones_grajj");
    }
    
    public int getVotosPoolMinSize() {
        return getIntProperty("votos.db.pool.minSize", 5);
    }
    
    public int getVotosPoolMaxSize() {
        return getIntProperty("votos.db.pool.maxSize", 50);
    }
    
    public int getVotosPoolTimeout() {
        return getIntProperty("votos.db.pool.timeout", 30000);
    }
    
    // ========== MÉTODOS ESPECÍFICOS PARA REINTENTOS ==========
    
    public int getVotosRetryMaxAttempts() {
        return getIntProperty("votos.db.retry.maxAttempts", 3);
    }
    
    public int getVotosRetryDelayMs() {
        return getIntProperty("votos.db.retry.delayMs", 2000);
    }
    
    public double getVotosRetryBackoffMultiplier() {
        return getDoubleProperty("votos.db.retry.backoffMultiplier", 2.0);
    }
    
    // ========== MÉTODOS DE UTILIDAD ==========
    
    public void printConfiguration() {
        System.out.println("📋 ===== CONFIGURACIÓN BASE DE DATOS DE VOTOS =====");
        System.out.println("🗄️  Base de datos de votos:");
        System.out.println("   URL: " + getVotosDatabaseUrl());
        System.out.println("   Usuario: " + getVotosDatabaseUser());
        System.out.println("   Pool: " + getVotosPoolMinSize() + "-" + getVotosPoolMaxSize() + " conexiones");
        System.out.println("🔄 Reintentos:");
        System.out.println("   Máximo: " + getVotosRetryMaxAttempts() + " intentos");
        System.out.println("   Delay: " + getVotosRetryDelayMs() + "ms");
        System.out.println("⏱️  Timeout Pool: " + getVotosPoolTimeout() + "ms");
        System.out.println("=================================================");
    }
} 