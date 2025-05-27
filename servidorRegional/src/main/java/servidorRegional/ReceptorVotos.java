package servidorRegional;

import Demo.*;
import com.zeroc.Ice.Current;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReceptorVotos implements IRegistrarVoto
{
    private final Set<Long> recibidos = ConcurrentHashMap.newKeySet();

    @Override
    public void enviarVoto(Voto v, IConfirmacionVotoPrx callback, Current __)
    {
        boolean nuevo = recibidos.add(v.idVoto);
        Ack ack;

        if (nuevo) {
            System.out.printf(" Voto registrado", v.idVoto, v.idMesa, v.idCandidato);
            ack = new Ack(v.idVoto, true, "Voto registrado correctamente");
        } else {
            System.out.printf("Voto duplicado", v.idVoto, v.idMesa);
            ack = new Ack(v.idVoto, false, "Voto duplicado: ya se había contado anteriormente");
        }

        /* Devolver confirmación */
        try {
            callback.recibirAck(ack);
        } catch (Exception ex) {
            System.err.println("No se pudo devolver ACK a la mesa: " + ex);
        }
    }
    /* ——— aún no usamos enviarCandidatos; déjalo vacío de momento ——— */
    @Override
    public void enviarCandidatos(Candidato[] candidatos, IConfirmacionCandidatosPrx cb, Current __) {}
}
