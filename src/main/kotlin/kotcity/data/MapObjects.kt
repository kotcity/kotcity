package kotcity.data

import kotcity.util.getRandomElement

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

data class Location(val coordinate: BlockCoordinate, val building: Building)

data class Contract(
        val to: Location,
        val from: Location,
        val tradeable: Tradeable,
        val quantity: Int

) {
    override fun toString(): String {
        return "Contract(to=${to.building.description} from=${from.building.description} tradeable=$tradeable quantity=$quantity)"
    }
}

class Inventory {
    private val inventory : MutableMap<Tradeable, Int> = mutableMapOf()

    fun add(tradeable: Tradeable, quantity: Int): Int {
        val onHand = inventory.getOrDefault(tradeable, 0)
        inventory[tradeable] = onHand + quantity
        return inventory[tradeable] ?: 0
    }

    fun subtract(tradeable: Tradeable, quantity: Int): Int {
        val onHand = inventory.getOrDefault(tradeable, 0)
        inventory[tradeable] = onHand - quantity
        return inventory[tradeable] ?: 0
    }

    fun has(tradeable: Tradeable, quantity: Int): Boolean {
        return inventory.getOrDefault(tradeable, 0) >= quantity
    }

    fun quantity(tradeable: Tradeable): Int {
        return inventory.getOrDefault(tradeable, 0)
    }
}

const val DEFAULT_MONEY = 10

abstract class Building(private val cityMap: CityMap) {
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
    val contracts: MutableList<Contract> = mutableListOf()
    private val inventory: Inventory = Inventory()

    init {
        // everyone gets 10 dollars...
        addInventory(Tradeable.MONEY, DEFAULT_MONEY)
    }

    fun balance(): Int {
        return inventory.quantity(Tradeable.MONEY)
    }

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
            if (it.to.building == this) {
                summaryBuffer.append("Receiving ${it.quantity} ${it.tradeable} from ${it.from.building.description}\n")
            }
            if (it.from.building == this) {
                summaryBuffer.append("Sending ${it.quantity} ${it.tradeable} to ${it.to.building.description}\n")
            }
        }
        return summaryBuffer.toString()
    }

    fun quantityForSale(tradeable: Tradeable): Int {
        val filter = {contract: Contract -> contract.from.building }
        val hash = produces
        return calculateAvailable(hash, tradeable, filter)
    }

    fun quantityWanted(tradeable: Tradeable): Int {
        val inventoryCount = consumes[tradeable] ?: 0
        val contractCount = contracts.filter { it.to.building == this && it.tradeable == tradeable }.map { it.quantity }.sum()
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
        val ourBlocks = cityMap.coordinatesForBuilding(this)
        val theirBlocks = cityMap.coordinatesForBuilding(otherBuilding)
        if (ourBlocks == null || theirBlocks == null) {
            println("Sorry, we couldn't find one of the buildings!")
            return
        }
        val ourLocation = Location(ourBlocks, this)
        val theirLocation = Location(theirBlocks, otherBuilding)
        val newContract = Contract(ourLocation, theirLocation, tradeable, quantity)
        if (otherBuilding.quantityForSale(tradeable) >= newContract.quantity) {
            contracts.add(newContract)
            otherBuilding.addContract(newContract)
        } else {
            println("Tried to make an invalid contract: $newContract but failed because ${otherBuilding.name} doesn't have enough $tradeable (it has ${otherBuilding.quantityForSale(tradeable)})")
        }
    }

    fun voidContractsWith(otherBuilding: Building, reciprocate: Boolean = true) {
        contracts.removeAll {
            it.to.building == otherBuilding || it.from.building == otherBuilding
        }
        if (reciprocate) {
            otherBuilding.voidContractsWith(this, false)
        }
    }

    fun needs(tradeable: Tradeable): Int {
        val requiredCount = consumes[tradeable] ?: return 0
        val contractCount = contracts.filter { it.to.building == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return requiredCount - contractCount
    }

    fun voidRandomContract() {
        if (contracts.count() > 0) {
            val contractToKill = contracts.getRandomElement()
            voidContractsWith(contractToKill.to.building)
        }
    }

    fun supplyCount(tradeable: Tradeable): Int {
        return contracts.filter { it.to.building == this && it.tradeable == tradeable }.map { it.quantity }.sum()
    }

    fun productList(): List<Tradeable> {
        return produces.keys.distinct()
    }

    fun addInventory(tradeable: Tradeable, quantity: Int): Int {
        return inventory.add(tradeable, quantity)
    }

    fun subtractInventory(tradeable: Tradeable, quantity: Int): Int {
        if (inventory.has(tradeable, quantity)) {
            return inventory.subtract(tradeable, quantity)
        }
        return 0
    }

    fun transferInventory(to: Location, tradeable: Tradeable, quantity: Int): Int {
        if (inventory.has(tradeable, quantity)) {
            inventory.subtract(tradeable, quantity)
            to.building.addInventory(tradeable, quantity)
            return quantity
        }
        return 0
    }

    fun payWorkers() {
        val workContracts = contracts.filter { it.to.building == this && it.tradeable == Tradeable.LABOR }
        workContracts.forEach { contract ->
            if (inventory.has(Tradeable.MONEY, 1)) {
                transferInventory(contract.from, Tradeable.MONEY, 1)
            }
        }
    }
}

class PowerPlant : Building {

    override val variety: String
    var powerGenerated: Int = 0

    constructor(variety: String, cityMap: CityMap) : super(cityMap) {
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

class Road(cityMap: CityMap) : Building(cityMap) {
    override var width = 1
    override var height = 1
    override var type = BuildingType.ROAD
    override var description: String? = "Road"
}

class PowerLine(cityMap: CityMap) : Building(cityMap) {
    override var type: BuildingType = BuildingType.POWER_LINE
    override var width = 1
    override var height = 1
    override val powerRequired = 1
    override var description: String? = "Power Line"
}

class LoadableBuilding(cityMap: CityMap) : Building(cityMap) {
    var level: Int = 1
    override var height: Int = 1
    override var width: Int = 1
    override lateinit var type: BuildingType
}
