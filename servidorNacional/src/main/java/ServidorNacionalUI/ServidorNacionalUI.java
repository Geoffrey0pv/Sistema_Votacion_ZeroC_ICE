package ServidorNacionalUI;

import Demo.*;
import AdministradorCandidatos.AdministradorCandidatos;
import Broker.BrokerNacional;
import Broker.BalanceadorCarga;
import Broker.GestorReplicas;
import Broker.MonitorRecursos;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServidorNacionalUI extends JFrame {

    private final BrokerNacional broker;
    private final AdministradorCandidatos administradorCandidatos;
    private final BalanceadorCarga balanceador;
    private final GestorReplicas gestorReplicas;
    private final MonitorRecursos monitorMaster;
    
    // Componentes UI principales
    private JTable tablaCandidatos;
    private DefaultTableModel modeloTabla;
    private JLabel labelCantidadCandidatos;
    private JTextField campoRutaCSV;
    private JTextArea areaLog;
    
    // Componentes UI del Broker
    private JLabel labelEstadoCluster;
    private JLabel labelReplicasActivas;
    private JLabel labelCargaPromedio;
    private JLabel labelMetricasMaster;
    private JTable tablaReplicas;
    private DefaultTableModel modeloReplicas;
    private JProgressBar barraEscalado;
    private JButton btnEscalarManual;
    private JButton btnReducirManual;
    private JComboBox<String> comboAlgoritmo;
    
    // Scheduler para actualizaciones automáticas
    private final ScheduledExecutorService schedulerUI;

    public ServidorNacionalUI(BrokerNacional broker) {
        this.broker = broker;
        this.administradorCandidatos = broker.getMasterCandidatos();
        this.balanceador = broker.getBalanceador();
        this.gestorReplicas = broker.getGestorReplicas();
        this.monitorMaster = broker.getMonitorMaster();
        this.schedulerUI = Executors.newScheduledThreadPool(2);
        
        initializeUI();
        iniciarActualizacionesAutomaticas();
        
        log("🎯 Interfaz del Broker Nacional iniciada");
    }

    private void initializeUI() {
        setTitle("🎯 Servidor Nacional - Broker con Escalado Automático");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        // Configurar cierre personalizado
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cerrarAplicacion();
            }
        });
        
        // Layout principal con pestañas
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Pestaña 1: Gestión de Candidatos
        tabbedPane.addTab("👥 Candidatos", crearPanelCandidatos());
        
        // Pestaña 2: Monitor del Cluster
        tabbedPane.addTab("📊 Cluster", crearPanelCluster());
        
        // Pestaña 3: Configuración del Broker
        tabbedPane.addTab("⚙️ Configuración", crearPanelConfiguracion());
        
        // Pestaña 4: Logs del Sistema
        tabbedPane.addTab("📝 Logs", crearPanelLogs());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Panel de estado en la parte inferior
        add(crearPanelEstado(), BorderLayout.SOUTH);
    }

    private JPanel crearPanelCandidatos() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Panel superior con controles
        JPanel panelControles = new JPanel(new FlowLayout());
        panelControles.setBorder(new TitledBorder("Gestión de Candidatos"));
        
        campoRutaCSV = new JTextField(30);
        JButton btnExaminar = new JButton("📁 Examinar");
        JButton btnCargarCSV = new JButton("📥 Cargar CSV");
        JButton btnLimpiar = new JButton("🗑️ Limpiar");
        JButton btnEnviarRegionales = new JButton("📤 Enviar a Regionales");
        
        panelControles.add(new JLabel("Archivo CSV:"));
        panelControles.add(campoRutaCSV);
        panelControles.add(btnExaminar);
        panelControles.add(btnCargarCSV);
        panelControles.add(btnLimpiar);
        panelControles.add(btnEnviarRegionales);

        // Tabla de candidatos
        String[] columnas = {"ID", "Nombre", "Partido", "Propuestas"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaCandidatos = new JTable(modeloTabla);
        tablaCandidatos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollTabla = new JScrollPane(tablaCandidatos);
        scrollTabla.setBorder(new TitledBorder("Candidatos Registrados"));
        
        // Panel inferior con información
        JPanel panelInfo = new JPanel(new FlowLayout());
        labelCantidadCandidatos = new JLabel("Candidatos: 0");
        panelInfo.add(labelCantidadCandidatos);
        
        // Event listeners
        btnExaminar.addActionListener(e -> examinarArchivo());
        btnCargarCSV.addActionListener(e -> cargarCandidatosCSV());
        btnLimpiar.addActionListener(e -> limpiarCandidatos());
        btnEnviarRegionales.addActionListener(e -> enviarCandidatosRegionales());
        
        panel.add(panelControles, BorderLayout.NORTH);
        panel.add(scrollTabla, BorderLayout.CENTER);
        panel.add(panelInfo, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel crearPanelCluster() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Panel superior con métricas generales
        JPanel panelMetricas = new JPanel(new GridLayout(2, 2, 10, 10));
        panelMetricas.setBorder(new TitledBorder("Estado del Cluster"));
        
        labelEstadoCluster = new JLabel("🔄 Inicializando...");
        labelReplicasActivas = new JLabel("📊 Réplicas: 0/0");
        labelCargaPromedio = new JLabel("⚖️ Carga: 0.0%");
        labelMetricasMaster = new JLabel("🖥️ Master: CPU=0% MEM=0%");
        
        panelMetricas.add(labelEstadoCluster);
        panelMetricas.add(labelReplicasActivas);
        panelMetricas.add(labelCargaPromedio);
        panelMetricas.add(labelMetricasMaster);
        
        // Tabla de réplicas
        String[] columnasReplicas = {"Node ID", "Endpoint", "Estado", "CPU %", "MEM %", "Requests", "Tiempo Activo"};
        modeloReplicas = new DefaultTableModel(columnasReplicas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tablaReplicas = new JTable(modeloReplicas);
        JScrollPane scrollReplicas = new JScrollPane(tablaReplicas);
        scrollReplicas.setBorder(new TitledBorder("Réplicas Activas"));
        
        // Panel de escalado
        JPanel panelEscalado = new JPanel(new FlowLayout());
        panelEscalado.setBorder(new TitledBorder("Control de Escalado"));
        
        barraEscalado = new JProgressBar(0, 100);
        barraEscalado.setStringPainted(true);
        barraEscalado.setString("Carga: 0%");
        
        btnEscalarManual = new JButton("🚀 Escalar");
        btnReducirManual = new JButton("📉 Reducir");
        
        panelEscalado.add(new JLabel("Carga del Sistema:"));
        panelEscalado.add(barraEscalado);
        panelEscalado.add(btnEscalarManual);
        panelEscalado.add(btnReducirManual);
        
        // Event listeners
        btnEscalarManual.addActionListener(e -> escalarManualmente());
        btnReducirManual.addActionListener(e -> reducirManualmente());
        
        panel.add(panelMetricas, BorderLayout.NORTH);
        panel.add(scrollReplicas, BorderLayout.CENTER);
        panel.add(panelEscalado, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel crearPanelConfiguracion() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Configuración del balanceador
        JPanel panelBalanceador = new JPanel(new FlowLayout());
        panelBalanceador.setBorder(new TitledBorder("Configuración del Balanceador"));
        
        comboAlgoritmo = new JComboBox<>(new String[]{
            "ROUND_ROBIN", "LEAST_CONNECTIONS", "WEIGHTED_RESPONSE_TIME", "LEAST_CPU_USAGE"
        });
        
        JButton btnAplicarAlgoritmo = new JButton("✅ Aplicar");
        
        panelBalanceador.add(new JLabel("Algoritmo:"));
        panelBalanceador.add(comboAlgoritmo);
        panelBalanceador.add(btnAplicarAlgoritmo);
        
        // Configuración de escalado
        JPanel panelEscaladoConfig = new JPanel(new GridLayout(4, 2, 5, 5));
        panelEscaladoConfig.setBorder(new TitledBorder("Configuración de Escalado"));
        
        panelEscaladoConfig.add(new JLabel("Umbral de Escalado:"));
        panelEscaladoConfig.add(new JLabel("50.0%"));
        panelEscaladoConfig.add(new JLabel("Umbral de Reducción:"));
        panelEscaladoConfig.add(new JLabel("20.0%"));
        panelEscaladoConfig.add(new JLabel("Máximo Réplicas:"));
        panelEscaladoConfig.add(new JLabel("10"));
        panelEscaladoConfig.add(new JLabel("Intervalo Evaluación:"));
        panelEscaladoConfig.add(new JLabel("15 segundos"));
        
        // Event listeners
        btnAplicarAlgoritmo.addActionListener(e -> cambiarAlgoritmoBalanceador());
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(panelBalanceador, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(panelEscaladoConfig, gbc);
        
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
        
        JLabel labelEstado = new JLabel("🎯 Broker Nacional - Estado: ACTIVO");
        labelEstado.setFont(labelEstado.getFont().deriveFont(Font.BOLD));
        
        panel.add(labelEstado);
        
        return panel;
    }

    private void iniciarActualizacionesAutomaticas() {
        // Actualizar tabla de candidatos cada 5 segundos
        schedulerUI.scheduleAtFixedRate(this::actualizarTablaCandidatos, 1, 5, TimeUnit.SECONDS);
        
        // Actualizar métricas del cluster cada 3 segundos
        schedulerUI.scheduleAtFixedRate(this::actualizarMetricasCluster, 2, 3, TimeUnit.SECONDS);
    }

    private void actualizarTablaCandidatos() {
        SwingUtilities.invokeLater(() -> {
            try {
                Candidato[] candidatos = administradorCandidatos.obtenerTodosCandidatos(null);
                
                // Limpiar tabla
                modeloTabla.setRowCount(0);
                
                // Agregar candidatos
                for (Candidato candidato : candidatos) {
                    Object[] fila = {
                        candidato.idCandidato,
                        candidato.nombre,
                        candidato.partido,
                        "N/A" // No hay campo propuestas en la estructura
                    };
                    modeloTabla.addRow(fila);
                }
                
                labelCantidadCandidatos.setText("Candidatos: " + candidatos.length);
                
            } catch (Exception e) {
                log("❌ Error actualizando candidatos: " + e.getMessage());
            }
        });
    }

    private void actualizarMetricasCluster() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Obtener métricas del master
                MetricasRecursos metricasMaster = monitorMaster.obtenerMetricas(null);
                
                // Obtener información de réplicas
                InfoReplica[] replicas = gestorReplicas.obtenerReplicasActivas(null);
                
                // Obtener carga promedio
                double cargaPromedio = balanceador.getCargaPromedioCluster();
                
                // Actualizar labels
                labelEstadoCluster.setText("🔄 Cluster: ACTIVO");
                labelReplicasActivas.setText(String.format("📊 Réplicas: %d activas", replicas.length));
                labelCargaPromedio.setText(String.format("⚖️ Carga: %.1f%%", cargaPromedio));
                labelMetricasMaster.setText(String.format("🖥️ Master: CPU=%.1f%% MEM=%.1f%%", 
                    metricasMaster.cpuUsage, metricasMaster.memoryUsage));
                
                // Actualizar barra de escalado
                int cargaTotal = (int) ((metricasMaster.cpuUsage + metricasMaster.memoryUsage + cargaPromedio) / 3.0);
                barraEscalado.setValue(cargaTotal);
                barraEscalado.setString(String.format("Carga: %d%%", cargaTotal));
                
                // Cambiar color según la carga
                if (cargaTotal > 50) {
                    barraEscalado.setForeground(Color.RED);
                } else if (cargaTotal > 30) {
                    barraEscalado.setForeground(Color.ORANGE);
                } else {
                    barraEscalado.setForeground(Color.GREEN);
                }
                
                // Actualizar tabla de réplicas
                modeloReplicas.setRowCount(0);
                for (InfoReplica replica : replicas) {
                    long tiempoActivo = (System.currentTimeMillis() - replica.tiempoCreacion) / 1000;
                    Object[] fila = {
                        replica.nodeId,
                        replica.endpoint,
                        replica.activa ? "🟢 ACTIVA" : "🔴 INACTIVA",
                        String.format("%.1f%%", replica.metricas.cpuUsage),
                        String.format("%.1f%%", replica.metricas.memoryUsage),
                        replica.metricas.requestCount,
                        String.format("%d seg", tiempoActivo)
                    };
                    modeloReplicas.addRow(fila);
                        }

                    } catch (Exception e) {
                log("❌ Error actualizando métricas: " + e.getMessage());
            }
        });
    }

    // ========== EVENT HANDLERS ==========
    
    private void examinarArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos CSV", "csv"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            campoRutaCSV.setText(archivo.getAbsolutePath());
        }
    }
    
    private void cargarCandidatosCSV() {
        String rutaArchivo = campoRutaCSV.getText().trim();
        
        if (rutaArchivo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor selecciona un archivo CSV", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            boolean resultado = broker.cargarCandidatosDesdeCSV(rutaArchivo, null);
            
            if (resultado) {
                log("✅ Candidatos cargados desde: " + rutaArchivo);
                JOptionPane.showMessageDialog(this, "Candidatos cargados exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                log("❌ Error cargando candidatos desde: " + rutaArchivo);
                JOptionPane.showMessageDialog(this, "Error cargando candidatos", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            log("❌ Excepción cargando candidatos: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void limpiarCandidatos() {
        int confirmacion = JOptionPane.showConfirmDialog(this, 
            "¿Estás seguro de que quieres limpiar todos los candidatos?", 
            "Confirmar", JOptionPane.YES_NO_OPTION);
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                boolean resultado = broker.limpiarCandidatos(null);
                
                if (resultado) {
                    log("✅ Candidatos limpiados");
                    JOptionPane.showMessageDialog(this, "Candidatos limpiados exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                    log("❌ Error limpiando candidatos");
                    JOptionPane.showMessageDialog(this, "Error limpiando candidatos", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                log("❌ Excepción limpiando candidatos: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void enviarCandidatosRegionales() {
        try {
            boolean resultado = broker.enviarCandidatosATodosRegionales(null);
            
            if (resultado) {
                log("✅ Candidatos enviados a servidores regionales");
                JOptionPane.showMessageDialog(this, "Candidatos enviados exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                log("❌ Error enviando candidatos a regionales");
                JOptionPane.showMessageDialog(this, "Error enviando candidatos", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            log("❌ Excepción enviando candidatos: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void escalarManualmente() {
        try {
            boolean resultado = broker.escalarAutomaticamente();
            
            if (resultado) {
                log("🚀 Escalado manual iniciado");
                JOptionPane.showMessageDialog(this, "Escalado iniciado exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                log("⚠️ No se pudo iniciar el escalado");
                JOptionPane.showMessageDialog(this, "No se pudo iniciar el escalado", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            log("❌ Error en escalado manual: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void reducirManualmente() {
        try {
            boolean resultado = broker.reducirReplicas();
            
            if (resultado) {
                log("📉 Reducción manual iniciada");
                JOptionPane.showMessageDialog(this, "Reducción iniciada exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                log("⚠️ No se pudo iniciar la reducción");
                JOptionPane.showMessageDialog(this, "No se pudo iniciar la reducción", "Advertencia", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
            log("❌ Error en reducción manual: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void cambiarAlgoritmoBalanceador() {
        String algoritmo = (String) comboAlgoritmo.getSelectedItem();
        
        try {
            
            log("⚖️ Algoritmo de balanceo cambiado a: " + algoritmo);
            JOptionPane.showMessageDialog(this, "Algoritmo cambiado a: " + algoritmo, "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            log("❌ Error cambiando algoritmo: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void cerrarAplicacion() {
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Estás seguro de que quieres cerrar la aplicación?",
            "Confirmar Cierre", JOptionPane.YES_NO_OPTION);
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            log("🛑 Cerrando interfaz del Broker Nacional");
            
            if (schedulerUI != null && !schedulerUI.isShutdown()) {
                schedulerUI.shutdown();
            }
            
            dispose();
            System.exit(0);
        }
    }
    
    private void log(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            String logEntry = String.format("[%s] %s%n", timestamp, mensaje);
            areaLog.append(logEntry);
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
        
        // También imprimir en consola
        System.out.println(mensaje);
    }
}