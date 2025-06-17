package Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Configuración de despliegue para diferentes escenarios
 * Maneja configuraciones específicas para desarrollo, testing y producción
 */
public class DeploymentConfig implements IConfig {
    private static DeploymentConfig instance;
    private Properties properties;
    private static final String CONFIG_FILE = "/deployment.cfg";
    private DeploymentMode currentMode;
    private final Map<String, DeploymentScenario> scenarios = new HashMap<>();
    private final HostConfig hostConfig;
    
    // Modos de despliegue soportados
    public enum DeploymentMode {
        DEVELOPMENT("development"),
        TESTING("testing"),
        STAGING("staging"),
        PRODUCTION("production");
        
        private final String mode;
        DeploymentMode(String mode) { this.mode = mode; }
        public String getMode() { return mode; }
    }
    
    // Configuraciones predefinidas para diferentes escenarios
    public static class DeploymentScenario {
        public String name;
        public String description;
        public Map<String, String> configuration;
        public List<String> requiredHosts;
        public Map<String, Integer> portMappings;
        
        public DeploymentScenario(String name, String description) {
            this.name = name;
            this.description = description;
            this.configuration = new HashMap<>();
            this.requiredHosts = new ArrayList<>();
            this.portMappings = new HashMap<>();
        }
    }
    
    private DeploymentConfig() {
        this.hostConfig = HostConfig.getInstance(); // Inicializar configuración centralizada
        loadConfiguration();
        initializeScenarios();
    }
    
    public static synchronized DeploymentConfig getInstance() {
        if (instance == null) {
            instance = new DeploymentConfig();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        properties = new Properties();
        
        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("❌ No se pudo encontrar: " + CONFIG_FILE);
                System.err.println("   Usando configuración de despliegue por defecto");
                loadDefaultProperties();
                return;
            }
            
            properties.load(input);
            System.out.println("✅ Configuración de despliegue cargada: " + CONFIG_FILE);
            
        } catch (IOException e) {
            System.err.println("❌ Error cargando configuración de despliegue: " + e.getMessage());
            loadDefaultProperties();
        }
        
        // Determinar modo actual
        String modeStr = getProperty("deployment.mode", "development");
        try {
            currentMode = DeploymentMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Modo de despliegue inválido: " + modeStr + ", usando DEVELOPMENT");
            currentMode = DeploymentMode.DEVELOPMENT;
        }
        
