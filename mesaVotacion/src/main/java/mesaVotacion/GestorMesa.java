package mesaVotacion;

import Demo.*;
import com.zeroc.Ice.*;

public class GestorMesa implements AutoCloseable {

    private final Communicator ic;
    private final IRegistrarVotoPrx votoPrx;
    private final IConfirmacionVotoPrx cbProxy;

    public GestorMesa() throws java.lang.Exception {    
        ic = Util.initialize(new String[]{}, "mesa.cfg");

        ObjectAdapter cbAdapter = ic.createObjectAdapterWithEndpoints(
                "AckAdapter", "tcp -h 0.0.0.0 -p 0");
        cbProxy = IConfirmacionVotoPrx.uncheckedCast(
                cbAdapter.add(new ConfirmacionVotoI(),
                              Util.stringToIdentity("cb")));
        cbAdapter.activate();

        votoPrx = IRegistrarVotoPrx.checkedCast(
                ic.stringToProxy("VoteCollector@RegionalAdapter"));
        if (votoPrx == null)
            throw new IllegalStateException("Proxy VoteCollector nulo");
    }

    public void emitirVoto(String idMesa, String hashElector, long idCand) {
        Voto v = new Voto(System.currentTimeMillis(), idMesa,
                          hashElector, idCand, System.currentTimeMillis());
        votoPrx.enviarVoto(v, cbProxy);
        System.out.println("Voto lanzado, esperando ACK…");
    }

    private static class ConfirmacionVotoI implements IConfirmacionVoto {
        public void recibirAck(Ack a, Current __) {
            if (a.registrado)
                System.out.println(" Voto " + a.idVoto + " registrado");
            else
                System.err.println(" Voto rechazado: " + a.mensaje);
        }
    }

    @Override public void close() { ic.destroy(); }
}
