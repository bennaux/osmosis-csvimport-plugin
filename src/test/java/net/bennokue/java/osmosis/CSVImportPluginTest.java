package net.bennokue.java.osmosis;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
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
 * Test class. Also useful if you want to see how you use OSMOSIS and this
 * plugin a a library.
 *
 * @author bennokue
 */
public class CSVImportPluginTest {

    /**
     * Set this to false if you want to have a look at the generated osm files
     * for yourself. They will be created in your system's temp directory.
     */
    public static final boolean deleteTemporaryFiles = true;
    /**
     * How large should the cache be? Too small: More cache misses. Too large:
     * Lines will be read multiple times.
     */
    public static final int cacheSize = 6000;

    private static final Logger logger = Logger.getLogger(CSVImportPluginTest.class.getName());

    public CSVImportPluginTest() {
    }

    @BeforeClass
    /**
     * Initialize the logging system. Specify your needs at the
     * {@code logging.properties} file.
     */
    public static void initLogging() {
        logger.log(Level.FINEST, "Set up logging.");
        Calendar calendar = Calendar.getInstance();
        System.out.println("Test started at " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + ".");
    }

    @AfterClass
    /**
     * Print a footer.
     */
    public static void printTail() {
        Calendar calendar = Calendar.getInstance();
        System.out.println("Test ended at " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + ".");
    }

    @Rule
    /**
     * Use this to expect exceptions and their messages.
     */
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    /**
     * Test the distance calculation.
     */
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
    /**
     * Fail-test without input filename.
     */
    public void testWithoutFilename() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("You have to provide an input file!");
        CSVImportPlugin_task task = new CSVImportPlugin_task("", 1, -1, -1, 2, "testTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    /**
     * Fail-test with illegal input file.
     */
    public void testWithIllegalInputFile() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("iAmNotHere.csv is not a file or not readable!");
        CSVImportPlugin_task task = new CSVImportPlugin_task("iAmNotHere.csv", 1, -1, -1, 2, "testTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    /**
     * Fail-test with {@code maxDist} but no {@code latPos}.
     */
    public void testWithMaxDistButNoLatPos() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Provide latPos and lonPos when using maxDist");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), 1, -1, 2, 2, "testTag", 7, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    /**
     * Fail-test with {@code maxDist} but no {@code lonPos}.
     */
    public void testWithMaxDistButNoLonPos() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Provide latPos and lonPos when using maxDist");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), 1, 2, -1, 2, "testTag", 7, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    /**
     * Fail-test without {@code idPos}.
     */
    public void testWithoutOSMIDPos() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Please provide an idPos greater than 0");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), -1, -1, -1, 2, "testTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    /**
     * Fail-test without {@code tagDataPos}.
     */
    public void testWithoutTagDataPos() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Please provide a tagDataPos greater than 0");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), 1, -1, -1, -1, "testTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    /**
     * Fail-test without {@code outputTagName}.
     */
    public void testWithoutOutputTagName() throws URISyntaxException {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Please provide an outputTag");
        File inputFile = new File(new URI(CSVImportPluginTest.class.getResource("/sorted_linenumbers.csv").toString()).getSchemeSpecificPart());
        CSVImportPlugin_task task = new CSVImportPlugin_task(inputFile.getPath(), 1, -1, -1, 2, "", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
    }

    @Test
    /**
     * Test with a sorted input file.
     */
    public void testWithSortedInputFile() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        File testFile = conductTest("/munich_lmu_original.osm", "/sorted_linenumbers.csv", 1, -1, -1, 2, "lmuTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
        XMLFlattener flattener = new XMLFlattener(testFile);
        String[] resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        String[] expectedValues = fillWithStringRange(1, 5507);
        assertArrayEquals(expectedValues, resultValues);
    }

    @Test
    /**
     * Test with a shuffled input file.
     */
    public void testWithUnsortedInputFile() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        File testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers.csv", 1, -1, -1, 2, "lmuTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
        XMLFlattener flattener = new XMLFlattener(testFile);
        String[] resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        String[] expectedValues = fillWithStringRange(1, 5507);
        assertArrayEquals(expectedValues, resultValues);
    }

    @Test
    /**
     * Test with a shuffled input file where some ids are commented out.
     */
    public void testWithUnsortedInputFileMissingIds() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        File testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers_missingIds.csv", 1, -1, -1, 2, "lmuTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
        XMLFlattener flattener = new XMLFlattener(testFile);
        String[] resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        String[] expectedValues = fillWithStringRange(1, 5507, new int[]{4925, 2320, 4745, 3565});
        assertArrayEquals(expectedValues, resultValues);
    }

    @Test
    /**
     * Test with a shuffled input file with some empty lines.
     */
    public void testWithUnsortedInputFileEmptyLines() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        File testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers_emptyLines.csv", 1, -1, -1, 2, "lmuTag", Double.POSITIVE_INFINITY, CSVImportPlugin_task.MaxDistAction.WARN, cacheSize);
        XMLFlattener flattener = new XMLFlattener(testFile);
        String[] resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        String[] expectedValues = fillWithStringRange(1, 5507);
        assertArrayEquals(expectedValues, resultValues);
    }

