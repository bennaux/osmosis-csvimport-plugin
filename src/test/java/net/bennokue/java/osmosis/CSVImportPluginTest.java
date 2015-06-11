/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/* TODO Benno Javadoc */
package net.bennokue.java.osmosis;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bennokue
 */
public class CSVImportPluginTest {
    
    public CSVImportPluginTest() {
    }
    
    @Test
    public void testDistanceChecks() {
        double lat1 = 48.1465401;
        double lon1 = 11.5932276;
        double lat2 = 48.1471246;
        double lon2 = 11.5918108;
        double expectedDistance = 0.1236*1000;
        double distance = CSVItem.distFrom(lat1, lon1, lat2, lon2);
        // Direct test
        assertEquals(expectedDistance, distance, 0.1);
        // Object test
        CSVItem item = new CSVItem(13, lat1, lon1, "Not important");
        double distance_obj = item.getDistance(lat2, lon2);
        assertEquals(distance, distance_obj, 0);
    }
    

//    @Test(expected = IllegalArgumentException.class)
//    public void noFileTest() {
//        CSVImportPlugin_task task = new CSVImportPlugin_task("", 1, -1, -1, -1, "test", 0, CSVImportPlugin_task.MaxDistAction.WARN);
//    }
//    
//    @Test(expected = IllegalArgumentException.class)
//    public void illegalFileTest() {
//        CSVImportPlugin_task task = new CSVImportPlugin_task("iAmNotHere.txt", -1, -1, -1, -1, "", -1, CSVImportPlugin_task.MaxDistAction.WARN);
//    }
    
}
