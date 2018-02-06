package kotcity.data.assets

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.keys
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotcity.data.*
import kotcity.data.BuildingType.*
import java.io.FileReader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors.toList

class AssetManager {

    private val directories = listOf("residential", "commercial", "industrial")

    fun findResources(): List<String> {
        // OK now let's find the files in each...
        return directories.map { dir ->
            assetsInDir(dir)
        }.flatten()
    }

    private fun assetsInDir(dir: String): List<String> {
        val glob = "glob:*.json"
        val matcher = FileSystems.getDefault().getPathMatcher(glob)

        val path = Paths.get("./assets/$dir/")
        return Files.walk(path)
                .filter { it: Path? ->
                    it?.let { matcher.matches(it.fileName) } ?: false
                }
                .collect(toList())
                .map({ it.toAbsolutePath().toString() })
    }

    fun all(): List<Building> {
        return directories.map { dir ->
            assetsInDir(dir).map { assetFile ->
                val buildingJson = gson.fromJson<JsonElement>(FileReader(assetFile)).asJsonObject
                val lb = LoadableBuilding()
                lb.name = buildingJson["name"].asString
                buildingJson["type"].asString.let {
                    lb.type = when(it) {
                        "commercial" -> COMMERCIAL
                        "residential" -> RESIDENTIAL
                        "industrial" -> INDUSTRIAL
                        else -> throw RuntimeException("Unknown building type: $it")
                    }
                }

                // OK let's populate the rest...
                populateBuildingData(lb, buildingJson)
                lb
            }

        }.flatten()
    }

    fun buildingFor(buildingType: BuildingType, name: String): Building {

        // ok we need to find the matching JSON file for this crap...
        val assetFile = findAsset(buildingType, name)

        val buildingJson = gson.fromJson<JsonObject>(FileReader(assetFile))

        val lb = LoadableBuilding()
        lb.name = name
        lb.type = buildingType
        // OK let's populate the rest...
        populateBuildingData(lb, buildingJson)
        return lb
    }

    private fun findAsset(type: BuildingType, name: String): String {
        return when (type) {
            RESIDENTIAL -> "./assets/residential/$name.json"
            COMMERCIAL -> "./assets/commercial/$name.json"
            INDUSTRIAL -> "./assets/industrial/$name.json"
            else -> throw RuntimeException("I don't know how to handle asset $type/$name")
        }
    }

    private fun populateBuildingData(lb: LoadableBuilding, buildingJson: JsonObject) {
        lb.width = buildingJson["width"].asInt
        lb.height = buildingJson["height"].asInt
        lb.sprite = buildingJson["sprite"].asString
        lb.description = buildingJson["description"].asString
        lb.level = buildingJson["level"].asInt
        populateProduction(lb, buildingJson)
    }

    private fun populateProduction(lb: LoadableBuilding, buildingJson: JsonObject) {
        buildingJson["production"]?.let { production ->

            production["consumes"].asJsonObject?.let { consumes ->
                val names = consumes.keys()
                names.forEach { name ->
                    lb.consumes[Tradeable.valueOf(name.toUpperCase())] = consumes[name].asInt
                }
            }

            production["produces"].asJsonObject?.let { produces ->
                val names = produces.keys()
                names.forEach { name ->
                    lb.produces[Tradeable.valueOf(name.toUpperCase())] = produces[name].asInt
                }
            }

        }
    }
}