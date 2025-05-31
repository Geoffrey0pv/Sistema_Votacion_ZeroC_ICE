package servidorRegional;
import Demo.Ack;

public class AckImp extends Ack {
    public AckImp(long idVoto, boolean registrado, String mensaje) {
        super(idVoto, registrado, mensaje);
    }

    @Override
    public String toString() {
        return "Ack{" +
                "idVoto=" + idVoto +
                ", registrado=" + registrado +
                ", mensaje='" + mensaje + '\'' +
                '}';
    }
    public boolean isExito() {
        return registrado;
    }
    public long getIdVoto() {
        return idVoto;
    }
    public String getMensaje() {
        return mensaje;
    }
    public void setIdVoto(long idVoto) {
    this.idVoto = idVoto;
    }

    public void setRegistrado(boolean registrado) {
        this.registrado = registrado;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
