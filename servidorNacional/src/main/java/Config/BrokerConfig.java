package Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuración específica para el patrón Broker
 * Maneja configuración de réplicas, balanceadores de carga y escalado automático
 */
public class BrokerConfig implements IConfig {
    private static final Map<String, BrokerConfig> instances = new ConcurrentHashMap<>();
    private Properties properties;
    private final String configFile;
    private final String brokerType;
    private final HostConfig hostConfig;
    
    // Tipos de broker soportados
    public enum BrokerType {
        NACIONAL("nacional");
        
        private final String type;
        BrokerType(String type) { this.type = type; }
        public String getType() { return type; }
    }
    
    private BrokerConfig(String brokerType) {
        this.brokerType = brokerType;
        this.configFile = "/broker-" + brokerType + ".cfg";
        this.hostConfig = HostConfig.getInstance();
        loadConfiguration();
    }
    
    public static synchronized BrokerConfig getInstance(BrokerType type) {
        return instances.computeIfAbsent(type.getType(), 
            k -> new BrokerConfig(type.getType()));
    }
    
    public static BrokerConfig getNacionalInstance() {
        return getInstance(BrokerType.NACIONAL);
    }
    
    
    private void loadConfiguration() {
        properties = new Properties();
        
        try (InputStream input = getClass().getResourceAsStream(configFile)) {
            if (input == null) {
                System.err.println("❌ No se pudo encontrar: " + configFile);
                System.err.println("   Usando valores por defecto para broker " + brokerType);
                loadDefaultProperties();
                return;
            }
            
            properties.load(input);
            System.out.println("✅ Configuración broker cargada: " + configFile);
            
        } catch (IOException e) {
            System.err.println("❌ Error cargando configuración broker: " + e.getMessage());
            loadDefaultProperties();
        }
    }
    
    private void loadDefaultProperties() {
        if ("nacional".equals(brokerType)) {
            loadNacionalDefaults();
        } else if ("regional".equals(brokerType)) {
            loadRegionalDefaults();
        }
    }
    
    private void loadNacionalDefaults() {
        // Configuración de red usando HostConfig
        properties.setProperty("broker.host", hostConfig.getBrokerNacionalHost());
        properties.setProperty("broker.port", String.valueOf(hostConfig.getBrokerNacionalPort()));
        properties.setProperty("broker.endpoints", hostConfig.getBrokerNacionalEndpoint());
        
        // Configuración de réplicas
        properties.setProperty("replica.minReplicas", "1");
        properties.setProperty("replica.maxReplicas", String.valueOf(hostConfig.getMaxReplicas()));
        properties.setProperty("replica.basePort", String.valueOf(hostConfig.getReplicaBasePort()));
        properties.setProperty("replica.hostPattern", hostConfig.getReplicaHost());
        properties.setProperty("replica.autoScaling", "true");
        
        // Configuración de escalado
        properties.setProperty("scaling.cpuThreshold", "70.0");
        properties.setProperty("scaling.memoryThreshold", "80.0");
        properties.setProperty("scaling.scaleUpThreshold", "75.0");
        properties.setProperty("scaling.scaleDownThreshold", "25.0");
        properties.setProperty("scaling.evaluationInterval", "30000");
        properties.setProperty("scaling.cooldownPeriod", "60000");
        
        // Configuración de balanceador
        properties.setProperty("loadBalancer.algorithm", "ROUND_ROBIN");
        properties.setProperty("loadBalancer.healthCheckInterval", "15000");
        properties.setProperty("loadBalancer.maxFailures", "3");
        properties.setProperty("loadBalancer.failureTimeout", "30000");
        
        // Configuración de monitoreo
        properties.setProperty("monitor.metricsInterval", "10000");
        properties.setProperty("monitor.resourceCheckInterval", "5000");
        properties.setProperty("monitor.enableDetailedMetrics", "true");
        
        // Configuración de cluster
        properties.setProperty("cluster.name", "nacional-cluster");
        properties.setProperty("cluster.discovery.enabled", "true");
        properties.setProperty("cluster.discovery.port", "7946");
        properties.setProperty("cluster.heartbeat.interval", "5000");
    }
    
