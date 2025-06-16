package Config;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuración de cluster para despliegue en múltiples computadores
 * Maneja descubrimiento de nodos, topología de red y configuración distribuida
 */
public class ClusterConfig implements IConfig {
    private static ClusterConfig instance;
    private Properties properties;
    private static final String CONFIG_FILE = "/cluster.cfg";
    private final Map<String, NodeInfo> knownNodes = new ConcurrentHashMap<>();
    private String localNodeId;
    private String localIpAddress;
    
    // Información de nodo en el cluster
    public static class NodeInfo {
        public String nodeId;
        public String ipAddress;
        public int port;
        public String role; // "nacional", "regional", "mesa"
        public String status; // "active", "inactive", "failed"
        public long lastHeartbeat;
        public Map<String, String> metadata;
        
        public NodeInfo(String nodeId, String ipAddress, int port, String role) {
            this.nodeId = nodeId;
            this.ipAddress = ipAddress;
            this.port = port;
            this.role = role;
            this.status = "active";
            this.lastHeartbeat = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }
        
        @Override
        public String toString() {
            return String.format("Node{id=%s, ip=%s:%d, role=%s, status=%s}", 
                nodeId, ipAddress, port, role, status);
        }
    }
    
    private ClusterConfig() {
        detectLocalInfo();
        loadConfiguration();
    }
    
    public static synchronized ClusterConfig getInstance() {
        if (instance == null) {
            instance = new ClusterConfig();
        }
        return instance;
    }
    
    private void detectLocalInfo() {
        try {
            // Generar ID único para este nodo
            this.localNodeId = generateNodeId();
            
            // Detectar IP local
            this.localIpAddress = detectLocalIpAddress();
            
            System.out.println("🏷️ Nodo local detectado: " + localNodeId + " @ " + localIpAddress);
            
        } catch (Exception e) {
            System.err.println("❌ Error detectando información local: " + e.getMessage());
            this.localNodeId = "node-" + System.currentTimeMillis();
            this.localIpAddress = "localhost";
        }
    }
    
