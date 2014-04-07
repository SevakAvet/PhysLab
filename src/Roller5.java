import java.awt.*;
import java.util.Vector;


public class Roller5 extends Simulation implements ObjectListener {
    protected static final String MASS = "mass", DAMPING = "damping",
            GRAVITY = "gravity", PATH = "path",
            SHOW_ENERGY = "show energy";
    private String[] params = {MASS, DAMPING, GRAVITY, PATH, SHOW_ENERGY};
    protected CMass m_Mass1;
    protected CBitmap m_TrackBM = null;
    protected double gravity = 2.0;
    protected CPath m_Path = null;
    protected CPoint m_Point = new CPoint();
    protected CText m_Text = null;
    protected int m_Path_Num = 0;
    protected boolean showEnergy = false;
    protected MyChoice pathControl;

    public Roller5(Container app, int the_path) {
        super(app, 3);
        setCoordMap(new CoordMap(CoordMap.INCREASE_UP, 0, 1,
                0, 1, CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
        var_names = new String[]{
                "x-position",
                "x-velocity",
                "position",
                "velocity"
        };
        createElements();
        set_path(the_path);
        modifyObjects();
        cvs.setObjectListener(this);
    }

    public void setupControls() {
        super.setupControls();


        addObserverControl(pathControl =
                new MyChoice(this, PATH, m_Path_Num, 0, PathName.getPathNames()));

        addObserverControl(new DoubleField(this, MASS, 2));
        addObserverControl(new DoubleField(this, DAMPING, 2));
        addObserverControl(new DoubleField(this, GRAVITY, 2));
        addObserverControl(new MyCheckbox(this, SHOW_ENERGY));
        showControls(true);
    }

    public void setupGraph() {
        super.setupGraph();
        if (graph != null) graph.setVars(2, 3);
    }

    protected void createElements() {
        m_Text = new CText(0, 0, "energy ");
        if (showEnergy)
            cvs.addElement(m_Text);

        m_Mass1 = new CMass(1, 1, 0.3, 0.3, CElement.MODE_CIRCLE_FILLED);
        m_Mass1.m_Mass = 0.5;
        m_Mass1.m_Damping = 0;
        cvs.addElement(m_Mass1);
    }


    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(MASS)) {
            m_Mass1.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING)) {
            m_Mass1.m_Damping = value;
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            gravity = value;
            return true;
        } else if (name.equalsIgnoreCase(PATH)) {
            set_path((int) value);
            modifyObjects();
            return true;
        } else if (name.equalsIgnoreCase(SHOW_ENERGY)) {
            boolean wantEnergy = value != 0;
            if (wantEnergy && !showEnergy)
                cvs.addElement(m_Text);
            else if (!wantEnergy && showEnergy)
                cvs.removeElement(m_Text);
            showEnergy = wantEnergy;
            return true;
        }
        return super.trySetParameter(name, value);
    }


    public double getParameter(String name) {
        if (name.equalsIgnoreCase(MASS))
            return m_Mass1.m_Mass;
        else if (name.equalsIgnoreCase(DAMPING))
            return m_Mass1.m_Damping;
        else if (name.equalsIgnoreCase(GRAVITY))
            return gravity;
        else if (name.equalsIgnoreCase(PATH))
            return m_Path_Num;
        else if (name.equalsIgnoreCase(SHOW_ENERGY))
            return showEnergy ? 1.0 : 0.0;
        return super.getParameter(name);
    }


    public String[] getParameterNames() {
        return params;
    }

    public int numVariables() {
        return var_names.length;
    }

    public double getVariable(int i) {
        if (i <= 2)
            return vars[i];
        if (i == 3) {

            double x = vars[0];
            double d = -(7.0 / 3.0) * x + (2.0 / 3.0) * x * x * x;
            return Math.sqrt(1.0 + d * d) * vars[1];
        }
        return 0;
    }

    protected void resetTrackBitmap() {
        if (m_TrackBM != null)
            cvs.removeElement(m_TrackBM);
        m_TrackBM = new CBitmap(container, m_Path);


        Rectangle r = cvs.getConvertMap().getScreenRect();
        m_TrackBM.setGraphicsTopLeft(r.x, r.y);
        cvs.prependElement(m_TrackBM);
    }

    protected void set_path(int the_path) {
        System.out.println("path = " + the_path);
        PathName[] pNames = PathName.getPathNames();
        if (the_path >= 0 && the_path < pNames.length) {
            m_Path_Num = the_path;
            m_Path = CPath.makePath(pNames[the_path]);
            System.out.println("m_Path = " + m_Path);
            cvs.getCoordMap().setRange(m_Path.left, m_Path.right, m_Path.bottom, m_Path.top);
            resetTrackBitmap();
            if (graph != null)
                graph.reset();
            DoubleRect r = cvs.getConvertMap().getSimBounds();
            m_Text.setX1(r.getXMin() + r.getWidth() * 0.1);
            m_Text.setY1(r.getYMax() - r.getHeight() * 0.1);


            vars[0] = 3;
            vars[1] = 0;
        } else
            throw new IllegalArgumentException("no such path number " + the_path);
    }


    public void objectChanged(Object o) {
        if (cvs == o)
            resetTrackBitmap();
    }

    public void modifyObjects() {

        double x = vars[0];
        double y = 3.0 - x * x * 7.0 / 6.0 + x * x * x * x / 6.0;
        m_Mass1.setCenterX(x);
        m_Mass1.setCenterY(y);

    }

    protected double getEnergy() {


        double e = 0.5 * m_Mass1.m_Mass * vars[1] * vars[1];

        e += m_Mass1.m_Mass * gravity * m_Mass1.getCenterY();
        return e;
    }

    public void startDrag(Dragable e) {
        if (e == m_Mass1) {
            calc[0] = false;
            calc[1] = false;
        }
    }

    public void constrainedSet(Dragable e, double x, double y) {
        if (e == m_Mass1) {


            double w = m_Mass1.m_Width / 2;
            vars[0] = m_Path.map_x_y_to_p(x + w, y + w);
            vars[1] = 0;
            modifyObjects();
        }
    }


    public void evaluate(double[] x, double[] change) {
        double x2 = x[0] * x[0];
        change[0] = x[1];
        double r = -(x[0] * (-7.0 + 2.0 * x2) * (3.0 * gravity + (-7.0 + 6.0 * x2) * x[1] * x[1]));
        change[1] = r / (9.0 + 49.0 * x2 - 28.0 * x2 * x2 + 4.0 * x2 * x2 * x2);


        double d = -(7.0 / 3.0) * x[0] + (2.0 / 3.0) * x[0] * x[0] * x[0];
        change[2] = Math.sqrt(1 + d * d) * x[1];
    }

    public Vector findAllCollisions() {
        return null;
    }

    public void handleCollisions(Vector collisions) {
    }

}
