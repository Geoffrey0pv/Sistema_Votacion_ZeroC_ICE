package Models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo para representar un voto en el sistema
 */
public class VotoModel {
    private Long id;
    private String mesaId;
    private String candidatoId;
    private LocalDateTime timestamp;
    private String municipio;
    private String departamento;
    private String hashVerificacion;
    private String firmaMesa;
    private LocalDateTime fechaRecepcion;
    
    // Constructores
    public VotoModel() {
        this.fechaRecepcion = LocalDateTime.now();
    }
    
    public VotoModel(String mesaId, String candidatoId, LocalDateTime timestamp, 
                     String municipio, String departamento, String hashVerificacion, String firmaMesa) {
        this();
        this.mesaId = mesaId;
        this.candidatoId = candidatoId;
        this.timestamp = timestamp;
        this.municipio = municipio;
        this.departamento = departamento;
        this.hashVerificacion = hashVerificacion;
        this.firmaMesa = firmaMesa;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMesaId() {
        return mesaId;
    }
    
    public void setMesaId(String mesaId) {
        this.mesaId = mesaId;
    }
    
    public String getCandidatoId() {
        return candidatoId;
    }
    
    public void setCandidatoId(String candidatoId) {
        this.candidatoId = candidatoId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMunicipio() {
        return municipio;
    }
    
    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }
    
    public String getDepartamento() {
        return departamento;
    }
    
    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }
    
    public String getHashVerificacion() {
        return hashVerificacion;
    }
    
    public void setHashVerificacion(String hashVerificacion) {
        this.hashVerificacion = hashVerificacion;
    }
    
    public String getFirmaMesa() {
        return firmaMesa;
    }
    
    public void setFirmaMesa(String firmaMesa) {
        this.firmaMesa = firmaMesa;
    }
    
    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }
    
    public void setFechaRecepcion(LocalDateTime fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }
    
    // Métodos de utilidad
    public boolean isValid() {
        return mesaId != null && !mesaId.trim().isEmpty() &&
               candidatoId != null && !candidatoId.trim().isEmpty() &&
               timestamp != null &&
               municipio != null && !municipio.trim().isEmpty() &&
               departamento != null && !departamento.trim().isEmpty() &&
               hashVerificacion != null && !hashVerificacion.trim().isEmpty() &&
               firmaMesa != null && !firmaMesa.trim().isEmpty();
    }
    
    public String toJson() {
        return String.format(
            "{\"id\":%d,\"mesa_id\":\"%s\",\"candidato_id\":\"%s\",\"timestamp\":\"%s\"," +
            "\"municipio\":\"%s\",\"departamento\":\"%s\",\"hash_verificacion\":\"%s\"," +
            "\"firma_mesa\":\"%s\",\"fecha_recepcion\":\"%s\"}",
            id != null ? id : 0, mesaId, candidatoId, 
            timestamp != null ? timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "",
            municipio, departamento, hashVerificacion, firmaMesa,
            fechaRecepcion != null ? fechaRecepcion.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : ""
        );
    }
    
    @Override
    public String toString() {
        return String.format("VotoModel{id=%d, mesa='%s', candidato='%s', municipio='%s'}", 
                           id != null ? id : 0, mesaId, candidatoId, municipio);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        VotoModel voto = (VotoModel) obj;
        return mesaId.equals(voto.mesaId) && 
               candidatoId.equals(voto.candidatoId) &&
               timestamp.equals(voto.timestamp) &&
               hashVerificacion.equals(voto.hashVerificacion);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(mesaId, candidatoId, timestamp, hashVerificacion);
    }
} 