import Demo.IRegistrarVotoPrx;
import servidorRegional.ReceptorVotos;

import com.zeroc.Ice.*;

public class ServidorRegional
{
    public static void main(String[] args)
    {
        // 1. Arranca el Communicator leyendo regional.cfg quí debe estar Ice.Default.Locator cuando uses IceGrid
        try (Communicator ic = Util.initialize(args, "regional.cfg")) {

            /* 2. Obtiene el ObjectAdapter
                  • Si el proceso lo arranca icegridnode, el adapter está en application.xml;
                  • Si corres a pelo, lo creamos con endpoints fijos. */
            ObjectAdapter adapter;

            try {
                // modo IceGrid ─ el endpoint lo marca application.xml
                adapter = ic.createObjectAdapter("RegionalAdapter");
            } catch (InitializationException ex) {
                // modo local ─ endpoint explícito
                adapter = ic.createObjectAdapterWithEndpoints("RegionalAdapter", "tcp -h 0.0.0.0 -p 10000");
            }

            // 3. Registra el servant y publica el proxy indirecto VoteCollector
            ReceptorVotos servant = new ReceptorVotos();
            IRegistrarVotoPrx prx = IRegistrarVotoPrx.uncheckedCast(adapter.add(servant, Util.stringToIdentity("VoteCollector")));

            System.out.println("RegionalServer listo en " + prx);   // log de utilidad

            adapter.activate();
            ic.waitForShutdown();
        }
    }
}
