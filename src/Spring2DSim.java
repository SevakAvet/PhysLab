import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Spring2DSim extends Simulation implements ActionListener {
    private static final String MASS = "масса", DAMPING = "сопротивление",
            LENGTH = "длина пружины", STIFFNESS = "жесткость",
            GRAVITY = "ускорение";
    private String[] params = {MASS, DAMPING, STIFFNESS, LENGTH, GRAVITY};
    private CSpring spring;
    private CMass bob, topMass;
    private JButton button_stop;
    private double gravity = 9.8;
    private double m_Damping = 0;

    public Spring2DSim(Container container) {
        super(container, 4);
        setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -6, 6.0, -6, 6,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
        double w = 1;

        topMass = new CMass(-w / 2, 3, w, w, CElement.MODE_RECT);
        cvs.addElement(topMass);

        bob = new CMass(-2.5, -2, w, w, CElement.MODE_CIRCLE);
        bob.m_Mass = 0.5;
        cvs.addElement(bob);


        spring = new CSpring(topMass.m_X1 + w / 2, topMass.m_Y1, 2.5, 0.6);
        spring.m_SpringConst = 6.0;
        cvs.addElement(spring);

        var_names = new String[]{
                "x координата",
                "y координата",
                "x ускорение",
                "y ускорение"
        };

        vars[0] = bob.m_X1 + bob.m_Width / 2;
        vars[1] = bob.m_Y1 + bob.m_Height / 2;
        vars[2] = 0;
        vars[3] = 0;
        modifyObjects();
    }

    public void setupControls() {
        super.setupControls();

        addControl(button_stop = new JButton("сбросить"));
        button_stop.addActionListener(this);

        for (String param : params) addObserverControl(new DoubleField(this, param, 2));
        showControls(true);
    }

    public void setupGraph() {
        super.setupGraph();
        if (graph != null)
            graph.setVars(0, 1);
    }

    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(MASS)) {
            bob.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING)) {
            m_Damping = value;
            return true;
        } else if (name.equalsIgnoreCase(STIFFNESS)) {
            spring.m_SpringConst = value;
            return true;
        } else if (name.equalsIgnoreCase(LENGTH)) {
            spring.m_RestLength = value;
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            gravity = value;
            return true;
        }
        return super.trySetParameter(name, value);
    }


    public double getParameter(String name) {
        if (name.equalsIgnoreCase(MASS))
            return bob.m_Mass;
        else if (name.equalsIgnoreCase(DAMPING))
            return m_Damping;
        else if (name.equalsIgnoreCase(STIFFNESS))
            return spring.m_SpringConst;
        else if (name.equalsIgnoreCase(LENGTH))
            return spring.m_RestLength;
        else if (name.equalsIgnoreCase(GRAVITY))
            return gravity;
        return super.getParameter(name);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button_stop) {
            vars[0] = topMass.m_X1 + topMass.m_Width / 2;
            vars[1] = topMass.m_Y1 - spring.m_RestLength -
                    bob.m_Mass * gravity / spring.m_SpringConst;
            vars[2] = 0;
            vars[3] = 0;
        }
    }

    public void modifyObjects() {
        bob.setX1(vars[0] - bob.m_Width / 2);
        bob.setY1(vars[1] - bob.m_Height / 2);
        spring.setX2(vars[0]);
        spring.setY2(vars[1]);
    }

    public void startDrag(Dragable e) {
        if (e == bob) {
            calc[0] = false;
            calc[1] = false;
            calc[2] = false;
            calc[3] = false;
        }
    }

    public void constrainedSet(Dragable e, double x, double y) {
        if (e == topMass) {
            ((CElement) e).setX1(x);
            ((CElement) e).setY1(y);
            spring.setX1(x + topMass.m_Width / 2);
            spring.setY1(y);
        } else if (e == bob) {
            vars[0] = x + bob.m_Width / 2;
            vars[1] = y + bob.m_Height / 2;
            vars[2] = 0;
            vars[3] = 0;
            modifyObjects();
        }
    }


    public void evaluate(double[] x, double[] change) {
        double xx, yy, len, m, r, b;
        m = bob.m_Mass;
        b = m_Damping;

        xx = x[0] - spring.m_X1;
        yy = -x[1] + spring.m_Y1;
        len = Math.sqrt(xx * xx + yy * yy);

        change[0] = x[2];
        change[1] = x[3];

        r = -(spring.m_SpringConst / m) * (len - spring.m_RestLength) * xx / len;
        if (b != 0)
            r -= (b / m) * x[2];
        change[2] = r;


        r = -gravity + (spring.m_SpringConst / m) * (len - spring.m_RestLength) * yy / len;
        if (b != 0)
            r -= (b / m) * x[3];
        change[3] = r;
    }
}


