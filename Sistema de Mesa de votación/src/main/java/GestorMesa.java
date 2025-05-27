package com.zeroc.Ice.examples.votacion;

import Demo.*;
import com.zeroc.Ice.*;

public class GestorMesa
{
    public static void main(String[] args)
    {
        try (Communicator ic = Util.initialize(args, "mesa.cfg")) {

            /* 1. Adapter para el callback ---------------------------------- */
            ObjectAdapter cbAdapter = ic.createObjectAdapterWithEndpoints(
                    "AckAdapter", "tcp -h 0.0.0.0 -p 0");
            IConfirmacionVotoPrx cbProxy = IConfirmacionVotoPrx.uncheckedCast(
                    cbAdapter.add(new ConfirmacionVotoI(),
                                  Util.stringToIdentity("cb")));
            cbAdapter.activate();

            /* 2. Proxy al servidor regional via IceGrid --------------------- */
            IRegistrarVotoPrx votoPrx = IRegistrarVotoPrx.checkedCast(
                    ic.stringToProxy("VoteCollector@RegionalAdapter"));
            if (votoPrx == null)
                throw new Error("Proxy nulo");

            /* 3. Construir y enviar el voto --------------------------------- */
            Voto voto = new Voto(
                    System.currentTimeMillis(), "mesa-01", "hashElector123",
                    42, System.currentTimeMillis());

            votoPrx = votoPrx.ice_retryIntervals(-1);   // reintentos infinitos
            votoPrx.enviarVoto(voto, cbProxy);

            System.out.println("Voto enviado; esperando ACK…");
            ic.waitForShutdown();
        }
    }

    /* ---------- Servant de callback ---------- */
    private static class ConfirmacionVotoI implements IConfirmacionVoto
    {
        @Override
        public void recibirAck(Ack a, Current __)
        {
            if (a.registrado)
                System.out.println("✅  Voto " + a.idVoto + " registrado");
            else
                System.err.println("❌  Voto rechazado: " + a.mensaje);

            __.adapter.getCommunicator().shutdown();
        }
    }
}
