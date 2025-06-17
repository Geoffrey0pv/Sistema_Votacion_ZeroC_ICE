package GestorMesa;

import Demo.*;
import Demo.Candidato;
import Demo.ICargarCandidatosPrx;
import Demo.IConfirmacionCandidatosPrx;
import Demo.IConfirmacionVotoPrx;
import Demo.IRecibirCandidatos;
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


public class GestorMesa implements IRecibirCandidatos {
    private final ReliableMessageManager messageManager;
    private IRegistrarVotoPrx servidorRegional;
    private ICargarCandidatosPrx gestionCandidatos;
    private ObjectAdapter adapter;
    private Communicator communicator;
    private com.zeroc.IceGrid.QueryPrx query;
    private String idMesa;
    private List<Candidato> candidatosDisponibles;
    private List<String> electoresYaVotaron;
    private boolean candidatosCargados = false;
    
    // NUEVO: Sistema de Verificación integrado
    private SistemaVerificacion sistemaVerificacion;
    private boolean verificacionLocalActiva = false;
    
    // NUEVO: Gestor de Candidatos SQLite para consumir desde Servidor Regional
    private GestorCandidatosSQLite gestorCandidatosSQLite;
    private boolean sincronizacionAutomatica = true;

    public GestorMesa(String idMesa) {
        this.idMesa = idMesa;
        this.messageManager = new ReliableMessageManager();
        this.candidatosDisponibles = new ArrayList<>();
        this.electoresYaVotaron = new ArrayList<>();

        // Inicializar sistema de verificación SQLite si está disponible
        try {
            this.sistemaVerificacion = new SistemaVerificacion(idMesa);
            this.verificacionLocalActiva = true;
            System.out.println("✅ Sistema de verificación local inicializado para Mesa " + idMesa);
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo inicializar verificación local: " + e.getMessage());
            System.out.println("💡 Funcionará en modo básico sin verificación SQLite");
            this.verificacionLocalActiva = false;
        }
        
        // NUEVO: Inicializar gestor de candidatos SQLite
        try {
            this.gestorCandidatosSQLite = new GestorCandidatosSQLite(idMesa, "tcp -h localhost -p 8080");
            System.out.println("✅ Gestor de Candidatos SQLite inicializado para Mesa " + idMesa);
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo inicializar Gestor de Candidatos SQLite: " + e.getMessage());
            System.out.println("💡 Continuará con sistema de candidatos tradicional");
            this.gestorCandidatosSQLite = null;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public boolean inicializar(Communicator communicator) {
        this.communicator = communicator;

        try {
            // Crear y activar el adaptador usando la configuración del .cfg
            this.adapter = communicator.createObjectAdapter("MesaCallbackAdapter");

            // Registrar esta mesa como receptor de candidatos
            Identity mesaIdentity = new Identity();
            mesaIdentity.name = idMesa;
            mesaIdentity.category = "RecibirCandidatos";
            adapter.add(this, mesaIdentity);

            adapter.activate();

            // Obtener el proxy de IceGrid Query usando el Locator configurado
            this.query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                    communicator.stringToProxy("DemoIceGrid/Query"));

            this.servidorRegional = obtenerServidorRegional(communicator);
            this.gestionCandidatos = obtenerGestionCandidatos(communicator);

            if (servidorRegional != null) {
                System.out.println("Gestor de Mesa inicializado correctamente");
                System.out.println("  Mesa ID: " + idMesa);
                System.out.println("  Conectado al servidor regional");

                // Procesar mensajes pendientes si los hay
                if (messageManager.hayMensajesPendientes()) {
                    System.out.println("📤 Procesando mensajes pendientes...");
                    messageManager.procesarMensajesPendientes(servidorRegional, adapter, communicator);
                }

                solicitarCandidatos();

                return true;
            } else {
                System.err.println("⚠️  No hay servidor regional disponible");
                System.err.println("   📤 Los votos se guardarán para envío posterior");
                return false;
            }

        } catch (com.zeroc.Ice.Exception e) {
            System.err.println("❌ Error inicializando gestor de mesa: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void recibirCandidatos(Candidato[] candidatos, IConfirmacionCandidatosPrx callback, Current current) {
        try {
            System.out.println("📋 Recibiendo lista de candidatos del servidor regional...");
            if (candidatos != null && candidatos.length > 0) {
                this.candidatosDisponibles = new ArrayList<>();
                for (Candidato candidato : candidatos) {
                    candidatosDisponibles.add(candidato);
                    System.out.println("   ✓ " + candidato.idCandidato + " - " + candidato.nombre +
                            " (" + candidato.partido + ")");
                }
                System.out.println("✅ " + candidatos.length + " candidatos recibidos correctamente");
                
                try {
                    callback.recibirConfirmacion(true,
                            "Candidatos recibidos correctamente en mesa " + idMesa);
                } catch (Exception callbackEx) {
                    System.err.println(" Error enviando confirmación: " + callbackEx.getMessage());
                }
            } else {
                System.err.println("❌ Lista de candidatos vacía o nula");
                try {
                    callback.recibirConfirmacion(false,
                            "Error procesando candidatos en mesa " + idMesa + ": Lista vacía");
                } catch (Exception callbackEx) {
                    System.err.println("Error enviando confirmación de error: " + callbackEx.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error procesando candidatos: " + e.getMessage());
            e.printStackTrace();
            try {
                callback.recibirConfirmacion(false,
                        "Error procesando candidatos en mesa " + idMesa + ": " + e.getMessage());
            } catch (Exception callbackEx) {
                System.err.println("Error enviando confirmación de error: " + callbackEx.getMessage());
            }
        }
    }

    private void solicitarCandidatos() {
        if (gestionCandidatos != null) {
            try {
                System.out.println(" Solicitando candidatos al servidor regional...");

                // Obtener la configuración del adaptador desde el communicator
                Properties props = communicator.getProperties();
                String endpoints = props.getProperty("MesaCallbackAdapter.Endpoints");
                
                // Construir el endpoint para la mesa
                String endpointMesa = idMesa + ":" + endpoints;
                System.out.println(" Usando endpoint: " + endpointMesa);

                CompletableFuture.supplyAsync(() -> {
                    try {
                        return gestionCandidatos.enviarCandidatosAMesas(endpointMesa);
                    } catch (Exception e) {
                        System.err.println("Error en solicitud asíncrona: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }).orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(resultado -> {
                    if (!resultado) {
                        System.err.println("  Error en la solicitud de candidatos");
                        if (!candidatosCargados) {
                            cargarCandidatosPorDefecto();
                        }
                    }
                }).exceptionally(ex -> {
                    System.err.println("  Timeout o error en solicitud de candidatos: " + ex.getMessage());
                    if (!candidatosCargados) {
                        cargarCandidatosPorDefecto();
                    }
                    return null;
                });

                // Esperar un poco más para dar tiempo a que lleguen los candidatos
                Thread.sleep(5000);

                if (!candidatosCargados) {
                    System.err.println("  No se recibieron candidatos después de esperar, cargando por defecto");
                    cargarCandidatosPorDefecto();
                }

            } catch (Exception e) {
                System.err.println(" Error solicitando candidatos: " + e.getMessage());
                e.printStackTrace();
                if (!candidatosCargados) {
                    cargarCandidatosPorDefecto();
                }
            } catch (InterruptedException e) {
                System.err.println(" Interrupción durante la espera: " + e.getMessage());
                Thread.currentThread().interrupt();
                if (!candidatosCargados) {
                    cargarCandidatosPorDefecto();
                }
            }
        } else {
            System.err.println("  No hay conexión con GestionCandidatos, cargando candidatos por defecto");
            cargarCandidatosPorDefecto();
        }
    }

    public void cargarCandidatos() {
        if (!candidatosCargados) {
            // NUEVO: Intentar sincronizar candidatos desde Servidor Regional primero
            if (sincronizacionAutomatica && gestorCandidatosSQLite != null) {
                System.out.println("🔄 Intentando sincronizar candidatos desde Servidor Regional...");
                boolean sincronizado = sincronizarCandidatosDesdeServidorRegional();
                if (sincronizado) {
                    System.out.println("✅ Candidatos sincronizados desde Servidor Regional");
                    return;
                }
                System.out.println("⚠️ No se pudieron sincronizar desde Servidor Regional, continuando con método tradicional...");
            }
            
            // Método tradicional de solicitar candidatos
            solicitarCandidatos();

            // Esperar un momento para que lleguen los candidatos del servidor
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Si aún no se han cargado, usar candidatos por defecto
            if (!candidatosCargados) {
                cargarCandidatosPorDefecto();
            }
        }
    }

    private void cargarCandidatosPorDefecto() {
        System.out.println("📋 Cargando candidatos por defecto...");
        candidatosDisponibles.clear();
        candidatosDisponibles.add(new Candidato(1, "Juan Pérez", "Partido A"));
        candidatosDisponibles.add(new Candidato(2, "María García", "Partido B"));
        candidatosDisponibles.add(new Candidato(3, "Carlos López", "Partido C"));
        candidatosDisponibles.add(new Candidato(4, "Ana Martínez", "Partido D"));
        candidatosDisponibles.add(new Candidato(5, "Luis Rodríguez", "Partido E"));

        candidatosCargados = true;
        System.out.println("✅ Candidatos por defecto cargados: " + candidatosDisponibles.size());
    }

    public boolean validarElector(String documentoIdentidad) {
        // 1. VALIDACIÓN BÁSICA (Original)
        if (documentoIdentidad == null || documentoIdentidad.trim().isEmpty()) {
            System.err.println("❌ Documento de identidad no válido");
            return false;
        }
        
        String hashElector = generarHashElector(documentoIdentidad);
        if (electoresYaVotaron.contains(hashElector)) {
            System.err.println("❌ El elector ya votó en esta mesa");
            return false;
        }

        // 2. NUEVA VALIDACIÓN: Verificar que pertenece a esta mesa
        if (verificacionLocalActiva) {
            try {
                boolean perteneceAMesa = sistemaVerificacion.verificarVotante(documentoIdentidad);
                if (!perteneceAMesa) {
                    System.err.println("❌ El documento " + documentoIdentidad.substring(0, Math.min(3, documentoIdentidad.length())) + "*** NO está registrado en esta mesa");
                    System.err.println("💡 Verifique que está en la mesa correcta");
                    return false;
                }
                System.out.println("✅ Documento verificado: pertenece a Mesa " + idMesa);
            } catch (Exception e) {
                System.err.println("⚠️ Error en verificación local: " + e.getMessage());
                System.out.println("💡 Continuando con validación básica...");
            }
        } else {
            System.out.println("⚠️ Verificación local no disponible - usando validación básica");
        }

        // 3. VALIDACIÓN EXITOSA
        System.out.println("✅ Elector validado: " + documentoIdentidad.substring(0,
                Math.min(3, documentoIdentidad.length())) + "***");
        return true;
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
                
                // NUEVO: Registrar voto en SQLite local si está disponible
                if (gestorCandidatosSQLite != null) {
                    try {
                        boolean registradoLocal = gestorCandidatosSQLite.registrarVotoCandidato(idCandidato);
                        if (registradoLocal) {
                            System.out.println("📊 Voto registrado en estadísticas locales");
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Error registrando en estadísticas locales: " + e.getMessage());
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
        ICargarCandidatosPrx nuevaGestionCandidatos = obtenerGestionCandidatos(communicator);

        if (nuevoServidorVotos != null) {
            this.servidorRegional = nuevoServidorVotos;
            this.gestionCandidatos = nuevaGestionCandidatos;

            System.out.println(" Reconectado al servidor regional");

            if (messageManager.hayMensajesPendientes()) {
                System.out.println(" Procesando mensajes pendientes...");
                messageManager.procesarMensajesPendientes(servidorRegional, adapter, communicator);
            }

            // Solicitar candidatos actualizados
            if (!candidatosCargados || candidatosDisponibles.isEmpty()) {
                solicitarCandidatos();
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

    private ICargarCandidatosPrx obtenerGestionCandidatos(com.zeroc.Ice.Communicator communicator) {
        ICargarCandidatosPrx cargarCandidatos = null;
        
        try {
            // Conectar al servicio de gestión de candidatos
            System.out.println("🔗 Conectando al servicio de gestión de candidatos...");
            
            // Usar IceGrid Query para encontrar el servicio
            com.zeroc.IceGrid.QueryPrx query = com.zeroc.IceGrid.QueryPrx.checkedCast(
                    communicator.stringToProxy("DemoIceGrid/Query"));
            
            if (query != null) {
                try {
                    // Buscar por tipo de interfaz
                    com.zeroc.Ice.ObjectPrx obj = query.findObjectByType("::Demo::ICargarCandidatos");
                    if (obj != null) {
                        cargarCandidatos = ICargarCandidatosPrx.checkedCast(obj);
                        if (cargarCandidatos != null) {
                            System.out.println("✅ Conectado al servicio de candidatos via IceGrid Query");
                            return cargarCandidatos;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error buscando gestión candidatos por tipo: " + e.getMessage());
                }
                
                // Intentar por identidad específica
                try {
                    com.zeroc.Ice.ObjectPrx objById = query.findObjectById(
                            com.zeroc.Ice.Util.stringToIdentity("gestionCandidatos"));
                    if (objById != null) {
                        cargarCandidatos = ICargarCandidatosPrx.checkedCast(objById);
                        if (cargarCandidatos != null) {
                            System.out.println("✅ Conectado al servicio de candidatos via identidad");
                            return cargarCandidatos;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error buscando gestión candidatos por identidad: " + e.getMessage());
                }
            }
            
            // Método alternativo: conexión directa
            try {
                System.out.println("🔄 Intentando conexión directa al servicio de candidatos...");
                String proxyString = "gestionCandidatos@RegionalAdapter";
                com.zeroc.Ice.ObjectPrx directObj = communicator.stringToProxy(proxyString);
                if (directObj != null) {
                    cargarCandidatos = ICargarCandidatosPrx.checkedCast(directObj);
                    if (cargarCandidatos != null) {
                        System.out.println("✅ Conectado directamente al servicio de candidatos");
                        return cargarCandidatos;
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️  Error en conexión directa a candidatos: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error general conectando al servicio de candidatos: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.err.println("❌ No se pudo establecer conexión con el servicio de gestión de candidatos");
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
        System.out.println("🔍 Verificación local activa: " + (verificacionLocalActiva ? "Sí" : "No"));
        System.out.println("📁 Mensajes pendientes: " + (messageManager.hayMensajesPendientes() ? "Sí" : "No"));
        
        // NUEVO: Mostrar estadísticas del GestorCandidatosSQLite
        if (gestorCandidatosSQLite != null) {
            System.out.println("🔄 Sincronización Servidor Regional: Disponible");
            System.out.println("📊 Base de datos local: " + gestorCandidatosSQLite.getDbPath());
            System.out.println("🔌 Conectado al Servidor Regional: " + (gestorCandidatosSQLite.isConectado() ? "Sí" : "No"));
            
            // Mostrar resumen de candidatos SQLite
            try {
                gestorCandidatosSQLite.mostrarResumenCandidatos();
            } catch (Exception e) {
                System.err.println("⚠️ Error mostrando estadísticas SQLite: " + e.getMessage());
            }
        } else {
            System.out.println("🔄 Sincronización Servidor Regional: No disponible");
        }
        
        System.out.println("═".repeat(50));
    }

    /**
     * NUEVO: Obtiene el gestor de candidatos SQLite
     */
    public GestorCandidatosSQLite getGestorCandidatosSQLite() {
        return gestorCandidatosSQLite;
    }
    
    /**
     * NUEVO: Verifica si la sincronización con Servidor Regional está disponible
     */
    public boolean isSincronizacionDisponible() {
        return gestorCandidatosSQLite != null;
    }
    
    /**
     * NUEVO: Obtiene el estado de la sincronización automática
     */
    public boolean isSincronizacionAutomatica() {
        return sincronizacionAutomatica;
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
     * NUEVO: Sincroniza candidatos consumiendo del Servidor Regional via IConsultaMesaSQLite
     */
    public boolean sincronizarCandidatosDesdeServidorRegional() {
        if (gestorCandidatosSQLite == null) {
            System.err.println("❌ Gestor de Candidatos SQLite no disponible");
            return false;
        }
        
        try {
            System.out.println("🔌 Iniciando sincronización con Servidor Regional...");
            
            // Conectar al Servidor Regional
            boolean conectado = gestorCandidatosSQLite.inicializarConexionICE(this.communicator);
            if (!conectado) {
                System.err.println("❌ No se pudo conectar al Servidor Regional");
                return false;
            }
            
            // Sincronizar candidatos
            boolean sincronizado = gestorCandidatosSQLite.sincronizarCandidatos();
            if (!sincronizado) {
                System.err.println("❌ Error en sincronización de candidatos");
                return false;
            }
            
            // Cargar candidatos sincronizados en la memoria del GestorMesa
            List<Candidato> candidatosSincronizados = gestorCandidatosSQLite.obtenerCandidatosDisponibles();
            if (candidatosSincronizados != null && !candidatosSincronizados.isEmpty()) {
                candidatosDisponibles.clear();
                candidatosDisponibles.addAll(candidatosSincronizados);
                candidatosCargados = true;
                
                System.out.println("✅ " + candidatosSincronizados.size() + " candidatos cargados desde Servidor Regional");
                mostrarCandidatosCargados();
                return true;
            } else {
                System.err.println("❌ No se obtuvieron candidatos válidos");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error sincronizando candidatos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * NUEVO: Muestra los candidatos que se han cargado exitosamente
     */
    private void mostrarCandidatosCargados() {
        System.out.println("\n📊 === CANDIDATOS CARGADOS EN MESA " + idMesa + " ===");
        for (int i = 0; i < candidatosDisponibles.size(); i++) {
            Candidato candidato = candidatosDisponibles.get(i);
            System.out.printf("   %d. [%d] %s - %s%n", 
                (i + 1), 
                candidato.idCandidato, 
                candidato.nombre, 
                candidato.partido
            );
        }
        System.out.println("═".repeat(50));
    }
    
    /**
     * NUEVO: Permite activar/desactivar la sincronización automática
     */
    public void configurarSincronizacionAutomatica(boolean activar) {
        this.sincronizacionAutomatica = activar;
        System.out.println((activar ? "✅ Activada" : "❌ Desactivada") + " sincronización automática de candidatos");
    }
    
    /**
     * NUEVO: Fuerza la sincronización de candidatos bajo demanda
     */
    public boolean forzarSincronizacionCandidatos() {
        candidatosCargados = false; // Reset para forzar nueva sincronización
        return sincronizarCandidatosDesdeServidorRegional();
    }
}