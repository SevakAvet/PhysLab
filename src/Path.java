import java.awt.*;

class CPoint {
    public double x;
    public double y;
    public double p;
    public double slope = 0;
    public double radius = 0;
    public boolean radius_flag = false;
    public int direction;
    public int ball = 0;

    CPoint() {
    }

    CPoint(int num) {
        ball = num;
    }
}

class C2Points {
    public double x1;
    public double y1;
    public int ball = 0;
}

class PathName {
    public static final PathName LOOP = new PathName("loop");
    public static final PathName CIRCLE = new PathName("circle");
    public static final PathName FLAT = new PathName("flat");
    public static final PathName LEMNISCATE = new PathName("infinity");
    public static final PathName OVAL = new PathName("oval");
    public static final PathName SPIRAL = new PathName("spiral");
    public static final PathName HUMP = new PathName("hump");
    public static final PathName CARDIOID = new PathName("cardioid");
    private final String name;

    private PathName(String name) {
        this.name = name;
    }

    public static PathName[] getPathNames() {
        PathName[] p = new PathName[7];
        p[0] = PathName.HUMP;
        p[1] = PathName.LOOP;
        p[2] = PathName.CIRCLE;
        p[3] = PathName.LEMNISCATE;
        p[4] = PathName.OVAL;
        p[5] = PathName.SPIRAL;
        p[6] = PathName.CARDIOID;
        return p;
    }

    public String toString() {
        return name;
    }
}


abstract class CPath implements Drawable {
    protected static final int DRAW_POINTS = 500;
    protected static final int DATA_POINTS = 9000;
    private static final int BALLS = 4;
    public double tLo = 0;
    public double tHi = 1;
    public double left = 0, top = 1, right = 1, bottom = 0;
    public boolean exact_slope = false;
    protected boolean closed = false;
    protected double plen = 0;
    private double[] xvals;
    private double[] yvals;
    private double[] pvals;
    private PathName pathName;

    private int[] p_index;
    private int[] x_index;

    CPath() {
        initialize();
        xvals = new double[DATA_POINTS];
        yvals = new double[DATA_POINTS];
        pvals = new double[DATA_POINTS];
        p_index = new int[BALLS];
        x_index = new int[BALLS];
        for (int i = 0; i < BALLS; i++) {
            p_index[i] = -1;
            x_index[i] = -1;
        }
        make_table();
    }

    public String toString() {
        return "Path " + this.pathName;
    }

    public double modp(double p) {
        if (closed && ((p < 0) || (p > plen))) p = p - plen * Math.floor(p / plen);
        return p;
    }

    protected abstract void initialize();

    protected abstract double x_func(double t);

    protected abstract double y_func(double x);

    protected double my_path_func(double x) {
        throw new RuntimeException();
    }

    protected double slope(double p) {
        throw new RuntimeException();
    }

    private int binSearch(double arr[], double x, int guess) {
        int i, min, max;
        int n = arr.length;
        if (n < 2)
            throw new IllegalArgumentException("array must have more than one element");
        boolean dir = arr[0] < arr[n - 1];
        if (guess < 0)
            i = 0;
        else if (guess > 0)
            i = n - 1;
        else
            i = guess;
        if (dir) {
            min = 0;
            max = n - 1;
        } else {
            min = n - 1;
            max = 0;
        }

        if (dir) {
            if (x < arr[0])
                return -1;
            if (x > arr[n - 1])
                return n;
        } else {
            if (x < arr[n - 1])
                return n;
            if (x > arr[0])
                return -1;
        }
        while (Math.abs(max - min) > 1) {
            if (x > arr[i]) {
                if (dir)
                    min = i;
                else
                    max = i;
            } else {
                if (dir)
                    max = i;
                else
                    min = i;
            }
            if (dir)
                i = min + (max - min) / 2;
            else
                i = max + (min - max) / 2;
        }
        return i;
    }

    private void make_table() {
        double t, p, delta;
        double dx, dy;
        boolean warn = true;
        delta = (tHi - tLo) / (double) (DATA_POINTS - 1);
        t = tLo;
        p = 0;
        pvals[0] = 0;
        xvals[0] = x_func(t);
        yvals[0] = y_func(t);
        int i = 1;
        do {
            t += delta;
            xvals[i] = x_func(t);
            yvals[i] = y_func(t);
            dx = xvals[i] - xvals[i - 1];
            if (warn && (dx == 0)) {
                System.out.println("track has a vertical section");
                warn = false;
            }
            dy = yvals[i] - yvals[i - 1];

            p += Math.sqrt(dx * dx + dy * dy);
            pvals[i] = p;
        }
        while (++i < DATA_POINTS);
        plen = pvals[DATA_POINTS - 1];
    }


    public void draw(Graphics g, ConvertMap map) {
        int scrx, scry, oldx, oldy;
        double p_first, p_final;
        double p, p_prev, x, y;
        double delta;
        int p_index;

        p_first = pvals[0];
        p_final = pvals[DATA_POINTS - 1];
        if (p_final <= p_first)
            System.out.println("draw_track reports track data is out of order");


        delta = (p_final - p_first) / DRAW_POINTS;
        p_index = 0;
        p_prev = pvals[p_index];

        x = xvals[p_index];
        y = yvals[p_index];
        scrx = map.simToScreenX(x);
        scry = map.simToScreenY(y);

        g.setPaintMode();
        g.setColor(Color.white);
        Rectangle r = map.getScreenRect();
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(Color.black);

        do {
            do {
                p_index++;
                if (p_index > DATA_POINTS - 1) {
                    p = p_final;
                    p_index = DATA_POINTS - 1;
                    break;
                } else
                    p = pvals[p_index];
            } while (p - p_prev < delta);
            p_prev = p;

            oldx = scrx;
            oldy = scry;
            x = xvals[p_index];
            y = yvals[p_index];
            scrx = map.simToScreenX(x);
            scry = map.simToScreenY(y);
            g.drawLine(oldx, oldy, scrx, scry);
        } while (p < p_final);

    }
}

