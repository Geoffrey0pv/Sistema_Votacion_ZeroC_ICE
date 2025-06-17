module Demo
{
    struct Voto
    {
        long   idVoto;
        string idMesa;
        string idElectorHash;
        long   idCandidato;
        long   tsEmitido;
    };
    struct Ack
    {
        long   idVoto;
        bool   registrado;
        string mensaje;
    };
    struct Candidato
    {
        long   idCandidato;
        string nombre;
        string partido;
    };

    sequence<Candidato> SeqCandidatos;

    interface IConfirmacionVoto
    {
        void recibirAck(Ack a);
    };
    interface IConfirmacionCandidatos
    {
        void recibirConfirmacion(bool ok, string mensaje);
    };
    interface IRegistrarVoto
    {
        void enviarVoto(Voto v, IConfirmacionVoto* callback);
    };
    interface IRecibirCandidatos
    {
        void recibirCandidatos(SeqCandidatos candidatos, IConfirmacionCandidatos* callback);
    };
    interface ICargarCandidatos{
        bool enviarCandidatosATodasMesas();
        bool enviarCandidatosAMesas(string endpointMesa);
    };
    interface IAdministradorCandidatos
    {
        bool cargarCandidatosDesdeCSV(string rutaArchivo);
        bool cargarCandidatosDesdeArray(SeqCandidatos candidatos);
        int obtenerCantidadCandidatos();
        SeqCandidatos obtenerTodosCandidatos();
        bool limpiarCandidatos();
        bool enviarCandidatosARegional(string endpointRegional);
        bool enviarCandidatosATodosRegionales();
    };
    interface IPersistenciaCandidatos
    {
        bool guardarCandidatos(SeqCandidatos candidatos);
        SeqCandidatos cargarCandidatos();
        bool limpiarCandidatos();
        int obtenerCantidadCandidatos();
        bool hayDatos();
    };

    // ========== NUEVAS INTERFACES PARA PATRÓN BROKER ==========
    
    struct MetricasRecursos
    {
        double cpuUsage;
        double memoryUsage;
        double networkUsage;
        long requestCount;
        long timestamp;
        string nodeId;
    };

    struct InfoReplica
    {
        string nodeId;
        string endpoint;
        bool activa;
        MetricasRecursos metricas;
        long tiempoCreacion;
    };

    sequence<InfoReplica> SeqReplicas;

    interface IMonitorRecursos
    {
        MetricasRecursos obtenerMetricas();
        bool estaDisponible();
        void notificarCarga(double carga);
    };

    interface IGestorReplicas
    {
        bool crearReplica(string nodeId, string endpoint);
        bool eliminarReplica(string nodeId);
        SeqReplicas obtenerReplicasActivas();
        bool activarReplica(string nodeId);
        bool desactivarReplica(string nodeId);
        InfoReplica obtenerInfoReplica(string nodeId);
    };

    interface IBalanceadorCarga
    {
        string obtenerMejorReplica();
        void registrarReplica(string nodeId, string endpoint);
        void desregistrarReplica(string nodeId);
        void actualizarMetricas(string nodeId, MetricasRecursos metricas);
        SeqReplicas obtenerEstadoReplicas();
    };

    interface IBrokerNacional extends IAdministradorCandidatos
    {
        // Métodos heredados de IAdministradorCandidatos
        // Métodos adicionales del broker
        bool registrarReplica(string nodeId, string endpoint);
        bool desregistrarReplica(string nodeId);
        MetricasRecursos obtenerMetricasGlobales();
        SeqReplicas obtenerReplicasDisponibles();
        string obtenerEndpointOptimo();
        bool escalarAutomaticamente();
        bool reducirReplicas();
    };

    interface IReplicaNacional extends IAdministradorCandidatos, IMonitorRecursos
    {
        // Combina funcionalidades de administración y monitoreo
        bool sincronizarConMaster(SeqCandidatos candidatos);
        bool notificarEstado(MetricasRecursos metricas);
        string obtenerNodeId();
    };

    // ========== INTERFAZ INFORMACIÓN DE RÉPLICAS ==========
    
    struct InfoEjecucionReplica
    {
        string replicaId;
        string nodeId;
        int puerto;
        string host;
        string endpoint;
        bool activa;
        long tiempoInicio;
        MetricasRecursos metricas;
    };

    sequence<InfoEjecucionReplica> SeqInfoReplicas;

    interface IReplicaInfo
    {
        // Obtener información de la réplica actual
        InfoEjecucionReplica obtenerInfoReplica();
        
        // Obtener puerto de ejecución
        int obtenerPuertoEjecucion();
        
        // Obtener endpoint completo
        string obtenerEndpoint();
        
        // Obtener ID de la réplica
        string obtenerReplicaId();
        
        // Verificar si la réplica está activa
        bool estaActiva();
        
        // Obtener tiempo de actividad
        long obtenerTiempoActividad();
    };

    // ========== INTERFAZ HELLO WORLD ==========
    
    interface IHelloWorld
    {
        string sayHello();
        string sayHelloTo(string name);
        string getServerInfo();
        long getCurrentTime();
    };

    // ========== INTERFAZ CONSULTA MESA ==========
    
    struct MesaInfo
    {
        string departamento;
        string municipio;
        string puesto;
        string mesa;
    };

    interface IConsultaMesa
    {
        MesaInfo consultarMesaPorDocumento(string documento);
        bool verificarConexionBD();
    };

    // ========== INTERFAZ CONSULTA CIUDADANOS ==========
    
    struct CiudadanoInfo
    {
        long id;
        string documento;
        string nombre;
        string apellido;
        string mesa;
        string mesaId;
        string puesto;
        string municipio;
        string departamento;
    };

    sequence<CiudadanoInfo> SeqCiudadanos;
    sequence<string> SeqDepartamentos;
    sequence<string> SeqMesas;
    sequence<string> SeqStrings;
    sequence<byte> SeqBytes;

    struct ResultadoPaginado
    {
        SeqCiudadanos ciudadanos;
        long totalRegistros;
        int paginaActual;
        int totalPaginas;
        bool hayMasPaginas;
    };

    interface IConsultaCiudadanos
    {
        // Método original (mantener compatibilidad) - con límite por defecto de 1000
        SeqCiudadanos consultarCiudadanosPorDepartamentos(SeqDepartamentos departamentos);
        
        // Método optimizado con paginación
        ResultadoPaginado consultarCiudadanosPaginado(SeqDepartamentos departamentos, int pagina, int tamanoPagina);
        
        // Método para obtener solo el conteo (rápido)
        long contarCiudadanosPorDepartamentos(SeqDepartamentos departamentos);
        
        // Método con límite personalizable
        SeqCiudadanos consultarCiudadanosConLimite(SeqDepartamentos departamentos, int limite);
        
        bool verificarConexionBD();
    };

    // ========== INTERFAZ CONSULTA CANDIDATOS ==========
    
    struct CandidatoElectoral
    {
        long id;
        string nombre;
        string partido;
        string fechaCreacion;
        bool activo;
    };

    sequence<CandidatoElectoral> SeqCandidatosElectorales;

    interface IConsultaCandidatos
    {
        // Obtener todos los candidatos electorales
        SeqCandidatosElectorales obtenerTodosCandidatosElectorales();
        
        // Obtener candidatos por partido
        SeqCandidatosElectorales obtenerCandidatosPorPartido(string partido);
        
        // Contar total de candidatos
        long contarCandidatos();
        
        // Verificar conexión a BD
        bool verificarConexionBD();
        
        // ========== MÉTODOS ADICIONALES PARA MESAS ==========
        
        // Buscar candidato por ID
        CandidatoElectoral buscarCandidatoPorId(long idCandidato);
        
        // Buscar candidatos por nombre (búsqueda parcial)
        SeqCandidatosElectorales buscarCandidatosPorNombre(string nombre);
        
        // Obtener lista de partidos disponibles
        SeqStrings obtenerPartidosDisponibles();
        
        // Sincronizar candidatos desde servidor nacional
        bool sincronizarCandidatos();
        
        // Verificar si un candidato es válido
        bool validarCandidato(long idCandidato);
        
        // Obtener candidatos para una mesa específica
        SeqCandidatosElectorales obtenerCandidatosParaMesa(string mesaId);
        
        // Verificar conectividad del servicio
        bool verificarServicio();
    };

    // ========== INTERFAZ REGISTRO DE VOTOS ==========
    
    struct VotoCompleto
    {
        long id;
        string mesaId;
        long timestamp;
        long candidatoId;
        string hashVerificacion;
        string municipio;
        string departamento;
    };

    sequence<VotoCompleto> SeqVotosCompletos;

    struct ResultadoRegistroVotos
    {
        bool exito;
        int totalVotos;
        int votosRegistrados;
        int votosRechazados;
        string mensaje;
        long tiempoProcessamiento;
    };

    interface IRegistroVotos
    {
        // Registrar un solo voto
        bool registrarVoto(VotoCompleto voto);
        
        // Registrar múltiples votos en lote
        ResultadoRegistroVotos registrarVotosLote(SeqVotosCompletos votos);
        
        // Verificar si un voto ya existe por hash
        bool existeVotoPorHash(string hashVerificacion);
        
        // Obtener estadísticas de votos
        long contarVotosPorMesa(string mesaId);
        long contarVotosPorCandidato(long candidatoId);
        long contarVotosPorMunicipio(string municipio);
        
        // Verificar conexión a BD
        bool verificarConexionBD();
    };

    // ========== INTERFAZ RECEPTOR DE VOTOS REGIONAL ==========
    
    struct VotoRegional
    {
        long idVoto;
        string mesaId;
        long timestamp;
        long candidatoId;
        string hashElector;
        string municipio;
        string departamento;
        string estadoRegistro; // "NUEVO", "PROCESADO", "ERROR"
    };

    sequence<VotoRegional> SeqVotosRegionales;

    struct ResultadoRecepcionVotos
    {
        bool exito;
        int totalRecibidos;
        int votosGuardados;
        int votosRechazados;
        string mensaje;
        long tiempoProcessamiento;
        SeqStrings errores; // Lista de errores específicos
    };

    interface IReceptorVotosRegional
    {
        // Recibir y guardar una lista de votos en SQLite
        ResultadoRecepcionVotos recibirListaVotos(SeqVotosRegionales votos);
        
        // Recibir un solo voto
        bool recibirVoto(VotoRegional voto);
        
        // Obtener estadísticas de votos almacenados
        long contarVotosAlmacenados();
        long contarVotosPorMesa(string mesaId);
        long contarVotosPorCandidato(long candidatoId);
        
        // Obtener votos por criterios
        SeqVotosRegionales obtenerVotosPorMesa(string mesaId);
        SeqVotosRegionales obtenerVotosPorCandidato(long candidatoId);
        
        // Verificar si un voto ya existe
        bool existeVoto(long idVoto);
        bool existeVotoPorHash(string hashElector, string mesaId);
        
        // Limpiar votos (para testing)
        bool limpiarVotos();
        bool limpiarVotosMesa(string mesaId);
        
        // Verificar conectividad del servicio
        bool verificarServicio();
        
        // Obtener estadísticas detalladas
        string obtenerEstadisticasDetalladas();
    };

    // ========== INTERFAZ PROCESAMIENTO DE VOTOS EN LOTE ==========
    
    struct ResultadoProcesamiento
    {
        bool exito;
        int totalVotos;
        int votosEncolados;
        string mensaje;
        long timestamp;
    };

    struct EstadisticasVotos
    {
        long totalVotos;
        int totalMesas;
        int totalCandidatos;
        int totalMunicipios;
        string primerVoto;
        string ultimoVoto;
    };

    interface IConfirmacionLoteVotos
    {
        void recibirConfirmacion(ResultadoProcesamiento resultado);
    };

    interface IProcesadorLoteVotos
    {
        void procesarLoteVotos(string jsonVotos, IConfirmacionLoteVotos* callback);
        EstadisticasVotos obtenerEstadisticas();
        bool verificarDisponibilidad();
    };

    // ========== INTERFAZ REPORTES ELECTORALES ==========
    
    struct JornadaStats
    {
        long totalVotos;
        int totalMesas;
        int totalCandidatos;
        string primerVoto;
        string ultimoVoto;
        bool jornadaCerrada;
        string fechaCierre;
    };
    
    struct ReportResult
    {
        bool success;
        string message;
        string reportDirectory;
        int filesGenerated;
    };
    
    exception ReportException
    {
        string reason;
    };
    
    interface IElectoralReports
    {
        // Cerrar jornada electoral
        bool cerrarJornada() throws ReportException;
        
        // Generar todos los reportes CSV
        ReportResult generateAllReports() throws ReportException;
        
        // Obtener estadísticas de la jornada
        JornadaStats getJornadaStats() throws ReportException;
        
        // Verificar si la jornada está cerrada
        bool isJornadaCerrada();
        
        // Obtener fecha de cierre
        string getFechaCierre();
    };

    // ========== INTERFAZ DISTRIBUCIÓN DE MESAS ==========
    
    struct DistribucionMesa
    {
        string mesaId;
        string departamento;
        string municipio;
        string puesto;
        int cantidadVotantes;
        SeqCiudadanos votantes;
    };

    sequence<DistribucionMesa> SeqDistribucionMesas;

    struct ResultadoDistribucion
    {
        bool exito;
        int totalMesas;
        int totalVotantes;
        int mesasDistribuidas;
        string mensaje;
        long timestamp;
    };

    struct EstadisticasMesa
    {
        string mesaId;
        string departamento;
        string municipio;
        string puesto;
        int votantesAsignados;
        int votantesVerificados;
        bool mesaActiva;
        long ultimaActualizacion;
    };

    sequence<EstadisticasMesa> SeqEstadisticasMesas;

    interface IConfirmacionDistribucion
    {
        void recibirConfirmacionDistribucion(ResultadoDistribucion resultado);
    };

    interface IDistribuidorMesas
    {
        // Distribuir votantes de un departamento a todas sus mesas (LOCALMENTE)
        ResultadoDistribucion distribuirVotantesPorDepartamento(string departamento);
        
        // NUEVO: Distribuir votantes remotamente a mesas registradas
        ResultadoDistribucion distribuirVotantesRemotamente(string departamento);
        
        // NUEVO: Enviar archivo SQLite a una mesa específica
        bool enviarArchivoAMesa(string mesaId, string endpointMesa);
        
        // Distribuir votantes específicos a una mesa
        bool distribuirVotantesAMesa(string mesaId, SeqCiudadanos votantes);
        
        // Obtener estadísticas de distribución
        SeqEstadisticasMesas obtenerEstadisticasDistribucion();
        
        // Limpiar distribución de un departamento
        bool limpiarDistribucionDepartamento(string departamento);
        
        // Verificar mesas disponibles
        SeqEstadisticasMesas obtenerMesasDisponibles();
        
        bool verificarConexion();
    };

    interface IMesaVotacion
    {
        // Recibir votantes asignados a esta mesa
        bool recibirVotantesAsignados(SeqCiudadanos votantes, string departamento);
        
        // NUEVO: Recibir archivo SQLite completo
        bool recibirArchivoSQLite(SeqBytes datosArchivo, string nombreArchivo);
        
        // NUEVO: Verificar si la mesa está lista para recibir datos
        bool estaListaParaRecibir();
        
        // Verificar si un votante pertenece a esta mesa
        bool verificarVotanteEnMesa(string documento);
        
        // Obtener información de un votante de la mesa
        CiudadanoInfo obtenerVotanteDeMesa(string documento);
        
        // Obtener estadísticas de la mesa
        EstadisticasMesa obtenerEstadisticasMesa();
        
        // Contar votantes en la mesa
        int contarVotantesEnMesa();
        
        // Limpiar datos de la mesa
        bool limpiarDatosMesa();
        
        // Verificar estado de la mesa
        bool verificarEstadoMesa();
        
        // Obtener ID de la mesa
        string obtenerIdMesa();
    };

    interface IRegistroMesas
    {
        // Registrar una nueva mesa en el sistema
        bool registrarMesa(string mesaId, string endpoint, string departamento, string municipio, string puesto);
        
        // Desregistrar una mesa
        bool desregistrarMesa(string mesaId);
        
        // Obtener endpoint de una mesa específica
        string obtenerEndpointMesa(string mesaId);
        
        // Obtener todas las mesas registradas de un departamento
        SeqMesas obtenerMesasPorDepartamento(string departamento);
        
        // Verificar si una mesa está registrada
        bool mesaEstaRegistrada(string mesaId);
        
        // Obtener información de todas las mesas
        SeqEstadisticasMesas obtenerTodasLasMesas();
        
        bool verificarConexion();
    };

    // ========== INTERFAZ CONSULTA INFORMACIÓN DE MESAS SQLite ==========
    
    struct VotanteMesa
    {
        int id;
        long ciudadanoId;
        string documento;
        string nombre;
        string apellido;
        string mesa;
        string mesaId;
        string puesto;
        string municipio;
        string departamento;
        string fechaAsignacion;
        int verificado;
        string fechaVerificacion;
    };

    sequence<VotanteMesa> SeqVotantesMesa;

    struct EstadisticasMesaSQLite
    {
        string mesaId;
        string departamento;
        string municipio;
        string puesto;
        int totalVotantes;
        int votantesVerificados;
        int mesaActiva;
        string fechaCreacion;
        long ultimaActualizacion;
    };

    struct LogVerificacion
    {
        int id;
        string documento;
        string accion;
        string resultado;
        string timestamp;
    };

    sequence<LogVerificacion> SeqLogsVerificacion;

    struct InfoCompletaMesa
    {
        EstadisticasMesaSQLite estadisticas;
        SeqVotantesMesa votantes;
        SeqLogsVerificacion logs;
        bool archivoExiste;
        string rutaArchivo;
    };

    interface IConsultaMesaSQLite
    {
        // Verificar si existe la base de datos de una mesa
        bool existeMesaSQLite(string mesaId);
        
        // Obtener información estadística de una mesa
        EstadisticasMesaSQLite obtenerEstadisticasMesa(string mesaId);
        
        // Obtener todos los votantes de una mesa
        SeqVotantesMesa obtenerVotantesDeMesa(string mesaId);
        
        // Obtener votantes con paginación
        SeqVotantesMesa obtenerVotantesPaginados(string mesaId, int pagina, int tamanoPagina);
        
        // Buscar votante por documento en una mesa
        VotanteMesa buscarVotantePorDocumento(string mesaId, string documento);
        
        // Obtener logs de verificación de una mesa
        SeqLogsVerificacion obtenerLogsVerificacion(string mesaId);
        
        // Obtener información completa de una mesa (estadísticas + votantes + logs)
        InfoCompletaMesa obtenerInfoCompletaMesa(string mesaId);
        
        // Contar votantes de una mesa
        int contarVotantesMesa(string mesaId);
        
        // Contar votantes verificados de una mesa
        int contarVotantesVerificados(string mesaId);
        
        // Listar todas las mesas SQLite disponibles
        SeqMesas listarMesasDisponibles();
        
        // Verificar conectividad del servicio
        bool verificarServicio();
    };
}