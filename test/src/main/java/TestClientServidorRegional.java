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
    
    // Proxy tipado para el servicio de consulta de mesas SQLite del servidor regional
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
            
            System.out.println("✅ Servicio IConsultaMesaSQLite conectado");
            System.out.println("═══════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error conectando al Servidor Regional: " + e.getMessage());
            System.err.println("💡 Asegúrese de que el Servidor Regional esté ejecutándose en " + servidorRegionalEndpoint);
            throw new RuntimeException(e);
        }
    }
    
    private void mostrarBienvenida() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("🗳️  CLIENTE DE PRUEBA - SERVIDOR REGIONAL 🗳️");
        System.out.println("   Sistema de Votación ZeroC ICE");
        System.out.println("═".repeat(70));
        System.out.println("🎯 Objetivo: Probar funcionalidades de consulta de votantes");
        System.out.println("🔌 Servicio: IConsultaMesaSQLite");
        System.out.println("📡 Endpoint: " + servidorRegionalEndpoint);
        System.out.println("═".repeat(70));
    }
    
    private void menuPrincipal() {
        while (true) {
            System.out.println("\n" + "═".repeat(60));
            System.out.println("📋 MENÚ PRINCIPAL - CONSULTAS DE VOTANTES");
            System.out.println("═".repeat(60));
            System.out.println("🗂️  CONSULTAS DE MESAS:");
            System.out.println("   1. Listar mesas SQLite disponibles");
            System.out.println("   2. Obtener estadísticas de una mesa");
            System.out.println("   9. Verificar si existe una mesa");
            System.out.println();
            System.out.println("👥 CONSULTAS DE VOTANTES:");
            System.out.println("   3. Obtener todos los votantes de una mesa");
            System.out.println("   4. Obtener votantes con paginación");
            System.out.println("   5. Buscar votante por documento");
            System.out.println("   6. Contar total de votantes en mesa");
            System.out.println("   7. Contar votantes verificados");
            System.out.println();
            System.out.println("🔧 PRUEBAS Y UTILIDADES:");
            System.out.println("   8. Ejecutar pruebas automáticas");
            System.out.println("   0. Salir");
            System.out.println("═".repeat(60));
            System.out.print("👉 Seleccione una opción: ");
            
            String opcion = scanner.nextLine().trim();
            
            switch (opcion) {
                case "1":
                    listarMesasDisponibles();
                    break;
                case "2":
                    obtenerEstadisticasMesa();
                    break;
                case "3":
                    obtenerTodosVotantesMesa();
                    break;
                case "4":
                    obtenerVotantesConPaginacion();
                    break;
                case "5":
                    buscarVotantePorDocumento();
                    break;
                case "6":
                    contarVotantesMesa();
                    break;
                case "7":
                    contarVotantesVerificados();
                    break;
                case "8":
                    pruebasAutomaticas();
                    break;
                case "9":
                    verificarExisteMesa();
                    break;
                case "0":
                    System.out.println("\n👋 ¡Gracias por usar el Cliente de Prueba!");
                    System.out.println("🔌 Cerrando conexión con el Servidor Regional...");
                    return;
                default:
                    System.out.println("❌ Opción inválida. Por favor seleccione una opción del 0 al 9.");
            }
            
            // Pausa antes de mostrar el menú nuevamente
            System.out.println("\n⏎ Presione Enter para continuar...");
            scanner.nextLine();
        }
    }
    
    private void listarMesasDisponibles() {
        System.out.println("\n📊 ═══ LISTAR MESAS SQLITE DISPONIBLES ═══");
        
        try {
            System.out.println("🔄 Solicitando mesas disponibles al Servidor Regional...");
            long startTime = System.currentTimeMillis();
            
            String[] mesas = consultaMesaSQLiteProxy.listarMesasDisponibles();
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Consulta exitosa en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            
            if (mesas.length == 0) {
                System.out.println("⚠️  No hay mesas SQLite disponibles");
                System.out.println("💡 Ejecute 'distribuir <departamento>' en el Servidor Regional");
            } else {
                System.out.println("📋 Mesas SQLite encontradas (" + mesas.length + "):");
                for (int i = 0; i < mesas.length; i++) {
                    System.out.println("   " + (i + 1) + ". " + mesas[i]);
                }
            }
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error listando mesas: " + e.getMessage());
            System.out.println("💡 Verifique que el Servidor Regional esté ejecutándose y tenga mesas distribuidas");
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
            System.out.println("🔄 Obteniendo estadísticas para mesa: " + mesaId);
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
            if (stats.ultimaActualizacion > 0) {
                System.out.println("🕐 Última Actualización: " + new Date(stats.ultimaActualizacion));
            }
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error obteniendo estadísticas: " + e.getMessage());
            System.out.println("💡 Verifique que la mesa '" + mesaId + "' exista y esté distribuida");
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
            System.out.println("🔄 Obteniendo votantes para mesa: " + mesaId);
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
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error obteniendo votantes: " + e.getMessage());
            System.out.println("💡 Verifique que la mesa '" + mesaId + "' exista y tenga votantes distribuidos");
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
            System.out.println("🔄 Obteniendo votantes paginados para mesa: " + mesaId);
            System.out.println("   Página: " + pagina + " | Tamaño: " + tamano);
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
                    if (v.fechaAsignacion != null && !v.fechaAsignacion.isEmpty()) {
                        System.out.println("      Fecha Asignación: " + v.fechaAsignacion);
                    }
                }
            } else {
                System.out.println("⚠️  No hay votantes en esta página");
            }
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error obteniendo votantes paginados: " + e.getMessage());
            System.out.println("💡 Verifique que la mesa exista y que la página solicitada sea válida");
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
            System.out.println("🔄 Buscando votante con documento: " + documento + " en mesa: " + mesaId);
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
                if (votante.fechaAsignacion != null && !votante.fechaAsignacion.isEmpty()) {
                    System.out.println("   📅 Fecha Asignación: " + votante.fechaAsignacion);
                }
                if (votante.verificado == 1 && votante.fechaVerificacion != null && !votante.fechaVerificacion.isEmpty()) {
                    System.out.println("   🕐 Fecha Verificación: " + votante.fechaVerificacion);
                }
            } else {
                System.out.println("⚠️  Votante con documento " + documento + " no encontrado en la mesa " + mesaId);
            }
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error buscando votante: " + e.getMessage());
            System.out.println("💡 Verifique que la mesa y el documento sean válidos");
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
            System.out.println("🔄 Contando votantes para mesa: " + mesaId);
            long startTime = System.currentTimeMillis();
            
            int totalVotantes = consultaMesaSQLiteProxy.contarVotantesMesa(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Conteo completado en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("🗳️  Mesa: " + mesaId);
            System.out.println("👥 Total de votantes: " + totalVotantes);
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error contando votantes: " + e.getMessage());
            System.out.println("💡 Verifique que la mesa '" + mesaId + "' exista");
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
            System.out.println("🔄 Contando votantes verificados para mesa: " + mesaId);
            long startTime = System.currentTimeMillis();
            
            int votantesVerificados = consultaMesaSQLiteProxy.contarVotantesVerificados(mesaId);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Conteo completado en " + (endTime - startTime) + "ms");
            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("🗳️  Mesa: " + mesaId);
            System.out.println("✅ Votantes verificados: " + votantesVerificados);
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error contando votantes verificados: " + e.getMessage());
            System.out.println("💡 Verifique que la mesa '" + mesaId + "' exista");
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
            System.out.println("🔄 Verificando existencia de mesa: " + mesaId);
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
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error verificando existencia de mesa: " + e.getMessage());
        }
    }
    
    private void pruebasAutomaticas() {
        System.out.println("\n🤖 ═══ EJECUTANDO PRUEBAS AUTOMÁTICAS ═══");
        System.out.println("🔄 Iniciando batería de pruebas para el Servidor Regional...\n");
        
        int pruebasExitosas = 0;
        int pruebasFallidas = 0;
        
        // Prueba 1: Verificar conectividad básica
        System.out.println("1️⃣ Prueba de Conectividad Básica");
        try {
            long startTime = System.currentTimeMillis();
            consultaMesaSQLiteProxy.ice_ping();
            long endTime = System.currentTimeMillis();
            
            System.out.println("   ✅ ÉXITO - Servidor responde en " + (endTime - startTime) + "ms");
            pruebasExitosas++;
        } catch (java.lang.Exception e) {
            System.out.println("   ❌ FALLO - Error de conectividad: " + e.getMessage());
            pruebasFallidas++;
        }
        
        // Prueba 2: Verificar proxy válido
        System.out.println("\n2️⃣ Prueba de Proxy Válido");
        try {
            if (consultaMesaSQLiteProxy != null) {
                String proxyString = consultaMesaSQLiteProxy.toString();
                if (proxyString.contains("consultaMesaSQLite") && proxyString.contains("localhost:8080")) {
                    System.out.println("   ✅ ÉXITO - Proxy configurado correctamente");
                    System.out.println("   📡 " + proxyString);
                    pruebasExitosas++;
                } else {
                    System.out.println("   ❌ FALLO - Proxy mal configurado: " + proxyString);
                    pruebasFallidas++;
                }
            } else {
                System.out.println("   ❌ FALLO - Proxy es null");
                pruebasFallidas++;
            }
        } catch (java.lang.Exception e) {
            System.out.println("   ❌ FALLO - Error verificando proxy: " + e.getMessage());
            pruebasFallidas++;
        }
        
        // Prueba 3: Verificar timeout de conexión
        System.out.println("\n3️⃣ Prueba de Timeout de Conexión");
        try {
            ObjectPrx proxyConTimeout = consultaMesaSQLiteProxy.ice_timeout(5000); // 5 segundos
            long startTime = System.currentTimeMillis();
            proxyConTimeout.ice_ping();
            long endTime = System.currentTimeMillis();
            
            if ((endTime - startTime) < 5000) {
                System.out.println("   ✅ ÉXITO - Timeout funciona correctamente (" + (endTime - startTime) + "ms)");
                pruebasExitosas++;
            } else {
                System.out.println("   ❌ FALLO - Timeout no respetado");
                pruebasFallidas++;
            }
        } catch (java.lang.Exception e) {
            System.out.println("   ❌ FALLO - Error en prueba de timeout: " + e.getMessage());
            pruebasFallidas++;
        }
        
        // Prueba 4: Verificar múltiples pings consecutivos
        System.out.println("\n4️⃣ Prueba de Múltiples Pings Consecutivos");
        try {
            int pingsExitosos = 0;
            long tiempoTotal = 0;
            
            for (int i = 0; i < 5; i++) {
                long startTime = System.currentTimeMillis();
                consultaMesaSQLiteProxy.ice_ping();
                long endTime = System.currentTimeMillis();
                
                pingsExitosos++;
                tiempoTotal += (endTime - startTime);
            }
            
            if (pingsExitosos == 5) {
                System.out.println("   ✅ ÉXITO - 5/5 pings exitosos (promedio: " + (tiempoTotal/5) + "ms)");
                pruebasExitosas++;
            } else {
                System.out.println("   ❌ FALLO - Solo " + pingsExitosos + "/5 pings exitosos");
                pruebasFallidas++;
            }
        } catch (java.lang.Exception e) {
            System.out.println("   ❌ FALLO - Error en pings múltiples: " + e.getMessage());
            pruebasFallidas++;
        }
        
        // Prueba 5: Verificar información del proxy
        System.out.println("\n5️⃣ Prueba de Información del Proxy");
        try {
            ObjectPrx identity = consultaMesaSQLiteProxy.ice_identity(communicator.stringToIdentity("consultaMesaSQLite"));
            if (identity != null) {
                System.out.println("   ✅ ÉXITO - Identidad del proxy válida");
                System.out.println("   🆔 " + identity.ice_getIdentity().name);
                pruebasExitosas++;
            } else {
                System.out.println("   ❌ FALLO - No se pudo obtener identidad");
                pruebasFallidas++;
            }
        } catch (java.lang.Exception e) {
            System.out.println("   ❌ FALLO - Error obteniendo información: " + e.getMessage());
            pruebasFallidas++;
        }
        
        // Resumen de pruebas
        System.out.println("\n" + "═".repeat(50));
        System.out.println("📊 RESUMEN DE PRUEBAS AUTOMÁTICAS");
        System.out.println("═".repeat(50));
        System.out.println("✅ Pruebas exitosas: " + pruebasExitosas);
        System.out.println("❌ Pruebas fallidas: " + pruebasFallidas);
        System.out.println("📈 Tasa de éxito: " + (pruebasExitosas * 100 / (pruebasExitosas + pruebasFallidas)) + "%");
        
        if (pruebasFallidas == 0) {
            System.out.println("🎉 ¡TODAS LAS PRUEBAS PASARON! El cliente está listo para usar.");
        } else {
            System.out.println("⚠️  Algunas pruebas fallaron. Verifique la configuración del servidor.");
        }
        
        System.out.println("═".repeat(50));
    }
} 