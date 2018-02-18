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
    RAW_MATERIALS,
    WHOLESALE_GOODS
}

data class Contract(
        val to: Building,
        val from: Building,
        val tradeable: Tradeable,
        val quantity: Int


) {
    override fun toString(): String {
        return "Contract(to=${to.name} from=${from.name} tradeable=$tradeable quantity=$quantity)"
    }
}

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

    fun summarizeContracts(): String {
        val summaryBuffer = StringBuffer()

        val tradeables = consumes.keys.distinct()
        tradeables.forEach {
            summaryBuffer.append("Consumes: ${consumes[it]} $it\n")
        }

        produces.keys.distinct().forEach {
            summaryBuffer.append("Produces: ${produces[it]} $it\n")
        }

        contracts.forEach {
            // summaryBuffer.append(it.toString() + "\n")
            if (it.to == this) {
                summaryBuffer.append("Receiving ${it.quantity} ${it.tradeable} from ${it.from.name}\n")
            }
            if (it.from == this) {
                summaryBuffer.append("Sending ${it.quantity} ${it.tradeable} to ${it.to.name}\n")
            }
        }
        return summaryBuffer.toString()
    }

    fun quantityForSale(tradeable: Tradeable): Int {
        val filter = {contract: Contract -> contract.from }
        val hash = produces
        return calculateAvailable(hash, tradeable, filter)
    }

    fun quantityWanted(tradeable: Tradeable): Int {
        val inventoryCount = consumes[tradeable] ?: 0
        val contractCount = contracts.filter { it.to == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return inventoryCount - contractCount
    }

    private fun calculateAvailable(hash: MutableMap<Tradeable, Int>, tradeable: Tradeable, filter: (Contract) -> Building): Int {
        val inventoryCount = hash[tradeable] ?: 0
        val contractCount = contracts.filter { filter(it) == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return inventoryCount - contractCount
    }

    private fun addContract(contract: Contract) {
        this.contracts.add(contract)
    }

    fun createContract(otherBuilding: Building, tradeable: Tradeable, quantity: Int) {
        val newContract = Contract(this, otherBuilding, tradeable, quantity)
        // TODO: check to make sure we actually have this amount...
        if (otherBuilding.quantityForSale(tradeable) >= newContract.quantity) {
            contracts.add(newContract)
            otherBuilding.addContract(newContract)
        } else {
            println("Tried to make an invalid contract: $newContract but failed because ${this.name} doesn't have enough $tradeable")
        }
    }

    fun voidContractsWith(otherBuilding: Building, reciprocate: Boolean = true) {
        contracts.removeAll {
            it.to == otherBuilding || it.from == otherBuilding
        }
        if (reciprocate) {
            otherBuilding.voidContractsWith(this, false)
        }
    }

    fun needs(tradeable: Tradeable): Int {
        val requiredCount = consumes[tradeable] ?: return 0
        println("${this.name} requires $requiredCount $tradeable")
        val contractCount = contracts.filter { it.to == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        println("Same building has $contractCount $tradeable being received")
        val balance = requiredCount - contractCount
        println("That means we need $balance $tradeable")
        return balance
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
