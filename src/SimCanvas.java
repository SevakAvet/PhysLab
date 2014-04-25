import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;


public class SimCanvas extends JComponent implements KeyListener, MouseListener, MouseMotionListener, SimPanel {
    protected MouseDragHandler mdh = null;
    protected Dragable dragObj = null;
    protected CoordMap map = new CoordMap();
    private Image offScreen = null;
    private Graphics offScreenGraphics = null;
    private boolean needExpand = false;
    private int dragOffsetX = 0, dragOffsetY = 0;

    private Vector<Drawable> drawables = new Vector<Drawable>(10);
    private ObjectListener objListen = null;

    public SimCanvas(MouseDragHandler mdh) {
        this.mdh = mdh;
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        if (!isOpaque()) {
            setOpaque(true);
        }
    }

    public String toString() {
        return getClass().getName() + " with offScreen= " + offScreen.toString();
    }

    public void freeOffscreen() {
        offScreen = null;

        if (offScreenGraphics != null) {
            offScreenGraphics.dispose();
            offScreenGraphics = null;
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(300, 300);
    }

    public synchronized void addElement(Drawable e) {
        drawables.addElement(e);
    }


    public synchronized void setCoordMap(CoordMap map) {
        this.map = map;

        Dimension d = this.getSize();
        map.setScreen(0, 0, d.width, d.height);
        freeOffscreen();


        if (objListen != null)
            objListen.objectChanged(this);
    }

    public synchronized void setSize(int width, int height) {

        super.setSize(width, height);
        map.setScreen(0, 0, width, height);
        freeOffscreen();

        if (needExpand) {
            if (map.expand())
                needExpand = false;
        }
        if (objListen != null)
            objListen.objectChanged(this);
    }


    public void paintComponent(Graphics g) {
        if (offScreen != null) {
            if (offScreenGraphics == null)
                offScreenGraphics = offScreen.getGraphics();
            Dimension size = getSize();
            offScreenGraphics.setClip(0, 0, size.width, size.height);

            offScreenGraphics.setColor(Color.white);
            offScreenGraphics.fillRect(0, 0, size.width, size.height);
            drawElements(offScreenGraphics, map.getConvertMap());
            g.drawImage(offScreen, 0, 0, null);
            g.drawRect(10, 10, 100, 100);
        } else {
            Dimension size = getSize();
            g.setClip(0, 0, size.width, size.height);

            g.setColor(Color.white);
            g.fillRect(0, 0, size.width, size.height);
            drawElements(g, map.getConvertMap());
        }
    }

    protected synchronized void drawElements(Graphics g, ConvertMap map) {
        for (Enumeration<Drawable> e = drawables.elements(); e.hasMoreElements(); )
            (e.nextElement()).draw(g, map);
    }


    protected synchronized Dragable findNearestDragable(double x, double y) {
        double distance = Double.POSITIVE_INFINITY;
        Dragable nearest = null;

        for (Enumeration<Drawable> e = drawables.elements(); e.hasMoreElements(); ) {
            Object o = e.nextElement();
            if (o instanceof Dragable) {
                Dragable d = (Dragable) o;
                if (d.isDragable()) {
                    double dist = d.distanceSquared(x, y);
                    if (dist < distance) {
                        distance = dist;
                        nearest = d;
                    }
                }
            }
        }
        return nearest;
    }

    public void mousePressed(MouseEvent evt) {
        int scr_x = evt.getX();
        int scr_y = evt.getY();

        double sim_x = map.screenToSimX(scr_x);
        double sim_y = map.screenToSimY(scr_y);
        dragObj = findNearestDragable(sim_x, sim_y);
        if (dragObj != null) {
            dragOffsetX = scr_x - map.simToScreenX(dragObj.getX());
            dragOffsetY = scr_y - map.simToScreenY(dragObj.getY());
            if (mdh != null)
                mdh.startDrag(dragObj);
        }

    }

    public void mouseDragged(MouseEvent evt) {
        if (dragObj != null) {
            double sim_x = map.screenToSimX(evt.getX() - dragOffsetX);
            double sim_y = map.screenToSimY(evt.getY() - dragOffsetY);


            if (mdh != null)
                mdh.constrainedSet(dragObj, sim_x, sim_y);
        }

    }

    public void mouseReleased(MouseEvent evt) {
        if (dragObj != null && mdh != null)
            mdh.finishDrag(dragObj);
        dragObj = null;
    }

    public void mouseClicked(MouseEvent evt) {
    }

    public void mouseEntered(MouseEvent evt) {
    }

    public void mouseExited(MouseEvent evt) {
    }

    public void mouseMoved(MouseEvent evt) {
    }


    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public boolean isFocusable() {
        return true;
    }

}

