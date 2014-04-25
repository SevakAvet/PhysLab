public interface DiffEq {
    public double[] getVars();
    public void evaluate(double[] x, double[] change);
    public boolean[] getCalc();
}
