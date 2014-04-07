import java.awt.*;

public class PendulumSim extends Simulation {
    private static final String MASS = "mass",
            DAMPING = "damping",
            LENGTH = "length",
            AMPLITUDE = "drive amplitude",
            FREQUENCY = "drive frequency",
            GRAVITY = "gravity",
            RADIUS = "radius";


    private String[] params = {MASS, DAMPING, LENGTH, AMPLITUDE, FREQUENCY, GRAVITY,
            RADIUS};
    private CMass m_Mass;
    private CSpring m_Spring;
    private CArc m_Drive;
    private double driveAmplitude = 1.15;
    private double m_DriveFrequency = 2.0 / 3.0;
    private double m_Gravity = 1.0;

    public PendulumSim(Container container) {
        super(container, 3);
        var_names = new String[]{
                "angle",
                "angular velocity",
                "time",
                "angular accel"
        };

        setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -1.5, 1.5, -1.5, 1.5,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));


        m_Drive = new CArc(0, 0, (0.5 * driveAmplitude), -90, 0);
        cvs.addElement(m_Drive);

        double len = 1;

        m_Spring = new CSpring(0, 0, len, 0.4);
        m_Spring.m_DrawMode = CElement.MODE_LINE;
        cvs.addElement(m_Spring);

        double w = 0.3;

        m_Mass = new CMass(-w / 2 + Math.sin(0) * len,
                -w / 2 + Math.cos(0) * len, w, w, CElement.MODE_CIRCLE);
        m_Spring.setX2(m_Mass.m_X2 + w / 2);
        m_Spring.setY2(m_Mass.m_Y2 + w / 2);
        m_Mass.m_Mass = 1;
        m_Mass.m_Damping = 0.5;
        cvs.addElement(m_Mass);

        vars[0] = Math.PI / 4;
        vars[1] = 0;
        vars[2] = 0;
        modifyObjects();
    }

    public String toString() {
        return "Pendulum simulation";
    }

    public void setupGraph() {
        super.setupGraph();
        if (graph != null) {
            graph.setDrawMode(Graph.DOTS);

            graph.setZVar(3);
        }
    }

    public void setupControls() {
        super.setupControls();

        addObserverControl(new DoubleField(this, MASS, 3));
        addObserverControl(new DoubleField(this, DAMPING, 3));
        addObserverControl(new DoubleField(this, LENGTH, 3));
        addObserverControl(new DoubleField(this, AMPLITUDE, 3));
        addObserverControl(new DoubleField(this, FREQUENCY, 7));
        addObserverControl(new DoubleField(this, GRAVITY, 3));
        showControls(true);
    }

    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(MASS)) {
            m_Mass.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING)) {
            m_Mass.m_Damping = value;
            return true;
        } else if (name.equalsIgnoreCase(LENGTH)) {
            m_Spring.m_RestLength = value;
            return true;
        } else if (name.equalsIgnoreCase(AMPLITUDE)) {
            driveAmplitude = value;
            m_Drive.m_Radius = 0.5 * value;
            return true;
        } else if (name.equalsIgnoreCase(FREQUENCY)) {
            m_DriveFrequency = value;
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            m_Gravity = value;
            return true;
        } else if (name.equalsIgnoreCase(RADIUS)) {
            m_Mass.setHeight(2 * value);
            m_Mass.setWidth(2 * value);
            return true;
        }
        return super.trySetParameter(name, value);
    }

    public double getParameter(String name) {
        if (name.equalsIgnoreCase(MASS))
            return m_Mass.m_Mass;
        else if (name.equalsIgnoreCase(DAMPING))
            return m_Mass.m_Damping;
        else if (name.equalsIgnoreCase(LENGTH))
            return m_Spring.m_RestLength;
        else if (name.equalsIgnoreCase(AMPLITUDE))
            return driveAmplitude;
        else if (name.equalsIgnoreCase(FREQUENCY))
            return m_DriveFrequency;
        else if (name.equalsIgnoreCase(GRAVITY))
            return m_Gravity;
        else if (name.equalsIgnoreCase(RADIUS))
            return m_Mass.getWidth() * 2;
        return super.getParameter(name);
    }

    public String[] getParameterNames() {
        return params;
    }

    public void modifyObjects() {


        if (vars[0] > Math.PI)
            vars[0] = vars[0] - 2 * Math.PI * Math.floor(vars[0] / Math.PI);
        else if (vars[0] < -Math.PI)
            vars[0] = vars[0] - 2 * Math.PI * Math.ceil(vars[0] / Math.PI);


        double len = m_Spring.m_RestLength;
        double w = m_Mass.m_Width / 2;
        m_Mass.setX1(len * Math.sin(vars[0]) - w);
        m_Mass.setY1(len * Math.cos(vars[0]) - w);
        m_Spring.setX2(m_Mass.m_X1 + w);
        m_Spring.setY2(m_Mass.m_Y1 + w);


        double t = m_DriveFrequency * vars[2];

        t = 180 * t / Math.PI;
        t = t - 360 * Math.floor(t / 360);


        if ((t > 0) && (t <= 180))
            t = 90 - t;
        else
            t = t - 270;
        m_Drive.m_Angle = t;
    }

    public int numVariables() {
        return var_names.length;
    }

    public double getVariable(int i) {
        if (i <= 2)
            return vars[i];
        else {
            double[] rate = new double[vars.length];
            evaluate(vars, rate);
            return rate[1];
        }
    }

    public void startDrag(Dragable e) {
        if (e == m_Mass) {
            calc[0] = false;
            calc[1] = false;
        }
    }

    public void constrainedSet(Dragable e, double x, double y) {
        if (e == m_Mass) {


            double w = m_Mass.m_Width / 2;
            double th = Math.atan2(x + w, y + w);
            vars[0] = th;
            vars[1] = 0;
        }
    }


    public void evaluate(double[] x, double[] change) {

        change[0] = x[1];

        double l = m_Spring.m_RestLength;
        double dd = -(m_Gravity / l) * Math.sin(x[0]);
        double mlsq = m_Mass.m_Mass * l * l;
        dd += -(m_Mass.m_Damping / mlsq) * x[1];
        dd += (driveAmplitude / mlsq) * Math.cos(m_DriveFrequency * x[2]);
        change[1] = dd;
        change[2] = 1;
    }

}
