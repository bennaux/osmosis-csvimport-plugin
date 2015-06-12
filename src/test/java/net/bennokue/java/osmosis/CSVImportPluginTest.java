/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/* TODO Benno Javadoc */
package net.bennokue.java.osmosis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;
import org.xml.sax.SAXException;

/**
 *
 * @author bennokue
 */
public class CSVImportPluginTest {

    /**
     * Set this to false if you want to have a look at the generated osm files
     * for yourself. They will be created in your system's temp directory.
     */
    public static final boolean deleteTemporaryFiles = false;
    public static final int cacheSize = 6000;

    private static final Logger logger = Logger.getLogger(CSVImportPluginTest.class.getName());

    public CSVImportPluginTest() {
    }

    @BeforeClass
    public static void initLogging() {
//        try (InputStream is = CSVImportPluginTest.class.getResourceAsStream("/logging.properties")) {
//            LogManager.getLogManager().readConfiguration(is);
//        } catch (SecurityException | IOException | NullPointerException ex) {
//            logger.log(Level.SEVERE, "ERROR: {0}\nlogging.properties not found inside jar!", ex.getMessage());
//            System.out.println("Huhuhuhuhuuuu" + ex.getMessage());
//            ex.printStackTrace();
//        }
//        logger.setLevel(Level.ALL);
//        logger.addHandler(new ConsoleHandler());
//        System.out.println(logger.getHandlers().length);
        logger.log(Level.FINEST, "Set up logging.");
        Calendar calendar = Calendar.getInstance();
        System.out.println("Test started at " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + ".");
    }

