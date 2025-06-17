import Demo.*;
import com.zeroc.Ice.*;

/**
 * Cliente de prueba para verificar la interfaz ICE de candidatos
 */
public class test_candidatos_ice {
    
    public static void main(String[] args) {
        System.out.println("🧪 === PRUEBA DE INTERFAZ ICE CANDIDATOS ===");
        
        try (Communicator communicator = Util.initialize(args)) {
            
            // Probar conexión al servicio especializado
            System.out.println("\n🔌 Probando ConsultaCandidatosEspecializado...");
            testServicioEspecializado(communicator);
            
            // Probar conexión al gestor SQLite
            System.out.println("\n🔌 Probando GestorCandidatosSQLite...");
            testGestorSQLite(communicator);
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error en prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testServicioEspecializado(Communicator communicator) {
        try {
            // Conectar al servicio especializado
            String endpoint = "consultaCandidatosEspecializado:tcp -h localhost -p 8080";
            ObjectPrx base = communicator.stringToProxy(endpoint);
            IConsultaCandidatosPrx servicio = IConsultaCandidatosPrx.checkedCast(base);
            
            if (servicio == null) {
                System.err.println("❌ No se pudo conectar al servicio especializado");
                return;
            }
            
            System.out.println("✅ Conexión exitosa al servicio especializado");
            
            // Verificar servicio
            boolean disponible = servicio.verificarServicio();
            System.out.println("🔧 Servicio disponible: " + (disponible ? "✅ SÍ" : "❌ NO"));
            
            // Contar candidatos
            long total = servicio.contarCandidatos();
            System.out.println("📊 Total candidatos: " + total);
            
            // Obtener partidos
            String[] partidos = servicio.obtenerPartidosDisponibles();
            System.out.println("🏛️ Partidos disponibles: " + partidos.length);
            for (String partido : partidos) {
                System.out.println("   • " + partido);
            }
            
            // Obtener algunos candidatos
            if (total > 0) {
                CandidatoElectoral[] candidatos = servicio.obtenerTodosCandidatosElectorales();
                System.out.println("📋 Primeros candidatos:");
                for (int i = 0; i < Math.min(3, candidatos.length); i++) {
                    CandidatoElectoral c = candidatos[i];
                    System.out.println("   " + c.id + ". " + c.nombre + " (" + c.partido + ")");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error probando servicio especializado: " + e.getMessage());
        }
    }
    
    private static void testGestorSQLite(Communicator communicator) {
        try {
            // Conectar al gestor SQLite
            String endpoint = "consultaCandidatos:tcp -h localhost -p 8080";
            ObjectPrx base = communicator.stringToProxy(endpoint);
            IConsultaCandidatosPrx gestor = IConsultaCandidatosPrx.checkedCast(base);
            
            if (gestor == null) {
                System.err.println("❌ No se pudo conectar al gestor SQLite");
                return;
            }
            
            System.out.println("✅ Conexión exitosa al gestor SQLite");
            
            // Verificar conexión BD
            boolean conexionBD = gestor.verificarConexionBD();
            System.out.println("💾 Conexión BD: " + (conexionBD ? "✅ OK" : "❌ FALLO"));
            
            // Contar candidatos
            long total = gestor.contarCandidatos();
            System.out.println("📊 Total candidatos: " + total);
            
            // Forzar sincronización si no hay candidatos
            if (total == 0) {
                System.out.println("🔄 Intentando sincronizar candidatos...");
                boolean sincronizado = gestor.sincronizarCandidatos();
                System.out.println("🔄 Sincronización: " + (sincronizado ? "✅ EXITOSA" : "❌ FALLO"));
                
                // Contar nuevamente
                total = gestor.contarCandidatos();
                System.out.println("📊 Total candidatos después de sincronizar: " + total);
            }
            
            // Buscar candidato por ID
            if (total > 0) {
                System.out.println("🔍 Buscando candidato ID 1...");
                CandidatoElectoral candidato = gestor.buscarCandidatoPorId(1);
                if (candidato != null) {
                    System.out.println("✅ Candidato encontrado: " + candidato.nombre + " (" + candidato.partido + ")");
                } else {
                    System.out.println("⚠️ Candidato ID 1 no encontrado");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error probando gestor SQLite: " + e.getMessage());
        }
    }
} 