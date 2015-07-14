package net.bennokue.java.osmosis;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

/**
 * This class implements the task to import data from a CSV file and match it to
 * the OSM data. The plugin iterates over the nodes that are read eg. by
 * {@code --read-xml} and over a csv file that contains node ids with
 * latitude/longitude and any kind of data and merges the data into a tag of the
 * corresponding OSM nodes.
 * <p>
 * There are the following parameters (named like the CLI parameters
 * here):</p><ul><li>{@code idPos}: The position of the OSM id in each line of
 * the csv file (all {@code ~pos} values start counting at {@code 1}, so
 * {@code 0} is not a valid argument).</li><li>{@code latPos}: Optional
 * argument. The CSV line position of the nodes'
 * latitude.</li><li>{@code lonPos}: Optional argument. The CSV line position of
 * the nodes' longitude.</li><li>{@code tagDataPos}: The CSV line position of
 * the data that should be imported into a node tag.</li><li>{@code outputTag}:
 * The name of that tag.</li><li>{@code maxDist}: Optional argument, only
 * working whith {@code latPos} and {@code lonPos}: If given, there will be some
 * action if the position of the CSV node and the OSM node differ more than
 * {@code maxDist} meters. <em>Defaults to
 * {@code POSITIVE_INFINITY}</em>.</li><li>{@code maxDistAction}: The action
 * that should be taken if a distance exceeds {@code maxDist}. See
 * {@link MaxDistAction}.<em>Defaults to
 * {@link MaxDistAction#WARN}</em>.</li><li>{@code inputCSV}: The path to the
 * CSV file to import.</li><li>{@code csvCacheSize}: The size of the CSV lines
 * cache.</li></ul>Note: Empty lines and lines starting with a semicolon will be
 * ignored.<p>
 * If you want to use the plugin as a lib, you might also be interested at the
 * test classes.</p>
 *
 * @author bennokue
 */
public class CSVImportPlugin_task implements SinkSource, EntityProcessor {

    /**
     * What should be done if the position of a OSM node and the position of the
     * node at the matching line in the CSV files differ more then
     * {@code maxDist}?
     *
     * @see CSVImportPlugin_task
     */
    public static enum MaxDistAction {

        /**
         * Warn only.
         */
        WARN,
        /**
         * Warn and do <strong>not</strong> import this value.
         */
        DELETE,
        /**
         * Like DELETE, but also dump the lines into a logfile.
         */
        LOG
    }

    private static final Logger logger = Logger.getLogger(CSVImportPlugin_task.class.getName());
    /**
     * The next stage of the OSMOSIS pipeline.
     */
    private Sink sink;
    /**
     * The name of the tag where the output value will be stored at.
     */
    private final String outputTag;

    /**
     * CSV line position of the OSM id (first field = {@code 0}).
     */
    private final int osmIdCSVPosition;
    /**
     * CSV line position of the OSM latitude (first field = {@code 0}).
     */
    private final int osmLatitudeCSVPosition;
    /**
     * CSV line position of the OSM longitude (first field = {@code 0}).
     */
    private final int osmLongitudeCSVPosition;
    /**
     * CSV line position of the data to import (first field = {@code 0}).
     */
    private final int tagDataCSVPosition;
    /**
     * See {@link CSVImportPlugin_task}.
     */
    private final double maxNodeDistance;
    /**
     * See {@link CSVImportPlugin_task}.
     */
    private final MaxDistAction maxDistAction;
    /**
     * THe CSV input file.
     */
    private final File inputCSV;
    /**
     * Our cache csv loader.
     */
    private CSVLoader csvLoader;
    /**
     * The writer of the logfile (used in mode {@link MaxDistAction#LOG}).
     */
    private PrintWriter logWriter;
    /**
     * The path to the logfile.
     */
    private String logfilePath;
    /**
     * Statistics.
     */
    private int numberOfNodesProcessed = 0, numberOfNodesImportedSuccessfully = 0;

