import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;


class MoveableDoublePendulumCanvas extends SimCanvas {

    public MoveableDoublePendulumCanvas(MouseDragHandler mdh) {
        super(mdh);
    }

    public void mousePressed(MouseEvent evt) {
        int scr_x = evt.getX();
        int scr_y = evt.getY();

        double sim_x = map.screenToSimX(scr_x);
        double sim_y = map.screenToSimY(scr_y);
        dragObj = findNearestDragable(sim_x, sim_y);
        if (dragObj != null) {
            if (mdh != null) {
                mdh.startDrag(dragObj);
                mdh.constrainedSet(dragObj, sim_x, sim_y);
            }
        }
    }

    public void mouseDragged(MouseEvent evt) {
        if (dragObj != null) {
            double sim_x = map.screenToSimX(evt.getX());
            double sim_y = map.screenToSimY(evt.getY());

            if (mdh != null) mdh.constrainedSet(dragObj, sim_x, sim_y);
        }
    }


    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void mouseEntered(MouseEvent evt) {

        requestFocus();
    }

    protected void drawElements(Graphics g, ConvertMap map) {
        super.drawElements(g, map);
        ((MoveableDoublePendulum) mdh).drawRubberBand(g, map);
    }
}


public class MoveableDoublePendulum extends Simulation implements ActionListener {
    private static final String MASS1 = "mass1",
            MASS2 = "mass2", LENGTH1 = "stick1 length", LENGTH2 = "stick2 length",
            GRAVITY = "gravity", DAMPING1 = "damping1", DAMPING2 = "damping2",
            STIFFNESS = "mouse spring stiffness", ANCHOR_DAMPING = "anchor damping";


    private String[] params = {MASS1, MASS2, LENGTH1, LENGTH2, GRAVITY,
            DAMPING1, DAMPING2, STIFFNESS, ANCHOR_DAMPING};
    private CMass m_Mass1, m_Mass2, topMass;
    private CSpring m_Stick1, m_Stick2;
    private double gravity = 9.8;
    private JButton button_stop;
    private boolean mouseDown = false;
    private double mouseX = 0;
    private double mouseY = 0;
    private double damping1 = 0.5;
    private double damping2 = 0.5;
    private double stiffness = 3;
    private double anchorDamping = 0.8;

    public MoveableDoublePendulum(Container container) {
        super(container, 9);
        var_names = new String[]{
                "angle1",
                "angle1 velocity",
                "angle2",
                "angle2 velocity",
                "time",
                "anchorX",
                "anchorX velocity",
                "anchorY",
                "anchorY velocity"
        };

        setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -4, 4, -2.2, 1.5,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

        double xx = 0, yy = 0, w = 0.5;
        topMass = new CMass(xx - w / 2, yy - w / 2, w, w, CElement.MODE_RECT);
        topMass.m_Color = Color.red;
        cvs.addElement(topMass);


        m_Stick1 = new CSpring(0, 0, 1, 0.4);
        m_Stick1.m_DrawMode = CElement.MODE_LINE;
        cvs.addElement(m_Stick1);

        m_Stick2 = new CSpring(0, 0, 1, 0.4);
        m_Stick2.m_DrawMode = CElement.MODE_LINE;
        cvs.addElement(m_Stick2);

        w = 0.2;

        m_Mass1 = new CMass(0, 0, w, w, CElement.MODE_CIRCLE_FILLED);
        m_Mass1.m_Mass = 0.5;
        m_Mass1.m_Color = Color.blue;
        cvs.addElement(m_Mass1);


        m_Mass2 = new CMass(0, 0, w, w, CElement.MODE_CIRCLE_FILLED);
        m_Mass2.m_Mass = 0.5;
        m_Mass2.m_Damping = 0;
        m_Mass2.m_Color = Color.blue;
        cvs.addElement(m_Mass2);


        vars[0] = vars[1] = vars[2] = vars[3] = 0;
        vars[4] = 0;
        vars[5] = 0;
        vars[6] = 0;
        vars[7] = 0;
        vars[8] = 0;
        modifyObjects();
    }

    protected SimCanvas makeSimCanvas() {
        return new MoveableDoublePendulumCanvas(this);
    }

