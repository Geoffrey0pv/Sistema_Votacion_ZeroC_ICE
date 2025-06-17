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
    private static ReceptorVotos receptorVotos;
    private static GestionCandidatos gestionCandidatos;
    private static GestorCandidatosSQLite gestorCandidatosSQLite;
    private static ConsultaCandidatosImpl consultaCandidatosImpl;
    private static ConsultorVotantesRegional consultorVotantes;
    private static DistribuidorMesas distribuidorMesas;
    private static ConsultaMesaSQLiteImpl consultaMesaSQLite;
    private static Scanner scanner;
    private static boolean servidorActivo = true;

    public static void main(String[] args) {
        System.out.println("🎯 === SERVIDOR REGIONAL CON CONSULTOR DE VOTANTES Y CANDIDATOS ===");
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try {
            // Configurar propiedades antes de crear el communicator
            configurarPropiedades(args);
            com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, extraArgs);
            scanner = new Scanner(System.in);
            //Runtime.getRuntime().addShutdownHook(new Thread(() -> communicator.destroy()));

            if(!extraArgs.isEmpty()) {
                System.err.println("too many arguments");
                status = 1;
            } else {
                com.zeroc.Ice.Properties properties = communicator.getProperties();

                // Componentes existentes
                receptorVotos = new ReceptorVotos(properties.getProperty("Ice.ProgramName"));
                gestionCandidatos = new GestionCandidatos(communicator);

                // NUEVO: Gestor de Candidatos SQLite
                gestorCandidatosSQLite = new GestorCandidatosSQLite(communicator);
                
                // NUEVO: Consulta de Candidatos especializada
                consultaCandidatosImpl = new ConsultaCandidatosImpl(gestorCandidatosSQLite, "ServidorRegional");

                // Nuevo componente: Consultor de Votantes
                consultorVotantes = new ConsultorVotantesRegional(communicator);
                
                // CONEXIÓN AUTOMÁTICA al servidor nacional
                System.out.println("🔗 Conectando automáticamente al servidor nacional...");
                boolean conexionExitosa = consultorVotantes.conectarConServidorNacional();
                if (conexionExitosa) {
                    System.out.println("✅ ¡Conexión automática exitosa!");
                } else {
                    System.out.println("❌ No se pudo conectar automáticamente. Use 'conectar' manualmente.");
                    System.out.println("💡 Verifique que el servidor nacional esté ejecutándose.");
                }
                
                // Nuevo componente: Distribuidor de Mesas
                distribuidorMesas = new DistribuidorMesas(communicator, consultorVotantes.getDatabaseManager());

                // Nuevo componente: Consultor de Mesas SQLite
                consultaMesaSQLite = new ConsultaMesaSQLiteImpl();

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

                // NUEVO: Registrar Gestor de Candidatos SQLite
                com.zeroc.Ice.Identity idCandidatosSQLite = com.zeroc.Ice.Util.stringToIdentity("consultaCandidatos");
                adapter.add(gestorCandidatosSQLite, idCandidatosSQLite);
                
                // NUEVO: Registrar Servicio de Consulta de Candidatos especializado
                com.zeroc.Ice.Identity idConsultaCandidatos = com.zeroc.Ice.Util.stringToIdentity("consultaCandidatosEspecializado");
                adapter.add(consultaCandidatosImpl, idConsultaCandidatos);

                com.zeroc.Ice.Identity idConsultaMesa = com.zeroc.Ice.Util.stringToIdentity("consultaMesaSQLite");
                adapter.add(consultaMesaSQLite, idConsultaMesa);

                adapter.activate();
                
                System.out.println("✅ Servidor Regional iniciado correctamente");
                System.out.println("📊 Componentes disponibles:");
                System.out.println("   • ConsultorVotantesRegional: Consulta de votantes del servidor nacional");
                System.out.println("   • DistribuidorMesas: Distribución de votantes por mesas");
                System.out.println("   • ConsultaMesaSQLite: Consulta información de mesas desde SQLite");
                System.out.println("   • GestorCandidatosSQLite: Consulta candidatos desde servidor nacional (10.147.17.113)");
                System.out.println("   • ConsultaCandidatosImpl: Servicio especializado de consulta de candidatos");
                System.out.println("Servidor Regional iniciado correctamente");
                System.out.println("- ReceptorVotos disponible en: " + idReceptor.name);
                System.out.println("- GestionCandidatos disponible en: " + idGestion.name);
                System.out.println("- ConsultaMesaSQLite disponible en: " + idConsultaMesa.name);
                System.out.println("- ConsultaCandidatos disponible en: " + idCandidatosSQLite.name);
                System.out.println("- ConsultaCandidatosEspecializado disponible en: " + idConsultaCandidatos.name);

                
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
        System.out.println("   distribuir <dep> - Distribuir votantes por mesas (crear archivos SQLite LOCAL)");
        System.out.println("   distribuirremoto <dep> - Distribuir archivos SQLite a mesas REMOTAS");
        System.out.println("   registrar <mesa> <endpoint> - Registrar mesa remota");
        System.out.println("   desregistrar <mesa> - Desregistrar mesa remota");
        System.out.println("   verificarmesas - Verificar conectividad con mesas remotas");
        System.out.println("   mesas <dep>  - Ver mesas identificadas de un departamento");
        System.out.println("   estadisticasdist - Ver estadísticas de distribución");
        System.out.println("   limpiardist <dep> - Limpiar archivos de distribución");
        System.out.println("   paginar <dep> <pag> <tam> - Consulta paginada");
        System.out.println("   multiple <dep1,dep2,...> - Múltiples departamentos");
        System.out.println("   estadisticas - Ver estadísticas de base de datos local");
        System.out.println("   limpiar <dep>- Limpiar datos de departamento en SQLite");
        // Nuevos comandos para consulta de mesas SQLite
        System.out.println("   ━━━ CONSULTA MESAS SQLite ━━━");
        System.out.println("   listarmesas  - Listar todas las mesas SQLite disponibles");
        System.out.println("   infomesa <mesaId> - Obtener información completa de una mesa");
        System.out.println("   estadsmesa <mesaId> - Obtener estadísticas de una mesa");
        System.out.println("   votantesmesa <mesaId> [pag] [tam] - Obtener votantes de una mesa (paginado)");
        System.out.println("   buscarvotante <mesaId> <documento> - Buscar votante en una mesa");
        System.out.println("   contarmesa <mesaId> - Contar votantes en una mesa");
        System.out.println("   logsmesa <mesaId> - Obtener logs de verificación de una mesa");
        System.out.println("   verificarservicio - Verificar servicio de consulta SQLite");
        // Nuevos comandos para candidatos
        System.out.println("   ━━━ CONSULTA CANDIDATOS ━━━");
        System.out.println("   candidatos   - Listar todos los candidatos");
        System.out.println("   candidatospartido <partido> - Candidatos por partido");
        System.out.println("   buscarcandidato <id> - Buscar candidato por ID");
        System.out.println("   buscarnombre <nombre> - Buscar candidatos por nombre");
        System.out.println("   partidos     - Listar partidos disponibles");
        System.out.println("   sincronizarcandidatos - Sincronizar con servidor nacional");
        System.out.println("   validarcandidato <id> - Validar candidato por ID");
        System.out.println("   estadscandidatos - Estadísticas de candidatos");
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
                    case "distribuir":
                        if (partes.length > 1) {
                            comandoDistribuir(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: distribuir <departamento>");
                        }
                        break;
                    case "mesas":
                        if (partes.length > 1) {
                            comandoMesas(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: mesas <departamento>");
                        }
                        break;
                    case "estadisticasdist":
                        comandoEstadisticasDistribucion();
                        break;
                    case "limpiardist":
                        if (partes.length > 1) {
                            comandoLimpiarDistribucion(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: limpiardist <departamento>");
                        }
                        break;
                    case "distribuirremoto":
                        if (partes.length > 1) {
                            comandoDistribuirRemoto(String.join(" ", Arrays.copyOfRange(partes, 1, partes.length)));
                        } else {
                            System.out.println("❌ Uso: distribuirremoto <departamento>");
                        }
                        break;
                    case "registrar":
                        if (partes.length > 2) {
                            comandoRegistrarMesa(partes[1], partes[2]);
                        } else {
                            System.out.println("❌ Uso: registrar <mesaId> <endpoint>");
                        }
                        break;
                    case "desregistrar":
                        if (partes.length > 1) {
                            comandoDesregistrarMesa(partes[1]);
                        } else {
                            System.out.println("❌ Uso: desregistrar <mesaId>");
                        }
                        break;
                    case "verificarmesas":
                        comandoVerificarMesas();
                        break;

                    // Nuevos comandos para consulta de mesas SQLite
                    case "listarmesas":
                        comandoListarMesas();
                        break;
                    case "infomesa":
                        if (partes.length > 1) {
                            comandoInfoMesa(partes[1]);
                        } else {
                            System.out.println("❌ Uso: infomesa <mesaId>");
                        }
                        break;
                    case "estadsmesa":
                        if (partes.length > 1) {
                            comandoEstadisticasMesa(partes[1]);
                        } else {
                            System.out.println("❌ Uso: estadsmesa <mesaId>");
                        }
                        break;
                    case "votantesmesa":
                        if (partes.length > 1) {
                            int pagina = partes.length > 2 ? Integer.parseInt(partes[2]) : 1;
                            int tamano = partes.length > 3 ? Integer.parseInt(partes[3]) : 10;
                            comandoVotantesMesa(partes[1], pagina, tamano);
                        } else {
                            System.out.println("❌ Uso: votantesmesa <mesaId> [pagina] [tamaño]");
                        }
                        break;
                    case "buscarvotante":
                        if (partes.length > 2) {
                            comandoBuscarVotante(partes[1], partes[2]);
                        } else {
                            System.out.println("❌ Uso: buscarvotante <mesaId> <documento>");
                        }
                        break;
                    case "contarmesa":
                        if (partes.length > 1) {
                            comandoContarMesa(partes[1]);
                        } else {
                            System.out.println("❌ Uso: contarmesa <mesaId>");
                        }
                        break;
                    case "logsmesa":
                        if (partes.length > 1) {
                            comandoLogsMesa(partes[1]);
                        } else {
                            System.out.println("❌ Uso: logsmesa <mesaId>");
                        }
                        break;
                    case "verificarservicio":
                        comandoVerificarServicioMesa();
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

    private static void comandoDistribuir(String departamento) {
        if (distribuidorMesas == null) {
            System.out.println("❌ Distribuidor de Mesas no inicializado");
            return;
        }
        
        System.out.println("🗳️ Distribuyendo votantes de: " + departamento);
        boolean distribuidos = distribuidorMesas.distribuirVotantesPorDepartamento(departamento);
        if (distribuidos) {
            System.out.println("✅ Votantes distribuidos correctamente");
        } else {
            System.out.println("❌ No se pudo distribuir los votantes");
        }
    }

    private static void comandoMesas(String departamento) {
        if (distribuidorMesas == null) {
            System.out.println("❌ Distribuidor de Mesas no inicializado");
            return;
        }
        
        System.out.println("🔍 Consultando mesas identificadas de: " + departamento);
        java.util.Set<String> mesas = distribuidorMesas.obtenerMesasDelDepartamento(departamento);
        
        System.out.println("📊 Total identificadas: " + String.format("%,d", mesas.size()));
        
        if (mesas.isEmpty()) {
            System.out.println("   No se encontraron mesas. Ejecute 'guardar " + departamento + "' primero.");
            return;
        }
        
        System.out.println("\n🗳️ Mesas identificadas:");
        int i = 1;
        for (String mesaId : mesas) {
            System.out.println(String.format("   %2d. Mesa %s", i++, mesaId));
        }
        
        System.out.println("\n💡 Use 'distribuir " + departamento + "' para crear archivos SQLite por mesa");
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
            System.out.println("💡 La conexión automática falló al iniciar. Use 'conectar' para reintentar.");
            System.out.println("   Verifique que el servidor nacional esté ejecutándose en tcp -h localhost -p 9090");
            return false;
        }
        return true;
    }

    private static void comandoEstadisticasDistribucion() {
        if (distribuidorMesas == null) {
            System.out.println("❌ Distribuidor de Mesas no inicializado");
            return;
        }
        
        java.util.List<String> estadisticas = distribuidorMesas.obtenerEstadisticasDistribucion();
        for (String linea : estadisticas) {
            System.out.println(linea);
        }
    }

    private static void comandoLimpiarDistribucion(String departamento) {
        if (distribuidorMesas == null) {
            System.out.println("❌ Distribuidor de Mesas no inicializado");
            return;
        }
        
        System.out.println("🧹 Limpiando archivos de distribución de: " + departamento);
        int eliminados = distribuidorMesas.limpiarDistribucionDepartamento(departamento);
        System.out.println("✅ Eliminados " + eliminados + " archivos");
    }
    
    private static void comandoDistribuirRemoto(String departamento) {
        if (distribuidorMesas == null) {
            System.out.println("❌ Distribuidor de Mesas no inicializado");
            return;
        }
        
        System.out.println("🌐 Iniciando distribución remota para: " + departamento);
        boolean exito = distribuidorMesas.distribuirVotantesRemotamente(departamento);
        
        if (exito) {
            System.out.println("✅ Distribución remota completada exitosamente");
        } else {
            System.out.println("❌ Error en distribución remota");
        }
    }
    
    private static void comandoRegistrarMesa(String mesaId, String endpoint) {
        if (distribuidorMesas == null) {
            System.out.println("❌ Distribuidor de Mesas no inicializado");
            return;
        }
        
        System.out.println("📝 Registrando mesa remota: " + mesaId + " -> " + endpoint);
        distribuidorMesas.registrarMesaRemota(mesaId, endpoint);
        System.out.println("✅ Mesa remota registrada exitosamente");
    }
    
    private static void comandoDesregistrarMesa(String mesaId) {
        if (distribuidorMesas == null) {
            System.out.println("❌ Distribuidor de Mesas no inicializado");
            return;
        }
        
        System.out.println("🗑️ Desregistrando mesa remota: " + mesaId);
        boolean exito = distribuidorMesas.desregistrarMesaRemota(mesaId);
        
        if (exito) {
            System.out.println("✅ Mesa desregistrada exitosamente");
        } else {
            System.out.println("❌ Mesa no estaba registrada");
        }
    }
    
    private static void comandoVerificarMesas() {
        if (distribuidorMesas == null) {
            System.out.println("❌ Distribuidor de Mesas no inicializado");
            return;
        }
        
        System.out.println("🔗 Verificando conectividad con mesas remotas...");
        
        // Mostrar mesas registradas
        java.util.Map<String, String> mesasRegistradas = distribuidorMesas.obtenerMesasRegistradas();
        System.out.println("📋 Mesas registradas: " + mesasRegistradas.size());
        
        if (mesasRegistradas.isEmpty()) {
            System.out.println("⚠️ No hay mesas registradas");
            System.out.println("💡 Use 'registrar <mesaId> <endpoint>' para registrar mesas");
            return;
        }
        
        for (java.util.Map.Entry<String, String> entry : mesasRegistradas.entrySet()) {
            System.out.println("   • Mesa " + entry.getKey() + " -> " + entry.getValue());
        }
        
        // Verificar conectividad
        int conectadas = distribuidorMesas.verificarConectividadMesas();
        System.out.println("📊 Resultado: " + conectadas + "/" + mesasRegistradas.size() + " mesas conectadas");
    }

    // Nuevos comandos para consulta de mesas SQLite
    private static void comandoListarMesas() {
        if (consultaMesaSQLite == null) {
            System.out.println("❌ Consultor de Mesas SQLite no inicializado");
            return;
        }
        
        System.out.println("🔍 Consultando todas las mesas SQLite disponibles...");
        String[] mesas = consultaMesaSQLite.listarMesasDisponibles(null);
        
        System.out.println("📊 Total encontradas: " + String.format("%,d", mesas.length));
        
        if (mesas.length == 0) {
            System.out.println("⚠️ No hay mesas SQLite disponibles");
            return;
        }
        
        System.out.println("\n🗳️ Mesas SQLite:");
        for (int i = 0; i < mesas.length; i++) {
            System.out.println(String.format("   %2d. Mesa %s", i + 1, mesas[i]));
        }
    }

    private static void comandoInfoMesa(String mesaId) {
        if (consultaMesaSQLite == null) {
            System.err.println("❌ Servicio de consulta SQLite no disponible");
            return;
        }
        
        System.out.println("🔍 Consultando información completa de la mesa: " + mesaId);
        Demo.InfoCompletaMesa infoCompleta = consultaMesaSQLite.obtenerInfoCompletaMesa(mesaId, null);
        
        if (infoCompleta != null && infoCompleta.archivoExiste) {
            System.out.println("📋 Información completa de la mesa:");
            System.out.println("   Mesa ID: " + mesaId);
            System.out.println("   Archivo existe: " + infoCompleta.archivoExiste);
            System.out.println("   Ruta archivo: " + infoCompleta.rutaArchivo);
            
            if (infoCompleta.estadisticas != null) {
                System.out.println("   Departamento: " + infoCompleta.estadisticas.departamento);
                System.out.println("   Municipio: " + infoCompleta.estadisticas.municipio);
                System.out.println("   Total votantes: " + infoCompleta.estadisticas.totalVotantes);
                System.out.println("   Fecha creación: " + infoCompleta.estadisticas.fechaCreacion);
            }
        } else {
            System.out.println("❌ No se encontró información para la mesa: " + mesaId);
        }
    }

    private static void comandoEstadisticasMesa(String mesaId) {
        if (consultaMesaSQLite == null) {
            System.err.println("❌ Servicio de consulta SQLite no disponible");
            return;
        }
        
        System.out.println("🔢 Consultando estadísticas de la mesa: " + mesaId);
        Demo.EstadisticasMesaSQLite estadisticas = consultaMesaSQLite.obtenerEstadisticasMesa(mesaId, null);
        
        if (estadisticas != null) {
            System.out.println("📋 Estadísticas de la mesa:");
            System.out.println("   Mesa ID: " + estadisticas.mesaId);
            System.out.println("   Departamento: " + estadisticas.departamento);
            System.out.println("   Municipio: " + estadisticas.municipio);
            System.out.println("   Puesto: " + estadisticas.puesto);
            System.out.println("   Total votantes: " + estadisticas.totalVotantes);
            System.out.println("   Votantes verificados: " + estadisticas.votantesVerificados);
            System.out.println("   Mesa activa: " + (estadisticas.mesaActiva == 1 ? "Sí" : "No"));
            System.out.println("   Fecha de creación: " + estadisticas.fechaCreacion);
            System.out.println("   Última actualización: " + estadisticas.ultimaActualizacion);
        } else {
            System.out.println("❌ No se encontraron estadísticas para la mesa: " + mesaId);
        }
    }

    private static void comandoVotantesMesa(String mesaId, int pagina, int tamano) {
        if (consultaMesaSQLite == null) {
            System.err.println("❌ Servicio de consulta SQLite no disponible");
            return;
        }
        
        System.out.println("📄 Consultando votantes de la mesa: " + mesaId + " (página " + pagina + ", tamaño " + tamano + ")");
        Demo.VotanteMesa[] votantes = consultaMesaSQLite.obtenerVotantesPaginados(mesaId, pagina, tamano, null);
        
        if (votantes != null && votantes.length > 0) {
            System.out.println("📊 Se encontraron " + votantes.length + " votantes:");
            for (int i = 0; i < Math.min(votantes.length, 10); i++) { // Mostrar máximo 10
                Demo.VotanteMesa v = votantes[i];
                System.out.println("   " + (i+1) + ". " + v.documento + " - " + v.nombre + " " + v.apellido + 
                                 " (Verificado: " + (v.verificado == 1 ? "Sí" : "No") + ")");
            }
            if (votantes.length > 10) {
                System.out.println("   ... y " + (votantes.length - 10) + " más");
            }
        } else {
            System.out.println("❌ No se encontraron votantes para la mesa: " + mesaId);
        }
    }

    private static void comandoBuscarVotante(String mesaId, String documento) {
        if (consultaMesaSQLite == null) {
            System.err.println("❌ Servicio de consulta SQLite no disponible");
            return;
        }
        
        System.out.println("🔍 Buscando votante en la mesa: " + mesaId + " (documento: " + documento + ")");
        Demo.VotanteMesa votante = consultaMesaSQLite.buscarVotantePorDocumento(mesaId, documento, null);
        
        if (votante != null) {
            System.out.println("📋 Información del votante:");
            System.out.println("   ID: " + votante.id);
            System.out.println("   Documento: " + votante.documento);
            System.out.println("   Nombre: " + votante.nombre + " " + votante.apellido);
            System.out.println("   Mesa: " + votante.mesa + " (ID: " + votante.mesaId + ")");
            System.out.println("   Puesto: " + votante.puesto);
            System.out.println("   Municipio: " + votante.municipio);
            System.out.println("   Departamento: " + votante.departamento);
            System.out.println("   Verificado: " + (votante.verificado == 1 ? "Sí" : "No"));
            System.out.println("   Fecha asignación: " + votante.fechaAsignacion);
            if (votante.verificado == 1) {
                System.out.println("   Fecha verificación: " + votante.fechaVerificacion);
            }
        } else {
            System.out.println("❌ Votante no encontrado en la mesa: " + mesaId);
        }
    }

    private static void comandoContarMesa(String mesaId) {
        if (consultaMesaSQLite == null) {
            System.err.println("❌ Servicio de consulta SQLite no disponible");
            return;
        }
        
        System.out.println("🔢 Contando votantes en la mesa: " + mesaId);
        int total = consultaMesaSQLite.contarVotantesMesa(mesaId, null);
        int verificados = consultaMesaSQLite.contarVotantesVerificados(mesaId, null);
        
        System.out.println("📊 Resultados para mesa " + mesaId + ":");
        System.out.println("   Total de votantes: " + String.format("%,d", total));
        System.out.println("   Votantes verificados: " + String.format("%,d", verificados));
        if (total > 0) {
            double porcentaje = (verificados * 100.0) / total;
            System.out.println("   Porcentaje verificado: " + String.format("%.2f%%", porcentaje));
        }
    }

    private static void comandoLogsMesa(String mesaId) {
        if (consultaMesaSQLite == null) {
            System.err.println("❌ Servicio de consulta SQLite no disponible");
            return;
        }
        
        System.out.println("📋 Logs de verificación de la mesa: " + mesaId);
        Demo.LogVerificacion[] logs = consultaMesaSQLite.obtenerLogsVerificacion(mesaId, null);
        
        if (logs == null || logs.length == 0) {
            System.out.println("⚠️ No hay logs de verificación para la mesa: " + mesaId);
        } else {
            System.out.println("\n🗓️ Logs de verificación (" + logs.length + " registros):");
            int maxLogs = Math.min(logs.length, 20); // Mostrar máximo 20 logs
            for (int i = 0; i < maxLogs; i++) {
                Demo.LogVerificacion log = logs[i];
                System.out.println("   Log " + (i+1) + ":");
                System.out.println("     ID: " + log.id);
                System.out.println("     Documento: " + log.documento);
                System.out.println("     Acción: " + log.accion);
                System.out.println("     Resultado: " + log.resultado);
                System.out.println("     Timestamp: " + log.timestamp);
                System.out.println("   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }
            if (logs.length > maxLogs) {
                System.out.println("   ... y " + (logs.length - maxLogs) + " logs más");
            }
        }
    }

    private static void comandoVerificarServicioMesa() {
        if (consultaMesaSQLite == null) {
            System.err.println("❌ Servicio de consulta SQLite no disponible");
            return;
        }
        
        System.out.println("🔗 Verificando servicio de consulta SQLite...");
        boolean servicioActivo = consultaMesaSQLite.verificarServicio(null);
        
        if (servicioActivo) {
            System.out.println("✅ Servicio de consulta SQLite activo");
            
            // Obtener estadísticas adicionales
            String[] mesas = consultaMesaSQLite.listarMesasDisponibles(null);
            System.out.println("📊 Mesas SQLite disponibles: " + mesas.length);
            
            if (mesas.length > 0) {
                System.out.println("📋 Primeras 5 mesas disponibles:");
                for (int i = 0; i < Math.min(mesas.length, 5); i++) {
                    System.out.println("   - Mesa: " + mesas[i]);
                }
            }
        } else {
            System.out.println("❌ Servicio de consulta SQLite inactivo o con problemas");
        }
    }
}