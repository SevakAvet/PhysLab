import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;


public class Molecule1 extends CollidingSim implements ActionListener, ObjectListener {
    private static final int TOP_WALL = 1, BOTTOM_WALL = 2, LEFT_WALL = 3, RIGHT_WALL = 4;
    private static final String MASS1 = "blue mass", MASS2 = "red mass",
            ELASTICITY = "elasticity", GRAVITY = "gravity",
            DAMPING = "damping", LENGTH = "spring rest length", STIFFNESS = "spring stiffness";


    private String[] params = {MASS1, MASS2, ELASTICITY, GRAVITY, DAMPING, STIFFNESS, LENGTH};
    private CSpring m_Spring;
    private CMass m_Mass1, m_Mass2;
    private CRect m_Walls;
    private double m_Elasticity = 0.8;
    private double m_Damping = 0;
    private double m_Gravity = 6;
    private double m_Left, m_Right, m_Top, m_Bottom;
    private JButton button_stop;
    private Vector collisions = new Vector(10);


    public Molecule1(Container container) {
        super(container, 8);
        var_names = new String[]{
                "x1 position",
                "y1 position",
                "x2 position",
                "y2 position",
                "x1 velocity",
                "y1 velocity",
                "x2 velocity",
                "y2 velocity"
        };
        setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -6, 6, -6, 6,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

        double xx = 0;
        double yy = -2;
        double w = 0.5;

        DoubleRect box = cvs.getSimBounds();
        m_Left = box.getXMin() + w / 2;
        m_Right = box.getXMax() - w / 2;
        m_Bottom = box.getYMin() + w / 2;
        m_Top = box.getYMax() - w / 2;
        m_Walls = new CRect(box);
        cvs.addElement(m_Walls);
        cvs.setObjectListener(this);

        m_Mass1 = new CMass(0, 0, w, w, CElement.MODE_CIRCLE_FILLED);
        m_Mass1.m_Mass = .5;
        m_Mass1.m_Color = Color.blue;
        cvs.addElement(m_Mass1);

        m_Mass2 = new CMass(1, -1, w, w, CElement.MODE_CIRCLE_FILLED);
        m_Mass2.m_Mass = .5;
        cvs.addElement(m_Mass2);

        m_Spring = new CSpring(0, 0, 1.0, 0.3);
        m_Spring.setX2(m_Mass2.getCenterX());
        m_Spring.setY2(m_Mass2.getCenterY());
        m_Spring.m_SpringConst = 6;
        cvs.addElement(m_Spring);


        vars[0] = m_Mass1.getCenterX();
        vars[1] = m_Mass1.getCenterY();
        vars[2] = m_Mass2.getCenterX();
        vars[3] = m_Mass2.getCenterY();
        vars[4] = vars[5] = vars[6] = vars[7] = 0;
        vars[4] = 2;
        modifyObjects();
    }

    public void setupControls() {
        super.setupControls();
        addControl(button_stop = new JButton("reset"));
        button_stop.addActionListener(this);

        for (int i = 0; i < params.length; i++)
            addObserverControl(new DoubleField(this, params[i], 2));
        showControls(true);
    }

