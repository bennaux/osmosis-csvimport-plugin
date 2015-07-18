package net.bennokue.java.osmosis.niceThings;

/**
 * Implementing this interface, an OSMOSIS task will be able to get monitored by
 * {@link ProgressMonitoringThread}, which makes it more usable.
 *
 * @author bennokue
 */
public interface ProgressTellingOsmosisTask {

    /**
     * Give a message a user can get information from. E.g. &quot;Current node: 593898".
     * @return 
     */
    public String getProgressMessage();

    /**
     * A short description of the task that is taking place. Or just its name.
     * @return 
     */
    public String getTaskDescription();
}
