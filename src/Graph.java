import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;


public class Graph extends JComponent implements MouseListener, ItemListener, ActionListener, SimPanel {
    public static final int DOTS = 0;
    public static final int LINES = 1;
    private int drawMode = LINES;
    private static final int memLen = 3000;
    private double[] memX = new double[memLen];
    private double[] memY = new double[memLen];
    private double[] memZ = new double[memLen];

    static private float[] red = Color.RGBtoHSB(1, 0, 0, null);
    static private float redHue = red[0];
    static private float[] blue = Color.RGBtoHSB(0, 0, 1, null);
    static private float blueHue = blue[0];
    static private float diffHue = (redHue < blueHue) ? blueHue - redHue : redHue - blueHue;
    static private float lowHue = (redHue < blueHue) ? redHue : blueHue;
    static private boolean zMode = false;
    private Image offScreen = null;
    private Graphable sim;
    private CoordMap map;

    private int xVar = 0;
    private int yVar = 1;
    private NumberFormat nf;
    private Font numFont = null;
    private FontMetrics numFM = null;
    private boolean autoScale;
    private boolean rangeSet;
    private boolean rangeDirty;
    private double rangeXHi, rangeXLo, rangeYHi, rangeYLo;
    private double rangeTime = 0;
    private boolean needRedraw = true;
    private int memIndex = 0;
    private int memSize = 0;
    private int memDraw = 0;
    private JComboBox<String> yGraphChoice;
    private JPanel yPanel;
    private JComboBox<String> xGraphChoice;
    private JPanel xPanel;
    private JComboBox<String> dotChoice;
    private JButton clearButton;


    public Graph(Graphable sim) {
        this.sim = sim;

        int sz = 10;
        map = new CoordMap(CoordMap.INCREASE_UP, -sz, sz, -sz, sz,
                CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE);
        nf = NumberFormat.getNumberInstance();
        map.setFillScreen(true);
        setAutoScale(true);
        addMouseListener(this);
        if (!isOpaque()) {
            setOpaque(true);
        }
    }


    static private Color zToColor(double z) {
        float zFraction = (((float) z - (float) (-1.7)) / (float) 3.4);
        return Color.getHSBColor(zFraction * diffHue + lowHue, 1, 1);
    }

    public Dimension getPreferredSize() {
        return new Dimension(300, 300);
    }

    public void createButtons(Container container, int index) {
        clearButton = new JButton("Очистить график");
        clearButton.addActionListener(this);

        int n = sim.numVariables();
        yPanel = new JPanel();
        yPanel.setLayout(new BorderLayout(1, 1));
        yPanel.add(new MyLabel("Y:"), BorderLayout.WEST);
        yGraphChoice = new JComboBox<String>();
        for (int i = 0; i < n; i++)
            yGraphChoice.addItem(sim.getVariableName(i));
        yGraphChoice.addItemListener(this);
        yGraphChoice.setSelectedIndex(yVar);
        yPanel.add(yGraphChoice, BorderLayout.EAST);

        xPanel = new JPanel();
        xPanel.setLayout(new BorderLayout(1, 1));
        xPanel.add(new MyLabel("X:"), BorderLayout.WEST);
        xGraphChoice = new JComboBox<String>();
        for (int i = 0; i < n; i++)
            xGraphChoice.addItem(sim.getVariableName(i));
        xGraphChoice.addItemListener(this);
        xGraphChoice.setSelectedIndex(xVar);
        xPanel.add(xGraphChoice, BorderLayout.EAST);


        dotChoice = new JComboBox<String>();
        dotChoice.addItem("точка");
        dotChoice.addItem("линия");
        dotChoice.setSelectedIndex(drawMode);
        dotChoice.addItemListener(this);

        showControls(container, index);
    }

    public void enableControls(boolean b) {
        yGraphChoice.setEnabled(b);
        xGraphChoice.setEnabled(b);
        dotChoice.setEnabled(b);
        clearButton.setEnabled(b);
    }

    public void hideControls(Container container) {
        container.remove(yPanel);
        container.remove(xPanel);
        container.remove(dotChoice);
        container.remove(clearButton);
    }

    public void showControls(Container container, int index) {
        container.add(clearButton, index);
        container.add(yPanel, ++index);
        container.add(xPanel, ++index);
        container.add(dotChoice, ++index);
    }

