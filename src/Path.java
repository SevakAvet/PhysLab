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

    public static CPath makePath(PathName pName) {
        CPath p = null;
        if (pName == PathName.HUMP) p = new CPath_Hump();
        else if (pName == PathName.LOOP) p = new CPath_Loop();
        else if (pName == PathName.CIRCLE) p = new CPath_Circle();
        else if (pName == PathName.FLAT) p = new CPath_Flat();
        else if (pName == PathName.LEMNISCATE) p = new CPath_Lemniscate();
        else if (pName == PathName.OVAL) p = new CPath_Oval();
        else if (pName == PathName.SPIRAL) p = new CPath_Spiral();
        else if (pName == PathName.CARDIOID) p = new CPath_Cardioid();
        else
            throw new IllegalArgumentException("no such path " + pName);
        p.pathName = pName;
        return p;
    }

    public String toString() {
        return "Path " + this.pathName;
    }

    public double path_lo() {
        return pvals[0];
    }

    public double path_hi() {
        return pvals[DATA_POINTS - 1];
    }

    public double modp(double p) {

        if (closed && ((p < 0) || (p > plen))) {

            p = p - plen * Math.floor(p / plen);
        }
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

    public boolean off_track(double x) {
        if (closed)
            return false;
        else
            return ((x < xvals[0]) || (x > xvals[DATA_POINTS - 1]));
    }

    public double off_track_adjust(double x) {
        if (x < xvals[0])
            x = xvals[0] + 0.1;
        if (x > xvals[DATA_POINTS - 1])
            x = xvals[DATA_POINTS - 1] - 0.1;
        return x;
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

    private void make_table_old() {


        double delta, halfdelta, t, p;
        double p1, p2, p3;
        delta = (tHi - tLo) / (double) DATA_POINTS;
        halfdelta = delta / 2;
        t = tLo;
        p = 0;
        for (int i = 0; i < DATA_POINTS; i++) {
            xvals[i] = x_func(t);
            pvals[i] = p;
            yvals[i] = y_func(t);


            p1 = my_path_func(t) / 3;
            p2 = 4 * my_path_func(t + halfdelta) / 3;
            p3 = my_path_func(t + delta) / 3;
            p += halfdelta * (p1 + p2 + p3);
            t += delta;
        }
        plen = pvals[DATA_POINTS - 1];
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


    private double interp4(double xx[], double yy[], double x, int i) {

        double c1, c2, c3, c4, y;


        if (i < 0)
            i = 0;
        if (i > DATA_POINTS - 4)
            i = DATA_POINTS - 4;
        c1 = yy[i + 0];
        c2 = (yy[i + 1] - c1) / (xx[i + 1] - xx[i + 0]);
        c3 = (yy[i + 2] - (c1 + c2 * (xx[i + 2] - xx[i + 0]))) / ((xx[i + 2] - xx[i + 0]) * (xx[i + 2] - xx[i + 1]));
        c4 = yy[i + 3] - (c1 + c2 * (xx[i + 3] - xx[i + 0]) + c3 * (xx[i + 3] - xx[i + 0]) * (xx[i + 3] - xx[i + 1]));
        c4 = c4 / ((xx[i + 3] - xx[i + 0]) * (xx[i + 3] - xx[i + 1]) * (xx[i + 3] - xx[i + 2]));

        y = ((c4 * (x - xx[i + 2]) + c3) * (x - xx[i + 1]) + c2) * (x - xx[i + 0]) + c1;
        return y;
    }

    public void map_x(CPoint pt) {


        x_index[pt.ball] = binSearch(xvals, pt.x, x_index[pt.ball]);
        int k = x_index[pt.ball];

        pt.y = interp4(xvals, yvals, pt.x, k - 1);
        pt.p = interp4(xvals, pvals, pt.x, k - 1);
    }

    public double map_x_to_y(double x, int ball) {


        x_index[ball] = binSearch(xvals, x, x_index[ball]);

        return interp4(xvals, yvals, x, x_index[ball] - 1);
    }

    public double map_x_to_p(double x, int ball) {


        x_index[ball] = binSearch(xvals, x, x_index[ball]);


        return interp4(xvals, pvals, x, x_index[ball] - 1);
    }

    public double map_p_to_x(double p, int ball) {


        p = modp(p);
        p_index[ball] = binSearch(pvals, p, p_index[ball]);


        return interp4(pvals, xvals, p, p_index[ball] - 1);
    }

    public double map_p_to_y(double p, int ball) {


        p = modp(p);
        p_index[ball] = binSearch(pvals, p, p_index[ball]);


        return interp4(pvals, yvals, p, p_index[ball] - 1);
    }

    public double map_x_y_to_p(double x, double y) {


        double best_len = Double.MAX_VALUE;
        double p = -999999999;
        double len, xd, yd;

        for (int i = 0; i < DATA_POINTS; i++) {
            xd = x - xvals[i];
            yd = y - yvals[i];
            len = xd * xd + yd * yd;
            if (len < best_len) {
                best_len = len;
                p = pvals[i];
            }
        }
        return p;
    }

    public void closest_to_x_y(CPoint pt, double x, double y) {


        double best_len = Double.MAX_VALUE;
        double len, xd, yd;

        for (int i = 0; i < DATA_POINTS; i++) {
            xd = x - xvals[i];
            yd = y - yvals[i];
            len = xd * xd + yd * yd;
            if (len < best_len) {
                best_len = len;
                pt.x = xvals[i];
                pt.y = yvals[i];
                pt.p = pvals[i];
            }
        }

    }

    public void closest_slope(double x, double y, double p_guess, CPoint pt) {


        double len, xd, yd, dx, dy;
        p_guess = modp(p_guess);
        int i = binSearch(pvals, p_guess, p_index[pt.ball]);
        if (i < 0)
            i = 1;
        else if (i > DATA_POINTS - 1)
            i = DATA_POINTS - 2;
        else {
            xd = x - xvals[i];
            yd = y - yvals[i];
            double best_len = xd * xd + yd * yd;

            while (i < DATA_POINTS - 2) {
                xd = x - xvals[i + 1];
                yd = y - yvals[i + 1];
                len = xd * xd + yd * yd;
                if (len > best_len)
                    break;
                i++;
            }

            while (i > 1) {
                xd = x - xvals[i - 1];
                yd = y - yvals[i - 1];
                len = xd * xd + yd * yd;
                if (len > best_len)
                    break;
                i--;
            }
        }

        dx = xvals[i + 1] - xvals[i - 1];
        dy = yvals[i + 1] - yvals[i - 1];

        pt.slope = dy / dx;
        if (dx == 0)
            System.out.println("**** infinite slope ****");
        pt.p = pvals[i];
    }

    public void map_p_to_slope(CPoint pt) {


        int k;
        double dy, dx;


        pt.p = modp(pt.p);
        p_index[pt.ball] = binSearch(pvals, pt.p, p_index[pt.ball]);
        k = p_index[pt.ball];

        if (k < 0)
            k = 1;
        if (k >= DATA_POINTS - 1)
            k = DATA_POINTS - 2;


        pt.x = interp4(pvals, xvals, pt.p, k - 1);
        pt.y = interp4(pvals, yvals, pt.p, k - 1);
        if (xvals[k + 1] == xvals[k]) {


            pt.direction = (yvals[k + 1] > yvals[k]) ? 1 : -1;
            if (exact_slope)
                pt.slope = slope(pt.p);
            else
                pt.slope = Double.POSITIVE_INFINITY;
            pt.radius = Double.POSITIVE_INFINITY;
        } else {

            pt.direction = (xvals[k + 1] > xvals[k]) ? 1 : -1;


            if (exact_slope) {
                pt.slope = slope(pt.p);

            } else {

                dx = xvals[k + 1] - xvals[k];
                dy = yvals[k + 1] - yvals[k];
                pt.slope = dy / dx;
            }

            if (pt.radius_flag) {


                if ((k < 2) || (k > DATA_POINTS - 4))
                    pt.radius = Double.POSITIVE_INFINITY;
                else {


                    dx = xvals[k] - xvals[k - 2];
                    dy = yvals[k] - yvals[k - 2];
                    double b1 = dy / dx;
                    double p1 = pvals[k - 1];

                    dx = xvals[k + 3] - xvals[k + 1];
                    dy = yvals[k + 3] - yvals[k + 1];
                    double b2 = dy / dx;
                    double p2 = pvals[k + 2];
                    pt.radius = (p2 - p1) / (Math.atan(b2) - Math.atan(b1));


                }
            }
        }
    }

    public void find_intersect(C2Points pts, double ax, double ay, double qx, double qy) {


        boolean flip = false;
        if (qx < ax) {
            double h;
            h = ax;
            ax = qx;
            qx = h;
            h = ay;
            ay = qy;
            qy = h;
            flip = true;
        }


        x_index[pts.ball] = binSearch(xvals, ax, x_index[pts.ball]);
        int k = x_index[pts.ball];

        if (k < 0)
            k = 0;
        if (k >= DATA_POINTS)
            k = DATA_POINTS - 1;


        double slope2, x, y;
        if (ax == qx) {


            slope2 = (yvals[k + 1] - yvals[k]) / (xvals[k + 1] - xvals[k]);

            x = ax;
            y = yvals[k] + slope2 * (x - xvals[k]);
        } else {
            double slope1 = (qy - ay) / (qx - ax);

            y = interp4(xvals, yvals, ax, k - 1);


            if (y == ay) {
                x = ax;
                slope2 = (yvals[k] - yvals[k - 1]) / (xvals[k] - xvals[k - 1]);
                System.out.println("exact intersection");
            } else {
                double traj_y = ay;
                boolean below = (y < traj_y);
                boolean below2;

                int i = k;
                do {
                    i++;
                    if (i > DATA_POINTS - 1) {
                        i = DATA_POINTS - 1;
                        break;
                    }

                    if (xvals[i - 1] > qx) {
                        if (i == 1) {
                            i = 1;
                            break;
                        }
                        System.out.println("intersection trouble");
                        return;
                    }
                    traj_y = ay + slope1 * (xvals[i] - ax);
                    below2 = (yvals[i] < traj_y);
                } while (below2 == below);


                slope2 = (yvals[i] - yvals[i - 1]) / (xvals[i] - xvals[i - 1]);


                x = (-slope1 * ax + slope2 * xvals[i] + ay - yvals[i]) / (slope2 - slope1);


                y = ay + slope1 * (x - ax);

            }

        }

        pts.x1 = x;
        pts.y1 = y;

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


class CPath_Hump extends CPath {
    public CPath_Hump() {
        super();
    }

    protected void initialize() {
        tLo = -4.5;
        tHi = 4.5;
        left = tLo;
        right = tHi;
        top = 6;
        bottom = 0.5;
    }

    protected double x_func(double t) {

        return t;
    }

    protected double y_func(double x) {

        return 3 + x * x * (-7 + x * x) / 6;
    }

    protected double my_path_func(double x) {

        double d = x * (-14 + 4 * x * x) / 6;
        return Math.sqrt(1 + d * d);
    }

}


class CPath_Loop extends CPath {
    private static final double theta1 = 3.46334;
    private static final double theta2 = -0.321751;
    private static final double radius = 0.527046;
    private static final double ycenter = 2.41667;
    private static final double xcenter = 0;
    private static final double yoffset = 1;

    public CPath_Loop() {
        super();
    }

    protected void initialize() {
        tLo = -4;
        tHi = 8.5;
        left = -3;
        right = 3;
        top = 6;
        bottom = 0.5;
    }

    protected double x_func(double t) {
        if (t < 0.5)
            return t;
        else if (t < 0.5 + theta1 - theta2)
            return radius * Math.cos(t - 0.5 + theta2) + xcenter;
        else
            return t - theta1 + theta2 - 1;
    }

    protected double y_func(double t) {
        if (t < 0.5)
            return (t + 1) * (t + 1) + yoffset;
        else if (t < 0.5 + theta1 - theta2)
            return radius * Math.sin(t - 0.5 + theta2) + ycenter + yoffset;
        else {
            double dd = t - theta1 + theta2 - 2;
            return dd * dd + yoffset;
        }
    }

    protected double my_path_func(double t) {

        double dx, dy;
        if (t < 0.5) {
            dx = 1;
            dy = 2 * (t + 1);
        } else if (t < 0.5 + theta1 - theta2) {
            dx = -radius * Math.sin(t - 0.5 + theta2);
            dy = radius * Math.cos(t - 0.5 + theta2);
        } else {
            dx = 1;
            dy = 2 * (t - theta1 + theta2 - 2);
        }
        return Math.sqrt(dx * dx + dy * dy);
    }

}


class CPath_Oval extends CPath {
    private static final double s = 2;
    private static final double t0 = Math.PI / 2;
    private static final double t1 = Math.PI;
    private static final double t2 = t1 + s;
    private static final double t3 = t2 + Math.PI;
    private static final double t4 = t3 + s;
    private static final double t5 = t4 + Math.PI / 2;

    public CPath_Oval() {
        super();
    }

    protected void initialize() {
        tLo = t0;
        tHi = t5;
        closed = true;
        double b = 1.5;
        left = -b;
        right = b;
        top = b + s;
        bottom = -b;
    }

    protected double x_func(double t) {
        if (t < t1)
            return Math.cos(t);
        else if (t < t2)
            return -1;
        else if (t < t3)
            return Math.cos(Math.PI + t - t2);
        else if (t < t4)
            return 1;
        else if (t < t5)
            return Math.cos(t - t4);
        else
            return 0;
    }

    protected double y_func(double t) {
        if (t < t1)
            return s + Math.sin(t);
        else if (t < t2)
            return s - (t - t1);
        else if (t < t3)
            return Math.sin(Math.PI + t - t2);
        else if (t < t4)
            return t - t3;
        else if (t < t5)
            return s + Math.sin(t - t4);
        else
            return 0;
    }
}


class CPath_Circle extends CPath {
    final double radius = 1.5;
    final double edgeBuffer = 0.5;

    public CPath_Circle() {
        super();
    }

    protected void initialize() {
        tLo = -3 * Math.PI / 2;
        tHi = Math.PI / 2;
        closed = true;
        double b = radius + edgeBuffer;
        left = -b;
        right = b;
        top = b;
        bottom = -b;
        exact_slope = false;
    }

    protected double x_func(double t) {
        return radius * Math.cos(t);
    }

    protected double y_func(double t) {
        return radius * Math.sin(t);
    }


}


class CPath_Lemniscate extends CPath {
    private static final double a = 1.5;

    public CPath_Lemniscate() {
        super();
    }

    protected void initialize() {
        tLo = -Math.PI / 4;
        tHi = 3 * Math.PI / 4;
        closed = true;

        final double bx = 3;
        final double by = 1.5;
        left = -bx;
        right = bx;
        top = by;
        bottom = -by;
    }

    protected double x_func(double t) {
        if (t <= Math.PI / 4)
            return a * Math.sqrt(2 * Math.cos(2 * t)) * Math.cos(t);
        else if (t <= 3 * Math.PI / 4) {
            double T = -t + Math.PI / 2;
            return -a * Math.sqrt(2 * Math.cos(2 * T)) * Math.cos(T);
        } else
            return 0;
    }

    protected double y_func(double t) {
        if (t <= Math.PI / 4)
            return a * Math.sqrt(2 * Math.cos(2 * t)) * Math.sin(t);
        else if (t <= 3 * Math.PI / 4) {
            double T = -t + Math.PI / 2;
            return -a * Math.sqrt(2 * Math.cos(2 * T)) * Math.sin(T);
        } else
            return 0;
    }

    protected double my_path_func(double t) {


        throw new RuntimeException();

    }

}


class CPath_Cardioid extends CPath {
    private static final double a = 1.5;

    public CPath_Cardioid() {
        super();
    }

    protected void initialize() {
        tLo = 0;
        tHi = 2 * Math.PI;
        closed = true;

        final double bx = 2.8;
        final double by = 3;
        left = -bx;
        right = bx;
        top = 1.0;
        bottom = -3.5;
    }

    protected double x_func(double t) {
        double c = Math.cos(t);
        return a * Math.sin(t) * (1 + c);
    }

    protected double y_func(double t) {
        double c = Math.cos(t);
        return -a * c * (1 + c);
    }

}


class CPath_Flat extends CPath {

    public CPath_Flat() {
        super();
    }

    protected void initialize() {
        tLo = -8;
        tHi = 8;
        left = -3;
        right = 3;
        top = 6;
        bottom = 0.5;
    }

    protected double x_func(double t) {
        return t;
    }

    protected double y_func(double t) {
        return 1;
    }

    protected double my_path_func(double t) {

        return 1;
    }

}


class CPath_Spiral extends CPath {
    private static final double arc1x = -2.50287;
    private static final double arc1y = 5.67378;
    private static final double rad = 1;
    private static final double slo = 4.91318;
    private static final double slox = 0.122489;
    private static final double sloy = -0.601809;
    private static final double shi = 25.9566;
    private static final double shix = 2.20424;
    private static final double shiy = 2.38089;
    private static final double arc2y = sloy + rad;
    private static final double arc1rx = arc1x + Math.cos(Math.PI / 4);
    private static final double t1 = Math.PI / 2;
    private static final double t2 = t1 + arc1y - arc2y;
    private static final double t3 = t2 + Math.PI / 2;
    private static final double t4 = t3 + slox - arc1x;
    private static final double t5 = t4 + shi - slo;
    private static final double t6 = t5 + Math.sqrt(2) * (shix - arc1rx);
    private static final double t7 = t6 + Math.PI / 4;

    public CPath_Spiral() {
        super();
    }

    protected void initialize() {
        tLo = 0;
        tHi = t7;
        closed = true;
        left = -4;
        right = 4;
        top = 7;
        bottom = -4;
    }

    protected double x_func(double t) {
        if (t < t1)
            return Math.cos(t + Math.PI / 2) + arc1x;
        else if (t < t2)
            return arc1x - rad;
        else if (t < t3)
            return Math.cos(t - t2 + Math.PI) + arc1x;
        else if (t < t4)
            return arc1x + (t - t3);
        else if (t < t5)
            return ((t - t4 + slo) / 8) * Math.cos(t - t4 + slo);
        else if (t < t6)
            return shix - (t - t5) / Math.sqrt(2);
        else if (t < t7)
            return arc1x + Math.cos(Math.PI / 4 + t - t6);
        else
            return 0;
    }

    protected double y_func(double t) {
        if (t < t1)
            return Math.sin(t + Math.PI / 2) + arc1y;
        else if (t < t2)
            return arc1y - (t - t1);
        else if (t < t3)
            return Math.sin(t - t2 + Math.PI) + arc2y;
        else if (t < t4)
            return sloy;
        else if (t < t5)
            return ((t - t4 + slo) / 8) * Math.sin(t - t4 + slo);
        else if (t < t6)
            return shiy + (t - t5) / Math.sqrt(2);
        else if (t < t7)
            return arc1y + Math.sin(Math.PI / 4 + t - t6);
        else
            return 0;
    }
}

