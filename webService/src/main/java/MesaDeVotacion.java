public class MesaDeVotacion {
    public String numeroMesa;
    public String direccion;
    public String ciudad;
    public String departamento;
    public String puesto;
    
    // Constructor vacío
    public MesaDeVotacion() {
    }
    
    // Constructor con parámetros
    public MesaDeVotacion(String numeroMesa, String direccion, String ciudad, 
                         String departamento, String puesto) {
        this.numeroMesa = numeroMesa;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.departamento = departamento;
        this.puesto = puesto;
    }
    
    // Getters y Setters
    public String getNumeroMesa() { return numeroMesa; }
    public void setNumeroMesa(String numeroMesa) { this.numeroMesa = numeroMesa; }
    
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    
    public String getPuesto() { return puesto; }
    public void setPuesto(String puesto) { this.puesto = puesto; }
    
    @Override
    public String toString() {
        return "MesaDeVotacion{" +
                "numeroMesa='" + numeroMesa + '\'' +
                ", direccion='" + direccion + '\'' +
                ", ciudad='" + ciudad + '\'' +
                ", departamento='" + departamento + '\'' +
                ", puesto='" + puesto + '\'' +
                '}';
    }
} 