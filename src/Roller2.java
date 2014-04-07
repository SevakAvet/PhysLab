import java.awt.*;


public class Roller2 extends Roller1 {
    protected static final String STIFF = "spring stiffness",
            LENGTH = "spring rest length";
    protected CMass m_TopMass;
    protected CSpring m_Spring;

    public Roller2(Container app, int the_path) {
        super(app, the_path);
    }

    public void setupControls() {
        super.setupControls();
        addObserverControl(new DoubleField(this, LENGTH, 2));
        addObserverControl(new DoubleField(this, STIFF, 2));
    }

    protected void createElements() {
        super.createElements();
        m_Spring = new CSpring(1, 1, 1, 0.5);
        m_Spring.m_SpringConst = 5;
        cvs.addElement(m_Spring);

        m_TopMass = new CMass(0, 0, 0.5, 0.5, CElement.MODE_RECT);
        cvs.addElement(m_TopMass);
    }


    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(LENGTH)) {
            m_Spring.m_RestLength = value;
            return true;
        } else if (name.equalsIgnoreCase(STIFF)) {
            m_Spring.m_SpringConst = value;
            return true;
        }
        return super.trySetParameter(name, value);
    }


    public double getParameter(String name) {
        if (name.equalsIgnoreCase(LENGTH))
            return m_Spring.m_RestLength;
        else if (name.equalsIgnoreCase(STIFF))
            return m_Spring.m_SpringConst;
        return super.getParameter(name);
    }


    public String[] getParameterNames() {
        String[] params = {MASS, DAMPING, GRAVITY, PATH, SHOW_ENERGY, STIFF, LENGTH};
        return params;
    }

    protected void set_path(int the_path) {
        super.set_path(the_path);

        double xx, yy;
        if (m_Path.closed) {
            xx = m_Path.left + 0.05 * (m_Path.right - m_Path.left);
            yy = m_Path.bottom + 0.1 * (m_Path.top - m_Path.bottom);
        } else {
            xx = m_Path.left + 0.3 * (m_Path.right - m_Path.left);
            yy = m_Path.bottom + 0.5 * (m_Path.top - m_Path.bottom);
        }
        m_Spring.setX1(xx);
        m_Spring.setY1(yy);
        m_TopMass.setCenterX(xx);
        m_TopMass.setCenterY(yy);
    }

    public void modifyObjects() {
        vars[0] = m_Path.modp(vars[0]);
        m_Point.p = vars[0];
        m_Path.map_p_to_slope(m_Point);
        m_Mass1.setCenterX(m_Point.x);
        m_Mass1.setCenterY(m_Point.y);
        m_Spring.setX2(m_Point.x);
        m_Spring.setY2(m_Point.y);


        m_Text.setNumber(getEnergy());
    }

    protected double getEnergy() {

        double e = super.getEnergy();

        e += m_Spring.getEnergy();
        return e;
    }

    public void constrainedSet(Dragable e, double x, double y) {
        if (e == m_TopMass) {

            double w = m_TopMass.m_Width / 2;
            x += w;
            y += w;

            DoubleRect r = cvs.getSimBounds();
            double L = r.getXMin() + w;
            double R = r.getXMax() - w;
            double B = r.getYMin() + w;
            double T = r.getYMax() - w;


            if (x < L) x = L + 0.0001;
            if (x > R) x = R - 0.0001;
            if (y < B) y = B + 0.0001;
            if (y > T) y = T - 0.0001;

            m_TopMass.setCenterX(x);
            m_TopMass.setCenterY(y);

            m_Spring.setX1(x);
            m_Spring.setY1(y);
        } else if (e == m_Mass1) {


            double w = m_Mass1.m_Width / 2;
            vars[0] = m_Path.map_x_y_to_p(x + w, y + w);
            vars[1] = 0;
            modifyObjects();
        }
    }

    public void evaluate(double[] x, double[] change) {
        change[0] = x[1];


        m_Point.p = x[0];
        m_Path.map_p_to_slope(m_Point);
        double k = m_Point.slope;


        double sinTheta = Double.isInfinite(k) ? 1 : k / Math.sqrt(1 + k * k);
        change[1] = -gravity * m_Point.direction * sinTheta;

        change[1] -= m_Mass1.m_Damping * x[1] / m_Mass1.m_Mass;


        double sx = m_Spring.m_X1 - m_Point.x;
        double sy = m_Spring.m_Y1 - m_Point.y;
        double slen = Math.sqrt(sx * sx + sy * sy);

        double cosTheta;
        if (Double.isInfinite(k))
            cosTheta = m_Point.direction * sy / slen;
        else
            cosTheta = m_Point.direction * (sx + k * sy) / (slen * Math.sqrt(1 + k * k));
        if (cosTheta > 1 || cosTheta < -1)
            System.out.println("cosTheta out of range in diffeq1");

        double stretch = slen - m_Spring.m_RestLength;

        change[1] += m_Spring.m_SpringConst * cosTheta * stretch / m_Mass1.m_Mass;
    }
}
