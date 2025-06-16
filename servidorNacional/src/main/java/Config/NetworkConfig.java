package Config;

import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Configuración de red para despliegues multi-computador
 * Maneja descubrimiento de hosts, topología de red y conectividad
 */
public class NetworkConfig {
    private static NetworkConfig instance;
    private final Map<String, HostInfo> discoveredHosts = new ConcurrentHashMap<>();
    private final Map<String, NetworkInterface> networkInterfaces = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Configuración de red
    private String localHostname;
    private String localIP;
    private List<String> availableIPs;
    private NetworkTopology topology;
    
    public enum NetworkTopology {
        SINGLE_HOST("single-host", "Una sola máquina"),
        LAN("lan", "Red de área local"),
        WAN("wan", "Red de área amplia"),
        HYBRID("hybrid", "Red híbrida"),
        CLOUD("cloud", "Despliegue en la nube");
        
        private final String type;
        private final String description;
        
        NetworkTopology(String type, String description) {
            this.type = type;
            this.description = description;
        }
        
        public String getType() { return type; }
        public String getDescription() { return description; }
    }
    
    public static class HostInfo {
        public String hostname;
        public String ip;
        public int port;
        public String role; // nacional, regional, mesa
        public boolean isReachable;
        public long lastSeen;
        public Map<String, Object> metadata;
        
        public HostInfo(String hostname, String ip, int port, String role) {
            this.hostname = hostname;
            this.ip = ip;
            this.port = port;
            this.role = role;
            this.isReachable = false;
            this.lastSeen = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }
        
        @Override
        public String toString() {
            return String.format("HostInfo{%s:%d [%s] - %s}", 
                ip, port, role, isReachable ? "UP" : "DOWN");
        }
    }
    
    private NetworkConfig() {
        initializeNetworkInfo();
        startHostDiscovery();
    }
    
    public static synchronized NetworkConfig getInstance() {
        if (instance == null) {
            instance = new NetworkConfig();
        }
        return instance;
    }
    