    public void setupGraph() {
        super.setupGraph();
        if (graph != null)
            graph.setVars(0, 1);
        showGraph(false);
    }


    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(MASS1)) {
            m_Mass1.m_Mass = value;
            return true;
        }
        if (name.equalsIgnoreCase(MASS2)) {
            m_Mass2.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(ELASTICITY)) {
            m_Elasticity = value;
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            m_Gravity = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING)) {
            m_Damping = value;
            return true;
        } else if (name.equalsIgnoreCase(STIFFNESS)) {
            m_Spring.m_SpringConst = value;
            return true;
        } else if (name.equalsIgnoreCase(LENGTH)) {
            m_Spring.m_RestLength = value;
            return true;
        }
        return super.trySetParameter(name, value);
    }


    public double getParameter(String name) {
        if (name.equalsIgnoreCase(MASS1))
            return m_Mass1.m_Mass;
        if (name.equalsIgnoreCase(MASS2))
            return m_Mass2.m_Mass;
        else if (name.equalsIgnoreCase(ELASTICITY))
            return m_Elasticity;
        else if (name.equalsIgnoreCase(GRAVITY))
            return m_Gravity;
        else if (name.equalsIgnoreCase(DAMPING))
            return m_Damping;
        else if (name.equalsIgnoreCase(STIFFNESS))
            return m_Spring.m_SpringConst;
        else if (name.equalsIgnoreCase(LENGTH))
            return m_Spring.m_RestLength;
        return super.getParameter(name);
    }


    public String[] getParameterNames() {
        return params;
    }

    public void objectChanged(Object o) {
        if (o == cvs) {
            DoubleRect box = cvs.getSimBounds();
            m_Walls.setBounds(box);
            double w = m_Mass1.m_Width;
            m_Left = box.getXMin() + w / 2;
            m_Right = box.getXMax() - w / 2;
            m_Bottom = box.getYMin() + w / 2;
            m_Top = box.getYMax() - w / 2;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button_stop) {
            vars[0] = -1;
            vars[1] = -1;
            vars[2] = 1;
            vars[3] = 1;
            vars[4] = vars[5] = vars[6] = vars[7] = 0;
        }
    }

    public void modifyObjects() {

        double w = m_Mass1.m_Width / 2;
        m_Mass1.setX1(vars[0] - w);
        m_Mass1.setY1(vars[1] - w);
        m_Mass2.setX1(vars[2] - w);
        m_Mass2.setY1(vars[3] - w);
        m_Spring.setX1(vars[0]);
        m_Spring.setY1(vars[1]);
        m_Spring.setX2(vars[2]);
        m_Spring.setY2(vars[3]);
    }


    public void startDrag(Dragable e) {
        if (e == m_Mass1) {
            calc[0] = calc[1] = calc[4] = calc[5] = false;
        } else if (e == m_Mass2) {
            calc[2] = calc[3] = calc[6] = calc[7] = false;
        }
    }


    public void constrainedSet(Dragable e, double x, double y) {

        double w = m_Mass1.m_Width / 2;
        x += w;
        y += w;


        if (x < m_Left)
            x = m_Left + 0.0001;
        if (x > m_Right)
            x = m_Right - 0.0001;
        if (y < m_Bottom)
            y = m_Bottom + 0.0001;
        if (y > m_Top)
            y = m_Top - 0.0001;

        if (e == m_Mass1) {
            vars[0] = x;
            vars[1] = y;
            vars[4] = vars[5] = 0;
        } else if (e == m_Mass2) {
            vars[2] = x;
            vars[3] = y;
            vars[6] = vars[7] = 0;
        }

    }

    private void addCollision(int obj1, int obj2) {
        collisions.addElement(new int[]{obj1, obj2});
    }


    public Vector findAllCollisions() {
        collisions.removeAllElements();
        for (int j = 0; j < 2; j++) {


            if (vars[2 * j] < m_Left)
                addCollision(LEFT_WALL, j);
            if (vars[2 * j] > m_Right)
                addCollision(RIGHT_WALL, j);
            if (vars[1 + 2 * j] < m_Bottom)
                addCollision(BOTTOM_WALL, j);
            if (vars[1 + 2 * j] > m_Top)
                addCollision(TOP_WALL, j);
        }
        return (collisions.size() > 0) ? collisions : null;
    }

    public void handleCollisions(Vector collisions) {
        for (int i = 0; i < collisions.size(); i++) {
            int[] objs = (int[]) collisions.elementAt(i);


            int mi = 2 * objs[1];
            switch (objs[0]) {
                case LEFT_WALL:
                case RIGHT_WALL:
                    vars[4 + mi] = -m_Elasticity * vars[4 + mi];
                    break;
                case TOP_WALL:
                case BOTTOM_WALL:
                    vars[5 + mi] = -m_Elasticity * vars[5 + mi];
                    break;
            }
        }
    }

  /* 2-D spring simulation with gravity
    y increases UP

         m2     .
          \     .
           \ th .
            \   .
             \  .
              \ .
               m1

    m1, m2 = masses of particle 1 and 2
    th = angle formed with vertical, 0=up, positive is counter clockwise
    L = displacement of spring from rest length
    R = rest length
    U1, U2 = position of CENTER of mass of particle 1 or 2
    V1, V2 = velocity of particle
    F1, F2 = force on particle
    k = spring constant
    b1, b2 = damping constants for each particle

    F1x = k L sin(th) -b1 V1x = m1 V1x'
    F1y = -m1 g +k L cos(th) -b1 V1y = m1 V1y'
    F2x = -k L sin(th) -b2 V2x = m2 V2x'
    F2y = -m2 g -k L cos(th) -b2 V2y = m2 V2y'
    xx = U2x - U1x
    yy = U2y - U1y
    len = sqrt(xx^2+yy^2)
    L = len - R
    cos(th) = yy / len
    sin(th) = xx / len

    CONTACT FORCE
    We detect when a particle is in resting contact with floor or wall.
    Consider contact with the floor.  Suppose the particle is 'close' to
    the floor, then there are 3 cases:
      1. vertical velocity is 'large' and positive.  Then the particle is
      separating from the floor, so nothing needs to be done.
      2. vertical velocity is 'large' and negative.  A collision is imminent,
      so let the collision software handle this case.
      3. vertical velocity is 'small'.  Now the particle is likely in contact
      with the floor.  There are two cases:
        a.  Net force positive = particle is being pulled off floor.  In this
        case do nothing, there is no reaction force from the floor.
        b.  Net force negative = particle is being pulled downwards.
        Here, we set the net force to zero, because the force is resisted
        by the reaction force from the floor.

    How small is 'small' velocity?
      We are trying to avoid the case where there is a tiny upwards velocity
      and a large downwards force, which just results in zillions of collisions
      over the time step we are solving (typically about 0.03 seconds).
      Instead, we assume that the particle stops bouncing and comes into
      contact with the floor in this case.
      For a given force (assuming it stays approx constant over the time span
      of 0.03 seconds), there is an 'escape velocity' that would allow the particle
      to leave contact and be above the floor at the end of the time step.
        (If the particle is still below the floor at the end of the timestep,
        then the animation would not look any different, even though there is
        some small amount of physics still happening... so our threshold here
        depends on whether physical accuracy or visual appearance is more
        important.)

      Let the time step = h, the force = F, mass = m, initial vertical velocity = v0.
      Then we have
        v' = F/m
        v = (F/m)t + v0
        y = (F/2m)t^2 + v0*t
      Requiring the particle to be below the floor at time h gives the condition
        0 > y = (F/2m)h^2 + v0*h
      Dividing by h gives
        0 > F*h/2m + v0
        -F*h/2m > v0
      This is our definition of a small velocity.  Note that it depends
      on the net force.  Because with a large downward force, it would take a big
      velocity to actually result in contact being lost at the end of the time period.
      Equivalently, if there is just a slight downward force (e.g. spring almost
      offsetting gravity), then just a little velocity is enough to result in
      contact being broken.

      vars: 0   1   2   3   4   5   6   7
           U1x U1y U2x U2y V1x V1y V2x V2y
  */

    public void evaluate(double[] x, double[] change) {
        change[0] = x[4];  //U1x' = V1x
        change[1] = x[5];  //U1y' = V1y
        change[2] = x[6];  //U2x' = V2x
        change[3] = x[7];  //U2y' = V2y
        double xx = x[2] - x[0];
        double yy = x[3] - x[1];
        double len = Math.sqrt(xx * xx + yy * yy);
        double m1 = m_Mass1.m_Mass;
        double m2 = m_Mass2.m_Mass;
        double DIST_TOL = 0.02;
        double timeStep = 0.03;  // assume timeStep is this length or longer

        // V1x' = (k/m1) L sin(th)
        double r = (m_Spring.m_SpringConst / m1) *
                (len - m_Spring.m_RestLength) * xx / len;
        if (r < 0 && Math.abs(x[0] - m_Left) < DIST_TOL && Math.abs(x[4]) < -r * timeStep / (2 * m1)) {
            r = x[4] = 0;
            x[0] = m_Left;
        } else if (r > 0 && Math.abs(x[0] - m_Right) < DIST_TOL && Math.abs(x[4]) < r * timeStep / (2 * m1)) {
            r = x[4] = 0;
            x[0] = m_Right;
        } else {
            if (m_Damping != 0)
                r -= (m_Damping / m1) * x[4];
        }
        change[4] = r;

        // V1y' = -g -(k/m1) L cos(th)
        r = -m_Gravity + (m_Spring.m_SpringConst / m1) *
                (len - m_Spring.m_RestLength) * yy / len;
        // floor contact if (downward force, near floor, and low velocity)
        if (r < 0 && Math.abs(x[1] - m_Bottom) < DIST_TOL && Math.abs(x[5]) < -r * timeStep / (2 * m1)) {
            // also set velocity to zero, to avoid buildup of small velocity over time
            r = x[5] = 0;
            x[1] = m_Bottom;
        } else if (r > 0 && Math.abs(x[1] - m_Top) < DIST_TOL && Math.abs(x[5]) < r * timeStep / (2 * m1)) {
            r = x[5] = 0;
            x[1] = m_Top;
        } else {
            if (m_Damping != 0)
                r -= (m_Damping / m1) * x[5];
        }
        change[5] = r;

        // V2x'
        r = -(m_Spring.m_SpringConst / m2) *
                (len - m_Spring.m_RestLength) * xx / len;
        if (r < 0 && Math.abs(x[2] - m_Left) < DIST_TOL && Math.abs(x[6]) < -r * timeStep / (2 * m2)) {
            r = x[6] = 0;
            x[2] = m_Left;
        } else if (r > 0 && Math.abs(x[2] - m_Right) < DIST_TOL && Math.abs(x[6]) < r * timeStep / (2 * m2)) {
            r = x[6] = 0;
            x[2] = m_Right;
        } else {
            if (m_Damping != 0)
                r -= (m_Damping / m2) * x[6];
        }
        change[6] = r;

        // V2y'
        r = -m_Gravity - (m_Spring.m_SpringConst / m2) *
                (len - m_Spring.m_RestLength) * yy / len;
        if (r < 0 && Math.abs(x[3] - m_Bottom) < DIST_TOL && Math.abs(x[7]) < -r * timeStep / (2 * m2)) {
            r = x[7] = 0;
            x[3] = m_Bottom;
        } else if (r > 0 && Math.abs(x[3] - m_Top) < DIST_TOL && Math.abs(x[7]) < r * timeStep / (2 * m2)) {
            r = x[7] = 0;
            x[3] = m_Top;
        } else {
            if (m_Damping != 0)
                r -= (m_Damping / m2) * x[7];
        }
        change[7] = r;
    }
}
