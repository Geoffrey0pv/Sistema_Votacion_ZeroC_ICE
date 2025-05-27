package mesa;

import Demo.*;
import com.zeroc.Ice.*;

public class GestorMesa {

    public static void main(String[] args) {

        // Inicia Ice leyendo mesa.cfg (allí puedes poner Ice.RetryIntervals=-1)
        try (Communicator ic = Util.initialize(args, "mesa.cfg")) {

            /* 1. Adapter para el callback ---------------------------------- */
            ObjectAdapter cbAdapter = ic.createObjectAdapterWithEndpoints(
                    "AckAdapter",         // nombre lógico
                    "tcp -h 0.0.0.0 -p 0" // puerto dinámico
            );

            IConfirmacionVotoPrx cbProxy = IConfirmacionVotoPrx.uncheckedCast(
                    cbAdapter.add(
                            new ConfirmacionVotoI(),
                            Util.stringToIdentity("cb"))
            );
            cbAdapter.activate();

            /* 2. Proxy al servidor regional vía IceGrid -------------------- */
            IRegistrarVotoPrx votoPrx = IRegistrarVotoPrx.checkedCast(
                    ic.stringToProxy("VoteCollector@RegionalAdapter"));
            if (votoPrx == null) {
                throw new Error("Proxy nulo");
            }

            /* 3. Construir y enviar el voto -------------------------------- */
            Voto voto = new Voto(
                    System.currentTimeMillis(), // idVoto simplificado
                    "mesa-01",
                    "hashElector123",
                    42,
                    System.currentTimeMillis()
            );

            // --- Llamada oneway: la Mesa no se bloquea ---------------------
            votoPrx.enviarVoto(voto, cbProxy);

            System.out.println("Voto enviado; esperando ACK…");
            ic.waitForShutdown(); // se queda hasta recibir la confirmación
        }
    }

    /* ---------- Servant de callback ------------------------------------- */
    private static class ConfirmacionVotoI implements IConfirmacionVoto {
        @Override
        public void recibirAck(Ack a, Current __) {
            if (a.registrado) {
                System.out.println("Voto " + a.idVoto + " registrado");
            } else {
                System.err.println("Voto rechazado: " + a.mensaje);
            }
            // Cierra la Mesa después de recibir el ACK (demo)
            __.adapter.getCommunicator().shutdown();
        }
    }
}
