import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;


class ThrusterCanvas extends SimCanvas {

    public ThrusterCanvas(MouseDragHandler mdh) {
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

        int keyCode = e.getKeyCode();
        ((Thruster5) mdh).handleKeyEvent(keyCode, true);
    }

    public void keyReleased(KeyEvent e) {

        int keyCode = e.getKeyCode();
        ((Thruster5) mdh).handleKeyEvent(keyCode, false);
    }

    public void mouseEntered(MouseEvent evt) {

        requestFocus();
    }

    protected synchronized void drawElements(Graphics g, ConvertMap map) {
        super.drawElements(g, map);
        ((Thruster5) mdh).drawRubberBand(g, map);
    }
}


public class Thruster5 extends CollidingSim implements ActionListener, ObjectListener {
    public static final double DISTANCE_TOL = 0.01;
    public static final double VELOCITY_TOL = 0.5;
    public static final int RIGHT_WALL = -1, BOTTOM_WALL = -2, LEFT_WALL = -3, TOP_WALL = -4;
    protected static final int MAX_BODIES = 6;
    protected static final String NUM_BODIES = "number bodies",
            DAMPING = "damping", GRAVITY = "gravity", ELASTICITY = "elasticity",
            THRUST = "thrust",
            SHOW_ENERGY = "show energy";


    protected String[] params = {NUM_BODIES, DAMPING, GRAVITY, ELASTICITY, THRUST,
            SHOW_ENERGY};
    protected int dragObj = -1;
    protected double mouseX, mouseY;
    protected int numBods;
    protected Thruster5Object[] bods;
    protected Vector collisionsFound = new Vector(10);
    protected NumberFormat nf = NumberFormat.getNumberInstance();
    protected double gravity = 0, damping, elasticity = 1.0, thrust = 0.5;
    protected double m_Left, m_Right, m_Bottom, m_Top;
    protected CRect m_Walls;
    protected boolean debug = false;
    protected boolean doCollisions = true;
    protected boolean showCollisionDot = false;
    protected Vector rxnForces = new Vector(20);
    protected boolean showEnergy = false;
    protected BarChart energyBar;
    protected double zeroEnergyLevel = 0;
    protected int stuckCounter = 0;
    protected boolean gameMode = false;
    protected JButton buttonReset;
    private CText preText, postText;
    private CText message = null;
    private int winningHits = 10;
    private int greenHits = 0, blueHits = 0;
    private CText greenLabel, blueLabel;
    private CText message2 = null;


    public Thruster5(Container container, boolean gameMode) {
        super(container);
        this.gameMode = gameMode;
        double w = 5;
        setCoordMap(new CoordMap(CoordMap.INCREASE_UP, -w, w, -w, w,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));


        DoubleRect box = cvs.getSimBounds();
        m_Left = box.getXMin();
        m_Right = box.getXMax();
        m_Bottom = box.getYMin();

        zeroEnergyLevel = m_Bottom;
        m_Top = box.getYMax();
        cvs.setObjectListener(this);

        damping = gameMode ? 0.2 : 0;
        numBods = gameMode ? 2 : 3;

        reset();


        Collision c = new Collision();


        if (gameMode) cvs.requestFocus();
    }

    public void setupControls() {
        super.setupControls();
        addControl(buttonReset = new JButton("reset"));
        buttonReset.addActionListener(this);
        if (!gameMode) {

            nf.setMinimumFractionDigits(0);
            String[] choices = new String[MAX_BODIES];
            for (int i = 0; i < MAX_BODIES; i++)
                choices[i] = nf.format(i + 1) + " object" + (i > 0 ? "s" : "");

            addObserverControl(new MyChoice(this, NUM_BODIES, numBods, 1, choices));
        }

        addObserverControl(new DoubleField(this, ELASTICITY, 2));
        addObserverControl(new DoubleField(this, GRAVITY, 2));
        addObserverControl(new DoubleField(this, DAMPING, 2));
        addObserverControl(new DoubleField(this, THRUST, 2));
        if (!gameMode)
            addObserverControl(new MyCheckbox(this, SHOW_ENERGY));
        showControls(true);

    }

    public void setupGraph() {

    }

    public String getVariableName(int i) {

        int j = i % 6;

        int obj = i / 6;
        switch (j) {
            case 0:
                return "x position " + obj;
            case 1:
                return "x velocity " + obj;
            case 2:
                return "y position " + obj;
            case 3:
                return "y velocity " + obj;
            case 4:
                return "angle " + obj;
            case 5:
                return "angular velocity " + obj;
            default:
                return "";
        }
    }

    public void objectChanged(Object o) {
        if (o == cvs) {
            DoubleRect box = cvs.getSimBounds();
            m_Left = box.getXMin();
            m_Right = box.getXMax();
            zeroEnergyLevel = m_Bottom = box.getYMin();

            m_Top = box.getYMax();
            m_Walls.setBounds(new DoubleRect(m_Left, m_Bottom, m_Right, m_Top));
        }
    }

    public void startDrag(Dragable e) {
        dragObj = -1;
        for (int i = 0; i < bods.length; i++)
            if (e == bods[i])
                dragObj = i;
    }

