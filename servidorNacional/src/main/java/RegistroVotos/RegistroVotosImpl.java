package RegistroVotos;

import Demo.*;
import com.zeroc.Ice.Current;
import Database.DatabaseManager;
import Database.VotosDatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de registro de votos
 * Utiliza la base de datos de votos
 */
public class RegistroVotosImpl implements IRegistroVotos {
    
    private final DatabaseManager dbManager;
    private final VotosDatabaseConnection dbConnection;
    
    public RegistroVotosImpl() {
        this.dbManager = DatabaseManager.getInstance();
        this.dbConnection = dbManager.getVotosConnection();
        
        System.out.println("🗳️  RegistroVotos inicializado");
        System.out.println("   📊 Base de datos: votos");
        
        // Verificar conexión inicial
        if (verificarConexionBD(null)) {
            System.out.println("   ✅ Conexión a BD de votos: OK");
        } else {
            System.out.println("   ⚠️  Conexión a BD de votos: FALLO");
        }
    }
    
    @Override
    public boolean registrarVoto(VotoCompleto voto, Current current) {
        System.out.println("📝 Registrando voto individual - Mesa: " + voto.mesaId + 
                          ", Candidato: " + voto.candidatoId);
        
        // Validar datos del voto
        if (!validarVoto(voto)) {
            System.err.println("❌ Voto inválido - datos incompletos");
            return false;
        }
        
        // Verificar si el voto ya existe por hash
        if (existeVotoPorHash(voto.hashVerificacion, null)) {
            System.err.println("❌ Voto duplicado - hash ya existe: " + voto.hashVerificacion);
            return false;
        }
        
        String sql = "INSERT INTO voto (id, mesa_id, timestamp, candidato_id, hash_verificacion, municipio, departamento) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                System.err.println("❌ No se pudo obtener conexión a la base de datos");
                return false;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, voto.id);
                stmt.setString(2, voto.mesaId);
                stmt.setTimestamp(3, new Timestamp(voto.timestamp));
                stmt.setLong(4, voto.candidatoId);
                stmt.setString(5, voto.hashVerificacion);
                stmt.setString(6, voto.municipio);
                stmt.setString(7, voto.departamento);
                
                int filasAfectadas = stmt.executeUpdate();
                
                long endTime = System.currentTimeMillis();
                
