import Demo.*;
import com.zeroc.Ice.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Test para exportar TODOS los ciudadanos a un archivo de texto
 * Utiliza paginación para manejar grandes volúmenes de datos eficientemente
 */
public class TestExportarCiudadanos {
    
    private static final int TAMAÑO_PAGINA = 1000; // Páginas más grandes para eficiencia
    private static final String ARCHIVO_SALIDA = "ciudadanos_exportados.txt";
    
    public static void main(String[] args) {
        Communicator communicator = null;
        int status = 0;
        PrintWriter writer = null;
        
        try {
            System.out.println("📤 ===== EXPORTACIÓN COMPLETA DE CIUDADANOS =====");
            System.out.println();
            
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servicio ConsultaCiudadanos
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
            
            // Verificar conexión a BD
            System.out.println("🔧 Verificando conexión a base de datos...");
            boolean conexionOK = consultaCiudadanos.verificarConexionBD();
            if (!conexionOK) {
                System.err.println("❌ ERROR: Sin conexión a base de datos");
                return;
            }
            System.out.println("✅ Conexión a BD verificada");
            System.out.println();
            
            // Departamentos a exportar (usar argumentos si se proporcionan)
            String[] departamentos;
            if (args.length > 0) {
                departamentos = args;
                System.out.println("📄 Exportando departamentos especificados: " + String.join(", ", args));
            } else {
                // Exportar los departamentos por defecto
                departamentos = new String[]{"VALLE DEL CAUCA", "QUINDÍO", "GUAVIARE"};
                System.out.println("📄 Exportando departamentos por defecto: " + String.join(", ", departamentos));
            }
            System.out.println();
            
            // Contar total de ciudadanos
            System.out.println("🔢 Contando total de ciudadanos...");
            long startTime = System.currentTimeMillis();
            long totalCiudadanos = consultaCiudadanos.contarCiudadanosPorDepartamentos(departamentos);
            long countTime = System.currentTimeMillis() - startTime;
            
            if (totalCiudadanos <= 0) {
                System.err.println("❌ No se encontraron ciudadanos para exportar");
                return;
            }
            
            System.out.printf("📊 Total ciudadanos a exportar: %,d (conteo en %dms)%n", totalCiudadanos, countTime);
            
            // Calcular páginas necesarias
            int totalPaginas = (int) Math.ceil((double) totalCiudadanos / TAMAÑO_PAGINA);
            System.out.printf("📄 Se procesarán %d páginas de %d registros cada una%n", totalPaginas, TAMAÑO_PAGINA);
            System.out.println();
            
            // Crear archivo de salida
            String nombreArchivo = generarNombreArchivo(departamentos);
            System.out.println("📝 Creando archivo: " + nombreArchivo);
            writer = new PrintWriter(new FileWriter(nombreArchivo), true);
            
            // Escribir encabezado
            escribirEncabezado(writer, departamentos, totalCiudadanos);
            
            // Variables para estadísticas
            long ciudadanosExportados = 0;
            long tiempoTotalConsultas = 0;
            int paginasExitosas = 0;
            int errores = 0;
            
            System.out.println("🚀 Iniciando exportación...");
            System.out.println("=" .repeat(60));
            
            // Procesar página por página
            for (int pagina = 1; pagina <= totalPaginas; pagina++) {
                try {
                    // Mostrar progreso
                    if (pagina % 100 == 0 || pagina == 1 || pagina == totalPaginas) {
                        double progreso = (double) pagina / totalPaginas * 100;
                        System.out.printf("📋 Procesando página %d/%d (%.1f%%) - Exportados: %,d%n", 
                            pagina, totalPaginas, progreso, ciudadanosExportados);
                    }
                    
                    // Obtener página de ciudadanos
                    long paginaStartTime = System.currentTimeMillis();
                    ResultadoPaginado resultado = consultaCiudadanos.consultarCiudadanosPaginado(
                        departamentos, pagina, TAMAÑO_PAGINA);
                    long paginaTime = System.currentTimeMillis() - paginaStartTime;
                    tiempoTotalConsultas += paginaTime;
                    
                    if (resultado != null && resultado.ciudadanos != null && resultado.ciudadanos.length > 0) {
                        // Escribir ciudadanos al archivo
                        for (CiudadanoInfo ciudadano : resultado.ciudadanos) {
                            writer.printf("%d|%s|%s|%s%n", 
                                ciudadano.id, 
                                ciudadano.documento != null ? ciudadano.documento : "",
                                ciudadano.nombre != null ? ciudadano.nombre : "",
                                ciudadano.apellido != null ? ciudadano.apellido : "");
                        }
                        
                        ciudadanosExportados += resultado.ciudadanos.length;
                        paginasExitosas++;
                        
                        // Flush periódico para asegurar escritura
                        if (pagina % 50 == 0) {
                            writer.flush();
                        }
                        
                    } else {
                        System.err.printf("⚠️ Página %d vacía o con error%n", pagina);
                        errores++;
                    }
                    
                    // Pausa pequeña para no saturar el servidor
                    if (pagina % 100 == 0) {
                        Thread.sleep(100);
                    }
                    
                } catch (java.lang.Exception e) {
                    System.err.printf("❌ Error procesando página %d: %s%n", pagina, e.getMessage());
                    errores++;
                    
                    // Si hay muchos errores consecutivos, abortar
                    if (errores > 10 && (double) errores / pagina > 0.1) {
                        System.err.println("❌ Demasiados errores. Abortando exportación.");
                        break;
                    }
                }
            }
            
            // Escribir estadísticas finales al archivo
            escribirEstadisticasFinales(writer, ciudadanosExportados, paginasExitosas, errores, tiempoTotalConsultas);
            
            System.out.println("=" .repeat(60));
            System.out.println("🏁 ===== EXPORTACIÓN COMPLETADA =====");
            System.out.printf("📊 Ciudadanos exportados: %,d de %,d (%.1f%%)%n", 
                ciudadanosExportados, totalCiudadanos, 
                (double) ciudadanosExportados / totalCiudadanos * 100);
            System.out.printf("📄 Páginas procesadas: %d/%d exitosas%n", paginasExitosas, totalPaginas);
            System.out.printf("❌ Errores: %d%n", errores);
            System.out.printf("⏱️ Tiempo total consultas: %.2f segundos%n", tiempoTotalConsultas / 1000.0);
            System.out.printf("⚡ Promedio por página: %dms%n", 
                paginasExitosas > 0 ? tiempoTotalConsultas / paginasExitosas : 0);
            System.out.printf("📁 Archivo generado: %s%n", nombreArchivo);
            
            // Mostrar información del archivo
            File archivo = new File(nombreArchivo);
            if (archivo.exists()) {
                System.out.printf("📏 Tamaño del archivo: %.2f MB%n", archivo.length() / (1024.0 * 1024.0));
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ ERROR GENERAL: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (communicator != null) {
                communicator.destroy();
            }
        }
        
        System.exit(status);
    }
    
    private static String generarNombreArchivo(String[] departamentos) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        
        if (departamentos.length == 1) {
            String deptLimpio = departamentos[0].replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            return String.format("ciudadanos_%s_%s.txt", deptLimpio, timestamp);
        } else {
            return String.format("ciudadanos_multiples_deptos_%s.txt", timestamp);
        }
    }
    
    private static void escribirEncabezado(PrintWriter writer, String[] departamentos, long totalCiudadanos) {
        writer.println("# ===== EXPORTACIÓN DE CIUDADANOS =====");
        writer.println("# Generado: " + new Date());
        writer.println("# Departamentos: " + String.join(", ", departamentos));
        writer.println("# Total estimado: " + String.format("%,d", totalCiudadanos));
        writer.println("# Formato: ID|DOCUMENTO|NOMBRE|APELLIDO");
        writer.println("# =====================================");
        writer.println();
    }
    
    private static void escribirEstadisticasFinales(PrintWriter writer, long exportados, int exitosas, 
                                                   int errores, long tiempoTotal) {
        writer.println();
        writer.println("# ===== ESTADÍSTICAS DE EXPORTACIÓN =====");
        writer.println("# Registros exportados: " + String.format("%,d", exportados));
        writer.println("# Páginas exitosas: " + exitosas);
        writer.println("# Errores: " + errores);
        writer.println("# Tiempo total consultas: " + String.format("%.2f", tiempoTotal / 1000.0) + " segundos");
        writer.println("# Finalizado: " + new Date());
        writer.println("# ========================================");
    }
} 