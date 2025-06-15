package ReliableMessageManager;
import java.time.LocalDateTime;
import java.util.UUID;
import GestorVotos.VotoImp;

public class MensajePendiente {
    private String id;
    private VotoImp voto;
    private LocalDateTime timestamp;
    private int intentos;
    private int maxIntentos;
    private String estado; // PENDIENTE, ENVIADO, FALLIDO
    
    public MensajePendiente(VotoImp voto) {
        this.id = UUID.randomUUID().toString();
        this.voto = voto;
        this.timestamp = LocalDateTime.now();
        this.intentos = 0;
        this.maxIntentos = 5;
        this.estado = "PENDIENTE";
    }
    
    // Getters y setters
    public String getId() { return id; }
    public VotoImp getVoto() { return voto; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getIntentos() { return intentos; }
    public int getMaxIntentos() { return maxIntentos; }
    public String getEstado() { return estado; }
    
    public void incrementarIntentos() { this.intentos++; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public boolean puedeReintentar() {
        return intentos < maxIntentos && "PENDIENTE".equals(estado);
    }
}