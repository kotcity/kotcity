package kotcity.data

enum class BuildingType {
    ROAD, RESIDENTIAL, COMMERCIAL, INDUSTRIAL, POWER_LINE, POWER_PLANT, CIVIC
}

enum class ZoneType {
    RESIDENTIAL, COMMERCIAL, INDUSTRIAL
}

val POWER_PLANT_TYPES = listOf("coal", "nuclear")

data class Zone(val type: ZoneType)

enum class Tradeable {
    MONEY,
    GOODS,
    LABOR,
    RAW_MATERIALS
}

data class Contract(
        val to: Building,
        val from: Building,
        val tradeable: Tradeable,
        val quantity: Int
)

abstract class Building {
    abstract var width: Int
    abstract var height: Int
    abstract var type: BuildingType
    open val variety: String? = null
    open var name: String? = null
    open var sprite: String? = null
    open var description: String? = null
    var powered = false
    open val powerRequired = 0
    val consumes: MutableMap<Tradeable, Int> = mutableMapOf()
    val produces: MutableMap<Tradeable, Int> = mutableMapOf()
    open var upkeep: Int = 0
    private val contracts: MutableList<Contract> = mutableListOf()

    fun sellingQuantity(tradeable: Tradeable): Int {
        val filter = {contract: Contract -> contract.from }
        val hash = produces
        return calculateAvailable(hash, tradeable, filter)
    }

    fun buyingQuantity(tradeable: Tradeable): Int {
        val filter = {contract: Contract -> contract.to }
        val hash = consumes
        return calculateAvailable(hash, tradeable, filter)
    }

    private fun calculateAvailable(hash: MutableMap<Tradeable, Int>, tradeable: Tradeable, filter: (Contract) -> Building): Int {
        val inventoryCount = hash[tradeable] ?: 0
        val contractCount = contracts.filter { filter(it) == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return inventoryCount - contractCount
    }

    fun createContract(to: Building, tradeable: Tradeable, quantity: Int) {
        contracts.add(Contract(to, this, tradeable, quantity))
        contracts.add(Contract(this, to, tradeable, -quantity))
    }

    fun voidContractsWith(otherBuilding: Building) {
        contracts.removeAll {
            it.to == otherBuilding || it.from == otherBuilding
        }
    }
}

class Road : Building() {
    override var width = 1
    override var height = 1
    override var type = BuildingType.ROAD
    override var description: String? = "Road"
}

class PowerPlant : Building {

    override val variety: String
    var powerGenerated: Int = 0

    constructor(variety: String) : super() {
        if (!POWER_PLANT_TYPES.contains(variety)) {
            throw RuntimeException("Invalid power plant type: $variety")
        }
        this.variety = variety
        when (variety) {
            "coal" -> { this.powerGenerated = 2000; this.description = "Coal Power Plant" }
            "nuclear" -> { this.powerGenerated = 5000; this.description = "Nuclear Power Plant" }
        }
        this.type = BuildingType.POWER_PLANT
    }

    override var type: BuildingType
    override var width = 4
    override var height = 4
}

class PowerLine : Building() {
    override var type: BuildingType = BuildingType.POWER_LINE
    override var width = 1
    override var height = 1
    override val powerRequired = 1
    override var description: String? = "Power Line"
}

class LoadableBuilding: Building() {
    var level: Int = 1
    override var height: Int = 1
    override var width: Int = 1
    override lateinit var type: BuildingType
}
