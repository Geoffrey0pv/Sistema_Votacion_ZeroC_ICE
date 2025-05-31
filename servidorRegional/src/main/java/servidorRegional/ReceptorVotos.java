package servidorRegional;

import Demo.*;
import com.zeroc.Ice.Current;
import servidorRegional.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReceptorVotos implements IRegistrarVoto
{
    private final Set<Long> recibidos = ConcurrentHashMap.newKeySet();

    public ReceptorVotos(String nombre)
    {
        System.out.printf("Servidor Regional '%s' iniciado\n", nombre);
    }

    @Override
    public void enviarVoto(Voto v, IConfirmacionVotoPrx callback, Current current) {
        boolean nuevo = recibidos.add(v.idVoto);
        AckImp ack;

        if (nuevo) {
           
            System.out.printf(" Voto registrado: ID=%d, Mesa=%s, Candidato=%d\n", v.idVoto, v.idMesa, v.idCandidato);
            ack = new AckImp(v.idVoto, true, "Voto registrado correctamente");
        } else {
            System.out.printf(" Voto duplicado: ID=%d, Mesa=%s\n", v.idVoto, v.idMesa);
            ack = new AckImp(v.idVoto, false, "Voto duplicado: ya se había contado anteriormente");
        }

        // Enviar ACK usando callback asíncrono
        if (callback != null) {
            try {
                System.out.println(" Enviando ACK vía callback...");
                System.out.println(" Callback proxy: " + callback.toString());
                
                // Llamada asíncrona al callback
                callback.recibirAckAsync(ack).whenComplete((result, exception) -> {
                    if (exception != null) {
                        System.err.println(" Error enviando ACK: " + exception.getMessage());
                        exception.printStackTrace();
                    } else {
                        System.out.println(" ACK enviado exitosamente al callback");
                    }
                });
                
            } catch (Exception ex) {
                System.err.println(" Excepción enviando ACK: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            System.out.println("ℹ  No hay callback - procesado en modo síncrono");
        }
    }
    
    // Esta método hay que implementarlo XD
    @Override
    public void enviarCandidatos(Candidato[] candidatos, IConfirmacionCandidatosPrx cb, Current __) {}
}
