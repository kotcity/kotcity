package kotcity.data

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