import com.zeroc.Ice.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.sql.*;

/**
 * Prueba completa del Receptor de Votos Regional
 * Prueba todas las funcionalidades implementadas en SQLite
 */
public class test_receptor_votos_regional {
    
    private static Communicator communicator;
    private static Demo.IReceptorVotosRegionalPrx receptorVotos;
    private static Random random = new Random();
    
    public static void main(String[] args) {
        System.out.println("🧪 === PRUEBA DEL RECEPTOR DE VOTOS REGIONAL ===");
        
        try {
            // Inicializar Ice
            communicator = Util.initialize(args);
            
            // Conectar al receptor de votos regional
            String proxy = "receptorVotosRegional:tcp -h localhost -p 8080";
            ObjectPrx base = communicator.stringToProxy(proxy);
            receptorVotos = Demo.IReceptorVotosRegionalPrx.checkedCast(base);
            
            if (receptorVotos == null) {
                System.err.println("❌ No se pudo conectar al receptor de votos regional");
                System.err.println("💡 Verifique que el servidor regional esté ejecutándose");
                return;
            }
            
            System.out.println("✅ Conectado al receptor de votos regional");
            
            // Ejecutar todas las pruebas
            ejecutarPruebas();
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error en las pruebas: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
    }
    
    private static void ejecutarPruebas() {
        System.out.println("\n🔬 Iniciando suite de pruebas...");
        
        try {
            // Prueba 1: Verificar servicio
            prueba1_VerificarServicio();
            
            // Prueba 2: Limpiar datos anteriores
            prueba2_LimpiarDatos();
            
            // Prueba 3: Enviar voto individual
            prueba3_EnviarVotoIndividual();
            
            // Prueba 4: Enviar lote de votos
            prueba4_EnviarLoteVotos();
            
            // Prueba 5: Consultar estadísticas
            prueba5_ConsultarEstadisticas();
            
            // Prueba 6: Consultar votos por mesa
            prueba6_ConsultarVotosPorMesa();
            
            // Prueba 7: Consultar votos por candidato
            prueba7_ConsultarVotosPorCandidato();
            
            // Prueba 8: Verificar existencia de votos
            prueba8_VerificarExistenciaVotos();
            
            // Prueba 9: Estadísticas detalladas
            prueba9_EstadisticasDetalladas();
            
            // Prueba 10: Prueba de duplicados
            prueba10_PruebaDuplicados();
            
            System.out.println("\n🎉 ¡Todas las pruebas completadas!");
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error ejecutando pruebas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void prueba1_VerificarServicio() {
        System.out.println("\n--- Prueba 1: Verificar Servicio ---");
        
        try {
            boolean servicioActivo = receptorVotos.verificarServicio();
            
            if (servicioActivo) {
                System.out.println("✅ Servicio activo y funcionando");
            } else {
                System.out.println("❌ Servicio inactivo");
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error verificando servicio: " + e.getMessage());
        }
    }
    
    private static void prueba2_LimpiarDatos() {
        System.out.println("\n--- Prueba 2: Limpiar Datos Anteriores ---");
        
        try {
            boolean limpiado = receptorVotos.limpiarVotos();
            
            if (limpiado) {
                System.out.println("✅ Datos anteriores limpiados");
            } else {
                System.out.println("⚠️ No se pudieron limpiar datos anteriores");
            }
            
            // Verificar que esté limpio
            long totalVotos = receptorVotos.contarVotosAlmacenados();
            System.out.println("📊 Votos después de limpiar: " + totalVotos);
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error limpiando datos: " + e.getMessage());
        }
    }
    
    private static void prueba3_EnviarVotoIndividual() {
        System.out.println("\n--- Prueba 3: Enviar Voto Individual ---");
        
        try {
            // Crear voto de prueba
            Demo.VotoRegional voto = crearVotoPrueba(1001, "9060", 101, "hash001");
            
            System.out.println("📤 Enviando voto individual:");
            System.out.println("   ID: " + voto.idVoto);
            System.out.println("   Mesa: " + voto.mesaId);
            System.out.println("   Candidato: " + voto.candidatoId);
            
            boolean recibido = receptorVotos.recibirVoto(voto);
            
            if (recibido) {
                System.out.println("✅ Voto individual recibido correctamente");
            } else {
                System.out.println("❌ Error recibiendo voto individual");
            }
            
            // Verificar que se guardó
            long totalVotos = receptorVotos.contarVotosAlmacenados();
            System.out.println("📊 Total votos después: " + totalVotos);
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error enviando voto individual: " + e.getMessage());
        }
    }
    
    private static void prueba4_EnviarLoteVotos() {
        System.out.println("\n--- Prueba 4: Enviar Lote de Votos ---");
        
        try {
            // Crear lote de votos de prueba
            List<Demo.VotoRegional> listaVotos = new ArrayList<>();
            
            // Votos para mesa 9060
            listaVotos.add(crearVotoPrueba(2001, "9060", 101, "hash002"));
            listaVotos.add(crearVotoPrueba(2002, "9060", 102, "hash003"));
            listaVotos.add(crearVotoPrueba(2003, "9060", 103, "hash004"));
            
            // Votos para mesa 9061
            listaVotos.add(crearVotoPrueba(2004, "9061", 101, "hash005"));
            listaVotos.add(crearVotoPrueba(2005, "9061", 102, "hash006"));
            
            // Votos para mesa 9062
            listaVotos.add(crearVotoPrueba(2006, "9062", 103, "hash007"));
            listaVotos.add(crearVotoPrueba(2007, "9062", 101, "hash008"));
            listaVotos.add(crearVotoPrueba(2008, "9062", 102, "hash009"));
            
            Demo.VotoRegional[] arrayVotos = listaVotos.toArray(new Demo.VotoRegional[0]);
            
            System.out.println("📦 Enviando lote de " + arrayVotos.length + " votos:");
            for (Demo.VotoRegional voto : arrayVotos) {
                System.out.println("   Voto ID " + voto.idVoto + " - Mesa " + voto.mesaId + " - Candidato " + voto.candidatoId);
            }
            
            Demo.ResultadoRecepcionVotos resultado = receptorVotos.recibirListaVotos(arrayVotos);
            
            System.out.println("📊 Resultado del lote:");
            System.out.println("   Éxito: " + resultado.exito);
            System.out.println("   Total recibidos: " + resultado.totalRecibidos);
            System.out.println("   Votos guardados: " + resultado.votosGuardados);
            System.out.println("   Votos rechazados: " + resultado.votosRechazados);
            System.out.println("   Tiempo procesamiento: " + resultado.tiempoProcessamiento + "ms");
            System.out.println("   Mensaje: " + resultado.mensaje);
            
            if (resultado.errores != null && resultado.errores.length > 0) {
                System.out.println("   Errores:");
                for (String error : resultado.errores) {
                    System.out.println("     - " + error);
                }
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error enviando lote de votos: " + e.getMessage());
        }
    }
    
    private static void prueba5_ConsultarEstadisticas() {
        System.out.println("\n--- Prueba 5: Consultar Estadísticas ---");
        
        try {
            long totalVotos = receptorVotos.contarVotosAlmacenados();
            System.out.println("📊 Total votos almacenados: " + totalVotos);
            
            // Contar por mesas específicas
            String[] mesas = {"9060", "9061", "9062"};
            for (String mesa : mesas) {
                long votosMesa = receptorVotos.contarVotosPorMesa(mesa);
                System.out.println("📊 Votos en mesa " + mesa + ": " + votosMesa);
            }
            
            // Contar por candidatos
            long[] candidatos = {101, 102, 103};
            for (long candidato : candidatos) {
                long votosCandidato = receptorVotos.contarVotosPorCandidato(candidato);
                System.out.println("📊 Votos para candidato " + candidato + ": " + votosCandidato);
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error consultando estadísticas: " + e.getMessage());
        }
    }
    
    private static void prueba6_ConsultarVotosPorMesa() {
        System.out.println("\n--- Prueba 6: Consultar Votos por Mesa ---");
        
        try {
            String mesaPrueba = "9060";
            Demo.VotoRegional[] votosMesa = receptorVotos.obtenerVotosPorMesa(mesaPrueba);
            
            System.out.println("🗳️ Votos en mesa " + mesaPrueba + ": " + votosMesa.length);
            
            for (int i = 0; i < Math.min(votosMesa.length, 5); i++) {
                Demo.VotoRegional voto = votosMesa[i];
                System.out.println("   Voto " + (i+1) + ":");
                System.out.println("     ID: " + voto.idVoto);
                System.out.println("     Candidato: " + voto.candidatoId);
                System.out.println("     Hash: " + voto.hashElector);
                System.out.println("     Timestamp: " + voto.timestamp);
                System.out.println("     Estado: " + voto.estadoRegistro);
            }
            
            if (votosMesa.length > 5) {
                System.out.println("   ... y " + (votosMesa.length - 5) + " votos más");
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error consultando votos por mesa: " + e.getMessage());
        }
    }
    
    private static void prueba7_ConsultarVotosPorCandidato() {
        System.out.println("\n--- Prueba 7: Consultar Votos por Candidato ---");
        
        try {
            long candidatoPrueba = 101;
            Demo.VotoRegional[] votosCandidato = receptorVotos.obtenerVotosPorCandidato(candidatoPrueba);
            
            System.out.println("🏆 Votos para candidato " + candidatoPrueba + ": " + votosCandidato.length);
            
            for (int i = 0; i < Math.min(votosCandidato.length, 3); i++) {
                Demo.VotoRegional voto = votosCandidato[i];
                System.out.println("   Voto " + (i+1) + ":");
                System.out.println("     ID: " + voto.idVoto);
                System.out.println("     Mesa: " + voto.mesaId);
                System.out.println("     Hash: " + voto.hashElector);
                System.out.println("     Municipio: " + voto.municipio);
                System.out.println("     Departamento: " + voto.departamento);
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error consultando votos por candidato: " + e.getMessage());
        }
    }
    
    private static void prueba8_VerificarExistenciaVotos() {
        System.out.println("\n--- Prueba 8: Verificar Existencia de Votos ---");
        
        try {
            // Verificar voto que debería existir
            long idVotoExistente = 1001;
            boolean existe1 = receptorVotos.existeVoto(idVotoExistente);
            System.out.println("🔍 Voto ID " + idVotoExistente + " existe: " + existe1);
            
            // Verificar voto que no debería existir
            long idVotoInexistente = 9999;
            boolean existe2 = receptorVotos.existeVoto(idVotoInexistente);
            System.out.println("🔍 Voto ID " + idVotoInexistente + " existe: " + existe2);
            
            // Verificar por hash y mesa
            boolean existeHash1 = receptorVotos.existeVotoPorHash("hash001", "9060");
            System.out.println("🔍 Voto con hash 'hash001' en mesa '9060' existe: " + existeHash1);
            
            boolean existeHash2 = receptorVotos.existeVotoPorHash("hash_inexistente", "9060");
            System.out.println("🔍 Voto con hash 'hash_inexistente' en mesa '9060' existe: " + existeHash2);
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error verificando existencia: " + e.getMessage());
        }
    }
    
    private static void prueba9_EstadisticasDetalladas() {
        System.out.println("\n--- Prueba 9: Estadísticas Detalladas ---");
        
        try {
            String estadisticas = receptorVotos.obtenerEstadisticasDetalladas();
            System.out.println("📋 Estadísticas detalladas:");
            System.out.println(estadisticas);
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error obteniendo estadísticas detalladas: " + e.getMessage());
        }
    }
    
    private static void prueba10_PruebaDuplicados() {
        System.out.println("\n--- Prueba 10: Prueba de Duplicados ---");
        
        try {
            // Intentar enviar el mismo voto dos veces
            Demo.VotoRegional votoDuplicado = crearVotoPrueba(1001, "9060", 101, "hash001");
            
            System.out.println("📤 Intentando enviar voto duplicado (ID: " + votoDuplicado.idVoto + ")");
            boolean recibido = receptorVotos.recibirVoto(votoDuplicado);
            
            System.out.println("📊 Resultado envío duplicado: " + recibido);
            System.out.println("💡 (Debería actualizarse, no crear duplicado)");
            
            // Verificar conteo total
            long totalVotos = receptorVotos.contarVotosAlmacenados();
            System.out.println("📊 Total votos después de duplicado: " + totalVotos);
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error probando duplicados: " + e.getMessage());
        }
    }
    
    /**
     * Crea un voto de prueba
     */
    private static Demo.VotoRegional crearVotoPrueba(long idVoto, String mesaId, long candidatoId, String hashElector) {
        Demo.VotoRegional voto = new Demo.VotoRegional();
        voto.idVoto = idVoto;
        voto.mesaId = mesaId;
        voto.candidatoId = candidatoId;
        voto.hashElector = hashElector;
        voto.timestamp = System.currentTimeMillis();
        voto.municipio = "Cali";
        voto.departamento = "Valle del Cauca";
        voto.estadoRegistro = "NUEVO";
        
        return voto;
    }
    
    private static void mostrarSeparador() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
} 