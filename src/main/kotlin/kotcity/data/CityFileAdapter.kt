package kotcity.data

import GzipUtil
import com.github.salomonbrys.kotson.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotcity.data.assets.AssetManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")


fun parseDate(str: String): Date {
    simpleDateFormat.timeZone = TimeZone.getDefault()
    return simpleDateFormat.parse(str)
}

fun serializeDate(date: Date): String {
    simpleDateFormat.timeZone = TimeZone.getDefault()
    return simpleDateFormat.format(date)
}

val gson = GsonBuilder()
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
                readGroundTiles(data, cityMap)
                readBuildingLayer(data, cityMap)
                cityMap.updateBuildingIndex()
                readPowerlineLayer(data, cityMap)
                readZoneLayer(data, cityMap)
                readResourceLayers(data, cityMap)
                readDesirabilityLayers(data, cityMap)
                readContracts(data, cityMap)

                cityMap.updateBuildingIndex()

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
        var zoneObj = it.asJsonObject
        val x = zoneObj["x"].asInt
        val y = zoneObj["y"].asInt
        val type = ZoneType.valueOf(zoneObj["type"].asString)
        cityMap.zoneLayer[BlockCoordinate(x, y)] = Zone(type)
    }
}

private fun readPowerlineLayer(data: JsonObject, cityMap: CityMap) {
    data["powerLineLayer"].asJsonArray.forEach {
        var x = it["x"].asInt
        var y = it["y"].asInt
        cityMap.powerLineLayer[BlockCoordinate(x, y)] = PowerLine(cityMap)
    }
}

fun writeDesirabilityLayers(data: JsonObject, it: SerializerArg<CityMap>) {
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
        val type = ZoneType.valueOf(layerObj["type"].asString)
        val level = layerObj["level"].asInt
        cityMap.desirabilityLayer(type, level)?.let { desirabilityLayer ->
            layerObj["values"].asJsonArray.forEach {
                val x = it["x"].asInt
                val y = it["y"].asInt
                val value = it["value"].asDouble
                desirabilityLayer.put(BlockCoordinate(x, y), value)
            }
        }
    }
}

private fun readBuildingLayer(data: JsonObject, cityMap: CityMap) {
    val assetManager = AssetManager(cityMap)
    data["buildingLayer"].asJsonArray.forEach {
        val buildingObj = it.asJsonObject
        var x = it["x"].asInt
        var y = it["y"].asInt
        val type = BuildingType.valueOf(buildingObj["type"].asString)
        val name = buildingObj["name"]?.asString

        if (name != null) {
            val building = when (type) {
                BuildingType.RESIDENTIAL -> assetManager.buildingFor(type, name)
                BuildingType.COMMERCIAL -> assetManager.buildingFor(type, name)
                BuildingType.INDUSTRIAL -> assetManager.buildingFor(type, name)
                BuildingType.CIVIC -> assetManager.buildingFor(type, name)
                else -> throw RuntimeException("Unknown named building: $name")
            }
            cityMap.buildingLayer[BlockCoordinate(x, y)] = building
        } else {
            val building = when (type) {
                BuildingType.ROAD -> Road(cityMap)
                BuildingType.POWER_PLANT -> PowerPlant(it["variety"].asString, cityMap)

                else -> throw RuntimeException("Unknown building: $it")
            }
            cityMap.buildingLayer[BlockCoordinate(x, y)] = building
        }

    }
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
}

private fun writePowerlineLayer(data: JsonObject, it: SerializerArg<CityMap>) {
    data["powerLineLayer"] = it.src.powerLineLayer.map { entry ->
        val x = entry.key.x
        val y = entry.key.y
        jsonObject("x" to x, "y" to y)
    }.toJsonArray()
}

fun readContracts(data: JsonObject, cityMap: CityMap) {
    val contractData = data["contracts"].asJsonArray
    contractData.forEach { contractElement ->
        val contractObj = contractElement.asJsonObject
        // the first building MAY not be correct but let's try...
        val from = BlockCoordinate(contractObj["from_x"].asInt, contractObj["from_y"].asInt)
        val to = BlockCoordinate(contractObj["to_x"].asInt, contractObj["to_y"].asInt)
        val fromBuilding = cityMap.buildingsIn(from).first()
        val toBuilding = cityMap.buildingsIn(to).first()
        val tradeable = Tradeable.valueOf(contractObj["tradeable"].asString)
        val quantity = contractObj["quantity"].asInt
        // val newContract = Contract(fromBuilding, toBuilding, tradeable, quantity)
        fromBuilding.building.createContract(toBuilding.building, tradeable, quantity)
    }
}

fun writeContracts(data: JsonObject, it: SerializerArg<CityMap>) {
    if (it.src.buildingLayer.keys.count() > 0) {
        data["contracts"] = it.src.buildingLayer.mapNotNull { entry->
            val building = entry.value
            if (building.contracts.count() == 0) {
                null
            } else {
                building.contracts.map { contract ->
                    jsonObject(
                            "to_x" to contract.to.coordinate.x,
                            "to_y" to contract.to.coordinate.y,
                            "from_x" to contract.from.coordinate.x,
                            "from_y" to contract.from.coordinate.y,
                            "tradeable" to contract.tradeable.toString(),
                            "quantity" to contract.quantity
                    )
                }.toJsonArray()
            }
        }.flatten().toJsonArray()
    }
}

private fun writeBuildingLayer(data: JsonObject, it: SerializerArg<CityMap>) {
    data["buildingLayer"] = it.src.buildingLayer.map { entry ->
        val x = entry.key.x
        val y = entry.key.y
        val building = entry.value
        val type = building.type.toString()

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

        println("How many values in the resource map? -> " + layer.count())
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
                "type" to zone.type.toString(),
                "x" to x,
                "y" to y
        )
    }.toJsonArray()
}

object CityFileAdapter {

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
        println("Want to load map from $file")
        val bytes = GzipUtil.uncompress(file.readBytes())
        val city = gson.fromJson<CityMap>(bytes)
        city.fileName = file.absoluteFile.toString()
        return city
    }

}