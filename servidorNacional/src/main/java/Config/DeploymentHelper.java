package Config;

import java.io.*;
import java.util.*;

/**
 * Utilidad para ayudar en el despliegue del sistema
 * Proporciona herramientas para configurar y desplegar en múltiples computadores
 */
public class DeploymentHelper {
    
    public static void main(String[] args) {
        DeploymentHelper helper = new DeploymentHelper();
        
        if (args.length == 0) {
            helper.showHelp();
            return;
        }
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "info":
                helper.showSystemInfo();
                break;
            case "network":
                helper.showNetworkInfo();
                break;
            case "scenarios":
                helper.showScenarios();
                break;
            case "apply":
                if (args.length > 1) {
                    helper.applyScenario(args[1]);
                } else {
                    System.err.println("❌ Especifica el nombre del escenario");
                }
                break;
            case "generate":
                if (args.length > 1) {
                    helper.generateConfig(args[1]);
                } else {
                    System.err.println("❌ Especifica el tipo de configuración (docker, kubernetes, scripts)");
                }
                break;
            case "test":
                helper.testConnectivity();
                break;
            case "setup":
                helper.interactiveSetup();
                break;
            default:
                System.err.println("❌ Comando desconocido: " + command);
                helper.showHelp();
        }
    }
    
    private void showHelp() {
        System.out.println("\n🚀 ===== SISTEMA DE VOTACIÓN - DEPLOYMENT HELPER =====");
        System.out.println("Utilidad para configurar y desplegar el sistema en múltiples computadores\n");
        
        System.out.println("📋 Comandos disponibles:");
        System.out.println("  info       - Mostrar información del sistema");
        System.out.println("  network    - Mostrar información de red");
        System.out.println("  scenarios  - Listar escenarios de despliegue disponibles");
        System.out.println("  apply <escenario> - Aplicar un escenario específico");
        System.out.println("  generate <tipo> - Generar configuraciones (docker, kubernetes, scripts)");
        System.out.println("  test       - Probar conectividad entre hosts");
        System.out.println("  setup      - Configuración interactiva");
        
        System.out.println("\n💡 Ejemplos:");
        System.out.println("  java Config.DeploymentHelper info");
        System.out.println("  java Config.DeploymentHelper apply lan-deployment");
        System.out.println("  java Config.DeploymentHelper generate docker");
        System.out.println("  java Config.DeploymentHelper setup");
        
        System.out.println("\n📚 Escenarios disponibles:");
        System.out.println("  • local-development  - Desarrollo en una sola máquina");
        System.out.println("  • lan-deployment     - Despliegue en red local");
        System.out.println("  • cloud-deployment   - Despliegue en la nube");
        System.out.println("  • hybrid-deployment  - Despliegue híbrido");
        System.out.println("  • high-availability  - Alta disponibilidad");
        
        System.out.println("=====================================================\n");
    }
    
    private void showSystemInfo() {
        System.out.println("📊 Mostrando información del sistema...\n");
        
        DeploymentConfig deployConfig = DeploymentConfig.getInstance();
        deployConfig.printDeploymentInfo();
    }
    
    private void showNetworkInfo() {
        System.out.println("🌐 Analizando configuración de red...\n");
        
        NetworkConfig networkConfig = NetworkConfig.getInstance();
        networkConfig.printNetworkInfo();
    }
    
    private void showScenarios() {
        System.out.println("📋 Escenarios de despliegue disponibles:\n");
        
        DeploymentConfig deployConfig = DeploymentConfig.getInstance();
        Collection<DeploymentConfig.DeploymentScenario> scenarios = deployConfig.getAllScenarios();
        
        for (DeploymentConfig.DeploymentScenario scenario : scenarios) {
            System.out.println("🎯 " + scenario.name);
            System.out.println("   Descripción: " + scenario.description);
            System.out.println("   Hosts requeridos: " + scenario.requiredHosts.size());
            System.out.println("   Configuraciones: " + scenario.configuration.size());
            
            if (!scenario.requiredHosts.isEmpty()) {
                System.out.println("   IPs: " + String.join(", ", scenario.requiredHosts));
            }
            
            if (!scenario.portMappings.isEmpty()) {
                System.out.println("   Puertos:");
                for (Map.Entry<String, Integer> entry : scenario.portMappings.entrySet()) {
                    System.out.println("     • " + entry.getKey() + ": " + entry.getValue());
                }
            }
            System.out.println();
        }
    }
    
    private void applyScenario(String scenarioName) {
        System.out.println("🎯 Aplicando escenario: " + scenarioName + "\n");
        
        DeploymentConfig deployConfig = DeploymentConfig.getInstance();
        deployConfig.applyScenario(scenarioName);
        
        System.out.println("\n✅ Escenario aplicado. Nueva configuración:");
        deployConfig.printDeploymentInfo();
    }
    
    private void generateConfig(String type) {
        System.out.println("🔧 Generando configuración: " + type + "\n");
        
        DeploymentConfig deployConfig = DeploymentConfig.getInstance();
        
        switch (type.toLowerCase()) {
            case "docker":
                generateDockerConfig(deployConfig);
                break;
            case "kubernetes":
                generateKubernetesConfig(deployConfig);
                break;
            case "scripts":
                generateScripts(deployConfig);
                break;
            default:
                System.err.println("❌ Tipo de configuración no soportado: " + type);
                System.out.println("Tipos disponibles: docker, kubernetes, scripts");
        }
    }
    
    private void generateDockerConfig(DeploymentConfig deployConfig) {
        Map<String, String> config = deployConfig.generateDockerComposeConfig();
        
        StringBuilder dockerCompose = new StringBuilder();
        dockerCompose.append("# Docker Compose para Sistema de Votación\n");
        dockerCompose.append("# Generado automáticamente\n\n");
        dockerCompose.append("version: '").append(config.get("version")).append("'\n\n");
        dockerCompose.append("services:\n");
        
        // Servicio Nacional
        dockerCompose.append("  servidor-nacional:\n");
        dockerCompose.append("    image: ").append(config.get("nacional.image")).append("\n");
        dockerCompose.append("    ports:\n");
        dockerCompose.append("      - \"").append(config.get("nacional.ports")).append("\"\n");
        dockerCompose.append("    environment:\n");
        dockerCompose.append("      - DEPLOYMENT_MODE=").append(config.get("DEPLOYMENT_MODE")).append("\n");
        dockerCompose.append("      - ENVIRONMENT=").append(config.get("ENVIRONMENT")).append("\n");
        dockerCompose.append("    networks:\n");
        dockerCompose.append("      - votacion-network\n\n");
        
        // Servicio Regional
        dockerCompose.append("  servidor-regional:\n");
        dockerCompose.append("    image: ").append(config.get("regional.image")).append("\n");
        dockerCompose.append("    ports:\n");
        dockerCompose.append("      - \"").append(config.get("regional.ports")).append("\"\n");
        dockerCompose.append("    environment:\n");
        dockerCompose.append("      - DEPLOYMENT_MODE=").append(config.get("DEPLOYMENT_MODE")).append("\n");
        dockerCompose.append("      - ENVIRONMENT=").append(config.get("ENVIRONMENT")).append("\n");
        dockerCompose.append("    depends_on:\n");
        dockerCompose.append("      - servidor-nacional\n");
        dockerCompose.append("    networks:\n");
        dockerCompose.append("      - votacion-network\n\n");
        
        dockerCompose.append("networks:\n");
        dockerCompose.append("  votacion-network:\n");
        dockerCompose.append("    driver: bridge\n");
        
        try {
            writeToFile("docker-compose.yml", dockerCompose.toString());
            System.out.println("✅ Archivo docker-compose.yml generado");
        } catch (IOException e) {
            System.err.println("❌ Error generando docker-compose.yml: " + e.getMessage());
        }
    }
    
    private void generateKubernetesConfig(DeploymentConfig deployConfig) {
        Map<String, String> config = deployConfig.generateKubernetesConfig();
        
        StringBuilder k8sConfig = new StringBuilder();
        k8sConfig.append("# Configuración Kubernetes para Sistema de Votación\n");
        k8sConfig.append("# Generado automáticamente\n\n");
        
        // ConfigMap
        k8sConfig.append("apiVersion: v1\n");
        k8sConfig.append("kind: ConfigMap\n");
        k8sConfig.append("metadata:\n");
        k8sConfig.append("  name: votacion-config\n");
        k8sConfig.append("data:\n");
        k8sConfig.append("  deployment.mode: \"").append(config.get("configmap.deployment.mode")).append("\"\n");
        k8sConfig.append("  environment: \"").append(config.get("configmap.environment")).append("\"\n\n");
        
        k8sConfig.append("---\n\n");
        
        // Deployment Nacional
        k8sConfig.append("apiVersion: ").append(config.get("apiVersion")).append("\n");
        k8sConfig.append("kind: ").append(config.get("kind")).append("\n");
        k8sConfig.append("metadata:\n");
        k8sConfig.append("  name: servidor-nacional\n");
        k8sConfig.append("spec:\n");
        k8sConfig.append("  replicas: ").append(config.get("replicas")).append("\n");
        k8sConfig.append("  selector:\n");
        k8sConfig.append("    matchLabels:\n");
        k8sConfig.append("      app: servidor-nacional\n");
        k8sConfig.append("  template:\n");
        k8sConfig.append("    metadata:\n");
        k8sConfig.append("      labels:\n");
        k8sConfig.append("        app: servidor-nacional\n");
        k8sConfig.append("    spec:\n");
        k8sConfig.append("      containers:\n");
        k8sConfig.append("      - name: servidor-nacional\n");
        k8sConfig.append("        image: ").append(config.get("image")).append("\n");
        k8sConfig.append("        ports:\n");
        k8sConfig.append("        - containerPort: 9090\n");
        k8sConfig.append("        envFrom:\n");
        k8sConfig.append("        - configMapRef:\n");
        k8sConfig.append("            name: votacion-config\n");
        
        try {
            writeToFile("kubernetes-deployment.yml", k8sConfig.toString());
            System.out.println("✅ Archivo kubernetes-deployment.yml generado");
        } catch (IOException e) {
            System.err.println("❌ Error generando kubernetes-deployment.yml: " + e.getMessage());
        }
    }
    
    private void generateScripts(DeploymentConfig deployConfig) {
        // Script de inicio
        StringBuilder startScript = new StringBuilder();
        startScript.append("#!/bin/bash\n");
        startScript.append("# Script de inicio para Sistema de Votación\n");
        startScript.append("# Generado automáticamente\n\n");
        startScript.append("echo \"🚀 Iniciando Sistema de Votación...\"\n\n");
        
        startScript.append("# Configuración\n");
        startScript.append("DEPLOYMENT_MODE=\"").append(deployConfig.getCurrentMode().getMode()).append("\"\n");
        startScript.append("ENVIRONMENT=\"").append(deployConfig.getEnvironment()).append("\"\n");
        startScript.append("VERSION=\"").append(deployConfig.getVersion()).append("\"\n\n");
        
        startScript.append("# Verificar Java\n");
        startScript.append("if ! command -v java &> /dev/null; then\n");
        startScript.append("    echo \"❌ Java no está instalado\"\n");
        startScript.append("    exit 1\n");
        startScript.append("fi\n\n");
        
        startScript.append("# Iniciar Servidor Nacional\n");
        startScript.append("echo \"📡 Iniciando Servidor Nacional...\"\n");
        startScript.append("java -jar servidor-nacional.jar &\n");
        startScript.append("NACIONAL_PID=$!\n\n");
        
        startScript.append("# Esperar un momento\n");
        startScript.append("sleep 5\n\n");
        
        startScript.append("# Iniciar Servidor Regional\n");
        startScript.append("echo \"🏢 Iniciando Servidor Regional...\"\n");
        startScript.append("java -jar servidor-regional.jar &\n");
        startScript.append("REGIONAL_PID=$!\n\n");
        
        startScript.append("echo \"✅ Sistema iniciado\"\n");
        startScript.append("echo \"Nacional PID: $NACIONAL_PID\"\n");
        startScript.append("echo \"Regional PID: $REGIONAL_PID\"\n");
        
        // Script de parada
        StringBuilder stopScript = new StringBuilder();
        stopScript.append("#!/bin/bash\n");
        stopScript.append("# Script de parada para Sistema de Votación\n\n");
        stopScript.append("echo \"🛑 Deteniendo Sistema de Votación...\"\n\n");
        stopScript.append("pkill -f \"servidor-nacional.jar\"\n");
        stopScript.append("pkill -f \"servidor-regional.jar\"\n\n");
        stopScript.append("echo \"✅ Sistema detenido\"\n");
        
        try {
            writeToFile("start-system.sh", startScript.toString());
            writeToFile("stop-system.sh", stopScript.toString());
            
            // Hacer ejecutables
            new File("start-system.sh").setExecutable(true);
            new File("stop-system.sh").setExecutable(true);
            
            System.out.println("✅ Scripts generados:");
            System.out.println("   • start-system.sh");
            System.out.println("   • stop-system.sh");
        } catch (IOException e) {
            System.err.println("❌ Error generando scripts: " + e.getMessage());
        }
    }
    
    private void testConnectivity() {
        System.out.println("🔗 Probando conectividad...\n");
        
        NetworkConfig networkConfig = NetworkConfig.getInstance();
        Collection<NetworkConfig.HostInfo> hosts = networkConfig.getDiscoveredHosts();
        
        if (hosts.isEmpty()) {
            System.out.println("⚠️ No hay hosts configurados para probar");
            return;
        }
        
        System.out.println("🎯 Probando " + hosts.size() + " hosts...\n");
        
        int reachable = 0;
        for (NetworkConfig.HostInfo host : hosts) {
            System.out.print("   Probando " + host.ip + ":" + host.port + " ... ");
            
            if (isHostReachable(host.ip, host.port)) {
                System.out.println("✅ OK");
                reachable++;
            } else {
                System.out.println("❌ FAIL");
            }
        }
        
        System.out.println("\n📊 Resultado: " + reachable + "/" + hosts.size() + " hosts alcanzables");
        
        if (reachable == hosts.size()) {
            System.out.println("🎉 ¡Todos los hosts están disponibles!");
        } else if (reachable > 0) {
            System.out.println("⚠️ Algunos hosts no están disponibles");
        } else {
            System.out.println("❌ Ningún host está disponible");
        }
    }
    
    private boolean isHostReachable(String ip, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(ip, port), 5000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void interactiveSetup() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("🛠️ ===== CONFIGURACIÓN INTERACTIVA =====\n");
        
        // Seleccionar escenario
        System.out.println("📋 Escenarios disponibles:");
        System.out.println("1. local-development  - Desarrollo local");
        System.out.println("2. lan-deployment     - Red local");
        System.out.println("3. cloud-deployment   - Nube");
        System.out.println("4. hybrid-deployment  - Híbrido");
        System.out.println("5. high-availability  - Alta disponibilidad");
        
        System.out.print("\n🎯 Selecciona un escenario (1-5): ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consumir newline
        
        String[] scenarios = {
            "local-development",
            "lan-deployment", 
            "cloud-deployment",
            "hybrid-deployment",
            "high-availability"
        };
        
        if (choice < 1 || choice > 5) {
            System.err.println("❌ Selección inválida");
            return;
        }
        
        String selectedScenario = scenarios[choice - 1];
        System.out.println("✅ Escenario seleccionado: " + selectedScenario);
        
        // Aplicar escenario
        applyScenario(selectedScenario);
        
        // Preguntar si generar configuraciones adicionales
        System.out.print("\n🔧 ¿Generar configuraciones adicionales? (docker/kubernetes/scripts/no): ");
        String configType = scanner.nextLine().toLowerCase();
        
        if (!configType.equals("no") && !configType.isEmpty()) {
            generateConfig(configType);
        }
        
        // Probar conectividad
        System.out.print("\n🔗 ¿Probar conectividad? (s/n): ");
        String testConn = scanner.nextLine().toLowerCase();
        
        if (testConn.equals("s") || testConn.equals("si") || testConn.equals("y") || testConn.equals("yes")) {
            testConnectivity();
        }
        
        System.out.println("\n🎉 ¡Configuración completada!");
        System.out.println("💡 Usa 'java Config.DeploymentHelper info' para ver la configuración actual");
        
        scanner.close();
    }
    
    private void writeToFile(String filename, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }
} 