    @AfterClass
    public static void printTail() {
        Calendar calendar = Calendar.getInstance();
        System.out.println("Test ended at " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + ".");
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testDistanceChecks() {
        double lat1 = 48.1465401;
        double lon1 = 11.5932276;
        double lat2 = 48.1471246;
        double lon2 = 11.5918108;
        double expectedDistance = 0.1236 * 1000;
        double distance = CSVItem.distFrom(lat1, lon1, lat2, lon2);
        // Direct test
        assertEquals(expectedDistance, distance, 0.1);
        // Object test
        CSVItem item = new CSVItem(13, lat1, lon1, "Not important");
        double distance_obj = item.getDistance(lat2, lon2);
        assertEquals(distance, distance_obj, 0);
    }

    @Test
    public void testWithoutFilename() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("You have to provide an input file!");
        CSVImportPlugin_task task = new CSVImportPlugin_task("", 1, -1, -1, 2, "testTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    public void testWithIllegalInputFile() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("iAmNotHere.csv is not a file or not readable!");
        CSVImportPlugin_task task = new CSVImportPlugin_task("iAmNotHere.csv", 1, -1, -1, 2, "testTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    public void testWithMaxDistButNoLatPos() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Provide latPos and lonPos when using maxDist");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), 1, 2, -1, 2, "testTag", 7, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    public void testWithMaxDistButNoLonPos() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Provide latPos and lonPos when using maxDist");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), 1, -1, 2, 2, "testTag", 7, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    public void testWithoutOSMIDPos() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Please provide an idPos greater than 0");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), -1, -1, -1, 2, "testTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    public void testWithoutTagDataPos() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Please provide a tagDataPos greater than 0");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), 1, -1, -1, -1, "testTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    public void testWithoutOutputTagName() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Please provide an outputTag");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), 1, -1, -1, 2, "", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    public void testWithSortedInputFile() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        File testFile = conductTest("/munich_lmu_original.osm", "/sorted_linenumbers.csv", 1, -1, -1, 2, "lmuTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
        XMLFlattener flattener = new XMLFlattener(testFile);
        String[] resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        String[] expectedValues = fillWithStringRange(1, 5507);
        assertArrayEquals(expectedValues, resultValues);
    }

    @Test
    public void testWithUnsortedInputFile() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        File testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers.csv", 1, -1, -1, 2, "lmuTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
        XMLFlattener flattener = new XMLFlattener(testFile);
        String[] resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        String[] expectedValues = fillWithStringRange(1, 5507);
        assertArrayEquals(expectedValues, resultValues);
    }

    @Test
    public void testWithUnsortedInputFileMissingIds() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        File testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers_missingIds.csv", 1, -1, -1, 2, "lmuTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
        XMLFlattener flattener = new XMLFlattener(testFile);
        String[] resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        String[] expectedValues = fillWithStringRange(1, 5507, new int[]{4925, 2320, 4745, 3565});
        assertArrayEquals(expectedValues, resultValues);
    }

    @Test
    public void testWithUnsortedInputFileEmptyLines() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        File testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers_emptyLines.csv", 1, -1, -1, 2, "lmuTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
        XMLFlattener flattener = new XMLFlattener(testFile);
        String[] resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        String[] expectedValues = fillWithStringRange(1, 5507);
        assertArrayEquals(expectedValues, resultValues);
    }

    @Test
    public void testWithUnsortedInputFileDifferingPositions() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        // 3 meters distance -- WARN only
        File testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers_differingLonLat.csv", 1, 2, 3, 4, "lmuTag", 3.0, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
        XMLFlattener flattener = new XMLFlattener(testFile);
        String[] resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        String[] expectedValues = fillWithStringRange(1, 5507); // Warn only!
        assertArrayEquals("First turn", expectedValues, resultValues);

        // 3 meters distance -- DELETE nodes
        testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers_differingLonLat.csv", 1, 2, 3, 4, "lmuTag", 3.0, CSVImportPlugin_task.MaxDistAction.DELETE, cacheSize);
        flattener = new XMLFlattener(testFile);
        resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        expectedValues = fillWithStringRange(1, 5507, new int[]{2597});
        assertArrayEquals("Second turn", expectedValues, resultValues);

        // 1 meter distance -- DELETE nodes
        testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers_differingLonLat.csv", 1, 2, 3, 4, "lmuTag", 1.0, CSVImportPlugin_task.MaxDistAction.DELETE, cacheSize);
        flattener = new XMLFlattener(testFile);
        resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        expectedValues = fillWithStringRange(1, 5507, new int[]{2597, 1683});
        assertArrayEquals("Third turn", expectedValues, resultValues);
    }

    private static File conductTest(String OSMinputFileString, String CSVinputFileString, int idPos,
            int latPos, int lonPos, int tagDataPos, String outputTag,
            Double maxDist, CSVImportPlugin_task.MaxDistAction maxDistAction,
            int csvCacheSize) throws URISyntaxException, IOException {
        File inputOSMFile = new File(new URI(CSVImportPluginTest.class.getResource(OSMinputFileString).toString()));
        File inputCSVFile = new File(new URI(CSVImportPluginTest.class.getResource(CSVinputFileString).toString()));
        File outputFile = java.io.File.createTempFile("osmosiscsvimporttest", null, null);
        if (deleteTemporaryFiles) {
            outputFile.deleteOnExit();
        }

        CSVImportPlugin_task importTask = new CSVImportPlugin_task(inputCSVFile.getPath(), idPos, latPos, lonPos, tagDataPos, outputTag, maxDist, maxDistAction, csvCacheSize);
        try (BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));) {
            XmlReader xmlReader = new XmlReader(inputOSMFile, false, CompressionMethod.None);
            XmlWriter xmlWriter = new XmlWriter(outputWriter);

            // Chain the pieces together: XMLLoader -- Importer -- XMLWriter
            xmlReader.setSink(importTask);
            importTask.setSink(xmlWriter);

            // RUN
            xmlReader.run();
        } catch (Exception e) {
            throw e;
        }
        return outputFile;
    }

    private String[] fillWithStringRange(int start, int end) {
        String[] result = new String[(end - start) + 1];
        int value = start;
        for (int i = 0; i < result.length; i++) {
            result[i] = String.valueOf(value);
            value++;
        }
        return result;
    }

    private String[] fillWithStringRange(int start, int end, int[] leaveOutNumbers) {
        // Set creation 
        HashSet<Integer> leaveOutNumbersSet = new HashSet<>(leaveOutNumbers.length);
        for (int i = 0; i < leaveOutNumbers.length; i++) {
            if (leaveOutNumbers[i] > end || leaveOutNumbers[i] < start) {
                throw new IllegalArgumentException("LeaveOut only allowed inside range");
            }
            leaveOutNumbersSet.add(leaveOutNumbers[i]);
        }

        // Result creation
        String[] result = new String[((end - start) + 1) - leaveOutNumbers.length];
        int value = start;
        for (int i = 0; i < result.length; i++) {
            if (leaveOutNumbersSet.contains(value)) {
                leaveOutNumbersSet.remove(value);
                i--;
                value++;
                continue;
            }
            result[i] = String.valueOf(value);
            value++;
        }
        return result;
    }
}