    public void constrainedSet(Dragable e, double x, double y) {
        mouseX = x;
        mouseY = y;
    }

    public void finishDrag(Dragable e) {

        super.finishDrag(e);
        dragObj = -1;
    }

    public void drawRubberBand(Graphics g, ConvertMap map) {

        if (dragObj >= 0) {
            g.setColor(Color.black);
            g.drawLine(map.simToScreenX(mouseX), map.simToScreenY(mouseY),
                    map.simToScreenX(bods[dragObj].tx), map.simToScreenY(bods[dragObj].ty));
        }
    }


    protected boolean trySetParameter(String name, double value) {
        if (name.equalsIgnoreCase(ELASTICITY)) {
            elasticity = value;
            return true;
        } else if (name.equalsIgnoreCase(DAMPING)) {
            damping = value;
            return true;
        } else if (name.equalsIgnoreCase(GRAVITY)) {
            gravity = value;
            return true;
        } else if (name.equalsIgnoreCase(THRUST)) {
            thrust = value;
            for (int i = 0; i < numBods; i++) {
                bods[i].tMagnitude = thrust;
            }
            return true;
        } else if (name.equalsIgnoreCase(NUM_BODIES)) {
            numBods = (int) value;


            m_Animating = false;
            reset();
            m_Animating = true;

            return true;
        } else if (name.equalsIgnoreCase(SHOW_ENERGY)) {
            showEnergy = value != 0;
            boolean chartVisible = cvs.containsElement(energyBar);
            if (showEnergy && !chartVisible) {
                cvs.prependElement(energyBar);
                if (showMomentum()) {
                    cvs.prependElement(preText);
                    cvs.prependElement(postText);
                }
            } else if (!showEnergy && chartVisible) {
                cvs.removeElement(energyBar);
                if (showMomentum()) {
                    cvs.removeElement(preText);
                    cvs.removeElement(postText);
                }
            }


            container.invalidate();
            container.validate();
            return true;
        }
        return super.trySetParameter(name, value);
    }


    public double getParameter(String name) {
        if (name.equalsIgnoreCase(ELASTICITY))
            return elasticity;
        else if (name.equalsIgnoreCase(DAMPING))
            return damping;
        else if (name.equalsIgnoreCase(GRAVITY))
            return gravity;
        else if (name.equalsIgnoreCase(THRUST))
            return thrust;
        else if (name.equalsIgnoreCase(NUM_BODIES))
            return numBods;
        else if (name.equalsIgnoreCase(SHOW_ENERGY))
            return showEnergy ? 1 : 0;
        return super.getParameter(name);
    }


