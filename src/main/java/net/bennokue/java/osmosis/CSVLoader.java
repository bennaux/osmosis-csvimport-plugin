package net.bennokue.java.osmosis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class reads a CSV file over and over and lets you query for OSM ids. To
 * speed things up, it uses a cache to store the last read lines. Empty lines
 * and <strong>lines starting with {@code ;} will be ignored</strong>.
 *
 * @author bennokue
 */
public class CSVLoader {

    private static final Logger logger = Logger.getLogger(CSVLoader.class.getName());

    /**
     * The CSV input file.
     */
    private final File csvInputFile;
    /**
     * Here we store the read and parsed lines.
     */
    private final HashMap<Long, CSVItem> cache;
    /**
     * Don't let the cache get bigger than this.
     */
    private final int cacheSize;
    /**
     * At this position in each line we look for the OSM id (first element in a
     * line has position {@code 1}).
     */
    private final int osmIdPos;
    /**
     * At this position in each line we look for the OSM latitude (first element
     * in a line has position {@code 1}).
     */
    private final int osmLatPos;
    /**
     * At this position in each line we look for the OSM longitude (first
     * element in a line has position {@code 1}).
     */
    private final int osmLonPos;
    /**
     * At this position in each line we look for the String that we want to
     * import as a new Node tag (first element in a line has position
     * {@code 1}).
     */
    private final int tagDataPos;
    private FileInputStream fileInputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    /**
     * Our current line position at the {@link #csvInputFile}.
     */
    private long lineNumber;
    /**
     * How often did we cycle through the input csv?
     */
    private int runsThroughFile = 1;
    /**
     * {@link #markLine()} and {@link #updateMark()} use this field to update
     * {@link #passedMark}.
     */
    private long marked_line;
    /**
     * {@link #markLine()} and {@link #updateMark()} use this field to update
     * {@link #passedMark}.
     */
    private int marked_runThroughFile;
    /**
     * This flag tells you if we re-met the same spot in the input csv where we
     * started. When looking for a line, we do not want to start all over at the
     * input file, and we also do not want to cycle in it forever. So, if you
     * start searching, call {@link #markLine()}. {@code passedMark} will get
     * {@code true} when we drop by at the same spot in the input file the next
     * time.
     *
     * Do not forget to call {@link #updateMark()} every time you read a line to
     * keep this working.
     */
    private boolean passedMark;

    /**
     * Standard constructor.
     *
     * @param csvInputFile The CSV file to import.
     * @param cacheSize The cache will not exceed the number of slots specified
     * here.
     * @param osmIdPos At this position in each line we look for the OSM id
     * (first element in a line has position {@code 1}).
     * @param osmLatPos At this position in each line we look for the OSM
     * latitude (first element in a line has position {@code 1}).
     * @param osmLonPos At this position in each line we look for the OSM
     * longitude (first element in a line has position {@code 1}).
     * @param tagDataPos At this position in each line we look for the String
     * that we want to import as a new Node tag (first element in a line has
     * position {@code 1}).
     * @throws FileNotFoundException If anything goes wrong.
     */
    public CSVLoader(File csvInputFile, int cacheSize, int osmIdPos, int osmLatPos, int osmLonPos, int tagDataPos) throws FileNotFoundException {
        this.csvInputFile = csvInputFile;
        this.cacheSize = cacheSize;
        this.cache = new HashMap<>();
        this.osmIdPos = osmIdPos;
        this.osmLatPos = osmLatPos;
        this.osmLonPos = osmLonPos;
        this.tagDataPos = tagDataPos;

        // Initialize the readers
        this.fileInputStream = new FileInputStream(this.csvInputFile);
        this.inputStreamReader = new InputStreamReader(this.fileInputStream);
        this.bufferedReader = new BufferedReader(this.inputStreamReader);
    }

    /**
     * Read as many lines as are needed to fill the cache completely, parse them
     * and store them in cache. If the input file has fewer lines than the
     * cache, lines will be read and parsed more than once.
     */
    private void fillCache() throws IOException {
        logger.log(Level.FINER, "Filling cache");
        for (int i = 0; i <= (this.cacheSize - this.cache.size()); i++) {
            String line = this.readLine();
            CSVItem currentItem = this.parseCSVItem(line);
            if (null != currentItem) {
                this.cache.put(currentItem.OSM_ID, currentItem);
            }
        }
    }