    private void initializeNetworkInfo() {
        try {
            // Obtener información del host local
            InetAddress localHost = InetAddress.getLocalHost();
            localHostname = localHost.getHostName();
            localIP = localHost.getHostAddress();
            
            // Obtener todas las IPs disponibles
            availableIPs = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                networkInterfaces.put(networkInterface.getName(), networkInterface);
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        availableIPs.add(address.getHostAddress());
                    }
                }
            }
            
            // Determinar topología de red
            determineNetworkTopology();
            
            System.out.println("🌐 Información de red inicializada:");
            System.out.println("   Hostname: " + localHostname);
            System.out.println("   IP principal: " + localIP);
            System.out.println("   IPs disponibles: " + availableIPs);
            System.out.println("   Topología: " + topology.getDescription());
            
        } catch (Exception e) {
            System.err.println("❌ Error inicializando información de red: " + e.getMessage());
            // Valores por defecto
            localHostname = "localhost";
            localIP = "127.0.0.1";
            availableIPs = Arrays.asList("127.0.0.1");
            topology = NetworkTopology.SINGLE_HOST;
        }
    }
    
    private void determineNetworkTopology() {
        if (availableIPs.size() <= 1 && availableIPs.contains("127.0.0.1")) {
            topology = NetworkTopology.SINGLE_HOST;
        } else if (hasPrivateIP()) {
            topology = NetworkTopology.LAN;
        } else if (hasPublicIP()) {
            topology = NetworkTopology.CLOUD;
        } else {
            topology = NetworkTopology.HYBRID;
        }
    }
    
    private boolean hasPrivateIP() {
        return availableIPs.stream().anyMatch(this::isPrivateIP);
    }
    
    private boolean hasPublicIP() {
        return availableIPs.stream().anyMatch(ip -> !isPrivateIP(ip) && !ip.equals("127.0.0.1"));
    }
    
    private boolean isPrivateIP(String ip) {
        return ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               (ip.startsWith("172.") && isInRange(ip, "172.16.", "172.31."));
    }
    
    private boolean isInRange(String ip, String start, String end) {
        try {
            String[] parts = ip.split("\\.");
            int secondOctet = Integer.parseInt(parts[1]);
            return secondOctet >= 16 && secondOctet <= 31;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void startHostDiscovery() {
        // Descubrimiento periódico de hosts
        scheduler.scheduleAtFixedRate(this::discoverHosts, 0, 30, TimeUnit.SECONDS);
        
        // Verificación de conectividad
        scheduler.scheduleAtFixedRate(this::checkHostConnectivity, 10, 60, TimeUnit.SECONDS);
    }
    
    private void discoverHosts() {
        try {
            System.out.println("🔍 Iniciando descubrimiento de hosts...");
            
            // Descubrimiento por multicast (para LAN)
            if (topology == NetworkTopology.LAN) {
                discoverHostsMulticast();
            }
            
            // Descubrimiento por configuración
            discoverConfiguredHosts();
            
            System.out.println("✅ Descubrimiento completado. Hosts encontrados: " + discoveredHosts.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error en descubrimiento de hosts: " + e.getMessage());
        }
    }
    
    private void discoverHostsMulticast() {
        try {
            // Enviar mensaje de descubrimiento por multicast
            MulticastSocket socket = new MulticastSocket();
            InetAddress group = InetAddress.getByName("224.0.0.251");
            
            String message = "DISCOVER:" + localHostname + ":" + localIP;
            byte[] buffer = message.getBytes();
            
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, 7947);
            socket.send(packet);
            socket.close();
            
        } catch (Exception e) {
            System.err.println("⚠️ Error en descubrimiento multicast: " + e.getMessage());
        }
    }
    
    private void discoverConfiguredHosts() {
        DeploymentConfig deployConfig = DeploymentConfig.getInstance();
        
        // Hosts nacionales
        List<String> nacionalHosts = deployConfig.getNacionalHosts();
        for (String host : nacionalHosts) {
            if (!host.equals("localhost") && !host.equals(localIP)) {
                addDiscoveredHost(host, deployConfig.getNacionalPort(), "nacional");
            }
        }
        
        // Hosts regionales
        List<String> regionalHosts = deployConfig.getRegionalHosts();
        for (String host : regionalHosts) {
            if (!host.equals("localhost") && !host.equals(localIP)) {
                addDiscoveredHost(host, deployConfig.getRegionalPort(), "regional");
            }
        }
        
        // Seeds del cluster
        List<String> clusterSeeds = deployConfig.getClusterSeeds();
        for (String seed : clusterSeeds) {
            String[] parts = seed.split(":");
            if (parts.length == 2 && !parts[0].equals("localhost") && !parts[0].equals(localIP)) {
                try {
                    int port = Integer.parseInt(parts[1]);
                    addDiscoveredHost(parts[0], port, "cluster");
                } catch (NumberFormatException e) {
                    System.err.println("⚠️ Puerto inválido en seed: " + seed);
                }
            }
        }
    }
    
    private void addDiscoveredHost(String ip, int port, String role) {
        String key = ip + ":" + port;
        if (!discoveredHosts.containsKey(key)) {
            try {
                String hostname = InetAddress.getByName(ip).getHostName();
                HostInfo hostInfo = new HostInfo(hostname, ip, port, role);
                discoveredHosts.put(key, hostInfo);
                System.out.println("🆕 Host descubierto: " + hostInfo);
            } catch (Exception e) {
                System.err.println("⚠️ Error resolviendo hostname para " + ip + ": " + e.getMessage());
            }
        }
    }
    
    private void checkHostConnectivity() {
        System.out.println("🔗 Verificando conectividad de hosts...");
        
        for (HostInfo host : discoveredHosts.values()) {
            boolean wasReachable = host.isReachable;
            host.isReachable = isHostReachable(host.ip, host.port);
            
            if (host.isReachable) {
                host.lastSeen = System.currentTimeMillis();
            }
            
            // Notificar cambios de estado
            if (wasReachable != host.isReachable) {
                String status = host.isReachable ? "UP" : "DOWN";
                System.out.println("🔄 Estado cambiado - " + host.ip + ":" + host.port + " -> " + status);
            }
        }
    }
    
    private boolean isHostReachable(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 5000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ========== MÉTODOS PÚBLICOS ==========
    
    public String getLocalHostname() {
        return localHostname;
    }
    
    public String getLocalIP() {
        return localIP;
    }
    
    public List<String> getAvailableIPs() {
        return new ArrayList<>(availableIPs);
    }
    
    public NetworkTopology getTopology() {
        return topology;
    }
    
    public Collection<HostInfo> getDiscoveredHosts() {
        return new ArrayList<>(discoveredHosts.values());
    }
    
    public List<HostInfo> getHostsByRole(String role) {
        return discoveredHosts.values().stream()
            .filter(host -> role.equals(host.role))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public List<HostInfo> getReachableHosts() {
        return discoveredHosts.values().stream()
            .filter(host -> host.isReachable)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public String getBestLocalIP() {
        // Preferir IPs privadas para LAN, públicas para cloud
        if (topology == NetworkTopology.LAN) {
            return availableIPs.stream()
                .filter(this::isPrivateIP)
                .findFirst()
                .orElse(localIP);
        } else if (topology == NetworkTopology.CLOUD) {
            return availableIPs.stream()
                .filter(ip -> !isPrivateIP(ip) && !ip.equals("127.0.0.1"))
                .findFirst()
                .orElse(localIP);
        }
        return localIP;
    }
    
    public String getBindAddress() {
        // Para desarrollo local, usar localhost
        if (topology == NetworkTopology.SINGLE_HOST) {
            return "127.0.0.1";
        }
        // Para otros casos, usar 0.0.0.0 para escuchar en todas las interfaces
        return "0.0.0.0";
    }
    
    public String getPublicAddress() {
        return getBestLocalIP();
    }
    
    public boolean isMultiHost() {
        return topology != NetworkTopology.SINGLE_HOST;
    }
    
    public boolean isCloudDeployment() {
        return topology == NetworkTopology.CLOUD;
    }
    
    public boolean isLANDeployment() {
        return topology == NetworkTopology.LAN;
    }
    
    public void addStaticHost(String ip, int port, String role) {
        addDiscoveredHost(ip, port, role);
    }
    
    public void removeHost(String ip, int port) {
        String key = ip + ":" + port;
        HostInfo removed = discoveredHosts.remove(key);
        if (removed != null) {
            System.out.println("🗑️ Host removido: " + removed);
        }
    }
    
    public void printNetworkInfo() {
        System.out.println("\n🌐 ===== INFORMACIÓN DE RED =====");
        System.out.println("🖥️ Host local:");
        System.out.println("   Hostname: " + localHostname);
        System.out.println("   IP principal: " + localIP);
        System.out.println("   Mejor IP local: " + getBestLocalIP());
        System.out.println("   Dirección de bind: " + getBindAddress());
        System.out.println("   Dirección pública: " + getPublicAddress());
        
        System.out.println("\n🔗 Topología: " + topology.getDescription());
        System.out.println("   Multi-host: " + (isMultiHost() ? "✅" : "❌"));
        System.out.println("   Cloud: " + (isCloudDeployment() ? "✅" : "❌"));
        System.out.println("   LAN: " + (isLANDeployment() ? "✅" : "❌"));
        
        System.out.println("\n📡 IPs disponibles:");
        for (String ip : availableIPs) {
            String type = isPrivateIP(ip) ? "Privada" : "Pública";
            System.out.println("   • " + ip + " (" + type + ")");
        }
        
        System.out.println("\n🖥️ Hosts descubiertos:");
        if (discoveredHosts.isEmpty()) {
            System.out.println("   (ninguno)");
        } else {
            for (HostInfo host : discoveredHosts.values()) {
                String status = host.isReachable ? "🟢" : "🔴";
                long timeSince = (System.currentTimeMillis() - host.lastSeen) / 1000;
                System.out.println("   " + status + " " + host + " (hace " + timeSince + "s)");
            }
        }
        
        System.out.println("\n📊 Estadísticas:");
        Map<String, Long> roleStats = new HashMap<>();
        for (HostInfo host : discoveredHosts.values()) {
            roleStats.merge(host.role, 1L, Long::sum);
        }
        
        for (Map.Entry<String, Long> entry : roleStats.entrySet()) {
            System.out.println("   " + entry.getKey() + ": " + entry.getValue());
        }
        
        long reachableCount = discoveredHosts.values().stream()
            .mapToLong(host -> host.isReachable ? 1 : 0)
            .sum();
        System.out.println("   Alcanzables: " + reachableCount + "/" + discoveredHosts.size());
        
        System.out.println("================================\n");
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 