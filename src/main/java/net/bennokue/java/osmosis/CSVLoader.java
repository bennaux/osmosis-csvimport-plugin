/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/* TODO Benno Javadoc */
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
 *
 * @author bennokue
 */
public class CSVLoader {
    private static final Logger logger = Logger.getLogger(CSVLoader.class.getName());

    private final File csvInputFile;
    private final HashMap<Long, CSVItem> cache;
    private final int cacheSize;
    private final int osmIdPos;
    private final int osmLatPos;
    private final int osmLonPos;
    private final int tagDataPos;
    private FileInputStream fileInputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private long lineNumber;
    private long inputLineCount;
    private int runsThroughFile;

    public CSVLoader(File csvInputFile, int cacheSize, int osmIdPos, int osmLatPos, int osmLonPos, int tagDataPos) throws FileNotFoundException {
        this.csvInputFile = csvInputFile;
        this.cacheSize = cacheSize;
        this.cache = new HashMap<>();
        this.osmIdPos = osmIdPos;
        this.osmLatPos = osmLatPos;
        this.osmLonPos = osmLonPos;
        this.tagDataPos = tagDataPos;

        this.fileInputStream = new FileInputStream(this.csvInputFile);
        this.inputStreamReader = new InputStreamReader(this.fileInputStream);
        this.bufferedReader = new BufferedReader(this.inputStreamReader);
    }

    private void fillCache() throws IOException {
        logger.log(Level.FINER, "Filling cache");
        for (int i = 0; i <= (this.cacheSize - this.cache.size()); i++) {
            String line = this.readLine();
            CSVItem currentItem = this.parseCSVItem(line);
            this.cache.put(currentItem.OSM_ID, currentItem);
        }
    }
    
    private String readLine() throws IOException {
        String line = this.bufferedReader.readLine();
        if (null == line) {
            this.resetReaders();
            line = this.bufferedReader.readLine();
            this.runsThroughFile++;
        }
        if (null == line) {
            throw new IOException("CSV file seems to be empty");
        }
        this.lineNumber++;
        return line;
    }
    
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
        this.inputLineCount = Math.max(this.inputLineCount, this.lineNumber);
        this.lineNumber = 1;
    }
    
    private CSVItem parseCSVItem(String line) {
        if (line.equals("")) {
            return null;
        }
        String[] lineChunks = line.split(",");
        if (lineChunks.length < Math.max(Math.max(this.osmIdPos, this.osmLatPos), Math.max(this.osmLonPos, this.tagDataPos))) {
            logger.log(Level.WARNING, "Line is too short: {0}", line);
            return null;
        }
        // Read the id
        long osmId = Long.parseLong(lineChunks[this.osmIdPos-1]);
        // Read the data
        String tagData = lineChunks[this.tagDataPos-1];
        // Read lon and lat
        double longitude = Double.NaN;
        double latitude = Double.NaN;
        if (this.osmLatPos > 0 && this.osmLonPos > 0) {
            latitude = Double.parseDouble(lineChunks[this.osmLatPos-1]);
            longitude = Double.parseDouble(lineChunks[this.osmLonPos-1]);
        }
        CSVItem item = new CSVItem(osmId, latitude, longitude, tagData);
        return item;
    }
    
    public CSVItem findItem(long id) throws IOException {
        // Lookup in the cache
        CSVItem item = this.cache.get(id);
        if (null != item) {
            logger.log(Level.FINEST, "Cache hit");
            return item;
        }
        // Search the item
        logger.log(Level.FINEST, "Searching {0}", id);
        int currentRunThroughFile = this.runsThroughFile;
        long currentLineNumber = this.lineNumber;
        while (null == item && (this.runsThroughFile <= currentRunThroughFile || this.lineNumber < currentLineNumber)) {
            this.cache.clear();
            this.fillCache();
            item = this.cache.get(id);
        }
        return item;    // null or the item
    }
}