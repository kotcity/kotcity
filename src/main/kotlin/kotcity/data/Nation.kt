package kotcity.data

// all the outside shares one contract list...
data class OutsideTradeEntity(private val nationalTradeEntity: NationalTradeEntity, override val coordinate: BlockCoordinate, val cityMap: CityMap) : TradeEntity, HasContracts by nationalTradeEntity, HasInventory by nationalTradeEntity {
    override fun voidContractsWith(otherTradeEntity: TradeEntity) {
        nationalTradeEntity.voidContractsWith(otherTradeEntity)
    }

    override fun addContract(contract: Contract) {
        nationalTradeEntity.addContract(contract)
    }

    override fun createContract(otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int) {
        val newContract = Contract(this, otherTradeEntity, tradeable, quantity)
        if (otherTradeEntity.quantityForSale(tradeable) >= newContract.quantity) {
            addContract(newContract)
            otherTradeEntity.addContract(newContract)
        } else {
            println("Tried to make an invalid contract: $newContract but failed because ${otherTradeEntity.description()} doesn't have enough $tradeable (it has ${otherTradeEntity.quantityForSale(tradeable)})")
        }
    }

    override fun building(): Building? {
        return null
    }

    override fun description(): String? {
        return "Outside the city"
    }

    override fun quantityForSale(tradeable: Tradeable): Int {
        return nationalTradeEntity.quantityForSale(tradeable)
    }

    override fun wantsHowMany(tradeable: Tradeable): Int {
        return nationalTradeEntity.wantsHowMany(tradeable)
    }

}

class TradeableCounter {

    private val counters: MutableMap<Tradeable, Int> = mutableMapOf()

    operator fun set(tradeable: Tradeable, quantity: Int): Int {
        counters[tradeable] = quantity
        return quantity
    }

    operator fun get(tradeable: Tradeable): Int {
        return counters[tradeable] ?: 0
    }

    fun increment(tradeable: Tradeable, quantity: Int): Int {
        counters[tradeable] = quantity + counters.getOrDefault(tradeable, 0)
        return counters.getOrDefault(tradeable, 0)
    }

    fun decrement(tradeable: Tradeable, quantity: Int): Int {
        counters[tradeable] = counters.getOrDefault(tradeable, 0) - quantity
        return counters.getOrDefault(tradeable, 0)
    }
}

data class NationalTradeEntity(val cityMap: CityMap): HasContracts, HasInventory {

    private val contracts: MutableList<Contract> = mutableListOf()
    private val wantsCounter: TradeableCounter = TradeableCounter()
    private val providesCounter: TradeableCounter = TradeableCounter()

    fun resetCounts() {
        // gotta read population from citymap...
        val population = cityMap.censusTaker.population
        listOf(wantsCounter, providesCounter).forEach { counter ->
            Tradeable.values().forEach { tradeable ->
                counter[tradeable] = Math.floor(population * 0.05).toInt()
            }
        }
    }

    override fun quantityForSale(tradeable: Tradeable): Int {
        return providesCounter[tradeable]
    }

    override fun quantityWanted(tradeable: Tradeable): Int {
        return wantsCounter[tradeable]
    }

    override fun needs(tradeable: Tradeable): Int {
        return wantsCounter[tradeable]
    }

    override fun totalProvided(tradeable: Tradeable): Int {
        return providesCounter[tradeable]
    }

    override fun supplyCount(tradeable: Tradeable): Int {
        return providesCounter[tradeable]
    }

    override fun productList(): List<Tradeable> {
        return Tradeable.values().toList()
    }

    override fun wantsHowMany(tradeable: Tradeable): Int {
        return wantsCounter[tradeable]
    }

    override fun summarizeContracts(): String {
        return "I will return demand here..."
    }

    override fun balance(): Int {
        return 10000
    }

    fun outsideEntity(coordinate: BlockCoordinate): TradeEntity {
        return OutsideTradeEntity(this, coordinate, cityMap)
    }

    fun voidContractsWith(otherTradeEntity: TradeEntity) {
        val iterator = contracts.iterator()
        iterator.forEach { contract ->
            if (contract.to == otherTradeEntity || contract.from == otherTradeEntity) {
                contracts.remove(contract)
            }
        }
    }

    fun addContract(contract: Contract) {
        contracts.add(contract)
    }

    override fun addInventory(tradeable: Tradeable, quantity: Int): Int {
        return wantsCounter.increment(tradeable, quantity)
    }

    override fun setInventory(tradeable: Tradeable, quantity: Int): Int {
        return wantsCounter.set(tradeable, quantity)
    }

    override fun subtractInventory(tradeable: Tradeable, quantity: Int): Int {
        return wantsCounter.decrement(tradeable, quantity)
    }

    override fun summarizeInventory(): String {
        val summaryBuffer = StringBuffer()
        Tradeable.values().forEach {
            summaryBuffer.append("Provides: ${providesCounter[it]}\n")
        }
        return summaryBuffer.toString()
    }

    override fun quantityOnHand(tradeable: Tradeable): Int {
        return providesCounter[tradeable]
    }

    override fun transferInventory(to: TradeEntity, tradeable: Tradeable, quantity: Int): Int {
        if (providesCounter[tradeable] >= quantity) {
            providesCounter.decrement(tradeable, quantity)
            to.building()?.addInventory(tradeable, quantity)
            return quantity
        }
        return 0
    }
}