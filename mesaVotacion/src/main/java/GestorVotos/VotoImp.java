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

    // Método para crear un voto completo pidiendo datos al usuario
    public static VotoImp crearVotoInteractivo() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== CREAR NUEVO VOTO ===");

        // Generar ID único para el voto
        long idVoto = System.currentTimeMillis() + random.nextInt(1000);

        // Solicitar ID de mesa
        System.out.print("Ingrese ID de Mesa: ");
        String idMesa = scanner.nextLine().trim();
        if (idMesa.isEmpty()) {
            idMesa = "MESA_001"; // Valor por defecto
        }

        // Solicitar identificación del elector (se convertirá a hash)
        System.out.print("Ingrese su número de identificación: ");
        String identificacion = scanner.nextLine().trim();
        String idElectorHash = generarHash(identificacion);

        // Solicitar ID del candidato
        System.out.print("Ingrese ID del Candidato: ");
        long idCandidato = 0;
        try {
            idCandidato = Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido, usando candidato por defecto (1)");
            idCandidato = 1;
        }

        // Timestamp actual
        long tsEmitido = Instant.now().getEpochSecond();

        VotoImp voto = new VotoImp(idVoto, idMesa, idElectorHash, idCandidato, tsEmitido);

        System.out.println("\n=== VOTO CREADO ===");
        System.out.println(voto.toString());
        System.out.println("==================\n");

        return voto;
    }

    // Método para crear un voto de prueba
    public static VotoImp crearVotoPrueba() {
        long idVoto = System.currentTimeMillis() + random.nextInt(1000);
        String idMesa = "MESA_TEST_" + random.nextInt(100);
        String idElectorHash = generarHash("ELECTOR_" + random.nextInt(10000));
        long idCandidato = random.nextInt(5) + 1; // Candidatos del 1 al 5
        long tsEmitido = Instant.now().getEpochSecond();

        return new VotoImp(idVoto, idMesa, idElectorHash, idCandidato, tsEmitido);
    }

    // Generar hash de la identificación del elector
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
            return hexString.toString().substring(0, 16); // Solo primeros 16 caracteres
        } catch (NoSuchAlgorithmException e) {
            // Fallback simple si SHA-256 no está disponible
            return "HASH_" + Math.abs(input.hashCode());
        }
    }

    // Validar que el voto tenga todos los campos requeridos
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