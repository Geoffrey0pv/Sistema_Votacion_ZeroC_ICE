import com.zeroc.Ice.*;
import Demo.*;
import java.util.*;

public class TestClient {
    private Communicator communicator;
    private Scanner scanner;
    private String serverEndpoint = "tcp -h 10.147.17.113 -p 9090";
    
    // Referencias a los proxies de servicios
    private IConsultaMesaPrx consultaMesaProxy;
    private IConsultaCiudadanosPrx consultaCiudadanosProxy;
    private IConsultaCandidatosPrx consultaCandidatosProxy;
    private IRegistroVotosPrx registroVotosProxy;
    
    public TestClient() {
        scanner = new Scanner(System.in);
    }
    
    public static void main(String[] args) {
        TestClient client = new TestClient();
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
            
            // Conectar a los servicios
            connectToServices();
            
            // Mostrar menú principal
            showWelcome();
            mainMenu();
            
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
    
    private void connectToServices() {
        try {
            System.out.println("🔌 Conectando al Servidor Nacional...");
            System.out.println("   Endpoint: " + serverEndpoint);
            
            // Conectar ConsultaMesa
            try {
                ObjectPrx base = communicator.stringToProxy("ConsultaMesa:" + serverEndpoint);
                consultaMesaProxy = IConsultaMesaPrx.checkedCast(base);
                System.out.println("✅ Servicio ConsultaMesa conectado");
            } catch (java.lang.Exception e) {
                System.out.println("⚠️  Servicio ConsultaMesa no disponible: " + e.getMessage());
            }
            
            // Conectar ConsultaCiudadanos
            try {
                ObjectPrx base = communicator.stringToProxy("ConsultaCiudadanos:" + serverEndpoint);
                consultaCiudadanosProxy = IConsultaCiudadanosPrx.checkedCast(base);
                System.out.println("✅ Servicio ConsultaCiudadanos conectado");
            } catch (java.lang.Exception e) {
                System.out.println("⚠️  Servicio ConsultaCiudadanos no disponible: " + e.getMessage());
            }
            
            // Conectar ConsultaCandidatos
            try {
                ObjectPrx base = communicator.stringToProxy("ConsultaCandidatos:" + serverEndpoint);
                consultaCandidatosProxy = IConsultaCandidatosPrx.checkedCast(base);
                System.out.println("✅ Servicio ConsultaCandidatos conectado");
            } catch (java.lang.Exception e) {
                System.out.println("⚠️  Servicio ConsultaCandidatos no disponible: " + e.getMessage());
            }
            
            // Conectar RegistroVotos
            try {
                ObjectPrx base = communicator.stringToProxy("RegistroVotos:" + serverEndpoint);
                registroVotosProxy = IRegistroVotosPrx.checkedCast(base);
                System.out.println("✅ Servicio RegistroVotos conectado");
            } catch (java.lang.Exception e) {
                System.out.println("⚠️  Servicio RegistroVotos no disponible: " + e.getMessage());
            }
            
            System.out.println("═══════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error conectando a servicios: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    private void showWelcome() {
        System.out.println();
        System.out.println("🧪 ═══════════════════════════════════════════════════════════");
        System.out.println("   CLIENTE DE PRUEBAS - SERVIDOR NACIONAL");
        System.out.println("   Sistema de Votación ZeroC ICE");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
    }
    
    private void mainMenu() {
        while (true) {
            System.out.println("\n📋 MENÚ DE SERVICIOS:");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("1. 🏛️  Consultar Lugar de Votación por Documento");
            System.out.println("2. 👥 Obtener Votantes por Departamento");
            System.out.println("3. 🗳️  Consultar Candidatos Electorales");
            System.out.println("4. ✅ Registro de Votos");
            System.out.println("0. 🚪 Salir");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.print("Seleccione una opción: ");
            
            try {
                int option = Integer.parseInt(scanner.nextLine().trim());
                
                switch (option) {
                    case 1:
                        consultarLugarVotacion();
                        break;
                    case 2:
                        obtenerVotantesPorDepartamento();
                        break;
                    case 3:
                        consultarCandidatosElectorales();
                        break;
                    case 4:
                        registroVotosMenu();
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
            }
        }
    }
    
    private void consultarLugarVotacion() {
        if (consultaMesaProxy == null) {
            System.out.println("❌ Servicio ConsultaMesa no disponible");
            return;
        }
        
        System.out.println("\n🏛️ ═══ CONSULTAR LUGAR DE VOTACIÓN ═══");
        System.out.println("Este servicio permite buscar el lugar de votación usando un documento de identidad.");
        System.out.println();
        
        System.out.print("📄 Ingrese el número de documento: ");
        String documento = scanner.nextLine().trim();
        
        if (documento.isEmpty()) {
            System.out.println("❌ El documento no puede estar vacío");
            return;
        }
        
        System.out.println("⏳ Consultando lugar de votación...");
        long startTime = System.currentTimeMillis();
        
        try {
            MesaInfo mesaInfo = consultaMesaProxy.consultarMesaPorDocumento(documento);
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n✅ Lugar de votación encontrado en " + (endTime - startTime) + "ms:");
            System.out.println("══════════════════════════════════════════════════════════");
            System.out.println("🌍 Departamento: " + mesaInfo.departamento);
            System.out.println("🏙️  Municipio: " + mesaInfo.municipio);
            System.out.println("🏢 Puesto de Votación: " + mesaInfo.puesto);
            System.out.println("🗳️  Mesa: " + mesaInfo.mesa);
            System.out.println("══════════════════════════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            long endTime = System.currentTimeMillis();
            System.out.println("❌ Error consultando lugar de votación (" + (endTime - startTime) + "ms): " + e.getMessage());
        }
    }
    
    private void obtenerVotantesPorDepartamento() {
        if (consultaCiudadanosProxy == null) {
            System.out.println("❌ Servicio ConsultaCiudadanos no disponible");
            return;
        }
        
        System.out.println("\n👥 ═══ OBTENER VOTANTES POR DEPARTAMENTO ═══");
        System.out.println("Este servicio permite obtener la lista de votantes por departamento(s).");
        System.out.println();
        
        while (true) {
            System.out.println("Opciones disponibles:");
            System.out.println("1. Consultar votantes (método estándar - límite 1000)");
            System.out.println("2. Consultar votantes con paginación");
            System.out.println("3. Contar total de votantes");
            System.out.println("4. Consultar votantes con límite personalizado");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione una opción: ");
            
            try {
                int option = Integer.parseInt(scanner.nextLine().trim());
                
                switch (option) {
                    case 1:
                        consultarVotantesEstandar();
                        break;
                    case 2:
                        consultarVotantesPaginado();
                        break;
                    case 3:
                        contarVotantes();
                        break;
                    case 4:
                        consultarVotantesConLimite();
                        break;
                    case 0:
                        return;
                    default:
                        System.out.println("❌ Opción inválida");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Por favor, ingrese un número válido.");
            } catch (java.lang.Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
    }
    
    private void consultarVotantesEstandar() {
        System.out.println("\n📋 CONSULTA ESTÁNDAR (Límite 1000 registros)");
        String[] departamentos = obtenerDepartamentos();
        if (departamentos == null) return;
        
        try {
            System.out.println("🔍 Consultando votantes...");
            long startTime = System.currentTimeMillis();
            
            CiudadanoInfo[] votantes = consultaCiudadanosProxy.consultarCiudadanosPorDepartamentos(departamentos);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADOS:");
            System.out.println("══════════════════════════════════════");
            System.out.println("📊 Total de votantes encontrados: " + votantes.length);
            System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
            if (votantes.length > 0) {
                mostrarVotantes(votantes, Math.min(5, votantes.length));
            }
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error en la consulta: " + e.getMessage());
        }
    }
    
    private void consultarVotantesPaginado() {
        System.out.println("\n📄 CONSULTA CON PAGINACIÓN");
        String[] departamentos = obtenerDepartamentos();
        if (departamentos == null) return;
        
        System.out.print("📑 Ingrese el tamaño de página (por defecto 50): ");
        String tamanoStr = scanner.nextLine().trim();
        int tamano = tamanoStr.isEmpty() ? 50 : Integer.parseInt(tamanoStr);
        
        int paginaActual = 1;
        
        try {
            while (true) {
                System.out.println("\n🔍 Consultando página " + paginaActual + "...");
                long startTime = System.currentTimeMillis();
                
                ResultadoPaginado resultado = consultaCiudadanosProxy.consultarCiudadanosPaginado(
                    departamentos, paginaActual, tamano);
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                System.out.println("\n✅ RESULTADOS - PÁGINA " + paginaActual + ":");
                System.out.println("══════════════════════════════════════");
                System.out.println("📊 Votantes en esta página: " + resultado.ciudadanos.length);
                System.out.println("📈 Total de votantes: " + resultado.totalRegistros);
                System.out.println("📑 Página actual: " + resultado.paginaActual + " de " + resultado.totalPaginas);
                System.out.println("▶️  Hay más páginas: " + (resultado.hayMasPaginas ? "Sí" : "No"));
                System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
                System.out.println("══════════════════════════════════════");
                
                if (resultado.ciudadanos.length > 0) {
                    mostrarVotantes(resultado.ciudadanos, Math.min(3, resultado.ciudadanos.length));
                }
                
                if (!resultado.hayMasPaginas) {
                    System.out.println("📄 No hay más páginas disponibles.");
                    break;
                }
                
                System.out.print("\n➡️  ¿Desea ver la siguiente página? (s/n): ");
                String continuar = scanner.nextLine().trim().toLowerCase();
                if (!continuar.equals("s") && !continuar.equals("si")) {
                    break;
                }
                
                paginaActual++;
            }
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error en la consulta paginada: " + e.getMessage());
        }
    }
    
    private void contarVotantes() {
        System.out.println("\n🔢 CONTAR VOTANTES");
        String[] departamentos = obtenerDepartamentos();
        if (departamentos == null) return;
        
        try {
            System.out.println("🔍 Contando votantes...");
            long startTime = System.currentTimeMillis();
            
            long total = consultaCiudadanosProxy.contarCiudadanosPorDepartamentos(departamentos);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADO DEL CONTEO:");
            System.out.println("══════════════════════════════════════");
            System.out.println("📊 Total de votantes: " + total);
            System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error en el conteo: " + e.getMessage());
        }
    }
    
    private void consultarVotantesConLimite() {
        System.out.println("\n🎯 CONSULTA CON LÍMITE PERSONALIZADO");
        String[] departamentos = obtenerDepartamentos();
        if (departamentos == null) return;
        
        System.out.print("🔢 Ingrese el límite de registros: ");
        try {
            int limite = Integer.parseInt(scanner.nextLine().trim());
            
            System.out.println("🔍 Consultando votantes con límite de " + limite + "...");
            long startTime = System.currentTimeMillis();
            
            CiudadanoInfo[] votantes = consultaCiudadanosProxy.consultarCiudadanosConLimite(departamentos, limite);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADOS:");
            System.out.println("══════════════════════════════════════");
            System.out.println("📊 Votantes encontrados: " + votantes.length);
            System.out.println("🎯 Límite solicitado: " + limite);
            System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
            if (votantes.length > 0) {
                mostrarVotantes(votantes, Math.min(5, votantes.length));
            }
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Por favor, ingrese un número válido.");
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error en la consulta: " + e.getMessage());
        }
    }
    
    private String[] obtenerDepartamentos() {
        System.out.println("Ingrese los departamentos (separados por comas):");
        System.out.println("Ejemplos: Antioquia, Cundinamarca, Valle del Cauca");
        System.out.print("Departamentos: ");
        
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            System.out.println("❌ Debe ingresar al menos un departamento");
            return null;
        }
        
        String[] departamentos = input.split(",");
        for (int i = 0; i < departamentos.length; i++) {
            departamentos[i] = departamentos[i].trim();
        }
        
        System.out.println("📍 Departamentos a consultar: " + Arrays.toString(departamentos));
        return departamentos;
    }
    
    private void mostrarVotantes(CiudadanoInfo[] votantes, int limite) {
        System.out.println("\n👤 MUESTRA DE VOTANTES (primeros " + limite + "):");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        for (int i = 0; i < limite && i < votantes.length; i++) {
            CiudadanoInfo v = votantes[i];
            System.out.println("🆔 ID: " + v.id + " | 📄 Doc: " + v.documento + " | 👤 " + v.nombre + " " + v.apellido);
            System.out.println("   📍 " + v.departamento + " > " + v.municipio + " > " + v.puesto + " > Mesa " + v.mesa + " > " + v.mesaId);
            if (i < limite - 1) System.out.println();
        }
        
        if (votantes.length > limite) {
            System.out.println("... y " + (votantes.length - limite) + " votantes más");
        }
        System.out.println("─────────────────────────────────────────────────────────────");
    }
    
    private void consultarCandidatosElectorales() {
        if (consultaCandidatosProxy == null) {
            System.out.println("❌ Servicio ConsultaCandidatos no disponible");
            return;
        }
        
        System.out.println("\n🗳️ ═══ CONSULTAR CANDIDATOS ELECTORALES ═══");
        System.out.println("Este servicio permite consultar los candidatos registrados en la base de datos electoral.");
        System.out.println();
        
        while (true) {
            System.out.println("Opciones disponibles:");
            System.out.println("1. Obtener todos los candidatos electorales");
            System.out.println("2. Buscar candidatos por partido");
            System.out.println("3. Contar total de candidatos");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione una opción: ");
            
            try {
                int option = Integer.parseInt(scanner.nextLine().trim());
                
                switch (option) {
                    case 1:
                        obtenerTodosCandidatos();
                        break;
                    case 2:
                        buscarCandidatosPorPartido();
                        break;
                    case 3:
                        contarCandidatos();
                        break;
                    case 0:
                        return;
                    default:
                        System.out.println("❌ Opción inválida");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Por favor, ingrese un número válido.");
            } catch (java.lang.Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
    }
    
    private void obtenerTodosCandidatos() {
        System.out.println("\n📋 OBTENIENDO TODOS LOS CANDIDATOS ELECTORALES");
        
        try {
            System.out.println("🔍 Consultando candidatos...");
            long startTime = System.currentTimeMillis();
            
            CandidatoElectoral[] candidatos = consultaCandidatosProxy.obtenerTodosCandidatosElectorales();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADOS:");
            System.out.println("══════════════════════════════════════");
            System.out.println("📊 Total de candidatos encontrados: " + candidatos.length);
            System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
            if (candidatos.length > 0) {
                mostrarCandidatos(candidatos, Math.min(10, candidatos.length));
            } else {
                System.out.println("ℹ️  No se encontraron candidatos en la base de datos.");
            }
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error consultando candidatos: " + e.getMessage());
        }
    }
    
    private void buscarCandidatosPorPartido() {
        System.out.println("\n🔍 BUSCAR CANDIDATOS POR PARTIDO");
        System.out.print("Ingrese el nombre del partido (o parte del nombre): ");
        
        String partido = scanner.nextLine().trim();
        if (partido.isEmpty()) {
            System.out.println("❌ El nombre del partido no puede estar vacío");
            return;
        }
        
        try {
            System.out.println("🔍 Buscando candidatos del partido: " + partido);
            long startTime = System.currentTimeMillis();
            
            CandidatoElectoral[] candidatos = consultaCandidatosProxy.obtenerCandidatosPorPartido(partido);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADOS:");
            System.out.println("══════════════════════════════════════");
            System.out.println("🔍 Búsqueda: " + partido);
            System.out.println("📊 Candidatos encontrados: " + candidatos.length);
            System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
            if (candidatos.length > 0) {
                mostrarCandidatos(candidatos, candidatos.length);
            } else {
                System.out.println("ℹ️  No se encontraron candidatos para el partido: " + partido);
            }
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error buscando candidatos: " + e.getMessage());
        }
    }
    
    private void contarCandidatos() {
        System.out.println("\n🔢 CONTAR CANDIDATOS ELECTORALES");
        
        try {
            System.out.println("🔍 Contando candidatos...");
            long startTime = System.currentTimeMillis();
            
            long total = consultaCandidatosProxy.contarCandidatos();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADO DEL CONTEO:");
            System.out.println("══════════════════════════════════════");
            System.out.println("📊 Total de candidatos activos: " + total);
            System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error contando candidatos: " + e.getMessage());
        }
    }
    
    private void mostrarCandidatos(CandidatoElectoral[] candidatos, int limite) {
        System.out.println("\n🗳️ CANDIDATOS ELECTORALES (mostrando " + limite + " de " + candidatos.length + "):");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        for (int i = 0; i < limite && i < candidatos.length; i++) {
            CandidatoElectoral c = candidatos[i];
            System.out.println("🆔 ID: " + c.id + " | 👤 " + c.nombre);
            System.out.println("   🏛️  Partido: " + c.partido);
            System.out.println("   📅 Fecha: " + c.fechaCreacion + " | ✅ Activo: " + (c.activo ? "Sí" : "No"));
            if (i < limite - 1) System.out.println();
        }
        
        if (candidatos.length > limite) {
            System.out.println("... y " + (candidatos.length - limite) + " candidatos más");
        }
        System.out.println("─────────────────────────────────────────────────────────────");
    }
    
    private void registroVotosMenu() {
        if (registroVotosProxy == null) {
            System.out.println("❌ Servicio RegistroVotos no disponible");
            return;
        }
        
        System.out.println("\n✅ ═══ REGISTRO DE VOTOS ═══");
        System.out.println("Este servicio permite registrar votos individuales y obtener estadísticas.");
        System.out.println();
        
        while (true) {
            System.out.println("Opciones disponibles:");
            System.out.println("1. Registrar voto individual");
            System.out.println("2. Estadísticas de votos (por mesa o hash)");
            System.out.println("3. Contar votos por candidato");
            System.out.println("4. Contar votos por municipio");
            System.out.println("5. Verificar conexión a BD");
            System.out.println("0. Volver al menú principal");
            System.out.print("Seleccione una opción: ");
            
            try {
                int option = Integer.parseInt(scanner.nextLine().trim());
                
                switch (option) {
                    case 1:
                        registrarVotoIndividual();
                        break;
                    case 2:
                        obtenerEstadisticasVotos();
                        break;
                    case 3:
                        contarVotosPorCandidato();
                        break;
                    case 4:
                        contarVotosPorMunicipio();
                        break;
                    case 5:
                        verificarConexionRegistroVotos();
                        break;
                    case 0:
                        return;
                    default:
                        System.out.println("❌ Opción inválida");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Por favor, ingrese un número válido.");
            } catch (java.lang.Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
    }
    
    private void registrarVotoIndividual() {
        System.out.println("\n📝 REGISTRAR VOTO INDIVIDUAL");
        System.out.println("Ingrese los datos del voto:");
        
        try {
            System.out.print("🆔 Mesa ID: ");
            String mesaId = scanner.nextLine().trim();
            
            System.out.print("👤 Candidato ID: ");
            long candidatoId = Long.parseLong(scanner.nextLine().trim());
            
            System.out.print("🔐 Hash de verificación: ");
            String hashVerificacion = scanner.nextLine().trim();
            
            System.out.print("🌍 Departamento: ");
            String departamento = scanner.nextLine().trim();
            
            System.out.print("🏙️  Municipio: ");
            String municipio = scanner.nextLine().trim();
            
            if (mesaId.isEmpty() || hashVerificacion.isEmpty() || departamento.isEmpty() || municipio.isEmpty()) {
                System.out.println("❌ Todos los campos son obligatorios");
                return;
            }
            
            // Crear el voto
            VotoCompleto voto = new VotoCompleto();
            voto.id = 0; // Se asignará automáticamente
            voto.mesaId = mesaId;
            voto.timestamp = System.currentTimeMillis();
            voto.candidatoId = candidatoId;
            voto.hashVerificacion = hashVerificacion;
            voto.departamento = departamento;
            voto.municipio = municipio;
            
            System.out.println("⏳ Registrando voto...");
            long startTime = System.currentTimeMillis();
            
            boolean resultado = registroVotosProxy.registrarVoto(voto);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADO DEL REGISTRO:");
            System.out.println("══════════════════════════════════════");
            System.out.println("🎯 Éxito: " + (resultado ? "Sí" : "No"));
            if (resultado) {
                System.out.println("💬 Mensaje: Voto registrado exitosamente");
            } else {
                System.out.println("💬 Mensaje: Error al registrar el voto");
            }
            System.out.println("⏱️  Tiempo de registro: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Por favor, ingrese un número válido para Candidato ID.");
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error registrando voto: " + e.getMessage());
        }
    }
    
    private void obtenerEstadisticasVotos() {
        System.out.println("\n📊 ESTADÍSTICAS DE VOTOS");
        System.out.println("Seleccione el tipo de estadística:");
        System.out.println("1. Contar votos por mesa");
        System.out.println("2. Verificar existencia de voto por hash");
        System.out.print("Opción: ");
        
        try {
            int opcion = Integer.parseInt(scanner.nextLine().trim());
            
            switch (opcion) {
                case 1:
                    System.out.print("🆔 Ingrese Mesa ID: ");
                    String mesaId = scanner.nextLine().trim();
                    
                    long startTime = System.currentTimeMillis();
                    long totalVotos = registroVotosProxy.contarVotosPorMesa(mesaId);
                    long endTime = System.currentTimeMillis();
                    
                    System.out.println("\n✅ ESTADÍSTICAS DE MESA:");
                    System.out.println("══════════════════════════════════════");
                    System.out.println("🆔 Mesa ID: " + mesaId);
                    System.out.println("📊 Total de votos: " + totalVotos);
                    System.out.println("⏱️  Tiempo de consulta: " + (endTime - startTime) + " ms");
                    System.out.println("══════════════════════════════════════");
                    break;
                    
                case 2:
                    System.out.print("🔐 Ingrese hash de verificación: ");
                    String hash = scanner.nextLine().trim();
                    
                    startTime = System.currentTimeMillis();
                    boolean existe = registroVotosProxy.existeVotoPorHash(hash);
                    endTime = System.currentTimeMillis();
                    
                    System.out.println("\n✅ VERIFICACIÓN DE HASH:");
                    System.out.println("══════════════════════════════════════");
                    System.out.println("🔐 Hash: " + hash);
                    System.out.println("📊 Existe: " + (existe ? "Sí" : "No"));
                    System.out.println("⏱️  Tiempo de consulta: " + (endTime - startTime) + " ms");
                    System.out.println("══════════════════════════════════════");
                    break;
                    
                default:
                    System.out.println("❌ Opción inválida");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Por favor, ingrese un número válido.");
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error obteniendo estadísticas: " + e.getMessage());
        }
    }
    
    private void contarVotosPorCandidato() {
        System.out.println("\n🗳️ CONTAR VOTOS POR CANDIDATO");
        System.out.print("👤 Ingrese el ID del candidato: ");
        
        try {
            long candidatoId = Long.parseLong(scanner.nextLine().trim());
            
            System.out.println("🔍 Contando votos para candidato ID: " + candidatoId);
            long startTime = System.currentTimeMillis();
            
            long totalVotos = registroVotosProxy.contarVotosPorCandidato(candidatoId);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADO DEL CONTEO:");
            System.out.println("══════════════════════════════════════");
            System.out.println("👤 Candidato ID: " + candidatoId);
            System.out.println("📊 Total de votos: " + totalVotos);
            System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Por favor, ingrese un número válido.");
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error contando votos: " + e.getMessage());
        }
    }
    
    private void contarVotosPorMunicipio() {
        System.out.println("\n🏙️ CONTAR VOTOS POR MUNICIPIO");
        System.out.print("🏙️  Ingrese el nombre del municipio: ");
        
        String municipio = scanner.nextLine().trim();
        if (municipio.isEmpty()) {
            System.out.println("❌ El nombre del municipio no puede estar vacío");
            return;
        }
        
        try {
            System.out.println("🔍 Contando votos para municipio: " + municipio);
            long startTime = System.currentTimeMillis();
            
            long totalVotos = registroVotosProxy.contarVotosPorMunicipio(municipio);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADO DEL CONTEO:");
            System.out.println("══════════════════════════════════════");
            System.out.println("🏙️  Municipio: " + municipio);
            System.out.println("📊 Total de votos: " + totalVotos);
            System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error contando votos: " + e.getMessage());
        }
    }
    
    private void verificarConexionRegistroVotos() {
        System.out.println("\n🔍 VERIFICAR CONEXIÓN A BASE DE DATOS");
        System.out.println("Este servicio permite verificar la conexión a la base de datos.");
        System.out.println();
        
        try {
            System.out.println("⏳ Verificando conexión...");
            long startTime = System.currentTimeMillis();
            
            boolean conexionExitosa = registroVotosProxy.verificarConexionBD();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("\n✅ RESULTADO DE LA VERIFICACIÓN:");
            System.out.println("══════════════════════════════════════");
            System.out.println("📊 Conexión exitosa: " + (conexionExitosa ? "Sí" : "No"));
            System.out.println("⏱️  Tiempo de consulta: " + duration + " ms");
            System.out.println("══════════════════════════════════════");
            
        } catch (java.lang.Exception e) {
            System.out.println("❌ Error verificando conexión: " + e.getMessage());
        }
    }
} 