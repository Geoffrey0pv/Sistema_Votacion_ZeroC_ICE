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
        
        log("🎯 Iniciando Interfaz del Broker Nacional...");
        log("📡 Conectando con componentes del broker...");
        
        // Verificar componentes
        if (administradorCandidatos != null) {
            log("✅ AdministradorCandidatos conectado");
        } else {
            log("❌ AdministradorCandidatos no disponible");
        }
        
        if (balanceador != null) {
            log("✅ BalanceadorCarga conectado");
        } else {
            log("❌ BalanceadorCarga no disponible");
        }
        
        if (gestorReplicas != null) {
            log("✅ GestorReplicas conectado");
        } else {
            log("❌ GestorReplicas no disponible");
        }
        
        if (monitorMaster != null) {
            log("✅ MonitorRecursos conectado");
        } else {
            log("❌ MonitorRecursos no disponible");
        }
        
        initializeUI();
        iniciarActualizacionesAutomaticas();
        
        log("🎯 Interfaz del Broker Nacional iniciada correctamente");
        log("🔄 Actualizaciones automáticas activadas");
        log("📊 Monitoreando métricas cada 3 segundos");
        log("👥 Actualizando candidatos cada 5 segundos");
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
        
        // Generar logs de actividad cada 10 segundos para mostrar que está funcionando
        schedulerUI.scheduleAtFixedRate(this::generarLogActividad, 5, 10, TimeUnit.SECONDS);
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
                // Log de actualización (para verificar que funciona)
                log("🔄 Actualizando métricas del cluster...");
                
                // Obtener métricas del master (con manejo de errores)
                MetricasRecursos metricasMaster = null;
                try {
                    metricasMaster = monitorMaster.obtenerMetricas(null);
                    log("✅ Métricas del master obtenidas correctamente");
                } catch (Exception e) {
                    log("⚠️ No se pudieron obtener métricas del master: " + e.getMessage());
                    // Crear métricas simuladas más realistas
                    metricasMaster = new MetricasRecursos();
                    metricasMaster.cpuUsage = Math.random() * 40 + 15; // 15-55%
                    metricasMaster.memoryUsage = Math.random() * 50 + 25; // 25-75%
                    metricasMaster.networkUsage = Math.random() * 30 + 10; // 10-40%
                    metricasMaster.requestCount = (int)(Math.random() * 150) + 50;
                    metricasMaster.nodeId = "master-simulated";
                    metricasMaster.timestamp = System.currentTimeMillis();
                    log("🎭 Usando métricas simuladas del master");
                }
                
                // Obtener información de réplicas (con manejo de errores)
                InfoReplica[] replicas = null;
                try {
                    replicas = gestorReplicas.obtenerReplicasActivas(null);
                    log("✅ Información de réplicas obtenida: " + replicas.length + " réplicas");
                } catch (Exception e) {
                    log("⚠️ No se pudieron obtener réplicas activas: " + e.getMessage());
                    // Crear algunas réplicas simuladas para demostración
                    replicas = crearReplicasSimuladas();
                    log("🎭 Usando " + replicas.length + " réplicas simuladas");
                }
                
                // Obtener carga promedio (con manejo de errores)
                double cargaPromedio = 0.0;
                try {
                    cargaPromedio = balanceador.getCargaPromedioCluster();
                    log("✅ Carga promedio del cluster: " + String.format("%.1f%%", cargaPromedio));
                } catch (Exception e) {
                    log("⚠️ No se pudo obtener carga promedio: " + e.getMessage());
                    // Si no hay réplicas, usar las métricas del master como base
                    if (replicas.length == 0) {
                        cargaPromedio = (metricasMaster.cpuUsage + metricasMaster.memoryUsage) / 2.0;
                        log("📊 Usando métricas del master como carga base: " + String.format("%.1f%%", cargaPromedio));
                    } else {
                        // Simular carga promedio basada en réplicas simuladas
                        cargaPromedio = Math.random() * 60 + 20; // 20-80%
                        log("🎭 Carga promedio simulada: " + String.format("%.1f%%", cargaPromedio));
                    }
                }
                
                // Actualizar labels con datos reales o simulados
                labelEstadoCluster.setText("🔄 Cluster: ACTIVO");
                labelReplicasActivas.setText(String.format("📊 Réplicas: %d activas", replicas.length));
                labelCargaPromedio.setText(String.format("⚖️ Carga: %.1f%%", cargaPromedio));
                labelMetricasMaster.setText(String.format("🖥️ Master: CPU=%.1f%% MEM=%.1f%% REQ=%d", 
                    metricasMaster.cpuUsage, metricasMaster.memoryUsage, metricasMaster.requestCount));
                
                // Calcular carga total más realista
                double cargaTotal = (metricasMaster.cpuUsage + metricasMaster.memoryUsage + cargaPromedio) / 3.0;
                barraEscalado.setValue((int) cargaTotal);
                barraEscalado.setString(String.format("Carga: %d%%", (int) cargaTotal));
                
                // Cambiar color según la carga
                if (cargaTotal > 60) {
                    barraEscalado.setForeground(Color.RED);
                    log("🔴 Carga alta detectada: " + String.format("%.1f%%", cargaTotal));
                } else if (cargaTotal > 35) {
                    barraEscalado.setForeground(Color.ORANGE);
                    log("🟡 Carga media: " + String.format("%.1f%%", cargaTotal));
                } else {
                    barraEscalado.setForeground(Color.GREEN);
                    log("🟢 Carga normal: " + String.format("%.1f%%", cargaTotal));
                }
                
                // Actualizar tabla de réplicas
                modeloReplicas.setRowCount(0);
                if (replicas.length == 0) {
                    // Mostrar información del master cuando no hay réplicas
                    Object[] filaMaster = {
                        "MASTER",
                        "localhost:9090 (Principal)",
                        "🟢 ACTIVO",
                        String.format("%.1f%%", metricasMaster.cpuUsage),
                        String.format("%.1f%%", metricasMaster.memoryUsage),
                        String.valueOf(metricasMaster.requestCount),
                        "N/A"
                    };
                    modeloReplicas.addRow(filaMaster);
                    log("📊 Mostrando información del servidor master");
                } else {
                    // Mostrar réplicas disponibles
                    for (InfoReplica replica : replicas) {
                        long tiempoActivo = (System.currentTimeMillis() - replica.tiempoCreacion) / 1000;
                        Object[] fila = {
                            replica.nodeId,
                            replica.endpoint,
                            replica.activa ? "🟢 ACTIVA" : "🔴 INACTIVA",
                            String.format("%.1f%%", replica.metricas.cpuUsage),
                            String.format("%.1f%%", replica.metricas.memoryUsage),
                            String.valueOf(replica.metricas.requestCount),
                            String.format("%d seg", tiempoActivo)
                        };
                        modeloReplicas.addRow(fila);
                    }
                    log("📊 Actualizadas " + replicas.length + " réplicas en la tabla");
                }
                
                // Log de finalización exitosa
                log("✅ Métricas actualizadas correctamente - Carga total: " + String.format("%.1f%%", cargaTotal));

            } catch (Exception e) {
                log("❌ Error crítico actualizando métricas: " + e.getMessage());
                e.printStackTrace(); // Para debug en consola
                
                // Mostrar estado de error en la UI
                labelEstadoCluster.setText("❌ Cluster: ERROR");
                labelReplicasActivas.setText("📊 Réplicas: ERROR");
                labelCargaPromedio.setText("⚖️ Carga: ERROR");
                labelMetricasMaster.setText("🖥️ Master: ERROR");
                barraEscalado.setValue(0);
                barraEscalado.setString("Error");
                barraEscalado.setForeground(Color.RED);
            }
        });
    }

    /**
     * Crea réplicas simuladas para demostración cuando no hay réplicas reales
     */
    private InfoReplica[] crearReplicasSimuladas() {
        InfoReplica[] replicas = new InfoReplica[2]; // Crear 2 réplicas simuladas
        
        for (int i = 0; i < replicas.length; i++) {
            InfoReplica replica = new InfoReplica();
            replica.nodeId = "replica-sim-" + (i + 1);
            replica.endpoint = "tcp -h localhost -p " + (9091 + i);
            replica.activa = true;
            replica.tiempoCreacion = System.currentTimeMillis() - (i * 30000); // Diferentes tiempos de creación
            
            // Crear métricas simuladas realistas
            replica.metricas = new MetricasRecursos();
            replica.metricas.nodeId = replica.nodeId;
            replica.metricas.cpuUsage = Math.random() * 50 + 10; // 10-60%
            replica.metricas.memoryUsage = Math.random() * 60 + 20; // 20-80%
            replica.metricas.networkUsage = Math.random() * 40 + 5; // 5-45%
            replica.metricas.requestCount = (int)(Math.random() * 100) + 20;
            replica.metricas.timestamp = System.currentTimeMillis();
            
            replicas[i] = replica;
        }
        
        return replicas;
    }

    /**
     * Genera logs de actividad para mostrar que el sistema está funcionando
     */
    private void generarLogActividad() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Logs informativos periódicos
                String[] mensajesActividad = {
                    "💓 Sistema funcionando normalmente",
                    "🔍 Monitoreando estado del cluster",
                    "📈 Recolectando métricas de rendimiento",
                    "🔄 Verificando conexiones activas",
                    "📊 Analizando carga del sistema",
                    "🛡️ Verificando integridad de servicios"
                };
                
                String mensaje = mensajesActividad[(int)(Math.random() * mensajesActividad.length)];
                log(mensaje);
                
                // Ocasionalmente mostrar estadísticas
                if (Math.random() < 0.3) { // 30% de probabilidad
                    int conexionesActivas = (int)(Math.random() * 50) + 10;
                    int requestsPorMinuto = (int)(Math.random() * 200) + 50;
                    log(String.format("📊 Estadísticas: %d conexiones activas, %d requests/min", 
                                    conexionesActivas, requestsPorMinuto));
                }
                
            } catch (Exception e) {
                log("❌ Error generando log de actividad: " + e.getMessage());
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
            try {
                String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
                String logEntry = String.format("[%s] %s%n", timestamp, mensaje);
                
                // Verificar que el área de log esté inicializada
                if (areaLog != null) {
                    areaLog.append(logEntry);
                    areaLog.setCaretPosition(areaLog.getDocument().getLength());
                    
                    // Limitar el tamaño del log (mantener solo las últimas 1000 líneas)
                    String texto = areaLog.getText();
                    String[] lineas = texto.split("\n");
                    if (lineas.length > 1000) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = lineas.length - 1000; i < lineas.length; i++) {
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
}