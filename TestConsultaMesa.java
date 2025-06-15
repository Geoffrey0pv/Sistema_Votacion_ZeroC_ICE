import Demo.*;
import com.zeroc.Ice.*;

/**
 * Test class para consultar mesa de votación por documento
 * Conecta al servidor nacional y realiza consultas de prueba
 * Versión mejorada con manejo de servicio inactivo
 */
public class TestConsultaMesa {
    
    public static void main(String[] args) {
        Communicator communicator = null;
        int status = 0;
        
        try {
            System.out.println("🧪 ===== TEST CONSULTA MESA DE VOTACIÓN =====");
            System.out.println();
            
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servicio ConsultaMesa en el servidor nacional
            String endpoint = "ConsultaMesa:tcp -h localhost -p 9090";
            System.out.println("🔌 Conectando a: " + endpoint);
            
            ObjectPrx base = communicator.stringToProxy(endpoint);
            IConsultaMesaPrx consultaMesa = IConsultaMesaPrx.checkedCast(base);
            
            if (consultaMesa == null) {
                System.err.println("❌ ERROR: No se pudo conectar al servicio ConsultaMesa");
                System.err.println("   Verifique que el servidor nacional esté ejecutándose");
                return;
            }
            
            System.out.println("✅ Conectado exitosamente al servicio ConsultaMesa");
            System.out.println();
            
            // Test 1: Verificar conexión a base de datos
            System.out.println("🔧 TEST 1: Verificando conexión a base de datos...");
            boolean conexionOK = consultaMesa.verificarConexionBD();
            System.out.println("   Resultado: " + (conexionOK ? "✅ CONEXIÓN OK" : "❌ ERROR DE CONEXIÓN"));
            System.out.println();
            
            if (!conexionOK) {
                System.err.println("⚠️  ADVERTENCIA: Sin conexión a BD, las consultas pueden fallar");
                System.out.println();
            }
            
            // Test 2: Consultas con diferentes documentos
            String[] documentosPrueba = {
                "440527206",    // Documento por defecto
                "441795721",    // Documento alternativo
                "359784174",    // Documento de prueba
                "609753602"     // Documento que probablemente no existe
            };
            
            // Si se proporciona un documento como argumento, usarlo
            if (args.length > 0) {
                documentosPrueba = new String[]{args[0]};
                System.out.println("📄 Usando documento proporcionado: " + args[0]);
            }
            
            System.out.println("🔍 TEST 2: Realizando consultas de mesa...");
            System.out.println("==========================================");
            
            for (int i = 0; i < documentosPrueba.length; i++) {
                String documento = documentosPrueba[i];
                
                System.out.println("📋 Consulta " + (i + 1) + "/" + documentosPrueba.length);
                System.out.println("   Documento: " + documento);
                
                try {
                    // Realizar la consulta
                    long startTime = System.currentTimeMillis();
                    MesaInfo mesa = consultaMesa.consultarMesaPorDocumento(documento);
                    long endTime = System.currentTimeMillis();
                    
                    // Mostrar resultados según el tipo de respuesta
                    if (mesa != null) {
                        if ("SERVICIO_INACTIVO".equals(mesa.departamento)) {
                            System.out.println("   🚫 SERVICIO INACTIVO:");
                            System.out.println("      " + mesa.municipio);
                        } else if ("ERROR".equals(mesa.departamento)) {
                            System.out.println("   ❌ ERROR EN CONSULTA:");
                            System.out.println("      " + mesa.municipio);
                        } else if (!mesa.departamento.isEmpty()) {
                            System.out.println("   ✅ MESA ENCONTRADA:");
                            System.out.println("      📍 Departamento: " + mesa.departamento);
                            System.out.println("      🏛️  Municipio: " + mesa.municipio);
                            System.out.println("      🏢 Puesto: " + mesa.puesto);
                            System.out.println("      📊 Mesa: " + mesa.mesa);
                            System.out.println("      ⏱️  Tiempo: " + (endTime - startTime) + "ms");
                        } else {
                            System.out.println("   ⚠️  NO ENCONTRADO: No existe mesa para documento " + documento);
                            System.out.println("      ⏱️  Tiempo: " + (endTime - startTime) + "ms");
                        }
                    } else {
                        System.out.println("   ❌ RESPUESTA NULA del servidor");
                    }
                    
                } catch (java.lang.Exception e) {
                    System.err.println("   ❌ EXCEPCIÓN: " + e.getMessage());
                }
                
                System.out.println();
                
                // Pausa entre consultas para no saturar
                if (i < documentosPrueba.length - 1) {
                    Thread.sleep(500);
                }
            }
            
            // Test 3: Test de rendimiento (múltiples consultas rápidas)
            if (args.length == 0) { // Solo si no se especificó un documento particular
                System.out.println("⚡ TEST 3: Test de rendimiento...");
                String docRendimiento = "12345678";
                int numConsultas = 5;
                
                long tiempoTotal = 0;
                int exitosas = 0;
                int servicioInactivo = 0;
                int errores = 0;
                
                for (int i = 0; i < numConsultas; i++) {
                    try {
                        long start = System.currentTimeMillis();
                        MesaInfo mesa = consultaMesa.consultarMesaPorDocumento(docRendimiento);
                        long end = System.currentTimeMillis();
                        
                        tiempoTotal += (end - start);
                        
                        if (mesa != null) {
                            if ("SERVICIO_INACTIVO".equals(mesa.departamento)) {
                                servicioInactivo++;
                            } else if ("ERROR".equals(mesa.departamento)) {
                                errores++;
                            } else {
                                exitosas++;
                            }
                        }
                        
                    } catch (java.lang.Exception e) {
                        System.err.println("   Error en consulta " + (i + 1) + ": " + e.getMessage());
                        errores++;
                    }
                }
                
                System.out.println("   📊 RESULTADOS:");
                System.out.println("      Consultas realizadas: " + numConsultas);
                System.out.println("      Consultas exitosas: " + exitosas);
                System.out.println("      Servicio inactivo: " + servicioInactivo);
                System.out.println("      Errores: " + errores);
                System.out.println("      Tiempo promedio: " + (tiempoTotal / numConsultas) + "ms");
                System.out.println("      Tiempo total: " + tiempoTotal + "ms");
            }
            
            System.out.println();
            System.out.println("🏁 ===== TESTS COMPLETADOS =====");
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ ERROR GENERAL: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
        
        System.exit(status);
    }
} 