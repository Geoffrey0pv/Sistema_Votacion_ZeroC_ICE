package mesaVotacion;

import javax.swing.*;
import java.awt.*;

public class MesaUI extends JFrame {

    private final GestorMesa gestor;

    public MesaUI(GestorMesa gestor) {
        super("Mesa de Votación");
        this.gestor = gestor;
        initGui();
    }

    private void initGui() {
        JButton votarBtn = new JButton("Emitir voto");
        votarBtn.addActionListener(e ->
            gestor.emitirVoto("mesa-01", "hashElector123", 42)
        );

        setLayout(new BorderLayout());
        add(votarBtn, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(300,140);
        setLocationRelativeTo(null);
    }
}
