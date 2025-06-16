// ServidorRegional.java
import Demo.IRegistrarVotoPrx;
import servidorRegional.*;
import com.zeroc.Ice.*;
import java.lang.Exception;
import java.util.Scanner;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServidorRegional {
    private static ConsultorVotantesRegional consultorVotantes;
    private static Scanner scanner;
    private static boolean servidorActivo = true;

    public static void main(String[] args) {
        System.out.println("🎯 === SERVIDOR REGIONAL CON CONSULTOR DE VOTANTES ===");
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try {
            // Configurar propiedades antes de crear el communicator
            configurarPropiedades(args);
            
            com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, extraArgs);
            
            scanner = new Scanner(System.in);

            if(!extraArgs.isEmpty()) {
                System.err.println("too many arguments");
                status = 1;
            } else {
                com.zeroc.Ice.Properties properties = communicator.getProperties();

                // Componentes existentes
                ReceptorVotos receptorVotos = new ReceptorVotos(properties.getProperty("Ice.ProgramName"));
                GestionCandidatos gestionCandidatos = new GestionCandidatos(communicator);

                // Nuevo componente: Consultor de Votantes
                consultorVotantes = new ConsultorVotantesRegional(communicator);

                // Crear adaptador con configuración
                com.zeroc.Ice.ObjectAdapter adapter;
                String endpoints = properties.getProperty("RegionalAdapter.Endpoints");
                
                if (endpoints != null && !endpoints.isEmpty()) {
                    // Usar configuración del archivo
                    adapter = communicator.createObjectAdapter("RegionalAdapter");
                    System.out.println("✅ Usando configuración: " + endpoints);
                } else {
                    // Configuración por defecto si no hay archivo de configuración
                    adapter = communicator.createObjectAdapterWithEndpoints(
                        "RegionalAdapter", "tcp -h localhost -p 8080");
                    System.out.println("✅ Usando configuración por defecto: tcp -h localhost -p 8080");
                }

                // Registrar componentes
                com.zeroc.Ice.Identity idReceptor = com.zeroc.Ice.Util.stringToIdentity("receptorVotos");
                adapter.add(receptorVotos, idReceptor);

                com.zeroc.Ice.Identity idGestion = com.zeroc.Ice.Util.stringToIdentity("gestionCandidatos");
                adapter.add(gestionCandidatos, idGestion);

                com.zeroc.Ice.Identity idReceptorTipo = com.zeroc.Ice.Util.stringToIdentity("IRegistrarVoto");
                adapter.add(receptorVotos, idReceptorTipo);

                com.zeroc.Ice.Identity idGestionTipo = com.zeroc.Ice.Util.stringToIdentity("ICargarCandidatos");
                adapter.add(gestionCandidatos, idGestionTipo);

                adapter.activate();
                
                System.out.println("✅ Servidor Regional iniciado correctamente");
                System.out.println("📊 Componentes disponibles:");
                System.out.println("   • ReceptorVotos: " + idReceptor.name + " y " + idReceptorTipo.name);
                System.out.println("   • GestionCandidatos: " + idGestion.name + " y " + idGestionTipo.name);
                System.out.println("   • ConsultorVotantesRegional: Consulta de votantes del servidor nacional");
                
                try {
                    com.zeroc.IceGrid.RegistryPrx registry = com.zeroc.IceGrid.RegistryPrx.checkedCast(
                        communicator.stringToProxy("DemoIceGrid/Registry"));
                    if (registry != null) {
                        System.out.println("✅ Conectado al Registry de IceGrid");
                    }
                } catch (Exception e) {
                    System.out.println("⚠️  No se pudo conectar al Registry: " + e.getMessage());
                }

                System.out.println("\n🎮 === CONSOLA INTERACTIVA ACTIVADA ===");
                System.out.println("💡 Comandos disponibles:");
                mostrarComandosDisponibles();

                // Hilo para manejar comandos de consola
                Thread consolaThread = new Thread(() -> manejarComandosConsola());
                consolaThread.setDaemon(false);
                consolaThread.start();

                // Configurar shutdown hook
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    servidorActivo = false;
                    if (consultorVotantes != null) {
                        consultorVotantes.cerrarConexion();
                    }
                    communicator.destroy();
                }));

                // Mantener el servidor corriendo
                while (servidorActivo) {
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            System.err.println("Error en ServidorRegional: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        }

        System.exit(status);
    }

    private static void configurarPropiedades(String[] args) {
        // Intentar cargar archivo de configuración
        java.io.InputStream configStream = ServidorRegional.class.getResourceAsStream("/servidorRegional.cfg");
        
        if (configStream != null) {
            try {
                java.util.Properties props = new java.util.Properties();
                props.load(configStream);
                
                // Establecer propiedades del sistema
                for (String key : props.stringPropertyNames()) {
                    String value = props.getProperty(key);
                    System.setProperty(key, value);
                }
                
                System.out.println("✅ Configuración cargada desde servidorRegional.cfg");
                configStream.close();
            } catch (Exception e) {
                System.out.println("⚠️  Error cargando configuración: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ️  No se encontró archivo de configuración, usando valores por defecto");
        }
    }

    private static void mostrarComandosDisponibles() {
        System.out.println("   conectar     - Conectar al servidor nacional");
        System.out.println("   estado       - Mostrar estado de conexión");
        System.out.println("   contar <dep> - Contar votantes por departamento (servidor nacional)");
        System.out.println("   listar <dep> - Listar votantes por departamento (servidor nacional)");
        System.out.println("   guardar <dep>- Consultar y guardar votantes en SQLite");
        System.out.println("   local <dep>  - Listar votantes desde SQLite local");
        System.out.println("   contarlocal <dep> - Contar votantes desde SQLite local");
        System.out.println("   paginar <dep> <pag> <tam> - Consulta paginada");
        System.out.println("   multiple <dep1,dep2,...> - Múltiples departamentos");
        System.out.println("   estadisticas - Ver estadísticas de base de datos local");
        System.out.println("   limpiar <dep>- Limpiar datos de departamento en SQLite");
        System.out.println("   ejemplos     - Ejecutar ejemplos de prueba");
        System.out.println("   ayuda        - Mostrar esta ayuda");
        System.out.println("   salir        - Terminar el servidor");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private static void manejarComandosConsola() {
        System.out.print("\n> ");
        
        while (servidorActivo && scanner.hasNextLine()) {
            String entrada = scanner.nextLine().trim();
            
            if (entrada.isEmpty()) {
                System.out.print("> ");
                continue;
            }

            String[] partes = entrada.split("\\s+");
            String comando = partes[0].toLowerCase();

            try {
                switch (comando) {
                    case "conectar":
                        comandoConectar();
                        break;
                    case "estado":
                        comandoEstado();
                        break;
                    case "contar":
                        if (partes.length > 1) {
                            comandoContar(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: contar <departamento>");
                        }
                        break;
                    case "listar":
                        if (partes.length > 1) {
                            comandoListar(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: listar <departamento>");
                        }
                        break;
                    case "guardar":
                        if (partes.length > 1) {
                            comandoGuardar(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: guardar <departamento>");
                        }
                        break;
                    case "local":
                        if (partes.length > 1) {
                            comandoLocal(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: local <departamento>");
                        }
                        break;
                    case "contarlocal":
                        if (partes.length > 1) {
                            comandoContarLocal(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: contarlocal <departamento>");
                        }
                        break;
                    case "paginar":
                        if (partes.length >= 4) {
                            comandoPaginar(partes[1], Integer.parseInt(partes[2]), Integer.parseInt(partes[3]));
                        } else {
                            System.out.println("❌ Uso: paginar <departamento> <pagina> <tamaño>");
                        }
                        break;
                    case "multiple":
                        if (partes.length > 1) {
                            comandoMultiple(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: multiple <dep1,dep2,dep3>");
                        }
                        break;
                    case "estadisticas":
                        comandoEstadisticas();
                        break;
                    case "limpiar":
                        if (partes.length > 1) {
                            comandoLimpiar(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: limpiar <departamento>");
                        }
                        break;
                    case "ejemplos":
                        comandoEjemplos();
                        break;
                    case "ayuda":
                        mostrarComandosDisponibles();
                        break;
                    case "salir":
                        System.out.println("🚪 Cerrando servidor...");
                        servidorActivo = false;
                        return;
                    default:
                        System.out.println("❌ Comando desconocido: " + comando);
                        System.out.println("💡 Escriba 'ayuda' para ver comandos disponibles");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Error: Número inválido");
            } catch (Exception e) {
                System.out.println("❌ Error ejecutando comando: " + e.getMessage());
            }

            if (servidorActivo) {
                System.out.print("> ");
            }
        }
    }

    private static void comandoConectar() {
        System.out.println("🔗 Conectando al servidor nacional...");
        boolean conectado = consultorVotantes.conectarConServidorNacional();
        if (conectado) {
            System.out.println("✅ ¡Conexión exitosa!");
        } else {
            System.out.println("❌ No se pudo conectar. Verifique que el servidor nacional esté ejecutándose.");
        }
    }

    private static void comandoEstado() {
        consultorVotantes.mostrarEstado();
    }

    private static void comandoContar(String departamento) {
        if (!verificarConexion()) return;
        
        System.out.println("🔢 Contando votantes en: " + departamento);
        long total = consultorVotantes.contarVotantesPorDepartamentos(Arrays.asList(departamento));
        System.out.println("📊 Total de votantes: " + String.format("%,d", total));
    }

    private static void comandoListar(String departamento) {
        if (!verificarConexion()) return;
        
        System.out.println("🔍 Consultando votantes de: " + departamento);
        List<Demo.CiudadanoInfo> votantes = consultorVotantes.consultarVotantesPorDepartamento(departamento);
        
        System.out.println("📊 Total encontrados: " + String.format("%,d", votantes.size()));
        
        if (votantes.isEmpty()) {
            System.out.println("   No se encontraron votantes.");
            return;
        }
        
        System.out.println("\n👥 Primeros 10 votantes:");
        int maxMostrar = Math.min(10, votantes.size());
        for (int i = 0; i < maxMostrar; i++) {
            Demo.CiudadanoInfo v = votantes.get(i);
            System.out.println(String.format("   %2d. %s %s (Doc: %s, Mesa: %s)", 
                i + 1, v.nombre, v.apellido, v.documento, v.mesa));
        }
        
        if (votantes.size() > 10) {
            System.out.println("   ... y " + String.format("%,d", votantes.size() - 10) + " votantes más");
        }
    }

    private static void comandoGuardar(String departamento) {
        if (!verificarConexion()) return;
        
        System.out.println("🔍 Consultando y guardando votantes de: " + departamento);
        List<Demo.CiudadanoInfo> votantes = consultorVotantes.consultarVotantesPorDepartamento(departamento, true);
        
        System.out.println("📊 Total encontrados: " + String.format("%,d", votantes.size()));
        
        if (votantes.isEmpty()) {
            System.out.println("   No se encontraron votantes.");
            return;
        }
        
        System.out.println("\n👥 Primeros 10 votantes:");
        int maxMostrar = Math.min(10, votantes.size());
        for (int i = 0; i < maxMostrar; i++) {
            Demo.CiudadanoInfo v = votantes.get(i);
            System.out.println(String.format("   %2d. %s %s (Doc: %s, Mesa: %s)", 
                i + 1, v.nombre, v.apellido, v.documento, v.mesa));
        }
        
        if (votantes.size() > 10) {
            System.out.println("   ... y " + String.format("%,d", votantes.size() - 10) + " votantes más");
        }
    }

    private static void comandoLocal(String departamento) {
        if (consultorVotantes == null) {
            System.out.println("❌ Consultor no inicializado");
            return;
        }
        
        System.out.println("🗄️ Consultando votantes locales de: " + departamento);
        List<Demo.CiudadanoInfo> votantes = consultorVotantes.consultarVotantesLocales(departamento);
        
        System.out.println("📊 Total encontrados: " + String.format("%,d", votantes.size()));
        
        if (votantes.isEmpty()) {
            System.out.println("   No se encontraron votantes en la base de datos local.");
            return;
        }
        
        System.out.println("\n👥 Primeros 10 votantes:");
        int maxMostrar = Math.min(10, votantes.size());
        for (int i = 0; i < maxMostrar; i++) {
            Demo.CiudadanoInfo v = votantes.get(i);
            System.out.println(String.format("   %2d. %s %s (Doc: %s, Mesa: %s)", 
                i + 1, v.nombre, v.apellido, v.documento, v.mesa));
        }
        
        if (votantes.size() > 10) {
            System.out.println("   ... y " + String.format("%,d", votantes.size() - 10) + " votantes más");
        }
    }

    private static void comandoContarLocal(String departamento) {
        if (consultorVotantes == null) {
            System.out.println("❌ Consultor no inicializado");
            return;
        }
        
        System.out.println("🔢 Contando votantes locales en: " + departamento);
        long total = consultorVotantes.contarVotantesLocales(departamento);
        System.out.println("📊 Total de votantes locales: " + String.format("%,d", total));
    }

    private static void comandoPaginar(String departamento, int pagina, int tamano) {
        if (!verificarConexion()) return;
        
        System.out.println("📄 Consulta paginada: " + departamento + " (página " + pagina + ", tamaño " + tamano + ")");
        Demo.ResultadoPaginado resultado = consultorVotantes.consultarVotantesPaginado(
            Arrays.asList(departamento), pagina, tamano);
        
        if (resultado != null) {
            System.out.println("📊 Página " + resultado.paginaActual + "/" + resultado.totalPaginas);
            System.out.println("   Total registros: " + String.format("%,d", resultado.totalRegistros));
            System.out.println("   En esta página: " + resultado.ciudadanos.length);
            
            for (int i = 0; i < resultado.ciudadanos.length; i++) {
                Demo.CiudadanoInfo v = resultado.ciudadanos[i];
                System.out.println(String.format("   %2d. %s %s (Doc: %s)", 
                    i + 1, v.nombre, v.apellido, v.documento));
            }
        }
    }

    private static void comandoMultiple(String departamentosStr) {
        if (!verificarConexion()) return;
        
        List<String> departamentos = Arrays.stream(departamentosStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        
        System.out.println("🌍 Contando votantes en: " + departamentos);
        long total = consultorVotantes.contarVotantesPorDepartamentos(departamentos);
        System.out.println("📊 Total en " + departamentos.size() + " departamentos: " + String.format("%,d", total));
    }

    private static void comandoEstadisticas() {
        if (consultorVotantes == null) {
            System.out.println("❌ Consultor no inicializado");
            return;
        }
        
        consultorVotantes.mostrarEstadisticasLocales();
    }

    private static void comandoLimpiar(String departamento) {
        if (consultorVotantes == null) {
            System.out.println("❌ Consultor no inicializado");
            return;
        }
        
        System.out.println("🧹 Limpiando datos locales de: " + departamento);
        int eliminados = consultorVotantes.limpiarDepartamentoLocal(departamento);
        System.out.println("✅ Eliminados " + eliminados + " registros");
    }

    private static void comandoEjemplos() {
        if (!verificarConexion()) return;
        
        System.out.println("🧪 Ejecutando ejemplos de prueba...");
        
        // Ejemplo 1: Consultar un departamento específico
        System.out.println("\n--- Ejemplo 1: Consultar Valle del Cauca ---");
        comandoContar("Valle del Cauca");
        
        // Ejemplo 2: Consultar múltiples departamentos
        System.out.println("\n--- Ejemplo 2: Múltiples departamentos ---");
        comandoMultiple("Valle del Cauca,Cundinamarca");
        
        // Ejemplo 3: Consulta paginada
        System.out.println("\n--- Ejemplo 3: Consulta paginada ---");
        comandoPaginar("Valle del Cauca", 1, 5);
        
        System.out.println("\n🎉 Ejemplos completados!");
    }

    private static boolean verificarConexion() {
        if (!consultorVotantes.verificarConexion()) {
            System.out.println("❌ No hay conexión con el servidor nacional.");
            System.out.println("💡 Use el comando 'conectar' primero.");
            return false;
        }
        return true;
    }
}