    /**
     * See {@link #passedMark}.
     */
    private void markLine() {
        this.marked_line = this.lineNumber;
        this.marked_runThroughFile = this.runsThroughFile;
        this.passedMark = false;
    }

    /**
     * Call this method every time you read a line to keep {@link #passedMark}
     * working.
     */
    private void updateMark() {
        if (this.runsThroughFile == this.marked_runThroughFile + 1 && this.lineNumber == this.marked_line) {
            this.passedMark = true;
            return;
        }
        // Emergency break
        if (this.runsThroughFile > this.marked_line + 1) {
            this.passedMark = true;
        }
    }

    /**
     * Reads a line from the input CSV and starts all over when reaching the
     * bottom.
     *
     * @return
     * @throws IOException If the file contains no lines at all.
     */
    private String readLine() throws IOException {
        String line = this.bufferedReader.readLine();
        if (null == line) {
            this.resetReaders();
            line = this.bufferedReader.readLine();
            this.runsThroughFile++;
        }
        // Still null? File seems to be empty
        if (null == line) {
            throw new IOException("CSV file seems to be empty");
        }
        this.lineNumber++;
        this.updateMark();
        return line;
    }

    /**
     * Start reading all over again (updates {@link #lineNumber}).
     *
     * @throws IOException If anything goes wrong.
     */
    private void resetReaders() throws IOException {
        logger.log(Level.FINER, "Resetting readers");
        // Close
        this.bufferedReader.close();
        this.inputStreamReader.close();
        this.fileInputStream.close();
        // Reset
        this.fileInputStream = new FileInputStream(this.csvInputFile);
        this.inputStreamReader = new InputStreamReader(this.fileInputStream);
        this.bufferedReader = new BufferedReader(this.inputStreamReader);
        this.lineNumber = 1;
    }

    /**
     * Parse a line into a {@link CSVItem}. Empty lines or lines starting with
     * {@code ;} will be ignored.
     *
     * @param line The line to parse.
     * @return A {@link CSVItem}.
     */
    private CSVItem parseCSVItem(String line) {
        if (line.equals("") || line.startsWith(";")) {
            logger.log(Level.FINE, "Empty line (or starting with >;<): {0}", this.lineNumber);
            return null;
        }
        // Split the line and check if there are enough parts
        String[] lineChunks = line.split(",");
        if (lineChunks.length < Math.max(Math.max(this.osmIdPos, this.osmLatPos), Math.max(this.osmLonPos, this.tagDataPos))) {
            logger.log(Level.WARNING, "Line is too short: {0}", line);
            return null;
        }
        // Read the id
        long osmId;
        try {
            osmId = Long.parseLong(lineChunks[this.osmIdPos - 1]);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Mal-formed line (id): " + this.lineNumber, e);
            return null;
        }
        // Read the data
        String tagData = lineChunks[this.tagDataPos - 1];
        // Read lon and lat
        double longitude = Double.NaN;
        double latitude = Double.NaN;
        if (this.osmLatPos > 0 && this.osmLonPos > 0) {
            try {
                latitude = Double.parseDouble(lineChunks[this.osmLatPos - 1]);
                longitude = Double.parseDouble(lineChunks[this.osmLonPos - 1]);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Mal-formed line (lat/lon): " + this.lineNumber, e);
            }
        }
        CSVItem item = new CSVItem(osmId, latitude, longitude, tagData);
        return item;
    }

    /**
     * Try to find a {@link CSVItem} at the cache (cache-hit) and if it isn't
     * there, seek through the whole file. The cache will be cleared and
     * re-filled when seeking.
     *
     * @param id The OSM id of the element to find.
     * @return The {@link CSVItem} with the matching id or {@code null} if it
     * isn't present at the file.
     * @throws IOException If something goes wrong.
     */
    public CSVItem findItem(long id) throws IOException {
        // Lookup in the cache
        CSVItem item = this.cache.get(id);
        if (null != item) {
            logger.log(Level.FINEST, "Cache hit");
            return item;
        }
        // Search the item
        logger.log(Level.FINEST, "Cache miss {0}", id);
        this.markLine();
        while (null == item && !this.passedMark) {
            this.cache.clear();
            this.fillCache();
            item = this.cache.get(id);
        }
        if (null == item) {
            logger.log(Level.FINE, "Could not find osm id {0}", id);
        } else {
            logger.log(Level.FINEST, "Cache hit");
        }
        return item;    // null or the item
    }
}
