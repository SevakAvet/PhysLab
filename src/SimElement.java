import java.awt.*;


abstract class CElement implements Drawable {
    public static final int NO_DRAW = 0;
    public static final int MODE_RECT = 1;
    public static final int MODE_CIRCLE = 2;
    public static final int MODE_SPRING = 3;
    public static final int MODE_LINE = 4;
    public static final int MODE_CIRCLE_FILLED = 5;


    public double m_X1, m_Y1, m_X2, m_Y2;


    public int m_DrawMode;
    public double m_Mass;
    public Color m_Color;

    public CElement(double X1, double Y1, double X2, double Y2) {
        m_X1 = X1;
        m_Y1 = Y1;
        m_X2 = X2;
        m_Y2 = Y2;
        m_DrawMode = 0;
        m_Mass = 0;
        m_Color = Color.black;
    }


    public double distanceSquared(double x, double y) {
        double dx = (m_X2 + m_X1) / 2 - x;
        double dy = (m_Y2 + m_Y1) / 2 - y;
        return dx * dx + dy * dy;
    }

    public void setBounds(double x1, double y1, double x2, double y2) {
        m_X1 = x1;
        m_Y1 = y1;
        m_X2 = x2;
        m_Y2 = y2;
    }

    public void setX1(double p) {
        m_X1 = p;
    }

    public void setX2(double p) {
        m_X2 = p;
    }

    public void setY1(double p) {
        m_Y1 = p;
    }

    public void setY2(double p) {
        m_Y2 = p;
    }

    public abstract void draw(Graphics g, ConvertMap map);

    public double getCenterX() {
        return (m_X1 + m_X2) / 2;
    }

    public double getCenterY() {
        return (m_Y1 + m_Y2) / 2;
    }

    public double getX() {
        return m_X1;
    }

    public double getY() {
        return m_Y1;
    }
}

class CMass extends CElement implements Dragable {
    public double m_Height;
    public double m_Width;
    public double m_Damping = 0;
    public boolean m_Dragable = false;


    public CMass(double X1, double Y1, double width, double height, int drawMode) {
        super(X1, Y1, X1 + width, Y1 + height);
        m_Height = height;
        m_Width = width;
        m_Mass = 1;
        m_DrawMode = drawMode;
        m_Dragable = true;
        m_Color = Color.red;

    }

    public boolean isDragable() {
        return m_Dragable;
    }

    public void setPosition(double Xpos, double Ypos) {
        m_X1 = Xpos;
        m_X2 = Xpos + m_Width;
        m_Y1 = Ypos;
        m_Y2 = Ypos + m_Height;

    }

    public void setX1(double Xpos) {
        m_X1 = Xpos;
        m_X2 = Xpos + m_Width;
    }

    public void setY1(double Ypos) {
        m_Y1 = Ypos;
        m_Y2 = Ypos + m_Height;
    }

    public void draw(Graphics g, ConvertMap map) {
        int x1, y1, x2, y2;
        x1 = map.simToScreenX(m_X1);
        y1 = map.simToScreenY(m_Y1);
        x2 = map.simToScreenX(m_X2);
        y2 = map.simToScreenY(m_Y2);
        if (y2 < y1) {
            int d = y2;
            y2 = y1;
            y1 = d;
        }

        g.setColor(m_Color);
        switch (m_DrawMode) {
            case CElement.NO_DRAW:
                break;
            case CElement.MODE_RECT:
                g.drawRect(x1, y1, x2 - x1, y2 - y1);
                break;
            case CElement.MODE_CIRCLE:
                g.drawOval(x1, y1, x2 - x1, y2 - y1);
                break;
            case CElement.MODE_CIRCLE_FILLED:
                g.fillOval(x1, y1, x2 - x1, y2 - y1);
                break;
        }

    }
}

class CSpring extends CElement {
    public double m_RestLength = 1.0;
    public double m_Thickness = 0.5;
    public double m_SpringConst = 1.0;
    public Color m_Color2 = null;

