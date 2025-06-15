public class MesaInfo {
    public String departamento;
    public String municipio;
    public String puesto;
    public String mesa;
    
    // Constructor vacío
    public MesaInfo() {
        this.departamento = "";
        this.municipio = "";
        this.puesto = "";
        this.mesa = "";
    }
    
    // Constructor con parámetros
    public MesaInfo(String departamento, String municipio, String puesto, String mesa) {
        this.departamento = departamento != null ? departamento : "";
        this.municipio = municipio != null ? municipio : "";
        this.puesto = puesto != null ? puesto : "";
        this.mesa = mesa != null ? mesa : "";
    }
    
    // Getters y Setters
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { 
        this.departamento = departamento != null ? departamento : ""; 
    }
    
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { 
        this.municipio = municipio != null ? municipio : ""; 
    }
    
    public String getPuesto() { return puesto; }
    public void setPuesto(String puesto) { 
        this.puesto = puesto != null ? puesto : ""; 
    }
    
    public String getMesa() { return mesa; }
    public void setMesa(String mesa) { 
        this.mesa = mesa != null ? mesa : ""; 
    }
    
    @Override
    public String toString() {
        return "MesaInfo{" +
                "departamento='" + departamento + '\'' +
                ", municipio='" + municipio + '\'' +
                ", puesto='" + puesto + '\'' +
                ", mesa='" + mesa + '\'' +
                '}';
    }
} 