    private String generateNodeId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String timestamp = String.valueOf(System.currentTimeMillis() % 100000);
            return hostname + "-" + timestamp;
        } catch (Exception e) {
            return "node-" + System.currentTimeMillis();
        }
    }
    
    private String detectLocalIpAddress() throws Exception {
        // Buscar la primera interfaz de red no-loopback
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }
            
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                
                if (!address.isLoopbackAddress() && 
                    !address.isLinkLocalAddress() && 
                    address.isSiteLocalAddress()) {
                    return address.getHostAddress();
                }
            }
        }
        
        // Fallback a localhost si no se encuentra otra IP
        return "localhost";
    }
    
    private void loadConfiguration() {
        properties = new Properties();
        
        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("❌ No se pudo encontrar: " + CONFIG_FILE);
                System.err.println("   Usando configuración de cluster por defecto");
                loadDefaultProperties();
                return;
            }
            
            properties.load(input);
            System.out.println("✅ Configuración de cluster cargada: " + CONFIG_FILE);
            
            // Cargar nodos conocidos desde configuración
            loadKnownNodesFromConfig();
            
        } catch (IOException e) {
            System.err.println("❌ Error cargando configuración de cluster: " + e.getMessage());
            loadDefaultProperties();
        }
    }
    
    private void loadDefaultProperties() {
        // Configuración de descubrimiento
        properties.setProperty("cluster.discovery.enabled", "true");
        properties.setProperty("cluster.discovery.multicast.address", "224.0.0.1");
        properties.setProperty("cluster.discovery.multicast.port", "7946");
        properties.setProperty("cluster.discovery.interval", "30000");
        properties.setProperty("cluster.discovery.timeout", "5000");
        
        // Configuración de heartbeat
        properties.setProperty("cluster.heartbeat.enabled", "true");
        properties.setProperty("cluster.heartbeat.interval", "10000");
        properties.setProperty("cluster.heartbeat.timeout", "30000");
        properties.setProperty("cluster.heartbeat.maxMissed", "3");
        
        // Configuración de red
        properties.setProperty("cluster.network.bindAddress", localIpAddress);
        properties.setProperty("cluster.network.port", "7947");
        properties.setProperty("cluster.network.maxConnections", "100");
        properties.setProperty("cluster.network.connectionTimeout", "10000");
        
        // Configuración de roles
        properties.setProperty("cluster.roles.nacional.maxInstances", "3");
        properties.setProperty("cluster.roles.regional.maxInstances", "10");
        properties.setProperty("cluster.roles.mesa.maxInstances", "1000");
        
        // Configuración de failover
        properties.setProperty("cluster.failover.enabled", "true");
        properties.setProperty("cluster.failover.electionTimeout", "15000");
        properties.setProperty("cluster.failover.leaderHeartbeat", "5000");
        
        // Configuración de seguridad
        properties.setProperty("cluster.security.enabled", "false");
        properties.setProperty("cluster.security.keystore", "");
        properties.setProperty("cluster.security.password", "");
        
        // Nodos semilla por defecto (para desarrollo local)
        properties.setProperty("cluster.seeds", "localhost:7947");
    }
    
    private void loadKnownNodesFromConfig() {
        String seedsStr = getProperty("cluster.seeds", "");
        if (!seedsStr.isEmpty()) {
            String[] seeds = seedsStr.split(",");
            for (String seed : seeds) {
                try {
                    String[] parts = seed.trim().split(":");
                    if (parts.length == 2) {
                        String ip = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        String nodeId = "seed-" + ip.replace(".", "-") + "-" + port;
                        
                        NodeInfo node = new NodeInfo(nodeId, ip, port, "unknown");
                        knownNodes.put(nodeId, node);
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error parseando nodo semilla: " + seed);
                }
            }
        }
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
    
    // ========== MÉTODOS DE INFORMACIÓN LOCAL ==========
    
    public String getLocalNodeId() {
        return localNodeId;
    }
    
    public String getLocalIpAddress() {
        return localIpAddress;
    }
    
    public NodeInfo getLocalNodeInfo(String role, int port) {
        NodeInfo local = new NodeInfo(localNodeId, localIpAddress, port, role);
        local.metadata.put("startTime", String.valueOf(System.currentTimeMillis()));
        local.metadata.put("javaVersion", System.getProperty("java.version"));
        local.metadata.put("osName", System.getProperty("os.name"));
        return local;
    }
    
    // ========== MÉTODOS DE DESCUBRIMIENTO ==========
    
    public boolean isDiscoveryEnabled() {
        return getBooleanProperty("cluster.discovery.enabled", true);
    }
    
    public String getMulticastAddress() {
        return getProperty("cluster.discovery.multicast.address", "224.0.0.1");
    }
    
    public int getMulticastPort() {
        return getIntProperty("cluster.discovery.multicast.port", 7946);
    }
    
    public long getDiscoveryInterval() {
        return getIntProperty("cluster.discovery.interval", 30000);
    }
    
    public int getDiscoveryTimeout() {
        return getIntProperty("cluster.discovery.timeout", 5000);
    }
    
    // ========== MÉTODOS DE HEARTBEAT ==========
    
    public boolean isHeartbeatEnabled() {
        return getBooleanProperty("cluster.heartbeat.enabled", true);
    }
    
    public long getHeartbeatInterval() {
        return getIntProperty("cluster.heartbeat.interval", 10000);
    }
    
    public long getHeartbeatTimeout() {
        return getIntProperty("cluster.heartbeat.timeout", 30000);
    }
    
    public int getMaxMissedHeartbeats() {
        return getIntProperty("cluster.heartbeat.maxMissed", 3);
    }
    
    // ========== MÉTODOS DE RED ==========
    
    public String getBindAddress() {
        return getProperty("cluster.network.bindAddress", localIpAddress);
    }
    
    public int getNetworkPort() {
        return getIntProperty("cluster.network.port", 7947);
    }
    
    public int getMaxConnections() {
        return getIntProperty("cluster.network.maxConnections", 100);
    }
    
    public int getConnectionTimeout() {
        return getIntProperty("cluster.network.connectionTimeout", 10000);
    }
    
    // ========== MÉTODOS DE GESTIÓN DE NODOS ==========
    
    public void registerNode(NodeInfo node) {
        knownNodes.put(node.nodeId, node);
        System.out.println("📝 Nodo registrado: " + node);
    }
    
    public void unregisterNode(String nodeId) {
        NodeInfo removed = knownNodes.remove(nodeId);
        if (removed != null) {
            System.out.println("🗑️ Nodo desregistrado: " + removed);
        }
    }
    
    public NodeInfo getNode(String nodeId) {
        return knownNodes.get(nodeId);
    }
    
    public Collection<NodeInfo> getAllNodes() {
        return new ArrayList<>(knownNodes.values());
    }
    
    public List<NodeInfo> getNodesByRole(String role) {
        return knownNodes.values().stream()
            .filter(node -> role.equals(node.role))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<NodeInfo> getActiveNodes() {
        return knownNodes.values().stream()
            .filter(node -> "active".equals(node.status))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public void updateNodeHeartbeat(String nodeId) {
        NodeInfo node = knownNodes.get(nodeId);
        if (node != null) {
            node.lastHeartbeat = System.currentTimeMillis();
            node.status = "active";
        }
    }
    
    public void markNodeAsFailed(String nodeId) {
        NodeInfo node = knownNodes.get(nodeId);
        if (node != null) {
            node.status = "failed";
            System.err.println("❌ Nodo marcado como fallido: " + node);
        }
    }
    
    // ========== MÉTODOS DE FAILOVER ==========
    
    public boolean isFailoverEnabled() {
        return getBooleanProperty("cluster.failover.enabled", true);
    }
    
    public long getElectionTimeout() {
        return getIntProperty("cluster.failover.electionTimeout", 15000);
    }
    
    public long getLeaderHeartbeat() {
        return getIntProperty("cluster.failover.leaderHeartbeat", 5000);
    }
    
    // ========== MÉTODOS DE ROLES ==========
    
    public int getMaxInstancesForRole(String role) {
        return getIntProperty("cluster.roles." + role + ".maxInstances", 1);
    }
    
    public boolean canCreateInstanceForRole(String role) {
        int current = getNodesByRole(role).size();
        int max = getMaxInstancesForRole(role);
        return current < max;
    }
    
    // ========== MÉTODOS DE SEGURIDAD ==========
    
    public boolean isSecurityEnabled() {
        return getBooleanProperty("cluster.security.enabled", false);
    }
    
    public String getKeystore() {
        return getProperty("cluster.security.keystore", "");
    }
    
    public String getKeystorePassword() {
        return getProperty("cluster.security.password", "");
    }
    
    // ========== MÉTODOS DE UTILIDAD ==========
    
    public void printClusterStatus() {
        System.out.println("\n🏢 ===== ESTADO DEL CLUSTER =====");
        System.out.println("🏷️ Nodo local: " + localNodeId + " @ " + localIpAddress);
        System.out.println("🔍 Descubrimiento: " + (isDiscoveryEnabled() ? "✅" : "❌"));
        System.out.println("💓 Heartbeat: " + (isHeartbeatEnabled() ? "✅" : "❌"));
        System.out.println("🔒 Seguridad: " + (isSecurityEnabled() ? "✅" : "❌"));
        
        System.out.println("\n📊 Nodos conocidos (" + knownNodes.size() + "):");
        for (NodeInfo node : knownNodes.values()) {
            String statusIcon = "active".equals(node.status) ? "🟢" : 
                               "failed".equals(node.status) ? "🔴" : "🟡";
            long timeSinceHeartbeat = System.currentTimeMillis() - node.lastHeartbeat;
            System.out.printf("   %s %s [%s] (último heartbeat: %ds)%n", 
                statusIcon, node, node.role, timeSinceHeartbeat / 1000);
        }
        
        System.out.println("\n📈 Estadísticas por rol:");
        Map<String, Long> roleStats = knownNodes.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                node -> node.role,
                java.util.stream.Collectors.counting()));
        
        for (Map.Entry<String, Long> entry : roleStats.entrySet()) {
            int max = getMaxInstancesForRole(entry.getKey());
            System.out.printf("   %s: %d/%d%n", entry.getKey(), entry.getValue(), max);
        }
        
        System.out.println("===============================\n");
    }
    
    public List<String> getSeedNodes() {
        String seedsStr = getProperty("cluster.seeds", "");
        return seedsStr.isEmpty() ? new ArrayList<>() : Arrays.asList(seedsStr.split(","));
    }
    
    public Properties getAllProperties() {
        return new Properties(properties);
    }
} 