package GestorVotos;

import Demo.*;
import com.zeroc.Ice.Current;

public class ConfirmacionVotoI implements IConfirmacionVoto {

    @Override
    public void recibirAck(Ack a, Current current) {
        System.out.println("=== CONFIRMACIÓN RECIBIDA ===");

        if (a.registrado) {
            System.out.println(" ÉXITO: Voto ID " + a.idVoto + " procesado correctamente");
            System.out.println(" Mensaje: " + a.mensaje);
        } else {
            System.out.println(" ERROR: Problema con voto ID " + a.idVoto);
            System.out.println(" Mensaje: " + a.mensaje);
        }

        System.out.println("============================");
        System.out.print("==> "); // Mostrar prompt nuevamente
        System.out.flush();
    }
}