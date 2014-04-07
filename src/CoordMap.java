import java.awt.*;


final class DoubleRect {
    private final double xMin, xMax, yMin, yMax;

    public DoubleRect(double xMin, double yMin, double xMax, double yMax) {
        if (xMin > xMax)
            throw new IllegalArgumentException("xMin=" + xMin + " must be less than xMax=" + xMax);
        if (yMin > yMax)
            throw new IllegalArgumentException("yMin=" + yMin + " must be less than yMax=" + yMax);
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public String toString() {
        return "DoubleRect xMin=" + xMin + " xMax=" + xMax + " yMin=" + yMin + " yMax=" + yMax;
    }

    public double getXMin() {
        return xMin;
    }

    public double getXMax() {
        return xMax;
    }

    public double getYMin() {
        return yMin;
    }

    public double getYMax() {
        return yMax;
    }

    public double getWidth() {
        return xMax - xMin;
    }

    public double getHeight() {
        return yMax - yMin;
    }
}


class ConvertMap {
    private CoordMap map;

    public ConvertMap(CoordMap map) {
        this.map = map;
    }

    public int simToScreenScaleX(double x) {

        return map.simToScreenScaleX(x);
    }

    public int simToScreenScaleY(double y) {

        return map.simToScreenScaleY(y);
    }

    public int simToScreenX(double x) {

        return map.simToScreenX(x);
    }

    public int simToScreenY(double y) {

        return map.simToScreenY(y);
    }

    public double screenToSimX(int scr_x) {

        return map.screenToSimX(scr_x);
    }

    public double screenToSimY(int scr_y) {

        return map.screenToSimY(scr_y);
    }

    public DoubleRect getSimBounds() {
        return map.getSimBounds();
    }

    public Rectangle getScreenRect() {
        return map.getScreenRect();
    }
}

public class CoordMap {

    public static final int ALIGN_MIDDLE = 0;
    public static final int ALIGN_LEFT = 1;
    private int align_x = ALIGN_LEFT;
    public static final int ALIGN_RIGHT = 2;
    public static final int ALIGN_UPPER = 3;
    private int align_y = ALIGN_UPPER;
    public static final int ALIGN_LOWER = 4;
    public static final int INCREASE_UP = -1;
    public static final int INCREASE_DOWN = 1;
    private int y_direction = INCREASE_DOWN;
    private int origin_x = 0;
    private int origin_y = 0;
    private int screen_left = 0;
    private int screen_top = 0;
    private int screen_width = 0;
    private int screen_height = 0;
    private double pixel_per_unit_x = 100;
    private double pixel_per_unit_y = 100;
    private boolean fill_screen = false;
    private boolean zoomMode = false;
    private boolean originFixed = false;
    private boolean scaleFixed = false;
    private double zx1, zx2, zy1, zy2;
    private double simMinX = -10;
    private double simMaxX = 10;
    private double simMinY = -10;
    private double simMaxY = 10;
    private ConvertMap convertMap = new ConvertMap(this);

    public CoordMap() {
        this(INCREASE_UP, -10.0, 10.0, -10.0, 10.0, ALIGN_MIDDLE, ALIGN_MIDDLE);
    }

    public CoordMap(int y_dir, double x1, double x2, double y1, double y2,
                    int align_x, int align_y) {
        y_direction = y_dir;
        this.align_x = align_x;
        this.align_y = align_y;
        setRange(x1, x2, y1, y2);
    }


    public CoordMap(int y_dir, double x1, double x2, double y1, double y2,
                    int originX, int originY, double scaleX, double scaleY) {
        y_direction = y_dir;
        originFixed = true;
        origin_x = originX;
        origin_y = originY;
        scaleFixed = true;
        pixel_per_unit_x = scaleX;
        pixel_per_unit_y = scaleY;
        setRange(x1, x2, y1, y2);
    }

    public ConvertMap getConvertMap() {
        return convertMap;
    }