    public String[] getParameterNames() {
        return params;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == buttonReset) {
            reset();
        }
    }


    protected SimCanvas makeSimCanvas() {
        return new ThrusterCanvas(this);
    }


    protected Thruster5Object createBlock(double width, double height) {
        return new Thruster5Object(width, height);
    }

    protected synchronized void reset() {
        cvs.removeAllElements();
        message2 = null;
        m_Walls = new CRect(new DoubleRect(m_Left, m_Bottom, m_Right, m_Top));
        cvs.addElement(m_Walls);
        if (!gameMode) {
            DoubleRect r = cvs.getSimBounds();
            energyBar = new BarChart(r);
            preText = new CText(r.getXMin() + 0.05 * r.getWidth(),
                    r.getYMin() + 0.75 * r.getHeight(), "");
            preText.setFontSize(12);
            postText = new CText(r.getXMin() + 0.05 * r.getWidth(),
                    r.getYMin() + 0.70 * r.getHeight(), "");
            postText.setFontSize(12);
            if (showEnergy) {
                cvs.addElement(energyBar);
                if (showMomentum()) {
                    cvs.addElement(preText);
                    cvs.addElement(postText);
                }
            }
        } else {
            DoubleRect r = cvs.getSimBounds();
            greenLabel = new CText(r.getXMin() + 0.05 * r.getWidth(),
                    r.getYMin() + 0.75 * r.getHeight(), "");
            greenLabel.setFontSize(16);
            greenLabel.m_Color = Color.green;
            blueLabel = new CText(r.getXMin() + 0.05 * r.getWidth(),
                    r.getYMin() + 0.60 * r.getHeight(), "");
            blueLabel.setFontSize(16);
            blueLabel.m_Color = Color.blue;
            cvs.addElement(greenLabel);
            cvs.addElement(blueLabel);
        }
        bods = new Thruster5Object[numBods];
        for (int i = 0; i < numBods; i++) {
            bods[i] = createBlock(0.5, 3.0);
            bods[i].tMagnitude = thrust;
            cvs.addElement(bods[i]);
        }
        if (numBods > 0) {
            if (gameMode)
                bods[0].moveTo(2, 0, Math.PI / 4);
            else
                bods[0].moveTo(-2, 0, Math.PI / 2);
            bods[0].color = Color.green;
        }
        if (numBods > 1) {
            if (gameMode)
                bods[1].moveTo(-2, 0, -Math.PI / 4);
            else
                bods[1].moveTo(2, 1, 0);


            bods[1].color = Color.blue;
        }
        if (numBods > 2) {
            bods[2].moveTo(1, 0, 0.1);
            bods[2].color = Color.red;
        }
        if (numBods > 3) {
            bods[3].moveTo(-2.2, 1, 0.2 + Math.PI / 2);
            bods[3].color = Color.cyan;
        }
        if (numBods > 4) {
            bods[4].moveTo(-2.4, -1, -0.2 + Math.PI / 2);
            bods[4].color = Color.magenta;
        }
        if (numBods > 5) {
            bods[5].moveTo(-1.8, 2, 0.3 + Math.PI / 2);
            bods[5].color = Color.orange;
        }

        vars = new double[6 * numBods];
        calc = new boolean[vars.length];
        for (int i = 0; i < calc.length; i++)
            calc[i] = true;
        for (int i = 0; i < numBods; i++) {
            vars[6 * i] = bods[i].x;
            vars[6 * i + 2] = bods[i].y;
            vars[6 * i + 4] = bods[i].angle;

        }

        message = null;
        message2 = null;
        if (gameMode) {
            this.nf.setMinimumFractionDigits(0);
            greenLabel.setText("green " + nf.format(greenHits = 0));
            blueLabel.setText("blue " + nf.format(blueHits = 0));
        }
    }

    public void handleKeyEvent(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_J:
                bods[0].active[1] = pressed;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_L:
                bods[0].active[0] = pressed;
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_I:
                bods[0].active[3] = pressed;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_K:
                bods[0].active[2] = pressed;
                break;
            case KeyEvent.VK_S:
                bods[1].active[1] = pressed;
                break;
            case KeyEvent.VK_F:
                bods[1].active[0] = pressed;
                break;
            case KeyEvent.VK_E:
                bods[1].active[3] = pressed;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_C:
                bods[1].active[2] = pressed;
                break;
            default:
                break;
        }
    }

    public void modifyObjects() {
        modifyObjects(vars);
    }

    public void modifyObjects(double[] x) {
        for (int i = 0; i < numBods; i++)
            bods[i].moveTo(x[6 * i + 0], x[6 * i + 2], x[6 * i + 4]);
        if (energyBar != null) {
            energyBar.pe = energyBar.re = energyBar.te = 0;
            for (int i = 0; i < numBods; i++) {
                if (bods[i].mass == Double.POSITIVE_INFINITY)
                    continue;
                energyBar.pe += (x[2 + 6 * i] - zeroEnergyLevel - bods[i].getMinHeight()) * bods[i].mass * gravity;
                energyBar.re += bods[i].rotationalEnergy(x[5 + 6 * i]);
                energyBar.te += bods[i].translationalEnergy(x[1 + 6 * i], x[3 + 6 * i]);
            }
        }
    }

    protected double getEnergy() {
        double e = 0;
        for (int i = 0; i < numBods; i++) {
            if (bods[i].mass == Double.POSITIVE_INFINITY)
                continue;
            e += (vars[2 + 6 * i] - zeroEnergyLevel - bods[i].getMinHeight()) * bods[i].mass * gravity;
            e += bods[i].rotationalEnergy(vars[5 + 6 * i]);
            e += bods[i].translationalEnergy(vars[1 + 6 * i], vars[3 + 6 * i]);
        }
        return e;
    }

    protected boolean showMomentum() {
        return true;
    }

    public void printEnergy(int n, String s) {

        if (!showEnergy || !showMomentum()) {
            return;
        }
        double ke = 0, pe = 0;
        double[] m0 = new double[]{0, 0, 0};
        double[] m1 = new double[3];
        for (int i = 0; i < numBods; i++) {
            if (bods[i].mass == Double.POSITIVE_INFINITY)
                continue;

            pe += (vars[2 + 6 * i] - zeroEnergyLevel - bods[i].getMinHeight()) * bods[i].mass * gravity;
            ke += bods[i].kineticEnergy(vars[1 + 6 * i], vars[3 + 6 * i], vars[5 + 6 * i]);
            m1 = bods[i].momentum(vars[1 + 6 * i], vars[3 + 6 * i], vars[5 + 6 * i]);
            for (int j = 0; j < 3; j++)
                m0[j] += m1[j];
        }
        if (showMomentum()) {
            nf.setMaximumFractionDigits(3);
            nf.setMinimumFractionDigits(3);
            CText label = (n == 0) ? preText : postText;
            label.m_text = (s + " momentum x: " + nf.format(m0[0]) +
                    "   y: " + nf.format(m0[1]) +
                    "   angular: " + nf.format(m0[2]));
        }
    }


    protected void checkCollision(int obj, int corner) {
        Collision result = null;

        double cornerX, cornerY;
        switch (corner) {
            case 1:
                cornerX = bods[obj].ax;
                cornerY = bods[obj].ay;
                break;
            case 2:
                cornerX = bods[obj].bx;
                cornerY = bods[obj].by;
                break;
            case 3:
                cornerX = bods[obj].cx;
                cornerY = bods[obj].cy;
                break;
            case 4:
                cornerX = bods[obj].dx;
                cornerY = bods[obj].dy;
                break;
            default:
                throw new IllegalArgumentException("bad corner " + corner);
        }
        double d;


        if ((d = cornerX - m_Right) > 0) {
            result = new Collision();
            result.depth = d;
            result.normalX = -1;
            result.normalY = 0;
            result.normalObj = RIGHT_WALL;
        }
        if ((d = m_Left - cornerX) > 0 && (result == null || d > result.depth)) {
            result = new Collision();
            result.depth = d;
            result.normalX = 1;
            result.normalY = 0;
            result.normalObj = LEFT_WALL;
        }
        if ((d = cornerY - m_Top) > 0 && (result == null || d > result.depth)) {
            result = new Collision();
            result.depth = d;
            result.normalX = 0;
            result.normalY = -1;
            result.normalObj = TOP_WALL;
        }
        if ((d = m_Bottom - cornerY) > 0 && (result == null || d > result.depth)) {
            result = new Collision();
            result.depth = d;
            result.normalX = 0;
            result.normalY = 1;
            result.normalObj = BOTTOM_WALL;
        }

        for (int i = 0; i < numBods; i++)
            if (i != obj) {
                Collision c = bods[i].testCollision(cornerX, cornerY, obj, i);
                if (c != null) {
                    if (result == null || c.depth > result.depth)
                        result = c;
                }
            }

        if (result != null) {
            result.colliding = true;
            result.impactX = cornerX;
            result.impactY = cornerY;
            result.object = obj;
            result.corner = corner;
            result.depth = -result.depth;
            Collision.addCollision(collisionsFound, result);
        }
    }


    public Vector findAllCollisions() {
        collisionsFound.removeAllElements();
        if (doCollisions) {

            for (int i = 0; i < numBods; i++) {
                for (int j = 1; j <= 4; j++) {


                    checkCollision(i, j);
                }
            }
            if (debug && collisionsFound.size() > 0) {
                System.out.println("--------------------------------");
                if (collisionsFound.size() > 1) {
                    System.out.println(collisionsFound.size() + " collisions detected time= " + simTime);
                }
                if (collisionsFound.size() > 0) {
                    for (int i = 0; i < collisionsFound.size(); i++) {
                        DecimalFormat df = new DecimalFormat("0.0###");
                        Collision c = (Collision) collisionsFound.elementAt(i);
                        System.out.println("collision obj=" + df.format(c.object) +
                                " normalObj=" + df.format(c.normalObj) +
                                " impact x=" + df.format(c.impactX) + " y=" + df.format(c.impactY));
                        System.out.println("normal x=" + df.format(c.normalX)
                                + " y=" + df.format(c.normalY) + " depth= " + c.depth);
                    }
                }
            }
        }
        return (collisionsFound.size() > 0) ? collisionsFound : null;
    }

    protected void findNormal(Collision c1, Collision c2) {


        if (c1.impactX == c2.impactX) {

            c1.normalY = 0;
            c1.normalX = (vars[0 + 6 * c1.object] < c1.impactX) ? -1 : 1;
        } else {

            double slope = (c2.impactY - c1.impactY) / (c2.impactX - c1.impactX);
            double b = c1.impactY - slope * c1.impactX;


            int obj = 6 * c1.object;

            double nx = -slope;
            double ny = 1;
            if (vars[2 + obj] - slope * vars[0 + obj] - b < 0) {

                nx = -nx;
                ny = -ny;
            }

            double magnitude = Math.sqrt(nx * nx + ny * ny);
            c1.normalX = nx / magnitude;
            c1.normalY = ny / magnitude;
        }
        if (debug) System.out.println("findNormal " + c1.normalX + " " + c1.normalY);
    }

    protected void gameScore(Collision c) {
        if (gameMode && c.normalObj < 0) {
            this.nf.setMinimumFractionDigits(0);
            if (c.object == 0) {
                greenLabel.setText("green " + nf.format(++greenHits));
                if (greenHits >= winningHits && message2 == null)
                    cvs.addElement(message2
                            = new CText("Green hit wall " + winningHits + " times -- Blue wins!"));
            } else {
                blueLabel.setText("blue " + nf.format(++blueHits));
                if (blueHits >= winningHits && message2 == null)
                    cvs.addElement(message2
                            = new CText("Blue hit wall " + winningHits + " times -- Green wins!"));
            }
        }
    }


    protected void addImpact(Collision cd, double[] velo) {
        gameScore(cd);
        double nx = cd.normalX;
        double ny = cd.normalY;


        int objA, objB, offsetA, offsetB;
        double rax, ray, rbx, rby;
        double vax, vay, wa, vbx, vby, wb;
        double d, dx, dy, j;
        if (cd.normalObj < 0) {
            objB = cd.object;
            offsetB = 6 * objB;

            rbx = cd.impactX - bods[objB].x;
            rby = cd.impactY - bods[objB].y;
            double Ib = bods[objB].momentAboutCM();
            double mb = bods[objB].mass;
            vbx = vars[1 + offsetB];
            vby = vars[3 + offsetB];
            wb = vars[5 + offsetB];
            double e = elasticity;


            j = rbx * ny - rby * nx;
            j = (j * j) / Ib + (1 / mb);


            double normalVelocity = (vbx - rby * wb) * nx + (vby + rbx * wb) * ny;
            if (normalVelocity >= 0) {
                if (debug)
                    System.out.println("add Impact: positive relative velocity " + normalVelocity);
                return;
            }
            j = -(1 + e) * normalVelocity / j;

            velo[1 + offsetB] += nx * j / mb;
            velo[3 + offsetB] += ny * j / mb;

            velo[5 + offsetB] += j * (rbx * ny - rby * nx) / Ib;
            addCollisionDot(cd.impactX, cd.impactY, j, Color.blue);
        } else {


            objA = cd.object;
            objB = cd.normalObj;
            offsetA = 6 * objA;
            offsetB = 6 * objB;
            rax = cd.impactX - bods[objA].x;
            ray = cd.impactY - bods[objA].y;
            rbx = cd.impactX - bods[objB].x;
            rby = cd.impactY - bods[objB].y;

            double invIa, invIb, invma, invmb;
            invIa = bods[objA].invMomentAboutCM();
            invIb = bods[objB].invMomentAboutCM();
            invma = bods[objA].invMass();
            invmb = bods[objB].invMass();
            nx = -nx;
            ny = -ny;
            vax = vars[1 + offsetA];
            vay = vars[3 + offsetA];
            wa = vars[5 + offsetA];
            vbx = vars[1 + offsetB];
            vby = vars[3 + offsetB];
            wb = vars[5 + offsetB];


            d = rax * ny - ray * nx;
            j = d * d * invIa;
            d = -rby * nx + rbx * ny;
            j += d * d * invIb + invma + invmb;


            dx = vax + wa * (-ray) - vbx - wb * (-rby);
            dy = vay + wa * (rax) - vby - wb * (rbx);
            j = -(1 + elasticity) * (dx * nx + dy * ny) / j;

            velo[1 + offsetA] += j * nx * invma;
            velo[3 + offsetA] += j * ny * invma;
            velo[1 + offsetB] += -j * nx * invmb;
            velo[3 + offsetB] += -j * ny * invmb;

            velo[5 + offsetA] += j * (-ray * nx + rax * ny) * invIa;
            velo[5 + offsetB] += -j * (-rby * nx + rbx * ny) * invIb;
            if (debug) System.out.println("addImpact j= " + j + " normal= " + nx + " " + ny);
            addCollisionDot(cd.impactX, cd.impactY, j, Color.blue);
        }
    }


    protected Collision[] findMatch(Vector collisions) {
        int n = collisions.size();
        for (int i = 0; i < n; i++) {
            Collision c1 = (Collision) collisions.elementAt(i);
            if (c1.handled)
                continue;
            for (int j = i + 1; j < n; j++) {
                Collision c2 = (Collision) collisions.elementAt(j);
                if (c2.handled)
                    continue;
                if (c1.object == c2.object && c1.normalObj == c2.normalObj
                        || c1.object == c2.normalObj && c1.normalObj == c2.object) {

                    Collision[] result = {c1, c2};
                    return result;
                }
            }
        }
        return null;
    }


    protected void specialImpact(Vector collisions, double[] velo) {
        Collision[] match;
        while ((match = findMatch(collisions)) != null) {
            Collision c1 = match[0];
            Collision c2 = match[1];


            findNormal(c1, c2);

            c1.impactX = (c1.impactX + c2.impactX) / 2;
            c1.impactY = (c1.impactY + c2.impactY) / 2;
            addImpact(c1, velo);


            if (true || debug) System.out.println("special impact " + c1.object + " " + c1.normalObj);


            c2.handled = c1.handled = true;
        }
    }


    public void handleCollisions(Vector collisions) {

        printEnergy(0, "pre-collision ");
        double[] velo = new double[vars.length];
        for (int i = 0; i < vars.length; i++)
            velo[i] = 0;
        if (debug) System.out.println("handleCollisions " + collisions.size() +
                " collisions time=" + simTime);
        specialImpact(collisions, velo);
        if (debug) System.out.println("handleCollisions " + collisions.size() +
                " collisions after specialImpact");

        for (int i = 0; i < collisions.size(); i++) {
            Collision c = (Collision) collisions.elementAt(i);
            if (!c.handled) {
                addImpact((Collision) collisions.elementAt(i), velo);
                c.handled = true;
            }
        }
        for (int i = 0; i < vars.length; i++) {
            if (debug && velo[i] != 0)
                System.out.println("var " + i + " modified by " + velo[i]);
            vars[i] += velo[i];
        }

        printEnergy(1, "post-collision");
    }


    public void evaluate(double[] x, double[] change) {
        while (!rxnForces.isEmpty()) {
            Drawable d = (Drawable) rxnForces.lastElement();
            cvs.removeElement(d);
            rxnForces.removeElement(d);
        }
        for (int i = 0; i < vars.length; i++) {
            int j = i % 6;

            int obj = i / 6;
            int offset = 6 * obj;
            int k;
            double result = 0;
            double invMass = bods[obj].invMass();
            double invMoment = bods[obj].invMomentAboutCM();
            final double springConst = 1;
            switch (j) {
                case 0:
                    change[i] = x[1 + offset];
                    break;
                case 1:
                    result = -damping * x[1 + offset] * invMass;

                    for (k = 0; k < 4; k++) {
                        if (bods[obj].active[k]) {
                            double[] v = bods[obj].calcVectors(x[0 + offset], x[2 + offset], x[4 + offset], k);

                            result += v[4] * invMass;
                        }
                    }
                    if (obj == dragObj) {
                        double[] v = bods[obj].calcVectors(x[0 + offset], x[2 + offset], x[4 + offset], 0);

                        double Fx = springConst * (mouseX - (x[0 + offset] + v[0]));
                        result += Fx * invMass;
                    }
                    change[i] = result;
                    break;
                case 2:
                    change[i] = x[3 + offset];
                    break;
                case 3:
                    result = -damping * x[3 + offset] / bods[obj].mass;
                    if (invMass != 0)
                        result -= gravity;
                    for (k = 0; k < 4; k++) {
                        if (bods[obj].active[k]) {
                            double[] v = bods[obj].calcVectors(x[0 + offset], x[2 + offset], x[4 + offset], k);

                            result += v[5] * invMass;
                        }
                    }
                    if (obj == dragObj) {
                        double[] v = bods[obj].calcVectors(x[0 + offset], x[2 + offset], x[4 + offset], 0);

                        double Fy = springConst * (mouseY - (x[2 + offset] + v[1]));
                        result += Fy * invMass;
                    }
                    change[i] = result;
                    break;
                case 4:
                    change[i] = x[5 + offset];
                    break;
                case 5:
                    result = -damping * x[5 + offset];
                    for (k = 0; k < 4; k++) {
                        if (bods[obj].active[k]) {
                            double[] v = bods[obj].calcVectors(x[0 + offset], x[2 + offset], x[4 + offset], k);


                            result += (v[0] * v[5] - v[1] * v[4]) * invMoment;
                        }
                    }
                    if (obj == dragObj) {
                        double[] v = bods[obj].calcVectors(x[0 + offset], x[2 + offset], x[4 + offset], 0);

                        double Fx = springConst * (mouseX - (x[0 + offset] + v[0]));
                        double Fy = springConst * (mouseY - (x[2 + offset] + v[1]));

                        result += (v[0] * Fy - v[1] * Fx) * invMoment;
                    }
                    change[i] = result;
                    break;
            }
        }
    }

    public void advance(double timeStep) {
        super.advance(timeStep);


        if (lastTimeStep == 0)
            ++stuckCounter;
        if (lastTimeStep > 0) {
            stuckCounter = 0;
            if (message != null) {
                cvs.removeElement(message);
                message = null;
            }
        } else if (stuckCounter >= 4) {
            System.out.println("we are stuck at time " + simTime);
            if (message == null) {
                message = new CText("Simulation is stuck!  Click reset to continue.");
                cvs.addElement(message);
            }
        }
    }

    protected void addCollisionDot(double x, double y, double magnitude, Color c) {
        if (showCollisionDot) {
            final double w = Math.max(0.02, Math.abs(magnitude));
            CMass m = new CMass(x - w / 2, y - w / 2, w, w, CElement.MODE_CIRCLE_FILLED);
            m.m_Color = c;
            cvs.addElement(m);
            rxnForces.addElement(m);
        }
    }
}


