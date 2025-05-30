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

         void enviarCandidatos(SeqCandidatos  candidatos, IConfirmacionCandidatos* callback);
    };
    interface ICargarCandidatos
    {
        void cargarCandidatos(IConfirmacionCandidatos* callback);
    };
};
