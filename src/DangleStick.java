import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class DangleStick extends Simulation implements ActionListener {
    private static final String MASS1 = "upper mass",
            MASS2 = "lower mass",
            GRAVITY = "gravity", STICK_LENGTH = "stick length",
            SPRING_LENGTH = "spring rest length", STIFFNESS = "spring stiffness";


    private String[] params = {MASS1, MASS2, STIFFNESS, SPRING_LENGTH,
            STICK_LENGTH, GRAVITY};
    private CMass m_Mass1, m_Mass2;
    private CSpring m_Spring, m_Stick;
    private double gravity = 9.8;
    private JButton button_stop;


    public DangleStick(Container container) {
        super(container, 6);
        var_names = new String[]{
                "spring angle",
                "spring angle vel",
                "spring length",
                "spring length vel",
                "stick angle",
                "stick angle vel"
        };
        setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -2, 2, -4, 2,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
        double w = 0.3;

        m_Mass1 = new CMass(0.4, -1.2, w, w, CElement.MODE_CIRCLE);
        m_Mass1.m_Mass = 0.5;
        cvs.addElement(m_Mass1);


        m_Stick = new CSpring(0.4, -1.2, 1, 0.4);
        m_Stick.m_DrawMode = CElement.MODE_LINE;
        cvs.addElement(m_Stick);

        m_Mass2 = new CMass(0.4, -2.2, w, w, CElement.MODE_CIRCLE);


        m_Mass2.m_Mass = 0.5;
        m_Mass2.m_Damping = 0;
        cvs.addElement(m_Mass2);


        m_Spring = new CSpring(0, 0, 1, 0.4);
        m_Spring.m_SpringConst = 20;


        cvs.addElement(m_Spring);

        vars[0] = (Math.PI * 30) / 180;
        vars[1] = 0;
        vars[2] = 2.0;
        vars[3] = 0;
        vars[4] = (-Math.PI * 30) / 180;
        vars[5] = 0;
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
        } else if (name.equalsIgnoreCase(MASS2)) {
            m_Mass2.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            gravity = value;
            return true;
        } else if (name.equalsIgnoreCase(STIFFNESS)) {
            m_Spring.m_SpringConst = value;
            return true;
        } else if (name.equalsIgnoreCase(SPRING_LENGTH)) {
            m_Spring.m_RestLength = value;
            return true;
        } else if (name.equalsIgnoreCase(STICK_LENGTH)) {
            m_Stick.m_RestLength = value;
            return true;
        }
        return super.trySetParameter(name, value);
    }


    public double getParameter(String name) {
        if (name.equalsIgnoreCase(MASS1))
            return m_Mass1.m_Mass;
        else if (name.equalsIgnoreCase(MASS2))
            return m_Mass2.m_Mass;
        else if (name.equalsIgnoreCase(GRAVITY))
            return gravity;
        else if (name.equalsIgnoreCase(STIFFNESS))
            return m_Spring.m_SpringConst;
        else if (name.equalsIgnoreCase(SPRING_LENGTH))
            return m_Spring.m_RestLength;
        else if (name.equalsIgnoreCase(STICK_LENGTH))
            return m_Stick.m_RestLength;
        return super.getParameter(name);
    }


    public String[] getParameterNames() {
        return params;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button_stop)
            stop();
    }

    private void stop() {
        vars[0] = vars[1] = vars[3] = vars[4] = vars[5] = 0;
        double r = gravity * (m_Mass1.m_Mass + m_Mass2.m_Mass) / m_Spring.m_SpringConst;
        vars[2] = m_Spring.m_RestLength + r;
    }

    public void modifyObjects() {


        double w = m_Mass1.m_Width / 2;
        m_Mass1.setX1(vars[2] * Math.sin(vars[0]) - w);
        m_Mass1.setY1(-vars[2] * Math.cos(vars[0]) - w);
        double L = m_Stick.m_RestLength;
        m_Mass2.setX1(m_Mass1.m_X1 + L * Math.sin(vars[4]));
        m_Mass2.setY1(m_Mass1.m_Y1 - L * Math.cos(vars[4]));
        m_Spring.setX2(m_Mass1.m_X1 + w);
        m_Spring.setY2(m_Mass1.m_Y1 + w);
        m_Stick.setX1(m_Mass1.m_X1 + w);
        m_Stick.setY1(m_Mass1.m_Y1 + w);
        m_Stick.setX2(m_Mass2.m_X1 + w);
        m_Stick.setY2(m_Mass2.m_Y1 + w);
    }

    public void startDrag(Dragable e) {

        for (int i = 0; i < vars.length; i++)
            calc[i] = false;
    }

    public void constrainedSet(Dragable e, double x, double y) {
        double w = m_Mass1.m_Width / 2;
        if (e == m_Mass1) {


            double xx = x + w;
            double yy = y + w;
            double th = Math.atan2(xx, -yy);
            vars[0] = th;
            vars[2] = Math.sqrt(xx * xx + yy * yy);
            vars[1] = 0;
            vars[3] = 0;
            vars[5] = 0;
        } else if (e == m_Mass2) {

            double x1 = vars[2] * Math.sin(vars[0]);
            double y1 = -vars[2] * Math.cos(vars[0]);

            double x2 = x + w;
            double y2 = y + w;
            double th = Math.atan2(x2 - x1, -(y2 - y1));
            vars[4] = th;
            vars[1] = 0;
            vars[3] = 0;
            vars[5] = 0;
        }
    }


    public void evaluate(double[] x, double[] change) {
        double m2 = m_Mass2.m_Mass;
        double m1 = m_Mass1.m_Mass;
        double L = m_Stick.m_RestLength;
        double k = m_Spring.m_SpringConst;
        double b = m_Spring.m_RestLength;
        change[0] = x[1];
        change[2] = x[3];
        change[4] = x[5];

        double sum = -4 * m1 * (m1 + m2) * x[3] * x[1];
        sum += 2 * m1 * m2 * L * x[5] * x[5] * Math.sin(x[4] - x[0]);
        sum -= 2 * gravity * m1 * (m1 + m2) * Math.sin(x[0]);
        sum += k * m2 * (b - x[2]) * Math.sin(2 * (x[0] - x[4]));
        sum = sum / (2 * m1 * (m1 + m2) * x[2]);
        change[1] = sum;


        sum = 2 * b * k * m1 + b * k * m2 - 2 * k * m1 * x[2] - k * m2 * x[2];
        sum -= k * m2 * (b - x[2]) * Math.cos(2 * (x[0] - x[4]));
        sum += 2 * L * m1 * m2 * Math.cos(x[4] - x[0]) * x[5] * x[5];
        sum = sum / (2 * m1 * (m1 + m2));
        sum += x[2] * x[1] * x[1];
        sum += gravity * Math.cos(x[0]);
        change[3] = sum;


        change[5] = k * (b - x[2]) * Math.sin(x[4] - x[0]) / (L * m1);
    }
}