    @Test
    /**
     * Test with a shuffled input file with two faked positions (see
     * {@code unsorted_linenumbers_differingLonLat.csv}).
     */
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

        // 1 meter distance -- DELETE and LOG
        testFile = conductTest("/munich_lmu_original.osm", "/unsorted_linenumbers_differingLonLat.csv", 1, 2, 3, 4, "lmuTag", 1.0, CSVImportPlugin_task.MaxDistAction.LOG, cacheSize);
        flattener = new XMLFlattener(testFile);
        resultValues = flattener.getXPathAsArray("/osm/node/tag[@k=\"lmuTag\"]/@v");
        expectedValues = fillWithStringRange(1, 5507, new int[]{2597, 1683});
        assertArrayEquals("Fourth turn (values)", expectedValues, resultValues);
        // Check the log file
        File logFile = new File(new URI(CSVImportPluginTest.class.getResource("/unsorted_linenumbers_differingLonLat-dirtyNodes.csv").toString()));
        BufferedReader logFileReader = new BufferedReader(new FileReader(logFile));
        String[] expectedLogMessages = new String[]{
            "; osmId,lat,lon,csvLat,csvLon,csvData,deviation",
            "1565197595,48.1501216,11.5952002,48.1501316,11.5952002,1683," + CSVItem.distFrom(48.1501216, 11.5952002, 48.1501316, 11.5952002),
            "2524542752,48.1346312,11.5945651,48.1346812,11.5945651,2597," + CSVItem.distFrom(48.1346312, 11.5945651, 48.1346812, 11.5945651)
        };
        String line = logFileReader.readLine(); // The first line contains unforseeable data
        line = logFileReader.readLine();
        ArrayList<String> logLines = new ArrayList<>();
        while (null != line) {
            logLines.add(line);
            line = logFileReader.readLine();
        }
        assertArrayEquals("Fourth turn (logLines)", expectedLogMessages, logLines.toArray());
    }

    /**
     * Let the plugin run with specified parameters.
     *
     * @param OSMinputFileString
     * @param CSVinputFileString
     * @param idPos
     * @param latPos
     * @param lonPos
     * @param tagDataPos
     * @param outputTag
     * @param maxDist
     * @param maxDistAction
     * @param csvCacheSize
     * @return The OSM output file.
     * @throws URISyntaxException
     * @throws IOException
     */
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

    /**
     * Makes an array with an integer range as strings.
     *
     * @param start Lowest value.
     * @param end Highest value.
     * @return The result array.
     */
    private String[] fillWithStringRange(int start, int end) {
        String[] result = new String[(end - start) + 1];
        int value = start;
        for (int i = 0; i < result.length; i++) {
            result[i] = String.valueOf(value);
            value++;
        }
        return result;
    }

    /**
     * Makes an array with an integer range as strings and allows you to leave
     * some out.
     *
     * @param start Lowest value.
     * @param end Highest value.
     * @param leaveOutNumbers Array with the numbers to skip.
     * @return The result array.
     */
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
