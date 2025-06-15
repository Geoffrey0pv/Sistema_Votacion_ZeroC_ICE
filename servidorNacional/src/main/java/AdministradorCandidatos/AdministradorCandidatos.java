package AdministradorCandidatos;

import Demo.*;
import com.zeroc.Ice.*;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AdministradorCandidatos implements IAdministradorCandidatos {

    private final List<Candidato> candidatos;
    private final ReadWriteLock lock;
    private volatile boolean datosActualizados;
    private final List<String> endpointsRegionales;
    private final Communicator communicator;

    public AdministradorCandidatos(Communicator communicator) {
        this.communicator = communicator;
        this.candidatos = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.datosActualizados = false;
        this.endpointsRegionales = new ArrayList<>();

        cargarEndpointsRegionales();

        System.out.println("Administrador de Candidatos Nacional inicializado");
    }

    @Override
    public boolean cargarCandidatosDesdeCSV(String rutaArchivo, Current current) {
        lock.writeLock().lock();
        try {
            System.out.println("Cargando candidatos desde: " + rutaArchivo);

            List<Candidato> nuevosCandidatos = new ArrayList<>();

            try (CSVReader reader = new CSVReader(new FileReader(rutaArchivo))) {
                List<String[]> filas = reader.readAll();

                if (filas.isEmpty()) {
                    System.err.println("El archivo CSV está vacío");
                    return false;
                }

                String[] encabezados = filas.get(0);
                if (!validarEncabezados(encabezados)) {
                    System.err.println("Encabezados del CSV no válidos. Se esperaba: idCandidato,nombre,partido");
                    return false;
                }

                for (int i = 1; i < filas.size(); i++) {
                    String[] fila = filas.get(i);

                    if (fila.length < 3) {
                        System.err.println("Fila " + (i + 1) + " incompleta, omitiendo...");
                        continue;
                    }

                    try {
                        long idCandidato = Long.parseLong(fila[0].trim());
                        String nombre = fila[1].trim();
                        String partido = fila[2].trim();

                        if (nombre.isEmpty() || partido.isEmpty()) {
                            System.err.println("Fila " + (i + 1) + " tiene campos vacíos, omitiendo...");
                            continue;
                        }

                        Candidato candidato = new Candidato(idCandidato, nombre, partido);
                        nuevosCandidatos.add(candidato);

                    } catch (NumberFormatException e) {
                        System.err.println("Error en fila " + (i + 1) + ": ID no válido - " + fila[0]);
                    }
                }

                if (nuevosCandidatos.isEmpty()) {
                    System.err.println("No se pudo cargar ningún candidato válido");
                    return false;
                }
                candidatos.clear();
                candidatos.addAll(nuevosCandidatos);
                datosActualizados = true;

                System.out.println("Candidatos cargados exitosamente: " + candidatos.size());
                mostrarCandidatosCargados();

                return true;

            } catch (IOException | CsvException e) {
                System.err.println("Error leyendo archivo CSV: " + e.getMessage());
                return false;
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean cargarCandidatosDesdeArray(Candidato[] arraysCandidatos, Current current) {
        lock.writeLock().lock();
        try {
            System.out.println("Cargando candidatos desde array: " + arraysCandidatos.length + " elementos");

            candidatos.clear();

            for (Candidato candidato : arraysCandidatos) {
                if (candidato != null && validarCandidato(candidato)) {
                    candidatos.add(candidato);
                } else {
                    System.err.println("Candidato inválido omitido: " + candidato);
                }
            }

            datosActualizados = true;

            System.out.println("Candidatos cargados desde array: " + candidatos.size());
            mostrarCandidatosCargados();

            return !candidatos.isEmpty();

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int obtenerCantidadCandidatos(Current current) {
        lock.readLock().lock();
        try {
            return candidatos.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Candidato[] obtenerTodosCandidatos(Current current) {
        lock.readLock().lock();
        try {
            return candidatos.toArray(new Candidato[0]);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean limpiarCandidatos(Current current) {
        lock.writeLock().lock();
        try {
            candidatos.clear();
            datosActualizados = false;
            System.out.println("Lista de candidatos limpiada");
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean enviarCandidatosARegional(String endpointRegional, Current current) {
        lock.readLock().lock();
        try {
            if (candidatos.isEmpty()) {
                System.err.println("No hay candidatos para enviar");
                return false;
            }

            try {
                // Crear proxy para el servidor regional
                ObjectPrx base = communicator.stringToProxy(endpointRegional);
                IRecibirCandidatosPrx regional = IRecibirCandidatosPrx.checkedCast(base);

                if (regional == null) {
                    System.err.println("Proxy inválido para servidor regional: " + endpointRegional);
                    return false;
                }

                // Crear callback para recibir confirmación
                IConfirmacionCandidatosPrx callback = new _IConfirmacionCandidatosPrxI();

                Candidato[] arrayCandidatos = candidatos.toArray(new Candidato[0]);
                regional.recibirCandidatos(arrayCandidatos, callback);

                System.out.println("Candidatos enviados a servidor regional: " + endpointRegional);
                return true;

            } catch (Exception e) {
                System.err.println("Error enviando candidatos a regional " + endpointRegional + ": " + e.getMessage());
                return false;
            }

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean enviarCandidatosATodosRegionales(Current current) {
        boolean todosExitosos = true;

        for (String endpoint : endpointsRegionales) {
            boolean resultado = enviarCandidatosARegional(endpoint, current);
            if (!resultado) {
                todosExitosos = false;
                System.err.println("Fallo enviando a: " + endpoint);
            }
        }

        if (todosExitosos) {
            System.out.println("Candidatos enviados exitosamente a todos los servidores regionales");
        } else {
            System.err.println("Algunos servidores regionales no recibieron los candidatos");
        }

        return todosExitosos;
    }

    private void cargarEndpointsRegionales() {
        // Cargar desde propiedades de Ice o archivo de configuración
        Properties props = communicator.getProperties();

        // Ejemplo de endpoints regionales
        endpointsRegionales.add("IRecibirCandidatos:tcp -h localhost -p 10000");
        endpointsRegionales.add("IRecibirCandidatos:tcp -h localhost -p 10001");
        // Agregar más endpoints según configuración

        System.out.println("Endpoints regionales cargados: " + endpointsRegionales.size());
    }

    private boolean validarEncabezados(String[] encabezados) {
        if (encabezados.length < 3) return false;

        String col1 = encabezados[0].trim().toLowerCase();
        String col2 = encabezados[1].trim().toLowerCase();
        String col3 = encabezados[2].trim().toLowerCase();

        return (col1.equals("idcandidato") || col1.equals("id_candidato") || col1.equals("id")) &&
                (col2.equals("nombre")) &&
                (col3.equals("partido"));
    }

    private boolean validarCandidato(Candidato candidato) {
        return candidato.idCandidato > 0 &&
                candidato.nombre != null && !candidato.nombre.trim().isEmpty() &&
                candidato.partido != null && !candidato.partido.trim().isEmpty();
    }

    private void mostrarCandidatosCargados() {
        System.out.println("\n=== CANDIDATOS CARGADOS ===");
        for (Candidato c : candidatos) {
            System.out.printf("   ID: %-3d | %-25s | %s%n",
                    c.idCandidato, c.nombre, c.partido);
        }
        System.out.println("================================\n");
    }

    public List<Candidato> getCandidatosInternos() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(candidatos);
        } finally {
            lock.readLock().unlock();
        }
    }

    private static class ConfirmacionCallback implements IConfirmacionCandidatos {
        @Override
        public void recibirConfirmacion(boolean ok, String mensaje, Current current) {
            if (ok) {
                System.out.println("Confirmación exitosa: " + mensaje);
            } else {
                System.err.println("Error en confirmación: " + mensaje);
            }
        }
    }
}