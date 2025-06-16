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
    private final com.zeroc.Ice.Communicator communicator;
    private IRegistroVotosPrx registroVotosProxy;

    public ReceptorVotos(String nombreRegion, com.zeroc.Ice.Communicator communicator) {
        this.nombreRegion = nombreRegion != null ? nombreRegion : "RegionDesconocida";
        this.votosRecibidos = new ConcurrentHashMap<>();
        this.contadorVotos = new AtomicLong(0);
        this.communicator = communicator;
        conectarAServidorNacional();
    }

    private void conectarAServidorNacional() {
        try {
            System.out.println("Intentando conectar con el Servidor Nacional para registro de votos...");
            // Se asume que el servidor nacional expone el servicio con esta identidad.
            // Si falla, verificar la configuración en ServidorNacional.java.
            String proxyString = "registroVotos@ServidorNacionalAdapter";
            com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy(proxyString);
            this.registroVotosProxy = IRegistroVotosPrx.checkedCast(base);

            if (this.registroVotosProxy == null) {
                System.err.println("❌ Error: El proxy para IRegistroVotos es inválido. No se podrán enviar votos al servidor nacional.");
            } else {
                // Hacemos un ping para verificar la conexión de inmediato
                this.registroVotosProxy.ice_ping();
                System.out.println("✅ Conexión con Servidor Nacional (RegistroVotos) establecida correctamente.");
            }
        } catch (com.zeroc.Ice.Exception e) {
            System.err.println("❌ Error crítico al conectar con el Servidor Nacional: " + e.getMessage());
            System.err.println("   Los votos serán almacenados localmente pero no se enviarán al nacional hasta que se reinicie el servicio.");
            this.registroVotosProxy = null;
        }
    }

    @Override
    public void enviarVoto(Voto voto, IConfirmacionVotoPrx callback, Current current) {
        try {
            System.out.println("\n" + "=".repeat(55));
            System.out.println("==        VOTO RECIBIDO EN SERVIDOR REGIONAL        ==");
            System.out.println("=".repeat(55));
            System.out.println("  Región:       " + this.nombreRegion);
            System.out.println("  Mesa Origen:  " + voto.idMesa);
            System.out.println("  ID Voto:      " + voto.idVoto);
            System.out.println("  ID Candidato: " + voto.idCandidato);
            System.out.println("  Hash Elector: " + voto.idElectorHash);
            System.out.println("  Emitido:      " + new java.util.Date(voto.tsEmitido));
            System.out.println("-".repeat(55));

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

                System.out.println("  RESULTADO: Voto " + voto.idVoto + " ACEPTADO y almacenado localmente.");

                // Reenviar al servidor nacional
                enviarVotoANacional(voto);

            } else {
                // Crear acknowledgment de error
                Ack ack = new Ack();
                ack.idVoto = voto.idVoto;
                ack.registrado = false;
                ack.mensaje = "Voto inválido - no se pudo registrar. Verifique los logs del servidor regional.";

                if (callback != null) {
                    callback.recibirAck(ack);
                }

                System.out.println("  RESULTADO: Voto " + voto.idVoto + " RECHAZADO.");
                System.out.println("  (Consulte los mensajes de error de validación para más detalles)");
            }
            System.out.println("=".repeat(55) + "\n");

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

    private void enviarVotoANacional(Voto voto) {
        if (registroVotosProxy == null) {
            System.err.println("⚠️  No se puede enviar voto " + voto.idVoto + " al Servidor Nacional, el proxy no está disponible.");
            return;
        }

        try {
            System.out.println("  -> Reenviando voto " + voto.idVoto + " al Servidor Nacional...");

            VotoCompleto votoCompleto = new VotoCompleto();
            votoCompleto.id = voto.idVoto;
            votoCompleto.mesaId = voto.idMesa;
            votoCompleto.timestamp = voto.tsEmitido;
            votoCompleto.candidatoId = voto.idCandidato;
            votoCompleto.hashVerificacion = voto.idElectorHash;
            // Se usa el nombre de la región para municipio y departamento
            votoCompleto.municipio = this.nombreRegion;
            votoCompleto.departamento = this.nombreRegion;

            // Llamada asíncrona para no bloquear el servidor regional
            registroVotosProxy.registrarVotoAsync(votoCompleto)
                .whenComplete((registrado, ex) -> {
                    if (ex != null) {
                        System.err.println("❌ Error en la comunicación asíncrona al enviar voto " + voto.idVoto + ": " + ex.getMessage());
                    } else {
                        if (registrado) {
                            System.out.println("  -> ✅ Voto " + voto.idVoto + " registrado exitosamente por el Servidor Nacional.");
                        } else {
                            System.err.println("  -> ❌ El Servidor Nacional RECHAZÓ el voto " + voto.idVoto + ". Revisar logs del nacional.");
                        }
                    }
                });

        } catch (Exception e) {
             System.err.println("❌ Excepción inesperada al intentar enviar voto " + voto.idVoto + " al Nacional: " + e.getMessage());
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