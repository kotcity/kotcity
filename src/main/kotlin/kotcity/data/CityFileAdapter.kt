package kotcity.data

import java.io.File
import com.github.salomonbrys.kotson.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.FileReader


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
                data["buildingLayer"] = it.src.buildingLayer.map { entry ->
                    val x = entry.key.x
                    val y = entry.key.y
                    val type = entry.value.type.toString()
                    jsonObject(
                            "x" to x,
                            "y" to y,
                            "type" to type
                    )
                }.toJsonArray()
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
                val groundTiles = data["groundLayer"].asJsonArray
                println("The file has this many tiles: ${groundTiles.count()}")
                groundTiles.forEach {
                    val tileObj = it.asJsonObject
                    val coordinate = BlockCoordinate(tileObj["x"].asInt, tileObj["y"].asInt)
                    val tileType = TileType.valueOf(tileObj["type"].asString)
                    val tile = MapTile(tileType, tileObj["elevation"].asDouble)
                    cityMap.groundLayer[coordinate] = tile
                    println("Loaded $tile at $coordinate")
                }

                data["buildingLayer"].asJsonArray.forEach {
                    val buildingObj = it.asJsonObject
                    var x = it["x"].asInt
                    var y = it["y"].asInt
                    val type = BuildingType.valueOf(buildingObj["type"].asString)
                    val building = when(type) {
                        BuildingType.ROAD -> Road()
                        BuildingType.COAL_POWER_PLANT -> CoalPowerPlant()
                    }
                    println("Loaded building: $building")
                    cityMap.buildingLayer[BlockCoordinate(x, y)] = building
                }
                cityMap
            }

            createInstances {
                CityMap(512, 512)
            }

        }
        .setPrettyPrinting()
        .create()

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
            newFile.writeText(cityJson)
        } else {
            return false
        }
        println("Saved successfully!")
        return true
    }

    fun load(file: File): CityMap {
        println("Want to load map from $file")
        val city = gson.fromJson<CityMap>(FileReader(file))
        city.fileName = file.absoluteFile.toString()
        return city
    }

}