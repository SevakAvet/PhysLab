import javax.swing.*;
import java.awt.*;


public class LabTest extends JApplet {
    private SimThread timer = null;
    private Simulation sim = null;
    private boolean firstUpdate = true;

    public LabTest() {
        // do nothing in the constructor, because the applet
        // is not 'loaded' yet by the browser... no window, etc.
    }

    /* When run as an application, instead of as an applet, main() is the
      first procedure to be called.  We create a window (Frame) and instantiate
      the Lab applet.
      When run as an applet, the browser creates the window and instantiates
      the applet.
      */
    public static void main(String[] args) {
        JApplet applet = new LabTest();
        JFrame frame = new SimFrame(applet);
        frame.setContentPane(applet.getContentPane());
        frame.setVisible(true);
        applet.init();
    }

    /* init() is called by the browser or applet viewer to inform this applet
    that it has been loaded into the system. It is always called before
    the first time that the start method is called.
    Note that window size is 0,0 at this point.
    */
    public void init() {
        System.out.println("starting MyPhysicsLab Test");
        getContentPane().setBackground(Color.white);
        sim = new MoveableDoublePendulum(getContentPane());
        if (sim == null)
            throw new IllegalStateException("unable to create simulation " + sim);
        else
            System.out.println("Simulation created " + sim);

        // set location and scale of simulation
        CoordMap map = sim.getCoordMap();
        map.setOrigin(300, 150);
        map.setScale(100.0, 100.0);
        // set geometry of simulation
        sim.setParameter("gravity", 9.8);
        // set initial conditions of simulation
        sim.setVariable(0, 0.0);
        sim.setVariable(1, 1.5);
        // use a simple layout
        getContentPane().setLayout(new BorderLayout());
        Component cnvs = getContentPane().getComponent(0);  // this should be the SimCanvas
        getContentPane().add(cnvs, BorderLayout.CENTER);
        System.out.println("in init(), window size: " + getContentPane().getSize().width
                + " " + getContentPane().getSize().height);
        getContentPane().invalidate();
        getContentPane().validate();
        getContentPane().repaint();
        start();
    }

    // called when user returns to browser page containing applet
    public void start() {
        if (timer == null && sim != null) {
            timer = new SimThread(sim, 10);  // was 33
            timer.start();
        }
    }

    // called when user leaves browser page containing applet
    public void stop() {
        if (timer != null) {
            timer.interrupt();
            timer = null;  // destroys the thread
        }
    }

  /* override update to NOT erase the background before painting.
    For Swing components, paint() is always invoked as a result of both system-triggered and
	app-triggered paint requests;update() is never invoked on Swing components.
  public void update(Graphics g) {
    if (true)
      throw new IllegalStateException("is update really never called on swing components?");
    if (firstUpdate) {
      System.out.println("in update(), window size: "+getContentPane().getSize().width
        +" "+getContentPane().getSize().height);
      firstUpdate = false;
    }
    super.paint(g);
  }
   */
}



