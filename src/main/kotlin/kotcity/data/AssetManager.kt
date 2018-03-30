package kotcity.data

import com.github.salomonbrys.kotson.*
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotcity.ui.sprites.BuildingSpriteLoader
import kotcity.util.randomElement
import java.io.FileReader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors.toList
import kotlin.reflect.KClass

class AssetManager(val cityMap: CityMap) {

    private val directories = listOf("residential", "commercial", "industrial", "civic")

    fun findResources(): List<String> {
        // OK now let's find the files in each...
        return directories.map { dir ->
            assetsInDir(dir)
        }.flatten()
    }

    // TODO: use desirability later...
    fun findBuilding(zoneType: Zone, level: Int): Building? {
        return all().filterIsInstance(zoneTypeToClass(zoneType)).filter { it.level == level }.randomElement()
    }

    private fun zoneTypeToClass(zoneType: Zone): Class<out Building> {
        return when (zoneType) {
            Zone.RESIDENTIAL -> Residential::class.java
            Zone.COMMERCIAL -> Commercial::class.java
            Zone.INDUSTRIAL -> Industrial::class.java
        }
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

    fun all(): List<LoadableBuilding> {
        return directories.map { dir ->
            assetsInDir(dir).mapNotNull { assetFile ->
                loadFromFile(assetFile)
            }

        }.flatten()
    }

    /**
     * Takes a given file and (possibly) returns a [LoadableBuilding]
     * @param assetFile filename to load from
     */
    fun loadFromFile(assetFile: String): LoadableBuilding? {
        val buildingJson = CityFileAdapter.gson.fromJson<JsonElement>(FileReader(assetFile)).asJsonObject

        val buildingType = buildingJson["type"].nullString

        val lb = if (buildingType != null) {
            val lb = when (buildingType) {
                "commercial" -> Commercial(cityMap)
                "residential" -> Residential(cityMap)
                "industrial" -> Industrial(cityMap)
                "civic" -> Civic(cityMap)
                else -> {
                    throw RuntimeException("Unknown type: $buildingType")
                }
            }
            lb.name = buildingJson["name"].asString
            // OK let's populate the rest...
            populateBuildingData(lb, buildingJson)
            lb
        } else {
            null
        }
        return lb
    }

    fun buildingFor(klass: KClass<out Building>, name: String): Building {

        // ok we need to find the matching JSON file for this crap...
        val assetFile = findAsset(klass, name)

        val buildingJson = CityFileAdapter.gson.fromJson<JsonObject>(FileReader(assetFile))

        val lb : LoadableBuilding = when (klass) {
            Residential::class -> Residential(cityMap)
            Commercial::class -> Commercial(cityMap)
            Industrial::class -> Industrial(cityMap)
            Civic::class -> Civic(cityMap)
            else -> {
                throw RuntimeException("I don't know how to instantiate $klass")
            }
        }

        lb.name = name
        // OK let's populate the rest...
        populateBuildingData(lb, buildingJson)
        return lb
    }

    private fun findAsset(klass: KClass<out Building>, name: String): String {
        return when (klass) {
            Residential::class -> "./assets/residential/$name.json"
            Commercial::class -> "./assets/commercial/$name.json"
            Industrial::class -> "./assets/industrial/$name.json"
            Civic::class -> "./assets/civic/$name.json"
            else -> throw RuntimeException("I don't know how to handle asset $klass/$name")
        }
    }

    private fun populateBuildingData(lb: LoadableBuilding, buildingJson: JsonObject) {
        lb.width = buildingJson["width"].asInt
        lb.height = buildingJson["height"].asInt
        lb.sprite = buildingJson["sprite"].asString
        lb.pollution = buildingJson["pollution"].nullDouble ?: 0.0
        lb.description = buildingJson["description"].asString
        lb.level = buildingJson["level"].asInt
        if (buildingJson.has("upkeep")) {
            lb.upkeep = buildingJson["upkeep"].asInt
        }

        checkSprite(lb)
        populateProduction(lb, buildingJson)
    }

    private fun checkSprite(loadableBuilding: LoadableBuilding) {
        if (loadableBuilding.sprite == null || loadableBuilding.sprite == "") {
            throw RuntimeException("Could not load sprite for $loadableBuilding")
        }
        BuildingSpriteLoader.filename(loadableBuilding)
    }

    private fun populateProduction(lb: LoadableBuilding, buildingJson: JsonObject) {
        if (! buildingJson.has("production")) {
            return
        }
        buildingJson["production"].asJsonObject?.let { production ->

            if (production.has("consumes")) {
                production["consumes"].asJsonObject?.let { consumes ->
                    val names = consumes.keys()
                    names.forEach { name ->
                        lb.consumes[Tradeable.valueOf(name.toUpperCase())] = consumes[name].asInt
                    }
                }
            }

            if (production.has("produces")) {
                production["produces"].asJsonObject?.let { produces ->
                    val names = produces.keys()
                    names.forEach { name ->
                        lb.produces[Tradeable.valueOf(name.toUpperCase())] = produces[name].asInt
                    }
                }
            }

        }
    }
}