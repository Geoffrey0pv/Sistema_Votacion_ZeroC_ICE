package InterfazGrafica;

import GestorMesa.GestorMesa;
import Demo.Candidato;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class MesaVotacionUI extends JFrame {
    private GestorMesa gestorMesa;

    private JTextField txtDocumento;
    private JButton btnValidarElector;
    private JPanel panelCandidatos;
    private ButtonGroup grupoCandidatos;
    private JButton btnVotar;
    private JButton btnCancelar;
    private JLabel lblEstado;
    private JLabel lblMesa;
    private JLabel lblEstadisticas;

    private String documentoActual;
    private boolean electorValidado;

    public MesaVotacionUI(GestorMesa gestorMesa) {
        this.gestorMesa = gestorMesa;
        this.electorValidado = false;

        initializeComponents();
        setupLayout();
        setupEventListeners();

        gestorMesa.cargarCandidatos();
        actualizarCandidatos();
        actualizarEstadisticas();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Sistema de Votación - Mesa " + gestorMesa.getIdMesa());
        setSize(600, 500);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void initializeComponents() {
        lblMesa = new JLabel("Mesa de Votación: " + gestorMesa.getIdMesa(), JLabel.CENTER);
        lblMesa.setFont(new Font("Arial", Font.BOLD, 18));
        lblMesa.setForeground(new Color(0, 102, 204));

        txtDocumento = new JTextField(15);
        txtDocumento.setFont(new Font("Arial", Font.PLAIN, 14));

        btnValidarElector = new JButton("Validar Elector");
        btnValidarElector.setBackground(new Color(0, 153, 76));
        btnValidarElector.setForeground(Color.BLACK);
        btnValidarElector.setFont(new Font("Arial", Font.BOLD, 12));

        panelCandidatos = new JPanel();
        panelCandidatos.setLayout(new BoxLayout(panelCandidatos, BoxLayout.Y_AXIS));
        panelCandidatos.setBorder(BorderFactory.createTitledBorder("Seleccione su candidato:"));
        grupoCandidatos = new ButtonGroup();

        btnVotar = new JButton("CONFIRMAR VOTO");
        btnVotar.setBackground(new Color(0, 102, 204));
        btnVotar.setForeground(Color.BLACK);
        btnVotar.setFont(new Font("Arial", Font.BOLD, 14));
        btnVotar.setEnabled(false);

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setBackground(new Color(204, 0, 0));
        btnCancelar.setForeground(Color.BLACK);
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 12));

        lblEstado = new JLabel("Ingrese su documento de identidad para continuar", JLabel.CENTER);
        lblEstado.setFont(new Font("Arial", Font.ITALIC, 12));
        lblEstado.setForeground(new Color(102, 102, 102));

        lblEstadisticas = new JLabel("", JLabel.CENTER);
        lblEstadisticas.setFont(new Font("Arial", Font.PLAIN, 10));
        lblEstadisticas.setForeground(new Color(153, 153, 153));
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        panelSuperior.add(lblMesa, BorderLayout.CENTER);
        panelSuperior.add(lblEstadisticas, BorderLayout.SOUTH);

        JPanel panelCentral = new JPanel(new BorderLayout(10, 10));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel panelDocumento = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelDocumento.add(new JLabel("Documento de Identidad:"));
        panelDocumento.add(txtDocumento);
        panelDocumento.add(btnValidarElector);

        panelCentral.add(panelDocumento, BorderLayout.NORTH);
        panelCentral.add(new JScrollPane(panelCandidatos), BorderLayout.CENTER);

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JPanel panelBotones = new JPanel(new FlowLayout());
        panelBotones.add(btnVotar);
        panelBotones.add(btnCancelar);

        panelInferior.add(lblEstado, BorderLayout.CENTER);
        panelInferior.add(panelBotones, BorderLayout.SOUTH);

        add(panelSuperior, BorderLayout.NORTH);
        add(panelCentral, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);
    }

    private void setupEventListeners() {
        btnValidarElector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validarElector();
            }
        });

        btnVotar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmarVoto();
            }
        });

        btnCancelar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelarVotacion();
            }
        });

        txtDocumento.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validarElector();
            }
        });
    }

    private void validarElector() {
        String documento = txtDocumento.getText().trim();

        if (documento.isEmpty()) {
            mostrarMensaje("Por favor ingrese su documento de identidad", "warning");
            return;
        }

        boolean valido = gestorMesa.validarElector(documento);

        if (valido) {
            documentoActual = documento;
            electorValidado = true;

            habilitarSeleccionCandidatos(true);
            txtDocumento.setEnabled(false);
            btnValidarElector.setEnabled(false);

            mostrarMensaje("Elector validado correctamente. Seleccione su candidato.", "success");
        } else {
            mostrarMensaje("Elector no válido o ya votó en esta mesa", "error");
            electorValidado = false;
            habilitarSeleccionCandidatos(false);
        }
    }

    private void confirmarVoto() {
        if (!electorValidado) {
            mostrarMensaje("Debe validar su documento primero", "warning");
            return;
        }

        long idCandidatoSeleccionado = obtenerCandidatoSeleccionado();

        if (idCandidatoSeleccionado == -1) {
            mostrarMensaje("Debe seleccionar un candidato", "warning");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de confirmar su voto?\nEsta acción no se puede deshacer.",
                "Confirmar Voto",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            boolean votoRegistrado = gestorMesa.registrarVoto(documentoActual, idCandidatoSeleccionado);

            if (votoRegistrado) {
                mostrarMensaje("¡Voto registrado exitosamente!", "success");

                JOptionPane.showMessageDialog(
                        this,
                        "Su voto ha sido registrado correctamente.\n¡Gracias por participar!",
                        "Voto Exitoso",
                        JOptionPane.INFORMATION_MESSAGE
                );

                resetearFormulario();
                actualizarEstadisticas();
            } else {
                mostrarMensaje("Error registrando el voto. Intente nuevamente.", "error");
            }
        }
    }

    private void cancelarVotacion() {
        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de cancelar la votación?",
                "Cancelar Votación",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            resetearFormulario();
            mostrarMensaje("Votación cancelada", "info");
        }
    }

    private void resetearFormulario() {
        txtDocumento.setText("");
        txtDocumento.setEnabled(true);
        btnValidarElector.setEnabled(true);
        electorValidado = false;
        documentoActual = null;

        grupoCandidatos.clearSelection();
        habilitarSeleccionCandidatos(false);

        mostrarMensaje("Ingrese su documento de identidad para continuar", "info");
    }

    private void actualizarCandidatos() {
        panelCandidatos.removeAll();
        grupoCandidatos = new ButtonGroup();

        List<Candidato> candidatos = gestorMesa.getCandidatosDisponibles();

        for (Candidato candidato : candidatos) {
            JRadioButton rbCandidato = new JRadioButton(
                    candidato.idCandidato + " - " + candidato.nombre);
            rbCandidato.setFont(new Font("Arial", Font.PLAIN, 14));
            rbCandidato.setActionCommand(String.valueOf(candidato.idCandidato));
            rbCandidato.setEnabled(false);

            grupoCandidatos.add(rbCandidato);
            panelCandidatos.add(rbCandidato);
        }

        revalidate();
        repaint();
    }

    private void habilitarSeleccionCandidatos(boolean habilitar) {
        Component[] componentes = panelCandidatos.getComponents();
        for (Component comp : componentes) {
            if (comp instanceof JRadioButton) {
                comp.setEnabled(habilitar);
            }
        }
        btnVotar.setEnabled(habilitar);
    }

    private long obtenerCandidatoSeleccionado() {
        ButtonModel seleccionado = grupoCandidatos.getSelection();
        if (seleccionado != null) {
            return Long.parseLong(seleccionado.getActionCommand());
        }
        return -1;
    }

    private void mostrarMensaje(String mensaje, String tipo) {
        lblEstado.setText(mensaje);

        switch (tipo) {
            case "success":
                lblEstado.setForeground(new Color(0, 153, 76));
                break;
            case "error":
                lblEstado.setForeground(new Color(204, 0, 0));
                break;
            case "warning":
                lblEstado.setForeground(new Color(255, 153, 0));
                break;
            default:
                lblEstado.setForeground(new Color(102, 102, 102));
                break;
        }
    }

    private void actualizarEstadisticas() {
        if (gestorMesa.hayMensajesPendientes()) {
            lblEstadisticas.setText("⚠ Hay mensajes pendientes de envio");
            lblEstadisticas.setForeground(new Color(255, 153, 0));
        } else {
            lblEstadisticas.setText("✓ Conexion normal");
            lblEstadisticas.setForeground(new Color(0, 153, 76));
        }
    }

    public void mostrar() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
        });
    }

    public void cerrar() {
        gestorMesa.shutdown();
        dispose();
        System.exit(0);
    }
}