class Collision {
    public double depth;
    public double normalX;
    public double normalY;
    public double impactX;
    public double impactY;


    public double Rx;
    public double Ry;
    public double R2x;
    public double R2y;
    public double rxnForceX;
    public double rxnForceY;
    public int object;
    public int normalObj;
    public int corner;
    public boolean colliding = true;
    public boolean handled = false;

    public Collision() {
    }

    public static void addCollision(Vector collisions, Collision c2) {


        boolean shouldAdd = true;
        for (int i = 0; i < collisions.size(); i++) {
            Collision c = (Collision) collisions.elementAt(i);

            if (c.object == c2.object && c.normalObj == c2.normalObj ||
                    c.object == c2.normalObj && c.normalObj == c2.object) {

                double dx = c.impactX - c2.impactX;
                double dy = c.impactY - c2.impactY;
                if (Math.sqrt(dx * dx + dy * dy) <= Thruster5.DISTANCE_TOL) {


                    if (c2.depth < c.depth) {
                        collisions.removeElement(c);
                    } else
                        shouldAdd = false;
                }
            }
        }
        if (shouldAdd)
            collisions.addElement(c2);
    }

    public String toString() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(7);
        return (colliding ? "Collision" : "Contact") + " object=" + object
                + " normalObj=" + normalObj + " corner=" + corner
                + " depth=" + nf.format(depth)
                + " impact=(" + nf.format(impactX) + "," + nf.format(impactY)
                + ") normal=(" + nf.format(normalX) + "," + nf.format(normalY)
                + ")";
    }
}


