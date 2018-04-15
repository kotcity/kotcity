package kotcity.automata

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.buildings.PowerPlant

class PowerCoverageAutomata(
        coordinate: BlockCoordinate,
        powerPlant: PowerPlant,
        private val gridMap: MutableMap<BlockCoordinate, PowerCoverageAutomata>,
        val map: CityMap
) {

    private val openList = mutableSetOf<BlockCoordinate>()
    private var powerAvailable = 0

    init {
        openList.add(coordinate)
        powerAvailable = powerPlant.powerGenerated
    }

    fun done() = if (powerAvailable > 0) openList.isEmpty() else true

    fun tick() {
        if (done()) {
            return
        }
        val activeBlock = openList.first()
        openList.remove(activeBlock)
        powerAvailable -= 1

        gridMap[activeBlock] = this

        // get neighbors...
        val neighbors = activeBlock.neighbors(radius = 3)
        neighbors.forEach { block ->
            // if the block has no power... AND contains buildings
            // add to open list...
            val buildings = map.cachedLocationsIn(block)
            if (buildings.count() > 0 && !gridMap.containsKey(block)) {
                openList.add(block)
            }
            // if it contains a grid that's NOT us we need to gobble it up...
            gridMap[block]?.let { otherAutomata ->
                if (this != otherAutomata) {
                    ingest(otherAutomata)
                }
            }
        }
    }

    private fun ingest(otherAutomata: PowerCoverageAutomata) {
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
