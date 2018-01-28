package kotcity.data.assets

import com.github.salomonbrys.kotson.fromJson
import kotcity.data.*
import kotcity.data.BuildingType.*
import java.io.FileReader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors.toList
import javax.json.JsonObject

class AssetManager {
    fun findResources(): List<String> {
        val directories = listOf("residential", "commercial", "industrial")
        // OK now let's find the files in each...
        val resources = mutableListOf<String>()
        directories.forEach { dir ->
            //Object a matcher object from the supplied Glob pattern
            val glob = "glob:*.json"
            // println("Our glob is: $glob")
            val matcher = FileSystems.getDefault().getPathMatcher(glob)

            val path = Paths.get("./assets/$dir/")
            //Walk the file system
            Files.walk(path)
                    //Filter out anything that doesn't match the glob
                    .filter { it : Path? ->
                        // println("Checking $it")
                        it?.let { matcher.matches(it.fileName) } ?: false
                    }
                    //Collect to a list
                    .collect(toList())
                    //Print to the console
                    .forEach({ it -> resources.add(it.fileName.toString()) })
        }
        return resources
    }

    fun buildingFor(buildingType: BuildingType, name: String): Building {

        // ok we need to find the matching JSON file for this crap...
        val assetFile = findAsset(buildingType, name)

        val lb = LoadableBuilding()
        lb.name = name
        lb.type = buildingType
        // OK let's populate the rest...
        populateBuildingData(lb, assetFile)
        return lb
    }

    private fun populateBuildingData(lb: LoadableBuilding, assetFile: String) {
        val buildingJson = gson.fromJson<JsonObject>(FileReader(assetFile))
        lb.width = buildingJson.getInt("width")
        lb.height = buildingJson.getInt("height")
        lb.sprite = buildingJson.getString("sprite")
        lb.description = buildingJson.getString("description")
        lb.level = buildingJson.getInt("level")
    }

    private fun findAsset(type: BuildingType, name: String): String {
        return when (type) {
            RESIDENTIAL -> "./assets/residential/$name.json"
            COMMERCIAL -> "./assets/commercial/$name.json"
            INDUSTRIAL -> "./assets/industrial/$name.json"
            else -> throw RuntimeException("I don't know how to handle asset $type/$name")
        }
    }
}