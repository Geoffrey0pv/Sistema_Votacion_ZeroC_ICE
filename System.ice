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
        string puesto;
        string municipio;
        string departamento;
    };

    sequence<CiudadanoInfo> SeqCiudadanos;
    sequence<string> SeqDepartamentos;

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
};