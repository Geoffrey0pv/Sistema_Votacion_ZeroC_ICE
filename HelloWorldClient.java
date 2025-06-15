import com.zeroc.Ice.*;
import Demo.*;

public class HelloWorldClient {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {
            // Conectar al Hello World endpoint
            ObjectPrx base = communicator.stringToProxy("HelloWorld:tcp -h localhost -p 9090");
            IHelloWorldPrx helloWorld = IHelloWorldPrx.checkedCast(base);
            
            if (helloWorld == null) {
                throw new Error("❌ No se pudo conectar al endpoint HelloWorld");
            }
            
            System.out.println("🌍 ===== CLIENTE HELLO WORLD =====");
            System.out.println("📡 Conectado a: tcp -h localhost -p 9090");
            System.out.println("=====================================");
            
            // Probar los métodos
            System.out.println("\n📞 Llamando a sayHello():");
            System.out.println("📨 " + helloWorld.sayHello());
            
            System.out.println("\n📞 Llamando a sayHelloTo('Postman User'):");
            System.out.println("📨 " + helloWorld.sayHelloTo("Postman User"));
            
            System.out.println("\n📞 Llamando a getServerInfo():");
            System.out.println("📨 " + helloWorld.getServerInfo());
            
            System.out.println("\n📞 Llamando a getCurrentTime():");
            System.out.println("📨 Timestamp: " + helloWorld.getCurrentTime());
            
            System.out.println("\n✅ ¡Todas las pruebas completadas exitosamente!");
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 