    public void itemStateChanged(ItemEvent event) {
        if (yGraphChoice != null)
            yVar = yGraphChoice.getSelectedIndex();
        if (xGraphChoice != null)
            xVar = xGraphChoice.getSelectedIndex();
        if (dotChoice != null)
            drawMode = dotChoice.getSelectedIndex();
        reset();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == clearButton)
            reset();
    }

    public void setXVar(int xVar) {
        if ((xVar >= 0) && (xVar < sim.numVariables())) {
            this.xVar = xVar;
            xGraphChoice.setSelectedIndex(xVar);
            reset();
        }
    }

    public void setYVar(int yVar) {
        if ((yVar >= 0) && (yVar < sim.numVariables())) {
            this.yVar = yVar;
            yGraphChoice.setSelectedIndex(yVar);
            reset();
        }
    }

    public void setVars(int xVar, int yVar) {
        setXVar(xVar);
        setYVar(yVar);
    }

    public void setDrawMode(int drawMode) {
        this.drawMode = drawMode;
        dotChoice.setSelectedIndex(drawMode);
        reset();
    }

    public synchronized void reset() {
        rangeSet = false;
        rangeDirty = false;
        rangeTime = 0;
        memIndex = memSize = memDraw = 0;
        needRedraw = true;
        repaint();
    }

    public void setSize(int width, int height) {
        super.setSize(width, height);
        freeOffScreen();
        map.setScreen(0, 0, width, height);
        needRedraw = true;
    }

    public void freeOffScreen() {
        offScreen = null;
    }


    public synchronized void memorize() {
        memX[memIndex] = sim.getVariable(xVar);
        memY[memIndex] = sim.getVariable(yVar);
        int zVar = 2;
        if (zMode)
            memZ[memIndex] = sim.getVariable(zVar);
        if (autoScale)
            rangeCheck(memX[memIndex], memY[memIndex]);
        memIndex++;
        if (memSize < memLen)
            memSize++;
        if (memIndex >= memLen)
            memIndex = 0;
    }


    private int drawPoints(Graphics g, int from) {
        int pointer = from;
        if (memSize > 0)
            while (true) {

                int i1 = pointer;
                int i2 = (pointer + 1) % memLen;

                if (i2 != memIndex) {
                    g.setColor(Color.black);
                    int x, y, w, h;
                    if (zMode)
                        g.setColor(Graph.zToColor(memZ[i1]));
                    if (drawMode == DOTS) {
                        x = map.simToScreenX(memX[i1]);
                        y = map.simToScreenY(memY[i1]);
                        int dotSize = 1;
                        w = dotSize;
                        h = dotSize;
                        g.fillRect(x, y, w, h);
                    } else {
                        int x1 = map.simToScreenX(memX[i1]);
                        int y1 = map.simToScreenY(memY[i1]);
                        int x2 = map.simToScreenX(memX[i2]);
                        int y2 = map.simToScreenY(memY[i2]);
                        g.drawLine(x1, y1, x2, y2);
                    }
                    pointer = i2;
                } else
                    break;
            }
        return pointer;
    }


    private void updateOSB() {
        if (offScreen == null)
            offScreen = createImage(getSize().width, getSize().height);
        assert (offScreen != null);
        Graphics osb = offScreen.getGraphics();
        assert (osb != null);
        if (needRedraw) {
            Rectangle b = new Rectangle(0, 0, getWidth(), getHeight());

            osb.setColor(Color.white);
            osb.fillRect(b.x, b.y, b.width, b.height);
            osb.setColor(Color.lightGray);
            osb.drawRect(b.x, b.y, b.width - 1, b.height - 1);
            drawAxes(osb);

            int start = (memSize < memLen) ? 0 : memIndex;
            memDraw = drawPoints(osb, start);
            needRedraw = false;
        } else {

            memDraw = drawPoints(osb, memDraw);
        }
        osb.dispose();
    }

    protected synchronized void paintComponent(Graphics g) {
        updateOSB();
        Rectangle clip = g.getClipBounds();

        g.drawImage(offScreen, clip.x, clip.y, clip.width, clip.height, null);

        int xorX = map.simToScreenX(memX[memDraw]) - 1;
        int xorY = map.simToScreenY(memY[memDraw]) - 1;
        Color saveColor = g.getColor();
        g.setColor(Color.red);
        g.fillRect(xorX, xorY, 4, 4);
        g.setColor(saveColor);
    }


    public void setAutoScale(boolean auto) {
        autoScale = auto;
        rangeSet = false;
        rangeDirty = false;
        rangeTime = 0;
    }

    private double getTime() {
        return (double) System.currentTimeMillis() / 1000;
    }

    private void rangeCheck(double nowX, double nowY) {
        if (!rangeSet) {
            rangeXHi = nowX;
            rangeXLo = nowX;
            rangeYHi = nowY;
            rangeYLo = nowY;
            rangeSet = true;
        } else {
            double xspan = rangeXHi - rangeXLo;
            double yspan = rangeYHi - rangeYLo;
            double extra = 0.1;
            if (nowX <= rangeXLo) {
                rangeXLo = nowX - extra * xspan;
                rangeDirty = true;
            }
            if (nowX >= rangeXHi) {
                rangeXHi = nowX + extra * xspan;
                rangeDirty = true;
            }
            if (nowY <= rangeYLo) {
                rangeYLo = nowY - extra * yspan;
                rangeDirty = true;
            }
            if (nowY >= rangeYHi) {
                rangeYHi = nowY + extra * yspan;
                rangeDirty = true;
            }
        }
        double now = getTime();


        if (rangeTime == 0.0)
            rangeTime = now;
        if (rangeDirty && now > rangeTime + 2) {
            rangeTime = now;
            map.setRange(rangeXLo, rangeXHi, rangeYLo, rangeYHi);

            needRedraw = true;
            repaint();
            rangeDirty = false;
        }
    }

    private void setAxesFont(Graphics g) {
        if (numFont == null) {
            numFont = new Font("SansSerif", Font.PLAIN, 12);
            numFM = g.getFontMetrics(numFont);
        }
        g.setFont(numFont);
    }

    private void drawAxes(Graphics g) {
        setAxesFont(g);

        int x0, y0;
        double sim_x1 = map.getMinX();
        double sim_x2 = map.getMaxX();
        double sim_y1 = map.getMinY();
        double sim_y2 = map.getMaxY();
        x0 = map.simToScreenX(sim_x1 + 0.05 * (sim_x2 - sim_x1));

        y0 = map.simToScreenY(sim_y1) - (10 + numFM.getAscent() + numFM.getDescent());

        g.setColor(Color.darkGray);
        g.drawLine(map.simToScreenX(sim_x1), y0, map.simToScreenX(sim_x2), y0);

        g.drawLine(x0, map.simToScreenY(sim_y1), x0, map.simToScreenY(sim_y2));
        drawHorizTicks(y0, g);
        drawVertTicks(x0, g);
    }


    private void drawHorizTicks(int y0, Graphics g) {
        int y1 = y0 - 4;
        int y2 = y1 + 8;
        double sim_x1 = map.getMinX();
        double sim_x2 = map.getMaxX();
        double graphDelta = getNiceIncrement(sim_x2 - sim_x1);
        double x_sim = getNiceStart(sim_x1, graphDelta);
        while (x_sim < sim_x2) {
            int x_screen = map.simToScreenX(x_sim);
            g.setColor(Color.black);
            g.drawLine(x_screen, y1, x_screen, y2);


            g.setColor(Color.gray);
            String s = nf.format(x_sim);
            int textWidth = numFM.stringWidth(s);
            g.drawString(s, x_screen - textWidth / 2, y2 + numFM.getAscent());

            x_sim += graphDelta;
        }


        String hname = sim.getVariableName(xVar);
        int w = numFM.stringWidth(hname);
        g.drawString(hname, map.simToScreenX(sim_x2) - w - 5, y0 - 8);
    }

    private void drawVertTicks(int x0, Graphics g) {
        int x1 = x0 - 4;
        int x2 = x1 + 8;
        double sim_y1 = map.getMinY();
        double sim_y2 = map.getMaxY();
        double graphDelta = getNiceIncrement(sim_y2 - sim_y1);
        double y_sim = getNiceStart(sim_y1, graphDelta);
        while (y_sim < sim_y2) {
            int y_screen = map.simToScreenY(y_sim);
            g.setColor(Color.black);
            g.drawLine(x1, y_screen, x2, y_screen);


            g.setColor(Color.gray);
            String s = nf.format(y_sim);
            g.drawString(s, x2 + 5, y_screen + (numFM.getAscent() / 2));

            y_sim += graphDelta;
        }


        String vname = sim.getVariableName(yVar);
        int w = numFM.stringWidth(vname);
        g.drawString(vname, x0 + 6, map.simToScreenY(sim_y2) + 13);
    }

    private double getNiceIncrement(double range) {


        double power = Math.pow(10, Math.floor(Math.log(range) / Math.log(10)));
        double logTot = range / power;

        double incr;
        if (logTot >= 8)
            incr = 2;
        else if (logTot >= 5)
            incr = 1;
        else if (logTot >= 3)
            incr = 0.5;
        else if (logTot >= 2)
            incr = 0.4;
        else
            incr = 0.2;
        incr *= power;

        double dlog = Math.log(incr) / Math.log(10);
        int digits = (dlog < 0) ? (int) Math.ceil(-dlog) : 0;
        nf.setMaximumFractionDigits(digits);
        nf.setMinimumFractionDigits(0);
        return incr;
    }

    private double getNiceStart(double start, double incr) {

        return Math.ceil(start / incr) * incr;
    }

    public void mousePressed(MouseEvent evt) {
    }

    public void mouseReleased(MouseEvent evt) {
    }

    public void mouseClicked(MouseEvent evt) {

    }

    public void mouseEntered(MouseEvent evt) {
    }

    public void mouseExited(MouseEvent evt) {
    }

}