    public String toString() {
        String s = "CoordMap with [";
        s += scaleFixed ? " scaleFixed, " : "";
        s += originFixed ? " originFixed, " : "";
        s += fill_screen ? " fill_screen, " : "";
        s += ", y_direction=" + (y_direction == INCREASE_UP ? "UP" : "DOWN");
        s += ", SCREEN (left=" + screen_left;
        s += ", top=" + screen_top;
        s += ", width=" + screen_width;
        s += ", height=" + screen_height + ")";
        s += ", SIM (x1=" + simMinX + ",x2=" + simMaxX + ",y1=" + simMinY + ",y2=" + simMaxY + ")";
        s += ", scale(x=" + pixel_per_unit_x + ", y=" + pixel_per_unit_y + ")";
        s += ", origin(x=" + origin_x + ", y=" + origin_y + ")";
        s += "]";
        return s;
    }


    private void recalc() {
        double sim_width = simMaxX - simMinX;
        double sim_height = simMaxY - simMinY;
        if (zoomMode) {
            if (!scaleFixed) {
                pixel_per_unit_x = (double) screen_width / (zx2 - zx1);
                pixel_per_unit_y = (double) screen_height / (zy2 - zy1);
            }
            if (!originFixed) {
                origin_x = screen_left - (int) (zx1 * pixel_per_unit_x + 0.4999);
                if (y_direction == INCREASE_DOWN)
                    origin_y = screen_top - (int) (zy1 * pixel_per_unit_y + 0.4999);
                else
                    origin_y = screen_top + screen_height + (int) (zy1 * pixel_per_unit_y + 0.4999);
            }
        } else if (fill_screen) {

            if (!scaleFixed) {
                pixel_per_unit_x = (double) screen_width / sim_width;
                pixel_per_unit_y = (double) screen_height / sim_height;
            }
            if (!originFixed) {
                origin_x = screen_left - (int) (simMinX * pixel_per_unit_x + 0.4999);
                if (y_direction == INCREASE_DOWN)
                    origin_y = screen_top - (int) (simMinY * pixel_per_unit_y + 0.4999);
                else
                    origin_y = screen_top + screen_height + (int) (simMinY * pixel_per_unit_y + 0.4999);
            }
        } else {
            if (sim_width <= 0 || sim_height <= 0) {
                System.out.println("WARNING: Coordmap cannot recalc, found zero sim width "
                        + sim_width + " or height " + sim_height);
                return;
            }

            if (screen_width <= 0 || screen_height <= 0)
                return;


            int ideal_height = (int) ((double) screen_width * sim_height / sim_width);
            int ideal_width;
            int offset_x, offset_y;

            if (screen_height < ideal_height) {
                if (!scaleFixed)
                    pixel_per_unit_y = pixel_per_unit_x = (double) screen_height / sim_height;
                offset_y = 0;
                ideal_width = (int) (sim_width * pixel_per_unit_x);
                switch (align_x) {
                    case ALIGN_LEFT:
                        offset_x = 0;
                        break;
                    case ALIGN_RIGHT:
                        offset_x = screen_width - ideal_width;
                        break;
                    case ALIGN_MIDDLE:
                        offset_x = (screen_width - ideal_width) / 2;
                        break;
                    default:
                        offset_x = 0;
                        break;
                }
            } else {
                pixel_per_unit_y = pixel_per_unit_x = (double) screen_width / sim_width;
                offset_x = 0;
                ideal_height = (int) (sim_height * pixel_per_unit_y);
                switch (align_y) {
                    case ALIGN_UPPER:
                        offset_y = 0;
                        break;
                    case ALIGN_MIDDLE:
                        offset_y = (screen_height - ideal_height) / 2;
                        break;
                    case ALIGN_LOWER:
                        offset_y = screen_height - ideal_height;
                        break;
                    default:
                        offset_y = 0;
                        break;
                }
            }
            if (!originFixed) {
                origin_x = screen_left + offset_x - (int) (simMinX * pixel_per_unit_x);
                if (y_direction == INCREASE_DOWN)
                    origin_y = screen_top + offset_y - (int) (simMinY * pixel_per_unit_y);
                else
                    origin_y = screen_top + screen_height - offset_y + (int) (simMinY * pixel_per_unit_y);
            }
        }
    }

