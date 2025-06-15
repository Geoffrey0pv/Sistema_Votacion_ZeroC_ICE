import Demo.*;
import com.zeroc.Ice.*;

/**
 * 🗳️ Test del Endpoint de Lote de Votos
 * 
 * Consume la interfaz IProcesadorLoteVotos del servidor nacional
 * para probar el procesamiento de lotes de votos en formato JSON.
 */
public class TestLoteVotosEndpoint {
    
    private static final String SERVIDOR_NACIONAL_ENDPOINT = "IProcesadorLoteVotos:default -h localhost -p 9090";
    
    public static void main(String[] args) {
        System.out.println("🗳️ Test del Endpoint de Lote de Votos");
        System.out.println("=====================================");
        
        Communicator communicator = null;
        boolean allTestsPassed = true;
        
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servidor nacional
            ObjectPrx base = communicator.stringToProxy(SERVIDOR_NACIONAL_ENDPOINT);
            IProcesadorLoteVotosPrx procesadorLote = IProcesadorLoteVotosPrx.checkedCast(base);
            
            if (procesadorLote == null) {
                System.out.println("❌ No se pudo conectar al servidor nacional");
                System.out.println("   Endpoint: " + SERVIDOR_NACIONAL_ENDPOINT);
                System.out.println("   ⚠️  Asegúrate de que el servidor nacional esté ejecutándose");
                return;
            }
            
            System.out.println("✅ Conectado al servidor nacional");
            
            // Test de disponibilidad
            allTestsPassed &= testDisponibilidad(procesadorLote);
            
            // Test 1: Lote válido básico
            allTestsPassed &= testLoteValido(procesadorLote);
            
            // Test 2: Lote con múltiples mesas
            allTestsPassed &= testLoteMultiplesMesas(procesadorLote);
            
            // Test 3: JSON inválido
            allTestsPassed &= testJsonInvalido(procesadorLote);
            
            // Test 4: Lote grande
            allTestsPassed &= testLoteGrande(procesadorLote);
            
            // Test 5: Estadísticas
            allTestsPassed &= testEstadisticas(procesadorLote);
            
        } catch (LocalException e) {
            System.out.println("❌ Error de conexión ICE: " + e.getMessage());
            System.out.println("   ⚠️  Verifica que el servidor nacional esté ejecutándose en el puerto 9090");
            allTestsPassed = false;
            
        } catch (Exception e) {
            System.out.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            allTestsPassed = false;
            
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
        
        // Resultado final
        System.out.println("\n=====================================");
        if (allTestsPassed) {
            System.out.println("🎉 TODOS LOS TESTS DE LOTE DE VOTOS PASARON");
            System.exit(0);
        } else {
            System.out.println("❌ ALGUNOS TESTS FALLARON");
            System.exit(1);
        }
    }
    
