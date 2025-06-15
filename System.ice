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
};