    public void setAlignment(int align_x, int align_y) {
        this.align_x = align_x;
        this.align_y = align_y;
        originFixed = false;
        recalc();
    }

    public void setOrigin(int x, int y) {
        origin_x = x;
        origin_y = y;
        originFixed = true;
    }

    public void setScale(double x, double y) {
        pixel_per_unit_x = x;
        pixel_per_unit_y = y;
        scaleFixed = true;
    }

    public void zoom(double x1, double x2, double y1, double y2) {
        if (y2 <= y1)
            throw new IllegalArgumentException("CoordMap:zoom() y1=" + y1 + " must be less than y2=" + y2);
        if (x2 <= x1)
            throw new IllegalArgumentException("CoordMap:zoom() x1=" + x1 + " must be less than x2=" + x2);
        zoomMode = true;
        originFixed = scaleFixed = false;
        zx1 = x1;
        zx2 = x2;
        zy1 = y1;
        zy2 = y2;
        recalc();
    }

    public void setZoom(boolean state) {
        zoomMode = state;
        recalc();
    }


    public void setFillScreen(boolean f) {
        fill_screen = f;
        scaleFixed = false;
        recalc();
    }


    public boolean expand() {
        if (screen_width > 0 && screen_height > 0) {
            simMinX = screenToSimX(screen_left);
            simMaxX = screenToSimX(screen_left + screen_width);
            simMinY = screenToSimY(screen_top);
            simMaxY = screenToSimY(screen_top + screen_height);
            if (simMinY > simMaxY) {
                double d = simMinY;
                simMinY = simMaxY;
                simMaxY = d;
            }
            scaleFixed = false;
            recalc();
            return true;
        } else {

            return false;
        }
    }

    public void setRange(double xlo, double xhi, double ylo, double yhi) {

        simMinX = xlo;
        simMaxX = xhi;
        simMinY = ylo;
        simMaxY = yhi;
        recalc();
    }

    public void setScreen(int left, int top, int width, int height) {

        if ((width > 0) && (height > 0)) {
            screen_top = top;
            screen_left = left;
            screen_width = width;
            screen_height = height;
            recalc();
        }
    }

    public int simToScreenScaleX(double x) {

        return (int) (x * pixel_per_unit_x + 0.5);
    }

    public int simToScreenScaleY(double y) {

        return (int) (y * pixel_per_unit_y + 0.5);
    }

    public int simToScreenX(double x) {

        return origin_x + (int) (x * pixel_per_unit_x + 0.5);
    }

    public int simToScreenY(double y) {

        return origin_y + y_direction * (int) (y * pixel_per_unit_y + 0.5);
    }

    public double screenToSimX(int scr_x) {

        return (double) (scr_x - origin_x) / pixel_per_unit_x;
    }

    public double screenToSimY(int scr_y) {

        return y_direction * (double) (scr_y - origin_y) / pixel_per_unit_y;
    }

    public DoubleRect getSimBounds() {
        return new DoubleRect(simMinX, simMinY, simMaxX, simMaxY);
    }


    public Rectangle getScreenRect() {
        double top = (y_direction == INCREASE_UP) ? simMaxY : simMinY;

        return new Rectangle(simToScreenX(simMinX), simToScreenY(top),
                simToScreenScaleX(simMaxX - simMinX),
                simToScreenScaleY(simMaxY - simMinY));
    }

    public double getMinX() {
        return simMinX;
    }

    public double getMaxX() {
        return simMaxX;
    }

    public double getMinY() {
        return simMinY;
    }

    public double getMaxY() {
        return simMaxY;
    }

    public boolean intersectRect(Rectangle r) {
        int x1, y1, x2, y2;
        x1 = r.x;
        y1 = r.y;
        x2 = x1 + r.width;
        y2 = y1 + r.height;
        int sx1 = screen_left;
        int sy1 = screen_top;
        int sx2 = screen_left + screen_width;
        int sy2 = screen_top + screen_height;
        if (sx1 >= x2)
            return false;
        if (x1 >= sx2)
            return false;
        if (sy1 >= y2)
            return false;
        if (y1 >= sy2)
            return false;
        return true;
    }
}

