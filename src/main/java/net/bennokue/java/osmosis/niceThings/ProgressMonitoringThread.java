package net.bennokue.java.osmosis.niceThings;

/**
 * A simple Thread that displays status information from a running OSMOSIS task.
 * @author bennokue
 */
public class ProgressMonitoringThread extends Thread {

    /**
     * How long should we wait after each message?
     */
    private final int delaySeconds;
    /**
     * The task to monitor.
     */
    private final ProgressTellingOsmosisTask theTask;
    /**
     * We stop working if this gets {@code true}.
     */
    private boolean isFinished = false;

    /**
     * Build the MonitoringTask. Does not start it.
     * @param task The task to monitor.
     * @param delaySeconds The pause between the messages.
     */
    public ProgressMonitoringThread(ProgressTellingOsmosisTask task, int delaySeconds) {
        super();
        this.delaySeconds = delaySeconds;
        this.theTask = task;
    }

    /**
     * Tells the Thread to stop running.
     */
    public void taskFinished() {
        this.isFinished = true;
        this.interrupt();
    }

    @Override
    public void run() {
        System.out.println("Progress Monitoring of " + this.theTask.getTaskDescription() + " started.");
        while (!this.isInterrupted() && !this.isFinished) {
            try {
                Thread.sleep(delaySeconds * 1000);
            } catch (InterruptedException ex) {
                return;
            }
            System.out.println(this.theTask.getProgressMessage());
        }
        System.out.println("Progress Monitoring of " + this.theTask.getTaskDescription() + " finished.");
    }
}
