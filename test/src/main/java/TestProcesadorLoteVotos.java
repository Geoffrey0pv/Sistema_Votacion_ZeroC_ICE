import Demo.*;
import com.zeroc.Ice.*;

/**
 * 🧪 Cliente de prueba para el Procesador de Lotes de Votos
 * 
 * Demuestra cómo enviar lotes de votos al servidor nacional
 * usando el sistema de reliable messaging.
 */
public class TestProcesadorLoteVotos {
    
    // 📋 CONFIGURACIÓN DE DATOS DE PRUEBA - FÁCIL EDICIÓN
    private static final String[] MESA_IDS = {
        "105",
        "107", 
        "108",
        "109",
        "110",
        "111",
        "112",
        "113"
    };
    
    private static final String[] CANDIDATO_IDS = {
        "10",
        "11",
        "12", 
        "13",
        "14",
        "15"
    };
    
    private static final String[] MUNICIPIOS = {
        "CAQUETÁ",
        "CAUCA",
        "CESAR",
        "CÓRDOBA",
        "CUNDINAMARCA",
        "CHOCÓ",
        "HUILA",
        "LA GUAJIRA",
        "MAGDALENA"
    };
    
    private static final String[] DEPARTAMENTOS = {
        "CAQUETÁ",
        "CAUCA",
        "CESAR",
        "CÓRDOBA",
        "CUNDINAMARCA",
        "CHOCÓ",
        "HUILA",
        "LA GUAJIRA",
        "MAGDALENA"
    };
    
    // Número de votos a generar (puedes cambiar este valor)
    private static final int NUMERO_VOTOS = 5;
    
    public static void main(String[] args) {
        Communicator communicator = null;
        
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servidor nacional
            String endpoint = "tcp -h localhost -p 9090";
            ObjectPrx base = communicator.stringToProxy("ProcesadorLoteVotos:" + endpoint);
            IProcesadorLoteVotosPrx procesador = IProcesadorLoteVotosPrx.checkedCast(base);
            
            if (procesador == null) {
                System.err.println("❌ No se pudo conectar al ProcesadorLoteVotos");
                return;
            }
            
            System.out.println("✅ Conectado al ProcesadorLoteVotos");
            
            // Verificar disponibilidad
            boolean disponible = procesador.verificarDisponibilidad();
            System.out.println("📊 Servicio disponible: " + (disponible ? "SÍ" : "NO"));
            
            if (!disponible) {
                System.err.println("❌ El servicio no está disponible");
                return;
            }
            
            // Crear adaptador para recibir callbacks
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                "CallbackAdapter", "tcp -h localhost -p 0");
            
            // Crear callback para recibir confirmaciones
            ConfirmacionLoteVotosImpl callback = new ConfirmacionLoteVotosImpl();
            ObjectPrx callbackPrx = adapter.addWithUUID(callback);
            IConfirmacionLoteVotosPrx callbackProxy = IConfirmacionLoteVotosPrx.uncheckedCast(callbackPrx);
            
            adapter.activate();
            
            // Crear lote de votos de prueba
            String jsonVotos = crearLoteVotosPrueba();
            
            System.out.println("📦 Enviando lote de votos...");
            System.out.println("📄 JSON: " + jsonVotos);
            
            // Enviar lote de votos
            procesador.procesarLoteVotos(jsonVotos, callbackProxy);
            
            System.out.println("⏳ Esperando confirmación...");
            
            // Esperar confirmación (máximo 30 segundos)
            long startTime = System.currentTimeMillis();
            while (!callback.isConfirmacionRecibida() && 
                   (System.currentTimeMillis() - startTime) < 30000) {
                Thread.sleep(1000);
                System.out.print(".");
            }
            System.out.println();
            
            if (callback.isConfirmacionRecibida()) {
                System.out.println("✅ Confirmación recibida!");
                callback.mostrarResultado();
            } else {
                System.out.println("⏰ Timeout esperando confirmación");
            }
            
            // Obtener estadísticas
            System.out.println("\n📊 Obteniendo estadísticas...");
            EstadisticasVotos stats = procesador.obtenerEstadisticas();
            mostrarEstadisticas(stats);
            
            // Esperar un poco más para ver el procesamiento
            System.out.println("\n⏳ Esperando procesamiento (10 segundos)...");
            Thread.sleep(10000);
            
