import GestorMesa.GestorMesa;
import InterfazGrafica.MesaVotacionUI;
import mesaVotacion.MesaVotacionImpl;
import com.zeroc.Ice.*;
import javax.swing.SwingUtilities;

/**
 * Aplicación Principal de Mesa de Votación
 * VERSIÓN DISTRIBUIDA: Soporta votación normal + distribución remota
 */
public class MesaVotacion {
    
    public static void main(String[] args) {
        
        // Verificar argumentos
        if (args.length < 1) {
            mostrarUso();
            return;
        }
        
        String mesaId = args[0];
        String modo = args.length > 1 ? args[1] : "votar";
        
        try {
            switch (modo.toLowerCase()) {
                case "votar":
                case "local":
                    ejecutarModoVotacion(mesaId);
                    break;
                case "servidor":
                    ejecutarModoServidor(mesaId, args);
                    break;
                case "hibrido":
                    ejecutarModoHibrido(mesaId, args);
                    break;
                default:
                    System.err.println("❌ Modo desconocido: " + modo);
                    mostrarUso();
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error iniciando mesa de votación: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void mostrarUso() {
        System.err.println("❌ Uso:");
        System.err.println("   java -jar mesaVotacion.jar <ID_MESA> [modo]");
        System.err.println("");
        System.err.println("Modos disponibles:");
        System.err.println("   votar    - Interfaz gráfica de votación (por defecto)");
        System.err.println("   local    - Interfaz gráfica de votación (igual que votar)");
        System.err.println("   servidor - Solo servidor ICE para recibir archivos");
        System.err.println("   hibrido  - Servidor ICE + interfaz de votación");
        System.err.println("");
        System.err.println("Ejemplos:");
        System.err.println("   java -jar mesaVotacion.jar 1 votar");
        System.err.println("   java -jar mesaVotacion.jar 1 servidor");
        System.err.println("   java -jar mesaVotacion.jar 1 hibrido");
    }
    
    /**
     * MODO VOTACIÓN: Interfaz gráfica original para votar
     */
    private static void ejecutarModoVotacion(String mesaId) {
        System.out.println("🗳️ === INICIANDO MESA DE VOTACIÓN ===");
        System.out.println("📍 Mesa: " + mesaId);
        System.out.println("🖥️ Modo: Interfaz Gráfica de Votación");
        
        // Inicializar comunicador ICE para GestorMesa con archivo de configuración
        Communicator communicator = null;
        try {
            // Cargar configuración desde mesa.cfg
            String[] iceArgs = {"--Ice.Config=mesaVotacion/src/main/resources/mesa.cfg"};
            communicator = Util.initialize(iceArgs);
            
            // Crear GestorMesa (ya incluye Sistema de Verificación integrado)
            GestorMesa gestorMesa = new GestorMesa(mesaId);
            boolean mesaInicializada = gestorMesa.inicializar(communicator);
            
            if (mesaInicializada) {
                System.out.println("✅ Mesa de votación inicializada correctamente");
            } else {
                System.out.println("⚠️ Mesa funcionará sin conexión al servidor regional");
            }
            
            // Iniciar interfaz gráfica
            SwingUtilities.invokeLater(() -> {
                try {
                    MesaVotacionUI interfaz = new MesaVotacionUI(gestorMesa);
                    interfaz.mostrar();
                    System.out.println("🖥️ Interfaz gráfica de votación iniciada");
                                 } catch (java.lang.Exception e) {
                     System.err.println("❌ Error iniciando interfaz gráfica: " + e.getMessage());
                     e.printStackTrace();
                 }
            });
            
                 } catch (java.lang.Exception e) {
             System.err.println("❌ Error inicializando mesa: " + e.getMessage());
             e.printStackTrace();
         }
        
        // Mantener el programa activo
        System.out.println("💡 La mesa de votación está activa. Cierre la ventana para terminar.");
        
        // Configurar shutdown hook
        final Communicator finalCommunicator = communicator;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Cerrando mesa de votación...");
            if (finalCommunicator != null) {
                finalCommunicator.destroy();
            }
        }));
        
        // Esperar hasta que el usuario cierre la aplicación
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("🛑 Mesa de votación cerrada");
        }
    }
    
    /**
     * MODO SERVIDOR: Solo servidor ICE para recibir archivos
     */
    private static void ejecutarModoServidor(String mesaId, String[] args) {
        try {
            System.out.println("🗳️ === INICIANDO MESA DE VOTACIÓN (MODO SERVIDOR ICE) ===");
            
            // Inicializar ICE
            Communicator communicator = Util.initialize(args);
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("MesaVotacion", "tcp -p 1002" + mesaId);
            
            // Crear implementación
            MesaVotacionImpl mesaImpl = new MesaVotacionImpl(mesaId);
            ObjectPrx proxy = adapter.add(mesaImpl, Util.stringToIdentity("Mesa" + mesaId));
            
            adapter.activate();
            
            System.out.println("✅ Mesa " + mesaId + " lista para recibir archivos SQLite");
            System.out.println("📡 Endpoint: " + proxy.toString());
            System.out.println("💡 Registre esta mesa en el servidor regional:");
            System.out.println("   registrar " + mesaId + " " + proxy.toString());
            System.out.println("");
            System.out.println("⏳ Presione Ctrl+C para detener el servidor...");
            
            communicator.waitForShutdown();
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error en modo servidor: " + e.getMessage());
        }
    }
    
    /**
     * MODO HÍBRIDO: Servidor ICE + Interfaz de votación
     */
    private static void ejecutarModoHibrido(String mesaId, String[] args) {
        try {
            System.out.println("🗳️ === INICIANDO MESA DE VOTACIÓN (MODO HÍBRIDO) ===");
            
            // Inicializar ICE para recibir archivos
            Communicator communicator = Util.initialize(args);
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints("MesaVotacion", "tcp -p 1002" + mesaId);
            
            MesaVotacionImpl mesaImpl = new MesaVotacionImpl(mesaId);
            ObjectPrx proxy = adapter.add(mesaImpl, Util.stringToIdentity("Mesa" + mesaId));
            
            adapter.activate();
            
            System.out.println("✅ Servidor ICE activo: " + proxy.toString());
            
            // Inicializar GestorMesa para votación
            GestorMesa gestorMesa = new GestorMesa(mesaId);
            boolean mesaInicializada = gestorMesa.inicializar(communicator);
            
            if (mesaInicializada) {
                System.out.println("✅ Mesa de votación lista para votar");
            } else {
                System.out.println("⚠️ Mesa funcionará sin conexión al servidor regional");
            }
            
            // Iniciar interfaz gráfica
            SwingUtilities.invokeLater(() -> {
                try {
                    MesaVotacionUI interfaz = new MesaVotacionUI(gestorMesa);
                    interfaz.mostrar();
                    System.out.println("🖥️ Interfaz de votación iniciada (modo híbrido)");
                                 } catch (java.lang.Exception e) {
                     System.err.println("❌ Error iniciando interfaz: " + e.getMessage());
                     e.printStackTrace();
                 }
            });
            
            System.out.println("🌐 Mesa en modo híbrido:");
            System.out.println("   📡 Puede recibir archivos remotos via ICE");
            System.out.println("   🗳️ Interfaz de votación activa");
            System.out.println("   💡 Registre la mesa: registrar " + mesaId + " " + proxy.toString());
            
            communicator.waitForShutdown();
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error en modo híbrido: " + e.getMessage());
        }
    }
}