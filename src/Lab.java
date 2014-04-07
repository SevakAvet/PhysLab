import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class Lab extends JApplet implements ItemListener {
    private static int testNumber = 0;
    private SimThread timer = null;
    private Simulation sim = null;
    private JComboBox simMenu = null;
    private boolean browserMode = true;
    private boolean gameMode = false;
    private int currentSimMenuItem = -1;
    private String[] sims = {
            "single spring",
            "double spring",
            "pendulum",
            "2D spring",
            "double pendulum",
            "double 2D spring",
            "collide spring",
            "molecule",
            "molecule 2",
            "molecule 3",
            "molecule 4",
            "molecule 5",
            "molecule 6",
            "pendulum cart",
            "dangle stick",
            "rigid body collisions",
            "contact",
            "roller coaster",
            "Roller Coaster with Spring",
            "Roller Coaster with 2 Balls",
            "Roller Coaster with Flight",
            "Roller Coaster Lagrangian",
            "string",
            "Moveable Double Pendulum",
            "pendulum 2"
    };

    public Lab() {
    }

    public Lab(boolean browserMode) {
        this();
        this.browserMode = browserMode;
    }

    public static void main(String[] args) {
        JApplet applet = new Lab(true);
        JFrame frame = new SimFrame(applet);
        frame.setContentPane(applet.getContentPane());
        frame.setVisible(true);
        testNumber = Integer.parseInt(args[0]);
        applet.init();
    }

    public void init() {
        startSim(testNumber);
    }

    private void startSim(int simIndex) {
        Utility.println("startSim(" + simIndex + ")");
        if (sim != null) {
            Utility.println("stopping and shutting down " + sim);
            stop();
            sim.shutDown();  // hides all components of this sim
            sim = null;
            this.currentSimMenuItem = -1;
        }

        if (simIndex < 0)
            return;
        System.out.println("starting simulation " + sims[simIndex]);
        switch (simIndex) {
            case 0:
                sim = new SpringSim1(getContentPane());
                break;
            case 1:
                sim = new DoubleSpringSim(getContentPane(), 2);
                break;
            case 2:
                sim = new PendulumSim(getContentPane());
                break;
            case 3:
                sim = new Spring2DSim(getContentPane());
                break;
            case 4:
                sim = new DoublePendulum(getContentPane());
                break;
            case 5:
                sim = new Double2DSpring(getContentPane());
                break;
            case 6:
                sim = new CollideSpring(getContentPane());
                break;
            case 7:
                sim = new Molecule1(getContentPane());
                break;
            case 8:
                sim = new Molecule3(getContentPane(), 2);
                break;
            case 9:
                sim = new Molecule3(getContentPane(), 3);
                break;
            case 10:
                sim = new Molecule3(getContentPane(), 4);
                break;
            case 11:
                sim = new Molecule3(getContentPane(), 5);
                break;
            case 12:
                sim = new Molecule3(getContentPane(), 6);
                break;
            case 13:
                sim = new PendulumCart(getContentPane());
                break;
            case 14:
                sim = new DangleStick(getContentPane());
                break;
            case 15:
                sim = new Thruster5(getContentPane(), gameMode);
                break;
            case 16:
                sim = new ContactSim(getContentPane());
                break;
            case 17:
                sim = new Roller1(getContentPane(), 0);
                break;
            case 18:
                sim = new Roller2(getContentPane(), 0);
                break;
            case 19:
                sim = new Roller3(getContentPane(), 0);
                break;
            case 20:
                sim = new Roller4(getContentPane());
                break;
            case 21:
                sim = new Roller5(getContentPane(), 0);
                break;
            case 22:
                sim = new String1(getContentPane());
                break;
            case 23:
                sim = new MoveableDoublePendulum(getContentPane());
                break;
            case 24:
                sim = new Pendulum2(getContentPane());
                break;
            default:
                sim = null;
        }
        this.currentSimMenuItem = simIndex;
        Utility.println("created simulation " + sim);
        Utility.println(" with rootpane.isDoubleBuffered=" + getRootPane().isDoubleBuffered());
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
        Utility.println("Lab.start ");
        if (timer == null && sim != null) {
            timer = new SimThread(sim, 10);  // was 33
            timer.start();
        }
    }

    public void stop() {
        Utility.println("Lab.stop ");
        if (timer != null) {
            timer.interrupt();
            timer = null;
        }
    }

    public void setSize(int width, int height) {
        Utility.println("Lab.setSize " + width + " " + height);
        super.setSize(width, height);
    }

    public void setSize(Dimension d) {
        Utility.println("Lab.setSize " + d);
        super.setSize(d);
    }
}





