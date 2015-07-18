package net.bennokue.java.osmosis;

import java.util.logging.Logger;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkSourceManager;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

/**
 * Factory class for CSV import plugin.
 *
 * @author bennokue
 */
public class CSVImportPlugin_factory extends TaskManagerFactory {

    private static final Logger log = Logger.getLogger(CSVImportPlugin_factory.class.getName());
    /*
     * In the following, the CLI argument names and their default parameters are
     * defined. ARG_ variables store the argument name, DEFAULT_ variables the
     * default value.
     */
    private static final String ARG_OUTPUT_TAG = "outputTag";
    private static final String DEFAULT_OUTPUT_TAG = "";
    private static final String ARG_ID_POSITION = "idPos";
    private static final int DEFAULT_ID_POSITION = -1;
    private static final String ARG_LATITUDE_POSITION = "latPos";
    private static final int DEFAULT_LATITUDE_POSITION = -1;
    private static final String ARG_LONGITUDE_POSITION = "lonPos";
    private static final int DEFAULT_LONGITUDE_POSITION = -1;
    private static final String ARG_DATA_POSITION = "tagDataPos";
    private static final int DEFAULT_DATA_POSITION = -1;
    private static final String ARG_MAXDIST_VALUE = "maxDist";
    private static final double DEFAULT_MAXDIST_VALUE = Double.POSITIVE_INFINITY;
    private static final String ARG_MAXDIST_ACTION = "maxDistAction";
    private static final String DEFAULT_MAXDIST_ACTION = CSVImportPlugin_task.MaxDistAction.WARN.toString();
    private static final String ARG_INPUT_CSV = "inputCSV";
    private static final String DEFAULT_INPUT_CSV = "";
    private static final String ARG_CSV_CACHE_SIZE = "csvCacheSize";
    private static final int DEFAULT_CSV_CACHE_SIZE = -1;
    private static final String ARG_PROGRESS_INFO_INTERVAL = "progressInfoIntervalSecs";
    private static final int DEFAULT_PROGRESS_INFO_INTERVAL = -1;

    @Override
    protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
        // Get command line arguments
        String outputTag = getStringArgument(taskConfig, ARG_OUTPUT_TAG, DEFAULT_OUTPUT_TAG);
        int idPosition = getIntegerArgument(taskConfig, ARG_ID_POSITION, DEFAULT_ID_POSITION);
        int latPosition = getIntegerArgument(taskConfig, ARG_LATITUDE_POSITION, DEFAULT_LATITUDE_POSITION);
        int lonPosition = getIntegerArgument(taskConfig, ARG_LONGITUDE_POSITION, DEFAULT_LONGITUDE_POSITION);
        int dataPosition = getIntegerArgument(taskConfig, ARG_DATA_POSITION, DEFAULT_DATA_POSITION);
        double maxDist = getDoubleArgument(taskConfig, ARG_MAXDIST_VALUE, DEFAULT_MAXDIST_VALUE);
        CSVImportPlugin_task.MaxDistAction maxDistAction = CSVImportPlugin_task.MaxDistAction.valueOf(getStringArgument(taskConfig, ARG_MAXDIST_ACTION, DEFAULT_MAXDIST_ACTION).toUpperCase());
        String inputCSV = getStringArgument(taskConfig, ARG_INPUT_CSV, DEFAULT_INPUT_CSV);
        int csvCacheSize = getIntegerArgument(taskConfig, ARG_CSV_CACHE_SIZE, DEFAULT_CSV_CACHE_SIZE);
        int progressInfoIntervalSecs = getIntegerArgument(taskConfig, ARG_PROGRESS_INFO_INTERVAL, DEFAULT_PROGRESS_INFO_INTERVAL);

        // Create the task
        SinkSource task = new CSVImportPlugin_task(inputCSV, idPosition, latPosition, lonPosition, dataPosition, outputTag, maxDist, maxDistAction, csvCacheSize, progressInfoIntervalSecs);
        return new SinkSourceManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
    }
}