                if (filasAfectadas > 0) {
                    System.out.println("✅ Voto registrado exitosamente en " + 
                                     (endTime - startTime) + "ms");
                    return true;
                } else {
                    System.err.println("❌ No se pudo registrar el voto");
                    return false;
                }
            }
            
        } catch (SQLException e) {
            long endTime = System.currentTimeMillis();
            System.err.println("❌ Error registrando voto (" + (endTime - startTime) + "ms): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public ResultadoRegistroVotos registrarVotosLote(VotoCompleto[] votos, Current current) {
        System.out.println("📦 Registrando lote de " + votos.length + " votos...");
        
        ResultadoRegistroVotos resultado = new ResultadoRegistroVotos();
        resultado.totalVotos = votos.length;
        resultado.votosRegistrados = 0;
        resultado.votosRechazados = 0;
        resultado.exito = false;
        
        long startTime = System.currentTimeMillis();
        
        String sql = "INSERT INTO voto (id, mesa_id, timestamp, candidato_id, hash_verificacion, municipio, departamento) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                resultado.mensaje = "No se pudo obtener conexión a la base de datos";
                return resultado;
            }
            
            // Usar transacción para el lote
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                for (VotoCompleto voto : votos) {
                    try {
                        // Validar voto
                        if (!validarVoto(voto)) {
                            resultado.votosRechazados++;
                            System.err.println("⚠️  Voto rechazado - datos inválidos: " + voto.id);
                            continue;
                        }
                        
                        // Verificar duplicados
                        if (existeVotoPorHash(voto.hashVerificacion, null)) {
                            resultado.votosRechazados++;
                            System.err.println("⚠️  Voto rechazado - hash duplicado: " + voto.hashVerificacion);
                            continue;
                        }
                        
                        // Preparar statement
                        stmt.setLong(1, voto.id);
                        stmt.setString(2, voto.mesaId);
                        stmt.setTimestamp(3, new Timestamp(voto.timestamp));
                        stmt.setLong(4, voto.candidatoId);
                        stmt.setString(5, voto.hashVerificacion);
                        stmt.setString(6, voto.municipio);
                        stmt.setString(7, voto.departamento);
                        
                        stmt.addBatch();
                        resultado.votosRegistrados++;
                        
                    } catch (Exception e) {
                        resultado.votosRechazados++;
                        System.err.println("⚠️  Error procesando voto " + voto.id + ": " + e.getMessage());
                    }
                }
                
                // Ejecutar lote
                if (resultado.votosRegistrados > 0) {
                    int[] resultados = stmt.executeBatch();
                    dbConnection.commit();
                    
                    long endTime = System.currentTimeMillis();
                    resultado.tiempoProcessamiento = endTime - startTime;
                    resultado.exito = true;
                    resultado.mensaje = "Lote procesado exitosamente";
                    
                    System.out.println("✅ Lote registrado: " + resultado.votosRegistrados + 
                                     " votos en " + resultado.tiempoProcessamiento + "ms");
                } else {
                    dbConnection.rollback();
                    resultado.mensaje = "No hay votos válidos para registrar";
                }
                
            } catch (SQLException e) {
                dbConnection.rollback();
                resultado.mensaje = "Error en transacción: " + e.getMessage();
                System.err.println("❌ Error en lote de votos: " + e.getMessage());
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            resultado.mensaje = "Error de conexión: " + e.getMessage();
            System.err.println("❌ Error de conexión en lote: " + e.getMessage());
            e.printStackTrace();
        }
        
        resultado.tiempoProcessamiento = System.currentTimeMillis() - startTime;
        return resultado;
    }
    
    @Override
    public boolean existeVotoPorHash(String hashVerificacion, Current current) {
        if (hashVerificacion == null || hashVerificacion.trim().isEmpty()) {
            return false;
        }
        
        String sql = "SELECT COUNT(*) FROM voto WHERE hash_verificacion = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                return false;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, hashVerificacion.trim());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("⚠️  Error verificando hash: " + e.getMessage());
        }
        
        return false;
    }
    
    @Override
    public long contarVotosPorMesa(String mesaId, Current current) {
        if (mesaId == null || mesaId.trim().isEmpty()) {
            return 0;
        }
        
        String sql = "SELECT COUNT(*) FROM voto WHERE mesa_id = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                return 0;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, mesaId.trim());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long count = rs.getLong(1);
                        System.out.println("📊 Votos en mesa " + mesaId + ": " + count);
                        return count;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error contando votos por mesa: " + e.getMessage());
        }
        
        return 0;
    }
    
    @Override
    public long contarVotosPorCandidato(long candidatoId, Current current) {
        String sql = "SELECT COUNT(*) FROM voto WHERE candidato_id = ?";
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                return 0;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, candidatoId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long count = rs.getLong(1);
                        System.out.println("📊 Votos para candidato " + candidatoId + ": " + count);
                        return count;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error contando votos por candidato: " + e.getMessage());
        }
        
        return 0;
    }
    
    @Override
    public long contarVotosPorMunicipio(String municipio, Current current) {
        if (municipio == null || municipio.trim().isEmpty()) {
            return 0;
        }
        
        String sql = "SELECT COUNT(*) FROM voto WHERE LOWER(municipio) = LOWER(?)";
        
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                return 0;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, municipio.trim());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long count = rs.getLong(1);
                        System.out.println("📊 Votos en municipio " + municipio + ": " + count);
                        return count;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error contando votos por municipio: " + e.getMessage());
        }
        
        return 0;
    }
    
    @Override
    public boolean verificarConexionBD(Current current) {
        try (Connection conn = dbConnection.getConnection()) {
            if (conn == null) {
                return false;
            }
            
            // Verificar que la tabla voto existe
            String sql = "SELECT COUNT(*) FROM votos LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("⚠️  Error verificando conexión BD votos: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validar que un voto tenga todos los datos requeridos
     */
    private boolean validarVoto(VotoCompleto voto) {
        if (voto == null) {
            return false;
        }
        
        // Validar campos obligatorios
        if (voto.id <= 0) {
            System.err.println("⚠️  ID de voto inválido: " + voto.id);
            return false;
        }
        
        if (voto.mesaId == null || voto.mesaId.trim().isEmpty()) {
            System.err.println("⚠️  Mesa ID vacío");
            return false;
        }
        
        if (voto.candidatoId <= 0) {
            System.err.println("⚠️  Candidato ID inválido: " + voto.candidatoId);
            return false;
        }
        
        if (voto.hashVerificacion == null || voto.hashVerificacion.trim().isEmpty()) {
            System.err.println("⚠️  Hash de verificación vacío");
            return false;
        }
        
        if (voto.municipio == null || voto.municipio.trim().isEmpty()) {
            System.err.println("⚠️  Municipio vacío");
            return false;
        }
        
        if (voto.departamento == null || voto.departamento.trim().isEmpty()) {
            System.err.println("⚠️  Departamento vacío");
            return false;
        }
        
        if (voto.timestamp <= 0) {
            System.err.println("⚠️  Timestamp inválido: " + voto.timestamp);
            return false;
        }
        
        return true;
    }
    
    /**
     * Método para cerrar el servicio de forma limpia
     */
    public void shutdown() {
        System.out.println("🛑 RegistroVotos: Cerrando servicio...");
        System.out.println("✅ RegistroVotos: Servicio cerrado");
    }
} 