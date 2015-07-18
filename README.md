# osmosis-csvimport-plugin
OSMOSIS plugin to add node tags read from a CSV file. Use at your own risk.

 

## Usage ##
### Library usage ###
You can use the plugin as a library like you would use every other OSMOSIS plugin. Just build it and use it, at your own risk. See the javadoc and the test classes for more information.

### Usage from the CLI ###
If you want to use the plugin with OSMOSIS directly from the command line, build it and put its `.jar` file into OSMOSIS' `bin/plugins` folder. 

#### Command line parameters ####

The task name for this plugin is `import-tag-from-csv`.

- `idPos`: The position of the OSM id in each line of the csv file (all `~pos` values start counting at `1`, so `0` is not a valid argument).
- `latPos`: Optional argument. The CSV line position of the nodes' latitude.
- `lonPos`: Optional argument. The CSV line position of the nodes' longitude.
- `tagDataPos`: The CSV line position of the data that should be imported into a node tag.
- `outputTag`: The name of that tag. **Matching tags that already exist in the OSM data will be removed** before anything else happens.
- `maxDist`: Optional argument, only working whith `latPos` and `lonPos`: If given, there will be some action if the position of the CSV node and the OSM node differ more than `maxDist` meters. Defaults to `POSITIVE_INFINITY` (= feature switched off).
- `maxDistAction`: The action that should be taken if a distance exceeds `maxDist`. There are three actions at the moment: `DELETE` prevents the import of the CSV item and screams, `WARN` just screams. `LOG` acts like `DELETE` but also writes the nodes, the positions and the distance into a file named after the input file (with added `-dirtyNodes` before the extension).
- `inputCSV`: The path to the CSV file to import. CSV Lines starting with `;` will be ignored.
- `csvCacheSize`: The size of the CSV lines cache. This defaults to `-1` which makes the cache endless, and you really, really should not limit the cache's size unless your memory gives up, as limiting the cache makes the processing take hundreds of times longer time.
- `progressInfoIntervalSecs`: When you specify a number `s` here, you will be given a short status information every `s` seconds.

#### Example ####
Imagine you have an OSM file `input.osm` with three nodes that have the ids `1`, `2` and `3`.

You also have a CSV file named `littleCSV.csv` with the following contents: 

        1,I am one
        2,I am two

The following OSMOSIS call will bring you a file `output.osm` where the node with the id `1` has a tag named `testTag` with the value `I am one` and the node with the id `2` has a tag named `testTag` with the value `I am two`:

        osmosis --read-xml input.osm --import-tag-from-csv idPos=1 tagDataPos=2 outputTag=testTag inputCSV=littleCSV.csv --write-xml output.osm

## Versions ##

        v1.2: *Added the possibility to make the cache "endless" (issue #2).
              *Added the progressInfoIntervalSecs option. 

	    v1.1: *Added the LOG action (issue #3).
              *Added a small statistics output at the end of the plugin execution (issue #1). 
        
        v1.0: First version

## Thanks to... ##

- OSMOSIS developers
- Franz Graf (structure copied from his SRTM plugin)