    public void setupControls() {
        super.setupControls();

        for (int i = 0; i < params.length; i++)
            addObserverControl(new DoubleField(this, params[i], 2));
        showControls(true);
        addControl(button_stop = new JButton("reset"));
        button_stop.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button_stop) {
            vars[0] = vars[1] = vars[2] = vars[3] = 0;
            vars[4] = 0;
            vars[5] = 0;
            vars[6] = 0;
            vars[7] = 0;
            vars[8] = 0;
        }
    }

    public void setupGraph() {
        super.setupGraph();
        if (graph != null) {
            graph.setVars(0, 2);
            graph.setDrawMode(Graph.DOTS);
        }
    }

    protected void setValue(int param, double value) {
        switch (param) {
            case 0:
                m_Mass1.m_Mass = value;
                break;
            case 1:
                m_Mass2.m_Mass = value;
                break;
            case 2:
                m_Stick1.m_RestLength = value;
                break;
            case 3:
                m_Stick2.m_RestLength = value;
                break;
            case 4:
                gravity = value;
                break;
            case 5:
                damping1 = value;
                break;
            case 6:
                damping2 = value;
                break;
            case 7:
                stiffness = value;
                break;
            case 8:
                anchorDamping = value;
                break;
        }
    }


    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(MASS1)) {
            m_Mass1.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(MASS2)) {
            m_Mass2.m_Mass = value;
            return true;
        } else if (name.equalsIgnoreCase(LENGTH1)) {
            m_Stick1.m_RestLength = value;
            return true;
        } else if (name.equalsIgnoreCase(LENGTH2)) {
            m_Stick2.m_RestLength = value;
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            gravity = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING1)) {
            damping1 = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING2)) {
            damping2 = value;
            return true;
        } else if (name.equalsIgnoreCase(STIFFNESS)) {
            stiffness = value;
            return true;
        } else if (name.equalsIgnoreCase(ANCHOR_DAMPING)) {
            anchorDamping = value;
            return true;
        }
        return super.trySetParameter(name, value);
    }


    public double getParameter(String name) {
        if (name.equalsIgnoreCase(MASS1))
            return m_Mass1.m_Mass;
        else if (name.equalsIgnoreCase(MASS2))
            return m_Mass2.m_Mass;
        else if (name.equalsIgnoreCase(LENGTH1))
            return m_Stick1.m_RestLength;
        else if (name.equalsIgnoreCase(LENGTH2))
            return m_Stick2.m_RestLength;
        else if (name.equalsIgnoreCase(GRAVITY))
            return gravity;
        else if (name.equalsIgnoreCase(DAMPING1))
            return damping1;
        else if (name.equalsIgnoreCase(DAMPING2))
            return damping2;
        else if (name.equalsIgnoreCase(STIFFNESS))
            return stiffness;
        else if (name.equalsIgnoreCase(ANCHOR_DAMPING))
            return anchorDamping;
        return super.getParameter(name);
    }

    public void modifyObjects() {
        if (vars[0] > Math.PI)
            vars[0] = vars[0] - 2 * Math.PI * Math.floor(vars[0] / Math.PI);
        else if (vars[0] < -Math.PI)
            vars[0] = vars[0] - 2 * Math.PI * Math.ceil(vars[0] / Math.PI);

        if (vars[2] > Math.PI)
            vars[2] = vars[2] - 2 * Math.PI * Math.floor(vars[2] / Math.PI);
        else if (vars[2] < -Math.PI)
            vars[2] = vars[2] - 2 * Math.PI * Math.ceil(vars[2] / Math.PI);

        topMass.setPosition(vars[5] - topMass.m_Width / 2, vars[7] - topMass.m_Height / 2);
        double x0 = topMass.m_X1 + topMass.m_Width / 2;
        double y0 = topMass.m_Y1 + topMass.m_Height / 2;

        double w = m_Mass1.m_Width / 2;
        double L1 = m_Stick1.m_RestLength;
        double L2 = m_Stick2.m_RestLength;
        double th1 = vars[0];
        double th2 = vars[2];
        double x1 = x0 + L1 * Math.sin(th1);
        double y1 = y0 - L1 * Math.cos(th1);
        double x2 = x1 + L2 * Math.sin(th2);
        double y2 = y1 - L2 * Math.cos(th2);
        m_Stick1.setBounds(x0, y0, x1, y1);
        m_Mass1.setPosition(x1 - w, y1 - w);
        m_Stick2.setBounds(x1, y1, x2, y2);
        m_Mass2.setPosition(x2 - w, y2 - w);
    }

    public void startDrag(Dragable e) {
        if (e == topMass)
            this.mouseDown = true;

        if ((e == m_Mass1) || (e == m_Mass2)) {
            calc[0] = calc[1] = calc[2] = calc[3] = false;
        }
    }

    public void finishDrag(Dragable e) {
        super.finishDrag(e);
        if (e == topMass)
            this.mouseDown = false;
    }


    public void constrainedSet(Dragable e, double x, double y) {

        double x0 = vars[5];
        double y0 = vars[7];
        double w = m_Mass1.m_Width / 2;

        if (e == topMass) {
            this.mouseX = x;
            this.mouseY = y;
        } else if (e == m_Mass1) {
            double xx = (x - x0) + w;
            double yy = (y - y0) + w;
            double th1 = Math.atan2(xx, -yy);
            vars[0] = th1;
            vars[1] = 0;
            vars[3] = 0;
            modifyObjects();
        } else if (e == m_Mass2) {
            double L1 = m_Stick1.m_RestLength;
            double x1 = x0 + L1 * Math.sin(vars[0]);
            double y1 = y0 - L1 * Math.cos(vars[0]);

            double x2 = x + w;
            double y2 = y + w;
            double th2 = Math.atan2(x2 - x1, -(y2 - y1));
            vars[1] = 0;
            vars[2] = th2;
            vars[3] = 0;
            modifyObjects();
        }
    }


    public void evaluate(double[] x, double[] change) {
        change[4] = 1;
        change[5] = x[6];
        change[6] = -this.anchorDamping * x[6] + (this.mouseDown ? this.stiffness * (this.mouseX - x[5]) : 0);
        change[7] = x[8];
        change[8] = -this.anchorDamping * x[8] + (this.mouseDown ? this.stiffness * (this.mouseY - x[7]) : 0);
        double ddx0 = change[6];
        double ddy0 = change[8];
        double th1 = x[0];
        double dth1 = x[1];
        double th2 = x[2];
        double dth2 = x[3];
        double m2 = m_Mass2.m_Mass;
        double m1 = m_Mass1.m_Mass;
        double L1 = m_Stick1.m_RestLength;
        double L2 = m_Stick2.m_RestLength;
        double g = gravity;
        double b = damping1;
        double b2 = damping2;

        change[0] = dth1;

        change[1] = -((2 * b * dth1 +
                ddx0 * L1 * (2 * m1 + m2) * Math.cos(th1) -
                ddx0 * L1 * m2 * Math.cos(th1 - 2 * th2) +
                2 * ddy0 * L1 * m1 * Math.sin(th1) + 2 * g * L1 * m1 * Math.sin(th1) +
                ddy0 * L1 * m2 * Math.sin(th1) + g * L1 * m2 * Math.sin(th1) +
                ddy0 * L1 * m2 * Math.sin(th1 - 2 * th2) +
                g * L1 * m2 * Math.sin(th1 - 2 * th2) +
                2 * dth2 * dth2 * L1 * L2 * m2 * Math.sin(th1 - th2) +
                dth1 * dth1 * L1 * L1 * m2 * Math.sin(2 * (th1 - th2))) /
                (L1 * L1 * (2 * m1 + m2 - m2 * Math.cos(2 * (th1 - th2)))));

        change[2] = dth2;

        change[3] = -((2 * b * dth1 * L2 * m2 * Math.cos(th1 - th2) -
                b2 * (dth1 - dth2) * L1 * m2 * Math.cos(2 * (th1 - th2)) +
                L1 * (2 * b2 * dth1 * m1 - 2 * b2 * dth2 * m1 + b2 * dth1 * m2 -
                        b2 * dth2 * m2 +
                        ddx0 * L2 * m2 * (m1 + m2) * Math.cos(2 * th1 - th2) -
                        ddx0 * L2 * m2 * (m1 + m2) * Math.cos(th2) +
                        2 * dth1 * dth1 * L1 * L2 * m1 * m2 * Math.sin(th1 - th2) +
                        2 * dth1 * dth1 * L1 * L2 * m2 * m2 * Math.sin(th1 - th2) +
                        dth2 * dth2 * L2 * L2 * m2 * m2 * Math.sin(2 * (th1 - th2)) +
                        ddy0 * L2 * m1 * m2 * Math.sin(2 * th1 - th2) +
                        g * L2 * m1 * m2 * Math.sin(2 * th1 - th2) +
                        ddy0 * L2 * m2 * m2 * Math.sin(2 * th1 - th2) +
                        g * L2 * m2 * m2 * Math.sin(2 * th1 - th2) -
                        ddy0 * L2 * m1 * m2 * Math.sin(th2) -
                        g * L2 * m1 * m2 * Math.sin(th2) -
                        ddy0 * L2 * m2 * m2 * Math.sin(th2) - g * L2 * m2 * m2 * Math.sin(th2))
        ) / (L1 * L2 * L2 * m2 * (-2 * m1 - m2 + m2 * Math.cos(2 * (th1 - th2)))));
    }

    public void drawRubberBand(Graphics g, ConvertMap map) {
        if (this.mouseDown) {
            g.setColor(Color.red);
            g.drawLine(map.simToScreenX(this.mouseX), map.simToScreenY(this.mouseY),
                    map.simToScreenX(topMass.getCenterX()), map.simToScreenY(topMass.getCenterY()));
        }
    }

}
