package ServidorNacionalUI;

import Demo.*;
import AdministradorCandidatos.AdministradorCandidatos;
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
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServidorNacionalUI extends JFrame {

    private AdministradorCandidatos administradorCandidatos;
    private JTable tablaCandidatos;
    private DefaultTableModel modeloTabla;
    private JLabel lblEstado;
    private JLabel lblCantidadCandidatos;
    private JButton btnCargarCSV;
    private JButton btnLimpiarDatos;
    private JButton btnActualizar;
    private JButton btnEnviarATodos;
    private JButton btnEnviarARegional;
    private JTextArea txtLog;
    private JScrollPane scrollLog;
    private JTextField txtEndpointRegional;

    public ServidorNacionalUI(AdministradorCandidatos administradorCandidatos) {
        this.administradorCandidatos = administradorCandidatos;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        actualizarInterfaz();
    }

    private void initializeComponents() {
        setTitle("Servidor Nacional - Administración de Candidatos");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("No se pudo configurar Look and Feel: " + e.getMessage());
        }

        lblEstado = new JLabel("Sistema Nacional iniciado - Sin datos cargados");
        lblEstado.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        lblEstado.setOpaque(true);
        lblEstado.setBackground(Color.YELLOW);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        lblCantidadCandidatos = new JLabel("Candidatos: 0");
        lblCantidadCandidatos.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        btnCargarCSV = new JButton("📁 Cargar CSV");
        btnCargarCSV.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnCargarCSV.setBackground(new Color(76, 175, 80));
        btnCargarCSV.setForeground(Color.BLACK);
        btnCargarCSV.setFocusPainted(false);

        btnLimpiarDatos = new JButton("🗑️ Limpiar Datos");
        btnLimpiarDatos.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnLimpiarDatos.setBackground(new Color(244, 67, 54));
        btnLimpiarDatos.setForeground(Color.BLACK);
        btnLimpiarDatos.setFocusPainted(false);

        btnActualizar = new JButton("🔄 Actualizar");
        btnActualizar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnActualizar.setBackground(new Color(33, 150, 243));
        btnActualizar.setForeground(Color.BLACK);
        btnActualizar.setFocusPainted(false);

        btnEnviarATodos = new JButton("📤 Enviar a Todos");
        btnEnviarATodos.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnEnviarATodos.setBackground(new Color(156, 39, 176));
        btnEnviarATodos.setForeground(Color.BLACK);
        btnEnviarATodos.setFocusPainted(false);

        btnEnviarARegional = new JButton("📨 Enviar a Regional");
        btnEnviarARegional.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnEnviarARegional.setBackground(new Color(255, 152, 0));
        btnEnviarARegional.setForeground(Color.BLACK);
        btnEnviarARegional.setFocusPainted(false);

        txtEndpointRegional = new JTextField("IRecibirCandidatos:tcp -h localhost -p 10000", 30);
        txtEndpointRegional.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        // Tabla de candidatos
        String[] columnNames = {"ID", "Nombre", "Partido"};
        modeloTabla = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaCandidatos = new JTable(modeloTabla);
        tablaCandidatos.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tablaCandidatos.setRowHeight(25);
        tablaCandidatos.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        tablaCandidatos.getTableHeader().setBackground(new Color(63, 81, 181));
        tablaCandidatos.getTableHeader().setForeground(Color.WHITE);

        // Configurar ancho de columnas
        tablaCandidatos.getColumnModel().getColumn(0).setPreferredWidth(60);
        tablaCandidatos.getColumnModel().getColumn(1).setPreferredWidth(300);
        tablaCandidatos.getColumnModel().getColumn(2).setPreferredWidth(200);

        // Área de log
        txtLog = new JTextArea(10, 50);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        txtLog.setEditable(false);
        txtLog.setBackground(Color.BLACK);
        txtLog.setForeground(Color.GREEN);
        scrollLog = new JScrollPane(txtLog);
        scrollLog.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        agregarLogMessage("Servidor Nacional UI iniciado");
        agregarLogMessage("Use 'Cargar CSV' para importar candidatos");
        agregarLogMessage("Use 'Enviar a Todos' para distribuir a servidores regionales");
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Panel superior - Estado
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        panelSuperior.add(lblEstado, BorderLayout.CENTER);

        JPanel panelEstadisticas = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelEstadisticas.add(lblCantidadCandidatos);
        panelSuperior.add(panelEstadisticas, BorderLayout.EAST);

        // Panel de botones principales
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelBotones.add(btnCargarCSV);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnLimpiarDatos);

        // Panel de envío
        JPanel panelEnvio = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelEnvio.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Distribución a Servidores Regionales",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 11)
        ));

        panelEnvio.add(btnEnviarATodos);
        panelEnvio.add(new JLabel("Endpoint Específico:"));
        panelEnvio.add(txtEndpointRegional);
        panelEnvio.add(btnEnviarARegional);

        // Panel central - Tabla
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Candidatos Registrados",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12)
        ));

        JScrollPane scrollTabla = new JScrollPane(tablaCandidatos);
        scrollTabla.setPreferredSize(new Dimension(900, 300));
        panelCentral.add(scrollTabla, BorderLayout.CENTER);

        // Panel inferior - Log
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Log del Sistema",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 12)
        ));
        panelInferior.add(scrollLog, BorderLayout.CENTER);

        // Añadir paneles al frame
        add(panelSuperior, BorderLayout.NORTH);

        JPanel panelControles = new JPanel(new BorderLayout());
        panelControles.add(panelBotones, BorderLayout.NORTH);
        panelControles.add(panelEnvio, BorderLayout.SOUTH);
        add(panelControles, BorderLayout.BEFORE_FIRST_LINE);

        add(panelCentral, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        btnCargarCSV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cargarArchivoCSV();
            }
        });

        btnLimpiarDatos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                limpiarDatos();
            }
        });

        btnActualizar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarInterfaz();
            }
        });

        btnEnviarATodos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarATodosRegionales();
            }
        });

        btnEnviarARegional.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarARegionalEspecifico();
            }
        });

        // Control de cierre de ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int opcion = JOptionPane.showConfirmDialog(
                        ServidorNacionalUI.this,
                        "¿Está seguro de que desea cerrar el servidor nacional?",
                        "Confirmar cierre",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (opcion == JOptionPane.YES_OPTION) {
                    agregarLogMessage("Cerrando servidor nacional...");
                    System.exit(0);
                }
            }
        });
    }

    private void cargarArchivoCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar archivo CSV de candidatos");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int resultado = fileChooser.showOpenDialog(this);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivoSeleccionado = fileChooser.getSelectedFile();
            agregarLogMessage("Cargando archivo: " + archivoSeleccionado.getName());

            btnCargarCSV.setEnabled(false);
            btnCargarCSV.setText("Cargando...");

            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    try {
                        return administradorCandidatos.cargarCandidatosDesdeCSV(
                                archivoSeleccionado.getAbsolutePath(), null);
                    } catch (Exception e) {
                        agregarLogMessage("Error cargando CSV: " + e.getMessage());
                        return false;
                    }
                }

                @Override
                protected void done() {
                    try {
                        boolean exito = get();

                        if (exito) {
                            agregarLogMessage("Archivo CSV cargado exitosamente");
                            actualizarInterfaz();
                        } else {
                            agregarLogMessage("Error cargando archivo CSV");
                            JOptionPane.showMessageDialog(
                                    ServidorNacionalUI.this,
                                    "Error cargando el archivo CSV.\nVerifique el formato y contenido.",
                                    "Error de carga",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }

                    } catch (Exception e) {
                        agregarLogMessage("Excepción cargando CSV: " + e.getMessage());
                    } finally {
                        btnCargarCSV.setEnabled(true);
                        btnCargarCSV.setText("📁 Cargar CSV");
                    }
                }
            };

            worker.execute();
        }
    }

    private void limpiarDatos() {
        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de que desea limpiar todos los datos de candidatos?",
                "Confirmar limpieza",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (opcion == JOptionPane.YES_OPTION) {
            boolean exito = administradorCandidatos.limpiarCandidatos(null);

            if (exito) {
                agregarLogMessage("Datos de candidatos limpiados");
                actualizarInterfaz();
            } else {
                agregarLogMessage("Error limpiando datos");
            }
        }
    }

    private void enviarATodosRegionales() {
        if (administradorCandidatos.obtenerCantidadCandidatos(null) == 0) {
            JOptionPane.showMessageDialog(this,
                    "No hay candidatos para enviar.\nPrimero cargue datos desde un archivo CSV.",
                    "Sin datos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnEnviarATodos.setEnabled(false);
        btnEnviarATodos.setText("Enviando...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                agregarLogMessage("Iniciando envío a todos los servidores regionales...");
                return administradorCandidatos.enviarCandidatosATodosRegionales(null);
            }

            @Override
            protected void done() {
                try {
                    boolean exito = get();
                    if (exito) {
                        agregarLogMessage("Candidatos enviados exitosamente a todos los regionales");
                        JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                                "Candidatos enviados exitosamente a todos los servidores regionales",
                                "Envío completado",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        agregarLogMessage("Error enviando candidatos a algunos servidores regionales");
                        JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                                "Error enviando candidatos a algunos servidores regionales.\nRevise el log para más detalles.",
                                "Error de envío",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    agregarLogMessage("Excepción enviando candidatos: " + e.getMessage());
                    JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                            "Error inesperado enviando candidatos: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnEnviarATodos.setEnabled(true);
                    btnEnviarATodos.setText("📤 Enviar a Todos");
                }
            }
        };

        worker.execute();
    }

    private void enviarARegionalEspecifico() {
        String endpoint = txtEndpointRegional.getText().trim();

        if (endpoint.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe especificar un endpoint para el servidor regional",
                    "Endpoint requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (administradorCandidatos.obtenerCantidadCandidatos(null) == 0) {
            JOptionPane.showMessageDialog(this,
                    "No hay candidatos para enviar.\nPrimero cargue datos desde un archivo CSV.",
                    "Sin datos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnEnviarARegional.setEnabled(false);
        btnEnviarARegional.setText("Enviando...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                agregarLogMessage("Enviando candidatos a endpoint: " + endpoint);
                return administradorCandidatos.enviarCandidatosARegional(endpoint, null);
            }

            @Override
            protected void done() {
                try {
                    boolean exito = get();
                    if (exito) {
                        agregarLogMessage("Candidatos enviados exitosamente a: " + endpoint);
                        JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                                "Candidatos enviados exitosamente al servidor regional",
                                "Envío completado",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        agregarLogMessage("Error enviando candidatos a: " + endpoint);
                        JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                                "Error enviando candidatos al servidor regional.\nVerifique el endpoint y la conectividad.",
                                "Error de envío",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    agregarLogMessage("Excepción enviando a regional: " + e.getMessage());
                    JOptionPane.showMessageDialog(ServidorNacionalUI.this,
                            "Error inesperado enviando candidatos: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnEnviarARegional.setEnabled(true);
                    btnEnviarARegional.setText("📨 Enviar a Regional");
                }
            }
        };

        worker.execute();
    }

    private void actualizarInterfaz() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Actualizar contador de candidatos
                int cantidadCandidatos = administradorCandidatos.obtenerCantidadCandidatos(null);
                lblCantidadCandidatos.setText("Candidatos: " + cantidadCandidatos);

                // Actualizar estado
                if (cantidadCandidatos > 0) {
                    lblEstado.setText("Sistema Nacional - " + cantidadCandidatos + " candidatos cargados");
                    lblEstado.setBackground(new Color(76, 175, 80));
                    lblEstado.setForeground(Color.WHITE);
                } else {
                    lblEstado.setText("Sistema Nacional iniciado - Sin datos cargados");
                    lblEstado.setBackground(Color.YELLOW);
                    lblEstado.setForeground(Color.BLACK);
                }

                // Limpiar tabla
                modeloTabla.setRowCount(0);

                // Cargar datos en la tabla
                if (cantidadCandidatos > 0) {
                    Candidato[] candidatos = administradorCandidatos.obtenerTodosCandidatos(null);
                    for (Candidato candidato : candidatos) {
                        Object[] fila = {
                                candidato.idCandidato,
                                candidato.nombre,
                                candidato.partido
                        };
                        modeloTabla.addRow(fila);
                    }
                }

                // Habilitar/deshabilitar botones según estado
                boolean hayDatos = cantidadCandidatos > 0;
                btnEnviarATodos.setEnabled(hayDatos);
                btnEnviarARegional.setEnabled(hayDatos);
                btnLimpiarDatos.setEnabled(hayDatos);

                agregarLogMessage("Interfaz actualizada - " + cantidadCandidatos + " candidatos");

            } catch (Exception e) {
                agregarLogMessage("Error actualizando interfaz: " + e.getMessage());
            }
        });
    }

    private void agregarLogMessage(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String logEntry = "[" + timestamp + "] " + mensaje + "\n";
            txtLog.append(logEntry);
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    // Método para mostrar la ventana
    public void mostrar() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            toFront();
            requestFocus();
        });
    }

    // Método para obtener referencia al administrador
    public AdministradorCandidatos getAdministradorCandidatos() {
        return administradorCandidatos;
    }
}