        System.out.println("🚀 Modo de despliegue: " + currentMode);
    }
    
    private void loadDefaultProperties() {
        // Configuración básica usando HostConfig
        properties.setProperty("deployment.mode", "development");
        properties.setProperty("deployment.environment", "local");
        properties.setProperty("deployment.version", "1.0.0");
        
        // Hosts por defecto usando configuración centralizada
        properties.setProperty("nacional.host", hostConfig.getDevNacionalHost());
        properties.setProperty("nacional.port", String.valueOf(hostConfig.getNacionalPort()));
        properties.setProperty("regional.host", hostConfig.getDevRegionalHost());
        properties.setProperty("regional.port", String.valueOf(hostConfig.getRegionalPort()));
        
        // Configuración de cluster
        properties.setProperty("cluster.seeds", hostConfig.getDevClusterSeeds());
        
        // Configuración de desarrollo
        properties.setProperty("development.autoStart", "true");
        properties.setProperty("development.debugMode", "true");
        properties.setProperty("development.hotReload", "true");
        properties.setProperty("development.mockData", "true");
        
        // Configuración de testing
        properties.setProperty("testing.parallelExecution", "true");
        properties.setProperty("testing.mockExternalServices", "true");
        properties.setProperty("testing.cleanupAfterTests", "true");
        
        // Configuración de producción
        properties.setProperty("production.securityEnabled", "true");
        properties.setProperty("production.monitoringEnabled", "true");
        properties.setProperty("production.loggingLevel", "WARN");
        properties.setProperty("production.performanceOptimized", "true");
    }
    
    private void initializeScenarios() {
        // Escenario 1: Desarrollo local usando HostConfig
        DeploymentScenario localDev = new DeploymentScenario("local-development", 
            "Desarrollo local en una sola máquina");
        localDev.configuration.put("nacional.host", hostConfig.getDevNacionalHost());
        localDev.configuration.put("nacional.port", String.valueOf(hostConfig.getNacionalPort()));
        localDev.configuration.put("regional.host", hostConfig.getDevRegionalHost());
        localDev.configuration.put("regional.port", String.valueOf(hostConfig.getRegionalPort()));
        localDev.requiredHosts.add(hostConfig.getNetworkLocalHostname());
        localDev.portMappings.put("nacional", hostConfig.getNacionalPort());
        localDev.portMappings.put("regional", hostConfig.getRegionalPort());
        localDev.portMappings.put("cluster", 7947);
        scenarios.put("local-development", localDev);
        
        // Escenario 2: Red local (LAN)
        DeploymentScenario lanDeploy = new DeploymentScenario("lan-deployment", 
            "Despliegue en red local con múltiples computadores");
        lanDeploy.configuration.put("nacional.host", "192.168.1.100");
        lanDeploy.configuration.put("nacional.port", "9090");
        lanDeploy.configuration.put("regional.hosts", "192.168.1.101,192.168.1.102,192.168.1.103");
        lanDeploy.configuration.put("regional.port", "8080");
        lanDeploy.configuration.put("cluster.seeds", "192.168.1.100:7947,192.168.1.101:7947,192.168.1.102:7947");
        lanDeploy.requiredHosts.addAll(Arrays.asList("192.168.1.100", "192.168.1.101", "192.168.1.102", "192.168.1.103"));
        scenarios.put("lan-deployment", lanDeploy);
        
        // Escenario 3: Cloud/VPS
        DeploymentScenario cloudDeploy = new DeploymentScenario("cloud-deployment", 
            "Despliegue en cloud con IPs públicas");
        cloudDeploy.configuration.put("nacional.host", "203.0.113.10");
        cloudDeploy.configuration.put("nacional.port", "9090");
        cloudDeploy.configuration.put("regional.hosts", "203.0.113.11,203.0.113.12");
        cloudDeploy.configuration.put("regional.port", "8080");
        cloudDeploy.configuration.put("security.enabled", "true");
        cloudDeploy.configuration.put("firewall.enabled", "true");
        scenarios.put("cloud-deployment", cloudDeploy);
        
        // Escenario 4: Híbrido (Nacional en cloud, regionales en LAN)
        DeploymentScenario hybridDeploy = new DeploymentScenario("hybrid-deployment", 
            "Nacional en cloud, regionales en redes locales");
        hybridDeploy.configuration.put("nacional.host", "203.0.113.10");
        hybridDeploy.configuration.put("nacional.port", "9090");
        hybridDeploy.configuration.put("regional.hosts", "192.168.1.101,192.168.2.101,192.168.3.101");
        hybridDeploy.configuration.put("vpn.enabled", "true");
        hybridDeploy.configuration.put("nat.traversal", "true");
        scenarios.put("hybrid-deployment", hybridDeploy);
        
        // Escenario 5: Alta disponibilidad
        DeploymentScenario haDeploy = new DeploymentScenario("high-availability", 
            "Despliegue con alta disponibilidad y redundancia");
        haDeploy.configuration.put("nacional.hosts", "10.0.0.10,10.0.0.11,10.0.0.12");
        haDeploy.configuration.put("nacional.port", "9090");
        haDeploy.configuration.put("loadBalancer.enabled", "true");
        haDeploy.configuration.put("failover.enabled", "true");
        haDeploy.configuration.put("replication.factor", "3");
        scenarios.put("high-availability", haDeploy);
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
    
    // ========== MÉTODOS DE DESPLIEGUE ==========
    
    public DeploymentMode getCurrentMode() {
        return currentMode;
    }
    
    public boolean isDevelopmentMode() {
        return currentMode == DeploymentMode.DEVELOPMENT;
    }
    
    public boolean isTestingMode() {
        return currentMode == DeploymentMode.TESTING;
    }
    
    public boolean isProductionMode() {
        return currentMode == DeploymentMode.PRODUCTION;
    }
    
    public String getEnvironment() {
        return getProperty("deployment.environment", "local");
    }
    
    public String getVersion() {
        return getProperty("deployment.version", "1.0.0");
    }
    
    // ========== MÉTODOS DE ESCENARIOS ==========
    
    public DeploymentScenario getScenario(String name) {
        return scenarios.get(name);
    }
    
    public Collection<DeploymentScenario> getAllScenarios() {
        return scenarios.values();
    }
    
    public void applyScenario(String scenarioName) {
        DeploymentScenario scenario = scenarios.get(scenarioName);
        if (scenario == null) {
            System.err.println("❌ Escenario no encontrado: " + scenarioName);
            return;
        }
        
        System.out.println("🎯 Aplicando escenario: " + scenario.name);
        System.out.println("   Descripción: " + scenario.description);
        
        // Aplicar configuración del escenario
        for (Map.Entry<String, String> entry : scenario.configuration.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        
        System.out.println("✅ Escenario aplicado exitosamente");
    }
    
    // ========== MÉTODOS ESPECÍFICOS POR MODO ==========
    
    public boolean isAutoStartEnabled() {
        return getBooleanProperty("development.autoStart", isDevelopmentMode());
    }
    
    public boolean isDebugModeEnabled() {
        return getBooleanProperty("development.debugMode", isDevelopmentMode());
    }
    
    public boolean isHotReloadEnabled() {
        return getBooleanProperty("development.hotReload", isDevelopmentMode());
    }
    
    public boolean isMockDataEnabled() {
        return getBooleanProperty("development.mockData", isDevelopmentMode());
    }
    
    public boolean isSecurityEnabled() {
        return getBooleanProperty("production.securityEnabled", isProductionMode());
    }
    
    public boolean isMonitoringEnabled() {
        return getBooleanProperty("production.monitoringEnabled", !isDevelopmentMode());
    }
    
    public boolean isPerformanceOptimized() {
        return getBooleanProperty("production.performanceOptimized", isProductionMode());
    }
    
    // ========== MÉTODOS DE CONFIGURACIÓN DE RED ==========
    
    public List<String> getNacionalHosts() {
        String hosts = getProperty("nacional.hosts", getProperty("nacional.host", hostConfig.getDevNacionalHost()));
        return Arrays.asList(hosts.split(","));
    }
    
    public List<String> getRegionalHosts() {
        String hosts = getProperty("regional.hosts", getProperty("regional.host", hostConfig.getDevRegionalHost()));
        return Arrays.asList(hosts.split(","));
    }
    
    public int getNacionalPort() {
        return getIntProperty("nacional.port", 9090);
    }
    
    public int getRegionalPort() {
        return getIntProperty("regional.port", 8080);
    }
    
    public List<String> getClusterSeeds() {
        String seeds = getProperty("cluster.seeds", hostConfig.getDevClusterSeeds());
        return Arrays.asList(seeds.split(","));
    }
    
    // ========== MÉTODOS DE UTILIDAD ==========
    
    public void printDeploymentInfo() {
        System.out.println("\n🚀 ===== INFORMACIÓN DE DESPLIEGUE =====");
        System.out.println("📋 Modo: " + currentMode);
        System.out.println("🌍 Entorno: " + getEnvironment());
        System.out.println("🏷️ Versión: " + getVersion());
        
        System.out.println("\n🖥️ Configuración de hosts:");
        System.out.println("   Nacional: " + getNacionalHosts() + ":" + getNacionalPort());
        System.out.println("   Regional: " + getRegionalHosts() + ":" + getRegionalPort());
        System.out.println("   Cluster seeds: " + getClusterSeeds());
        
        System.out.println("\n⚙️ Características habilitadas:");
        System.out.println("   Auto-start: " + (isAutoStartEnabled() ? "✅" : "❌"));
        System.out.println("   Debug: " + (isDebugModeEnabled() ? "✅" : "❌"));
        System.out.println("   Hot reload: " + (isHotReloadEnabled() ? "✅" : "❌"));
        System.out.println("   Mock data: " + (isMockDataEnabled() ? "✅" : "❌"));
        System.out.println("   Seguridad: " + (isSecurityEnabled() ? "✅" : "❌"));
        System.out.println("   Monitoreo: " + (isMonitoringEnabled() ? "✅" : "❌"));
        System.out.println("   Optimización: " + (isPerformanceOptimized() ? "✅" : "❌"));
        
        System.out.println("\n📊 Escenarios disponibles:");
        for (DeploymentScenario scenario : scenarios.values()) {
            System.out.println("   • " + scenario.name + ": " + scenario.description);
        }
        
        System.out.println("=========================================\n");
    }
    
    public Map<String, String> generateDockerComposeConfig() {
        Map<String, String> config = new HashMap<>();
        
        // Configuración básica
        config.put("version", "3.8");
        config.put("nacional.image", "sistema-votacion/nacional:latest");
        config.put("regional.image", "sistema-votacion/regional:latest");
        
        // Puertos
        config.put("nacional.ports", getNacionalPort() + ":9090");
        config.put("regional.ports", getRegionalPort() + ":8080");
        
        // Variables de entorno
        config.put("DEPLOYMENT_MODE", currentMode.getMode());
        config.put("ENVIRONMENT", getEnvironment());
        config.put("VERSION", getVersion());
        
        return config;
    }
    
    public Map<String, String> generateKubernetesConfig() {
        Map<String, String> config = new HashMap<>();
        
        // Configuración de Kubernetes
        config.put("apiVersion", "apps/v1");
        config.put("kind", "Deployment");
        config.put("replicas", isProductionMode() ? "3" : "1");
        config.put("image", "sistema-votacion/broker:latest");
        
        // ConfigMap
        config.put("configmap.deployment.mode", currentMode.getMode());
        config.put("configmap.environment", getEnvironment());
        
        return config;
    }
    
    public Properties getAllProperties() {
        return new Properties(properties);
    }
} 