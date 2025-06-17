package GestorMesa;

import Demo.*;
import Demo.Candidato;
import Demo.IConfirmacionVotoPrx;
import Demo.IRegistrarVotoPrx;

import GestorVotos.GestorVotos;
import GestorVotos.VotoImp;

import ReliableMessageManager.ReliableMessageManager;

import mesaVotacion.SistemaVerificacion;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Exception;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Properties;
import com.zeroc.Ice.*;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class GestorMesa {
    private final ReliableMessageManager messageManager;
    private IRegistrarVotoPrx servidorRegional;
    private ObjectAdapter adapter;
    private Communicator communicator;
    private com.zeroc.IceGrid.QueryPrx query;
    private String idMesa;
    private List<Candidato> candidatosDisponibles;
    private List<String> electoresYaVotaron;
    private boolean candidatosCargados = false;
    
    // Sistema de Votantes SQLite para sincronizar votantes desde el servidor regional
    private GestorVotantesSQLite gestorVotantesSQLite;
    private IConsultaMesaSQLitePrx consultaMesaSQLiteProxy;

    public GestorMesa(String idMesa) {
        this.idMesa = idMesa;
        this.candidatosDisponibles = new ArrayList<>();
        this.electoresYaVotaron = new ArrayList<>();
        this.messageManager = new ReliableMessageManager();

        // Crear directorio data si no existe
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            boolean creado = dataDir.mkdirs();
            if (creado) {
                System.out.println("📁 Directorio 'data' creado");
            } else {
                System.err.println("❌ No se pudo crear directorio 'data'");
            }
        }

        // Inicializar gestor de votantes SQLite para sincronizar con servidor regional
        try {
            gestorVotantesSQLite = new GestorVotantesSQLite(idMesa, "tcp -h localhost -p 8080");
            System.out.println("✅ Gestor de Votantes SQLite inicializado para Mesa " + idMesa);
        } catch (Exception e) {
            System.err.println("❌ CRÍTICO: No se pudo inicializar Gestor de Votantes SQLite: " + e.getMessage());
            e.printStackTrace();
            this.gestorVotantesSQLite = null;
        }

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        } catch (Exception e) {
            System.err.println("❌ Error registrando shutdown hook: " + e.getMessage());
        }
    }

    public boolean inicializar(Communicator communicator) {
        this.communicator = communicator;

        try {
            // Crear un communicator local para el adaptador (sin IceGrid)
            System.out.println("🔌 Creando communicator local para callbacks...");
            com.zeroc.Ice.InitializationData initData = new com.zeroc.Ice.InitializationData();
            initData.properties = com.zeroc.Ice.Util.createProperties();
            // NO configurar locator para este communicator
            Communicator localCommunicator = com.zeroc.Ice.Util.initialize(initData);
            
            // Crear adaptador simple con el communicator local
            this.adapter = localCommunicator.createObjectAdapterWithEndpoints(
                "MesaCallbackAdapter", "tcp -h localhost -p 0");
            
            // Activar el adaptador (sin IceGrid)
            adapter.activate();
            System.out.println("✅ Adaptador de callbacks activado localmente");

            // Obtener el proxy de IceGrid Query usando el Locator configurado
            try {
                this.query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                        communicator.stringToProxy("DemoIceGrid/Query"));
                System.out.println("✅ Conexión a IceGrid Query establecida");
            } catch (Exception e) {
                System.out.println("⚠️ No se pudo conectar a IceGrid Query: " + e.getMessage());
                this.query = null;
            }

            // Conectar al servidor regional para votos
            this.servidorRegional = obtenerServidorRegional(communicator);

            // PRIMERO: Inicializar conexión al Servidor Regional para consultas SQLite
            boolean conexionRegionalOK = inicializarConexionServidorRegional(communicator);
            
            // SEGUNDO: Sincronizar votantes ANTES de continuar (OBLIGATORIO)
            System.out.println("\n🔄 === INICIALIZACIÓN OBLIGATORIA DE VOTANTES ===");
            boolean votantesSincronizados = sincronizarVotantesAlInicializar();
            if (!votantesSincronizados) {
                System.err.println("❌ FALLO CRÍTICO: No se pudieron obtener votantes");
                System.err.println("💡 La mesa NO puede funcionar sin votantes válidos");
                System.err.println("💡 Verifique que el Servidor Regional esté ejecutándose y que la mesa esté registrada");
                return false;
            }

            // TERCERO: Procesar mensajes pendientes si hay conexión al servidor regional
            if (servidorRegional != null) {
                System.out.println("✅ Gestor de Mesa inicializado correctamente");
                System.out.println("  Mesa ID: " + idMesa);
                System.out.println("  Conectado al servidor regional para votos");

                if (messageManager.hayMensajesPendientes()) {
                    System.out.println("📤 Procesando mensajes pendientes...");
                    messageManager.procesarMensajesPendientes(servidorRegional, adapter, communicator);
                }
            } else {
                System.out.println("⚠️ No hay servidor regional disponible para votos");
                System.out.println("   📤 Los votos se guardarán para envío posterior");
            }

            // CUARTO: Cargar candidatos por defecto
            cargarCandidatosPorDefecto();

            return true;

        } catch (com.zeroc.Ice.Exception e) {
            System.err.println("❌ Error inicializando gestor de mesa: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void cargarCandidatos() {
        if (!candidatosCargados) {
            cargarCandidatosPorDefecto();
        }
    }

    /**
     * Carga candidatos por defecto para la mesa de votación
     */
    private void cargarCandidatosPorDefecto() {
        try {
            System.out.println("📝 Cargando candidatos por defecto...");
            
            candidatosDisponibles.clear();
            candidatosDisponibles.add(new Candidato(1, "Juan Pérez", "Partido A"));
            candidatosDisponibles.add(new Candidato(2, "María García", "Partido B"));
            candidatosDisponibles.add(new Candidato(3, "Carlos López", "Partido C"));
            candidatosDisponibles.add(new Candidato(4, "Ana Martínez", "Independiente"));
            
            System.out.println("✅ Candidatos por defecto cargados: " + candidatosDisponibles.size() + " candidatos");
            
        } catch (Exception e) {
            System.err.println("❌ Error cargando candidatos por defecto: " + e.getMessage());
            // Asegurar que al menos haya candidatos básicos
            candidatosDisponibles.clear();
            candidatosDisponibles.add(new Candidato(1, "Candidato Por Defecto", "Sin Partido"));
        }
    }

    /**
     * Valida el elector usando el sistema regional
     * Prioriza base de datos local, conecta a servidor regional si es necesario
     */
    public boolean validarElector(String cedula) {
        if (gestorVotantesSQLite == null) {
            System.err.println("❌ CRÍTICO: Gestor de Votantes SQLite no disponible");
            return false;
        }
        
        try {
            // Obtener votantes (verifica local primero, luego servidor regional si es necesario)
            List<VotanteMesa> votantes = gestorVotantesSQLite.obtenerVotantesLocales();
            
            if (votantes == null || votantes.isEmpty()) {
                System.err.println("❌ CRÍTICO: No hay votantes disponibles");
                return false;
            }
            
            // Buscar el votante en la lista
            for (VotanteMesa votante : votantes) {
                if (votante.documento.equals(cedula)) {
                    System.out.println("✅ Elector válido: " + votante.nombre + " " + votante.apellido);
                    return true;
                }
            }
            
            System.out.println("❌ Elector no encontrado: " + cedula);
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error validando elector " + cedula + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean validarCandidato(long idCandidato) {
        return candidatosDisponibles.stream()
                .anyMatch(c -> c.idCandidato == idCandidato);
    }

    public boolean registrarVoto(String documentoIdentidad, long idCandidato) {
        try {
            if (!validarElector(documentoIdentidad)) {
                System.err.println(" Elector no válido o ya votó");
                return false;
            }

            if (!validarCandidato(idCandidato)) {
                System.err.println(" Candidato no válido: " + idCandidato);
                return false;
            }

            String hashElector = generarHashElector(documentoIdentidad);
            long idVoto = System.currentTimeMillis() + (int)(Math.random() * 1000);
            long timestamp = java.time.Instant.now().getEpochSecond();

            VotoImp voto = new VotoImp(idVoto, idMesa, hashElector, idCandidato, timestamp);

            if (!voto.esValido()) {
                System.err.println(" El voto generado no es válido");
                return false;
            }

            // Marcar elector como que ya votó
            electoresYaVotaron.add(hashElector);

            // Obtener nombre del candidato para mostrar
            String nombreCandidato = candidatosDisponibles.stream()
                    .filter(c -> c.idCandidato == idCandidato)
                    .map(c -> c.nombre)
                    .findFirst()
                    .orElse("Candidato " + idCandidato);

            System.out.println("🗳️  Registrando voto:");
            System.out.println("     Voto ID: " + idVoto);
            System.out.println("     Candidato: " + nombreCandidato);
            System.out.println("     Mesa: " + idMesa);

            boolean enviado = enviarVoto(voto);

            if (!enviado) {
                // Si no se pudo enviar, quitar al elector de la lista para que pueda intentar de nuevo
                electoresYaVotaron.remove(hashElector);
                System.err.println(" No se pudo procesar el voto");
            } else {
                System.out.println(" Voto procesado exitosamente");
                
                // Registrar que el votante ya ejerció su voto en SQLite local
                if (gestorVotantesSQLite != null) {
                    try {
                        boolean registradoVotante = gestorVotantesSQLite.registrarVoto(documentoIdentidad);
                        if (registradoVotante) {
                            System.out.println("📋 Votante marcado como que ya votó en base de datos local");
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Error registrando voto en base de datos de votantes: " + e.getMessage());
                    }
                }
            }

            return enviado;

        } catch (Exception e) {
            System.err.println(" Error registrando voto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean enviarVoto(VotoImp voto) {
        try {
            if (servidorRegional == null) {
                System.err.println("  No hay servidor disponible. Guardando voto para envío posterior...");
                messageManager.guardarVotoPendiente(voto);
                return true;
            }

            IConfirmacionVotoPrx callback = crearCallback();

            System.out.println(" Enviando voto al servidor regional...");
            servidorRegional.enviarVoto(voto, callback);

            System.out.println(" Voto enviado. Esperando confirmación...");
            return true;

        } catch (com.zeroc.Ice.NoEndpointException | com.zeroc.Ice.ConnectFailedException e) {
            System.err.println(" Servidor no disponible: " + e.getMessage());
            messageManager.guardarVotoPendiente(voto);
            reconectarServidor();
            return true;

        } catch (com.zeroc.Ice.LocalException ex) {
            System.err.println(" Error de comunicación ICE: " + ex.getMessage());
            messageManager.guardarVotoPendiente(voto);
            reconectarServidor();
            return true;

        } catch (Exception e) {
            System.err.println(" Error enviando voto: " + e.getMessage());
            messageManager.guardarVotoPendiente(voto);
            return false;
        }
    }

    public boolean reconectarServidor() {
        System.out.println(" Intentando reconectar al servidor...");

        IRegistrarVotoPrx nuevoServidorVotos = obtenerServidorRegional(communicator);

        if (nuevoServidorVotos != null) {
            this.servidorRegional = nuevoServidorVotos;

            System.out.println(" Reconectado al servidor regional");

            if (messageManager.hayMensajesPendientes()) {
                System.out.println(" Procesando mensajes pendientes...");
                messageManager.procesarMensajesPendientes(servidorRegional, adapter, communicator);
            }

            return true;
        } else {
            System.err.println(" No se pudo establecer conexión con ningún servidor");
            return false;
        }
    }

    private static IRegistrarVotoPrx obtenerServidorRegional(com.zeroc.Ice.Communicator communicator) {
        IRegistrarVotoPrx registrarVoto = null;
        
        try {
            System.out.println("🔗 Conectando al servidor regional (receptorVotos)...");
            
            // Conectar usando IceGrid Query para encontrar el objeto por identidad
            com.zeroc.IceGrid.QueryPrx query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                    communicator.stringToProxy("DemoIceGrid/Query"));
            
            if (query != null) {
                try {
                    System.out.println("✅ Conectado al IceGrid Query, buscando servidor por tipo...");
                    // Buscar el objeto por tipo
                    com.zeroc.Ice.ObjectPrx obj = query.findObjectByType("::Demo::IRegistrarVoto");
                    if (obj != null) {
                        registrarVoto = IRegistrarVotoPrx.checkedCast(obj);
                        if (registrarVoto != null) {
                            System.out.println("✅ Conectado al servidor regional via IceGrid Query (por tipo)");
                            return registrarVoto;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error buscando por tipo: " + e.getMessage());
                }
                
                // Si no funciona por tipo, intentar por identidad directa
                try {
                    System.out.println("🔄 Intentando conexión por identidad...");
                    com.zeroc.Ice.ObjectPrx objById = query.findObjectById(
                            com.zeroc.Ice.Util.stringToIdentity("receptorVotos"));
                    if (objById != null) {
                        registrarVoto = IRegistrarVotoPrx.checkedCast(objById);
                        if (registrarVoto != null) {
                            System.out.println("✅ Conectado al servidor regional via identidad");
                            return registrarVoto;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error buscando por identidad: " + e.getMessage());
                }
            } else {
                System.err.println("❌ No se pudo conectar al IceGrid Query");
            }
            
            // Método alternativo: conectar directamente al adaptador
            try {
                System.out.println("🔄 Intentando conexión directa al adaptador...");
                String proxyString = "receptorVotos:tcp -h localhost -p 10000";
                
                System.out.println("   Usando proxy: " + proxyString);
                com.zeroc.Ice.ObjectPrx directObj = communicator.stringToProxy(proxyString);
                if (directObj != null) {
                    registrarVoto = IRegistrarVotoPrx.checkedCast(directObj);
                    if (registrarVoto != null) {
                        System.out.println("✅ Conectado directamente al adaptador regional");
                        return registrarVoto;
                    } else {
                        System.err.println("❌ Error: El proxy no es del tipo esperado");
                    }
                } else {
                    System.err.println("❌ Error: No se pudo crear el proxy");
                }
            } catch (Exception e) {
                System.err.println("⚠️  Error en conexión directa: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error general conectando al servidor regional: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.err.println("❌ No se pudo establecer conexión con el servidor regional");
        return null;
    }

    private IConfirmacionVotoPrx crearCallback() {
        try {
            if (adapter != null) {
                GestorVotos confirmacionImpl = new GestorVotos();
                com.zeroc.Ice.ObjectPrx obj = adapter.addWithUUID(confirmacionImpl);
                return IConfirmacionVotoPrx.uncheckedCast(obj);
            }
        } catch (Exception e) {
            System.err.println(" Error creando callback: " + e.getMessage());
        }
        return null;
    }

    private String generarHashElector(String documentoIdentidad) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(documentoIdentidad.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println("Error crítico: No se encuentra el algoritmo SHA-256");
            // En caso de error, retornar un hash simple (no seguro, solo para evitar fallos)
            return documentoIdentidad.hashCode() + "";
        }
    }

    // Getters y métodos de utilidad
    public List<Candidato> getCandidatosDisponibles() {
        return new ArrayList<>(candidatosDisponibles);
    }

    public String getIdMesa() {
        return idMesa;
    }

    public boolean hayMensajesPendientes() {
        return messageManager.hayMensajesPendientes();
    }

    public boolean candidatosCargados() {
        return candidatosCargados;
    }

    public void mostrarEstadisticas() {
        System.out.println("\n📊 === ESTADÍSTICAS MESA " + idMesa + " ===");
        System.out.println("🗳️  Candidatos disponibles: " + candidatosDisponibles.size());
        System.out.println("👥 Electores que han votado: " + electoresYaVotaron.size());
        System.out.println("📋 Candidatos cargados: " + (candidatosCargados ? "Sí" : "No"));
        System.out.println("📁 Mensajes pendientes: " + (messageManager.hayMensajesPendientes() ? "Sí" : "No"));
        
        // Mostrar estadísticas del GestorVotantesSQLite
        if (gestorVotantesSQLite != null) {
            System.out.println("🔄 Sincronización con Servidor Regional: Disponible");
            System.out.println("📊 Base de datos local de votantes: " + gestorVotantesSQLite.getDbPath());
            System.out.println("🔌 Conectado al Servidor Regional: " + (gestorVotantesSQLite.isConectado() ? "Sí" : "No"));
        } else {
            System.out.println("🔄 Sincronización con Servidor Regional: No disponible");
        }
        
        System.out.println("═".repeat(50));
    }

    public void shutdown() {
        System.out.println("Cerrando gestor de mesa...");
        if (messageManager != null) {
            messageManager.shutdown();
        }
        if (adapter != null) {
            adapter.destroy();
        }
    }

    /**
     * Inicializa la conexión directa al Servidor Regional para consultas SQLite
     */
    private boolean inicializarConexionServidorRegional(Communicator communicator) {
        try {
            System.out.println("🔌 Inicializando conexión al Servidor Regional para consultas SQLite...");
            
            // Conectar directamente al endpoint del Servidor Regional
            ObjectPrx base = communicator.stringToProxy("consultaMesaSQLite:tcp -h localhost -p 8080");
            consultaMesaSQLiteProxy = IConsultaMesaSQLitePrx.checkedCast(base);
            
            if (consultaMesaSQLiteProxy != null) {
                consultaMesaSQLiteProxy.ice_ping();
                System.out.println("✅ Conexión directa al Servidor Regional establecida");
                return true;
            } else {
                System.err.println("⚠️ No se pudo establecer conexión directa al Servidor Regional");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error estableciendo conexión al Servidor Regional: " + e.getMessage());
            consultaMesaSQLiteProxy = null;
            return false;
        }
    }
    
    /**
     * OBLIGATORIO: Sincroniza votantes desde el servidor regional al inicializar la mesa
     * Primero verifica si existe base de datos local, sino busca en servidor regional
     */
    private boolean sincronizarVotantesAlInicializar() {
        if (gestorVotantesSQLite == null) {
            System.err.println("❌ CRÍTICO: Gestor de Votantes SQLite no disponible");
            System.err.println("💡 La mesa NO puede operar sin acceso a votantes");
            return false;
        }
        
        try {
            System.out.println("\n🔄 === INICIALIZACIÓN DE VOTANTES ===");
            System.out.println("📋 Mesa: " + idMesa);
            
            // Obtener votantes (verifica local primero, luego servidor regional si es necesario)
            List<VotanteMesa> votantes = gestorVotantesSQLite.obtenerVotantesLocales();
            
            if (votantes == null || votantes.isEmpty()) {
                System.err.println("❌ CRÍTICO: No se obtuvieron votantes para Mesa " + idMesa);
                System.err.println("💡 Verifique que la mesa esté registrada en el Servidor Regional");
                return false;
            }
            
            System.out.println("✅ ÉXITO: " + votantes.size() + " votantes disponibles para Mesa " + idMesa);
            System.out.println("═".repeat(50));
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ CRÍTICO: Error en inicialización de votantes: " + e.getMessage());
            System.err.println("💡 La mesa NO puede operar sin votantes");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Getters y métodos para GestorVotantesSQLite
     */
    public GestorVotantesSQLite getGestorVotantesSQLite() {
        return gestorVotantesSQLite;
    }
    
    public boolean isGestorVotantesDisponible() {
        return gestorVotantesSQLite != null;
    }
    
    public boolean sincronizarVotantesDesdeServidorRegional() {
        if (gestorVotantesSQLite == null) {
            System.err.println("❌ Gestor de Votantes SQLite no disponible");
            return false;
        }
        
        try {
            System.out.println("🔄 Iniciando sincronización manual de votantes...");
            
            // Conectar al Servidor Regional si no está conectado
            boolean conectado = gestorVotantesSQLite.isConectado() || 
                               gestorVotantesSQLite.inicializarConexionICE();
            
            if (!conectado) {
                System.err.println("❌ No se pudo conectar al Servidor Regional");
                return false;
            }
            
            // Sincronizar votantes
            boolean sincronizado = gestorVotantesSQLite.sincronizarVotantes();
            if (sincronizado) {
                System.out.println("✅ Votantes sincronizados manualmente");
                return true;
            } else {
                System.err.println("❌ Error en sincronización manual de votantes");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en sincronización manual de votantes: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void mostrarResumenVotantes() {
        if (gestorVotantesSQLite != null) {
            gestorVotantesSQLite.mostrarResumenVotantes();
        } else {
            System.out.println("⚠️ Gestor de Votantes SQLite no disponible");
        }
    }
    
    public EstadisticasMesaSQLite obtenerEstadisticasLocales() {
        if (gestorVotantesSQLite != null) {
            return gestorVotantesSQLite.obtenerEstadisticasLocales();
        }
        return null;
    }
    
    public IConsultaMesaSQLitePrx getConsultaMesaSQLiteProxy() {
        return consultaMesaSQLiteProxy;
    }
    
    /**
     * Método para consultar estadísticas del servidor regional directamente
     */
    public EstadisticasMesaSQLite obtenerEstadisticasDesdeServidorRegional() {
        if (consultaMesaSQLiteProxy == null) {
            System.err.println("❌ No hay conexión al Servidor Regional");
            return null;
        }
        
        try {
            System.out.println("🔄 Consultando estadísticas desde Servidor Regional para Mesa " + idMesa + "...");
            long startTime = System.currentTimeMillis();
            
            EstadisticasMesaSQLite stats = consultaMesaSQLiteProxy.obtenerEstadisticasMesa(idMesa);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ Estadísticas obtenidas en " + (endTime - startTime) + "ms");
            return stats;
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo estadísticas desde Servidor Regional: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Método para consultar votantes del servidor regional directamente  
     */
    public VotanteMesa[] obtenerVotantesDesdeServidorRegional() {
        if (consultaMesaSQLiteProxy == null) {
            System.err.println("❌ No hay conexión al Servidor Regional");
            return new VotanteMesa[0];
        }
        
        try {
            System.out.println("🔄 Consultando votantes desde Servidor Regional para Mesa " + idMesa + "...");
            long startTime = System.currentTimeMillis();
            
            VotanteMesa[] votantes = consultaMesaSQLiteProxy.obtenerVotantesDeMesa(idMesa);
            long endTime = System.currentTimeMillis();
            
            System.out.println("✅ " + votantes.length + " votantes obtenidos en " + (endTime - startTime) + "ms");
            return votantes;
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo votantes desde Servidor Regional: " + e.getMessage());
            return new VotanteMesa[0];
        }
    }
}