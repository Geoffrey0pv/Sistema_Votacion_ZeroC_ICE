package servidorRegional;

import Demo.IConsultaCandidatos;
import Demo.CandidatoElectoral;
import com.zeroc.Ice.Current;
import java.util.*;
import java.util.stream.Collectors;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementación del servicio de consulta de candidatos para el servidor regional
 * Expone los candidatos almacenados localmente para que otros componentes puedan acceder a ellos
 */
public class ConsultaCandidatosImpl implements IConsultaCandidatos {
    
    private final GestorCandidatosSQLite gestorCandidatos;
    private final String nombreServidor;
    private final String DB_URL = "jdbc:sqlite:data/candidatos_regional.sqlite";
    
    public ConsultaCandidatosImpl(GestorCandidatosSQLite gestorCandidatos, String nombreServidor) {
        this.gestorCandidatos = gestorCandidatos;
        this.nombreServidor = nombreServidor != null ? nombreServidor : "ServidorRegional";
        System.out.println("✅ Servicio ConsultaCandidatos inicializado en " + this.nombreServidor);
    }
    
    /**
     * Obtiene una conexión directa a la base de datos
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    @Override
    public CandidatoElectoral[] obtenerTodosCandidatosElectorales(Current current) {
        try {
            System.out.println("📋 Consultando todos los candidatos electorales...");
            
            try (Connection conn = getConnection()) {
                List<CandidatoElectoral> candidatos = new ArrayList<>();
                String sql = "SELECT id, nombre, partido, fecha_creacion, activo FROM candidatos_electorales ORDER BY partido, nombre";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        CandidatoElectoral candidato = new CandidatoElectoral();
                        candidato.id = rs.getLong("id");
                        candidato.nombre = rs.getString("nombre");
                        candidato.partido = rs.getString("partido");
                        candidato.fechaCreacion = rs.getString("fecha_creacion");
                        candidato.activo = rs.getBoolean("activo");
                        candidatos.add(candidato);
                    }
                }
                
                System.out.println("✅ Consultados " + candidatos.size() + " candidatos");
                return candidatos.toArray(new CandidatoElectoral[0]);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error al consultar candidatos: " + e.getMessage());
            e.printStackTrace();
            return new CandidatoElectoral[0];
        }
    }
    
    @Override
    public CandidatoElectoral[] obtenerCandidatosPorPartido(String partido, Current current) {
        try {
            System.out.println("🎯 Consultando candidatos del partido: " + partido);
            
            try (Connection conn = getConnection()) {
                List<CandidatoElectoral> candidatos = new ArrayList<>();
                String sql = "SELECT id, nombre, partido, fecha_creacion, activo FROM candidatos_electorales WHERE partido = ? AND activo = 1 ORDER BY nombre";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, partido);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            CandidatoElectoral candidato = new CandidatoElectoral();
                            candidato.id = rs.getLong("id");
                            candidato.nombre = rs.getString("nombre");
                            candidato.partido = rs.getString("partido");
                            candidato.fechaCreacion = rs.getString("fecha_creacion");
                            candidato.activo = rs.getBoolean("activo");
                            candidatos.add(candidato);
                        }
                    }
                }
                
                System.out.println("✅ Encontrados " + candidatos.size() + " candidatos del partido " + partido);
                return candidatos.toArray(new CandidatoElectoral[0]);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error al consultar candidatos por partido: " + e.getMessage());
            e.printStackTrace();
            return new CandidatoElectoral[0];
        }
    }
    
    @Override
    public long contarCandidatos(Current current) {
        try {
            try (Connection conn = getConnection()) {
                String sql = "SELECT COUNT(*) as total FROM candidatos_electorales WHERE activo = 1";
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    if (rs.next()) {
                        long total = rs.getLong("total");
                        System.out.println("📊 Total de candidatos activos: " + total);
                        return total;
                    }
                }
            }
            
            return 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error al contar candidatos: " + e.getMessage());
            return 0;
        }
    }
    
    @Override
    public boolean verificarConexionBD(Current current) {
        try {
            try (Connection conn = getConnection()) {
                boolean conectado = conn != null && !conn.isClosed();
                System.out.println(conectado ? "✅ Conexión BD activa" : "❌ Sin conexión BD");
                return conectado;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error verificando conexión: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public CandidatoElectoral buscarCandidatoPorId(long idCandidato, Current current) {
        try {
            System.out.println("🔍 Buscando candidato ID: " + idCandidato);
            
            try (Connection conn = getConnection()) {
                String sql = "SELECT id, nombre, partido, fecha_creacion, activo FROM candidatos_electorales WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, idCandidato);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            CandidatoElectoral candidato = new CandidatoElectoral();
                            candidato.id = rs.getLong("id");
                            candidato.nombre = rs.getString("nombre");
                            candidato.partido = rs.getString("partido");
                            candidato.fechaCreacion = rs.getString("fecha_creacion");
                            candidato.activo = rs.getBoolean("activo");
                            
                            System.out.println("✅ Candidato encontrado: " + candidato.nombre);
                            return candidato;
                        }
                    }
                }
            }
            
            System.out.println("⚠️ Candidato no encontrado");
            return null;
            
        } catch (SQLException e) {
            System.err.println("❌ Error al buscar candidato: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public CandidatoElectoral[] buscarCandidatosPorNombre(String nombre, Current current) {
        try {
            System.out.println("🔍 Buscando candidatos por nombre: " + nombre);
            
            try (Connection conn = getConnection()) {
                List<CandidatoElectoral> candidatos = new ArrayList<>();
                String sql = "SELECT id, nombre, partido, fecha_creacion, activo FROM candidatos_electorales " +
                            "WHERE nombre LIKE ? AND activo = 1 ORDER BY nombre";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String patron = "%" + nombre + "%";
                    stmt.setString(1, patron);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            CandidatoElectoral candidato = new CandidatoElectoral();
                            candidato.id = rs.getLong("id");
                            candidato.nombre = rs.getString("nombre");
                            candidato.partido = rs.getString("partido");
                            candidato.fechaCreacion = rs.getString("fecha_creacion");
                            candidato.activo = rs.getBoolean("activo");
                            candidatos.add(candidato);
                        }
                    }
                }
                
                System.out.println("✅ Encontrados " + candidatos.size() + " candidatos con nombre '" + nombre + "'");
                return candidatos.toArray(new CandidatoElectoral[0]);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error al buscar candidatos por nombre: " + e.getMessage());
            return new CandidatoElectoral[0];
        }
    }
    
    @Override
    public String[] obtenerPartidosDisponibles(Current current) {
        try {
            System.out.println("🏛️ Consultando partidos disponibles...");
            
            try (Connection conn = getConnection()) {
                Set<String> partidos = new HashSet<>();
                String sql = "SELECT DISTINCT partido FROM candidatos_electorales WHERE activo = 1 ORDER BY partido";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        String partido = rs.getString("partido");
                        if (partido != null && !partido.trim().isEmpty()) {
                            partidos.add(partido);
                        }
                    }
                }
                
                String[] resultado = partidos.toArray(new String[0]);
                System.out.println("✅ Encontrados " + resultado.length + " partidos: " + Arrays.toString(resultado));
                return resultado;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error al consultar partidos: " + e.getMessage());
            return new String[0];
        }
    }
    
    @Override
    public boolean sincronizarCandidatos(Current current) {
        try {
            System.out.println("🔄 Iniciando sincronización de candidatos...");
            // Usar el método público del gestor para sincronizar
            boolean sincronizado = gestorCandidatos.forzarSincronizacion();
            
            System.out.println(sincronizado ? 
                "✅ Sincronización exitosa" : 
                "⚠️ Error en sincronización");
            
            return sincronizado;
            
        } catch (Exception e) {
            System.err.println("❌ Error en sincronización: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean validarCandidato(long idCandidato, Current current) {
        try {
            CandidatoElectoral candidato = buscarCandidatoPorId(idCandidato, current);
            boolean valido = candidato != null && candidato.activo;
            
            System.out.println("🔍 Validación candidato ID " + idCandidato + ": " + 
                (valido ? "✅ VÁLIDO" : "❌ INVÁLIDO"));
            
            return valido;
            
        } catch (Exception e) {
            System.err.println("❌ Error validando candidato: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public CandidatoElectoral[] obtenerCandidatosParaMesa(String mesaId, Current current) {
        try {
            System.out.println("🗳️ Consultando candidatos para mesa: " + mesaId);
            
            // Por ahora retornamos todos los candidatos activos
            // En el futuro se podría filtrar por región/departamento de la mesa
            CandidatoElectoral[] candidatos = obtenerTodosCandidatosElectorales(current);
            
            // Filtrar solo candidatos activos
            List<CandidatoElectoral> candidatosActivos = Arrays.stream(candidatos)
                .filter(c -> c.activo)
                .collect(Collectors.toList());
            
            System.out.println("✅ " + candidatosActivos.size() + " candidatos disponibles para mesa " + mesaId);
            return candidatosActivos.toArray(new CandidatoElectoral[0]);
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo candidatos para mesa: " + e.getMessage());
            return new CandidatoElectoral[0];
        }
    }
    
    @Override
    public boolean verificarServicio(Current current) {
        try {
            boolean servicioActivo = verificarConexionBD(current);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            System.out.println("🔧 Verificación de servicio [" + timestamp + "]: " + 
                (servicioActivo ? "✅ ACTIVO" : "❌ INACTIVO"));
            
            return servicioActivo;
            
        } catch (Exception e) {
            System.err.println("❌ Error verificando servicio: " + e.getMessage());
            return false;
        }
    }
} 