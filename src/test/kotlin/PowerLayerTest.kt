import kotcity.data.*
import org.junit.jupiter.api.Test

class PowerLayerTest {
    @Test
    fun testPowerSpread() {
        val map = CityMap(512, 512)
        // set all tiles to ground...
        val xRange = 0 .. map.width
        val yRange = 0 .. map.height
        xRange.map { x ->
            yRange.map { y ->
                map.groundLayer[BlockCoordinate(x, y)] = MapTile(TileType.GROUND, 0.1)
            }
        }

        // now let's drop a coal power plant...
        val powerPlant1 = PowerPlant("coal")
        val powerPlant2 = PowerPlant("coal")

        map.build(powerPlant1, BlockCoordinate(0, 0))
        map.build(powerPlant2, BlockCoordinate(0, 20))

        // ok... that power plant will end at 3,0
        val roadStart = BlockCoordinate(4,0)
        val roadEnd = BlockCoordinate(4, 40)

        map.buildRoad(roadStart, roadEnd)

        PowerCoverageUpdater.update(map)

    }
}

object PowerCoverageUpdater {
    fun update(map: CityMap) {

        // OK we gotta find all the power plants on the map...
        val powerPlants = map.buildingLayer.filter { entry: Map.Entry<BlockCoordinate, Building> ->
            entry.value.type == BuildingType.POWER_PLANT
        }

        val gridmap = mutableMapOf<BlockCoordinate, PowerCoverageAutomata>()

        // now for each power plant we want to start an automata...
        var autoMataIndex = 0

        val automatas = powerPlants.map {
            autoMataIndex += 1
            PowerCoverageAutomata(it.key, it.value as PowerPlant, gridmap, map, autoMataIndex)
        }.toMutableSet()
        while (automatas.any { !it.done() }) {
            automatas.forEach {
                if (!it.done()) {
                    it.tick()
                }
            }
        }

        // ok now let's set all buildings to powered that were in teh grid list...
        map.buildingLayer.forEach { t, u ->
            u.powered = gridmap.containsKey(t)
        }

    }
}

class PowerCoverageAutomata(
        coordinate: BlockCoordinate,
        powerPlant: PowerPlant,
        val gridMap: MutableMap<BlockCoordinate, PowerCoverageAutomata>,
        val map: CityMap,
        val index: Int
) {

    val openList = mutableSetOf<BlockCoordinate>()
    var powerAvailable = 0

    init {
        openList.add(coordinate)
        powerAvailable = powerPlant.powerGenerated
    }

    fun done(): Boolean {
        return openList.isEmpty()
    }

    fun tick() {
        println("Number of open in $this -> ${this.openList.count()}")
        if (done()) {
            return
        }
        val activeCoord = openList.first()
        println("Processing: $activeCoord")
        openList.remove(activeCoord)

        gridMap[activeCoord] = this

        // get neighbors...
        // TODO: add radius...
        val neighbors = activeCoord.neighbors()
        neighbors.forEach {
            // if the block has no power... AND contains buildings
            // add to open list...
            val buildings = map.buildingsIn(it)
            if (buildings.count() > 0 && !gridMap.containsKey(it)) {
                openList.add(it)
            }
            // if it contains a grid that's NOT us we need to gobble it up...
            gridMap[it]?.let { otherAutomata ->
                if (this != otherAutomata) {
                    ingest(otherAutomata)
                }
            }
        }
    }

    private fun ingest(otherAutomata: PowerCoverageAutomata) {
        println("Gotta ingest another automata...")
        // we need to suck up all its open list
        this.openList.addAll(otherAutomata.openList)
        otherAutomata.openList.clear()

        this.powerAvailable += otherAutomata.powerAvailable
        otherAutomata.powerAvailable = 0
        // and set all its blocks to ours...

        val blocksToChange = gridMap.filter {
            it.value != this
        }

        blocksToChange.forEach {
            gridMap[it.key] = this
        }
    }
}
