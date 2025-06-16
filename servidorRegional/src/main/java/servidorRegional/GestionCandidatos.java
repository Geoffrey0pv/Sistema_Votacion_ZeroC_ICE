package servidorRegional;

import Demo.*;
import com.zeroc.Ice.*;
import java.io.InputStream;
import java.util.Properties;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class GestionCandidatos implements ICargarCandidatos {
    private final Communicator communicator;
    private final List<CandidatoElectoral> candidatosElectorales;
    private IConsultaCandidatosPrx consultaCandidatosNacional;
    private ObjectAdapter callbackAdapter;
    private Properties config;
    private final AtomicBoolean actualizandoCandidatos = new AtomicBoolean(false);

    public GestionCandidatos(Communicator communicator) {
        this.communicator = communicator;
        this.candidatosElectorales = new CopyOnWriteArrayList<>();
        cargarConfiguracion();

        // Crear adaptador para callbacks una sola vez
        try {
            this.callbackAdapter = communicator.createObjectAdapter("");
            this.callbackAdapter.activate();
        } catch (Exception e) {
            System.err.println("❌ Error creando adaptador para callbacks: " + e.getMessage());
        }

        conectarAServidorNacional();
    }

    private void cargarConfiguracion() {
        config = new Properties();
        try (InputStream input = GestionCandidatos.class.getClassLoader().getResourceAsStream("regional.properties")) {
            if (input == null) {
                System.err.println("No se pudo encontrar regional.properties");
                return;
            }
            config.load(input);
            System.out.println("Configuración cargada correctamente");
        } catch (Exception e) {
            System.err.println("Error cargando configuración: " + e.getMessage());
        }
    }

    private void conectarAServidorNacional() {
        try {
            System.out.println("🔄 Intentando conectar GestionCandidatos con Servidor Nacional...");

            // Proxy para IConsultaCandidatos
            ObjectPrx consultaBase = communicator.stringToProxy("ConsultaCandidatos@ServidorNacionalAdapter");
            consultaCandidatosNacional = IConsultaCandidatosPrx.checkedCast(consultaBase);
            if (consultaCandidatosNacional == null) {
                System.err.println("⚠️ No se pudo conectar al servicio de Consulta de Candidatos");
                System.err.println("💡 Los candidatos se cargarán bajo demanda cuando sea necesario");
                return;
            }
            
            System.out.println("✅ GestionCandidatos conectado al servicio de Consulta de Candidatos");
            actualizarCandidatosElectoralesDesdeNacional();
            
        } catch (Exception e) {
            System.err.println("⚠️ GestionCandidatos no pudo conectar: " + e.getMessage());
            System.err.println("💡 Los candidatos se cargarán bajo demanda cuando sea necesario");
        }
    }

    private void actualizarCandidatosElectoralesDesdeNacional() {
        try {
            if (consultaCandidatosNacional != null) {
                System.out.println("🗳️  Solicitando candidatos electorales al Servidor Nacional...");
                CandidatoElectoral[] resultado = consultaCandidatosNacional.obtenerTodosCandidatosElectorales();
                if (resultado != null && resultado.length > 0) {
                    this.candidatosElectorales.clear();
                    for (CandidatoElectoral c : resultado) {
                        this.candidatosElectorales.add(c);
                    }
                    System.out.println("✅ Candidatos Electorales actualizados: " + this.candidatosElectorales.size());
                } else {
                     System.err.println("⚠️ No se obtuvieron candidatos electorales (lista vacía).");
                }
            }
        } catch(Exception e) {
            System.err.println("❌ Error al obtener candidatos electorales: " + e.getMessage());
        }
    }

    @Override
    public boolean enviarCandidatosATodasMesas(Current current) {
        if (candidatosElectorales.isEmpty()) {
            System.err.println("⚠️ No hay candidatos para enviar, intentando actualizar desde el nacional...");
            actualizarCandidatosElectoralesDesdeNacional();
            return false;
        }
        
        System.out.println("Enviando candidatos a todas las mesas registradas (lógica no implementada).");
        return true;
    }

    @Override
    public byte[] distribuirPadron(String departamento, Current current) {
        System.out.println("🚚 Solicitud de padrón recibida para: " + departamento);
        if (departamento == null || departamento.trim().isEmpty()) {
            System.err.println("❌ Solicitud rechazada: el departamento no puede ser vacío.");
            return new byte[0];
        }
        try {
            byte[] padronBytes = PadronElectoral.generarPadron(departamento, 150);
            System.out.println("✅ Padrón para '" + departamento + "' generado, enviando " + padronBytes.length + " bytes.");
            return padronBytes;
        } catch (Exception e) {
            System.err.println("❌ Error crítico generando el padrón para '" + departamento + "': " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }

    @Override
    public boolean enviarCandidatosAMesas(String endpointMesa, Current current) {
        System.out.println("DEBUG: Solicitud recibida de la mesa: " + endpointMesa);

        if (candidatosElectorales.isEmpty()) {
            System.err.println("DEBUG: ⚠️ Lista de candidatos electorales vacía. Intentando actualizar...");
            actualizarCandidatosElectoralesDesdeNacional();
            
            try {
                Thread.sleep(3000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (candidatosElectorales.isEmpty()) {
                System.err.println("DEBUG: ❌ Fallo definitivo. No se pudieron obtener candidatos.");
                return false;
            }
        }

        // Convertir la lista de 'CandidatoElectoral' al formato 'Candidato' que espera la mesa.
        List<Candidato> candidatosParaEnviar = new ArrayList<>();
        for (CandidatoElectoral ce : this.candidatosElectorales) {
            // Asumiendo que solo los candidatos activos deben ser enviados
            if (ce.activo) {
                candidatosParaEnviar.add(new Candidato(ce.id, ce.nombre, ce.partido));
            }
        }
        
        Candidato[] arrayCandidatos = candidatosParaEnviar.toArray(new Candidato[0]);

        System.out.println("DEBUG: Preparando para enviar " + arrayCandidatos.length + " candidatos a la mesa.");
        if (arrayCandidatos.length > 0) {
            System.out.println("DEBUG: Primer candidato a enviar: " + arrayCandidatos[0].nombre);
        }

        return enviarCandidatosAMesa(endpointMesa, arrayCandidatos);
    }

    private boolean enviarCandidatosAMesa(String endpoint, Candidato[] candidatos) {
        try {
            System.out.println("Enviando " + candidatos.length + " candidatos a: " + endpoint);
            
            ObjectPrx mesaBase = communicator.stringToProxy(endpoint);
            IRecibirCandidatosPrx mesa = IRecibirCandidatosPrx.checkedCast(mesaBase);

            if (mesa == null) {
                System.err.println("❌ No se pudo obtener un proxy para la mesa en el endpoint: " + endpoint);
                return false;
            }

            ConfirmacionCallback callback = new ConfirmacionCallback();
            IConfirmacionCandidatosPrx callbackProxy = IConfirmacionCandidatosPrx.uncheckedCast(
                callbackAdapter.addWithUUID(callback)
            );

            mesa.recibirCandidatos(candidatos, callbackProxy);
            System.out.println("✅ Petición de candidatos enviada a " + endpoint);
            
            return true; 
        } catch (Exception e) {
            System.err.println("❌ Error enviando candidatos a mesa " + endpoint + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Métodos para gestionar la lista de candidatos
    public void actualizarCandidatos(Candidato[] nuevosCandidatos) {
        // Implementa la lógica para actualizar la lista de candidatos
    }

    public void agregarEndpointMesa(String endpoint) {
        // Implementa la lógica para agregar un nuevo endpoint de mesa
    }

    public void removerEndpointMesa(String endpoint) {
        // Implementa la lógica para remover un endpoint de mesa
    }

    public List<String> obtenerEndpointsMesas() {
        // Implementa la lógica para obtener la lista de endpoints de mesas
        return new ArrayList<>();
    }

    public int obtenerCantidadCandidatos() {
        return candidatosElectorales.size();
    }

    // Clase interna para manejar callbacks de confirmación
    private static class ConfirmacionCallback implements IConfirmacionCandidatos {
        private boolean exitoso = false;
        private String mensaje = "";

        @Override
        public void recibirConfirmacion(boolean ok, String mensaje, Current current) {
            this.exitoso = ok;
            this.mensaje = mensaje;
            System.out.println("Confirmación recibida - Éxito: " + ok + ", Mensaje: " + mensaje);
        }

        public boolean isExitoso() {
            return exitoso;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}