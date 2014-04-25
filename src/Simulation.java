import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Vector;

public abstract class Simulation implements Runnable, Graphable, MouseDragHandler, DiffEq, Subject {
    protected boolean m_Animating = true;
    protected String[] var_names;
    protected SimCanvas cvs;
    protected Graph graph;
    protected double[] vars;

    protected boolean[] calc;
    protected DiffEqSolver odeSolver;
    protected Container container;
    protected double startTime = 0;
    private double lastTime = -9999;
    private JCheckBox showGraphCheckbox;
    private JCheckBox showControlsCheckbox;
    private Listener listener = new Listener();
    private Vector<Component> controls = new Vector<Component>(10);
    private Vector<Observer> observers = new Vector<Observer>(10);
    private SimLine separatorLine = null;
    private boolean paintControlCludge = true;

    public Simulation(Container applet, int numVars) {
        this(applet);
        vars = new double[numVars];
        calc = new boolean[numVars];
        for (int i = 0; i < calc.length; i++)
            calc[i] = true;
    }

    public Simulation(Container applet) {
        this.container = applet;
        container.add(cvs = makeSimCanvas(), 0);
        odeSolver = makeDiffEqSolver();
    }

    protected SimCanvas makeSimCanvas() {
        return new SimCanvas(this);
    }

    protected LayoutManager makeLayoutManager() {
        return new SimLayout();
    }

    protected DiffEqSolver makeDiffEqSolver() {
        return new RungeKutta(this);
    }

    protected Graph makeGraph() {
        return new Graph(this);
    }

    public void setupControls() {
        container.add(showControlsCheckbox = new JCheckBox("Элементы управления"));
        showControlsCheckbox.setSelected(false);
        showControlsCheckbox.addItemListener(listener);
    }

    protected int getComponentIndex(Component c) {
        Component[] cArray = container.getComponents();
        for (int i = 0; i < cArray.length; i++) {
            if (cArray[i] == c)
                return i;
        }
        return -1;
    }

    private boolean containerHas(Component c) {
        int n = container.getComponentCount();
        for (int i = 0; i < n; i++)
            if (c == container.getComponent(i))
                return true;
        return false;
    }

    public void setCoordMap(CoordMap map) {
        cvs.setCoordMap(map);
    }

    protected void addControl(Component c) {
        if (showControlsCheckbox != null &&
                null != showControlsCheckbox.getSelectedObjects()) {
            container.add(c);
        }
        controls.addElement(c);
    }

    protected void addObserverControl(Observer obs) {
        addControl((Component) obs);
        attach(obs);
    }

    private void attach(Observer o) {
        observers.addElement(o);
    }

    public void setParameter(String name, double value) {
        if (trySetParameter(name, value)) {
            for (Enumeration<Observer> e = observers.elements(); e.hasMoreElements(); )
                (e.nextElement()).update(this, name, value);
        } else
            throw new IllegalArgumentException("no such parameter " + name);
    }

    public double getParameter(String name) {
        throw new IllegalArgumentException("no such parameter " + name);
    }

    protected boolean trySetParameter(String name, double value) {
        return false;
    }

    public void setupGraph() {
        graph = makeGraph();
        if (graph != null) {
            container.add(graph, 1);
            int index = getComponentIndex(showControlsCheckbox);
            if (index < 0)
                throw new IllegalStateException("cannot setup graph because controls not yet created");
            container.add(showGraphCheckbox = new JCheckBox("График"), ++index);
            showGraphCheckbox.setSelected(true);
            showGraphCheckbox.addItemListener(listener);
            separatorLine = new SimLine();
            container.add(separatorLine, ++index);

            graph.createButtons(container, index);
            graph.setVars(0, 1);
            container.invalidate();

        }
    }

    public void showGraph(boolean wantGraph) {

        if (!wantGraph && graph == null)
            return;
        if (graph == null || showGraphCheckbox == null)
            throw new IllegalStateException("cannot show graph because graph not created");
        boolean hasGraph = containerHas(graph);
        if (!hasGraph && wantGraph) {
            container.add(graph, 1);
            graph.setVisible(true);
            container.invalidate();
            container.validate();
            graph.showControls(container, getComponentIndex(separatorLine));
            graph.enableControls(true);
        } else if (hasGraph && !wantGraph) {

            graph.hideControls(container);
            container.remove(graph);
            container.invalidate();
            container.validate();
            graph.enableControls(false);
        }
        showGraphCheckbox.setSelected(wantGraph);
    }

    protected void shutDown() {
        showControls(false);
        if (graph != null) {
            graph.hideControls(container);
            container.remove(graph);
            graph.freeOffScreen();
        }
        container.remove(cvs);
        cvs.freeOffscreen();
        if (separatorLine != null)
            container.remove(separatorLine);
        if (showGraphCheckbox != null)
            container.remove(showGraphCheckbox);
        if (showControlsCheckbox != null)
            container.remove(showControlsCheckbox);
        controls.removeAllElements();
        observers.removeAllElements();
    }

    public synchronized void showControls(boolean wantControls) {
        for (Enumeration<Component> e = controls.elements(); e.hasMoreElements(); ) {
            if (wantControls) {
                Component m = e.nextElement();
                container.add(m);
            } else
                container.remove(e.nextElement());
        }
        if (showControlsCheckbox != null)
            showControlsCheckbox.setSelected(wantControls);
    }

    public int numVariables() {
        return (vars != null) ? vars.length : 0;
    }

    public String getVariableName(int i) {
        return (var_names != null) ? var_names[i] : "";
    }

    public double getVariable(int i) {
        return (i < vars.length) ? vars[i] : 0;
    }

    protected void advance(double time) {
        odeSolver.step(time);
        modifyObjects();
    }


    public void run() {
        if (m_Animating) {
            if (graph != null) {
                graph.memorize();
                graph.repaint();
            }
            advance(getTimeStep());
            cvs.repaint();
        }

        if (paintControlCludge && getTime() > 2.0) {
            container.repaint();
            paintControlCludge = false;
        }
    }

    public abstract void modifyObjects();

    public double getTime() {
        double now = (double) System.currentTimeMillis() / 1000;
        if (startTime == 0)
            startTime = now;
        return now - startTime;
    }

    public double getTimeStep() {

        double now = getTime();
        double h;
        if (lastTime < 0)
            h = 0.05;
        else {
            h = now - lastTime;
            final double limit = 0.01;
            if (h > 0.1) {
                h = limit;
            }
        }
        lastTime = now;
        return h;
    }

    public void constrainedSet(Dragable e, double x, double y) {
    }

    public void startDrag(Dragable e) {
    }

    public void finishDrag(Dragable e) {
        for (int i = 0; i < calc.length; i++)
            calc[i] = true;
    }

    public double[] getVars() {
        return vars;
    }

    public boolean[] getCalc() {
        return calc;
    }

    public String toString() {
        String s = getClass().getName() + " with " +
                (vars != null ? vars.length + " variables " : "no variables");
        return s;
    }

    protected class Listener implements ItemListener {

        public void itemStateChanged(ItemEvent event) {
            ItemSelectable isl = event.getItemSelectable();
            if (showGraphCheckbox != null && isl == showGraphCheckbox) {
                showGraph(null != showGraphCheckbox.getSelectedObjects());
                container.invalidate();
                container.validate();
                container.repaint();
            } else if (showControlsCheckbox != null && isl == showControlsCheckbox) {
                showControls(null != showControlsCheckbox.getSelectedObjects());
                container.invalidate();
                container.validate();
                container.repaint();
            }
        }
    }
}


