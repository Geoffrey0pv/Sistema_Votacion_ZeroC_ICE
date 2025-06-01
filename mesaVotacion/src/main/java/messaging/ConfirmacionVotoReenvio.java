package messaging;

import com.zeroc.Ice.Current;

import Demo.Ack;
import Demo.IConfirmacionVotoPrx;
import GestorVotos.ConfirmacionVotoI;

public class ConfirmacionVotoReenvio extends ConfirmacionVotoI {
    private final String mensajeId;
    
    public ConfirmacionVotoReenvio(String mensajeId) {
        this.mensajeId = mensajeId;
    }
    
    @Override
    public void recibirAck(Ack a, Current current) {
        System.out.println("=== CONFIRMACIÓN DE REENVÍO RECIBIDA ===");
        
        if (a.registrado) {
            System.out.println(" ÉXITO: Voto ID " + a.idVoto + " reenviado correctamente");
            System.out.println(" Mensaje: " + a.mensaje);
        } else {
            System.out.println(" ERROR: Problema reenviando voto ID " + a.idVoto);
            System.out.println(" Mensaje: " + a.mensaje);
        }
        
        System.out.println("============================");
        System.out.print("==> "); // Mostrar prompt nuevamente
        System.out.flush();
    }
}