    public CSpring(double X1, double Y1, double restLen, double thick) {


        super(X1, Y1, X1 + restLen, Y1);
        m_Thickness = thick;
        m_Mass = 0;
        m_SpringConst = 1;
        m_RestLength = restLen;
        m_DrawMode = CElement.MODE_SPRING;
        m_Color = Color.red;
        m_Color2 = Color.green;
    }

    public void draw(Graphics g, ConvertMap map) {
        if (m_Color2 == null)
            m_Color2 = m_Color.brighter();
        int cycles = 3;
        double x1 = m_X1;
        double x2 = m_X2;
        double y1 = m_Y1;
        double y2 = m_Y2;
        double cos_theta, sin_theta;


        {
            double theta = Math.atan((y2 - y1) / (x2 - x1));
            if (x2 < x1)
                theta += Math.acos(-1);
            cos_theta = Math.cos(theta);
            sin_theta = Math.sin(theta);
        }

        double len = Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
        if (len == 0)
            return;
        if ((m_DrawMode == CElement.MODE_SPRING) && (m_SpringConst == 0))
            return;
        double h = m_Thickness;
        double w = len / 16;
        double x, y;
        int xscr0, yscr0;


        xscr0 = map.simToScreenX(x1);
        yscr0 = map.simToScreenY(y1);

        if (m_DrawMode == CElement.MODE_LINE) {

            x2 = len;
            y2 = 0;
            x = x1 + cos_theta * x2 - sin_theta * y2;
            y = y1 + sin_theta * x2 + cos_theta * y2;
            int xscr = map.simToScreenX(x);
            int yscr = map.simToScreenY(y);

            g.setColor(m_Color);
            g.drawLine(xscr0, yscr0, xscr, yscr);
        } else if (m_DrawMode == CElement.MODE_SPRING) {

            int nPoints = 5 + 2 * cycles;
            int i = 0;

            int[] xPoints = new int[nPoints];
            int[] yPoints = new int[nPoints];
            xPoints[i] = xscr0;
            yPoints[i++] = yscr0;


            if (len < m_RestLength)
                g.setColor(m_Color);
            else
                g.setColor(m_Color2);


            x2 = w;
            y2 = 0;
            x = x1 + cos_theta * x2 - sin_theta * y2;
            y = y1 + sin_theta * x2 + cos_theta * y2;
            xPoints[i] = map.simToScreenX(x);
            yPoints[i++] = map.simToScreenY(y);


            x2 = 2 * w;
            y2 = -h / 2;
            x = x1 + cos_theta * x2 - sin_theta * y2;
            y = y1 + sin_theta * x2 + cos_theta * y2;
            xPoints[i] = map.simToScreenX(x);
            yPoints[i++] = map.simToScreenY(y);


            int j;
            for (j = 1; j <= cycles; j++) {
                x2 = 4 * j * w;
                y2 = h / 2;
                x = x1 + cos_theta * x2 - sin_theta * y2;
                y = y1 + sin_theta * x2 + cos_theta * y2;
                xPoints[i] = map.simToScreenX(x);
                yPoints[i++] = map.simToScreenY(y);

                x2 = (4 * j + 2) * w;
                y2 = -h / 2;
                x = x1 + cos_theta * x2 - sin_theta * y2;
                y = y1 + sin_theta * x2 + cos_theta * y2;
                xPoints[i] = map.simToScreenX(x);
                yPoints[i++] = map.simToScreenY(y);
            }


            x2 = (3 + cycles * 4) * w;
            y2 = 0;
            x = x1 + cos_theta * x2 - sin_theta * y2;
            y = y1 + sin_theta * x2 + cos_theta * y2;
            xPoints[i] = map.simToScreenX(x);
            yPoints[i++] = map.simToScreenY(y);


            x2 = len;
            y2 = 0;
            x = x1 + cos_theta * x2 - sin_theta * y2;
            y = y1 + sin_theta * x2 + cos_theta * y2;
            xPoints[i] = map.simToScreenX(x);
            yPoints[i++] = map.simToScreenY(y);

            g.drawPolyline(xPoints, yPoints, nPoints);
        }
    }
}