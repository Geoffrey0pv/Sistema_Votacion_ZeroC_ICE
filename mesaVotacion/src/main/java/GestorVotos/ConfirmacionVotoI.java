package GestorVotos;

import Demo.Ack;
import Demo.IConfirmacionVoto;
import com.zeroc.Ice.Current;

public class ConfirmacionVotoI implements IConfirmacionVoto {

    @Override
    public void recibirAck(Ack ack, Current current) {
        System.out.println("\n=== CONFIRMACIÓN DE VOTO RECIBIDA ===");
        System.out.println("ID Voto: " + ack.idVoto);
        System.out.println("Registrado: " + (ack.registrado ? "SÍ" : "NO"));
        System.out.println("Mensaje: " + ack.mensaje);
        System.out.println("=====================================\n");

        if (ack.registrado) {
            System.out.println("¡Tu voto ha sido registrado exitosamente!");
        } else {
            System.out.println("Error al registrar el voto: " + ack.mensaje);
        }
    }
}