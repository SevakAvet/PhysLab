public class RungeKutta implements DiffEqSolver {
    DiffEq ode;
    double[] inp, k1, k2, k3, k4;

    public RungeKutta(DiffEq ode) {
        this.ode = ode;
    }

    public void step(double stepSize) {
        double[] vars = ode.getVars();
        int N = vars.length;
        if ((inp == null) || (inp.length != N)) {
            inp = new double[N];
            k1 = new double[N];
            k2 = new double[N];
            k3 = new double[N];
            k4 = new double[N];
        }
        int i;
        ode.evaluate(vars, k1);
        for (i = 0; i < N; i++)
            inp[i] = vars[i] + k1[i] * stepSize / 2;
        ode.evaluate(inp, k2);
        for (i = 0; i < N; i++)
            inp[i] = vars[i] + k2[i] * stepSize / 2;
        ode.evaluate(inp, k3);
        for (i = 0; i < N; i++)
            inp[i] = vars[i] + k3[i] * stepSize;
        ode.evaluate(inp, k4);

        boolean[] calc = ode.getCalc();

        for (i = 0; i < N; i++)
            if (calc[i])
                vars[i] = vars[i] + (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]) * stepSize / 6;

    }
}
