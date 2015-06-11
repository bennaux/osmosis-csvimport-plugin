/*
 * TODO Benno javadoc
 */
package net.bennokue.java.osmosis;

import java.util.HashMap;
import java.util.Map;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

/**
 * Loads the CSVImportPlugin during OSMOSIS initialization.
 * @author bennokue
 */
public class CSVImportPlugin_loader implements PluginLoader {
    /**
     * The CLI argument that tells OSMOSIS to run the CSVImportPlugin.
     */
    public static final String taskName = "import-tag-from-csv";

    @Override
    public Map<String, TaskManagerFactory> loadTaskFactories() {
        Map<String, TaskManagerFactory> factoryMap = new HashMap<>();
        CSVImportPlugin_factory calculatorPlugin = new CSVImportPlugin_factory();

        factoryMap.put(taskName, calculatorPlugin);

        return factoryMap;
    }
}