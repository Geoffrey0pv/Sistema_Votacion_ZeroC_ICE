package GestorMesa;

/**
 * Clase para representar un voto registrado en la base de datos SQLite local
 */
public class VotoRegistro {
    public long id;
    public String mesaId;
    public long timestamp;
    public long candidatoId;
    public String hashVerificacion;
    public String municipio;
    public String departamento;
    
    public VotoRegistro() {}
    
    public VotoRegistro(long id, String mesaId, long timestamp, long candidatoId, 
                       String hashVerificacion, String municipio, String departamento) {
        this.id = id;
        this.mesaId = mesaId;
        this.timestamp = timestamp;
        this.candidatoId = candidatoId;
        this.hashVerificacion = hashVerificacion;
        this.municipio = municipio;
        this.departamento = departamento;
    }
    
    @Override
    public String toString() {
        return String.format("VotoRegistro{id=%d, mesa='%s', candidato=%d, municipio='%s', departamento='%s', timestamp=%d}",
                id, mesaId, candidatoId, municipio, departamento, timestamp);
    }
} 