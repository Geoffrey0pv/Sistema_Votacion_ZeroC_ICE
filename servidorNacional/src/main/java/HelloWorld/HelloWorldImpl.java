package HelloWorld;

import Demo.IHelloWorld;
import com.zeroc.Ice.Current;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementación de la interfaz IHelloWorld
 * Proporciona endpoints simples para demostrar funcionalidad básica
 */
public class HelloWorldImpl implements IHelloWorld {
    
    private final String serverName;
    private final String version;
    private final long startTime;
    
    public HelloWorldImpl() {
        this.serverName = "Servidor Nacional - Sistema de Votación";
        this.version = "1.0.0";
        this.startTime = System.currentTimeMillis();
    }
    
    public HelloWorldImpl(String serverName, String version) {
        this.serverName = serverName;
        this.version = version;
        this.startTime = System.currentTimeMillis();
    }
    
    @Override
    public String sayHello(Current current) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("🌍 [" + timestamp + "] Hello World endpoint llamado desde: " + 
                          (current.con != null ? current.con.toString() : "conexión desconocida"));
        
        return "¡Hola Mundo desde el " + serverName + "! 🎯\n" +
               "Timestamp: " + timestamp + "\n" +
               "Versión: " + version + "\n" +
               "Estado: ✅ Funcionando correctamente";
    }
    
    @Override
    public String sayHelloTo(String name, Current current) {
        if (name == null || name.trim().isEmpty()) {
            name = "Usuario Anónimo";
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("👋 [" + timestamp + "] Hello personalizado para: " + name);
        
        return "¡Hola " + name + "! 👋\n" +
               "Bienvenido al " + serverName + "\n" +
               "Hora del servidor: " + timestamp + "\n" +
               "¡Gracias por conectarte! 🚀";
    }
    
    @Override
    public String getServerInfo(Current current) {
        long uptime = System.currentTimeMillis() - startTime;
        long uptimeSeconds = uptime / 1000;
        long uptimeMinutes = uptimeSeconds / 60;
        long uptimeHours = uptimeMinutes / 60;
        
        String uptimeStr = String.format("%02d:%02d:%02d", 
                                       uptimeHours, 
                                       uptimeMinutes % 60, 
                                       uptimeSeconds % 60);
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        System.out.println("📊 [" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                          "] Información del servidor solicitada");
        
        return "📊 INFORMACIÓN DEL SERVIDOR\n" +
               "================================\n" +
               "🏷️  Nombre: " + serverName + "\n" +
               "🔢 Versión: " + version + "\n" +
               "⏰ Tiempo activo: " + uptimeStr + "\n" +
               "🖥️  JVM: " + System.getProperty("java.version") + "\n" +
               "💾 Memoria usada: " + (usedMemory / 1024 / 1024) + " MB\n" +
               "💾 Memoria total: " + (totalMemory / 1024 / 1024) + " MB\n" +
               "🔧 Procesadores: " + runtime.availableProcessors() + "\n" +
               "🌐 Host: " + System.getProperty("user.name") + "@" + getHostname() + "\n" +
               "📅 Fecha actual: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
               "================================\n" +
               "✅ Estado: Operativo";
    }
    
    @Override
    public long getCurrentTime(Current current) {
        long currentTime = System.currentTimeMillis();
        System.out.println("🕐 [" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                          "] Timestamp solicitado: " + currentTime);
        return currentTime;
    }
    
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }
} 