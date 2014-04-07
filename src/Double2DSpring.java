import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class Double2DSpring extends Simulation implements ActionListener {
    private static final String MASS1 = "mass1",
            MASS2 = "mass2",
            LENGTH1 = "spring1 length",
            LENGTH2 = "spring2 length",
            STIFF1 = "spring1 stiffness",
            STIFF2 = "spring2 stiffness",
            DAMPING = "damping",
            GRAVITY = "gravity";
    private String[] params = {MASS1, MASS2, LENGTH1, LENGTH2, STIFF1, STIFF2,
            DAMPING, GRAVITY};
    private CMass mass1, mass2, topMass;
    private CSpring spring1, spring2;
    private double gravity = 9.8, damping = 0.0;
    private JButton button_stop;

    public Double2DSpring(Container container) {
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
        setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -6, 6, -6, 6,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
        double xx = 0, yy = -2, w = 0.5;
        topMass = new CMass(xx - w / 2, yy - w, w, w, CElement.MODE_RECT);
        cvs.addElement(topMass);

        spring1 = new CSpring(xx, yy, 1.0, 0.3);
        spring1.setX2(xx);
        spring1.m_SpringConst = 6;
        cvs.addElement(spring1);

        mass1 = new CMass(xx - w / 2, 0, w, w, CElement.MODE_CIRCLE);
        mass1.m_Mass = .5;
        mass2 = new CMass(xx - w / 2, 0, w, w, CElement.MODE_CIRCLE);
        mass2.m_Mass = .5;

        double yy2 = yy + spring1.m_RestLength +
                (mass1.m_Mass + mass2.m_Mass) * gravity / spring1.m_SpringConst;
        mass1.setY1(yy2 - w / 2);
        spring1.setY2(yy2);
        mass1.m_Damping = 0;
        cvs.addElement(mass1);

        spring2 = new CSpring(xx, yy2, 1.0, 0.3);
        spring2.setX2(xx);
        spring2.m_SpringConst = 6;
        cvs.addElement(spring2);


        double yy3 = yy2 + spring2.m_RestLength +
                mass2.m_Mass * gravity / spring2.m_SpringConst;
        mass2.setY1(yy3 - w / 2);
        spring2.setY2(yy3);
        mass2.m_Damping = 0;
        cvs.addElement(mass2);

        stopMotion();
        vars[0] += 0.5;
        vars[1] += 0.5;
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
    }


    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(MASS1)) {
            mass1.m_Mass = value;
            return true;
        }
        if (name.equalsIgnoreCase(MASS2)) {
            mass2.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(LENGTH1)) {
            spring1.m_RestLength = value;
            return true;
        } else if (name.equalsIgnoreCase(LENGTH2)) {
            spring2.m_RestLength = value;
            return true;
        } else if (name.equalsIgnoreCase(STIFF1)) {
            spring1.m_SpringConst = value;
            return true;
        } else if (name.equalsIgnoreCase(STIFF2)) {
            spring2.m_SpringConst = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING)) {
            damping = value;
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            gravity = value;
            return true;
        }
        return super.trySetParameter(name, value);
    }


    public double getParameter(String name) {
        if (name.equalsIgnoreCase(MASS1))
            return mass1.m_Mass;
        if (name.equalsIgnoreCase(MASS2))
            return mass2.m_Mass;
        else if (name.equalsIgnoreCase(LENGTH1))
            return spring1.m_RestLength;
        else if (name.equalsIgnoreCase(LENGTH2))
            return spring2.m_RestLength;
        else if (name.equalsIgnoreCase(STIFF1))
            return spring1.m_SpringConst;
        else if (name.equalsIgnoreCase(STIFF2))
            return spring2.m_SpringConst;
        else if (name.equalsIgnoreCase(DAMPING))
            return damping;
        else if (name.equalsIgnoreCase(GRAVITY))
            return gravity;
        return super.getParameter(name);
    }


    public String[] getParameterNames() {
        return params;
    }

    private void stopMotion() {
        double m1 = mass1.m_Mass;
        double m2 = mass2.m_Mass;
        double k1 = spring1.m_SpringConst;
        double k2 = spring2.m_SpringConst;
        double r1 = spring1.m_RestLength;
        double r2 = spring2.m_RestLength;
        double T = topMass.m_Y2;


        vars[0] = vars[2] = topMass.m_X1 + topMass.m_Width / 2;


        vars[1] = gravity * (m1 + m2) / k1 + r1 + T;

        vars[3] = gravity * (m2 / k2 + (m1 + m2) / k1) + r1 + r2 + T;

        vars[4] = vars[5] = vars[6] = vars[7] = 0;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button_stop) {
            stopMotion();
        }
    }

    public void modifyObjects() {

        double w = mass1.m_Width / 2;
        mass1.setPosition(vars[0] - w, vars[1] - w);
        mass2.setPosition(vars[2] - w, vars[3] - w);
        spring1.setX2(mass1.m_X1 + w);
        spring1.setY2(mass1.m_Y1 + w);
        spring2.setBounds(mass1.m_X1 + w, mass1.m_Y1 + w, mass2.m_X1 + w, mass2.m_Y1 + w);
    }

    public void startDrag(Dragable e) {
        if (e == mass1) {
            calc[0] = calc[1] = calc[4] = calc[5] = false;
        } else if (e == mass2) {
            calc[2] = calc[3] = calc[6] = calc[7] = false;
        }
    }

    public void constrainedSet(Dragable e, double x, double y) {


        double w = mass1.m_Width / 2;
        if (e == topMass) {
            e.setPosition(x, y);

            spring1.setPosition(x + topMass.m_Width / 2, y + topMass.m_Height);
        } else if (e == mass1) {
            vars[0] = x + w;
            vars[1] = y + w;
            vars[4] = vars[5] = 0;
            modifyObjects();
        } else if (e == mass2) {
            vars[2] = x + w;
            vars[3] = y + w;
            vars[6] = vars[7] = 0;
            modifyObjects();
        }
    }


    public void evaluate(double[] x, double[] change) {
        double xx = x[0] - spring1.m_X1;
        double yy = x[1] - spring1.m_Y1;
        double len1 = Math.sqrt(xx * xx + yy * yy);
        double m1 = mass1.m_Mass;
        double xx2 = x[2] - x[0];
        double yy2 = x[3] - x[1];
        double len2 = Math.sqrt(xx2 * xx2 + yy2 * yy2);
        double m2 = mass2.m_Mass;

        change[0] = x[4];
        change[1] = x[5];
        change[2] = x[6];
        change[3] = x[7];


        double r = -(spring1.m_SpringConst / m1) *
                (len1 - spring1.m_RestLength) * xx / len1;

        r += (spring2.m_SpringConst / m1) *
                (len2 - spring2.m_RestLength) * xx2 / len2;

        if (damping != 0) {
            r -= (damping / m1) * x[4];
        }
        change[4] = r;


        r = gravity - (spring1.m_SpringConst / m1) *
                (len1 - spring1.m_RestLength) * yy / len1;

        r += (spring2.m_SpringConst / m1) *
                (len2 - spring2.m_RestLength) * yy2 / len2;

        if (damping != 0) {
            r -= (damping / m1) * x[5];
        }
        change[5] = r;


        r = -(spring2.m_SpringConst / m2) *
                (len2 - spring2.m_RestLength) * xx2 / len2;

        if (damping != 0) {
            r -= (damping / m2) * x[6];
        }
        change[6] = r;


        r = gravity - (spring2.m_SpringConst / m2) *
                (len2 - spring2.m_RestLength) * yy2 / len2;

        if (damping != 0) {
            r -= (damping / m2) * x[7];
        }
        change[7] = r;
    }
}
