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
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;


public class GestorMesa implements IMesaVotacion {
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

    // ⭐ NUEVO: Gestor de Candidatos desde Servidor Regional
    private GestorCandidatosMesa gestorCandidatos;

    // ⭐ NUEVO: Gestor de Votos SQLite Local
    private GestorVotosSQLite gestorVotosSQLite;

    // ⭐ NUEVO: Sincronizador Automático de Votos
    private SincronizadorVotosAutomatico sincronizadorVotos;

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

        // ⭐ NUEVO: Inicializar gestor de candidatos para servidor regional
        try {
            gestorCandidatos = new GestorCandidatosMesa(idMesa, "tcp -h localhost -p 8080");
            System.out.println("✅ Gestor de Candidatos inicializado para Mesa " + idMesa);
        } catch (Exception e) {
            System.err.println("❌ Error inicializando Gestor de Candidatos: " + e.getMessage());
            e.printStackTrace();
            this.gestorCandidatos = null;
        }

        // ⭐ NUEVO: Inicializar gestor de votos SQLite
        try {
            gestorVotosSQLite = new GestorVotosSQLite(idMesa);
            System.out.println("✅ Gestor de Votos SQLite inicializado para Mesa " + idMesa);
        } catch (Exception e) {
            System.err.println("❌ Error inicializando Gestor de Votos SQLite: " + e.getMessage());
            e.printStackTrace();
            this.gestorVotosSQLite = null;
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
            cargarCandidatos();

            // ⭐ QUINTO: Inicializar sincronizador automático de votos
            if (gestorVotosSQLite != null) {
                sincronizadorVotos = new SincronizadorVotosAutomatico(idMesa, gestorVotosSQLite, communicator);
                boolean sincronizadorIniciado = sincronizadorVotos.iniciar();
                
                if (sincronizadorIniciado) {
                    System.out.println("✅ Sincronización automática de votos activada (cada 10 segundos)");
                } else {
                    System.out.println("⚠️ Sincronización automática deshabilitada - funcionará sin servidor regional");
                }
            } else {
                System.out.println("⚠️ No se puede activar sincronización automática - Gestor de Votos SQLite no disponible");
            }

            return true;

        } catch (com.zeroc.Ice.Exception e) {
            System.err.println("❌ Error inicializando gestor de mesa: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void cargarCandidatos() {
        if (!candidatosCargados) {
            // PRIMERO: Intentar cargar candidatos desde el servidor regional
            if (cargarCandidatosDesdeServidorRegional()) {
                System.out.println("✅ Candidatos cargados desde servidor regional");
                candidatosCargados = true;
            } else {
                // RESPALDO: Usar candidatos por defecto si no se puede conectar
                System.out.println("⚠️ No se pudieron obtener candidatos del servidor regional");
                cargarCandidatosPorDefecto();
            }
        }
    }

    /**
     * Intenta cargar candidatos desde el servidor regional
     */
    private boolean cargarCandidatosDesdeServidorRegional() {
        if (gestorCandidatos == null) {
            System.out.println("❌ Gestor de candidatos no disponible");
            return false;
        }

        try {
            System.out.println("🔄 Consultando candidatos desde servidor regional...");

            // Primero verificar si hay candidatos locales
            List<Candidato> candidatosLocales = gestorCandidatos.obtenerCandidatosLocales();
            
            if (candidatosLocales != null && !candidatosLocales.isEmpty()) {
                System.out.println("📊 Usando candidatos desde base de datos local: " + candidatosLocales.size() + " candidatos");
                candidatosDisponibles.clear();
                candidatosDisponibles.addAll(candidatosLocales);
                return true;
            }

            // Si no hay candidatos locales, sincronizar desde servidor regional
            System.out.println("🔄 Sincronizando candidatos desde servidor regional...");
            boolean sincronizado = gestorCandidatos.sincronizarCandidatos();
            
            if (sincronizado) {
                // Obtener candidatos después de sincronizar
                candidatosLocales = gestorCandidatos.obtenerCandidatosLocales();
                if (candidatosLocales != null && !candidatosLocales.isEmpty()) {
                    System.out.println("✅ " + candidatosLocales.size() + " candidatos sincronizados correctamente");
                    candidatosDisponibles.clear();
                    candidatosDisponibles.addAll(candidatosLocales);
                    return true;
                }
            }

            System.out.println("❌ No se pudieron sincronizar candidatos");
            return false;

        } catch (Exception e) {
            System.err.println("❌ Error cargando candidatos desde servidor regional: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Carga candidatos por defecto para la mesa de votación
     */
    private void cargarCandidatosPorDefecto() {
        try {
            System.out.println("📝 Cargando candidatos por defecto como respaldo...");
            
            candidatosDisponibles.clear();
            candidatosDisponibles.add(new Candidato(1, "Juan Pérez", "Partido A"));
            candidatosDisponibles.add(new Candidato(2, "María García", "Partido B"));
            candidatosDisponibles.add(new Candidato(3, "Carlos López", "Partido C"));
            candidatosDisponibles.add(new Candidato(4, "Ana Martínez", "Independiente"));
            
            System.out.println("✅ Candidatos por defecto cargados: " + candidatosDisponibles.size() + " candidatos");
            candidatosCargados = true;
            
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
     * 
     * @param cedula Cédula del votante
     * @return 0 = válido, 1 = ya votó, 2 = no pertenece a la mesa, -1 = error crítico
     */
    public int validarElectorConCodigo(String cedula) {
        if (gestorVotantesSQLite == null) {
            System.err.println("❌ CRÍTICO: Gestor de Votantes SQLite no disponible");
            return -1;
        }
        
        try {
            // PASO 1: Verificar si ya votó
            if (gestorVotosSQLite != null && gestorVotosSQLite.yaVoto(cedula)) {
                System.out.println("❌ El votante con cédula " + cedula + " YA HA VOTADO");
                System.out.println("💡 No puede votar nuevamente");
                return 1; // Ya votó
            }
            
            // PASO 2: Verificar si el votante pertenece a esta mesa
            List<VotanteMesa> votantes = gestorVotantesSQLite.obtenerVotantesLocales();
            
            if (votantes == null || votantes.isEmpty()) {
                System.err.println("❌ CRÍTICO: No hay votantes disponibles");
                return -1; // Error crítico
            }
            
            // Buscar el votante en la lista
            for (VotanteMesa votante : votantes) {
                if (votante.documento.equals(cedula)) {
                    System.out.println("✅ Elector válido: " + votante.nombre + " " + votante.apellido);
                    System.out.println("📍 Mesa: " + votante.mesa + " | Municipio: " + votante.municipio + " | Departamento: " + votante.departamento);
                    return 0; // Válido
                }
            }
            
            System.out.println("❌ El votante con cédula " + cedula + " NO PERTENECE A ESTA MESA");
            System.out.println("💡 Debe dirigirse a su mesa de votación asignada");
            return 2; // No pertenece a la mesa
            
        } catch (Exception e) {
            System.err.println("❌ Error validando elector " + cedula + ": " + e.getMessage());
            e.printStackTrace();
            return -1; // Error crítico
        }
    }

    /**
     * Método original para mantener compatibilidad
     */
    public boolean validarElector(String cedula) {
        return validarElectorConCodigo(cedula) == 0;
    }

    public boolean validarCandidato(long idCandidato) {
        return candidatosDisponibles.stream()
                .anyMatch(c -> c.idCandidato == idCandidato);
    }

    public boolean registrarVoto(String documentoIdentidad, long idCandidato) {
        try {
            System.out.println("\n🗳️ === PROCESANDO VOTO ===");
            System.out.println("👤 Cédula: " + documentoIdentidad);
            System.out.println("🗳️ Candidato ID: " + idCandidato);
            System.out.println("📍 Mesa: " + idMesa);
            
            // PASO 1: Validar elector (incluye verificación de si ya votó y si pertenece a la mesa)
            if (!validarElector(documentoIdentidad)) {
                System.err.println("❌ VOTO RECHAZADO: Elector no válido");
                return false;
            }

            // PASO 2: Validar candidato
            if (!validarCandidato(idCandidato)) {
                System.err.println("❌ VOTO RECHAZADO: Candidato no válido: " + idCandidato);
                return false;
            }

            // PASO 3: Obtener información del votante para el registro
            VotanteMesa votanteInfo = obtenerInformacionVotante(documentoIdentidad);
            if (votanteInfo == null) {
                System.err.println("❌ VOTO RECHAZADO: No se pudo obtener información del votante");
                return false;
            }

            // PASO 4: Generar hash de verificación
            String hashElector = generarHashElector(documentoIdentidad);
            long timestamp = System.currentTimeMillis();

            // PASO 5: Crear registro de voto para SQLite local
            VotoRegistro votoLocal = new VotoRegistro(
                0, // ID se asignará automáticamente
                idMesa,
                timestamp,
                idCandidato,
                hashElector,
                votanteInfo.municipio,
                votanteInfo.departamento
            );

            // PASO 6: Registrar voto en SQLite local
            if (gestorVotosSQLite != null) {
                boolean votoRegistrado = gestorVotosSQLite.registrarVoto(votoLocal);
                if (!votoRegistrado) {
                    System.err.println("❌ VOTO RECHAZADO: Error registrando en base de datos local");
                    return false;
                }
                
                // Marcar votante como que ya votó
                boolean marcado = gestorVotosSQLite.marcarVotanteComoVotado(
                    documentoIdentidad, hashElector, votanteInfo.municipio, votanteInfo.departamento);
                
                if (!marcado) {
                    System.err.println("⚠️ Advertencia: No se pudo marcar votante como votado");
                }
            }

            // PASO 7: Crear voto para envío al servidor regional (usando la clase existente)
            long idVoto = System.currentTimeMillis() + (int)(Math.random() * 1000);
            VotoImp voto = new VotoImp(idVoto, idMesa, hashElector, idCandidato, timestamp / 1000);

            if (!voto.esValido()) {
                System.err.println("❌ El voto generado no es válido");
                return false;
            }

            // Marcar elector como que ya votó (lista en memoria - para compatibilidad)
            electoresYaVotaron.add(hashElector);

            // Obtener nombre del candidato para mostrar
            String nombreCandidato = candidatosDisponibles.stream()
                    .filter(c -> c.idCandidato == idCandidato)
                    .map(c -> c.nombre)
                    .findFirst()
                    .orElse("Candidato " + idCandidato);

            System.out.println("✅ VOTO REGISTRADO LOCALMENTE:");
            System.out.println("     Voto Local ID: " + votoLocal.id);
            System.out.println("     Voto Regional ID: " + idVoto);
            System.out.println("     Candidato: " + nombreCandidato);
            System.out.println("     Mesa: " + idMesa);
            System.out.println("     Municipio: " + votanteInfo.municipio);
            System.out.println("     Departamento: " + votanteInfo.departamento);

            // PASO 8: Intentar enviar al servidor regional
            boolean enviado = enviarVoto(voto);

            if (!enviado) {
                System.out.println("⚠️ No se pudo enviar al servidor regional, pero el voto está registrado localmente");
            } else {
                System.out.println("✅ Voto enviado al servidor regional");
                
                // Registrar que el votante ya ejerció su voto en SQLite de votantes
                if (gestorVotantesSQLite != null) {
                    try {
                        boolean registradoVotante = gestorVotantesSQLite.registrarVoto(documentoIdentidad);
                        if (registradoVotante) {
                            System.out.println("📋 Votante marcado en base de datos de votantes");
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Error registrando voto en base de datos de votantes: " + e.getMessage());
                    }
                }
            }

            System.out.println("🎉 SU VOTO HA SIDO REGISTRADO EXITOSAMENTE");
            System.out.println("📊 Puede consultar las estadísticas de votación");
            System.out.println("═".repeat(50));
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error registrando voto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene información completa del votante
     */
    private VotanteMesa obtenerInformacionVotante(String documentoIdentidad) {
        if (gestorVotantesSQLite == null) {
            return null;
        }
        
        try {
            List<VotanteMesa> votantes = gestorVotantesSQLite.obtenerVotantesLocales();
            for (VotanteMesa votante : votantes) {
                if (votante.documento.equals(documentoIdentidad)) {
                    return votante;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo información del votante: " + e.getMessage());
        }
        
        return null;
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
                String proxyString = "receptorVotos:tcp -h localhost -p 9090";
                
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
        System.out.println("👥 Electores que han votado (memoria): " + electoresYaVotaron.size());
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
        
        // ⭐ NUEVO: Mostrar estadísticas de votos SQLite
        if (gestorVotosSQLite != null) {
            System.out.println("\n🗳️ === ESTADÍSTICAS DE VOTACIÓN LOCAL ===");
            gestorVotosSQLite.mostrarEstadisticas();
        } else {
            System.out.println("🗳️ Gestor de Votos SQLite: No disponible");
        }
        
        System.out.println("═".repeat(50));
    }

    public void shutdown() {
        System.out.println("🛑 Cerrando gestor de mesa...");
        
        // ⭐ NUEVO: Detener sincronizador automático
        if (sincronizadorVotos != null && sincronizadorVotos.estaActivo()) {
            System.out.println("🔄 Deteniendo sincronización automática...");
            sincronizadorVotos.detener();
        }
        
        if (messageManager != null) {
            messageManager.shutdown();
        }
        if (adapter != null) {
            adapter.destroy();
        }
        // ⭐ NUEVO: Cerrar conexiones SQLite de votos
        if (gestorVotosSQLite != null) {
            gestorVotosSQLite.cerrarConexiones();
        }
        
        System.out.println("✅ Gestor de mesa cerrado correctamente");
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

    // ========== MÉTODOS ADICIONALES PARA CANDIDATOS ==========
    
    /**
     * Método adicional para obtener candidatos (no parte de IMesaVotacion)
     */
    public Candidato[] obtenerCandidatos(Current current) {
        System.out.println("📋 Obteniendo lista de candidatos para Mesa " + idMesa);
        
        // Si no hay candidatos cargados, intentar obtenerlos
        if (candidatosDisponibles.isEmpty() || !candidatosCargados) {
            System.out.println("⚠️ No hay candidatos cargados, intentando obtener desde Servidor Regional...");
            
            if (gestorCandidatos != null) {
                List<Candidato> candidatosRegionales = gestorCandidatos.obtenerCandidatosLocales();
                if (!candidatosRegionales.isEmpty()) {
                    candidatosDisponibles.clear();
                    candidatosDisponibles.addAll(candidatosRegionales);
                    candidatosCargados = true;
                }
            }
            
            if (candidatosDisponibles.isEmpty()) {
                System.out.println("⚠️ No se pudieron obtener candidatos, usando candidatos por defecto");
                cargarCandidatosPorDefecto();
            }
        }
        
        System.out.println("✅ Retornando " + candidatosDisponibles.size() + " candidatos disponibles");
        return candidatosDisponibles.toArray(new Candidato[0]);
    }
    
    /**
     * Método adicional para inicializar la mesa (no parte de IMesaVotacion)
     */
    public void inicializarMesa(String mesaId, Current current) {
        try {
            this.idMesa = mesaId;
            
            System.out.println("🗳️ === INICIALIZANDO MESA DE VOTACIÓN " + mesaId + " ===");
            System.out.println("📋 Configurando componentes de la mesa...");
            
            // ⭐ 1. INICIALIZAR GESTOR DE CANDIDATOS (NUEVO)
            System.out.println("🗳️ Inicializando gestor de candidatos desde Servidor Regional...");
            try {
                gestorCandidatos = new GestorCandidatosMesa(mesaId, "tcp -h localhost -p 8080");
                System.out.println("✅ Gestor de candidatos inicializado");
                
                // Obtener candidatos desde servidor regional
                List<Candidato> candidatosRegionales = gestorCandidatos.obtenerCandidatosLocales();
                
                if (!candidatosRegionales.isEmpty()) {
                    candidatosDisponibles.clear();
                    candidatosDisponibles.addAll(candidatosRegionales);
                    candidatosCargados = true;
                    System.out.println("✅ " + candidatosDisponibles.size() + " candidatos obtenidos desde Servidor Regional");
                } else {
                    System.out.println("⚠️ No se pudieron obtener candidatos del Servidor Regional");
                    System.out.println("🔄 Usando candidatos por defecto como respaldo...");
                    cargarCandidatosPorDefecto();
                }
                
            } catch (Exception e) {
                System.err.println("❌ Error inicializando gestor de candidatos: " + e.getMessage());
                System.out.println("🔄 Usando candidatos por defecto como respaldo...");
                cargarCandidatosPorDefecto();
            }
            
            // Mostrar candidatos cargados
            System.out.println("\n🗳️ === CANDIDATOS DISPONIBLES ===");
            for (Candidato candidato : candidatosDisponibles) {
                System.out.println("  • ID: " + candidato.idCandidato + " | " + 
                                  candidato.nombre + " (" + candidato.partido + ")");
            }
            
            // ⭐ 2. MOSTRAR ESTADÍSTICAS
            if (gestorCandidatos != null) {
                System.out.println("\n📊 === ESTADÍSTICAS DE CANDIDATOS ===");
                gestorCandidatos.mostrarEstadisticas();
            }
            
            System.out.println("✅ Mesa " + mesaId + " inicializada correctamente con candidatos");
            System.out.println("🗳️ Sistema listo para recibir votaciones");
            System.out.println("═".repeat(60));
            
        } catch (Exception e) {
            System.err.println("❌ Error inicializando Mesa " + mesaId + ": " + e.getMessage());
            e.printStackTrace();
            // Como respaldo, cargar candidatos por defecto
            cargarCandidatosPorDefecto();
        }
    }

    // ========== IMPLEMENTACIÓN DE IMesaVotacion ==========
    
    @Override
    public boolean recibirVotantesAsignados(CiudadanoInfo[] votantes, String departamento, Current current) {
        System.out.println("📥 Recibiendo " + votantes.length + " votantes asignados para departamento: " + departamento);
        // Implementación delegada al gestor de votantes
        if (gestorVotantesSQLite != null) {
            // Convertir y guardar votantes
            return true; // Simplificado por ahora
        }
        return false;
    }
    
    @Override
    public boolean recibirArchivoSQLite(byte[] datosArchivo, String nombreArchivo, Current current) {
        System.out.println("📁 Recibiendo archivo SQLite: " + nombreArchivo + " (" + datosArchivo.length + " bytes)");
        // Implementación de recepción de archivo SQLite
        return true; // Simplificado por ahora
    }
    
    @Override
    public boolean estaListaParaRecibir(Current current) {
        System.out.println("❓ Verificando si la mesa está lista para recibir datos...");
        return gestorVotantesSQLite != null && gestorCandidatos != null;
    }
    
    @Override
    public boolean verificarVotanteEnMesa(String documento, Current current) {
        System.out.println("🔍 Verificando votante con documento: " + documento);
        return validarElector(documento);
    }
    
    @Override
    public CiudadanoInfo obtenerVotanteDeMesa(String documento, Current current) {
        System.out.println("👤 Obteniendo información del votante: " + documento);
        
        if (gestorVotantesSQLite == null) {
            return null;
        }
        
        try {
            List<VotanteMesa> votantes = gestorVotantesSQLite.obtenerVotantesLocales();
            for (VotanteMesa votante : votantes) {
                if (votante.documento.equals(documento)) {
                    CiudadanoInfo info = new CiudadanoInfo();
                    info.documento = votante.documento;
                    info.nombre = votante.nombre;
                    info.apellido = votante.apellido;
                    info.mesa = votante.mesa;
                    info.municipio = votante.municipio;
                    info.departamento = votante.departamento;
                    return info;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo votante: " + e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public EstadisticasMesa obtenerEstadisticasMesa(Current current) {
        System.out.println("📊 Obteniendo estadísticas de la mesa: " + idMesa);
        
        EstadisticasMesa estadisticas = new EstadisticasMesa();
        estadisticas.mesaId = idMesa;
        estadisticas.votantesAsignados = 0;
        estadisticas.votantesVerificados = 0;
        estadisticas.mesaActiva = true;
        estadisticas.ultimaActualizacion = System.currentTimeMillis();
        
        if (gestorVotantesSQLite != null) {
            try {
                List<VotanteMesa> votantes = gestorVotantesSQLite.obtenerVotantesLocales();
                estadisticas.votantesAsignados = votantes.size();
                estadisticas.votantesVerificados = (int) votantes.stream()
                    .filter(v -> v.verificado == 1)
                    .count();
                    
                if (!votantes.isEmpty()) {
                    VotanteMesa primerVotante = votantes.get(0);
                    estadisticas.departamento = primerVotante.departamento;
                    estadisticas.municipio = primerVotante.municipio;
                    estadisticas.puesto = primerVotante.puesto;
                }
            } catch (Exception e) {
                System.err.println("❌ Error obteniendo estadísticas: " + e.getMessage());
            }
        }
        
        return estadisticas;
    }
    
    @Override
    public int contarVotantesEnMesa(Current current) {
        System.out.println("🔢 Contando votantes en la mesa: " + idMesa);
        
        if (gestorVotantesSQLite != null) {
            try {
                List<VotanteMesa> votantes = gestorVotantesSQLite.obtenerVotantesLocales();
                return votantes.size();
            } catch (Exception e) {
                System.err.println("❌ Error contando votantes: " + e.getMessage());
            }
        }
        
        return 0;
    }
    
    @Override
    public boolean limpiarDatosMesa(Current current) {
        System.out.println("🧹 Limpiando datos de la mesa: " + idMesa);
        
        try {
            candidatosDisponibles.clear();
            electoresYaVotaron.clear();
            candidatosCargados = false;
            
            System.out.println("✅ Datos de la mesa limpiados");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error limpiando datos: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean verificarEstadoMesa(Current current) {
        System.out.println("🔍 Verificando estado de la mesa: " + idMesa);
        
        boolean estadoOK = true;
        
        // Verificar gestor de votantes
        if (gestorVotantesSQLite == null) {
            System.out.println("❌ Gestor de votantes no disponible");
            estadoOK = false;
        }
        
        // Verificar candidatos cargados
        if (candidatosDisponibles.isEmpty()) {
            System.out.println("❌ No hay candidatos cargados");
            estadoOK = false;
        }
        
        if (estadoOK) {
            System.out.println("✅ Estado de la mesa: OK");
        } else {
            System.out.println("⚠️ Estado de la mesa: CON PROBLEMAS");
        }
        
        return estadoOK;
    }
    
    @Override
    public String obtenerIdMesa(Current current) {
        return idMesa;
    }

    // ⭐ NUEVOS MÉTODOS PARA CONTROL DEL SINCRONIZADOR
    
    /**
     * Fuerza una sincronización inmediata de votos
     */
    public boolean sincronizarVotosAhora() {
        if (sincronizadorVotos != null) {
            return sincronizadorVotos.sincronizarAhora();
        }
        System.err.println("❌ Sincronizador no disponible");
        return false;
    }
    
    /**
     * Muestra estadísticas del sincronizador automático
     */
    public void mostrarEstadisticasSincronizacion() {
        if (sincronizadorVotos != null) {
            sincronizadorVotos.mostrarEstadisticas();
        } else {
            System.out.println("⚠️ Sincronizador automático no disponible");
        }
    }
    
    /**
     * Verifica si el sincronizador está activo
     */
    public boolean isSincronizadorActivo() {
        return sincronizadorVotos != null && sincronizadorVotos.estaActivo();
    }
    
    /**
     * Obtiene el gestor de votos SQLite
     */
    public GestorVotosSQLite getGestorVotosSQLite() {
        return gestorVotosSQLite;
    }
}