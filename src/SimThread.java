public class SimThread extends Thread {
    private Runnable sim;
    private long delay;
    private boolean suspendRequested = false;

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

    public void requestSuspend() {
        suspendRequested = true;
    }

    private synchronized void checkSuspended() throws InterruptedException {
        while (suspendRequested)
            wait();
    }

    public synchronized void requestResume() {
        suspendRequested = false;
        notifyAll();
    }
}

