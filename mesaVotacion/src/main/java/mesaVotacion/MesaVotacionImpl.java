package mesaVotacion;

import Demo.*;
import com.zeroc.Ice.Current;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Implementación de la interfaz IMesaVotacion
 * Permite recibir archivos SQLite remotamente desde el servidor regional
 */
public class MesaVotacionImpl implements IMesaVotacion {
    
    private final String mesaId;
    private SistemaVerificacion sistemaVerificacion;
    
    public MesaVotacionImpl(String mesaId) {
        this.mesaId = mesaId;
        System.out.println("🗳️ Implementación de Mesa " + mesaId + " inicializada");
    }
    
    @Override
    public boolean recibirVotantesAsignados(CiudadanoInfo[] votantes, String departamento, Current current) {
        System.out.println("📥 Recibiendo " + votantes.length + " votantes para " + departamento);
        // TODO: Implementar si es necesario
        return true;
    }
    
    @Override
    public boolean recibirArchivoSQLite(byte[] datosArchivo, String nombreArchivo, Current current) {
        try {
            System.out.println("📥 Recibiendo archivo SQLite: " + nombreArchivo + " (" + datosArchivo.length + " bytes)");
            
            // Crear directorio data si no existe
            java.io.File dataDir = new java.io.File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                System.out.println("📁 Directorio 'data' creado");
            }
            
            // Escribir archivo
            String rutaArchivo = "data/" + nombreArchivo;
            Files.write(Paths.get(rutaArchivo), datosArchivo);
            
            System.out.println("✅ Archivo SQLite guardado: " + rutaArchivo);
            
            // Inicializar sistema de verificación con el nuevo archivo
            try {
                sistemaVerificacion = new SistemaVerificacion(mesaId);
                System.out.println("✅ Sistema de verificación actualizado");
            } catch (Exception e) {
                System.err.println("⚠️ Error inicializando sistema de verificación: " + e.getMessage());
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error guardando archivo SQLite: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean estaListaParaRecibir(Current current) {
        System.out.println("📋 Verificando si mesa está lista para recibir datos...");
        
        // Verificar que el directorio data existe o se puede crear
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            boolean creado = dataDir.mkdirs();
            if (!creado) {
                System.err.println("❌ No se puede crear directorio 'data'");
                return false;
            }
        }
        
        System.out.println("✅ Mesa " + mesaId + " lista para recibir datos");
        return true;
    }
    
    @Override
    public boolean verificarVotanteEnMesa(String documento, Current current) {
        if (sistemaVerificacion == null) {
            System.err.println("❌ Sistema de verificación no inicializado");
            return false;
        }
        
        return sistemaVerificacion.verificarVotante(documento);
    }
    
    @Override
    public CiudadanoInfo obtenerVotanteDeMesa(String documento, Current current) {
        if (sistemaVerificacion == null) {
            System.err.println("❌ Sistema de verificación no inicializado");
            return null;
        }
        
        return sistemaVerificacion.obtenerInformacionVotante(documento);
    }
    
    @Override
    public EstadisticasMesa obtenerEstadisticasMesa(Current current) {
        // TODO: Implementar conversión desde String a EstadisticasMesa
        EstadisticasMesa stats = new EstadisticasMesa();
        stats.mesaId = mesaId;
        stats.departamento = "N/A";
        stats.municipio = "N/A";
        stats.puesto = "N/A";
        stats.votantesAsignados = 0;
        stats.votantesVerificados = 0;
        stats.mesaActiva = true;
        stats.ultimaActualizacion = System.currentTimeMillis();
        
        return stats;
    }
    
    @Override
    public int contarVotantesEnMesa(Current current) {
        if (sistemaVerificacion == null) {
            return 0;
        }
        
        // Contar usando consulta SQL directa
        try {
            return sistemaVerificacion.listarVotantesMesa(0); // 0 = contar todos
        } catch (Exception e) {
            System.err.println("❌ Error contando votantes: " + e.getMessage());
            return 0;
        }
    }
    
    @Override
    public boolean limpiarDatosMesa(Current current) {
        try {
            String archivoPath = "data/mesa_" + mesaId.replaceAll("[^a-zA-Z0-9]", "_") + ".db";
            java.io.File archivo = new java.io.File(archivoPath);
            
            if (archivo.exists() && archivo.delete()) {
                System.out.println("🗑️ Datos de mesa limpiados: " + archivoPath);
                sistemaVerificacion = null;
                return true;
            } else {
                System.out.println("⚠️ No hay datos para limpiar");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error limpiando datos: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean verificarEstadoMesa(Current current) {
        boolean sistemaOK = sistemaVerificacion != null;
        boolean archivoOK = false;
        
        try {
            String archivoPath = "data/mesa_" + mesaId.replaceAll("[^a-zA-Z0-9]", "_") + ".db";
            java.io.File archivo = new java.io.File(archivoPath);
            archivoOK = archivo.exists();
        } catch (Exception e) {
            // archivo no OK
        }
        
        boolean estadoOK = sistemaOK && archivoOK;
        
        System.out.println("📊 Estado Mesa " + mesaId + ":");
        System.out.println("   Sistema verificación: " + (sistemaOK ? "✅" : "❌"));
        System.out.println("   Archivo SQLite: " + (archivoOK ? "✅" : "❌"));
        System.out.println("   Estado general: " + (estadoOK ? "✅ OPERATIVO" : "❌ NO OPERATIVO"));
        
        return estadoOK;
    }
    
    @Override
    public String obtenerIdMesa(Current current) {
        return mesaId;
    }
    
    /**
     * Obtiene el sistema de verificación (para uso interno)
     */
    public SistemaVerificacion getSistemaVerificacion() {
        return sistemaVerificacion;
    }
} 