    private void loadRegionalDefaults() {
        // Configuración de red usando HostConfig
        properties.setProperty("broker.host", hostConfig.getBrokerRegionalHost());
        properties.setProperty("broker.port", String.valueOf(hostConfig.getBrokerRegionalPort()));
        properties.setProperty("broker.endpoints", 
            "tcp -h " + hostConfig.getBrokerRegionalHost() + " -p " + hostConfig.getBrokerRegionalPort());
        
        // Configuración de réplicas
        properties.setProperty("replica.minReplicas", "1");
        properties.setProperty("replica.maxReplicas", "5");
        properties.setProperty("replica.basePort", "11000");
        properties.setProperty("replica.hostPattern", hostConfig.getReplicaHost());
        properties.setProperty("replica.autoScaling", "true");
        
        // Configuración de escalado
        properties.setProperty("scaling.cpuThreshold", "60.0");
        properties.setProperty("scaling.memoryThreshold", "70.0");
        properties.setProperty("scaling.scaleUpThreshold", "65.0");
        properties.setProperty("scaling.scaleDownThreshold", "20.0");
        properties.setProperty("scaling.evaluationInterval", "45000");
        properties.setProperty("scaling.cooldownPeriod", "90000");
        
        // Configuración de balanceador
        properties.setProperty("loadBalancer.algorithm", "LEAST_CONNECTIONS");
        properties.setProperty("loadBalancer.healthCheckInterval", "20000");
        properties.setProperty("loadBalancer.maxFailures", "2");
        properties.setProperty("loadBalancer.failureTimeout", "45000");
        
        // Configuración de monitoreo
        properties.setProperty("monitor.metricsInterval", "15000");
        properties.setProperty("monitor.resourceCheckInterval", "7500");
        properties.setProperty("monitor.enableDetailedMetrics", "false");
        
        // Configuración de cluster
        properties.setProperty("cluster.name", "regional-cluster");
        properties.setProperty("cluster.discovery.enabled", "true");
        properties.setProperty("cluster.discovery.port", "7947");
        properties.setProperty("cluster.heartbeat.interval", "7500");
        
        // Configuración específica regional
        properties.setProperty("regional.nacionalEndpoint", hostConfig.getBrokerNacionalEndpoint());
        properties.setProperty("regional.mesasPort", "12000");
        properties.setProperty("regional.maxMesas", "50");
    }
    
