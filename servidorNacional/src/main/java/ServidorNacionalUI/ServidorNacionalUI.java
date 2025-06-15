package ServidorNacionalUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// ICE imports
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

// Imports del proyecto
import Demo.*;
import Services.CandidatosService;
import Models.CandidatoModel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServidorNacionalUI extends JFrame {
    
    // Componentes principales
    private final CandidatosService candidatosService;
    
    // Componentes UI principales
    private JTable tablaCandidatos;
    private DefaultTableModel modeloTabla;
    private JLabel labelCantidadCandidatos;
    private JTextField campoRutaCSV;
    private JTextArea areaLog;
    private JLabel lblEstadoCandidatos;
    
    // Scheduler para actualizaciones automáticas
    private final ScheduledExecutorService schedulerUI;

    /**
     * Constructor principal
     */
    public ServidorNacionalUI() {
        this.candidatosService = new CandidatosService();
        this.schedulerUI = Executors.newScheduledThreadPool(1);
        
        // Configurar la ventana principal
        configurarVentana();
        
        // Inicializar la interfaz
        initializeUI();
        
        // Cargar candidatos iniciales
        cargarCandidatosDesdeDB();
        
        log("🎯 Servidor Nacional UI inicializado correctamente");
    }

    private void configurarVentana() {
        setTitle("🎯 Servidor Nacional - Gestión de Candidatos");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Configurar cierre personalizado
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                cerrarAplicacion();
            }
        });
    }

    private void initializeUI() {
        // Layout principal con pestañas
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Pestaña 1: Gestión de Candidatos
        tabbedPane.addTab("👥 Candidatos", crearPanelCandidatos());
        
        // Pestaña 2: Logs del Sistema
        tabbedPane.addTab("📝 Logs", crearPanelLogs());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Panel de estado en la parte inferior
        add(crearPanelEstado(), BorderLayout.SOUTH);
    }

    private JPanel crearPanelCandidatos() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Panel superior con controles
        JPanel panelControles = new JPanel(new GridBagLayout());
        panelControles.setBorder(new TitledBorder("🗳️ Gestión de Candidatos"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Fila 1 - Selección de archivo
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panelControles.add(new JLabel("Archivo:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        campoRutaCSV = new JTextField(40);
        panelControles.add(campoRutaCSV, gbc);
        
        gbc.gridx = 2; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton btnExaminar = new JButton("📁 Examinar");
        panelControles.add(btnExaminar, gbc);
        
        // Fila 2 - Botones principales
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JButton btnCargarCSV = new JButton("📄 Cargar CSV");
        btnCargarCSV.setBackground(new Color(76, 175, 80));
        btnCargarCSV.setForeground(Color.WHITE);
        btnCargarCSV.setPreferredSize(new Dimension(150, 35));
        panelControles.add(btnCargarCSV, gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        JButton btnCargarExcel = new JButton("📊 Cargar Excel");
        btnCargarExcel.setBackground(new Color(33, 150, 243));
        btnCargarExcel.setForeground(Color.WHITE);
        btnCargarExcel.setPreferredSize(new Dimension(150, 35));
        panelControles.add(btnCargarExcel, gbc);
        
        gbc.gridx = 2; gbc.gridy = 1;
        JButton btnEliminarTodo = new JButton("🗑️ Eliminar Todo");
        btnEliminarTodo.setBackground(new Color(244, 67, 54));
        btnEliminarTodo.setForeground(Color.WHITE);
        btnEliminarTodo.setPreferredSize(new Dimension(150, 35));
        panelControles.add(btnEliminarTodo, gbc);
        
        // Fila 3 - Estado
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        lblEstadoCandidatos = new JLabel("Listo para cargar candidatos");
        lblEstadoCandidatos.setFont(new Font("Arial", Font.ITALIC, 12));
        lblEstadoCandidatos.setForeground(new Color(102, 102, 102));
        panelControles.add(lblEstadoCandidatos, gbc);
        
        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 1;
        labelCantidadCandidatos = new JLabel("Candidatos: 0");
        labelCantidadCandidatos.setFont(new Font("Arial", Font.BOLD, 12));
        panelControles.add(labelCantidadCandidatos, gbc);

        // Tabla de candidatos EDITABLE y sincronizada
        String[] columnas = {"ID", "Nombre", "Partido"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Solo permitir editar Nombre y Partido (columnas 1 y 2)
                return column == 1 || column == 2;
            }
            
            @Override
            public void setValueAt(Object value, int row, int column) {
                super.setValueAt(value, row, column);
                // Guardar automáticamente cuando se edite
                guardarCambioEnBD(row, column, value);
            }
        };

        tablaCandidatos = new JTable(modeloTabla);
        tablaCandidatos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaCandidatos.getTableHeader().setReorderingAllowed(false);
        tablaCandidatos.setRowHeight(25);
        tablaCandidatos.setShowGrid(true);
        tablaCandidatos.setGridColor(new Color(230, 230, 230));
        
        // Menú contextual para eliminar
        JPopupMenu menuContextual = new JPopupMenu();
        JMenuItem eliminarItem = new JMenuItem("🗑️ Eliminar Candidato");
        eliminarItem.addActionListener(e -> eliminarCandidatoSeleccionado());
        menuContextual.add(eliminarItem);
        tablaCandidatos.setComponentPopupMenu(menuContextual);
        
        JScrollPane scrollTabla = new JScrollPane(tablaCandidatos);
        scrollTabla.setBorder(new TitledBorder("📋 Candidatos - Doble clic para editar | Clic derecho para eliminar"));
        scrollTabla.setPreferredSize(new Dimension(800, 400));
        
        // Event listeners
        btnExaminar.addActionListener(e -> examinarArchivoCandidatos());
        btnCargarCSV.addActionListener(e -> cargarCSV());
        btnCargarExcel.addActionListener(e -> cargarExcel());
        btnEliminarTodo.addActionListener(e -> eliminarTodosLosCandidatos());
        
        panel.add(panelControles, BorderLayout.NORTH);
        panel.add(scrollTabla, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel crearPanelLogs() {
        JPanel panel = new JPanel(new BorderLayout());
        
        areaLog = new JTextArea(20, 80);
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaLog.setBackground(Color.BLACK);
        areaLog.setForeground(Color.GREEN);
        
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(new TitledBorder("Registro de Eventos"));
        
        JPanel panelControlesLog = new JPanel(new FlowLayout());
        JButton btnLimpiarLog = new JButton("🗑️ Limpiar Log");
        btnLimpiarLog.addActionListener(e -> areaLog.setText(""));
        panelControlesLog.add(btnLimpiarLog);
        
        panel.add(scrollLog, BorderLayout.CENTER);
        panel.add(panelControlesLog, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel crearPanelEstado() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        JLabel labelEstado = new JLabel("🎯 Servidor Nacional - Estado: ACTIVO");
        labelEstado.setFont(labelEstado.getFont().deriveFont(Font.BOLD));
        
        // Mostrar estado de la base de datos
        JLabel labelBD = new JLabel();
        if (candidatosService.isServiceAvailable()) {
            labelBD.setText("🟢 Base de Datos: CONECTADA");
            labelBD.setForeground(new Color(76, 175, 80));
        } else {
            labelBD.setText("🔴 Base de Datos: DESCONECTADA");
            labelBD.setForeground(new Color(244, 67, 54));
        }
        
        panel.add(labelEstado);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(labelBD);
        
        return panel;
    }

    // ========== EVENT HANDLERS ==========
    
    private void examinarArchivoCandidatos() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Archivos de Candidatos (*.csv, *.xlsx)", "csv", "xlsx", "xls");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Seleccionar archivo de candidatos");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            campoRutaCSV.setText(archivo.getAbsolutePath());
            log("📁 Archivo seleccionado: " + archivo.getName());
        }
    }
    
    private void cargarCSV() {
        String rutaArchivo = campoRutaCSV.getText().trim();
        
        if (rutaArchivo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor selecciona un archivo CSV", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            JOptionPane.showMessageDialog(this, "El archivo no existe", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Confirmación
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Cargar candidatos desde CSV?\n\n" +
            "⚠️ ESTO ELIMINARÁ TODOS los candidatos existentes\n" +
            "y los reemplazará con los del archivo CSV.\n\n" +
            "¿Continuar?",
            "Confirmar Carga CSV", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        cargarArchivoEnBackground(archivo, "CSV");
    }
    
    private void cargarExcel() {
        String rutaArchivo = campoRutaCSV.getText().trim();
        
        if (rutaArchivo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor selecciona un archivo Excel", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            JOptionPane.showMessageDialog(this, "El archivo no existe", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Confirmación
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Cargar candidatos desde Excel?\n\n" +
            "⚠️ ESTO ELIMINARÁ TODOS los candidatos existentes\n" +
            "y los reemplazará con los del archivo Excel.\n\n" +
            "¿Continuar?",
            "Confirmar Carga Excel", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        cargarArchivoEnBackground(archivo, "Excel");
    }
    
    private void cargarArchivoEnBackground(File archivo, String tipo) {
        SwingWorker<Boolean, String> worker = new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                publish("Eliminando candidatos existentes...");
                candidatosService.eliminarTodosLosCandidatos();
                
                publish("Cargando candidatos desde " + tipo + "...");
                List<CandidatoModel> candidatos;
                
                if (tipo.equals("CSV")) {
                    candidatos = candidatosService.cargarCandidatosDesdeCSV(archivo);
                } else {
                    candidatos = candidatosService.cargarCandidatosDesdeExcel(archivo);
                }
                
                if (!candidatos.isEmpty()) {
                    publish("Guardando candidatos en base de datos...");
                    int guardados = candidatosService.guardarCandidatos(candidatos);
                    return guardados > 0;
                }
                return false;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String mensaje : chunks) {
                    log(mensaje);
                    lblEstadoCandidatos.setText(mensaje);
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean exito = get();
                    if (exito) {
                        lblEstadoCandidatos.setText("✅ " + tipo + " cargado exitosamente");
                        lblEstadoCandidatos.setForeground(new Color(76, 175, 80));
                        log("✅ " + tipo + " cargado y guardado exitosamente");
                        JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                            tipo + " cargado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        lblEstadoCandidatos.setText("❌ Error cargando " + tipo);
                        lblEstadoCandidatos.setForeground(new Color(244, 67, 54));
                        log("❌ Error cargando " + tipo);
                    }
                    // Actualizar tabla
                    cargarCandidatosDesdeDB();
                } catch (Exception e) {
                    lblEstadoCandidatos.setText("❌ Error cargando " + tipo);
                    lblEstadoCandidatos.setForeground(new Color(244, 67, 54));
                    log("❌ Error cargando " + tipo + ": " + e.getMessage());
                    JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                        "Error cargando " + tipo + ":\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
    
    private void eliminarTodosLosCandidatos() {
        int confirmacion = JOptionPane.showConfirmDialog(this, 
            "¿Estás seguro de que quieres eliminar TODOS los candidatos?\n\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Eliminación Total", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return candidatosService.eliminarTodosLosCandidatos();
                }
                
                @Override
                protected void done() {
                    try {
                        boolean exito = get();
                        if (exito) {
                            log("✅ Todos los candidatos eliminados exitosamente");
                            lblEstadoCandidatos.setText("✅ Base de datos limpiada");
                            lblEstadoCandidatos.setForeground(new Color(76, 175, 80));
                            JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                                "Todos los candidatos eliminados exitosamente", 
                                "Eliminación Completada", JOptionPane.INFORMATION_MESSAGE);
                            // Actualizar tabla
                            cargarCandidatosDesdeDB();
                        } else {
                            log("❌ Error eliminando candidatos");
                            lblEstadoCandidatos.setText("❌ Error eliminando candidatos");
                            lblEstadoCandidatos.setForeground(new Color(244, 67, 54));
                            JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                                "Error eliminando candidatos", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        log("❌ Error eliminando candidatos: " + e.getMessage());
                        lblEstadoCandidatos.setText("❌ Error eliminando candidatos");
                        lblEstadoCandidatos.setForeground(new Color(244, 67, 54));
                        JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                            "Error eliminando candidatos:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
    
    private void cargarCandidatosDesdeDB() {
        SwingWorker<List<CandidatoModel>, Void> worker = new SwingWorker<List<CandidatoModel>, Void>() {
            @Override
            protected List<CandidatoModel> doInBackground() throws Exception {
                return candidatosService.obtenerTodosLosCandidatos();
            }
            
            @Override
            protected void done() {
                try {
                    List<CandidatoModel> candidatos = get();
                    actualizarTablaCandidatos(candidatos);
                    log("🔄 Tabla sincronizada con BD: " + candidatos.size() + " candidatos");
                } catch (Exception e) {
                    log("❌ Error cargando desde BD: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void actualizarTablaCandidatos(List<CandidatoModel> candidatos) {
        // Limpiar tabla
        modeloTabla.setRowCount(0);
        
        if (candidatos != null) {
            for (CandidatoModel candidato : candidatos) {
                Object[] fila = {
                    candidato.getId(),
                    candidato.getNombre(),
                    candidato.getPartido()
                };
                modeloTabla.addRow(fila);
            }
        }
        
        labelCantidadCandidatos.setText("Candidatos: " + (candidatos != null ? candidatos.size() : 0));
    }
    
    private void cerrarAplicacion() {
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Estás seguro de que quieres cerrar la aplicación?",
            "Confirmar Cierre", JOptionPane.YES_NO_OPTION);
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            log("🛑 Cerrando Servidor Nacional UI");
            
            if (schedulerUI != null && !schedulerUI.isShutdown()) {
                schedulerUI.shutdown();
            }
            
            dispose();
            System.exit(0);
        }
    }
    
    private void log(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
                String logEntry = String.format("[%s] %s%n", timestamp, mensaje);
                
                // Verificar que el área de log esté inicializada
                if (areaLog != null) {
                    areaLog.append(logEntry);
                    areaLog.setCaretPosition(areaLog.getDocument().getLength());
                    
                    // Limitar el tamaño del log (mantener solo las últimas 500 líneas)
                    String texto = areaLog.getText();
                    String[] lineas = texto.split("\n");
                    if (lineas.length > 500) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = lineas.length - 500; i < lineas.length; i++) {
                            sb.append(lineas[i]).append("\n");
                        }
                        areaLog.setText(sb.toString());
                        areaLog.setCaretPosition(areaLog.getDocument().getLength());
                    }
                } else {
                    // Si el área de log no está inicializada, solo imprimir en consola
                    System.out.println("[UI-LOG] " + logEntry.trim());
                }
            } catch (Exception e) {
                // Fallback a consola si hay error con la UI
                System.err.println("[UI-LOG-ERROR] " + mensaje);
                e.printStackTrace();
            }
        });
        
        // También imprimir en consola para debug
        System.out.println("[UI] " + mensaje);
    }

    private void guardarCambioEnBD(int row, int column, Object nuevoValor) {
        // Obtener ID del candidato desde la tabla
        Long idCandidato = (Long) modeloTabla.getValueAt(row, 0);
        String valorAnterior = (String) modeloTabla.getValueAt(row, column);
        
        // Crear candidato temporal para actualizar
        CandidatoModel candidato = new CandidatoModel();
        candidato.setId(idCandidato);
        candidato.setNombre((String) modeloTabla.getValueAt(row, 1));
        candidato.setPartido((String) modeloTabla.getValueAt(row, 2));
        
        // Actualizar el campo correspondiente
        if (column == 1) { // Nombre
            candidato.setNombre(nuevoValor.toString().trim());
        } else if (column == 2) { // Partido
            candidato.setPartido(nuevoValor.toString().trim());
        } else {
            return; // Columna no editable
        }
        
        // Guardar en BD
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return candidatosService.actualizarCandidato(candidato);
            }
            
            @Override
            protected void done() {
                try {
                    boolean exito = get();
                    if (exito) {
                        String campo = column == 1 ? "nombre" : "partido";
                        log("✅ Candidato actualizado: " + campo + " = '" + nuevoValor + "' (ID: " + idCandidato + ")");
                        lblEstadoCandidatos.setText("✅ Cambio guardado");
                        lblEstadoCandidatos.setForeground(new Color(76, 175, 80));
                    } else {
                        log("❌ Error actualizando candidato ID: " + idCandidato);
                        lblEstadoCandidatos.setText("❌ Error guardando cambio");
                        lblEstadoCandidatos.setForeground(new Color(244, 67, 54));
                        
                        // Revertir cambio en la tabla
                        modeloTabla.setValueAt(valorAnterior, row, column);
                        
                        JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                            "Error guardando el cambio. Se ha revertido el valor anterior.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    log("❌ Excepción actualizando candidato: " + e.getMessage());
                    lblEstadoCandidatos.setText("❌ Error guardando cambio");
                    lblEstadoCandidatos.setForeground(new Color(244, 67, 54));
                    
                    // Revertir cambio en la tabla
                    modeloTabla.setValueAt(valorAnterior, row, column);
                }
            }
        };
        
        worker.execute();
    }
    
    private void eliminarCandidatoSeleccionado() {
        int filaSeleccionada = tablaCandidatos.getSelectedRow();
        
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this,
                "Por favor selecciona un candidato para eliminar",
                "Ningún candidato seleccionado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Obtener datos del candidato desde la tabla
        Long idCandidato = (Long) modeloTabla.getValueAt(filaSeleccionada, 0);
        String nombre = (String) modeloTabla.getValueAt(filaSeleccionada, 1);
        String partido = (String) modeloTabla.getValueAt(filaSeleccionada, 2);
        
        // Confirmación
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Estás seguro de eliminar el candidato?\n\n" +
            "ID: " + idCandidato + "\n" +
            "Nombre: " + nombre + "\n" +
            "Partido: " + partido + "\n\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Eliminar de BD
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return candidatosService.eliminarCandidato(idCandidato);
            }
            
            @Override
            protected void done() {
                try {
                    boolean exito = get();
                    if (exito) {
                        // Eliminar de la tabla
                        modeloTabla.removeRow(filaSeleccionada);
                        
                        // Actualizar contador
                        labelCantidadCandidatos.setText("Candidatos: " + modeloTabla.getRowCount());
                        
                        log("✅ Candidato eliminado: " + nombre + " (ID: " + idCandidato + ")");
                        lblEstadoCandidatos.setText("✅ Candidato eliminado");
                        lblEstadoCandidatos.setForeground(new Color(76, 175, 80));
                        
                    } else {
                        log("❌ Error eliminando candidato ID: " + idCandidato);
                        lblEstadoCandidatos.setText("❌ Error eliminando candidato");
                        lblEstadoCandidatos.setForeground(new Color(244, 67, 54));
                        
                        JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                            "Error eliminando el candidato",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    log("❌ Excepción eliminando candidato: " + e.getMessage());
                    lblEstadoCandidatos.setText("❌ Error eliminando candidato");
                    lblEstadoCandidatos.setForeground(new Color(244, 67, 54));
                    
                    JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                        "Error eliminando candidato:\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }

    /**
     * Método main para ejecutar la interfaz directamente
     */
    public static void main(String[] args) {
        try {
            // Configurar Look and Feel del sistema
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("No se pudo configurar Look and Feel: " + e.getMessage());
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("🎯 Iniciando Servidor Nacional UI...");
                
                // Crear y mostrar la interfaz
                ServidorNacionalUI ui = new ServidorNacionalUI();
                ui.setVisible(true);
                
                System.out.println("✅ Interfaz del Servidor Nacional iniciada correctamente");
                
            } catch (Exception e) {
                System.err.println("❌ Error iniciando la interfaz: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "Error iniciando la aplicación:\n" + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}