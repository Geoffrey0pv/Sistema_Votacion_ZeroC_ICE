package Config;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad para interpolación de variables en archivos de configuración
 * Reemplaza placeholders ${variable} con valores de HostConfig
 */
public class ConfigInterpolator {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private final HostConfig hostConfig;
    
    public ConfigInterpolator() {
        this.hostConfig = HostConfig.getInstance();
    }
    
    /**
     * Interpola variables en un Properties, reemplazando ${variable} con valores de HostConfig
     */
    public Properties interpolate(Properties original) {
        Properties interpolated = new Properties();
        
        for (String key : original.stringPropertyNames()) {
            String value = original.getProperty(key);
            String interpolatedValue = interpolateString(value);
            interpolated.setProperty(key, interpolatedValue);
        }
        
        return interpolated;
    }
    
    /**
     * Interpola variables en una cadena individual
     */
    public String interpolateString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = getVariableValue(variableName);
            
            if (replacement != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // Si no se encuentra la variable, dejar el placeholder original
                System.err.println("⚠️ Variable no encontrada: " + variableName);
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Obtiene el valor de una variable desde HostConfig
     */
    private String getVariableValue(String variableName) {
        try {
            switch (variableName) {
                // Configuración de ICE
                case "ice.locator.host":
                    return hostConfig.getIceLocatorHost();
                case "ice.locator.port":
                    return String.valueOf(hostConfig.getIceLocatorPort());
                
                // Configuración Nacional
                case "nacional.host":
                    return hostConfig.getNacionalHost();
                case "nacional.port":
                    return String.valueOf(hostConfig.getNacionalPort());
                case "nacional.adapter.endpoints":
                    return hostConfig.getNacionalAdapterEndpoints();
                
                // Configuración Regional
                case "regional.host":
                    return hostConfig.getRegionalHost();
                case "regional.port":
                    return String.valueOf(hostConfig.getRegionalPort());
                
                // Configuración de Réplicas
                case "replica.host":
                    return hostConfig.getReplicaHost();
                case "replica.base_port":
                    return String.valueOf(hostConfig.getReplicaBasePort());
                case "replica.max_replicas":
                    return String.valueOf(hostConfig.getMaxReplicas());
                
                // Configuración de Broker
                case "broker.nacional.host":
                    return hostConfig.getBrokerNacionalHost();
                case "broker.nacional.port":
                    return String.valueOf(hostConfig.getBrokerNacionalPort());
                case "broker.nacional.endpoint":
                    return hostConfig.getBrokerNacionalEndpoint();
                case "broker.regional.host":
                    return hostConfig.getBrokerRegionalHost();
                case "broker.regional.port":
                    return String.valueOf(hostConfig.getBrokerRegionalPort());
                
                // Configuración de Cluster
                case "cluster.seeds":
                    return hostConfig.getClusterSeeds();
                
                // Configuración de Red
                case "network.local.hostname":
                    return hostConfig.getNetworkLocalHostname();
                case "network.local.ip":
                    return hostConfig.getNetworkLocalIP();
                
                // Configuración de Base de Datos
                case "database.host":
                    return hostConfig.getDatabaseHost();
                case "database.port":
                    return String.valueOf(hostConfig.getDatabasePort());
                
                // Configuración de Monitoreo
                case "monitoring.host":
                    return hostConfig.getMonitoringHost();
                case "monitoring.port":
                    return String.valueOf(hostConfig.getMonitoringPort());
                
                // Configuración de Desarrollo
                case "dev.nacional.host":
                    return hostConfig.getDevNacionalHost();
                case "dev.regional.host":
                    return hostConfig.getDevRegionalHost();
                case "dev.cluster.seeds":
                    return hostConfig.getDevClusterSeeds();
                
                // Variables genéricas - intentar obtener desde HostConfig
                default:
                    return hostConfig.getProperty(variableName);
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo variable " + variableName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Método estático de conveniencia para interpolación rápida
     */
    public static String interpolate(String input) {
        ConfigInterpolator interpolator = new ConfigInterpolator();
        return interpolator.interpolateString(input);
    }
    
    /**
     * Método estático de conveniencia para interpolación de Properties
     */
    public static Properties interpolateProperties(Properties properties) {
        ConfigInterpolator interpolator = new ConfigInterpolator();
        return interpolator.interpolate(properties);
    }
    
    /**
     * Método para probar la interpolación con valores de ejemplo
     */
    public static void testInterpolation() {
        System.out.println("🧪 ===== PRUEBA DE INTERPOLACIÓN =====");
        
        String[] testStrings = {
            "tcp -h ${nacional.host} -p ${nacional.port}",
            "DemoIceGrid/Locator:default -h ${ice.locator.host} -p ${ice.locator.port}",
            "Cluster seeds: ${cluster.seeds}",
            "Réplicas en ${replica.host}:${replica.base_port}+",
            "Broker: ${broker.nacional.endpoint}",
            "Red local: ${network.local.hostname} (${network.local.ip})"
        };
        
        ConfigInterpolator interpolator = new ConfigInterpolator();
        
        for (String test : testStrings) {
            String result = interpolator.interpolateString(test);
            System.out.println("   Original: " + test);
            System.out.println("   Resultado: " + result);
            System.out.println();
        }
        
        System.out.println("==========================================");
    }
    
    /**
     * Valida que todas las variables necesarias estén definidas
     */
    public boolean validateVariables(String input) {
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        boolean allValid = true;
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = getVariableValue(variableName);
            
            if (value == null || value.trim().isEmpty()) {
                System.err.println("❌ Variable no válida o vacía: " + variableName);
                allValid = false;
            }
        }
        
        return allValid;
    }
    
    /**
     * Lista todas las variables encontradas en un texto
     */
    public java.util.List<String> findVariables(String input) {
        java.util.List<String> variables = new java.util.ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!variables.contains(variableName)) {
                variables.add(variableName);
            }
        }
        
        return variables;
    }
} 