    // ========== MÉTODOS DE IConfig ==========
    
    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    @Override
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    @Override
    public int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Valor inválido para " + key + ": " + properties.getProperty(key));
            return defaultValue;
        }
    }
    
    @Override
    public double getDoubleProperty(String key, double defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Valor inválido para " + key + ": " + properties.getProperty(key));
            return defaultValue;
        }
    }
    
    @Override
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    // ========== MÉTODOS ESPECÍFICOS DE BROKER ==========
    
    public String getBrokerHost() {
        return getProperty("broker.host", "nacional".equals(brokerType) ? 
            hostConfig.getBrokerNacionalHost() : hostConfig.getBrokerRegionalHost());
    }
    
    public int getBrokerPort() {
        return getIntProperty("broker.port", "nacional".equals(brokerType) ? 9090 : 8080);
    }
    
    public String getBrokerEndpoints() {
        return getProperty("broker.endpoints", 
            "tcp -h " + getBrokerHost() + " -p " + getBrokerPort());
    }
    
    // ========== CONFIGURACIÓN DE RÉPLICAS ==========
    
    public int getMinReplicas() {
        return getIntProperty("replica.minReplicas", 1);
    }
    
    public int getMaxReplicas() {
        return getIntProperty("replica.maxReplicas", "nacional".equals(brokerType) ? 10 : 5);
    }
    
    public int getReplicaBasePort() {
        return getIntProperty("replica.basePort", "nacional".equals(brokerType) ? 10000 : 11000);
    }
    
    public String getReplicaHostPattern() {
        return getProperty("replica.hostPattern", hostConfig.getReplicaHost());
    }
    
    public boolean isAutoScalingEnabled() {
        return getBooleanProperty("replica.autoScaling", true);
    }
    
    public List<String> getReplicaHosts() {
        String hostsStr = getProperty("replica.hosts", hostConfig.getReplicaHost());
        return Arrays.asList(hostsStr.split(","));
    }
    
    // ========== CONFIGURACIÓN DE ESCALADO ==========
    
    public double getCpuThreshold() {
        return getDoubleProperty("scaling.cpuThreshold", "nacional".equals(brokerType) ? 70.0 : 60.0);
    }
    
    public double getMemoryThreshold() {
        return getDoubleProperty("scaling.memoryThreshold", "nacional".equals(brokerType) ? 80.0 : 70.0);
    }
    
    public double getScaleUpThreshold() {
        return getDoubleProperty("scaling.scaleUpThreshold", "nacional".equals(brokerType) ? 75.0 : 65.0);
    }
    
    public double getScaleDownThreshold() {
        return getDoubleProperty("scaling.scaleDownThreshold", "nacional".equals(brokerType) ? 25.0 : 20.0);
    }
    
    public long getEvaluationInterval() {
        return getIntProperty("scaling.evaluationInterval", "nacional".equals(brokerType) ? 30000 : 45000);
    }
    
    public long getCooldownPeriod() {
        return getIntProperty("scaling.cooldownPeriod", "nacional".equals(brokerType) ? 60000 : 90000);
    }
    
    // ========== CONFIGURACIÓN DE BALANCEADOR ==========
    
    public String getLoadBalancerAlgorithm() {
        return getProperty("loadBalancer.algorithm", 
            "nacional".equals(brokerType) ? "ROUND_ROBIN" : "LEAST_CONNECTIONS");
    }
    
    public long getHealthCheckInterval() {
        return getIntProperty("loadBalancer.healthCheckInterval", 
            "nacional".equals(brokerType) ? 15000 : 20000);
    }
    
    public int getMaxFailures() {
        return getIntProperty("loadBalancer.maxFailures", 
            "nacional".equals(brokerType) ? 3 : 2);
    }
    
    public long getFailureTimeout() {
        return getIntProperty("loadBalancer.failureTimeout", 
            "nacional".equals(brokerType) ? 30000 : 45000);
    }
    
    // ========== CONFIGURACIÓN DE MONITOREO ==========
    
    public long getMetricsInterval() {
        return getIntProperty("monitor.metricsInterval", 
            "nacional".equals(brokerType) ? 10000 : 15000);
    }
    
    public long getResourceCheckInterval() {
        return getIntProperty("monitor.resourceCheckInterval", 
            "nacional".equals(brokerType) ? 5000 : 7500);
    }
    
    public boolean isDetailedMetricsEnabled() {
        return getBooleanProperty("monitor.enableDetailedMetrics", 
            "nacional".equals(brokerType));
    }
    
    // ========== CONFIGURACIÓN DE CLUSTER ==========
    
    public String getClusterName() {
        return getProperty("cluster.name", brokerType + "-cluster");
    }
    
    public boolean isClusterDiscoveryEnabled() {
        return getBooleanProperty("cluster.discovery.enabled", true);
    }
    
    public int getClusterDiscoveryPort() {
        return getIntProperty("cluster.discovery.port", 
            "nacional".equals(brokerType) ? 7946 : 7947);
    }
    
    public long getHeartbeatInterval() {
        return getIntProperty("cluster.heartbeat.interval", 
            "nacional".equals(brokerType) ? 5000 : 7500);
    }
    
    // ========== CONFIGURACIÓN ESPECÍFICA REGIONAL ==========
    
    public String getNacionalEndpoint() {
        return getProperty("regional.nacionalEndpoint", hostConfig.getBrokerNacionalEndpoint());
    }
    
    public int getMesasPort() {
        return getIntProperty("regional.mesasPort", 12000);
    }
    
    public int getMaxMesas() {
        return getIntProperty("regional.maxMesas", 50);
    }
    
    // ========== MÉTODOS DE UTILIDAD ==========
    
    public void printConfiguration() {
        System.out.println("🎯 ===== CONFIGURACIÓN BROKER " + brokerType.toUpperCase() + " =====");
        System.out.println("🌐 Red:");
        System.out.println("   Host: " + getBrokerHost());
        System.out.println("   Puerto: " + getBrokerPort());
        System.out.println("   Endpoints: " + getBrokerEndpoints());
        
        System.out.println("🔄 Réplicas:");
        System.out.println("   Mín/Máx: " + getMinReplicas() + "/" + getMaxReplicas());
        System.out.println("   Puerto base: " + getReplicaBasePort());
        System.out.println("   Auto-escalado: " + (isAutoScalingEnabled() ? "✅" : "❌"));
        
        System.out.println("📊 Escalado:");
        System.out.println("   CPU umbral: " + getCpuThreshold() + "%");
        System.out.println("   Memoria umbral: " + getMemoryThreshold() + "%");
        System.out.println("   Subir/Bajar: " + getScaleUpThreshold() + "%/" + getScaleDownThreshold() + "%");
        
        System.out.println("⚖️ Balanceador:");
        System.out.println("   Algoritmo: " + getLoadBalancerAlgorithm());
        System.out.println("   Health check: " + getHealthCheckInterval() + "ms");
        
        System.out.println("🏢 Cluster:");
        System.out.println("   Nombre: " + getClusterName());
        System.out.println("   Descubrimiento: " + (isClusterDiscoveryEnabled() ? "✅" : "❌"));
        System.out.println("   Puerto descubrimiento: " + getClusterDiscoveryPort());
        
        if ("regional".equals(brokerType)) {
            System.out.println("🏛️ Regional específico:");
            System.out.println("   Endpoint nacional: " + getNacionalEndpoint());
            System.out.println("   Puerto mesas: " + getMesasPort());
            System.out.println("   Máx mesas: " + getMaxMesas());
        }
        
        System.out.println("===============================================");
    }
    
    public String getBrokerType() {
        return brokerType;
    }
    
    public Properties getAllProperties() {
        return new Properties(properties);
    }
} 