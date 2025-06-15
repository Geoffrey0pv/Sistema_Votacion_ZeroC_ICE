import Demo.*;
import com.zeroc.Ice.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🚀 Test de Rendimiento - Procesamiento de Lote de Votos
 * 
 * Prueba el rendimiento del endpoint bajo diferentes cargas:
 * - ⚡ Tiempo de respuesta promedio
 * - 🔥 Procesamiento concurrente
 * - 📈 Throughput (votos por segundo)
 * - 💪 Test de estrés con múltiples clientes
 */
public class TestRendimientoLoteVotos {
    
    private static IProcesadorLoteVotosPrx procesadorPrx;
    private static Communicator communicator;
    
    public static void main(String[] args) {
        System.out.println("🚀 === TEST DE RENDIMIENTO - LOTE DE VOTOS ===");
        
        try {
            // Inicializar conexión
            if (!inicializarConexion(args)) {
                System.exit(1);
            }
            
            // Ejecutar tests de rendimiento
            System.out.println("\n📊 Iniciando tests de rendimiento...");
            
            // Test 1: Tiempo de respuesta básico
            testTiempoRespuesta();
            
            // Test 2: Throughput con lotes pequeños
            testThroughputLotesPequenos();
            
            // Test 3: Throughput con lotes grandes
            testThroughputLotesGrandes();
            
            // Test 4: Procesamiento concurrente
            testProcesoConcurrente();
            
            // Test 5: Test de estrés
            testEstres();
            
            System.out.println("\n🎯 === TESTS DE RENDIMIENTO COMPLETADOS ===");
            
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }
    
    /**
     * Inicializar conexión
     */
    private static boolean inicializarConexion(String[] args) {
        try {
            communicator = Util.initialize(args);
            ObjectPrx base = communicator.stringToProxy("ProcesadorLoteVotos:tcp -h localhost -p 9090");
            procesadorPrx = IProcesadorLoteVotosPrx.checkedCast(base);
            
            if (procesadorPrx == null) {
                System.out.println("❌ No se pudo conectar al servicio");
                return false;
            }
            
            if (!procesadorPrx.verificarDisponibilidad()) {
                System.out.println("❌ El servicio no está disponible");
                return false;
            }
            
            System.out.println("✅ Conexión establecida para tests de rendimiento");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Error conectando: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test 1: Tiempo de respuesta básico
     */
    private static void testTiempoRespuesta() {
        System.out.println("\n⏱️ Test 1: Midiendo tiempo de respuesta básico...");
        
        try {
            String jsonVoto = crearVotoJSON("TIEMPO-001", "CAND-001");
            
            long inicio = System.currentTimeMillis();
            
            CallbackRendimiento callback = new CallbackRendimiento();
            procesadorPrx.procesarLoteVotos(jsonVoto, callback);
            
            // Esperar respuesta
            while (!callback.recibido && (System.currentTimeMillis() - inicio) < 10000) {
                Thread.sleep(100);
            }
            
            long tiempoTotal = System.currentTimeMillis() - inicio;
            
            if (callback.recibido) {
                System.out.println("   ✅ Tiempo de respuesta: " + tiempoTotal + "ms");
                if (tiempoTotal < 1000) {
                    System.out.println("   🚀 Rendimiento: EXCELENTE (< 1s)");
                } else if (tiempoTotal < 3000) {
                    System.out.println("   👍 Rendimiento: BUENO (< 3s)");
                } else {
                    System.out.println("   ⚠️ Rendimiento: LENTO (> 3s)");
                }
            } else {
                System.out.println("   ❌ Timeout - No se recibió respuesta en 10s");
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 2: Throughput con lotes pequeños
     */
    private static void testThroughputLotesPequenos() {
        System.out.println("\n📈 Test 2: Throughput con lotes pequeños (1 voto c/u)...");
        
        try {
            int numLotes = 10;
            long inicio = System.currentTimeMillis();
            AtomicInteger completados = new AtomicInteger(0);
            
            for (int i = 1; i <= numLotes; i++) {
                String jsonVoto = crearVotoJSON("SMALL-" + i, "CAND-" + (i % 3 + 1));
                
                CallbackRendimiento callback = new CallbackRendimiento() {
                    @Override
                    public void recibirConfirmacion(ResultadoProcesamiento r, Current current) {
                        super.recibirConfirmacion(r, current);
                        completados.incrementAndGet();
                    }
                };
                
                procesadorPrx.procesarLoteVotos(jsonVoto, callback);
                Thread.sleep(100); // Pequeña pausa entre envíos
            }
            
            // Esperar que todos completen
            while (completados.get() < numLotes && (System.currentTimeMillis() - inicio) < 30000) {
                Thread.sleep(500);
            }
            
            long tiempoTotal = System.currentTimeMillis() - inicio;
            double throughput = (double) completados.get() / (tiempoTotal / 1000.0);
            
            System.out.println("   📊 Lotes completados: " + completados.get() + "/" + numLotes);
            System.out.println("   ⏱️ Tiempo total: " + tiempoTotal + "ms");
            System.out.println("   🚀 Throughput: " + String.format("%.2f", throughput) + " lotes/segundo");
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 3: Throughput con lotes grandes
     */
    private static void testThroughputLotesGrandes() {
        System.out.println("\n📈 Test 3: Throughput con lotes grandes (5 votos c/u)...");
        
        try {
            int numLotes = 5;
            long inicio = System.currentTimeMillis();
            AtomicInteger completados = new AtomicInteger(0);
            AtomicInteger totalVotos = new AtomicInteger(0);
            
            for (int i = 1; i <= numLotes; i++) {
                String jsonLote = crearLoteGrandeJSON("BIG-" + i, 5);
                
                CallbackRendimiento callback = new CallbackRendimiento() {
                    @Override
                    public void recibirConfirmacion(ResultadoProcesamiento r, Current current) {
                        super.recibirConfirmacion(r, current);
                        completados.incrementAndGet();
                        totalVotos.addAndGet(r.totalVotos);
                    }
                };
                
                procesadorPrx.procesarLoteVotos(jsonLote, callback);
                Thread.sleep(200); // Pausa entre lotes grandes
            }
            
            // Esperar que todos completen
            while (completados.get() < numLotes && (System.currentTimeMillis() - inicio) < 45000) {
                Thread.sleep(1000);
            }
            
            long tiempoTotal = System.currentTimeMillis() - inicio;
            double throughputLotes = (double) completados.get() / (tiempoTotal / 1000.0);
            double throughputVotos = (double) totalVotos.get() / (tiempoTotal / 1000.0);
            
            System.out.println("   📊 Lotes completados: " + completados.get() + "/" + numLotes);
            System.out.println("   🗳️ Total votos procesados: " + totalVotos.get());
            System.out.println("   ⏱️ Tiempo total: " + tiempoTotal + "ms");
            System.out.println("   🚀 Throughput lotes: " + String.format("%.2f", throughputLotes) + " lotes/segundo");
            System.out.println("   🗳️ Throughput votos: " + String.format("%.2f", throughputVotos) + " votos/segundo");
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 4: Procesamiento concurrente
     */
    private static void testProcesoConcurrente() {
        System.out.println("\n🔄 Test 4: Procesamiento concurrente (3 hilos simultáneos)...");
        
        try {
            int numHilos = 3;
            int lotesPorHilo = 3;
            ExecutorService executor = Executors.newFixedThreadPool(numHilos);
            CountDownLatch latch = new CountDownLatch(numHilos);
            AtomicInteger totalCompletados = new AtomicInteger(0);
            
            long inicio = System.currentTimeMillis();
            
            for (int hilo = 1; hilo <= numHilos; hilo++) {
                final int hiloId = hilo;
                
                executor.submit(() -> {
                    try {
                        int completadosHilo = 0;
                        
                        for (int lote = 1; lote <= lotesPorHilo; lote++) {
                            String jsonVoto = crearVotoJSON("CONCURRENT-H" + hiloId + "-L" + lote, "CAND-" + hiloId);
                            
                            CallbackRendimiento callback = new CallbackRendimiento();
                            procesadorPrx.procesarLoteVotos(jsonVoto, callback);
                            
                            // Esperar respuesta de este lote
                            long tiempoEspera = System.currentTimeMillis();
                            while (!callback.recibido && (System.currentTimeMillis() - tiempoEspera) < 5000) {
                                Thread.sleep(100);
                            }
                            
                            if (callback.recibido && callback.resultado.exito) {
                                completadosHilo++;
                            }
                        }
                        
                        totalCompletados.addAndGet(completadosHilo);
                        System.out.println("   🧵 Hilo " + hiloId + " completó " + completadosHilo + "/" + lotesPorHilo + " lotes");
                        
                    } catch (Exception e) {
                        System.out.println("   ❌ Error en hilo " + hiloId + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Esperar que todos los hilos terminen
            latch.await(60, TimeUnit.SECONDS);
            executor.shutdown();
            
            long tiempoTotal = System.currentTimeMillis() - inicio;
            int totalEsperado = numHilos * lotesPorHilo;
            
            System.out.println("   📊 Total completados: " + totalCompletados.get() + "/" + totalEsperado);
            System.out.println("   ⏱️ Tiempo total: " + tiempoTotal + "ms");
            System.out.println("   🎯 Éxito concurrente: " + (totalCompletados.get() == totalEsperado ? "✅ SÍ" : "❌ NO"));
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 5: Test de estrés
     */
    private static void testEstres() {
        System.out.println("\n💪 Test 5: Test de estrés (20 lotes rápidos)...");
        
        try {
            int numLotes = 20;
            AtomicInteger exitosos = new AtomicInteger(0);
            AtomicInteger fallidos = new AtomicInteger(0);
            
            long inicio = System.currentTimeMillis();
            
            for (int i = 1; i <= numLotes; i++) {
                String jsonVoto = crearVotoJSON("STRESS-" + i, "CAND-" + (i % 5 + 1));
                
                CallbackRendimiento callback = new CallbackRendimiento() {
                    @Override
                    public void recibirConfirmacion(ResultadoProcesamiento r, Current current) {
                        super.recibirConfirmacion(r, current);
                        if (r.exito) {
                            exitosos.incrementAndGet();
                        } else {
                            fallidos.incrementAndGet();
                        }
                    }
                };
                
                procesadorPrx.procesarLoteVotos(jsonVoto, callback);
                // Sin pausa - envío rápido para estrés
            }
            
            // Esperar que la mayoría complete
            Thread.sleep(15000);
            
            long tiempoTotal = System.currentTimeMillis() - inicio;
            int totalProcesados = exitosos.get() + fallidos.get();
            double porcentajeExito = totalProcesados > 0 ? (double) exitosos.get() / totalProcesados * 100 : 0;
            
            System.out.println("   📊 Lotes exitosos: " + exitosos.get());
            System.out.println("   ❌ Lotes fallidos: " + fallidos.get());
            System.out.println("   📈 Porcentaje de éxito: " + String.format("%.1f", porcentajeExito) + "%");
            System.out.println("   ⏱️ Tiempo total: " + tiempoTotal + "ms");
            
            if (porcentajeExito >= 80) {
                System.out.println("   💪 Resistencia al estrés: ✅ EXCELENTE");
            } else if (porcentajeExito >= 60) {
                System.out.println("   💪 Resistencia al estrés: 👍 BUENA");
            } else {
                System.out.println("   💪 Resistencia al estrés: ⚠️ NECESITA MEJORAS");
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Crear JSON de un solo voto
     */
    private static String crearVotoJSON(String mesaId, String candidatoId) {
        return "[\n" +
            "  {\n" +
            "    \"mesa_id\": \"" + mesaId + "\",\n" +
            "    \"candidato_id\": \"" + candidatoId + "\",\n" +
            "    \"timestamp\": \"2025-01-15T" + String.format("%02d", (int)(Math.random() * 24)) + ":00:00\",\n" +
            "    \"municipio\": \"TestCity\",\n" +
            "    \"departamento\": \"TestDepto\",\n" +
            "    \"hash_verificacion\": \"hash_" + mesaId + "\",\n" +
            "    \"firma_mesa\": \"firma_" + mesaId + "\"\n" +
            "  }\n" +
            "]";
    }
    
    /**
     * Crear JSON de lote grande
     */
    private static String crearLoteGrandeJSON(String prefijo, int numVotos) {
        StringBuilder json = new StringBuilder("[\n");
        
        for (int i = 1; i <= numVotos; i++) {
            if (i > 1) json.append(",\n");
            json.append("  {\n")
                .append("    \"mesa_id\": \"").append(prefijo).append("-").append(i).append("\",\n")
                .append("    \"candidato_id\": \"CAND-").append((i % 3) + 1).append("\",\n")
                .append("    \"timestamp\": \"2025-01-15T").append(String.format("%02d", 10 + i)).append(":00:00\",\n")
                .append("    \"municipio\": \"BigCity\",\n")
                .append("    \"departamento\": \"BigDepto\",\n")
                .append("    \"hash_verificacion\": \"hash_").append(prefijo).append("_").append(i).append("\",\n")
                .append("    \"firma_mesa\": \"firma_").append(prefijo).append("_").append(i).append("\"\n")
                .append("  }");
        }
        
        json.append("\n]");
        return json.toString();
    }
    
    /**
     * Callback para tests de rendimiento
     */
    static class CallbackRendimiento implements IConfirmacionLoteVotos {
        public boolean recibido = false;
        public ResultadoProcesamiento resultado;
        
        @Override
        public void recibirConfirmacion(ResultadoProcesamiento r, Current current) {
            this.resultado = r;
            this.recibido = true;
        }
    }
} 