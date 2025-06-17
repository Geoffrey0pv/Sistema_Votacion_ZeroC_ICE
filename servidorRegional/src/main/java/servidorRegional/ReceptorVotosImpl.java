package servidorRegional;

import Demo.*;
import com.zeroc.Ice.Current;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementación del Receptor de Votos Regional
 * Recibe votos de las mesas de votación y los almacena en SQLite
 */
public class ReceptorVotosImpl implements IReceptorVotosRegional {
    
    private final GestorVotosRegionalSQLite gestorVotos;
    private final String nombreServidor;
    private long votosRecibidos = 0;
    private long tiempoInicioServicio;
    private final DatabaseManager databaseManager;
    
    public ReceptorVotosImpl(String nombreServidor) {
        this.nombreServidor = nombreServidor != null ? nombreServidor : "ServidorRegional";
        this.gestorVotos = new GestorVotosRegionalSQLite();
        this.databaseManager = new DatabaseManager();
        this.tiempoInicioServicio = System.currentTimeMillis();
        
        System.out.println("🏛️ Receptor de Votos Regional inicializado: " + this.nombreServidor);
        System.out.println("📊 Base de datos: " + gestorVotos.getDbPath());
    }
    
    @Override
    public boolean recibirVoto(VotoRegional voto, Current current) {
        try {
            if (voto == null) {
                System.err.println("❌ Voto nulo recibido");
                return false;
            }
            
            // Completar información geográfica si está vacía
            completarInformacionGeografica(voto);
            
            // Log del voto recibido
            System.out.println("📥 Voto recibido:");
            System.out.println("   ID: " + voto.idVoto);
            System.out.println("   Mesa: " + voto.mesaId);
            System.out.println("   Candidato: " + voto.candidatoId);
            System.out.println("   Hash Elector: " + voto.hashElector);
            System.out.println("   Municipio: " + (voto.municipio != null ? voto.municipio : "N/A"));
            System.out.println("   Departamento: " + (voto.departamento != null ? voto.departamento : "N/A"));
            System.out.println("   Timestamp: " + voto.timestamp);
            
            // Verificar si el voto ya existe
            if (gestorVotos.existeVoto(voto.idVoto)) {
                System.out.println("⚠️ Voto ya existe (ID: " + voto.idVoto + ") - actualizando");
            }
            
            // Verificar duplicado por hash y mesa
            if (gestorVotos.existeVotoPorHash(voto.hashElector, voto.mesaId)) {
                System.out.println("⚠️ Ya existe voto para este elector en la mesa " + voto.mesaId);
                // Aún así guardamos para actualizar
            }
            
            // Guardar el voto
            boolean guardado = gestorVotos.guardarVoto(voto);
            
            if (guardado) {
                votosRecibidos++;
                System.out.println("✅ Voto guardado exitosamente (Total: " + votosRecibidos + ")");
                
                // Log estadísticas cada 10 votos
                if (votosRecibidos % 10 == 0) {
                    mostrarEstadisticasRapidas();
                }
                
                return true;
            } else {
                System.err.println("❌ Error guardando voto ID: " + voto.idVoto);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error procesando voto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public ResultadoRecepcionVotos recibirListaVotos(VotoRegional[] votos, Current current) {
        try {
            if (votos == null || votos.length == 0) {
                System.err.println("❌ Lote de votos vacío o nulo");
                
                ResultadoRecepcionVotos resultado = new ResultadoRecepcionVotos();
                resultado.exito = false;
                resultado.totalRecibidos = 0;
                resultado.votosGuardados = 0;
                resultado.votosRechazados = 0;
                resultado.mensaje = "Lote vacío o nulo";
                resultado.tiempoProcessamiento = 0;
                resultado.errores = new String[]{"No se recibieron votos"};
                
                return resultado;
            }
            
            System.out.println("📦 Lote de votos recibido:");
            System.out.println("   Total votos: " + votos.length);
            System.out.println("   Procesando...");
            
            // Completar información geográfica para todos los votos del lote
            for (VotoRegional voto : votos) {
                if (voto != null) {
                    completarInformacionGeografica(voto);
                }
            }
            
            // Procesar el lote usando el gestor
            ResultadoRecepcionVotos resultado = gestorVotos.guardarVotosLote(votos);
            
            // Actualizar contador global
            votosRecibidos += resultado.votosGuardados;
            
            // Log del resultado
            System.out.println("📊 Resultado del lote:");
            System.out.println("   Éxito: " + resultado.exito);
            System.out.println("   Votos guardados: " + resultado.votosGuardados);
            System.out.println("   Votos rechazados: " + resultado.votosRechazados);
            System.out.println("   Tiempo: " + resultado.tiempoProcessamiento + "ms");
            System.out.println("   Total acumulado: " + votosRecibidos);
            
            // Mostrar estadísticas si es un lote grande
            if (votos.length >= 10) {
                mostrarEstadisticasRapidas();
            }
            
            return resultado;
            
        } catch (Exception e) {
            System.err.println("❌ Error procesando lote de votos: " + e.getMessage());
            e.printStackTrace();
            
            ResultadoRecepcionVotos resultado = new ResultadoRecepcionVotos();
            resultado.exito = false;
            resultado.totalRecibidos = votos != null ? votos.length : 0;
            resultado.votosGuardados = 0;
            resultado.votosRechazados = votos != null ? votos.length : 0;
            resultado.mensaje = "Error interno: " + e.getMessage();
            resultado.tiempoProcessamiento = 0;
            resultado.errores = new String[]{"Error interno del servidor: " + e.getMessage()};
            
            return resultado;
        }
    }
    
    @Override
    public long contarVotosAlmacenados(Current current) {
        try {
            long total = gestorVotos.contarVotosAlmacenados();
            System.out.println("📊 Consulta total votos: " + total);
            return total;
        } catch (Exception e) {
            System.err.println("❌ Error contando votos: " + e.getMessage());
            return -1;
        }
    }
    
    @Override
    public long contarVotosPorMesa(String mesaId, Current current) {
        try {
            if (mesaId == null || mesaId.trim().isEmpty()) {
                System.err.println("❌ Mesa ID vacío o nulo");
                return -1;
            }
            
            long total = gestorVotos.contarVotosPorMesa(mesaId);
            System.out.println("📊 Votos mesa " + mesaId + ": " + total);
            return total;
        } catch (Exception e) {
            System.err.println("❌ Error contando votos por mesa: " + e.getMessage());
            return -1;
        }
    }
    
    @Override
    public long contarVotosPorCandidato(long candidatoId, Current current) {
        try {
            if (candidatoId <= 0) {
                System.err.println("❌ Candidato ID inválido: " + candidatoId);
                return -1;
            }
            
            long total = gestorVotos.contarVotosPorCandidato(candidatoId);
            System.out.println("📊 Votos candidato " + candidatoId + ": " + total);
            return total;
        } catch (Exception e) {
            System.err.println("❌ Error contando votos por candidato: " + e.getMessage());
            return -1;
        }
    }
    
    @Override
    public VotoRegional[] obtenerVotosPorMesa(String mesaId, Current current) {
        try {
            if (mesaId == null || mesaId.trim().isEmpty()) {
                System.err.println("❌ Mesa ID vacío o nulo");
                return new VotoRegional[0];
            }
            
            List<VotoRegional> votos = gestorVotos.obtenerVotosPorMesa(mesaId);
            System.out.println("📊 Obtenidos " + votos.size() + " votos para mesa " + mesaId);
            return votos.toArray(new VotoRegional[0]);
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo votos por mesa: " + e.getMessage());
            return new VotoRegional[0];
        }
    }
    
    @Override
    public VotoRegional[] obtenerVotosPorCandidato(long candidatoId, Current current) {
        try {
            if (candidatoId <= 0) {
                System.err.println("❌ Candidato ID inválido: " + candidatoId);
                return new VotoRegional[0];
            }
            
            List<VotoRegional> votos = gestorVotos.obtenerVotosPorCandidato(candidatoId);
            System.out.println("📊 Obtenidos " + votos.size() + " votos para candidato " + candidatoId);
            return votos.toArray(new VotoRegional[0]);
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo votos por candidato: " + e.getMessage());
            return new VotoRegional[0];
        }
    }
    
    @Override
    public boolean existeVoto(long idVoto, Current current) {
        try {
            boolean existe = gestorVotos.existeVoto(idVoto);
            System.out.println("📊 Voto ID " + idVoto + " existe: " + existe);
            return existe;
        } catch (Exception e) {
            System.err.println("❌ Error verificando voto: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean existeVotoPorHash(String hashElector, String mesaId, Current current) {
        try {
            if (hashElector == null || hashElector.trim().isEmpty() || 
                mesaId == null || mesaId.trim().isEmpty()) {
                System.err.println("❌ Hash o Mesa ID vacío");
                return false;
            }
            
            boolean existe = gestorVotos.existeVotoPorHash(hashElector, mesaId);
            System.out.println("📊 Voto con hash " + hashElector + " en mesa " + mesaId + " existe: " + existe);
            return existe;
        } catch (Exception e) {
            System.err.println("❌ Error verificando voto por hash: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean limpiarVotos(Current current) {
        try {
            System.out.println("🧹 Solicitud de limpieza de votos...");
            
            boolean limpiado = gestorVotos.limpiarVotos();
            
            if (limpiado) {
                votosRecibidos = 0; // Resetear contador de sesión
                System.out.println("✅ Votos limpiados exitosamente");
                return true;
            } else {
                System.err.println("❌ Error limpiando votos");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en limpieza: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean limpiarVotosMesa(String mesaId, Current current) {
        try {
            if (mesaId == null || mesaId.trim().isEmpty()) {
                System.err.println("❌ Mesa ID vacío para limpieza");
                return false;
            }
            
            System.out.println("🧹 Limpiando votos de mesa: " + mesaId);
            
            boolean limpiado = gestorVotos.limpiarVotosMesa(mesaId);
            
            if (limpiado) {
                System.out.println("✅ Votos de mesa " + mesaId + " limpiados");
                return true;
            } else {
                System.err.println("❌ Error limpiando votos de mesa " + mesaId);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error limpiando mesa: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean verificarServicio(Current current) {
        try {
            boolean servicioOK = gestorVotos.verificarServicio();
            
            if (servicioOK) {
                System.out.println("✅ Servicio verificado - funcionando correctamente");
                return true;
            } else {
                System.err.println("❌ Servicio con problemas");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error verificando servicio: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String obtenerEstadisticasDetalladas(Current current) {
        try {
            StringBuilder stats = new StringBuilder();
            
            // Información del servicio
            long tiempoFuncionamiento = System.currentTimeMillis() - tiempoInicioServicio;
            String tiempoFormateado = formatearTiempo(tiempoFuncionamiento);
            
            stats.append("🏛️ RECEPTOR DE VOTOS REGIONAL\n");
            stats.append("═══════════════════════════════\n");
            stats.append("Servidor: ").append(nombreServidor).append("\n");
            stats.append("Tiempo funcionamiento: ").append(tiempoFormateado).append("\n");
            stats.append("Votos recibidos (sesión): ").append(votosRecibidos).append("\n");
            stats.append("Fecha consulta: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n\n");
            
            // Estadísticas detalladas de la base de datos
            String estadisticasDB = gestorVotos.obtenerEstadisticasDetalladas();
            stats.append(estadisticasDB);
            
            String resultado = stats.toString();
            System.out.println("📊 Estadísticas solicitadas:");
            System.out.println(resultado);
            
            return resultado;
            
        } catch (Exception e) {
            String error = "❌ Error obteniendo estadísticas: " + e.getMessage();
            System.err.println(error);
            return error;
        }
    }
    
    /**
     * Muestra estadísticas rápidas en consola
     */
    private void mostrarEstadisticasRapidas() {
        try {
            long totalDB = gestorVotos.contarVotosAlmacenados();
            long tiempoFuncionamiento = System.currentTimeMillis() - tiempoInicioServicio;
            
            System.out.println("📊 ESTADÍSTICAS RÁPIDAS:");
            System.out.println("   Votos en BD: " + totalDB);
            System.out.println("   Votos sesión: " + votosRecibidos);
            System.out.println("   Tiempo activo: " + formatearTiempo(tiempoFuncionamiento));
            
        } catch (Exception e) {
            System.err.println("❌ Error mostrando estadísticas: " + e.getMessage());
        }
    }
    
    /**
     * Formatea tiempo en milisegundos a formato legible
     */
    private String formatearTiempo(long milisegundos) {
        long segundos = milisegundos / 1000;
        long minutos = segundos / 60;
        long horas = minutos / 60;
        
        segundos = segundos % 60;
        minutos = minutos % 60;
        
        if (horas > 0) {
            return String.format("%dh %dm %ds", horas, minutos, segundos);
        } else if (minutos > 0) {
            return String.format("%dm %ds", minutos, segundos);
        } else {
            return String.format("%ds", segundos);
        }
    }
    
    /**
     * Obtiene información del servicio
     */
    public String getNombreServidor() {
        return nombreServidor;
    }
    
    public long getVotosRecibidos() {
        return votosRecibidos;
    }
    
    public long getTiempoFuncionamiento() {
        return System.currentTimeMillis() - tiempoInicioServicio;
    }
    
    /**
     * Obtiene el gestor de votos SQLite (para sincronización)
     */
    public GestorVotosRegionalSQLite getGestorVotos() {
        return gestorVotos;
    }
    
    private void completarInformacionGeografica(VotoRegional voto) {
        // Si ya tiene municipio y departamento, no hacer nada
        if (voto.municipio != null && !voto.municipio.trim().isEmpty() && 
            voto.departamento != null && !voto.departamento.trim().isEmpty()) {
            return;
        }
        
        try {
            // Buscar información del votante por hash en la base de datos
            // El hash del elector debería corresponder al documento del votante
            String documento = voto.hashElector;
            
            // Intentar buscar en diferentes departamentos conocidos
            String[] departamentosComunes = {"CUNDINAMARCA", "ANTIOQUIA", "VALLE", "ATLANTICO", "BOLIVAR"};
            
            for (String departamento : departamentosComunes) {
                List<CiudadanoInfo> votantes = databaseManager.consultarVotantesLocales(departamento);
                
                // Buscar por documento (hash) en este departamento
                for (CiudadanoInfo votante : votantes) {
                    if (votante.documento != null && votante.documento.equals(documento)) {
                        // Encontrado! Completar información
                        if (voto.municipio == null || voto.municipio.trim().isEmpty()) {
                            voto.municipio = votante.municipio != null ? votante.municipio : "DESCONOCIDO";
                        }
                        if (voto.departamento == null || voto.departamento.trim().isEmpty()) {
                            voto.departamento = votante.departamento != null ? votante.departamento : departamento;
                        }
                        
                        System.out.println("✅ Información geográfica completada desde BD:");
                        System.out.println("   Municipio: " + voto.municipio);
                        System.out.println("   Departamento: " + voto.departamento);
                        return;
                    }
                }
            }
            
            // Si no se encontró información específica, usar valores por defecto
            if (voto.municipio == null || voto.municipio.trim().isEmpty()) {
                voto.municipio = "REGIONAL_" + voto.mesaId.substring(0, Math.min(3, voto.mesaId.length()));
            }
            if (voto.departamento == null || voto.departamento.trim().isEmpty()) {
                voto.departamento = "CUNDINAMARCA"; // Departamento por defecto
            }
            
            System.out.println("⚠️ Información geográfica completada con valores por defecto:");
            System.out.println("   Municipio: " + voto.municipio);
            System.out.println("   Departamento: " + voto.departamento);
            
        } catch (Exception e) {
            System.err.println("⚠️ Error completando información geográfica: " + e.getMessage());
            
            // Valores de emergencia
            if (voto.municipio == null || voto.municipio.trim().isEmpty()) {
                voto.municipio = "DESCONOCIDO";
            }
            if (voto.departamento == null || voto.departamento.trim().isEmpty()) {
                voto.departamento = "CUNDINAMARCA";
            }
        }
    }
} 