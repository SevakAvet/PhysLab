public interface Dragable extends Drawable {
    public boolean isDragable();
    public double distanceSquared(double x, double y);
    public double getX();
    public double getY();
}
