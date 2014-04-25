public class SimThread extends Thread {
    private Runnable sim;
    private long delay;

    SimThread(Runnable sim, long delay) {
        super("SimThread");
        this.sim = sim;
        this.delay = delay;
    }

    public void run() {
        try {
            while (!interrupted()) {
                checkSuspended();
                sim.run();
                sleep(delay);
            }
        } catch (InterruptedException e) {
        }
    }

    private synchronized void checkSuspended() throws InterruptedException {
    }
}

