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
};