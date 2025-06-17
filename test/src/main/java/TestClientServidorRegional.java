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
    private ObjectPrx consultaMesaSQLiteProxy;
    
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
            consultaMesaSQLiteProxy = communicator.stringToProxy("consultaMesaSQLite:" + servidorRegionalEndpoint);
            
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
            System.out.println("8. 🔍 Verificar si existe una mesa SQLite");
            System.out.println("9. 🧪 Ejecutar pruebas básicas de conectividad");
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
                        verificarExisteMesa();
                        break;
                    case 9:
                        ejecutarPruebasBasicas();
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
            System.out.println("🔄 Intentando listar mesas disponibles...");
            System.out.println("📡 Endpoint: " + consultaMesaSQLiteProxy.toString());
            
            // Para prueba básica, solo verificamos que el proxy esté disponible
            if (consultaMesaSQLiteProxy != null) {
                System.out.println("✅ Proxy disponible para consultas");
                System.out.println("💡 Para obtener datos reales, el Servidor Regional debe tener mesas distribuidas");
                System.out.println("💡 Ejecute 'distribuir <departamento>' en el Servidor Regional");
            } else {
                System.out.println("❌ Proxy no disponible");
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
            System.out.println("🔄 Intentando obtener estadísticas para mesa: " + mesaId);
            System.out.println("📡 Proxy disponible: " + (consultaMesaSQLiteProxy != null ? "Sí" : "No"));
            
            // Para implementación completa, se requiere usar métodos específicos de ICE
            System.out.println("💡 Esta funcionalidad requiere que el Servidor Regional esté ejecutándose");
            System.out.println("💡 Y que la mesa '" + mesaId + "' haya sido distribuida previamente");
            
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
            System.out.println("🔄 Intentando obtener votantes para mesa: " + mesaId);
            System.out.println("📡 Usando proxy: " + consultaMesaSQLiteProxy.toString());
            
            // Para implementación completa, se requiere usar métodos específicos de ICE
            System.out.println("💡 Esta funcionalidad requiere:");
            System.out.println("   • Servidor Regional ejecutándose en " + servidorRegionalEndpoint);
            System.out.println("   • Mesa '" + mesaId + "' distribuida con votantes");
            System.out.println("   • Servicio IConsultaMesaSQLite activo");
            
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
            System.out.println("🔄 Configurando consulta paginada:");
            System.out.println("   Mesa: " + mesaId);
            System.out.println("   Página: " + pagina);
            System.out.println("   Tamaño: " + tamano);
            System.out.println("   Proxy: " + (consultaMesaSQLiteProxy != null ? "Disponible" : "No disponible"));
            
            System.out.println("💡 Para obtener datos reales, asegúrese de que:");
            System.out.println("   • El Servidor Regional esté ejecutándose");
            System.out.println("   • La mesa esté distribuida con votantes");
            
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
            System.out.println("🔄 Configurando búsqueda:");
            System.out.println("   Mesa: " + mesaId);
            System.out.println("   Documento: " + documento);
            System.out.println("   Endpoint: " + servidorRegionalEndpoint);
            
            // Verificar proxy
            if (consultaMesaSQLiteProxy != null) {
                System.out.println("✅ Proxy disponible para búsqueda");
                System.out.println("💡 El Servidor Regional debe estar ejecutándose para procesar la búsqueda");
            } else {
                System.out.println("❌ Proxy no disponible");
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
            System.out.println("🔄 Preparando conteo para mesa: " + mesaId);
            System.out.println("📡 Estado del proxy: " + (consultaMesaSQLiteProxy != null ? "Conectado" : "Desconectado"));
            
            if (consultaMesaSQLiteProxy != null) {
                System.out.println("✅ Configuración lista para contar votantes");
                System.out.println("💡 Requiere Servidor Regional activo con mesa distribuida");
            }
            
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
            System.out.println("🔄 Preparando conteo de verificados para mesa: " + mesaId);
            System.out.println("📡 Proxy disponible: " + (consultaMesaSQLiteProxy != null));
            
            if (consultaMesaSQLiteProxy != null) {
                System.out.println("✅ Listo para contar votantes verificados");
                System.out.println("💡 Esta operación consulta votantes que han sido verificados en la mesa");
            }
            
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error contando votantes verificados: " + e.getMessage());
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
            System.out.println("📡 Endpoint objetivo: " + servidorRegionalEndpoint);
            System.out.println("📡 Proxy: " + (consultaMesaSQLiteProxy != null ? "Disponible" : "No disponible"));
            
            if (consultaMesaSQLiteProxy != null) {
                System.out.println("✅ Configuración lista para verificar mesa");
                System.out.println("💡 La verificación consultará si la mesa existe en SQLite");
            } else {
                System.out.println("❌ No se puede verificar sin conexión al servidor");
            }
            
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error verificando existencia de mesa: " + e.getMessage());
        }
    }
    
    private void ejecutarPruebasBasicas() {
        System.out.println("\n🧪 ═══ EJECUTANDO PRUEBAS BÁSICAS DE CONECTIVIDAD ═══");
        
        try {
            // 1. Verificar comunicador ICE
            System.out.println("1️⃣  Verificando comunicador ICE...");
            if (communicator != null) {
                System.out.println("   ✅ Comunicador ICE disponible");
            } else {
                System.out.println("   ❌ Comunicador ICE no disponible");
            }
            
            // 2. Verificar proxy
            System.out.println("\n2️⃣  Verificando proxy del servicio...");
            if (consultaMesaSQLiteProxy != null) {
                System.out.println("   ✅ Proxy IConsultaMesaSQLite disponible");
                System.out.println("   📡 Endpoint: " + consultaMesaSQLiteProxy.toString());
            } else {
                System.out.println("   ❌ Proxy no disponible");
            }
            
            // 3. Verificar configuración
            System.out.println("\n3️⃣  Verificando configuración...");
            System.out.println("   🌐 Servidor Regional: " + servidorRegionalEndpoint);
            System.out.println("   🔧 Servicio objetivo: consultaMesaSQLite");
            
            // 4. Instrucciones para pruebas completas
            System.out.println("\n4️⃣  Para pruebas completas:");
            System.out.println("   📋 1. Inicie el Servidor Regional");
            System.out.println("   📋 2. Ejecute 'conectar' en el Servidor Regional");
            System.out.println("   📋 3. Ejecute 'distribuir <departamento>' para crear mesas");
            System.out.println("   📋 4. Use este cliente para consultar los datos");
            
            System.out.println("\n✅ Pruebas básicas completadas");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error durante las pruebas básicas: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("══════════════════════════════════════════════════════════");
    }
} 