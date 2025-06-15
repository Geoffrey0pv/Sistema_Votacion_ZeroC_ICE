package Services;

import Demo.*;
import com.zeroc.Ice.Current;
import Services.VotosService;

/**
 * 🗳️ Implementación del Procesador de Lotes de Votos
 * 
 * Recibe conjuntos de votos en formato JSON y los procesa usando
 * el sistema de reliable messaging existente.
 * 
 * Ejemplo de JSON esperado:
 * [
 *   {
 *     "mesa_id": "MESA-12345",
 *     "candidato_id": "PETRO2025", 
 *     "timestamp": "2025-06-15T18:42:31Z",
 *     "municipio": "Bogotá",
 *     "departamento": "Cundinamarca",
 *     "hash_verificacion": "9e1d8a6a23a1e0f0ee4fbb4573a6f51f...",
 *     "firma_mesa": "MIICIjANBgkqhki...base64..."
 *   }
 * ]
 */
public class ProcesadorLoteVotosImpl implements IProcesadorLoteVotos {
    
    private final VotosService votosService;
    
    public ProcesadorLoteVotosImpl() {
        this.votosService = new VotosService();
        System.out.println("✅ ProcesadorLoteVotos inicializado");
        
        // Test de conexión a base de datos
        votosService.testDatabaseConnection();
    }
    
    /**
     * Procesa un lote de votos en formato JSON
     */
    @Override
    public void procesarLoteVotos(String jsonVotos, IConfirmacionLoteVotosPrx callback, Current current) {
        System.out.println("📦 Recibiendo lote de votos...");
        
        if (jsonVotos == null || jsonVotos.trim().isEmpty()) {
            enviarRespuesta(callback, false, 0, 0, "JSON vacío o nulo");
            return;
        }
        
        try {
            // Procesar el lote usando el servicio existente
            VotosService.ProcessingResult resultado = votosService.receiveVotesPackage(jsonVotos);
            
            // Enviar confirmación asíncrona
            enviarRespuesta(callback, 
                resultado.success, 
                resultado.totalVotes, 
                resultado.enqueuedVotes, 
                resultado.message);
            
            System.out.println("✅ Lote procesado: " + resultado.enqueuedVotes + "/" + resultado.totalVotes + " votos");
            
        } catch (Exception e) {
            System.err.println("❌ Error procesando lote: " + e.getMessage());
            enviarRespuesta(callback, false, 0, 0, "Error interno: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene estadísticas de votos procesados
     */
    @Override
    public EstadisticasVotos obtenerEstadisticas(Current current) {
        try {
            VotosService.VotingStats stats = votosService.getVotingStats();
            
            EstadisticasVotos estadisticas = new EstadisticasVotos();
            estadisticas.totalVotos = stats.totalVotos;
            estadisticas.totalMesas = stats.totalMesas;
            estadisticas.totalCandidatos = stats.totalCandidatos;
            estadisticas.totalMunicipios = stats.totalMunicipios;
            estadisticas.primerVoto = stats.primerVoto != null ? stats.primerVoto.toString() : "";
            estadisticas.ultimoVoto = stats.ultimoVoto != null ? stats.ultimoVoto.toString() : "";
            
            return estadisticas;
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo estadísticas: " + e.getMessage());
            
            // Retornar estadísticas vacías en caso de error
            EstadisticasVotos estadisticas = new EstadisticasVotos();
            estadisticas.totalVotos = 0;
            estadisticas.totalMesas = 0;
            estadisticas.totalCandidatos = 0;
            estadisticas.totalMunicipios = 0;
            estadisticas.primerVoto = "";
            estadisticas.ultimoVoto = "";
            
            return estadisticas;
        }
    }
    
    /**
     * Verifica si el servicio está disponible
     */
    @Override
    public boolean verificarDisponibilidad(Current current) {
        return votosService.isServiceAvailable();
    }
    
    /**
     * Envía respuesta asíncrona al cliente
     */
    private void enviarRespuesta(IConfirmacionLoteVotosPrx callback, boolean exito, int totalVotos, int votosEncolados, String mensaje) {
        if (callback == null) {
            System.out.println("⚠️ No hay callback para enviar respuesta");
            return;
        }
        
        try {
            ResultadoProcesamiento resultado = new ResultadoProcesamiento();
            resultado.exito = exito;
            resultado.totalVotos = totalVotos;
            resultado.votosEncolados = votosEncolados;
            resultado.mensaje = mensaje;
            resultado.timestamp = System.currentTimeMillis();
            
            // Enviar confirmación de forma asíncrona usando el proxy
            callback.recibirConfirmacionAsync(resultado);
            
        } catch (Exception e) {
            System.err.println("❌ Error enviando confirmación: " + e.getMessage());
        }
    }
    
    /**
     * Limpieza de recursos
     */
    public void shutdown() {
        try {
            if (votosService != null) {
                votosService.shutdown();
            }
            System.out.println("✅ ProcesadorLoteVotos detenido");
        } catch (Exception e) {
            System.err.println("❌ Error en shutdown: " + e.getMessage());
        }
    }
} 