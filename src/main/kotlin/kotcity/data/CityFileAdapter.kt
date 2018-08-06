package kotcity.data

import GzipUtil
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotcity.automata.PowerCoverageUpdater
import kotcity.data.buildings.*
import kotcity.pathfinding.Pathfinder
import kotcity.util.Debuggable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CityFileAdapter : Debuggable {
    override var debug: Boolean = false

    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private fun parseDate(str: String): Date {
        simpleDateFormat.timeZone = TimeZone.getDefault()
        return simpleDateFormat.parse(str)
    }

    private fun serializeDate(date: Date): String {
        simpleDateFormat.timeZone = TimeZone.getDefault()
        return simpleDateFormat.format(date)
    }

    val gson: Gson = GsonBuilder()
        .registerTypeAdapter<CityMap> {

            serialize {
                /*
                    it.type: Type to serialize from
                    it.context: GSon context
                    it.src : Object to serialize
                */
                val data = JsonObject()
                val map = it.src
                data["height"] = map.height
                data["width"] = map.width
                data["cityName"] = map.cityName
                data["time"] = serializeDate(map.time)
                writeZoneLayer(data, it)
                writeGroundLayer(data, it)
                writeResourceLayers(data, it)
                writeBuildingLayer(data, it)
                writePowerlineLayer(data, it)
                writeDesirabilityLayers(data, it)
                writeContracts(data, it)
                data
            }

            deserialize {
                /*
                    it.type: Type to deserialize to
                    it.context: GSon context
                    it.json : JsonElement to deserialize from
                */
                val cityMap = CityMap(512, 512)
                val data = it.json.asJsonObject
                cityMap.height = data["height"].asInt
                cityMap.width = data["width"].asInt
                cityMap.cityName = data["cityName"].asString
                cityMap.time = parseDate(data["time"].asString)
                println("Reading ground tiles...")
                readGroundTiles(data, cityMap)
                println("Reading building layer...")
                readBuildingLayer(data, cityMap)
                println("Updating building index...")
                cityMap.updateBuildingIndex()
                println("Reading power line layer...")
                readPowerlineLayer(data, cityMap)
                println("Reading zone layer...")
                readZoneLayer(data, cityMap)
                println("Reading resource layers...")
                readResourceLayers(data, cityMap)
                println("Reading desirability layers...")
                readDesirabilityLayers(data, cityMap)
                println("Reading contracts...")
                readContracts(data, cityMap)

                println("Updating building index...")
                cityMap.updateBuildingIndex()
                println("Updating outside connections...")
                cityMap.updateOutsideConnections()

                // now let's force tick power...
                println("Forcing power to tick...")
                PowerCoverageUpdater.update(cityMap)

                cityMap
            }

            createInstances {
                CityMap(512, 512)
            }
        }
        .setPrettyPrinting()
        .create()

    private fun readResourceLayers(data: JsonObject, cityMap: CityMap) {
        data["resourceLayers"]?.asJsonObject?.forEach { layerName, jsonElement ->
            // ok now that element is an array...
            (jsonElement as JsonArray).forEach {
                val x = it["x"].asInt
                val y = it["y"].asInt
                val value = it["value"].asDouble
                cityMap.setResourceValue(layerName, BlockCoordinate(x, y), value)
            }
        }
    }

    private fun readZoneLayer(data: JsonObject, cityMap: CityMap) {
        data["zoneLayer"]?.asJsonArray?.forEach {
            val zoneObj = it.asJsonObject
            val x = zoneObj["x"].asInt
            val y = zoneObj["y"].asInt
            val type = Zone.valueOf(zoneObj["type"].asString)
            cityMap.zoneLayer[BlockCoordinate(x, y)] = type
        }
    }

    private fun readPowerlineLayer(data: JsonObject, cityMap: CityMap) {
        data["powerLineLayer"].asJsonArray.forEach {
            val x = it["x"].asInt
            val y = it["y"].asInt
            cityMap.powerLineLayer[BlockCoordinate(x, y)] = PowerLine()
        }
    }

    private fun writeDesirabilityLayers(data: JsonObject, it: SerializerArg<CityMap>) {
        data["desirabilityLayers"] = it.src.desirabilityLayers.map { desirabilityLayer ->
            jsonObject(
                "type" to desirabilityLayer.zoneType.toString(),
                "level" to desirabilityLayer.level,
                "values" to desirabilityLayer.entries().map {
                    jsonObject(
                        "x" to it.key.x,
                        "y" to it.key.y,
                        "value" to it.value
                    )
                }.toJsonArray()
            )
        }.toJsonArray()
    }

    private fun readDesirabilityLayers(data: JsonObject, cityMap: CityMap) {
        data["desirabilityLayers"].asJsonArray.forEach {
            val layerObj = it.asJsonObject
            val type = Zone.valueOf(layerObj["type"].asString)
            val level = layerObj["level"].asInt
            cityMap.desirabilityLayer(type, level)?.let { desirabilityLayer ->
                layerObj["values"].asJsonArray.forEach {
                    val x = it["x"].asInt
                    val y = it["y"].asInt
                    val value = it["value"].asDouble
                    desirabilityLayer[BlockCoordinate(x, y)] = value
                }
            }
        }
    }

    private fun readBuildingLayer(data: JsonObject, cityMap: CityMap) {
        val assetManager = AssetManager(cityMap)
        data["buildingLayer"].asJsonArray.forEach {
            val buildingObj = it.asJsonObject
            val x = it["x"].asInt
            val y = it["y"].asInt
            val type = Building.classByString(buildingObj["type"].nullString)
            val name = buildingObj["name"]?.asString

            if (name != null && type != null) {
                val building = assetManager.buildingFor(type, name)
                cityMap.build(building, BlockCoordinate(x, y), updateBuildingIndex = false)
            } else {
                val building = when (type) {
                    Road::class -> Road()
                    PowerPlant::class -> PowerPlant(it["variety"].asString)
                    Railroad::class -> Railroad()
                    RailDepot::class -> RailDepot()
                    TrainStation::class -> TrainStation()
                    else -> {
                        debug { "Unknown building: $it" }; null
                    }
                }
                if (building != null) {
                    cityMap.build(building, BlockCoordinate(x, y), updateBuildingIndex = false)
                }
            }
        }
        cityMap.updateBuildingIndex()
    }

    private fun readGroundTiles(data: JsonObject, cityMap: CityMap) {
        val groundTiles = data["groundLayer"].asJsonArray
        println("The file has this many tiles: ${groundTiles.count()}")
        groundTiles.forEach {
            val tileObj = it.asJsonObject
            val coordinate = BlockCoordinate(tileObj["x"].asInt, tileObj["y"].asInt)
            val tileType = TileType.valueOf(tileObj["type"].asString)
            val tile = MapTile(tileType, tileObj["elevation"].asDouble)
            cityMap.groundLayer[coordinate] = tile
        }
        println("All done reading in tiles!")
    }

    private fun writePowerlineLayer(data: JsonObject, it: SerializerArg<CityMap>) {
        data["powerLineLayer"] = it.src.powerLineLayer.map { entry ->
            val x = entry.key.x
            val y = entry.key.y
            jsonObject("x" to x, "y" to y)
        }.toJsonArray()
    }

    private fun readContracts(data: JsonObject, cityMap: CityMap) {
        if (data.has("contracts")) {
            val pathfinder = Pathfinder(cityMap)
            val contractData = data["contracts"].asJsonArray
            contractData.forEach { contractElement ->
                val contractObj = contractElement.asJsonObject
                // the first building MAY not be correct but let's try...
                val from = BlockCoordinate(contractObj["from_x"].asInt, contractObj["from_y"].asInt)
                val to = BlockCoordinate(contractObj["to_x"].asInt, contractObj["to_y"].asInt)
                val fromBuildings = cityMap.cachedLocationsIn(from)
                val toBuildings = cityMap.cachedLocationsIn(to)

                if (fromBuildings.count() == 0) {
                    println("Error during contact loading! Cannot find source building at $from")
                    return@forEach
                }
                if (toBuildings.count() == 0) {
                    println("Error during contact loading! Cannot find source building at $to")
                    return@forEach
                }

                val fromBuilding = fromBuildings.first()
                val toBuilding = toBuildings.first()
                val tradeable = Tradeable.valueOf(contractObj["tradeable"].asString)
                val quantity = contractObj["quantity"].asInt

                val path = pathfinder.tripTo(listOf(from), listOf(to))

                if (path == null) {
                    println("Error during contract loading! Can't find path!")
                    return@forEach
                }

                fromBuilding.building.createContract(
                        fromBuilding.coordinate,
                    CityTradeEntity(
                        toBuilding.coordinate,
                        toBuilding.building
                    ), tradeable, quantity, path
                )
            }
        }
    }

    private fun writeContracts(data: JsonObject, it: SerializerArg<CityMap>) {
        if (it.src.locations().count() > 0) {
            data["contracts"] = it.src.locations().mapNotNull { location ->
                val building = location.building
                if (building.contracts.count() == 0) {
                    null
                } else {
                    building.contracts.mapNotNull { contract ->
                        if (contract.from is CityTradeEntity && contract.to is CityTradeEntity) {
                            jsonObject(
                                "to_x" to contract.to.coordinate.x,
                                "to_y" to contract.to.coordinate.y,
                                "from_x" to contract.from.coordinate.x,
                                "from_y" to contract.from.coordinate.y,
                                "tradeable" to contract.tradeable.toString(),
                                "quantity" to contract.quantity
                            )
                        } else {
                            println("We are trading with outside... not writing!")
                            null
                        }
                    }.toJsonArray()
                }
            }.flatten().toJsonArray()
        }
    }

    private fun writeBuildingLayer(data: JsonObject, it: SerializerArg<CityMap>) {
        data["buildingLayer"] = it.src.locations().map { location ->
            val x = location.coordinate.x
            val y = location.coordinate.y
            val building = location.building
            val type = building::class.simpleName

            val buildingObj = mutableMapOf(
                "x" to x,
                "y" to y,
                "type" to type
            )

            building.name?.let {
                buildingObj["name"] = it
            }

            building.description?.let {
                buildingObj["description"] = it
            }

            building.variety?.let {
                buildingObj["variety"] = it
            }

            jsonObject(buildingObj.map { Pair(it.key, it.value) })
        }.toJsonArray()
    }

    private fun writeResourceLayers(data: JsonObject, it: SerializerArg<CityMap>) {
        val resourceLayers = JsonObject()

        it.src.resourceLayers.forEach { name, layer ->

            println("How many values in the resource cityMap? -> " + layer.count())
            val values = layer.map {
                val resource = JsonObject()
                // println("OK... dumping ${it.key.x},${it.key.y} with ${it.value}")
                resource.putAll(
                    mapOf(
                        "x" to it.key.x,
                        "y" to it.key.y,
                        "value" to it.value
                    )
                )
                resource
            }

            resourceLayers.put(Pair(name, values.toJsonArray()))
        }

        data["resourceLayers"] = resourceLayers
    }

    private fun writeGroundLayer(data: JsonObject, it: SerializerArg<CityMap>) {
        data["groundLayer"] = it.src.groundLayer.map { entry ->
            val x = entry.key.x
            val y = entry.key.y
            jsonObject(
                "x" to x,
                "y" to y,
                "elevation" to entry.value.elevation,
                "type" to entry.value.type.toString()
            )
        }.toJsonArray()
    }

    private fun writeZoneLayer(data: JsonObject, it: SerializerArg<CityMap>) {
        data["zoneLayer"] = it.src.zoneLayer.map { entry ->
            val x = entry.key.x
            val y = entry.key.y
            val zone = entry.value
            jsonObject(
                "type" to zone.toString(),
                "x" to x,
                "y" to y
            )
        }.toJsonArray()
    }

    fun save(map: CityMap, file: File): Boolean {
        val newFile = if (!file.name.contains(".")) {
            File(file.absolutePath + ".kcity")
        } else {
            file
        }
        println("Want to save $map to $newFile")
        val cityJson = gson.toJson(map)
        if (cityJson != null) {
            newFile.writeBytes(GzipUtil.compress(cityJson))
        } else {
            return false
        }
        println("Saved successfully!")
        return true
    }

    fun load(file: File): CityMap {
        println("Want to load cityMap from $file")
        val bytes = GzipUtil.uncompress(file.readBytes())
        val city = gson.fromJson<CityMap>(bytes)
        city.fileName = file.absoluteFile.toString()
        return city
    }
}
