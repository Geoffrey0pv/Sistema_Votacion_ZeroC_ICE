import Demo.*;
import com.zeroc.Ice.*;

/**
 * 🧪 Test de Casos de Uso - Procesamiento de Lote de Votos
 * 
 * Prueba diferentes escenarios del endpoint de procesamiento de lotes:
 * - ✅ Lote válido con múltiples votos
 * - ❌ JSON inválido o malformado
 * - 📭 Lote vacío
 * - 🔄 Múltiples lotes consecutivos
 * - ⚠️ Manejo de errores y timeouts
 */
public class TestCasosUsoLoteVotos {
    
    private static IProcesadorLoteVotosPrx procesadorPrx;
    private static Communicator communicator;
    
    public static void main(String[] args) {
        System.out.println("🧪 === TEST CASOS DE USO - LOTE DE VOTOS ===");
        
        try {
            // Inicializar conexión
            if (!inicializarConexion(args)) {
                System.exit(1);
            }
            
            // Ejecutar casos de uso
            int testsPasados = 0;
            int testsFallidos = 0;
            
            // Caso 1: Lote válido básico
            if (testLoteValido()) {
                testsPasados++;
                System.out.println("✅ Caso 1: Lote válido - PASÓ");
            } else {
                testsFallidos++;
                System.out.println("❌ Caso 1: Lote válido - FALLÓ");
            }
            
            // Caso 2: JSON inválido
            if (testJSONInvalido()) {
                testsPasados++;
                System.out.println("✅ Caso 2: JSON inválido - PASÓ");
            } else {
                testsFallidos++;
                System.out.println("❌ Caso 2: JSON inválido - FALLÓ");
            }
            
            // Caso 3: Lote vacío
            if (testLoteVacio()) {
                testsPasados++;
                System.out.println("✅ Caso 3: Lote vacío - PASÓ");
            } else {
                testsFallidos++;
                System.out.println("❌ Caso 3: Lote vacío - FALLÓ");
            }
            
            // Caso 4: Lote grande (múltiples votos)
            if (testLoteGrande()) {
                testsPasados++;
                System.out.println("✅ Caso 4: Lote grande - PASÓ");
            } else {
                testsFallidos++;
                System.out.println("❌ Caso 4: Lote grande - FALLÓ");
            }
            
            // Caso 5: Múltiples lotes consecutivos
            if (testMultiplesLotes()) {
                testsPasados++;
                System.out.println("✅ Caso 5: Múltiples lotes - PASÓ");
            } else {
                testsFallidos++;
                System.out.println("❌ Caso 5: Múltiples lotes - FALLÓ");
            }
            
            // Caso 6: Verificar estadísticas
            if (testEstadisticas()) {
                testsPasados++;
                System.out.println("✅ Caso 6: Estadísticas - PASÓ");
            } else {
                testsFallidos++;
                System.out.println("❌ Caso 6: Estadísticas - FALLÓ");
            }
            
            // Resumen final
            System.out.println("\n📊 === RESUMEN DE CASOS DE USO ===");
            System.out.println("✅ Tests exitosos: " + testsPasados);
            System.out.println("❌ Tests fallidos: " + testsFallidos);
            System.out.println("📈 Porcentaje de éxito: " + (testsPasados * 100 / (testsPasados + testsFallidos)) + "%");
            
            if (testsFallidos == 0) {
                System.out.println("🎉 ¡TODOS LOS CASOS DE USO PASARON!");
            } else {
                System.out.println("⚠️ Algunos casos de uso fallaron");
            }
            
        } catch (Exception e) {
            System.out.println("❌ ERROR GENERAL: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }
    
    /**
     * Inicializar conexión con el servidor
     */
    private static boolean inicializarConexion(String[] args) {
        try {
            communicator = Util.initialize(args);
            ObjectPrx base = communicator.stringToProxy("ProcesadorLoteVotos:tcp -h localhost -p 9090");
            procesadorPrx = IProcesadorLoteVotosPrx.checkedCast(base);
            
            if (procesadorPrx == null) {
                System.out.println("❌ No se pudo conectar al servicio");
                System.out.println("💡 Asegúrate de que el servidor nacional esté ejecutándose");
                return false;
            }
            
            // Verificar disponibilidad
            boolean disponible = procesadorPrx.verificarDisponibilidad();
            if (!disponible) {
                System.out.println("❌ El servicio no está disponible");
                return false;
            }
            
            System.out.println("✅ Conexión establecida correctamente");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Error conectando: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Caso 1: Test de lote válido básico
     */
    private static boolean testLoteValido() {
        System.out.println("\n🔍 Caso 1: Probando lote válido básico...");
        
        try {
            String jsonVotos = "[\n" +
                "  {\n" +
                "    \"mesa_id\": \"MESA-001\",\n" +
                "    \"candidato_id\": \"CAND-001\",\n" +
                "    \"timestamp\": \"2025-01-15T10:30:00\",\n" +
                "    \"municipio\": \"Bogotá\",\n" +
                "    \"departamento\": \"Cundinamarca\",\n" +
                "    \"hash_verificacion\": \"abc123def456\",\n" +
                "    \"firma_mesa\": \"firma_valida_001\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"mesa_id\": \"MESA-001\",\n" +
                "    \"candidato_id\": \"CAND-002\",\n" +
                "    \"timestamp\": \"2025-01-15T10:31:00\",\n" +
                "    \"municipio\": \"Bogotá\",\n" +
                "    \"departamento\": \"Cundinamarca\",\n" +
                "    \"hash_verificacion\": \"def456ghi789\",\n" +
                "    \"firma_mesa\": \"firma_valida_002\"\n" +
                "  }\n" +
                "]";
            
            CallbackTest callback = new CallbackTest();
            procesadorPrx.procesarLoteVotos(jsonVotos, callback);
            
            // Esperar respuesta
            Thread.sleep(3000);
            
            if (callback.recibido && callback.resultado.exito) {
                System.out.println("   ✅ Lote procesado: " + callback.resultado.totalVotos + " votos");
                return true;
            } else {
                System.out.println("   ❌ Lote rechazado: " + (callback.recibido ? callback.resultado.mensaje : "Sin respuesta"));
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Caso 2: Test de JSON inválido
     */
    private static boolean testJSONInvalido() {
        System.out.println("\n🔍 Caso 2: Probando JSON inválido...");
        
        try {
            String jsonInvalido = "{ esto no es json válido [";
            
            CallbackTest callback = new CallbackTest();
            procesadorPrx.procesarLoteVotos(jsonInvalido, callback);
            
            Thread.sleep(2000);
            
            // Esperamos que falle
            if (callback.recibido && !callback.resultado.exito) {
                System.out.println("   ✅ JSON inválido rechazado correctamente");
                return true;
            } else {
                System.out.println("   ❌ JSON inválido no fue rechazado");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Caso 3: Test de lote vacío
     */
    private static boolean testLoteVacio() {
        System.out.println("\n🔍 Caso 3: Probando lote vacío...");
        
        try {
            String jsonVacio = "[]";
            
            CallbackTest callback = new CallbackTest();
            procesadorPrx.procesarLoteVotos(jsonVacio, callback);
            
            Thread.sleep(2000);
            
            if (callback.recibido) {
                System.out.println("   ✅ Lote vacío manejado: " + callback.resultado.mensaje);
                return true;
            } else {
                System.out.println("   ❌ No se recibió respuesta para lote vacío");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Caso 4: Test de lote grande
     */
    private static boolean testLoteGrande() {
        System.out.println("\n🔍 Caso 4: Probando lote grande (10 votos)...");
        
        try {
            StringBuilder jsonBuilder = new StringBuilder("[\n");
            
            for (int i = 1; i <= 10; i++) {
                if (i > 1) jsonBuilder.append(",\n");
                jsonBuilder.append("  {\n")
                    .append("    \"mesa_id\": \"MESA-").append(String.format("%03d", i)).append("\",\n")
                    .append("    \"candidato_id\": \"CAND-").append((i % 3) + 1).append("\",\n")
                    .append("    \"timestamp\": \"2025-01-15T").append(String.format("%02d", 10 + i)).append(":00:00\",\n")
                    .append("    \"municipio\": \"Ciudad").append(i).append("\",\n")
                    .append("    \"departamento\": \"Depto").append((i % 5) + 1).append("\",\n")
                    .append("    \"hash_verificacion\": \"hash").append(i).append("abc\",\n")
                    .append("    \"firma_mesa\": \"firma").append(i).append("\"\n")
                    .append("  }");
            }
            
            jsonBuilder.append("\n]");
            
            CallbackTest callback = new CallbackTest();
            procesadorPrx.procesarLoteVotos(jsonBuilder.toString(), callback);
            
            Thread.sleep(5000); // Más tiempo para lote grande
            
            if (callback.recibido && callback.resultado.totalVotos == 10) {
                System.out.println("   ✅ Lote grande procesado: " + callback.resultado.votosEncolados + "/10 votos");
                return true;
            } else {
                System.out.println("   ❌ Lote grande falló: " + (callback.recibido ? callback.resultado.mensaje : "Sin respuesta"));
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Caso 5: Test de múltiples lotes consecutivos
     */
    private static boolean testMultiplesLotes() {
        System.out.println("\n🔍 Caso 5: Probando múltiples lotes consecutivos...");
        
        try {
            int lotesExitosos = 0;
            
            for (int lote = 1; lote <= 3; lote++) {
                String jsonVoto = "[\n" +
                    "  {\n" +
                    "    \"mesa_id\": \"MESA-MULTI-" + lote + "\",\n" +
                    "    \"candidato_id\": \"CAND-" + lote + "\",\n" +
                    "    \"timestamp\": \"2025-01-15T1" + lote + ":00:00\",\n" +
                    "    \"municipio\": \"MultiCiudad\",\n" +
                    "    \"departamento\": \"MultiDepto\",\n" +
                    "    \"hash_verificacion\": \"multi" + lote + "hash\",\n" +
                    "    \"firma_mesa\": \"multi" + lote + "firma\"\n" +
                    "  }\n" +
                    "]";
                
                CallbackTest callback = new CallbackTest();
                procesadorPrx.procesarLoteVotos(jsonVoto, callback);
                
                Thread.sleep(2000);
                
                if (callback.recibido && callback.resultado.exito) {
                    lotesExitosos++;
                    System.out.println("   ✅ Lote " + lote + "/3 procesado");
                } else {
                    System.out.println("   ❌ Lote " + lote + "/3 falló");
                }
            }
            
            boolean exito = lotesExitosos == 3;
            System.out.println("   📊 Resultado: " + lotesExitosos + "/3 lotes exitosos");
            return exito;
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Caso 6: Test de estadísticas
     */
    private static boolean testEstadisticas() {
        System.out.println("\n🔍 Caso 6: Probando obtención de estadísticas...");
        
        try {
            EstadisticasVotos stats = procesadorPrx.obtenerEstadisticas();
            
            System.out.println("   📊 Total votos: " + stats.totalVotos);
            System.out.println("   🗳️ Total mesas: " + stats.totalMesas);
            System.out.println("   👥 Total candidatos: " + stats.totalCandidatos);
            System.out.println("   🏛️ Total municipios: " + stats.totalMunicipios);
            
            // Las estadísticas deben tener valores >= 0
            boolean valido = stats.totalVotos >= 0 && 
                           stats.totalMesas >= 0 && 
                           stats.totalCandidatos >= 0 && 
                           stats.totalMunicipios >= 0;
            
            if (valido) {
                System.out.println("   ✅ Estadísticas obtenidas correctamente");
                return true;
            } else {
                System.out.println("   ❌ Estadísticas con valores inválidos");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error obteniendo estadísticas: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Callback simple para recibir confirmaciones
     */
    static class CallbackTest implements IConfirmacionLoteVotos {
        public boolean recibido = false;
        public ResultadoProcesamiento resultado;
        
        @Override
        public void recibirConfirmacion(ResultadoProcesamiento r, Current current) {
            this.resultado = r;
            this.recibido = true;
        }
    }
} 