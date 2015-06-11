/*
 * TODO Benno javadoc
 */
package net.bennokue.java.osmosis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.nashorn.internal.codegen.CompilerConstants;
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
 *
 * @author bennokue
 */
public class CSVImportPlugin_task implements SinkSource, EntityProcessor {

    public static enum MaxDistAction {

        WARN,
        DELETE
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

    private final int osmIdCSVPosition;
    private final int osmLatitudeCSVPosition;
    private final int osmLongitudeCSVPosition;
    private final int tagDataCSVPosition;
    private final double maxNodeDistance;
    private final MaxDistAction maxDistAction;
    private final File inputCSV;
    private CSVLoader csvLoader;

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
        
        if (this.maxNodeDistance < Double.POSITIVE_INFINITY && (this.osmLatitudeCSVPosition < 0 || this.osmLongitudeCSVPosition <0)) {
            throw new IllegalArgumentException("Provide latPos and lonPos when using maxDist");
        }
        
        try {
            this.csvLoader = new CSVLoader(this.inputCSV, csvCacheSize, osmIdPos, osmLatPos, osmLonPos, dataPos);
        } catch (FileNotFoundException ex) {
            this.csvLoader = null;
            logger.log(Level.SEVERE, null, ex);
            System.exit(1);
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

        // Add new output tag
        nodeTags.add(new Tag(this.outputTag, outputTagValue));

        // Create new node entity with adjusted attributes
        CommonEntityData ced = new CommonEntityData(
                node.getId(),
                node.getVersion(),
                node.getTimestamp(),
                node.getUser(),
                node.getChangesetId(),
                nodeTags);

        // Distribute the new nodecontainer to the following sink
        sink.process(new NodeContainer(new Node(ced, lat, lon)));
    }
    
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
                return "";
            }
            else if (this.maxDistAction == MaxDistAction.WARN) {
                logger.log(Level.WARNING, "Node {0} has distance {1} to the point where it should be!", new Object[]{osmId, distance});
                return item.DATA;
            }
            else {
                throw new IllegalArgumentException("Unknown action: " + this.maxDistAction.toString());
            }
        }
        return item.DATA;
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
}
