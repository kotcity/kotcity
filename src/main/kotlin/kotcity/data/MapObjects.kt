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

abstract class TradeEntity {
    abstract fun description(): String?
    abstract fun building(): Building?
    abstract val coordinate: BlockCoordinate
    abstract fun quantityForSale(tradeable: Tradeable): Int
    abstract fun addContract(contract: Contract)
}

val outsideContracts: MutableList<Contract> = mutableListOf()

// all the outside shares one contract list...
data class OutsideTradeEntity(override val coordinate: BlockCoordinate) : TradeEntity() {

    override fun addContract(contract: Contract) {
        outsideContracts.add(contract)
    }

    override fun quantityForSale(tradeable: Tradeable): Int {
        return 999
    }

    override fun building(): Building? {
        return null
    }

    override fun description(): String? {
        return "Outside the city"
    }

}

data class CityTradeEntity(override val coordinate: BlockCoordinate, val building: Building) : TradeEntity() {
    override fun addContract(contract: Contract) {
        building.addContract(contract)
    }

    override fun building(): Building? {
        return building
    }

    override fun description(): String? {
        return building.description
    }

    override fun quantityForSale(tradeable: Tradeable): Int {
        return building.quantityForSale(tradeable)
    }
}

data class Contract(
        val to: TradeEntity,
        val from: TradeEntity,
        val tradeable: Tradeable,
        val quantity: Int

) {
    override fun toString(): String {
        return "Contract(to=${to.description()} from=${from.description()} tradeable=$tradeable quantity=$quantity)"
    }
}

data class Location(val coordinate: BlockCoordinate, val building: Building)

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
    open var upkeep: Int = 0

    val consumes: MutableMap<Tradeable, Int> = mutableMapOf()
    val produces: MutableMap<Tradeable, Int> = mutableMapOf()
    private val inventory: Inventory = Inventory()

    val contracts: MutableList<Contract> = mutableListOf()

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
            if (it.to.building() == this) {
                summaryBuffer.append("Receiving ${it.quantity} ${it.tradeable} from ${it.from.description()}\n")
            }
            if (it.from.building() == this) {
                summaryBuffer.append("Sending ${it.quantity} ${it.tradeable} to ${it.to.description()}\n")
            }
        }
        return summaryBuffer.toString()
    }

    fun quantityForSale(tradeable: Tradeable): Int {
        val filter = {contract: Contract -> contract.from.building() }
        val hash = produces
        return calculateAvailable(hash, tradeable, filter)
    }

    fun quantityWanted(tradeable: Tradeable): Int {
        val inventoryCount = consumes[tradeable] ?: 0
        val contractCount = contracts.filter { it.to.building() == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return inventoryCount - contractCount
    }

    private fun calculateAvailable(hash: MutableMap<Tradeable, Int>, tradeable: Tradeable, filter: (Contract) -> Building?): Int {
        val inventoryCount = hash[tradeable] ?: 0
        val contractCount = contracts.filter { filter(it) == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return inventoryCount - contractCount
    }

    internal fun addContract(contract: Contract) {
        this.contracts.add(contract)
    }

    fun createContract(otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int) {
        val ourBlocks = cityMap.coordinatesForBuilding(this)
        if (ourBlocks == null) {
            println("Sorry, we couldn't find one of the buildings!")
            return
        }
        val ourLocation = CityTradeEntity(ourBlocks, this)
        val newContract = Contract(ourLocation, otherTradeEntity, tradeable, quantity)
        if (otherTradeEntity.quantityForSale(tradeable) >= newContract.quantity) {
            contracts.add(newContract)
            otherTradeEntity.addContract(newContract)
        } else {
            println("Tried to make an invalid contract: $newContract but failed because ${otherTradeEntity.description()} doesn't have enough $tradeable (it has ${otherTradeEntity.quantityForSale(tradeable)})")
        }
    }

    fun voidContractsWith(otherBuilding: Building, reciprocate: Boolean = true) {
        contracts.removeAll {
            it.to.building() == otherBuilding || it.from.building() == otherBuilding
        }
        if (reciprocate) {
            otherBuilding.voidContractsWith(this, false)
        }
    }

    fun needs(tradeable: Tradeable): Int {
        val requiredCount = consumes[tradeable] ?: return 0
        val contractCount = contracts.filter { it.to.building() == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return requiredCount - contractCount
    }

    fun voidRandomContract() {
        if (contracts.count() > 0) {
            val contractToKill = contracts.getRandomElement()
            val otherBuilding = contractToKill.to.building()
            if (otherBuilding != null) {
                voidContractsWith(otherBuilding)
            }

        }
    }

    fun supplyCount(tradeable: Tradeable): Int {
        return contracts.filter { it.to.building() == this && it.tradeable == tradeable }.map { it.quantity }.sum()
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

    fun transferInventory(to: TradeEntity, tradeable: Tradeable, quantity: Int): Int {
        if (inventory.has(tradeable, quantity)) {
            inventory.subtract(tradeable, quantity)
            to.building()?.addInventory(tradeable, quantity)
            return quantity
        }
        return 0
    }

    fun payWorkers() {
        val workContracts = contracts.filter { it.to.building() == this && it.tradeable == Tradeable.LABOR }
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
