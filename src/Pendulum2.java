import java.awt.*;
import java.util.Vector;

public class Pendulum2 extends Simulation {
    private static final String MASS = "mass",
            DAMPING = "damping",
            LENGTH = "length",
            AMPLITUDE = "drive amplitude",
            FREQUENCY = "drive frequency",
            GRAVITY = "gravity",
            RADIUS = "radius";


    private String[] params = {MASS, DAMPING, LENGTH, AMPLITUDE, FREQUENCY, GRAVITY,
            RADIUS};
    protected Vector rxnForces = new Vector(20);
    private CMass m_Mass, m_Mass2;
    private double radius = 0.4;
    private double mass = 1.0;
    private double damping = 0;
    private CSpring m_Spring, m_Spring2;
    private double m_Gravity = 1.0;
    private double offsetX = 0.5;
    private double offsetY = 0;

    public Pendulum2(Container container) {
        super(container, 8);
        var_names = new String[]{
                "x position",
                "x velocity",
                "y position",
                "y velocity",
                "angle",
                "angular velocity",
                "angle2",
                "angle2 velocity"
        };

        setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -1.5, 1.5, -1.5, 1.5,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

        double len = 1.0;


        double len2 = (radius * radius / 2.0 + len * len) / len;
        System.out.println("len=" + len + " len2=" + len2);
        m_Spring2 = new CSpring(0, 0, len2, 0.4);
        m_Spring2.m_DrawMode = CElement.MODE_LINE;
        m_Spring2.m_Color = Color.blue;
        cvs.addElement(m_Spring2);
        m_Mass2 = new CMass(0, 0, radius * 2, radius * 2, CElement.MODE_CIRCLE);
        cvs.addElement(m_Mass2);


        m_Spring = new CSpring(0, 0, len, 0.4);
        m_Spring.m_DrawMode = CElement.MODE_LINE;
        m_Spring.m_Color = Color.green;
        cvs.addElement(m_Spring);


        m_Mass = new CMass(0, 0, radius * 2, radius * 2, CElement.MODE_CIRCLE);


        cvs.addElement(m_Mass);


        vars[4] = 3 * Math.PI / 4;
        vars[0] = len * Math.sin(vars[4]);
        vars[2] = -len * Math.cos(vars[4]);
        vars[1] = vars[3] = vars[5] = 0;
        vars[6] = vars[4];
        vars[7] = 0;
        modifyObjects();
    }

    public String toString() {
        return "Pendulum2 simulation";
    }

    public void setupGraph() {
        super.setupGraph();
        if (graph != null) {
            graph.setDrawMode(Graph.DOTS);
            graph.setXVar(4);
            graph.setYVar(5);


        }
    }

