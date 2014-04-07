import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class PendulumCart extends Simulation implements ActionListener {
    private static final String MASS_CART = "cart mass",
            MASS_PENDULUM = "pendulum mass",
            DAMPING_CART = "cart damping",
            DAMPING_PENDULUM = "pendulum damping",
            LENGTH_PENDULUM = "pendulum length",
            STIFFNESS = "spring stiffness",
            GRAVITY = "gravity",
            SHOW_ENERGY = "show energy";
    private String[] params = {MASS_CART, MASS_PENDULUM, LENGTH_PENDULUM,
            STIFFNESS, GRAVITY, DAMPING_CART, DAMPING_PENDULUM, SHOW_ENERGY};
    private CMass cart, pendulum;
    private CSpring rod, spring;
    private BarChart chart;
    private boolean showEnergy = true;
    private double gravity = 9.8;
    private double damping_cart = 0.1;
    private double damping_pendulum = 0.1;
    private JButton button_stop;

    public PendulumCart(Container container) {
        super(container, 5);
        var_names = new String[]{
                "cart position",
                "pendulum angle",
                "cart velocity",
                "angle velocity",
                "work done by damping"
        };
        setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -3, 3, -2, 2,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

        double len = 1;


        rod = new CSpring(0, 0, len, 0.4);
        rod.m_DrawMode = CElement.MODE_LINE;
        rod.m_SpringConst = 6;
        cvs.addElement(rod);

        double w = 0.3;


        cart = new CMass(-w / 2, -w / 2, w, w, CElement.MODE_RECT);
        cart.m_Mass = 1;
        cvs.addElement(cart);


        pendulum = new CMass(-w / 2 - Math.sin(0) * len,
                w / 2 - Math.cos(0) * len, w, w, CElement.MODE_CIRCLE);
        rod.setX2(pendulum.m_X2 + w / 2);
        rod.setY2(pendulum.m_Y2 + w / 2);
        pendulum.m_Mass = 1;
        pendulum.m_Damping = 0;
        cvs.addElement(pendulum);


        spring = new CSpring(3, 0, 3, 0.4);
        spring.m_SpringConst = 6;
        cvs.addElement(spring);
        chart = new BarChart(cvs.getSimBounds());
        chart.tes = "kinetic energy";
        if (showEnergy)
            cvs.addElement(chart);

        stop();
        vars[1] = Math.PI / 8;
        modifyObjects();
        initWork();
    }

    public void setupControls() {
        super.setupControls();
        addControl(button_stop = new JButton("reset"));
        button_stop.addActionListener(this);

        for (int i = 0; i < params.length - 1; i++)
            addObserverControl(new DoubleField(this, params[i], 2));
        addObserverControl(new MyCheckbox(this, SHOW_ENERGY));
        showControls(true);
    }

    public void setupGraph() {
        super.setupGraph();
        if (graph != null) {
            graph.setVars(0, 1);
            showGraph(true);
        }
    }


    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(MASS_CART)) {
            cart.m_Mass = value;
            initWork();
            return true;
        } else if (name.equalsIgnoreCase(MASS_PENDULUM)) {
            pendulum.m_Mass = value;
            initWork();
            return true;
        } else if (name.equalsIgnoreCase(DAMPING_CART)) {
            damping_cart = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING_PENDULUM)) {
            damping_pendulum = value;
            return true;
        } else if (name.equalsIgnoreCase(STIFFNESS)) {
            spring.m_SpringConst = value;
            initWork();
            return true;
        } else if (name.equalsIgnoreCase(LENGTH_PENDULUM)) {
            rod.m_RestLength = value;
            initWork();
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            gravity = value;
            initWork();
            return true;
        } else if (name.equalsIgnoreCase(SHOW_ENERGY)) {
            showEnergy = value != 0;
            boolean chartVisible = cvs.containsElement(chart);
            if (showEnergy && !chartVisible)
                cvs.addElement(chart);
            else if (!showEnergy && chartVisible)
                cvs.removeElement(chart);
            return true;
        }
        return super.trySetParameter(name, value);
    }


    public double getParameter(String name) {
        if (name.equalsIgnoreCase(MASS_CART))
            return cart.m_Mass;
        else if (name.equalsIgnoreCase(MASS_PENDULUM))
            return pendulum.m_Mass;
        else if (name.equalsIgnoreCase(DAMPING_CART))
            return damping_cart;
        else if (name.equalsIgnoreCase(DAMPING_PENDULUM))
            return damping_pendulum;
        else if (name.equalsIgnoreCase(STIFFNESS))
            return spring.m_SpringConst;
        else if (name.equalsIgnoreCase(LENGTH_PENDULUM))
            return rod.m_RestLength;
        else if (name.equalsIgnoreCase(GRAVITY))
            return gravity;
        else if (name.equalsIgnoreCase(SHOW_ENERGY))
            return showEnergy ? 1 : 0;
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
        for (int i = 0; i < vars.length; i++)
            vars[i] = 0;
    }

    public void setVariable(int i, double value) {
        super.setVariable(i, value);
        initWork();
    }

    private void initWork() {
        vars[4] = 0;
        calcEnergy();
        chart.setWorkZero(chart.te + chart.pe);
    }

    private void calcEnergy() {
        chart.te = 0.5 * cart.m_Mass * vars[2] * vars[2];
        double csh = Math.cos(vars[1]);
        double snh = Math.sin(vars[1]);
        double d1 = vars[2] + rod.m_RestLength * vars[3] * csh;
        double d2 = rod.m_RestLength * vars[3] * snh;
        chart.te += 0.5 * pendulum.m_Mass * (d1 * d1 + d2 * d2);
        chart.pe = 0.5 * spring.m_SpringConst * vars[0] * vars[0];
        chart.pe += pendulum.m_Mass * gravity * rod.m_RestLength * (1 - csh);
        chart.work = vars[4];
    }

    public void modifyObjects() {


        double w = cart.m_Width / 2;
        cart.setX1(vars[0] - w);
        double L = rod.m_RestLength;
        pendulum.setX1(cart.m_X1 + L * Math.sin(vars[1]));
        pendulum.setY1(cart.m_Y1 - L * Math.cos(vars[1]));

        rod.setX1(cart.m_X1 + w);
        rod.setX2(pendulum.m_X1 + w);
        rod.setY2(pendulum.m_Y1 + w);

        spring.setX2(cart.m_X1 + w);
        calcEnergy();
    }

    public void startDrag(Dragable e) {

        for (int i = 0; i < vars.length; i++)
            calc[i] = false;
    }

    public void constrainedSet(Dragable e, double x, double y) {

        double w = cart.m_Width / 2;
        if (e == cart) {
            vars[0] = x + w;
            vars[2] = 0;
            initWork();
        } else if (e == pendulum) {

            double x1 = vars[0];
            double y1 = -w;

            double x2 = x + w;
            double y2 = y;
            double th = Math.atan2(x2 - x1, -(y2 - y1));
            vars[1] = th;
            vars[3] = 0;
            initWork();
        }
    }


    public void evaluate(double[] x, double[] change) {
        double m = pendulum.m_Mass;
        double M = cart.m_Mass;
        double L = rod.m_RestLength;
        double k = spring.m_SpringConst;
        double sh = Math.sin(x[1]);
        double csh = Math.cos(x[1]);
        double cs2h = csh * csh - sh * sh;

        change[0] = x[2];
        change[1] = x[3];


        double numer = m * x[3] * x[3] * L * sh + m * gravity * sh * csh - k * x[0]
                - damping_cart * x[2] + damping_pendulum * x[3] * csh / L;
        change[2] = numer / (M + m * sh * sh);


        numer = -m * x[3] * x[3] * L * sh * csh + k * x[0] * csh - (M + m) * gravity * sh + damping_cart * x[2] * csh;
        numer += -(m + M) * damping_pendulum * x[3] / (m * L);
        change[3] = numer / (L * (M + m * sh * sh));
        change[4] = -damping_cart * x[2] * x[2] - damping_pendulum * x[3] * x[3];
    }
}