    /**
     * Standard constructor with some sanity checks.
     *
     * @param inputCSV The input CSV file.
     * @param osmIdPos The CSV line position of the OSM id (first field =
     * {@code 0}).
     * @param osmLatPos The CSV line position of the OSM latitude (first field =
     * {@code 0}).
     * @param osmLonPos The CSV line position of the OSM longitude (first field
     * = {@code 0}).
     * @param dataPos The CSV line position of the data to be imported (first
     * field = {@code 0}).
     * @param outputTagName The name of the output tag.
     * @param maxDist See {@link CSVImportPlugin_task}.
     * @param maxDistAction See {@link CSVImportPlugin_task}.
     * @param csvCacheSize The line reading cache size, see
     * {@link CSVLoader#CSVLoader(java.io.File, int, int, int, int, int)}.
     */
    public CSVImportPlugin_task(String inputCSV, int osmIdPos, int osmLatPos, int osmLonPos, int dataPos, String outputTagName, double maxDist, MaxDistAction maxDistAction, int csvCacheSize) {
        if (inputCSV.equals("")) {
            throw new IllegalArgumentException("You have to provide an input file!");
        }
        this.inputCSV = new File(inputCSV);
        this.osmIdCSVPosition = osmIdPos;
        this.osmLatitudeCSVPosition = osmLatPos;
        this.osmLongitudeCSVPosition = osmLonPos;
        this.tagDataCSVPosition = dataPos;
        this.outputTag = outputTagName;
        this.maxNodeDistance = maxDist;
        this.maxDistAction = maxDistAction;

        // Sanity checks
        if (!this.inputCSV.isFile() || !this.inputCSV.canRead()) {
            throw new IllegalArgumentException(this.inputCSV.getPath() + " is not a file or not readable!");
        }

        if (this.osmIdCSVPosition <= 0) {
            throw new IllegalArgumentException("Please provide an idPos greater than 0");
        }
        if (this.tagDataCSVPosition <= 0) {
            throw new IllegalArgumentException("Please provide a tagDataPos greater than 0");
        }
        if (this.outputTag.equals("")) {
            throw new IllegalArgumentException("Please provide an outputTag");
        }

        if (this.maxNodeDistance < Double.POSITIVE_INFINITY && (this.osmLatitudeCSVPosition < 0 || this.osmLongitudeCSVPosition < 0)) {
            throw new IllegalArgumentException("Provide latPos and lonPos when using maxDist");
        }

        try {
            this.csvLoader = new CSVLoader(this.inputCSV, csvCacheSize, osmIdPos, osmLatPos, osmLonPos, dataPos);
            if (this.maxDistAction == MaxDistAction.LOG) {
                File logFileFile = new File(this.inputCSV.getParent(), stripExtension(this.inputCSV.getName()) + "-dirtyNodes.csv");
                this.logWriter = new PrintWriter(logFileFile);
                this.logfilePath = logFileFile.getPath();
                this.initLogfile();
            }
        } catch (FileNotFoundException ex) {
            this.csvLoader = null;
            logger.log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    /**
     * Prints statistics with sysout.
     */
    private void printStatistics() {
        System.out.println("CSV import finished. Processed nodes: " + this.numberOfNodesProcessed + "; Successful imorts: " + this.numberOfNodesImportedSuccessfully + "; Errors: " + (this.numberOfNodesProcessed - this.numberOfNodesImportedSuccessfully));
        if (null != this.logWriter) {
            System.out.println("Log file written to: " + this.logfilePath);
        }
    }

    @Override
    public void process(EntityContainer entityContainer) {
        entityContainer.process(this);
    }

    @Override
    public void process(BoundContainer boundContainer) {
        sink.process(boundContainer);
    }

    @Override
    public void process(NodeContainer container) {
        // Backup existing node entity
        Node node = container.getEntity();
        // Backup id, lat and lon of node entity
        long osmId = node.getId();
        double lat = node.getLatitude();
        double lon = node.getLongitude();

        // Get all the tags from the node
        Collection<Tag> nodeTags = node.getTags();
        /*
         * Remove the output attribute.
         */
        for (Tag tag : nodeTags) {
            if (tag.getKey().equalsIgnoreCase(this.outputTag)) {
                nodeTags.remove(tag);
                break;
            }
        }

        // Get the output value 
        String outputTagValue = this.getNodeTagValue(osmId, lat, lon);

        // Add new output tag if it is there
        if (null != outputTagValue && !outputTagValue.equals("")) {
            nodeTags.add(new Tag(this.outputTag, outputTagValue));
            this.numberOfNodesImportedSuccessfully++;
        }

        // Create new node entity with adjusted attributes
        CommonEntityData ced = new CommonEntityData(
                node.getId(),
                node.getVersion(),
                node.getTimestamp(),
                node.getUser(),
                node.getChangesetId(),
                nodeTags);

        this.numberOfNodesProcessed++;

        // Distribute the new nodecontainer to the following sink
        sink.process(new NodeContainer(new Node(ced, lat, lon)));
    }

    /**
     * Look for the current node at the cache and check the distance if found.
     *
     * @param osmId The OSM node ID.
     * @param lat The OSM node latitude.
     * @param lon The OSM node longitude.
     * @return The value to be imported or {@code null} if there is no such
     * element at the CSV or the distance is larger than
     * {@link #maxNodeDistance} and we are in {@link MaxDistAction#DELETE} mode.
     */
    private String getNodeTagValue(long osmId, double lat, double lon) {
        // Look for the item
        CSVItem item = null;
        try {
            item = this.csvLoader.findItem(osmId);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        if (null == item) {
            return "";
        }
        // Check the distance
        double distance = item.getDistance(lat, lon);
        if (distance > this.maxNodeDistance) {
            if (this.maxDistAction == MaxDistAction.DELETE) {
                logger.log(Level.WARNING, "Node {0} has distance {1} to the point where it should be! We do not import it.", new Object[]{osmId, distance});
                return "";
            } else if (this.maxDistAction == MaxDistAction.WARN) {
                logger.log(Level.WARNING, "Node {0} has distance {1} to the point where it should be!", new Object[]{osmId, distance});
                return item.DATA;
            } else if (this.maxDistAction == MaxDistAction.LOG) {
                logger.log(Level.WARNING, "Node {0} has distance {1} to the point where it should be! We do not import it.", new Object[]{osmId, distance});
                this.logWriter.print(osmId);
                this.logWriter.print(",");
                this.logWriter.print(lat);
                this.logWriter.print(",");
                this.logWriter.print(lon);
                this.logWriter.print(",");
                this.logWriter.print(item.OSM_LAT);
                this.logWriter.print(",");
                this.logWriter.print(item.OSM_LON);
                this.logWriter.print(",");
                this.logWriter.print(item.DATA);
                this.logWriter.print(",");
                this.logWriter.println(distance);
                return "";
            } else {
                throw new IllegalArgumentException("Unknown action: " + this.maxDistAction.toString());
            }
        }
        return item.DATA;
    }

    /**
     * Prints header comments to the log file.
     * @throws NullPointerException If there is no log file.
     */
    private void initLogfile() {
        this.logWriter.println("; Input file: " + this.inputCSV.getPath());
        this.logWriter.println("; osmId,lat,lon,csvLat,csvLon,csvData,deviation");
    }

    /**
     * Closes the log file if needed.
     */
    private void finishLogfile() {
        if (null != this.logWriter) {
            this.logWriter.close();
        }
    }

    @Override
    public void process(WayContainer container) {
        sink.process(container);
    }

    @Override
    public void process(RelationContainer container) {
        sink.process(container);
    }

    @Override
    public void complete() {
        this.printStatistics();
        this.finishLogfile();
        sink.complete();
    }

    @Override
    public void release() {
        sink.release();
    }

    @Override
    public void setSink(Sink sink) {
        this.sink = sink;
    }

    @Override
    public void initialize(Map<String, Object> metaData) {
        // added in osmosis 0.41
    }

    /**
     * Remove the extension from a filename.
     *
     * @param filename The filename.
     * @return The filename without its extension.
     */
    private static String stripExtension(String filename) {
        int indexOfDot = filename.lastIndexOf(".");
        if (indexOfDot > 0) {
            return filename.substring(0, indexOfDot);
        }
        return filename;
    }
}