class Thruster5Object implements Dragable {
    protected static final int BOTTOM = 0, RIGHT = 1, TOP = 2, LEFT = 3;
    protected static final int X = 0, VX = 1, Y = 2, VY = 3, W = 4, VW = 5;
    public double x = 0, y = 0;
    public double angle = 0;
    public double cmx, cmy;
    public double thrustX, thrustY;
    public double[] tAngle;
    public boolean[] active;
    public double tMagnitude = 0.5;
    public double mass = 1;
    public double ax, ay, bx, by, cx, cy, dx, dy, tx, ty;
    public Color color = Color.black;
    protected double width = 0.5;
    protected double height = 3.0;

    public Thruster5Object() {
        initialize();
    }

    public Thruster5Object(double width, double height) {
        initialize();
        setWidth(width);
        setHeight(height);
        thrustX = width / 2;
        thrustY = 0.8 * height;
    }

    private void initialize() {
        thrustX = width / 2;
        thrustY = 0.8 * height;
        tAngle = new double[4];
        tAngle[0] = Math.PI / 2;
        tAngle[1] = -Math.PI / 2;
        tAngle[2] = 0;
        tAngle[3] = Math.PI;
        active = new boolean[4];
        active[0] = active[1] = active[2] = active[3] = false;
        moveTo(x, y, angle);
    }

