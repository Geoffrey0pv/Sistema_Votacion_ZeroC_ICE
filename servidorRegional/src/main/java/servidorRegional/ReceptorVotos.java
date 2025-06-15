// ReceptorVotos.java (actualizado para trabajar con GestionCandidatos)
package servidorRegional;

import Demo.*;
import com.zeroc.Ice.*;

import java.lang.Exception;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ReceptorVotos implements IRegistrarVoto {
    private final String nombreRegion;
    private final ConcurrentHashMap<Long, Voto> votosRecibidos;
    private final AtomicLong contadorVotos;

    public ReceptorVotos(String nombreRegion) {
        this.nombreRegion = nombreRegion != null ? nombreRegion : "RegionDesconocida";
        this.votosRecibidos = new ConcurrentHashMap<>();
        this.contadorVotos = new AtomicLong(0);
    }

    @Override
    public void enviarVoto(Voto voto, IConfirmacionVotoPrx callback, Current current) {
        try {
            System.out.println("Recibiendo voto en región " + nombreRegion +
                    " - ID: " + voto.idVoto +
                    " - Mesa: " + voto.idMesa +
                    " - Candidato: " + voto.idCandidato);

            // Validar voto
            if (validarVoto(voto)) {
                // Almacenar voto
                votosRecibidos.put(voto.idVoto, voto);
                contadorVotos.incrementAndGet();

                // Crear acknowledgment exitoso
                Ack ack = new Ack();
                ack.idVoto = voto.idVoto;
                ack.registrado = true;
                ack.mensaje = "Voto registrado exitosamente en región " + nombreRegion;

                // Enviar confirmación
                if (callback != null) {
                    callback.recibirAck(ack);
                }

                System.out.println("Voto " + voto.idVoto + " procesado exitosamente");

            } else {
                // Crear acknowledgment de error
                Ack ack = new Ack();
                ack.idVoto = voto.idVoto;
                ack.registrado = false;
                ack.mensaje = "Voto inválido - no se pudo registrar";

                if (callback != null) {
                    callback.recibirAck(ack);
                }

                System.err.println("Voto " + voto.idVoto + " rechazado por validación");
            }

        } catch (Exception e) {
            System.err.println("Error procesando voto " + voto.idVoto + ": " + e.getMessage());

            // Enviar acknowledgment de error
            try {
                if (callback != null) {
                    Ack ack = new Ack();
                    ack.idVoto = voto.idVoto;
                    ack.registrado = false;
                    ack.mensaje = "Error interno del servidor: " + e.getMessage();
                    callback.recibirAck(ack);
                }
            } catch (Exception callbackException) {
                System.err.println("Error enviando callback: " + callbackException.getMessage());
            }
        }
    }

    private boolean validarVoto(Voto voto) {
        // Validación básica
        if (voto.idVoto <= 0) {
            System.err.println("ID de voto inválido: " + voto.idVoto);
            return false;
        }

        if (voto.idMesa == null || voto.idMesa.trim().isEmpty()) {
            System.err.println("ID de mesa inválido");
            return false;
        }

        if (voto.idElectorHash == null || voto.idElectorHash.trim().isEmpty()) {
            System.err.println("Hash de elector inválido");
            return false;
        }

        if (voto.idCandidato <= 0) {
            System.err.println("ID de candidato inválido: " + voto.idCandidato);
            return false;
        }

        if (voto.tsEmitido <= 0) {
            System.err.println("Timestamp inválido: " + voto.tsEmitido);
            return false;
        }

        // Verificar si el voto ya existe
        if (votosRecibidos.containsKey(voto.idVoto)) {
            System.err.println("Voto duplicado con ID: " + voto.idVoto);
            return false;
        }

        return true;
    }

    public long obtenerTotalVotos() {
        return contadorVotos.get();
    }

    public String obtenerNombreRegion() {
        return nombreRegion;
    }
}