package kotcity.data

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

    fun done(): Boolean {
        if (powerAvailable <= 0) {
            return true
        }
        return openList.isEmpty()
    }

    fun tick() {
        if (done()) {
            return
        }
        val activeCoord = openList.first()
        openList.remove(activeCoord)
        powerAvailable -= 1

        gridMap[activeCoord] = this

        // get neighbors...
        val neighbors = activeCoord.neighbors(radius = 3)
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