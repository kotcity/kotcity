package kotcity.data

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.keys
import com.github.salomonbrys.kotson.nullDouble
import com.github.salomonbrys.kotson.nullString
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotcity.data.buildings.*
import kotcity.ui.sprites.BuildingSpriteLoader
import kotcity.util.randomElement
import java.io.FileReader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors.toList
import kotlin.collections.set
import kotlin.reflect.KClass

class AssetManager(val cityMap: CityMap) {

    private val directories = listOf(
        "residential",
        "commercial",
        "industrial",
        "civic"
    )

    fun findResources() = directories.map { dir -> assetsInDir(dir) }.flatten()

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
            .map({ it.toAbsolutePath().toString() })
            .collect(toList<String>())
    }

    fun all() = directories.map { dir -> assetsInDir(dir).mapNotNull { loadFromFile(it) } }.flatten()

    /**
     * Takes a given file and (possibly) returns a [LoadableBuilding]
     * @param assetFile filename to load from
     */
    fun loadFromFile(assetFile: String): LoadableBuilding? {
        val buildingJson = CityFileAdapter.gson.fromJson<JsonElement>(FileReader(assetFile)).asJsonObject

        val buildingType = buildingJson["type"].nullString ?: return null

        val building = when (buildingType) {
            "commercial" -> Commercial()
            "residential" -> Residential()
            "industrial" -> Industrial()
            "civic" -> Civic()
            else -> throw RuntimeException("Unknown type: $buildingType")
        }
        building.name = buildingJson["name"].asString
        // OK let's populate the rest...
        populateBuildingData(building, buildingJson)
        return building
    }

    fun buildingFor(klass: KClass<out Building>, name: String): LoadableBuilding {
        // ok we need to find the matching JSON file for this crap...
        val assetFile = findAsset(klass, name)

        val buildingJson = CityFileAdapter.gson.fromJson<JsonObject>(FileReader(assetFile))

        val building = when (klass) {
            Residential::class -> Residential()
            Commercial::class -> Commercial()
            Industrial::class -> Industrial()
            Civic::class -> Civic()
            else -> throw RuntimeException("I don't know how to instantiate $klass")
        }

        building.name = name
        // OK let's populate the rest...
        populateBuildingData(building, buildingJson)
        return building
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

    private fun populateBuildingData(building: LoadableBuilding, json: JsonObject) {
        building.width = json["width"].asInt
        building.height = json["height"].asInt
        building.sprite = json["sprite"].asString
        building.pollution = json["pollution"].nullDouble ?: 0.0
        building.description = json["description"].asString
        building.level = json["level"].asInt
        if (json.has("upkeep")) {
            building.upkeep = json["upkeep"].asInt
        }

        checkSprite(building)
        populateProduction(building, json)
    }

    private fun checkSprite(building: LoadableBuilding) {
        if (building.sprite.isNullOrEmpty()) {
            throw RuntimeException("Could not load sprite for $building")
        }
        BuildingSpriteLoader.filename(building)
    }

    private fun populateProduction(building: LoadableBuilding, json: JsonObject) {
        if (!json.has("production")) {
            return
        }
        json["production"].asJsonObject?.let { production ->
            if (production.has("consumes")) {
                production["consumes"].asJsonObject?.let { consumes ->
                    val names = consumes.keys()
                    names.forEach { name ->
                        building.consumes[Tradeable.valueOf(name.toUpperCase())] = consumes[name].asInt
                    }
                }
            }
            if (production.has("produces")) {
                production["produces"].asJsonObject?.let { produces ->
                    val names = produces.keys()
                    names.forEach { name ->
                        building.produces[Tradeable.valueOf(name.toUpperCase())] = produces[name].asInt
                    }
                }
            }
        }
    }
}
