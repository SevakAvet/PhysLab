import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class Lab extends JApplet implements ItemListener {
    private SimThread timer = null;
    private Simulation sim = null;
    private JComboBox simMenu = null;
    private int currentSimMenuItem = -1;

    public static void main(String[] args) {
        JApplet applet = new Lab();
        JFrame frame = new SimFrame(applet);
        frame.setContentPane(applet.getContentPane());
        frame.setVisible(true);
        applet.init();
    }

    public void init() {
        startSim(0);
    }

    private void startSim(int simIndex) {
        if (sim != null) {
            stop();
            sim.shutDown();
            sim = null;
            this.currentSimMenuItem = -1;
        }

        sim = new Spring2DSim(getContentPane());

        this.currentSimMenuItem = simIndex;
        sim.setupControls();
        sim.setupGraph();
        getContentPane().setLayout(sim.makeLayoutManager());

        getContentPane().invalidate();
        getContentPane().validate();
        getContentPane().repaint();
        start();

        if (simMenu != null && simMenu.getSelectedIndex() != simIndex)
            simMenu.setSelectedIndex(simIndex);
    }

    public void itemStateChanged(ItemEvent event) {
        int i = simMenu.getSelectedIndex();
        if (i != this.currentSimMenuItem)
            startSim(simMenu.getSelectedIndex());
    }

    public void start() {
        if (timer == null && sim != null) {
            timer = new SimThread(sim, 10);
            timer.start();
        }
    }

    public void stop() {
        if (timer != null) {
            timer.interrupt();
            timer = null;
        }
    }

    public void setSize(int width, int height) {
        super.setSize(width, height);
    }

    public void setSize(Dimension d) {
        super.setSize(d);
    }
}





