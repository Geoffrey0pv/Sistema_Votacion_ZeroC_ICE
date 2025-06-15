package Models;

/**
 * Modelo de datos para representar un candidato
 * Usado para la carga masiva desde CSV/Excel y almacenamiento en BD
 */
public class CandidatoModel {
    private Long id;
    private String nombre;
    private String partido;
    private boolean idGenerado; // Indica si el ID fue generado automáticamente
    
    public CandidatoModel() {
        this.idGenerado = false;
    }
    
    public CandidatoModel(Long id, String nombre, String partido) {
        this.id = id;
        this.nombre = nombre;
        this.partido = partido;
        this.idGenerado = false;
    }
    
    public CandidatoModel(Long id, String nombre, String partido, boolean idGenerado) {
        this.id = id;
        this.nombre = nombre;
        this.partido = partido;
        this.idGenerado = idGenerado;
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getPartido() {
        return partido;
    }
    
    public void setPartido(String partido) {
        this.partido = partido;
    }
    
    public boolean isIdGenerado() {
        return idGenerado;
    }
    
    public void setIdGenerado(boolean idGenerado) {
        this.idGenerado = idGenerado;
    }
    
    // Métodos de utilidad
    public boolean isValid() {
        return id != null && 
               nombre != null && !nombre.trim().isEmpty() && 
               partido != null && !partido.trim().isEmpty();
    }
    
    public String toCSVLine() {
        return id + "," + nombre + "," + partido;
    }
    
    @Override
    public String toString() {
        return String.format("CandidatoModel{id=%d, nombre='%s', partido='%s', idGenerado=%s}", 
                           id, nombre, partido, idGenerado);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CandidatoModel that = (CandidatoModel) obj;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
} 