    /**
     * Test de disponibilidad del servicio
     */
    private static boolean testDisponibilidad(IProcesadorLoteVotosPrx procesador) {
        System.out.println("\n🔍 Test: Verificación de disponibilidad");
        
        try {
            boolean disponible = procesador.verificarDisponibilidad();
            
            if (disponible) {
                System.out.println("   ✅ Servicio disponible");
                return true;
            } else {
                System.out.println("   ⚠️  Servicio no disponible (pero responde)");
                return true; // Aún así es una respuesta válida
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error verificando disponibilidad: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test 1: Lote válido básico
     */
    private static boolean testLoteValido(IProcesadorLoteVotosPrx procesador) {
        System.out.println("\n🔍 Test 1: Lote válido básico");
        
        try {
            long timestamp = System.currentTimeMillis();
            
            String jsonVotos = "[\n" +
                "  {\n" +
                "    \"mesa_id\": \"MESA-TEST-" + timestamp + "\",\n" +
                "    \"candidato_id\": \"CAND-001\",\n" +
                "    \"timestamp\": \"2025-01-15T10:30:00\",\n" +
                "    \"municipio\": \"Bogotá\",\n" +
                "    \"departamento\": \"Cundinamarca\",\n" +
                "    \"hash_verificacion\": \"hash_" + timestamp + "_001\",\n" +
                "    \"firma_mesa\": \"firma_valida_001\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"mesa_id\": \"MESA-TEST-" + timestamp + "\",\n" +
                "    \"candidato_id\": \"CAND-002\",\n" +
                "    \"timestamp\": \"2025-01-15T10:31:00\",\n" +
                "    \"municipio\": \"Bogotá\",\n" +
                "    \"departamento\": \"Cundinamarca\",\n" +
                "    \"hash_verificacion\": \"hash_" + timestamp + "_002\",\n" +
                "    \"firma_mesa\": \"firma_valida_002\"\n" +
                "  }\n" +
                "]";
            
            System.out.println("   📤 Enviando lote de 2 votos...");
            
            CallbackLoteVotos callback = new CallbackLoteVotos();
            procesador.procesarLoteVotos(jsonVotos, callback);
            
            // Esperar respuesta
            Thread.sleep(3000);
            
            if (callback.recibido) {
                System.out.println("   📝 Resultado: " + callback.resultado.mensaje);
                System.out.println("   📊 Total votos: " + callback.resultado.totalVotos);
                System.out.println("   📊 Votos encolados: " + callback.resultado.votosEncolados);
                
                if (callback.resultado.exito) {
                    System.out.println("   ✅ Lote procesado exitosamente");
                    return true;
                } else {
                    System.out.println("   ❌ Lote rechazado");
                    return false;
                }
            } else {
                System.out.println("   ❌ Sin respuesta del servidor");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test 2: Lote con múltiples mesas
     */
    private static boolean testLoteMultiplesMesas(IProcesadorLoteVotosPrx procesador) {
        System.out.println("\n🔍 Test 2: Lote con múltiples mesas");
        
        try {
            long timestamp = System.currentTimeMillis();
            
            String jsonVotos = "[\n" +
                "  {\n" +
                "    \"mesa_id\": \"MESA-MULTI-001-" + timestamp + "\",\n" +
                "    \"candidato_id\": \"PETRO2025\",\n" +
                "    \"timestamp\": \"2025-01-15T14:00:00\",\n" +
                "    \"municipio\": \"Medellín\",\n" +
                "    \"departamento\": \"Antioquia\",\n" +
                "    \"hash_verificacion\": \"hash_multi_" + timestamp + "_001\",\n" +
                "    \"firma_mesa\": \"firma_medellin_001\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"mesa_id\": \"MESA-MULTI-002-" + timestamp + "\",\n" +
                "    \"candidato_id\": \"FICO2025\",\n" +
                "    \"timestamp\": \"2025-01-15T14:05:00\",\n" +
                "    \"municipio\": \"Cali\",\n" +
                "    \"departamento\": \"Valle del Cauca\",\n" +
                "    \"hash_verificacion\": \"hash_multi_" + timestamp + "_002\",\n" +
                "    \"firma_mesa\": \"firma_cali_001\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"mesa_id\": \"MESA-MULTI-003-" + timestamp + "\",\n" +
                "    \"candidato_id\": \"RODOLFO2025\",\n" +
                "    \"timestamp\": \"2025-01-15T14:10:00\",\n" +
                "    \"municipio\": \"Barranquilla\",\n" +
                "    \"departamento\": \"Atlántico\",\n" +
                "    \"hash_verificacion\": \"hash_multi_" + timestamp + "_003\",\n" +
                "    \"firma_mesa\": \"firma_barranquilla_001\"\n" +
                "  }\n" +
                "]";
            
            System.out.println("   📤 Enviando lote de 3 votos de diferentes ciudades...");
            
            CallbackLoteVotos callback = new CallbackLoteVotos();
            procesador.procesarLoteVotos(jsonVotos, callback);
            
            Thread.sleep(3000);
            
            if (callback.recibido && callback.resultado.exito) {
                System.out.println("   ✅ Lote multi-mesa procesado: " + callback.resultado.totalVotos + " votos");
                return true;
            } else {
                System.out.println("   ❌ Error procesando lote multi-mesa");
                if (callback.recibido) {
                    System.out.println("   📝 Mensaje: " + callback.resultado.mensaje);
                }
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test 3: JSON inválido
     */
    private static boolean testJsonInvalido(IProcesadorLoteVotosPrx procesador) {
        System.out.println("\n🔍 Test 3: Manejo de JSON inválido");
        
        try {
            String jsonInvalido = "{ esto no es json válido }";
            
            System.out.println("   📤 Enviando JSON inválido...");
            
            CallbackLoteVotos callback = new CallbackLoteVotos();
            procesador.procesarLoteVotos(jsonInvalido, callback);
            
            Thread.sleep(2000);
            
            if (callback.recibido) {
                System.out.println("   📝 Respuesta: " + callback.resultado.mensaje);
                
                if (!callback.resultado.exito) {
                    System.out.println("   ✅ JSON inválido rechazado correctamente");
                    return true;
                } else {
                    System.out.println("   ❌ JSON inválido fue aceptado (error)");
                    return false;
                }
            } else {
                System.out.println("   ❌ Sin respuesta del servidor");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test 4: Lote grande
     */
    private static boolean testLoteGrande(IProcesadorLoteVotosPrx procesador) {
        System.out.println("\n🔍 Test 4: Lote grande (10 votos)");
        
        try {
            long timestamp = System.currentTimeMillis();
            StringBuilder jsonBuilder = new StringBuilder("[\n");
            
            for (int i = 1; i <= 10; i++) {
                if (i > 1) jsonBuilder.append(",\n");
                
                jsonBuilder.append("  {\n")
                    .append("    \"mesa_id\": \"MESA-GRANDE-").append(String.format("%03d", i)).append("-").append(timestamp).append("\",\n")
                    .append("    \"candidato_id\": \"CAND-").append(String.format("%03d", (i % 5) + 1)).append("\",\n")
                    .append("    \"timestamp\": \"2025-01-15T").append(String.format("%02d", 10 + (i % 12))).append(":").append(String.format("%02d", i * 2)).append(":00\",\n")
                    .append("    \"municipio\": \"Ciudad").append(i % 3 + 1).append("\",\n")
                    .append("    \"departamento\": \"Departamento").append(i % 2 + 1).append("\",\n")
                    .append("    \"hash_verificacion\": \"hash_grande_").append(timestamp).append("_").append(String.format("%03d", i)).append("\",\n")
                    .append("    \"firma_mesa\": \"firma_grande_").append(i).append("\"\n")
                    .append("  }");
            }
            
            jsonBuilder.append("\n]");
            String jsonVotos = jsonBuilder.toString();
            
            System.out.println("   📤 Enviando lote grande de 10 votos...");
            
            CallbackLoteVotos callback = new CallbackLoteVotos();
            procesador.procesarLoteVotos(jsonVotos, callback);
            
            Thread.sleep(5000); // Más tiempo para lote grande
            
            if (callback.recibido) {
                System.out.println("   📊 Resultado: " + callback.resultado.totalVotos + " votos procesados");
                
                if (callback.resultado.exito && callback.resultado.totalVotos == 10) {
                    System.out.println("   ✅ Lote grande procesado exitosamente");
                    return true;
                } else {
                    System.out.println("   ❌ Error procesando lote grande");
                    System.out.println("   📝 Mensaje: " + callback.resultado.mensaje);
                    return false;
                }
            } else {
                System.out.println("   ❌ Sin respuesta del servidor");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test 5: Estadísticas
     */
    private static boolean testEstadisticas(IProcesadorLoteVotosPrx procesador) {
        System.out.println("\n🔍 Test 5: Consulta de estadísticas");
        
        try {
            EstadisticasVotos stats = procesador.obtenerEstadisticas();
            
            System.out.println("   📊 Estadísticas del sistema:");
            System.out.println("      Total votos: " + stats.totalVotos);
            System.out.println("      Total mesas: " + stats.totalMesas);
            System.out.println("      Total candidatos: " + stats.totalCandidatos);
            System.out.println("      Total municipios: " + stats.totalMunicipios);
            System.out.println("      Primer voto: " + stats.primerVoto);
            System.out.println("      Último voto: " + stats.ultimoVoto);
            
            System.out.println("   ✅ Estadísticas obtenidas exitosamente");
            return true;
            
        } catch (Exception e) {
            System.out.println("   ❌ Error obteniendo estadísticas: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Callback para recibir confirmaciones de lotes de votos
     */
    static class CallbackLoteVotos implements IConfirmacionLoteVotos {
        public boolean recibido = false;
        public ResultadoProcesamiento resultado;
        
        @Override
        public void recibirConfirmacion(ResultadoProcesamiento resultado, Current current) {
            this.resultado = resultado;
            this.recibido = true;
        }
    }
} 