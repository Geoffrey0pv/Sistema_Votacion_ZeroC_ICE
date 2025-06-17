import com.zeroc.Ice.*;
import Demo.*;
import java.util.*;

/**
 * Cliente de prueba específico para probar las funcionalidades de votantes
 * del Servidor Regional a través de la interfaz IConsultaMesaSQLite
 */
public class TestClientServidorRegional {
    private Communicator communicator;
    private Scanner scanner;
    private String servidorRegionalEndpoint = "tcp -h localhost -p 8080";
    
    // Proxy para el servicio de consulta de mesas SQLite del servidor regional
    private IConsultaMesaSQLitePrx consultaMesaSQLiteProxy;
    
    public TestClientServidorRegional() {
        scanner = new Scanner(System.in);
    }
    
    public static void main(String[] args) {
        TestClientServidorRegional client = new TestClientServidorRegional();
        try {
            client.run(args);
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void run(String[] args) {
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servidor regional
            conectarAServidorRegional();
            
            // Mostrar menú principal
            mostrarBienvenida();
            menuPrincipal();
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error en la aplicación: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
            scanner.close();
        }
    }
    
    private void conectarAServidorRegional() {
        try {
            System.out.println("🔌 Conectando al Servidor Regional...");
            System.out.println("   Endpoint: " + servidorRegionalEndpoint);
            
            // Conectar al servicio IConsultaMesaSQLite
            ObjectPrx base = communicator.stringToProxy("consultaMesaSQLite:" + servidorRegionalEndpoint);
            consultaMesaSQLiteProxy = IConsultaMesaSQLitePrx.checkedCast(base);
            
            if (consultaMesaSQLiteProxy == null) {
                throw new RuntimeException("No se pudo conectar al servicio IConsultaMesaSQLite");
            }
            
            // Verificar conectividad
            boolean servicioActivo = consultaMesaSQLiteProxy.verificarServicio();
            if (servicioActivo) {
                System.out.println("✅ Servicio IConsultaMesaSQLite conectado y verificado");
            } else {
                System.out.println("⚠️  Servicio conectado pero no completamente disponible");
            }
            
            System.out.println("═══════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error conectando al Servidor Regional: " + e.getMessage());
            System.err.println("💡 Asegúrese de que el Servidor Regional esté ejecutándose en " + servidorRegionalEndpoint);
            throw new RuntimeException(e);
        }
    }
    
    private void mostrarBienvenida() {
        System.out.println();
        System.out.println("🧪 ═══════════════════════════════════════════════════════════");
        System.out.println("   CLIENTE DE PRUEBAS - SERVIDOR REGIONAL");
        System.out.println("   Consulta de Votantes por Mesas SQLite");
        System.out.println("   Sistema de Votación ZeroC ICE");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
    }
    
    private void menuPrincipal() {
        while (true) {
            System.out.println("\n📋 MENÚ DE PRUEBAS - VOTANTES SERVIDOR REGIONAL:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("1. 📊 Listar todas las mesas SQLite disponibles");
            System.out.println("2. 📈 Obtener estadísticas de una mesa específica");
            System.out.println("3. 👥 Obtener todos los votantes de una mesa");
            System.out.println("4. 📄 Obtener votantes con paginación");
            System.out.println("5. 🔍 Buscar votante por documento en una mesa");
            System.out.println("6. 📊 Contar votantes de una mesa");
            System.out.println("7. ✅ Contar votantes verificados de una mesa");
            System.out.println("8. 📋 Obtener logs de verificación de una mesa");
            System.out.println("9. 🗂️  Obtener información completa de una mesa");
            System.out.println("10. 🔍 Verificar si existe una mesa SQLite");
            System.out.println("11. 🧪 Ejecutar pruebas automáticas");
            System.out.println("0. 🚪 Salir");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.print("Seleccione una opción: ");
            
            try {
                int opcion = Integer.parseInt(scanner.nextLine().trim());
                
                switch (opcion) {
                    case 1:
                        listarMesasDisponibles();
                        break;
                    case 2:
                        obtenerEstadisticasMesa();
                        break;
                    case 3:
                        obtenerTodosVotantesMesa();
                        break;
                    case 4:
                        obtenerVotantesConPaginacion();
                        break;
                    case 5:
                        buscarVotantePorDocumento();
                        break;
                    case 6:
                        contarVotantesMesa();
                        break;
                    case 7:
                        contarVotantesVerificados();
                        break;
                    case 8:
                        obtenerLogsVerificacion();
                        break;
                    case 9:
                        obtenerInfoCompletaMesa();
                        break;
                    case 10:
                        verificarExisteMesa();
                        break;
                    case 11:
                        ejecutarPruebasAutomaticas();
                        break;
                    case 0:
                        System.out.println("👋 ¡Hasta luego!");
                        return;
                    default:
                        System.out.println("❌ Opción inválida. Por favor, seleccione una opción válida.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Por favor, ingrese un número válido.");
            } catch (java.lang.Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void listarMesasDisponibles() {
        System.out.println("\n📊 ═══ LISTAR MESAS SQLITE DISPONIBLES ═══");
        
        try {
            long startTime = System.currentTimeMillis();
            String[] mesas = consultaMesaSQLiteProxy.listarMesasDisponibles();
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Consulta exitosa en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            
            if (mesas.length == 0) {
                System.out.println("⚠️  No hay mesas SQLite disponibles");
                System.out.println("💡 Ejecute el comando 'distribuir <departamento>' en el Servidor Regional");
            } else {
                System.out.println("📋 Mesas SQLite encontradas (" + mesas.length + "):");
                for (int i = 0; i < mesas.length; i++) {
                    System.out.println("   " + (i + 1) + ". " + mesas[i]);
                }
            }
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error listando mesas: " + e.getMessage());
        }
    }
    
    private void obtenerEstadisticasMesa() {
        System.out.println("\n📈 ═══ ESTADÍSTICAS DE MESA ═══");
        System.out.print("📝 Ingrese el ID de la mesa: ");
        String mesaId = scanner.nextLine().trim();
        
        if (mesaId.isEmpty()) {
            System.out.println("❌ El ID de mesa no puede estar vacío");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            EstadisticasMesaSQLite stats = consultaMesaSQLiteProxy.obtenerEstadisticasMesa(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Estadísticas obtenidas en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("🗳️  Mesa ID: " + stats.mesaId);
            System.out.println("🌍 Departamento: " + stats.departamento);
            System.out.println("🏙️  Municipio: " + stats.municipio);
            System.out.println("🏢 Puesto: " + stats.puesto);
            System.out.println("👥 Total Votantes: " + stats.totalVotantes);
            System.out.println("✅ Votantes Verificados: " + stats.votantesVerificados);
            System.out.println("🟢 Mesa Activa: " + (stats.mesaActiva == 1 ? "Sí" : "No"));
            System.out.println("📅 Fecha Creación: " + stats.fechaCreacion);
            System.out.println("🕐 Última Actualización: " + new Date(stats.ultimaActualizacion));
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error obteniendo estadísticas: " + e.getMessage());
        }
    }
    
    private void obtenerTodosVotantesMesa() {
        System.out.println("\n👥 ═══ TODOS LOS VOTANTES DE UNA MESA ═══");
        System.out.print("📝 Ingrese el ID de la mesa: ");
        String mesaId = scanner.nextLine().trim();
        
        if (mesaId.isEmpty()) {
            System.out.println("❌ El ID de mesa no puede estar vacío");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            VotanteMesa[] votantes = consultaMesaSQLiteProxy.obtenerVotantesDeMesa(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Votantes obtenidos en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            
            if (votantes.length == 0) {
                System.out.println("⚠️  No hay votantes en la mesa " + mesaId);
            } else {
                System.out.println("👥 Votantes encontrados: " + votantes.length);
                System.out.println();
                
                // Mostrar solo los primeros 10 para no saturar la consola
                int limite = Math.min(10, votantes.length);
                for (int i = 0; i < limite; i++) {
                    VotanteMesa v = votantes[i];
                    System.out.println("   " + (i + 1) + ". " + v.documento + " - " + v.nombre + " " + v.apellido);
                    System.out.println("      Mesa: " + v.mesa + " | Verificado: " + (v.verificado == 1 ? "Sí" : "No"));
                }
                
                if (votantes.length > limite) {
                    System.out.println("   ... y " + (votantes.length - limite) + " votantes más");
                    System.out.println("💡 Use la opción de paginación para ver todos los votantes");
                }
            }
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error obteniendo votantes: " + e.getMessage());
        }
    }
    
    private void obtenerVotantesConPaginacion() {
        System.out.println("\n📄 ═══ VOTANTES CON PAGINACIÓN ═══");
        System.out.print("📝 Ingrese el ID de la mesa: ");
        String mesaId = scanner.nextLine().trim();
        
        if (mesaId.isEmpty()) {
            System.out.println("❌ El ID de mesa no puede estar vacío");
            return;
        }
        
        System.out.print("📄 Página (por defecto 1): ");
        String paginaStr = scanner.nextLine().trim();
        int pagina = paginaStr.isEmpty() ? 1 : Integer.parseInt(paginaStr);
        
        System.out.print("📊 Tamaño de página (por defecto 10): ");
        String tamanoStr = scanner.nextLine().trim();
        int tamano = tamanoStr.isEmpty() ? 10 : Integer.parseInt(tamanoStr);
        
        try {
            long startTime = System.currentTimeMillis();
            VotanteMesa[] votantes = consultaMesaSQLiteProxy.obtenerVotantesPaginados(mesaId, pagina, tamano);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Votantes obtenidos en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("📄 Página " + pagina + " (tamaño: " + tamano + ")");
            System.out.println("👥 Votantes en esta página: " + votantes.length);
            System.out.println();
            
            if (votantes.length > 0) {
                for (int i = 0; i < votantes.length; i++) {
                    VotanteMesa v = votantes[i];
                    System.out.println("   " + (((pagina - 1) * tamano) + i + 1) + ". " + v.documento + " - " + v.nombre + " " + v.apellido);
                    System.out.println("      Mesa: " + v.mesa + " | Verificado: " + (v.verificado == 1 ? "Sí" : "No"));
                    System.out.println("      Fecha Asignación: " + v.fechaAsignacion);
                }
            } else {
                System.out.println("⚠️  No hay votantes en esta página");
            }
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error obteniendo votantes paginados: " + e.getMessage());
        }
    }
    
    private void buscarVotantePorDocumento() {
        System.out.println("\n🔍 ═══ BUSCAR VOTANTE POR DOCUMENTO ═══");
        System.out.print("📝 Ingrese el ID de la mesa: ");
        String mesaId = scanner.nextLine().trim();
        
        if (mesaId.isEmpty()) {
            System.out.println("❌ El ID de mesa no puede estar vacío");
            return;
        }
        
        System.out.print("📄 Ingrese el número de documento: ");
        String documento = scanner.nextLine().trim();
        
        if (documento.isEmpty()) {
            System.out.println("❌ El documento no puede estar vacío");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            VotanteMesa votante = consultaMesaSQLiteProxy.buscarVotantePorDocumento(mesaId, documento);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Búsqueda completada en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            
            if (votante != null && !votante.documento.isEmpty()) {
                System.out.println("🎯 Votante encontrado:");
                System.out.println("   📄 Documento: " + votante.documento);
                System.out.println("   👤 Nombre: " + votante.nombre + " " + votante.apellido);
                System.out.println("   🗳️  Mesa: " + votante.mesa + " (ID: " + votante.mesaId + ")");
                System.out.println("   🏢 Puesto: " + votante.puesto);
                System.out.println("   🏙️  Municipio: " + votante.municipio);
                System.out.println("   🌍 Departamento: " + votante.departamento);
                System.out.println("   ✅ Verificado: " + (votante.verificado == 1 ? "Sí" : "No"));
                System.out.println("   📅 Fecha Asignación: " + votante.fechaAsignacion);
                if (votante.verificado == 1) {
                    System.out.println("   🕐 Fecha Verificación: " + votante.fechaVerificacion);
                }
            } else {
                System.out.println("⚠️  Votante con documento " + documento + " no encontrado en la mesa " + mesaId);
            }
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error buscando votante: " + e.getMessage());
        }
    }
    
    private void contarVotantesMesa() {
        System.out.println("\n📊 ═══ CONTAR VOTANTES DE UNA MESA ═══");
        System.out.print("📝 Ingrese el ID de la mesa: ");
        String mesaId = scanner.nextLine().trim();
        
        if (mesaId.isEmpty()) {
            System.out.println("❌ El ID de mesa no puede estar vacío");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            int totalVotantes = consultaMesaSQLiteProxy.contarVotantesMesa(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Conteo completado en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("🗳️  Mesa: " + mesaId);
            System.out.println("👥 Total de votantes: " + totalVotantes);
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error contando votantes: " + e.getMessage());
        }
    }
    
    private void contarVotantesVerificados() {
        System.out.println("\n✅ ═══ CONTAR VOTANTES VERIFICADOS ═══");
        System.out.print("📝 Ingrese el ID de la mesa: ");
        String mesaId = scanner.nextLine().trim();
        
        if (mesaId.isEmpty()) {
            System.out.println("❌ El ID de mesa no puede estar vacío");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            int votantesVerificados = consultaMesaSQLiteProxy.contarVotantesVerificados(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Conteo completado en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("🗳️  Mesa: " + mesaId);
            System.out.println("✅ Votantes verificados: " + votantesVerificados);
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error contando votantes verificados: " + e.getMessage());
        }
    }
    
    private void obtenerLogsVerificacion() {
        System.out.println("\n📋 ═══ LOGS DE VERIFICACIÓN ═══");
        System.out.print("📝 Ingrese el ID de la mesa: ");
        String mesaId = scanner.nextLine().trim();
        
        if (mesaId.isEmpty()) {
            System.out.println("❌ El ID de mesa no puede estar vacío");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            LogVerificacion[] logs = consultaMesaSQLiteProxy.obtenerLogsVerificacion(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Logs obtenidos en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            
            if (logs.length == 0) {
                System.out.println("⚠️  No hay logs de verificación para la mesa " + mesaId);
            } else {
                System.out.println("📋 Logs de verificación (" + logs.length + "):");
                System.out.println();
                
                // Mostrar solo los últimos 10 logs
                int inicio = Math.max(0, logs.length - 10);
                for (int i = inicio; i < logs.length; i++) {
                    LogVerificacion log = logs[i];
                    System.out.println("   " + (i + 1) + ". [" + log.timestamp + "] " + log.accion);
                    System.out.println("      Documento: " + log.documento + " | Resultado: " + log.resultado);
                }
                
                if (logs.length > 10) {
                    System.out.println("   ... mostrando los últimos 10 de " + logs.length + " logs totales");
                }
            }
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error obteniendo logs: " + e.getMessage());
        }
    }
    
    private void obtenerInfoCompletaMesa() {
        System.out.println("\n🗂️ ═══ INFORMACIÓN COMPLETA DE MESA ═══");
        System.out.print("📝 Ingrese el ID de la mesa: ");
        String mesaId = scanner.nextLine().trim();
        
        if (mesaId.isEmpty()) {
            System.out.println("❌ El ID de mesa no puede estar vacío");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            InfoCompletaMesa info = consultaMesaSQLiteProxy.obtenerInfoCompletaMesa(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Información completa obtenida en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            
            // Mostrar estadísticas
            EstadisticasMesaSQLite stats = info.estadisticas;
            System.out.println("📊 ESTADÍSTICAS:");
            System.out.println("   🗳️  Mesa ID: " + stats.mesaId);
            System.out.println("   🌍 Departamento: " + stats.departamento);
            System.out.println("   🏙️  Municipio: " + stats.municipio);
            System.out.println("   🏢 Puesto: " + stats.puesto);
            System.out.println("   👥 Total Votantes: " + stats.totalVotantes);
            System.out.println("   ✅ Votantes Verificados: " + stats.votantesVerificados);
            System.out.println("   🟢 Mesa Activa: " + (stats.mesaActiva == 1 ? "Sí" : "No"));
            System.out.println("   📅 Fecha Creación: " + stats.fechaCreacion);
            
            // Información del archivo
            System.out.println("\n📁 ARCHIVO:");
            System.out.println("   🟢 Existe: " + (info.archivoExiste ? "Sí" : "No"));
            System.out.println("   📂 Ruta: " + info.rutaArchivo);
            
            // Resumen de votantes (solo los primeros 5)
            System.out.println("\n👥 VOTANTES (primeros 5):");
            VotanteMesa[] votantes = info.votantes;
            int limite = Math.min(5, votantes.length);
            for (int i = 0; i < limite; i++) {
                VotanteMesa v = votantes[i];
                System.out.println("   " + (i + 1) + ". " + v.documento + " - " + v.nombre + " " + v.apellido);
            }
            if (votantes.length > limite) {
                System.out.println("   ... y " + (votantes.length - limite) + " votantes más");
            }
            
            // Resumen de logs (solo los últimos 3)
            System.out.println("\n📋 LOGS RECIENTES (últimos 3):");
            LogVerificacion[] logs = info.logs;
            int inicioLogs = Math.max(0, logs.length - 3);
            for (int i = inicioLogs; i < logs.length; i++) {
                LogVerificacion log = logs[i];
                System.out.println("   " + (i + 1) + ". [" + log.timestamp + "] " + log.accion);
            }
            if (logs.length > 3) {
                System.out.println("   ... " + (logs.length - 3) + " logs anteriores");
            }
            
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error obteniendo información completa: " + e.getMessage());
        }
    }
    
    private void verificarExisteMesa() {
        System.out.println("\n🔍 ═══ VERIFICAR EXISTENCIA DE MESA ═══");
        System.out.print("📝 Ingrese el ID de la mesa: ");
        String mesaId = scanner.nextLine().trim();
        
        if (mesaId.isEmpty()) {
            System.out.println("❌ El ID de mesa no puede estar vacío");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            boolean existe = consultaMesaSQLiteProxy.existeMesaSQLite(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Verificación completada en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("🗳️  Mesa ID: " + mesaId);
            System.out.println("🟢 Existe: " + (existe ? "Sí" : "No"));
            
            if (!existe) {
                System.out.println("💡 La mesa no existe o no ha sido distribuida aún");
            }
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error verificando existencia de mesa: " + e.getMessage());
        }
    }
    
    private void ejecutarPruebasAutomaticas() {
        System.out.println("\n🧪 ═══ EJECUTANDO PRUEBAS AUTOMÁTICAS ═══");
        
        try {
            // 1. Verificar servicio
            System.out.println("1️⃣  Verificando servicio...");
            boolean servicioOK = consultaMesaSQLiteProxy.verificarServicio();
            System.out.println("   Resultado: " + (servicioOK ? "✅ OK" : "❌ FALLO"));
            
            // 2. Listar mesas disponibles
            System.out.println("\n2️⃣  Listando mesas disponibles...");
            String[] mesas = consultaMesaSQLiteProxy.listarMesasDisponibles();
            System.out.println("   Mesas encontradas: " + mesas.length);
            
            if (mesas.length > 0) {
                String mesaPrueba = mesas[0];
                System.out.println("   Usando mesa '" + mesaPrueba + "' para pruebas adicionales");
                
                // 3. Obtener estadísticas
                System.out.println("\n3️⃣  Obteniendo estadísticas de mesa...");
                EstadisticasMesaSQLite stats = consultaMesaSQLiteProxy.obtenerEstadisticasMesa(mesaPrueba);
                System.out.println("   Mesa: " + stats.mesaId + " | Votantes: " + stats.totalVotantes + " | Verificados: " + stats.votantesVerificados);
                
                // 4. Contar votantes
                System.out.println("\n4️⃣  Contando votantes...");
                int totalVotantes = consultaMesaSQLiteProxy.contarVotantesMesa(mesaPrueba);
                int votantesVerificados = consultaMesaSQLiteProxy.contarVotantesVerificados(mesaPrueba);
                System.out.println("   Total: " + totalVotantes + " | Verificados: " + votantesVerificados);
                
                // 5. Obtener votantes con paginación
                System.out.println("\n5️⃣  Obteniendo votantes (página 1, tamaño 5)...");
                VotanteMesa[] votantesPag = consultaMesaSQLiteProxy.obtenerVotantesPaginados(mesaPrueba, 1, 5);
                System.out.println("   Votantes obtenidos: " + votantesPag.length);
                
                if (votantesPag.length > 0) {
                    // 6. Buscar un votante específico
                    String documentoPrueba = votantesPag[0].documento;
                    System.out.println("\n6️⃣  Buscando votante por documento: " + documentoPrueba);
                    VotanteMesa votanteEncontrado = consultaMesaSQLiteProxy.buscarVotantePorDocumento(mesaPrueba, documentoPrueba);
                    System.out.println("   Resultado: " + (votanteEncontrado != null && !votanteEncontrado.documento.isEmpty() ? "✅ Encontrado" : "❌ No encontrado"));
                }
                
                // 7. Obtener logs de verificación
                System.out.println("\n7️⃣  Obteniendo logs de verificación...");
                LogVerificacion[] logs = consultaMesaSQLiteProxy.obtenerLogsVerificacion(mesaPrueba);
                System.out.println("   Logs encontrados: " + logs.length);
                
            } else {
                System.out.println("   ⚠️  No hay mesas disponibles para pruebas adicionales");
                System.out.println("   💡 Ejecute 'distribuir <departamento>' en el Servidor Regional");
            }
            
            System.out.println("\n✅ Pruebas automáticas completadas");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error durante las pruebas automáticas: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("══════════════════════════════════════════════════════════");
    }
} 