    public void setupControls() {
        super.setupControls();

        addObserverControl(new DoubleField(this, MASS, 3));
        addObserverControl(new DoubleField(this, DAMPING, 3));
        addObserverControl(new DoubleField(this, LENGTH, 3));
        addObserverControl(new DoubleField(this, GRAVITY, 3));
        showControls(true);
    }

    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(MASS)) {
            mass = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING)) {
            damping = value;
            return true;
        } else if (name.equalsIgnoreCase(LENGTH)) {


            m_Spring.m_RestLength = value;
            vars[0] = value * Math.sin(vars[4]);
            vars[2] = -value * Math.cos(vars[4]);
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            m_Gravity = value;
            return true;
        }
        return super.trySetParameter(name, value);
    }

    public double getParameter(String name) {
        if (name.equalsIgnoreCase(MASS))
            return mass;
        else if (name.equalsIgnoreCase(DAMPING))
            return damping;
        else if (name.equalsIgnoreCase(LENGTH))
            return m_Spring.m_RestLength;
        else if (name.equalsIgnoreCase(GRAVITY))
            return m_Gravity;
        return super.getParameter(name);
    }

    public String[] getParameterNames() {
        return params;
    }

    public void modifyObjects() {


        if (vars[4] > Math.PI)
            vars[4] = vars[4] - 2 * Math.PI * Math.floor(vars[4] / Math.PI);
        else if (vars[4] < -Math.PI)
            vars[4] = vars[4] - 2 * Math.PI * Math.ceil(vars[4] / Math.PI);


        double len = m_Spring.m_RestLength;


        m_Spring.setX1(vars[0] - len * Math.sin(vars[4]));
        m_Spring.setY1(vars[2] + len * Math.cos(vars[4]));

        m_Spring.setX2(vars[0]);
        m_Spring.setY2(vars[2]);
        m_Mass.setCenterX(m_Spring.m_X2);
        m_Mass.setCenterY(m_Spring.m_Y2);

        double len2 = m_Spring2.m_RestLength;
        m_Spring2.setX1(0 + offsetX);
        m_Spring2.setY1(0 + offsetY);
        m_Spring2.setX2(len2 * Math.sin(vars[6]) + offsetX);
        m_Spring2.setY2(-len2 * Math.cos(vars[6]) + offsetY);
        m_Mass2.setCenterX(m_Spring2.m_X2);
        m_Mass2.setCenterY(m_Spring2.m_Y2);

    }

    public int numVariables() {
        return var_names.length;
    }


    public void evaluate2(double[] x, double[] change) {
        double m = mass;
        change[0] = x[1];
        change[1] = 0;
        change[2] = x[3];
        change[3] = -m_Gravity;
        change[4] = x[5];
        change[5] = 0;


        double len = m_Spring.m_RestLength;
        double nx = 0;
        double ny = 1;
        double rx = -len * Math.sin(x[4]);
        double ry = len * Math.cos(x[4]);
        double w = x[5];
        double vx = x[1];
        double vy = x[3];
        double A[][] = new double[1][2];
        double B[] = new double[1];
        double f[] = new double[1];


        double b = 0;
        b += nx * (change[1] - change[5] * ry - w * w * rx) + ny * (change[3] + change[5] * rx - w * w * ry);
        B[0] = b;

        double I = m * (len * len) / 12;


        double a = nx * (nx / m - ry * (rx * ny - ry * nx) / I) + ny * (ny / m + rx * (rx * ny - ry * nx) / I);
        A[0][0] = a;
        A[0][1] = -B[0];
        Utility.matrixSolve(A, f);


        change[1] += f[0] * nx / m;
        change[3] += f[0] * ny / m;


        change[5] += (rx * f[0] * ny - ry * f[0] * nx) / I;


        double py = change[3] + change[5] * rx - w * w * ry;
        System.out.println("py''= " + py);
    }


    public void evaluate(double[] x, double[] change) {
        while (!rxnForces.isEmpty()) {
            Drawable d = (Drawable) rxnForces.lastElement();
            cvs.removeElement(d);
            rxnForces.removeElement(d);
        }

        double m = mass;
        change[0] = x[1];
        change[1] = -damping * x[1];
        change[2] = x[3];
        change[3] = -m_Gravity - damping * x[3];
        change[4] = x[5];
        change[5] = 0;

        change[6] = x[7];

        double l2 = m_Spring2.m_RestLength;
        double dd = -(m_Gravity / l2) * Math.sin(x[6]);
        double mlsq = m * l2 * l2;
        dd += -(damping / mlsq) * x[7];
        change[7] = dd;


        double len = m_Spring.m_RestLength;


        double I = m * (radius * radius / 2.0);
        double n0x = 0;
        double n0y = -1;
        double n1x = -1;
        double n1y = 0;
        double rx = -len * Math.sin(x[4]);
        double ry = len * Math.cos(x[4]);
        double vx = x[1];
        double vy = x[3];
        double w = x[5];


        double A[][] = new double[2][3];
        double B[] = new double[2];
        double f[] = new double[2];
        double nx, ny, nix, niy, b;
        nx = n0x;
        ny = n0y;


        b = nx * (change[1] - change[5] * ry - w * w * rx) + ny * (change[3] + change[5] * rx - w * w * ry);
        B[0] = b;


        nx = n1x;
        ny = n1y;

        b = nx * (change[1] - change[5] * ry - w * w * rx) + ny * (change[3] + change[5] * rx - w * w * ry);
        B[1] = b;


        nx = n0x;
        ny = n0y;
        nix = n0x;
        niy = n0y;
        A[0][0] = nix * (nx / m - ry * (rx * ny - ry * nx) / I) + niy * (ny / m + rx * (rx * ny - ry * nx) / I);
        nx = n1x;
        ny = n1y;
        nix = n0x;
        niy = n0y;
        A[0][1] = nix * (nx / m - ry * (rx * ny - ry * nx) / I) + niy * (ny / m + rx * (rx * ny - ry * nx) / I);
        nx = n0x;
        ny = n0y;
        nix = n1x;
        niy = n1y;
        A[1][0] = nix * (nx / m - ry * (rx * ny - ry * nx) / I) + niy * (ny / m + rx * (rx * ny - ry * nx) / I);
        nx = n1x;
        ny = n1y;
        nix = n1x;
        niy = n1y;
        A[1][1] = nix * (nx / m - ry * (rx * ny - ry * nx) / I) + niy * (ny / m + rx * (rx * ny - ry * nx) / I);


        A[0][2] = -B[0];
        A[1][2] = -B[1];


        Utility.matrixSolve(A, f);


        nx = n0x;
        ny = n0y;


        double Fx, Fy;
        Fx = f[0] * nx;
        Fy = f[0] * ny;
        showForce(0, 0, f[0] * nx, f[0] * ny);
        change[1] += f[0] * nx / m;
        change[3] += f[0] * ny / m;


        change[5] += (rx * f[0] * ny - ry * f[0] * nx) / I;

        nx = n1x;
        ny = n1y;
        Fx += f[1] * nx;
        Fy += f[1] * ny;
        showForce(0, 0, f[1] * nx, f[1] * ny);
        change[1] += f[1] * nx / m;
        change[3] += f[1] * ny / m;
        change[5] += (rx * f[1] * ny - ry * f[1] * nx) / I;

        double px = change[1] - change[5] * ry - w * w * rx;
        double py = change[3] + change[5] * rx - w * w * ry;


        showForce(0, 0, Fx, Fy);
    }

    private void showForce(double x, double y, double fx, double fy) {

        CVector v = new CVector(x, y, fx, fy);
        cvs.addElement(v);
        rxnForces.addElement(v);
    }
}
