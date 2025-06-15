import Demo.*;
import com.zeroc.Ice.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test para exportar ciudadanos procesando cada departamento por separado de forma simultánea
 * Genera un archivo individual por departamento con información completa
 */
public class TestExportarCiudadanosParalelo {
    
    private static final int TAMAÑO_PAGINA = 1000;
    private static final int MAX_THREADS = 3; // Máximo 3 hilos simultáneos
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("❌ ERROR: Debe proporcionar al menos un departamento");
            System.err.println("   Uso: java TestExportarCiudadanosParalelo \"DEPARTAMENTO1\" \"DEPARTAMENTO2\" ...");
            System.err.println("   Ejemplo: java TestExportarCiudadanosParalelo \"VALLE DEL CAUCA\" \"QUINDÍO\" \"GUAVIARE\"");
            System.exit(1);
        }
        
        System.out.println("🚀 ===== EXPORTACIÓN PARALELA POR DEPARTAMENTOS =====");
        System.out.printf("📋 Procesando %d departamentos de forma simultánea%n", args.length);
        System.out.println("📁 Se generará un archivo por departamento");
        System.out.println();
        
        // Crear pool de hilos
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(MAX_THREADS, args.length));
        
        // Contadores para estadísticas globales
        AtomicInteger departamentosExitosos = new AtomicInteger(0);
        AtomicInteger departamentosConError = new AtomicInteger(0);
        
        long tiempoInicio = System.currentTimeMillis();
        
        try {
            // Crear una tarea por cada departamento
            CompletableFuture<ResultadoDepartamento>[] futuros = new CompletableFuture[args.length];
            
            for (int i = 0; i < args.length; i++) {
                final String departamento = args[i];
                final int indice = i + 1;
                
                futuros[i] = CompletableFuture.supplyAsync(() -> {
                    return procesarDepartamento(departamento, indice, args.length);
                }, executor);
            }
            
            // Esperar a que todos los departamentos terminen
            System.out.println("⏳ Esperando a que terminen todos los procesos...");
            System.out.println("=" .repeat(60));
            
            for (int i = 0; i < futuros.length; i++) {
                try {
                    ResultadoDepartamento resultado = futuros[i].get();
                    
                    if (resultado.exitoso) {
                        departamentosExitosos.incrementAndGet();
                        System.out.printf("✅ %s: %,d ciudadanos exportados en %.2fs (archivo: %s)%n",
                            resultado.departamento, resultado.ciudadanosExportados, 
                            resultado.tiempoTotal / 1000.0, resultado.nombreArchivo);
                    } else {
                        departamentosConError.incrementAndGet();
                        System.err.printf("❌ %s: ERROR - %s%n", 
                            resultado.departamento, resultado.mensajeError);
                    }
                    
                } catch (java.lang.Exception e) {
                    departamentosConError.incrementAndGet();
                    System.err.printf("❌ Error procesando departamento %d: %s%n", i + 1, e.getMessage());
                }
            }
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        
        // Mostrar estadísticas finales
        System.out.println("=" .repeat(60));
        System.out.println("🏁 ===== EXPORTACIÓN PARALELA COMPLETADA =====");
        System.out.printf("📊 Departamentos procesados: %d%n", args.length);
        System.out.printf("✅ Exitosos: %d%n", departamentosExitosos.get());
        System.out.printf("❌ Con errores: %d%n", departamentosConError.get());
        System.out.printf("⏱️ Tiempo total: %.2f segundos%n", tiempoTotal / 1000.0);
        System.out.println();
        
        // Mostrar archivos generados
        System.out.println("📁 Archivos generados:");
        File directorio = new File(".");
        File[] archivos = directorio.listFiles((dir, name) -> name.startsWith("ciudadanos_") && name.endsWith(".txt"));
        
        if (archivos != null && archivos.length > 0) {
            long tamatoTotal = 0;
            for (File archivo : archivos) {
                long tamano = archivo.length();
                tamatoTotal += tamano;
                System.out.printf("  📄 %s (%.2f MB)%n", archivo.getName(), tamano / (1024.0 * 1024.0));
            }
            System.out.printf("📏 Tamaño total: %.2f MB%n", tamatoTotal / (1024.0 * 1024.0));
        } else {
            System.out.println("  ⚠️ No se encontraron archivos generados");
        }
        
        System.exit(departamentosConError.get() > 0 ? 1 : 0);
    }
    
    private static ResultadoDepartamento procesarDepartamento(String departamento, int indice, int total) {
        ResultadoDepartamento resultado = new ResultadoDepartamento();
        resultado.departamento = departamento;
        
        Communicator communicator = null;
        PrintWriter writer = null;
        
        try {
            System.out.printf("🔄 [%d/%d] Iniciando procesamiento de: %s%n", indice, total, departamento);
            
            // Inicializar ICE
            communicator = Util.initialize();
            
            // Conectar al servicio
            String endpoint = "ConsultaCiudadanos:tcp -h localhost -p 9090";
            ObjectPrx base = communicator.stringToProxy(endpoint);
            IConsultaCiudadanosPrx consultaCiudadanos = IConsultaCiudadanosPrx.checkedCast(base);
            
            if (consultaCiudadanos == null) {
                resultado.mensajeError = "No se pudo conectar al servicio ConsultaCiudadanos";
                return resultado;
            }
            
            // Verificar conexión BD
            if (!consultaCiudadanos.verificarConexionBD()) {
                resultado.mensajeError = "Sin conexión a base de datos";
                return resultado;
            }
            
            long tiempoInicio = System.currentTimeMillis();
            
            // PASO 1: Contar ciudadanos del departamento
            String[] deptoArray = {departamento};
            long totalCiudadanos = consultaCiudadanos.contarCiudadanosPorDepartamentos(deptoArray);
            
            if (totalCiudadanos <= 0) {
                resultado.mensajeError = "No se encontraron ciudadanos";
                return resultado;
            }
            
            System.out.printf("📊 [%s] Total ciudadanos: %,d%n", departamento, totalCiudadanos);
            
            // PASO 2: Calcular todas las páginas necesarias
            int totalPaginas = (int) Math.ceil((double) totalCiudadanos / TAMAÑO_PAGINA);
            System.out.printf("📋 [%s] Páginas a procesar: %d (tamaño: %d)%n", departamento, totalPaginas, TAMAÑO_PAGINA);
            
            // Crear archivo de salida
            String nombreArchivo = generarNombreArchivo(departamento);
            resultado.nombreArchivo = nombreArchivo;
            
            writer = new PrintWriter(new FileWriter(nombreArchivo), true);
            
            // Escribir encabezado
            escribirEncabezado(writer, departamento, totalCiudadanos);
            
            // PASO 3: ¡LANZAR TODAS LAS PETICIONES SIMULTÁNEAMENTE!
            System.out.printf("🚀 [%s] Lanzando %d peticiones simultáneas...%n", departamento, totalPaginas);
            
            // Crear pool de hilos para las páginas (máximo 10 hilos por departamento)
            int maxHilosPorDepto = Math.min(10, totalPaginas);
            ExecutorService executorPaginas = Executors.newFixedThreadPool(maxHilosPorDepto);
            
            // Array para almacenar los resultados de cada página en orden
            CompletableFuture<PaginaResultado>[] futurosPaginas = new CompletableFuture[totalPaginas];
            
            // Lanzar TODAS las peticiones de páginas simultáneamente
            for (int pagina = 1; pagina <= totalPaginas; pagina++) {
                final int numeroPagina = pagina;
                
                futurosPaginas[pagina - 1] = CompletableFuture.supplyAsync(() -> {
                    return obtenerPagina(consultaCiudadanos, deptoArray, numeroPagina, TAMAÑO_PAGINA, departamento);
                }, executorPaginas);
            }
            
            // Mostrar progreso mientras se procesan
            System.out.printf("⏳ [%s] Esperando %d páginas simultáneas...%n", departamento, totalPaginas);
            
            // Recopilar resultados conforme van llegando (pero escribir en orden)
            long ciudadanosExportados = 0;
            int errores = 0;
            int paginasCompletadas = 0;
            
            // Procesar resultados en orden para mantener consistencia en el archivo
            for (int i = 0; i < totalPaginas; i++) {
                try {
                    PaginaResultado paginaResult = futurosPaginas[i].get(30, TimeUnit.SECONDS);
                    paginasCompletadas++;
                    
                    if (paginaResult.exitoso && paginaResult.ciudadanos != null) {
                        // Escribir ciudadanos al archivo
                        for (CiudadanoInfo ciudadano : paginaResult.ciudadanos) {
                            writer.printf("%d|%s|%s|%s|%s|%s|%s|%s%n",
                                ciudadano.id,
                                ciudadano.documento != null ? ciudadano.documento : "",
                                ciudadano.nombre != null ? ciudadano.nombre : "",
                                ciudadano.apellido != null ? ciudadano.apellido : "",
                                ciudadano.mesa != null ? ciudadano.mesa : "",
                                ciudadano.puesto != null ? ciudadano.puesto : "",
                                ciudadano.municipio != null ? ciudadano.municipio : "",
                                ciudadano.departamento != null ? ciudadano.departamento : "");
                        }
                        
                        ciudadanosExportados += paginaResult.ciudadanos.length;
                        
                        // Mostrar progreso cada 50 páginas
                        if (paginasCompletadas % 50 == 0 || paginasCompletadas == totalPaginas) {
                            double progreso = (double) paginasCompletadas / totalPaginas * 100;
                            System.out.printf("📈 [%s] Progreso: %d/%d páginas (%.1f%%) - Exportados: %,d%n", 
                                departamento, paginasCompletadas, totalPaginas, progreso, ciudadanosExportados);
                        }
                        
                        // Flush periódico
                        if (paginasCompletadas % 20 == 0) {
                            writer.flush();
                        }
                        
                    } else {
                        errores++;
                        System.err.printf("❌ [%s] Error en página %d: %s%n", 
                            departamento, i + 1, paginaResult.error);
                    }
                    
                } catch (java.util.concurrent.TimeoutException e) {
                    errores++;
                    System.err.printf("⏰ [%s] Timeout en página %d%n", departamento, i + 1);
                } catch (java.lang.Exception e) {
                    errores++;
                    System.err.printf("❌ [%s] Error procesando página %d: %s%n", departamento, i + 1, e.getMessage());
                }
            }
            
            // Cerrar pool de hilos de páginas
            executorPaginas.shutdown();
            try {
                if (!executorPaginas.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorPaginas.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorPaginas.shutdownNow();
            }
            
            resultado.tiempoTotal = System.currentTimeMillis() - tiempoInicio;
            resultado.ciudadanosExportados = ciudadanosExportados;
            
            // Escribir estadísticas finales
            escribirEstadisticasFinales(writer, departamento, ciudadanosExportados, totalPaginas, errores, resultado.tiempoTotal);
            
            System.out.printf("✅ [%s] Completado: %,d ciudadanos en %.2fs (%d errores)%n", 
                departamento, ciudadanosExportados, resultado.tiempoTotal / 1000.0, errores);
            
            resultado.exitoso = true;
            
        } catch (java.lang.Exception e) {
            resultado.mensajeError = "Error general: " + e.getMessage();
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (communicator != null) {
                communicator.destroy();
            }
        }
        
        return resultado;
    }
    
    // Nuevo método para obtener una página específica
    private static PaginaResultado obtenerPagina(IConsultaCiudadanosPrx servicio, String[] departamentos, 
                                                 int numeroPagina, int tamanoPagina, String nombreDepto) {
        PaginaResultado resultado = new PaginaResultado();
        resultado.numeroPagina = numeroPagina;
        
        try {
            ResultadoPaginado resultadoPagina = servicio.consultarCiudadanosPaginado(
                departamentos, numeroPagina, tamanoPagina);
            
            if (resultadoPagina != null && resultadoPagina.ciudadanos != null) {
                resultado.ciudadanos = resultadoPagina.ciudadanos;
                resultado.exitoso = true;
            } else {
                resultado.error = "Resultado nulo o sin ciudadanos";
            }
            
        } catch (java.lang.Exception e) {
            resultado.error = "Error en petición: " + e.getMessage();
        }
        
        return resultado;
    }
    
    private static String generarNombreArchivo(String departamento) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        String deptLimpio = departamento.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return String.format("ciudadanos_%s_%s.txt", deptLimpio, timestamp);
    }
    
    private static void escribirEncabezado(PrintWriter writer, String departamento, long totalCiudadanos) {
        writer.println("# ===== EXPORTACIÓN DE CIUDADANOS =====");
        writer.println("# Generado: " + new Date());
        writer.println("# Departamento: " + departamento);
        writer.println("# Total estimado: " + String.format("%,d", totalCiudadanos));
        writer.println("# Formato: ID|DOCUMENTO|NOMBRE|APELLIDO|MESA|PUESTO|MUNICIPIO|DEPARTAMENTO");
        writer.println("# =====================================");
        writer.println();
    }
    
    private static void escribirEstadisticasFinales(PrintWriter writer, String departamento, 
                                                   long exportados, int paginas, int errores, long tiempo) {
        writer.println();
        writer.println("# ===== ESTADÍSTICAS DE EXPORTACIÓN =====");
        writer.println("# Departamento: " + departamento);
        writer.println("# Registros exportados: " + String.format("%,d", exportados));
        writer.println("# Páginas procesadas: " + paginas);
        writer.println("# Errores: " + errores);
        writer.println("# Tiempo total: " + String.format("%.2f", tiempo / 1000.0) + " segundos");
        writer.println("# Finalizado: " + new Date());
        writer.println("# ========================================");
    }
    
    // Clase para almacenar el resultado del procesamiento de un departamento
    private static class ResultadoDepartamento {
        String departamento;
        boolean exitoso = false;
        long ciudadanosExportados = 0;
        long tiempoTotal = 0;
        String nombreArchivo;
        String mensajeError;
    }
    
    // Clase para almacenar el resultado de una página específica
    private static class PaginaResultado {
        int numeroPagina;
        boolean exitoso = false;
        CiudadanoInfo[] ciudadanos;
        String error;
    }
} 