    public String toString() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(7);
        return "Thruster5Object x=" + nf.format(x) + " y=" + nf.format(y) + " angle=" + nf.format(angle) +
                " width=" + nf.format(width) + " height=" + nf.format(height) + " mass=" + nf.format(mass) + "\n" +
                " cornerA=(" + nf.format(ax) + "," + nf.format(ay) +
                ") cornerB=(" + nf.format(bx) + "," + nf.format(by) + ")\n" +
                " cornerC=(" + nf.format(cx) + "," + nf.format(cy) +
                ") cornerD=(" + nf.format(dx) + "," + nf.format(dy) + ")";
    }

    public boolean isDragable() {
        return (mass != Double.POSITIVE_INFINITY) ? true : false;
    }

    public double distanceSquared(double x, double y) {
        double dx = this.x - x;
        double dy = this.y - y;
        return dx * dx + dy * dy;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public void setPosition(double x, double y) {
        moveTo(x, y, this.angle);
    }

    public double getCornerX(int corner) {
        switch (corner) {
            case 1:
                return ax;
            case 2:
                return bx;
            case 3:
                return cx;
            case 4:
                return dx;
            default:
                return Double.POSITIVE_INFINITY;
        }
    }

    public double getCornerY(int corner) {
        switch (corner) {
            case 1:
                return ay;
            case 2:
                return by;
            case 3:
                return cy;
            case 4:
                return dy;
            default:
                return Double.POSITIVE_INFINITY;
        }
    }

    public double getWidth() {
        return this.width;
    }

    public void setWidth(double width) {
        this.width = width;
        cmx = width / 2;
    }

    public double getHeight() {
        return this.height;
    }

    public void setHeight(double height) {
        this.height = height;
        cmy = height / 2;
    }

    public double getMinHeight() {
        return (width < height) ? width / 2 : height / 2;
    }

    public double invMass() {
        return (mass == Double.POSITIVE_INFINITY) ? 0 : 1 / mass;
    }

    public double invMomentAboutCM() {
        return (mass == Double.POSITIVE_INFINITY) ? 0 : 12 / (mass * (width * width + height * height));
    }


    public double momentAboutCM() {
        return mass * (width * width + height * height) / 12;
    }


    public double kineticEnergy(double vx, double vy, double w) {
        double e = translationalEnergy(vx, vy);
        e += rotationalEnergy(w);
        return e;
    }

    public double rotationalEnergy(double w) {
        return 0.5 * momentAboutCM() * w * w;
    }

    public double translationalEnergy(double vx, double vy) {
        double e = 0.5 * mass * (vx * vx + vy * vy);
        return e;
    }


    public double[] momentum(double vx, double vy, double w) {
        double result[] = new double[3];
        result[0] = mass * vx;
        result[1] = mass * vy;

        result[2] = momentAboutCM() * w + mass * (x * vy - y * vx);
        return result;
    }


    public double[] setCornerAt(double H, double V, double angle) {
        double[] r = new double[2];
        double cosa = Math.cos(angle);
        double sina = Math.sin(angle);
        if (angle >= 0 && angle <= Math.PI) {


            r[0] = H - sina * cmy + cosa * cmx;
            r[1] = V + cosa * cmy + sina * cmx;
        } else if (angle < 0 && angle >= -Math.PI) {


            r[0] = H - sina * cmy + cosa * cmx - cosa * width;
            r[1] = V + cosa * cmy + sina * cmx - sina * width;
        } else
            throw new IllegalArgumentException("setCorner can only handle angles from -PI/2 to PI/2 "
                    + " but angle=" + angle);
        return r;
    }


    public double[] calcVectors(double x, double y, double angle, int thruster) {
        double sinAngle = Math.sin(angle);
        double cosAngle = Math.cos(angle);
        double ex = x + sinAngle * cmy;
        double ey = y - cosAngle * cmy;
        double ax = ex - cosAngle * cmx;
        double ay = ey - sinAngle * cmx;
        double tx = ax + cosAngle * thrustX - sinAngle * thrustY;
        double ty = ay + sinAngle * thrustX + cosAngle * thrustY;
        double tx2 = tx - Math.sin(angle + tAngle[thruster]) * tMagnitude;
        double ty2 = ty + Math.cos(angle + tAngle[thruster]) * tMagnitude;
        double rx = tx - x;
        double ry = ty - y;
        double rlen = Math.sqrt(rx * rx + ry * ry);
        double result[] = new double[6];
        result[0] = rx;
        result[1] = ry;
        result[2] = rx / rlen;
        result[3] = ry / rlen;
        result[4] = tx2 - tx;
        result[5] = ty2 - ty;
        return result;
    }

    public void moveTo(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;

        double sinAngle = Math.sin(this.angle);
        double cosAngle = Math.cos(this.angle);
        double ex = this.x + sinAngle * cmy;
        double ey = this.y - cosAngle * cmy;
        this.ax = ex - cosAngle * cmx;
        this.ay = ey - sinAngle * cmx;
        this.bx = this.ax + cosAngle * width;
        this.by = this.ay + sinAngle * width;
        this.cx = this.bx - sinAngle * height;
        this.cy = this.by + cosAngle * height;
        this.dx = this.ax - sinAngle * height;
        this.dy = this.ay + cosAngle * height;
        this.tx = this.ax + cosAngle * thrustX - sinAngle * thrustY;
        this.ty = this.ay + sinAngle * thrustX + cosAngle * thrustY;
    }

    public void draw(Graphics g, ConvertMap map) {

        int[] xPoints = new int[5];
        int[] yPoints = new int[5];
        int i = 0;
        xPoints[i] = map.simToScreenX(ax);
        yPoints[i++] = map.simToScreenY(ay);
        xPoints[i] = map.simToScreenX(bx);
        yPoints[i++] = map.simToScreenY(by);
        xPoints[i] = map.simToScreenX(cx);
        yPoints[i++] = map.simToScreenY(cy);
        xPoints[i] = map.simToScreenX(dx);
        yPoints[i++] = map.simToScreenY(dy);
        xPoints[i] = map.simToScreenX(ax);
        yPoints[i++] = map.simToScreenY(ay);
        g.setColor(this.color);
        g.fillPolygon(xPoints, yPoints, 5);
        if (mass != Double.POSITIVE_INFINITY) {

            double sz = 0.15 * ((width < height) ? width : height);
            int w = map.simToScreenScaleX(2 * sz);
            int sx = map.simToScreenX(tx - sz);
            int sy = map.simToScreenY(ty + sz);
            g.setColor(Color.gray);
            g.fillOval(sx, sy, w, w);


            double tx2, ty2;
            int k;
            for (k = 0; k < 4; k++) {
                if (active[k]) {
                    double len = Math.log(1 + tMagnitude) / 0.693219;
                    tx2 = tx + Math.sin(angle + tAngle[k]) * len;
                    ty2 = ty - Math.cos(angle + tAngle[k]) * len;
                    g.setColor(Color.red);
                    g.drawLine(map.simToScreenX(tx), map.simToScreenY(ty),
                            map.simToScreenX(tx2), map.simToScreenY(ty2));
                }
            }
        }
    }

    public Collision testCollision(double gx, double gy, int objIndex, int selfIndex) {


        gx -= this.x;
        gy -= this.y;
        double px = gx * Math.cos(-this.angle) - gy * Math.sin(-this.angle);
        double py = gx * Math.sin(-this.angle) + gy * Math.cos(-this.angle);
        px += this.cmx;
        py += this.cmy;

        int edge = 0;
        double dist = py;
        double d;
        d = this.width - px;
        if (d < dist) {
            dist = d;
            edge = 1;
        }
        d = this.height - py;
        if (d < dist) {
            dist = d;
            edge = 2;
        }
        d = px;
        if (d < dist) {
            dist = d;
            edge = 3;
        }
        if (dist > 0) {
            Collision result = new Collision();
            result.colliding = true;
            result.depth = dist;
            result.impactX = gx;
            result.impactY = gy;
            result.normalObj = selfIndex;
            result.object = objIndex;


            getNormalForEdge(result, edge);
            return result;
        } else
            return null;
    }

    protected void getNormalForEdge(Collision c, int edge) {


        double px, py;
        switch (edge) {
            case BOTTOM:
                px = 0;
                py = -1;
                break;
            case RIGHT:
                px = 1;
                py = 0;
                break;
            case TOP:
                px = 0;
                py = 1;
                break;
            case LEFT:
                px = -1;
                py = 0;
                break;
            default:
                throw new IllegalStateException("Can't find normal, no edge specified for " + c);
        }

        c.normalX = px * Math.cos(this.angle) - py * Math.sin(this.angle);
        c.normalY = px * Math.sin(this.angle) + py * Math.cos(this.angle);
    }
}
