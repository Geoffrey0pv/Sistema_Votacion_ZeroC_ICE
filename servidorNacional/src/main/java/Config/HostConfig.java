package Config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Configuración centralizada de hosts y networking
 * Reemplaza todas las referencias hardcodeadas a "10.147.17.113"
 */
public class HostConfig {
    private static HostConfig instance;
    private Properties properties;
    
    private HostConfig() {
        loadConfiguration();
    }
    
    public static synchronized HostConfig getInstance() {
        if (instance == null) {
            instance = new HostConfig();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        properties = new Properties();
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("hosts.cfg");
            if (is != null) {
                properties.load(is);
                is.close();
                System.out.println("✅ Configuración de hosts cargada desde hosts.cfg");
            } else {
                System.err.println("⚠️ No se encontró hosts.cfg, usando valores por defecto");
                loadDefaultProperties();
            }
        } catch (Exception e) {
            System.err.println("❌ Error cargando configuración de hosts: " + e.getMessage());
            loadDefaultProperties();
        }
    }
    
    private void loadDefaultProperties() {
        // Valores por defecto en caso de que no se encuentre el archivo
        properties.setProperty("nacional.host", "10.147.17.113");
        properties.setProperty("nacional.port", "9090");
        properties.setProperty("regional.host", "10.147.17.113");
        properties.setProperty("regional.port", "8080");
        properties.setProperty("replica.host", "10.147.17.113");
        properties.setProperty("replica.base_port", "10000");
        properties.setProperty("ice.locator.host", "10.147.17.113");
        properties.setProperty("ice.locator.port", "4061");
        properties.setProperty("cluster.seeds", "10.147.17.113:7947");
        properties.setProperty("network.local.hostname", "10.147.17.113");
        properties.setProperty("broker.nacional.host", "10.147.17.113");
        properties.setProperty("broker.nacional.port", "9090");
        properties.setProperty("broker.nacional.endpoint", "tcp -h 10.147.17.113 -p 9090");
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN NACIONAL =====
    public String getNacionalHost() {
        return properties.getProperty("nacional.host", "10.147.17.113");
    }
    
    public int getNacionalPort() {
        return Integer.parseInt(properties.getProperty("nacional.port", "9090"));
    }
    
    public String getNacionalEndpoint() {
        return String.format("tcp -h %s -p %d", getNacionalHost(), getNacionalPort());
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN REGIONAL =====
    public String getRegionalHost() {
        return properties.getProperty("regional.host", "10.147.17.113");
    }
    
    public int getRegionalPort() {
        return Integer.parseInt(properties.getProperty("regional.port", "8080"));
    }
    
    public String getRegionalEndpoint() {
        return String.format("tcp -h %s -p %d", getRegionalHost(), getRegionalPort());
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN DE RÉPLICAS =====
    public String getReplicaHost() {
        return properties.getProperty("replica.host", "10.147.17.113");
    }
    
    public int getReplicaBasePort() {
        return Integer.parseInt(properties.getProperty("replica.base_port", "10000"));
    }
    
    public String getReplicaEndpoint(int port) {
        return String.format("tcp -h %s -p %d", getReplicaHost(), port);
    }
    
    public int getMaxReplicas() {
        return Integer.parseInt(properties.getProperty("replica.max_replicas", "10"));
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN DE ICE GRID =====
    public String getIceLocatorHost() {
        return properties.getProperty("ice.locator.host", "10.147.17.113");
    }
    
    public int getIceLocatorPort() {
        return Integer.parseInt(properties.getProperty("ice.locator.port", "4061"));
    }
    
    public String getIceLocatorEndpoint() {
        return String.format("DemoIceGrid/Locator:default -h %s -p %d", 
            getIceLocatorHost(), getIceLocatorPort());
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN DE CLUSTER =====
    public String getClusterSeeds() {
        return properties.getProperty("cluster.seeds", "10.147.17.113:7947");
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN DE RED =====
    public String getNetworkLocalHostname() {
        return properties.getProperty("network.local.hostname", "10.147.17.113");
    }
    
    public String getNetworkLocalIP() {
        return properties.getProperty("network.local.ip", "127.0.0.1");
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN DE BROKER =====
    public String getBrokerNacionalHost() {
        return properties.getProperty("broker.nacional.host", "10.147.17.113");
    }
    
    public int getBrokerNacionalPort() {
        return Integer.parseInt(properties.getProperty("broker.nacional.port", "9090"));
    }
    
    public String getBrokerNacionalEndpoint() {
        return properties.getProperty("broker.nacional.endpoint", 
            String.format("tcp -h %s -p %d", getBrokerNacionalHost(), getBrokerNacionalPort()));
    }
    
    public String getBrokerRegionalHost() {
        return properties.getProperty("broker.regional.host", "10.147.17.113");
    }
    
    public int getBrokerRegionalPort() {
        return Integer.parseInt(properties.getProperty("broker.regional.port", "8080"));
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN DE DESARROLLO =====
    public String getDevNacionalHost() {
        return properties.getProperty("dev.nacional.host", "10.147.17.113");
    }
    
    public String getDevRegionalHost() {
        return properties.getProperty("dev.regional.host", "10.147.17.113");
    }
    
    public String getDevClusterSeeds() {
        return properties.getProperty("dev.cluster.seeds", "10.147.17.113:7947");
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN DE ADAPTADORES =====
    public String getNacionalAdapterEndpoints() {
        return properties.getProperty("nacional.adapter.endpoints", 
            String.format("tcp -h %s -p %d", getNacionalHost(), getNacionalPort()));
    }
    
    public String getReplicaAdapterEndpointPattern() {
        return properties.getProperty("replica.adapter.endpoint.pattern", 
            "tcp -h " + getReplicaHost() + " -p %PORT%");
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN DE BASE DE DATOS =====
    public String getDatabaseHost() {
        return properties.getProperty("database.host", "10.147.17.113");
    }
    
    public int getDatabasePort() {
        return Integer.parseInt(properties.getProperty("database.port", "5432"));
    }
    
    // ===== MÉTODOS PARA CONFIGURACIÓN DE MONITOREO =====
    public String getMonitoringHost() {
        return properties.getProperty("monitoring.host", "10.147.17.113");
    }
    
    public int getMonitoringPort() {
        return Integer.parseInt(properties.getProperty("monitoring.port", "8081"));
    }
    
    // ===== MÉTODO GENÉRICO =====
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    // ===== MÉTODO PARA RECARGAR CONFIGURACIÓN =====
    public void reload() {
        loadConfiguration();
        System.out.println("🔄 Configuración de hosts recargada");
    }
    
    // ===== MÉTODO PARA MOSTRAR CONFIGURACIÓN =====
    public void printConfiguration() {
        System.out.println("📋 ===== CONFIGURACIÓN DE HOSTS =====");
        System.out.println("   🏛️ Nacional: " + getNacionalEndpoint());
        System.out.println("   🏢 Regional: " + getRegionalEndpoint());
        System.out.println("   🔄 Réplicas: " + getReplicaHost() + ":" + getReplicaBasePort() + "+");
        System.out.println("   🧊 Ice Locator: " + getIceLocatorEndpoint());
        System.out.println("   🔗 Cluster Seeds: " + getClusterSeeds());
        System.out.println("   🌐 Network Hostname: " + getNetworkLocalHostname());
        System.out.println("   📡 Broker Nacional: " + getBrokerNacionalEndpoint());
        System.out.println("=====================================");
    }
} 