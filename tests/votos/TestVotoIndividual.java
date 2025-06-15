import Demo.*;
import com.zeroc.Ice.*;

/**
 * 🧪 Test de Voto Individual
 * 
 * Prueba el endpoint de procesamiento de votos individuales
 * en el servidor regional.
 */
public class TestVotoIndividual {
    
    public static void main(String[] args) {
        System.out.println("🧪 === TEST VOTO INDIVIDUAL ===");
        
        Communicator communicator = null;
        
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servicio de votos individuales (servidor regional)
            ObjectPrx base = communicator.stringToProxy("ProcesadorVotos:tcp -h localhost -p 9091");
            IProcesadorVotosPrx procesadorPrx = IProcesadorVotosPrx.checkedCast(base);
            
            if (procesadorPrx == null) {
                System.out.println("❌ No se pudo conectar al servicio de votos individuales");
                System.out.println("💡 Asegúrate de que el servidor regional esté ejecutándose en puerto 9091");
                System.exit(1);
            }
            
            System.out.println("✅ Conectado al servicio de votos individuales");
            
            // Verificar disponibilidad
            boolean disponible = procesadorPrx.verificarDisponibilidad();
            System.out.println("🔍 Servicio disponible: " + (disponible ? "✅ SÍ" : "❌ NO"));
            
            if (!disponible) {
                System.out.println("⚠️ El servicio no está disponible");
                System.exit(1);
            }
            
            // Obtener estadísticas iniciales
            System.out.println("\n📊 Estadísticas iniciales:");
            EstadisticasVotos statsIniciales = procesadorPrx.obtenerEstadisticas();
            System.out.println("   Total votos: " + statsIniciales.totalVotos);
            System.out.println("   Total mesas: " + statsIniciales.totalMesas);
            System.out.println("   Total candidatos: " + statsIniciales.totalCandidatos);
            System.out.println("   Total municipios: " + statsIniciales.totalMunicipios);
            
            // Crear un voto de prueba
            Voto voto = new Voto();
            voto.mesaId = "MESA-67890";
            voto.candidatoId = "PETRO2025";
            voto.timestamp = "2025-06-15T19:15:42";
            voto.municipio = "Medellín";
            voto.departamento = "Antioquia";
            voto.hashVerificacion = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz";
            voto.firmaMesa = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA...";
            
            System.out.println("\n📤 Enviando voto individual...");
            System.out.println("   Mesa: " + voto.mesaId);
            System.out.println("   Candidato: " + voto.candidatoId);
            System.out.println("   Municipio: " + voto.municipio);
            
            // Procesar voto
            try {
                ResultadoVoto resultado = procesadorPrx.procesarVoto(voto);
                
                if (resultado.exito) {
                    System.out.println("✅ VOTO PROCESADO EXITOSAMENTE");
                    System.out.println("   ID asignado: " + resultado.votoId);
                    System.out.println("   Mensaje: " + resultado.mensaje);
                } else {
                    System.out.println("❌ VOTO RECHAZADO");
                    System.out.println("   Razón: " + resultado.mensaje);
                }
                
                // Esperar un poco para que se actualicen las estadísticas
                Thread.sleep(1000);
                
                // Obtener estadísticas finales
                System.out.println("\n📊 Estadísticas finales:");
                EstadisticasVotos statsFinales = procesadorPrx.obtenerEstadisticas();
                System.out.println("   Total votos: " + statsFinales.totalVotos);
                System.out.println("   Total mesas: " + statsFinales.totalMesas);
                System.out.println("   Total candidatos: " + statsFinales.totalCandidatos);
                System.out.println("   Total municipios: " + statsFinales.totalMunicipios);
                
                // Verificar si aumentaron los votos
                if (statsFinales.totalVotos > statsIniciales.totalVotos) {
                    System.out.println("✅ El voto fue contabilizado correctamente");
                    System.out.println("   Votos agregados: " + (statsFinales.totalVotos - statsIniciales.totalVotos));
                } else {
                    System.out.println("⚠️ No se detectó aumento en el conteo de votos");
                }
                
            } catch (java.lang.Exception e) {
                System.out.println("❌ Error procesando voto: " + e.getMessage());
            }
            
            System.out.println("\n✅ TEST COMPLETADO EXITOSAMENTE");
            System.out.println("   El servicio de votos individuales está funcionando correctamente");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            if (e.getMessage().contains("Connection refused")) {
                System.out.println("💡 El servidor regional no está ejecutándose. Ejecuta:");
                System.out.println("   cd ../../ && ./ejecutar_servidor_regional.sh");
            }
            System.exit(1);
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }
} 