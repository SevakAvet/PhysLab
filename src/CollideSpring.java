import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Vector;


public class CollideSpring extends CollidingSim implements ActionListener {
    private static final int ID_MASS1 = 1, ID_MASS2 = 2, ID_LEFT_WALL = 3, ID_RIGHT_WALL = 4;
    private static final String MASS1 = "mass1", DAMPING = "damping",
            LENGTH = "spring rest length", STIFFNESS = "spring stiffness",
            MASS2 = "mass2";
    private String[] params = {MASS1, DAMPING, STIFFNESS, LENGTH, MASS2};
    private double damping = 0;
    private CSpring m_Spring;
    private CMass m_Mass1, m_Mass2;
    private CWall m_Wall, m_Wall2;
    private JButton button_stop;
    private Vector collisions = new Vector(10);

    public CollideSpring(Container container) {
        super(container, 4);
        var_names = new String[]{
                "position 1",
                "velocity 1",
                "position 2",
                "velocity 2"
        };

        setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -0.5, 7.5, -2, 2,
                CoordMap.ALIGN_LEFT, CoordMap.ALIGN_MIDDLE));

        double xx = 0.0;
        double yy = 0.0;
        m_Wall = new CWall(xx - 0.3, yy - 2, xx, yy + 2, 0);
        cvs.addElement(m_Wall);

        m_Spring = new CSpring(xx, yy, 2.5, 0.4);
        m_Spring.m_SpringConst = 6;
        cvs.addElement(m_Spring);

        double w = 0.3;
        m_Mass1 = new CMass(xx + m_Spring.m_RestLength - 2.0, yy - w, 2 * w, 2 * w, CElement.MODE_RECT);
        m_Spring.setX2(m_Mass1.m_X1);
        m_Mass1.m_Mass = 0.5;
        damping = 0;
        cvs.addElement(m_Mass1);

        m_Mass2 = new CMass(m_Mass1.m_X2 + 1.0, yy - w, 2 * w, 2 * w, CElement.MODE_RECT);
        m_Mass2.m_Mass = 1.5;
        cvs.addElement(m_Mass2);

        m_Wall2 = new CWall(7, yy - 2, 7.4, yy + 2, -90);
        cvs.addElement(m_Wall2);

        vars[0] = m_Mass1.m_X1;
        vars[1] = 0;
        vars[2] = m_Mass2.m_X1;
        vars[3] = 0;
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
            graph.setVars(0, 2);
        showGraph(false);
    }


    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(MASS1)) {
            m_Mass1.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(MASS2)) {
            m_Mass2.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING)) {
            damping = value;
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
        else if (name.equalsIgnoreCase(MASS2))
            return m_Mass2.m_Mass;
        else if (name.equalsIgnoreCase(DAMPING))
            return damping;
        else if (name.equalsIgnoreCase(STIFFNESS))
            return m_Spring.m_SpringConst;
        else if (name.equalsIgnoreCase(LENGTH))
            return m_Spring.m_RestLength;
        return super.getParameter(name);
    }


    public String[] getParameterNames() {
        return params;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button_stop) {
            vars[0] = 1;
            vars[1] = 0;
            vars[2] = 3;
            vars[3] = 0;
        }
    }

    public void modifyObjects() {
        m_Mass1.setX1(vars[0]);
        m_Spring.setX2(m_Mass1.m_X1);
        m_Mass2.setX1(vars[2]);
    }

    public void startDrag(Dragable e) {
        calc[0] = calc[1] = calc[2] = calc[3] = false;
    }

    public void constrainedSet(Dragable e, double x, double y) {
        if (e == m_Mass1) {
            if (x < m_Wall.m_X2)
                x = m_Wall.m_X2;

            if (x + m_Mass1.m_Width + m_Mass2.m_Width > m_Wall2.m_X1)
                x = m_Wall2.m_X1 - m_Mass2.m_Width - m_Mass1.m_Width;
            if (x + m_Mass1.m_Width > m_Mass2.m_X1)
                m_Mass2.setX1(x + m_Mass1.m_Width);
            m_Mass1.setX1(x);
            m_Spring.setX2(x);
            vars[0] = m_Mass1.m_X1;
            vars[1] = 0;
            vars[2] = m_Mass2.m_X1;
            vars[3] = 0;
        } else if (e == m_Mass2) {
            if (x + m_Mass2.m_Width > m_Wall2.m_X1)
                x = m_Wall2.m_X1 - m_Mass2.m_Width;
            if (x - m_Mass1.m_Width < m_Wall.m_X2)
                x = m_Wall.m_X2 + m_Mass1.m_Width;
            if (x < m_Mass1.m_X2) {

                m_Mass1.setX1(x - m_Mass1.m_Width - 0.001);
                m_Spring.setX2(m_Mass1.m_X1);
            }
            m_Mass2.setX1(x);
            vars[0] = m_Mass1.m_X1;
            vars[1] = 0;
            vars[2] = m_Mass2.m_X1;
            vars[3] = 0;
        }

    }

    private void addCollision(int obj1, int obj2) {
        collisions.addElement(new int[]{obj1, obj2});
    }

    public Vector findAllCollisions() {

        collisions.removeAllElements();
        if (m_Mass1.m_X1 < m_Wall.m_X2)
            addCollision(ID_LEFT_WALL, ID_MASS1);
        if (m_Mass1.m_X2 > m_Mass2.m_X1)
            addCollision(ID_MASS1, ID_MASS2);
        if (m_Mass2.m_X2 > m_Wall2.m_X1)
            addCollision(ID_RIGHT_WALL, ID_MASS2);
        return (collisions.size() > 0) ? collisions : null;
    }


    public void handleCollisions(Vector collisions) {
        for (Enumeration e = collisions.elements(); e.hasMoreElements(); ) {
            int[] objs = (int[]) e.nextElement();
            if (objs[0] == ID_LEFT_WALL) {

                vars[1] = -vars[1];
            } else if (objs[0] == ID_RIGHT_WALL) {

                vars[3] = -vars[3];
            } else if ((objs[0] == ID_MASS1) && (objs[1] == ID_MASS2)) {


                double vcm = (m_Mass1.m_Mass * vars[1] + m_Mass2.m_Mass * vars[3])
                        / (m_Mass1.m_Mass + m_Mass2.m_Mass);


                vars[1] = -vars[1] + 2 * vcm;
                vars[3] = -vars[3] + 2 * vcm;
            }
        }
    }


    public void evaluate(double[] x, double[] change) {
        change[0] = x[1];

        double r = -m_Spring.m_SpringConst * (x[0] - m_Spring.m_X1
                - m_Spring.m_RestLength) - damping * x[1];
        change[1] = r / m_Mass1.m_Mass;
        change[2] = x[3];
        change[3] = 0;
    }

}
