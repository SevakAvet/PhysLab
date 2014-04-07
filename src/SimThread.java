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
            while (!interrupted()) { // loop until interrupted
                checkSuspended();
                sim.run();
                sleep(delay);  // milliseconds
            }
        } catch (InterruptedException e) {
            Utility.println("SimThread thread interrupted.");
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

