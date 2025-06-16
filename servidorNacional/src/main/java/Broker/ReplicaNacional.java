package Broker;

import Demo.*;
import Services.CandidatosService;
import ConsultaCandidatos.ConsultaCandidatosImpl;
import Models.CandidatoModel;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Communicator;
import java.util.List;

public class ReplicaNacional implements IAdministradorCandidatos {
    
    private final MonitorRecursos monitorReplica;
    private final Communicator communicator;
    private final String nodeId;
    private final CandidatosService candidatosService;
    private final ConsultaCandidatosImpl consultaCandidatos;
    
    public ReplicaNacional(String nodeId, Communicator communicator) {
        this.nodeId = nodeId;
        this.communicator = communicator;
        this.candidatosService = new CandidatosService();
        this.consultaCandidatos = new ConsultaCandidatosImpl();
        this.monitorReplica = new MonitorRecursos(nodeId);
        
        System.out.printf("🔄 Réplica Nacional iniciada: %s%n", nodeId);
    }
    
    @Override
    public boolean cargarCandidatosDesdeCSV(String rutaArchivo, Current current) {
        monitorReplica.incrementarRequests();
        
        try {
            java.io.File archivo = new java.io.File(rutaArchivo);
            if (!archivo.exists()) {
                System.err.println("❌ Archivo CSV no encontrado: " + rutaArchivo);
                return false;
            }
            
            List<CandidatoModel> candidatos = candidatosService.cargarCandidatosDesdeCSV(archivo);
            boolean resultado = candidatosService.guardarCandidatos(candidatos) > 0;
            
            System.out.printf("📄 [%s] CSV procesado: %d candidatos%n", nodeId, candidatos.size());
            return resultado;
        } catch (Exception e) {
            System.err.printf("❌ [%s] Error cargando CSV: %s%n", nodeId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean cargarCandidatosDesdeArray(Candidato[] candidatos, Current current) {
        monitorReplica.incrementarRequests();
        
        try {
            // Convertir Candidato[] a CandidatoModel[]
            java.util.List<CandidatoModel> candidatosModel = new java.util.ArrayList<>();
            for (Candidato candidato : candidatos) {
                candidatosModel.add(new CandidatoModel(candidato.idCandidato, candidato.nombre, candidato.partido));
            }
            
            boolean resultado = candidatosService.guardarCandidatos(candidatosModel) > 0;
            System.out.printf("📦 [%s] Array procesado: %d candidatos%n", nodeId, candidatos.length);
            return resultado;
        } catch (Exception e) {
            System.err.printf("❌ [%s] Error cargando array: %s%n", nodeId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public int obtenerCantidadCandidatos(Current current) {
        monitorReplica.incrementarRequests();
        return (int) consultaCandidatos.contarCandidatos(current);
    }
    
    @Override
    public Candidato[] obtenerTodosCandidatos(Current current) {
        monitorReplica.incrementarRequests();
        
        CandidatoElectoral[] candidatosElectorales = consultaCandidatos.obtenerTodosCandidatosElectorales(current);
        
        // Convertir CandidatoElectoral[] a Candidato[]
        Candidato[] candidatos = new Candidato[candidatosElectorales.length];
        for (int i = 0; i < candidatosElectorales.length; i++) {
            candidatos[i] = new Candidato();
            candidatos[i].idCandidato = candidatosElectorales[i].id;
            candidatos[i].nombre = candidatosElectorales[i].nombre;
            candidatos[i].partido = candidatosElectorales[i].partido;
        }
        
        return candidatos;
    }
    
    @Override
    public boolean limpiarCandidatos(Current current) {
        monitorReplica.incrementarRequests();
        
        boolean resultado = candidatosService.eliminarTodosLosCandidatos();
        System.out.printf("🗑️ [%s] Candidatos limpiados: %s%n", nodeId, resultado ? "OK" : "ERROR");
        return resultado;
    }
    
    @Override
    public boolean enviarCandidatosARegional(String endpointRegional, Current current) {
        monitorReplica.incrementarRequests();
        
        try {
            // Obtener candidatos
            Candidato[] candidatos = obtenerTodosCandidatos(current);
            if (candidatos.length == 0) {
                System.err.printf("❌ [%s] No hay candidatos para enviar%n", nodeId);
                return false;
            }
            
            // Crear proxy al servidor regional
            com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy(endpointRegional);
            ICargarCandidatosPrx cargarCandidatos = ICargarCandidatosPrx.checkedCast(base);
            
            if (cargarCandidatos == null) {
                System.err.printf("❌ [%s] No se pudo conectar al servidor regional: %s%n", nodeId, endpointRegional);
                return false;
            }
            
            // Enviar candidatos
            boolean resultado = cargarCandidatos.enviarCandidatosATodasMesas();
            System.out.printf("%s [%s] Candidatos enviados al regional: %s%n", 
                resultado ? "✅" : "❌", nodeId, endpointRegional);
            return resultado;
            
        } catch (Exception e) {
            System.err.printf("❌ [%s] Error enviando candidatos a regional: %s%n", nodeId, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean enviarCandidatosATodosRegionales(Current current) {
        monitorReplica.incrementarRequests();
        
        // TODO: Implementar envío a todos los regionales conocidos
        System.out.printf("⚠️ [%s] enviarCandidatosATodosRegionales no implementado completamente%n", nodeId);
        return false;
    }
    
    // Métodos específicos de la réplica
    
    public MetricasRecursos obtenerMetricas() {
        return monitorReplica.obtenerMetricas(null);
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public boolean isServiceAvailable() {
        return candidatosService.isServiceAvailable();
    }
    
    /**
     * Sincroniza los candidatos de esta réplica con los datos proporcionados
     */
    public boolean sincronizarCandidatos(Candidato[] candidatos) {
        try {
            System.out.printf("🔄 [%s] Sincronizando %d candidatos...%n", nodeId, candidatos.length);
            
            // Limpiar candidatos existentes
            candidatosService.eliminarTodosLosCandidatos();
            
            // Cargar nuevos candidatos
            boolean resultado = cargarCandidatosDesdeArray(candidatos, null);
            
            System.out.printf("%s [%s] Sincronización completada%n", 
                resultado ? "✅" : "❌", nodeId);
            return resultado;
            
        } catch (Exception e) {
            System.err.printf("❌ [%s] Error en sincronización: %s%n", nodeId, e.getMessage());
            return false;
        }
    }
    
    // Getters para acceso a componentes internos
    public CandidatosService getCandidatosService() {
        return candidatosService;
    }
    
    public ConsultaCandidatosImpl getConsultaCandidatos() {
        return consultaCandidatos;
    }
    
    public MonitorRecursos getMonitorReplica() {
        return monitorReplica;
    }
    
    public void shutdown() {
        System.out.printf("🛑 [%s] Cerrando réplica...%n", nodeId);
        
        if (consultaCandidatos != null) {
            consultaCandidatos.shutdown();
        }
        
        System.out.printf("✅ [%s] Réplica cerrada%n", nodeId);
    }
} 