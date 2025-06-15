package Services;

import Database.DatabaseConnection;
import Models.CandidatoModel;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Servicio para gestionar candidatos
 * Maneja la carga desde CSV/Excel y operaciones de base de datos
 */
public class CandidatosService {
    
    private final DatabaseConnection dbConnection;
    private final AtomicLong nextGeneratedId;
    
    public CandidatosService() {
        this.dbConnection = new DatabaseConnection();
        this.nextGeneratedId = new AtomicLong(100000); // IDs generados empiezan en 100000
        System.out.println("🗳️  CandidatosService inicializado");
    }
    
    /**
     * Carga candidatos desde archivo CSV
     * @param csvFile Archivo CSV a procesar
     * @return Lista de candidatos procesados
     * @throws Exception Si hay error en el procesamiento
     */
    public List<CandidatoModel> cargarCandidatosDesdeCSV(File csvFile) throws Exception {
        List<CandidatoModel> candidatos = new ArrayList<>();
        Set<Long> idsExistentes = obtenerIdsExistentes();
        
        System.out.println("📄 Procesando archivo CSV: " + csvFile.getName());
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String linea;
            int numeroLinea = 0;
            
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                
                // Saltar líneas vacías o de encabezado
                if (linea.trim().isEmpty() || numeroLinea == 1) {
                    continue;
                }
                
                try {
                    CandidatoModel candidato = procesarLineaCSV(linea, idsExistentes);
                    if (candidato != null && candidato.isValid()) {
                        candidatos.add(candidato);
                        idsExistentes.add(candidato.getId());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error procesando línea " + numeroLinea + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("✅ CSV procesado: " + candidatos.size() + " candidatos válidos");
        return candidatos;
    }
    
    /**
     * Carga candidatos desde archivo Excel
     * @param excelFile Archivo Excel a procesar
     * @return Lista de candidatos procesados
     * @throws Exception Si hay error en el procesamiento
     */
    public List<CandidatoModel> cargarCandidatosDesdeExcel(File excelFile) throws Exception {
        List<CandidatoModel> candidatos = new ArrayList<>();
        Set<Long> idsExistentes = obtenerIdsExistentes();
        
        System.out.println("📊 Procesando archivo Excel: " + excelFile.getName());
        
        try (FileInputStream fis = new FileInputStream(excelFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0); // Primera hoja
            
            for (Row row : sheet) {
                // Saltar fila de encabezado
                if (row.getRowNum() == 0) {
                    continue;
                }
                
                try {
                    CandidatoModel candidato = procesarFilaExcel(row, idsExistentes);
                    if (candidato != null && candidato.isValid()) {
                        candidatos.add(candidato);
                        idsExistentes.add(candidato.getId());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️  Error procesando fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("✅ Excel procesado: " + candidatos.size() + " candidatos válidos");
        return candidatos;
    }
    
    /**
     * Procesa una línea CSV y crea un candidato
     * @param linea Línea CSV a procesar
     * @param idsExistentes Set de IDs ya existentes
     * @return CandidatoModel procesado
     */
    private CandidatoModel procesarLineaCSV(String linea, Set<Long> idsExistentes) {
        String[] campos = linea.split(",");
        
        if (campos.length < 3) {
            throw new IllegalArgumentException("Línea CSV inválida: debe tener 3 campos (id, nombre, partido)");
        }
        
        try {
            Long id = Long.parseLong(campos[0].trim());
            String nombre = campos[1].trim();
            String partido = campos[2].trim();
            
            // Verificar si el ID ya existe
            if (idsExistentes.contains(id)) {
                Long nuevoId = generarIdUnico(idsExistentes);
                System.out.println("⚠️  ID duplicado " + id + " para " + nombre + ", generando nuevo ID: " + nuevoId);
                return new CandidatoModel(nuevoId, nombre, partido, true);
            }
            
            return new CandidatoModel(id, nombre, partido, false);
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID inválido en línea CSV: " + campos[0]);
        }
    }
    
    /**
     * Procesa una fila Excel y crea un candidato
     * @param row Fila Excel a procesar
     * @param idsExistentes Set de IDs ya existentes
     * @return CandidatoModel procesado
     */
    private CandidatoModel procesarFilaExcel(Row row, Set<Long> idsExistentes) {
        if (row.getPhysicalNumberOfCells() < 3) {
            throw new IllegalArgumentException("Fila Excel inválida: debe tener 3 columnas (id, nombre, partido)");
        }
        
        try {
            Long id = (long) row.getCell(0).getNumericCellValue();
            String nombre = row.getCell(1).getStringCellValue().trim();
            String partido = row.getCell(2).getStringCellValue().trim();
            
            // Verificar si el ID ya existe
            if (idsExistentes.contains(id)) {
                Long nuevoId = generarIdUnico(idsExistentes);
                System.out.println("⚠️  ID duplicado " + id + " para " + nombre + ", generando nuevo ID: " + nuevoId);
                return new CandidatoModel(nuevoId, nombre, partido, true);
            }
            
            return new CandidatoModel(id, nombre, partido, false);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error procesando fila Excel: " + e.getMessage());
        }
    }
    
    /**
     * Genera un ID único que no esté en el conjunto de IDs existentes
     * @param idsExistentes Set de IDs ya existentes
     * @return ID único generado
     */
    private Long generarIdUnico(Set<Long> idsExistentes) {
        Long nuevoId;
        do {
            nuevoId = nextGeneratedId.getAndIncrement();
        } while (idsExistentes.contains(nuevoId));
        
        return nuevoId;
    }
    
    /**
     * Obtiene todos los IDs de candidatos existentes en la base de datos
     * @return Set de IDs existentes
     */
    private Set<Long> obtenerIdsExistentes() {
        Set<Long> ids = new HashSet<>();
        Connection conn = null;
        
        try {
            conn = dbConnection.getConnection();
            if (conn == null) {
                System.err.println("⚠️  No se pudo conectar a la BD principal, continuando sin validar IDs existentes");
                return ids;
            }
            
            String sql = "SELECT id FROM candidato";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo IDs existentes: " + e.getMessage());
        } finally {
            if (conn != null) {
                dbConnection.returnConnection(conn);
            }
        }
        
        return ids;
    }
    
    /**
     * Guarda una lista de candidatos en la base de datos
     * @param candidatos Lista de candidatos a guardar
     * @return Número de candidatos guardados exitosamente
     */
    public int guardarCandidatos(List<CandidatoModel> candidatos) {
        if (candidatos == null || candidatos.isEmpty()) {
            return 0;
        }
        
        Connection conn = null;
        int guardados = 0;
        
        try {
            conn = dbConnection.getConnection();
            if (conn == null) {
                throw new SQLException("No se pudo conectar a la base de datos principal");
            }
            
            // Usar INSERT simple ya que eliminamos todos los candidatos antes
            String sql = "INSERT INTO candidato (id, nombre, partido) VALUES (?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                for (CandidatoModel candidato : candidatos) {
                    if (candidato.isValid()) {
                        stmt.setLong(1, candidato.getId());
                        stmt.setString(2, candidato.getNombre());
                        stmt.setString(3, candidato.getPartido());
                        
                        int resultado = stmt.executeUpdate();
                        if (resultado > 0) {
                            guardados++;
                        }
                    }
                }
                
                conn.commit();
                System.out.println("✅ Candidatos guardados en BD: " + guardados + "/" + candidatos.size());
                
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error guardando candidatos: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("❌ Error en rollback: " + rollbackEx.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                dbConnection.returnConnection(conn);
            }
        }
        
        return guardados;
    }
    
    /**
     * Obtiene todos los candidatos de la base de datos
     * @return Lista de candidatos
     */
    public List<CandidatoModel> obtenerTodosLosCandidatos() {
        List<CandidatoModel> candidatos = new ArrayList<>();
        Connection conn = null;
        
        try {
            conn = dbConnection.getConnection();
            if (conn == null) {
                throw new SQLException("No se pudo conectar a la base de datos principal");
            }
            
            String sql = "SELECT id, nombre, partido FROM candidato ORDER BY id";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    CandidatoModel candidato = new CandidatoModel(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("partido")
                    );
                    candidatos.add(candidato);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error obteniendo candidatos: " + e.getMessage());
        } finally {
            if (conn != null) {
                dbConnection.returnConnection(conn);
            }
        }
        
        return candidatos;
    }
    
    /**
     * Verifica si el servicio de base de datos está disponible
     * @return true si está disponible
     */
    public boolean isServiceAvailable() {
        return dbConnection.isServiceActive();
    }
    
    /**
     * Borra todos los candidatos de la base de datos
     * @return true si se eliminaron candidatos exitosamente
     */
    public boolean eliminarTodosLosCandidatos() {
        String sql = "DELETE FROM candidato";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int borrados = stmt.executeUpdate();
            System.out.println("🗑️ " + borrados + " candidatos borrados de la base de datos");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error borrando todos los candidatos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Actualiza un candidato existente en la base de datos
     * @param candidato candidato con los datos actualizados
     * @return true si se actualizó correctamente
     */
    public boolean actualizarCandidato(CandidatoModel candidato) {
        String sql = "UPDATE candidato SET nombre = ?, partido = ? WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, candidato.getNombre());
            stmt.setString(2, candidato.getPartido());
            stmt.setLong(3, candidato.getId());
            
            int filasAfectadas = stmt.executeUpdate();
            boolean exito = filasAfectadas > 0;
            
            if (exito) {
                System.out.println("✅ Candidato actualizado: " + candidato.getNombre() + " (ID: " + candidato.getId() + ")");
            } else {
                System.out.println("⚠️ No se encontró candidato con ID: " + candidato.getId());
            }
            
            return exito;
            
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando candidato: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Elimina un candidato de la base de datos
     * @param idCandidato ID del candidato a eliminar
     * @return true si se eliminó correctamente
     */
    public boolean eliminarCandidato(Long idCandidato) {
        String sql = "DELETE FROM candidato WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, idCandidato);
            
            int filasAfectadas = stmt.executeUpdate();
            boolean exito = filasAfectadas > 0;
            
            if (exito) {
                System.out.println("✅ Candidato eliminado con ID: " + idCandidato);
            } else {
                System.out.println("⚠️ No se encontró candidato con ID: " + idCandidato);
            }
            
            return exito;
            
        } catch (SQLException e) {
            System.err.println("❌ Error eliminando candidato: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 