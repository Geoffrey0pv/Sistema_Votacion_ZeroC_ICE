package GestorVotos;

import Demo.Voto;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Random;
import java.util.Scanner;

public class VotoImp extends Voto {
    private static final Random random = new Random();

    public VotoImp(long idVoto, String idMesa, String idElectorHash, long idCandidato, long tsEmitido) {
        super(idVoto, idMesa, idElectorHash, idCandidato, tsEmitido);
    }
    public static VotoImp crearVotoPrueba() {
        long idVoto = System.currentTimeMillis() + random.nextInt(1000);
        String idMesa = "MESA_TEST_" + random.nextInt(100);
        String idElectorHash = generarHash("ELECTOR_" + random.nextInt(10000));
        long idCandidato = random.nextInt(5) + 1; // Candidatos del 1 al 5
        long tsEmitido = Instant.now().getEpochSecond();

        return new VotoImp(idVoto, idMesa, idElectorHash, idCandidato, tsEmitido);
    }
    private static String generarHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "HASH_" + Math.abs(input.hashCode());
        }
    }
    public boolean esValido() {
        return idVoto > 0 &&
                idMesa != null && !idMesa.trim().isEmpty() &&
                idElectorHash != null && !idElectorHash.trim().isEmpty() &&
                idCandidato > 0 &&
                tsEmitido > 0;
    }
    @Override
    public String toString() {
        return "VotoImp{" +
                "idVoto=" + idVoto +
                ", idMesa='" + idMesa + '\'' +
                ", idElectorHash='" + idElectorHash + '\'' +
                ", idCandidato=" + idCandidato +
                ", tsEmitido=" + tsEmitido +
                " (" + new java.util.Date(tsEmitido * 1000) + ")" +
                '}';
    }
}