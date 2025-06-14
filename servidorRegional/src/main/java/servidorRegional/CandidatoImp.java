package servidorRegional;

import Demo.Candidato;

public class CandidatoImp extends Candidato {
    private static long nextId = 1;

    public CandidatoImp(String nombre, String partido) {
        super(nextId++, nombre, partido);
    }

    public static CandidatoImp crearCandidatoPrueba() {
        String nombre = "Candidato_" + (nextId - 1);
        String partido = "Partido_" + (nextId % 5 + 1);
        return new CandidatoImp(nombre, partido);
    }

    @Override
    public String toString() {
        return "CandidatoImp{" +
                "idCandidato=" + idCandidato +
                ", nombre='" + nombre + '\'' +
                ", partido='" + partido + '\'' +
                '}';
    }
}
