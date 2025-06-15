import Demo.*;
import com.zeroc.Ice.*;

/**
 * 🧪 Test Simple de Conexión al Servicio de Lotes
 * 
 * Prueba la conexión al servicio de procesamiento de lotes
 * y obtiene estadísticas básicas.
 */
public class TestLoteVotos {
    
    public static void main(String[] args) {
        System.out.println("🧪 === TEST CONEXIÓN LOTE DE VOTOS ===");
        
        Communicator communicator = null;
        
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servicio de procesamiento de lotes
            ObjectPrx base = communicator.stringToProxy("ProcesadorLoteVotos:tcp -h localhost -p 9090");
            IProcesadorLoteVotosPrx procesadorPrx = IProcesadorLoteVotosPrx.checkedCast(base);
            
            if (procesadorPrx == null) {
                System.out.println("❌ No se pudo conectar al servicio de lotes de votos");
                System.out.println("💡 Asegúrate de que el servidor nacional esté ejecutándose en puerto 9090");
                System.exit(1);
            }
            
            System.out.println("✅ Conectado al servicio de lotes de votos");
            
            // Verificar disponibilidad
            boolean disponible = procesadorPrx.verificarDisponibilidad();
            System.out.println("🔍 Servicio disponible: " + (disponible ? "✅ SÍ" : "❌ NO"));
            
            if (!disponible) {
                System.out.println("⚠️ El servicio no está disponible");
                System.exit(1);
            }
            
            // Obtener estadísticas
            System.out.println("\n📊 Estadísticas del sistema:");
            EstadisticasVotos stats = procesadorPrx.obtenerEstadisticas();
            System.out.println("   Total votos: " + stats.totalVotos);
            System.out.println("   Total mesas: " + stats.totalMesas);
            System.out.println("   Total candidatos: " + stats.totalCandidatos);
            System.out.println("   Total municipios: " + stats.totalMunicipios);
            
            System.out.println("\n✅ TEST COMPLETADO EXITOSAMENTE");
            System.out.println("   El servicio de lotes está funcionando correctamente");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            if (e.getMessage().contains("Connection refused")) {
                System.out.println("💡 El servidor nacional no está ejecutándose. Ejecuta:");
                System.out.println("   cd ../../ && ./ejecutar_servidor_nacional.sh");
            }
            System.exit(1);
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }
} 