            // Obtener estadísticas actualizadas
            System.out.println("\n📊 Estadísticas actualizadas:");
            stats = procesador.obtenerEstadisticas();
            mostrarEstadisticas(stats);
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }
    
    /**
     * Crea un lote de votos de prueba en formato JSON usando los arreglos configurables
     */
    private static String crearLoteVotosPrueba() {
        StringBuilder json = new StringBuilder();
        json.append("[");
        
        for (int i = 0; i < NUMERO_VOTOS; i++) {
            if (i > 0) json.append(",");
            
            // Usar datos de los arrays de forma circular
            String mesaId = MESA_IDS[i % MESA_IDS.length];
            String candidatoId = CANDIDATO_IDS[i % CANDIDATO_IDS.length];
            String municipio = MUNICIPIOS[i % MUNICIPIOS.length];
            String departamento = DEPARTAMENTOS[i % DEPARTAMENTOS.length];
            
            // Timestamp incremental (en segundos desde epoch)
            long baseTimestamp = System.currentTimeMillis() / 1000; // Convertir a segundos
            long timestamp = baseTimestamp + (i * 60); // Incrementar 1 minuto por voto
            String timestampStr = java.time.Instant.ofEpochSecond(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Hash único para cada voto
            String hash = generarHashVerificacion(mesaId, candidatoId, String.valueOf(timestamp));
            
            // Firma de mesa simulada (en un sistema real sería una firma digital)
            String firmaMesa = "FIRMA_MESA_" + mesaId + "_" + timestamp;
            
            json.append(String.format(
                "{\"mesaId\":\"%s\",\"candidatoId\":\"%s\",\"timestamp\":\"%s\"," +
                "\"municipio\":\"%s\",\"departamento\":\"%s\",\"hashVerificacion\":\"%s\"," +
                "\"firmaMesa\":\"%s\"}",
                mesaId, candidatoId, timestampStr, municipio, departamento, hash, firmaMesa
            ));
        }
        
        json.append("]");
        return json.toString();
    }
    
    /**
     * Genera un hash de verificación único basado en los datos del voto
     */
    private static String generarHashVerificacion(String mesaId, String candidatoId, String timestamp) {
        String data = mesaId + candidatoId + timestamp;
        return String.format("%064x", data.hashCode() & 0xFFFFFFFFL).substring(0, 64);
    }
    
    /**
     * Muestra las estadísticas de votos
     */
    private static void mostrarEstadisticas(EstadisticasVotos stats) {
        System.out.println("📊 === ESTADÍSTICAS DE VOTOS ===");
        System.out.println("   📝 Total votos: " + stats.totalVotos);
        System.out.println("   🗳️  Total mesas: " + stats.totalMesas);
        System.out.println("   👥 Total candidatos: " + stats.totalCandidatos);
        System.out.println("   🏛️  Total municipios: " + stats.totalMunicipios);
        System.out.println("   ⏰ Primer voto: " + (stats.primerVoto.isEmpty() ? "N/A" : stats.primerVoto));
        System.out.println("   ⏰ Último voto: " + (stats.ultimoVoto.isEmpty() ? "N/A" : stats.ultimoVoto));
        System.out.println("================================");
    }
    
    /**
     * Implementación del callback para recibir confirmaciones
     */
    static class ConfirmacionLoteVotosImpl implements IConfirmacionLoteVotos {
        private boolean confirmacionRecibida = false;
        private ResultadoProcesamiento resultado;
        
        @Override
        public void recibirConfirmacion(ResultadoProcesamiento resultado, Current current) {
            this.resultado = resultado;
            this.confirmacionRecibida = true;
            
            System.out.println("\n🔔 === CONFIRMACIÓN RECIBIDA ===");
            System.out.println("   ✅ Éxito: " + (resultado.exito ? "SÍ" : "NO"));
            System.out.println("   📊 Total votos: " + resultado.totalVotos);
            System.out.println("   📥 Votos encolados: " + resultado.votosEncolados);
            System.out.println("   💬 Mensaje: " + resultado.mensaje);
            System.out.println("   ⏰ Timestamp: " + new java.util.Date(resultado.timestamp));
            System.out.println("===============================");
        }
        
        public boolean isConfirmacionRecibida() {
            return confirmacionRecibida;
        }
        
        public void mostrarResultado() {
            if (resultado != null) {
                System.out.println("📋 Resultado final:");
                System.out.println("   Estado: " + (resultado.exito ? "ÉXITO" : "FALLO"));
                System.out.println("   Procesados: " + resultado.votosEncolados + "/" + resultado.totalVotos);
                
                if (resultado.votosEncolados > 0) {
                    System.out.println("   ✅ Los votos están en la cola confiable y se procesarán automáticamente");
                    System.out.println("   🔄 Incluso si la BD falla, los votos se mantendrán seguros");
                }
            }
        }
    }
} 