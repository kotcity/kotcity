package kotcity.data

import kotcity.util.getRandomElement

interface HasInventory {
    fun balance(): Int
    fun addInventory(tradeable: Tradeable, quantity: Int): Int
    fun setInventory(tradeable: Tradeable, quantity: Int): Int
    fun subtractInventory(tradeable: Tradeable, quantity: Int): Int
    fun summarizeInventory(): String
    fun quantityOnHand(tradeable: Tradeable): Int
    fun transferInventory(to: TradeEntity, tradeable: Tradeable, quantity: Int): Int
}

interface HasConcreteInventory : HasInventory {
    val inventory: Inventory

    override fun balance(): Int {
        return inventory.quantity(Tradeable.MONEY)
    }

    override fun addInventory(tradeable: Tradeable, quantity: Int): Int {
        return inventory.add(tradeable, quantity)
    }

    override fun setInventory(tradeable: Tradeable, quantity: Int): Int {
        return inventory.put(tradeable, quantity)
    }

    override fun subtractInventory(tradeable: Tradeable, quantity: Int): Int {
        if (inventory.has(tradeable, quantity)) {
            return inventory.subtract(tradeable, quantity)
        }
        return 0
    }

    override fun summarizeInventory(): String {
        val inventoryBuffer = StringBuffer()
        inventory.forEach { tradeable, qty ->
            inventoryBuffer.append("Has $qty $tradeable\n")
        }
        return inventoryBuffer.toString()
    }

    override fun quantityOnHand(tradeable: Tradeable): Int {
        return inventory.quantity(tradeable)
    }

    override fun transferInventory(to: TradeEntity, tradeable: Tradeable, quantity: Int): Int {
        if (inventory.has(tradeable, quantity)) {
            inventory.subtract(tradeable, quantity)
            to.building()?.addInventory(tradeable, quantity)
            return quantity
        }
        return 0
    }
}

interface HasContacts {
    fun summarizeContracts(): String
    fun quantityForSale(tradeable: Tradeable): Int
    fun quantityWanted(tradeable: Tradeable): Int
    fun addContract(contract: Contract)
    fun createContract(otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int)
    fun voidContractsWith(otherEntity: TradeEntity)
    fun needs(tradeable: Tradeable): Int
    fun voidRandomContract()
    fun totalProvided(tradeable: Tradeable): Int
    fun supplyCount(tradeable: Tradeable): Int
    fun productList(): List<Tradeable>
    fun wantsHowMany(tradeable: Tradeable): Int
}

interface HasConcreteContacts : HasContacts {

    val contracts: MutableList<Contract>
    val consumes: MutableMap<Tradeable, Int>
    val produces: MutableMap<Tradeable, Int>
    val cityMap: CityMap

    override fun summarizeContracts(): String {
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

    override fun quantityForSale(tradeable: Tradeable): Int {
        val filter = {contract: Contract -> contract.from.building() }
        val hash = produces
        return calculateAvailable(hash, tradeable, filter)
    }

    override fun quantityWanted(tradeable: Tradeable): Int {
        val inventoryCount = consumes[tradeable] ?: 0
        val contractCount = contracts.filter { it.to.building() == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return inventoryCount - contractCount
    }

    private fun calculateAvailable(hash: MutableMap<Tradeable, Int>, tradeable: Tradeable, filter: (Contract) -> Building?): Int {
        val inventoryCount = hash[tradeable] ?: 0
        val contractCount = contracts.filter { filter(it) == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return inventoryCount - contractCount
    }

    override fun addContract(contract: Contract) {
        this.contracts.add(contract)
    }

    override fun voidContractsWith(otherEntity: TradeEntity) {
        contracts.removeAll {
            it.to == otherEntity || it.from == otherEntity
        }
    }

    override fun needs(tradeable: Tradeable): Int {
        val requiredCount = consumes[tradeable] ?: return 0
        val contractCount = contracts.filter { it.to.building() == this && it.tradeable == tradeable }.map { it.quantity }.sum()
        return requiredCount - contractCount
    }

    override fun voidRandomContract() {
        if (contracts.count() > 0) {
            val contractToKill = contracts.getRandomElement()
            val otherBuilding = contractToKill.to
            voidContractsWith(otherBuilding)
        }
    }

    override fun totalProvided(tradeable: Tradeable): Int {
        return contracts.filter { it.from.building() == this && it.tradeable == tradeable}
                .map { it.quantity }.sum()
    }

    override fun supplyCount(tradeable: Tradeable): Int {
        return contracts.filter { it.to.building() == this && it.tradeable == tradeable }.map { it.quantity }.sum()
    }

    override fun productList(): List<Tradeable> {
        return produces.keys.distinct()
    }

    override fun wantsHowMany(tradeable: Tradeable): Int {
        return needs(tradeable)
    }
}


abstract class Building(override val cityMap: CityMap) : HasConcreteInventory, HasConcreteContacts {
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

    override val consumes: MutableMap<Tradeable, Int> = mutableMapOf()
    override val produces: MutableMap<Tradeable, Int> = mutableMapOf()
    override val inventory: Inventory = Inventory()
    override val contracts: MutableList<Contract> = mutableListOf()

    init {
        // everyone gets 10 dollars...
        addInventory(Tradeable.MONEY, DEFAULT_MONEY)
    }

    override fun createContract(otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int) {
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

    fun payWorkers() {
        val workContracts = contracts.filter { it.to.building() == this && it.tradeable == Tradeable.LABOR }
        workContracts.forEach { contract ->
            if (inventory.has(Tradeable.MONEY, 1)) {
                transferInventory(contract.from, Tradeable.MONEY, 1)
            }
        }
    }

}

const val DEFAULT_MONEY = 10
val POWER_PLANT_TYPES = listOf("coal", "nuclear")

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
