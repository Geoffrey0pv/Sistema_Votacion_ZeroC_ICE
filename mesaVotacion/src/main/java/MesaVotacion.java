import javax.swing.*;
import mesaVotacion.*;

public class MesaVotacion {

    public static void main(String[] args) throws Exception {

        GestorMesa gestor = new GestorMesa();          // inicia ICE

        SwingUtilities.invokeLater(() -> {               // inicia UI
            MesaUI ui = new MesaUI(gestor);
            ui.setVisible(true);
        });

        /* Mantén vivo el communicator hasta que la app Swing se cierre */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { gestor.close(); } catch (Exception ignored) {}
        }));
    }
}
