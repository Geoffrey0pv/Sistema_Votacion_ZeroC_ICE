import Demo.*;
import com.zeroc.Ice.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Test class para consultar ciudadanos por departamentos
 * Conecta al servidor nacional y realiza consultas de prueba
 * Incluye tests de optimización con paginación y límites
 */
public class TestConsultaCiudadanos {
    
    public static void main(String[] args) {
        Communicator communicator = null;
        int status = 0;
        
        try {
            System.out.println("🧪 ===== TEST CONSULTA CIUDADANOS OPTIMIZADO =====");
            System.out.println();
            
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servicio ConsultaCiudadanos en el servidor nacional
            String endpoint = "ConsultaCiudadanos:tcp -h localhost -p 9090";
            System.out.println("🔌 Conectando a: " + endpoint);
            
            ObjectPrx base = communicator.stringToProxy(endpoint);
            IConsultaCiudadanosPrx consultaCiudadanos = IConsultaCiudadanosPrx.checkedCast(base);
            
            if (consultaCiudadanos == null) {
                System.err.println("❌ ERROR: No se pudo conectar al servicio ConsultaCiudadanos");
                System.err.println("   Verifique que el servidor nacional esté ejecutándose");
                return;
            }
            
            System.out.println("✅ Conectado exitosamente al servicio ConsultaCiudadanos");
            System.out.println();
            
            // Test 1: Verificar conexión a base de datos
            System.out.println("🔧 TEST 1: Verificando conexión a base de datos...");
            boolean conexionOK = consultaCiudadanos.verificarConexionBD();
            System.out.println("   Resultado: " + (conexionOK ? "✅ CONEXIÓN OK" : "❌ ERROR DE CONEXIÓN"));
            System.out.println();
            
            if (!conexionOK) {
                System.err.println("⚠️  ADVERTENCIA: Sin conexión a BD, las consultas pueden fallar");
                System.out.println();
            }
            
            // Departamentos para las pruebas
            String[] departamentosPrueba = {"VALLE DEL CAUCA", "QUINDÍO", "GUAVIARE"};
            
            // Si se proporcionan departamentos como argumentos, usarlos
            if (args.length > 0) {
                departamentosPrueba = args;
                System.out.println("📄 Usando departamentos proporcionados: " + String.join(", ", args));
            }
            
            System.out.println("📊 Departamentos a consultar: " + String.join(", ", departamentosPrueba));
            System.out.println();
            
            // Test 2: Contar ciudadanos (rápido)
            System.out.println("🔢 TEST 2: Contando ciudadanos totales...");
            long startTime = System.currentTimeMillis();
            long totalCiudadanos = consultaCiudadanos.contarCiudadanosPorDepartamentos(departamentosPrueba);
            long countTime = System.currentTimeMillis() - startTime;
            
            if (totalCiudadanos >= 0) {
                System.out.printf("   📊 Total ciudadanos encontrados: %,d (en %dms)%n", totalCiudadanos, countTime);
                
                if (totalCiudadanos > 10000) {
                    System.out.println("   ⚠️  ADVERTENCIA: Muchos registros detectados. Se recomienda usar paginación.");
                } else if (totalCiudadanos > 1000) {
                    System.out.println("   ℹ️  INFO: Volumen moderado de registros. Paginación recomendada.");
                }
            } else {
                System.err.println("   ❌ Error obteniendo el conteo");
            }
            System.out.println();
            
            // Test 3: Consulta con límite por defecto (1000)
            System.out.println("🔍 TEST 3: Consulta con límite por defecto (1000)...");
            startTime = System.currentTimeMillis();
            CiudadanoInfo[] ciudadanosLimitados = consultaCiudadanos.consultarCiudadanosPorDepartamentos(departamentosPrueba);
            long queryTime = System.currentTimeMillis() - startTime;
            
            if (ciudadanosLimitados != null) {
                System.out.printf("   ✅ Ciudadanos obtenidos: %d (en %dms)%n", ciudadanosLimitados.length, queryTime);
                
                if (ciudadanosLimitados.length > 0) {
                    // Mostrar algunos ejemplos
                    int maxMostrar = Math.min(3, ciudadanosLimitados.length);
                    System.out.println("   👥 Ejemplos:");
                    for (int i = 0; i < maxMostrar; i++) {
                        CiudadanoInfo c = ciudadanosLimitados[i];
                        System.out.printf("      %d. %s %s (Doc: %s, ID: %d)%n", 
                            i + 1, c.nombre, c.apellido, c.documento, c.id);
                    }
                    
                    if (ciudadanosLimitados.length == 1000) {
                        System.out.println("   ⚠️  Se alcanzó el límite de 1000. Hay más registros disponibles.");
                    }
                }
            } else {
                System.err.println("   ❌ Error en consulta limitada");
            }
            System.out.println();
            
            // Test 4: Consulta con límite personalizado
            System.out.println("🎯 TEST 4: Consulta con límite personalizado (100)...");
            startTime = System.currentTimeMillis();
            CiudadanoInfo[] ciudadanosCustom = consultaCiudadanos.consultarCiudadanosConLimite(departamentosPrueba, 100);
            queryTime = System.currentTimeMillis() - startTime;
            
            if (ciudadanosCustom != null) {
                System.out.printf("   ✅ Ciudadanos obtenidos: %d (en %dms)%n", ciudadanosCustom.length, queryTime);
            } else {
                System.err.println("   ❌ Error en consulta con límite personalizado");
            }
            System.out.println();
            
            // Test 5: Consulta paginada (solo si hay suficientes registros)
            if (totalCiudadanos > 50) {
                System.out.println("📄 TEST 5: Consulta paginada...");
                
                int tamañoPagina = 25;
                int paginasAMostrar = Math.min(3, (int) Math.ceil((double) totalCiudadanos / tamañoPagina));
                
                for (int pagina = 1; pagina <= paginasAMostrar; pagina++) {
                    System.out.printf("   📋 Obteniendo página %d (tamaño: %d)...%n", pagina, tamañoPagina);
                    
                    startTime = System.currentTimeMillis();
                    ResultadoPaginado resultado = consultaCiudadanos.consultarCiudadanosPaginado(
                        departamentosPrueba, pagina, tamañoPagina);
                    queryTime = System.currentTimeMillis() - startTime;
                    
                    if (resultado != null && resultado.ciudadanos != null) {
                        System.out.printf("      ✅ Página %d/%d: %d ciudadanos (en %dms)%n", 
                            resultado.paginaActual, resultado.totalPaginas, 
                            resultado.ciudadanos.length, queryTime);
                        System.out.printf("      📊 Total registros: %,d | Hay más páginas: %s%n", 
                            resultado.totalRegistros, resultado.hayMasPaginas ? "Sí" : "No");
                        
                        // Mostrar primer ciudadano de la página
                        if (resultado.ciudadanos.length > 0) {
                            CiudadanoInfo primer = resultado.ciudadanos[0];
                            System.out.printf("      👤 Primer ciudadano: %s %s (Doc: %s)%n", 
                                primer.nombre, primer.apellido, primer.documento);
                        }
                    } else {
                        System.err.println("      ❌ Error obteniendo página " + pagina);
                    }
                    
                    System.out.println();
                    
                    // Pausa entre páginas
                    if (pagina < paginasAMostrar) {
                        Thread.sleep(500);
                    }
                }
            } else {
                System.out.println("📄 TEST 5: Saltado (pocos registros para demostrar paginación)");
                System.out.println();
            }
            
            // Test 6: Comparación de rendimiento
            if (totalCiudadanos > 0) {
                System.out.println("⚡ TEST 6: Comparación de rendimiento...");
                
                // Conteo vs consulta limitada
                System.out.println("   🔢 Conteo rápido...");
                startTime = System.currentTimeMillis();
                long conteoRapido = consultaCiudadanos.contarCiudadanosPorDepartamentos(departamentosPrueba);
                long tiempoConteo = System.currentTimeMillis() - startTime;
                
                System.out.println("   📋 Consulta con límite 10...");
                startTime = System.currentTimeMillis();
                CiudadanoInfo[] consultaLimitada = consultaCiudadanos.consultarCiudadanosConLimite(departamentosPrueba, 10);
                long tiempoConsulta = System.currentTimeMillis() - startTime;
                
                System.out.printf("   📊 RESULTADOS:%n");
                System.out.printf("      Conteo: %,d registros en %dms%n", conteoRapido, tiempoConteo);
                System.out.printf("      Consulta limitada: %d registros en %dms%n", 
                    consultaLimitada != null ? consultaLimitada.length : 0, tiempoConsulta);
                System.out.printf("      Diferencia: %dms (%.1fx más rápido el conteo)%n", 
                    tiempoConsulta - tiempoConteo, (double) tiempoConsulta / tiempoConteo);
            }
            
            System.out.println();
            System.out.println("🏁 ===== TESTS COMPLETADOS =====");
            System.out.println("💡 RECOMENDACIONES:");
            System.out.println("   • Use contarCiudadanosPorDepartamentos() para obtener totales rápidamente");
            System.out.println("   • Use consultarCiudadanosConLimite() para consultas con límite específico");
            System.out.println("   • Use consultarCiudadanosPaginado() para navegar grandes conjuntos de datos");
            System.out.println("   • El método original tiene límite de 1000 para evitar sobrecarga");
            
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