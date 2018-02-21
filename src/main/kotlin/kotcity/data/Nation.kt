package kotcity.data

val outsideContracts: MutableList<Contract> = mutableListOf()

// all the outside shares one contract list...
data class OutsideTradeEntity(private val nationalTradeEntity: NationalTradeEntity, override val coordinate: BlockCoordinate, val cityMap: CityMap) : TradeEntity, HasContacts by nationalTradeEntity, HasInventory by nationalTradeEntity {

    override fun createContract(otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int) {
        val newContract = Contract(this, otherTradeEntity, tradeable, quantity)
        if (otherTradeEntity.quantityForSale(tradeable) >= newContract.quantity) {
            outsideContracts.add(newContract)
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

}

data class NationalTradeEntity(override val cityMap: CityMap): HasConcreteContacts, HasInventory {
    override fun balance(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addInventory(tradeable: Tradeable, quantity: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setInventory(tradeable: Tradeable, quantity: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subtractInventory(tradeable: Tradeable, quantity: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun summarizeInventory(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun quantityOnHand(tradeable: Tradeable): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun transferInventory(to: TradeEntity, tradeable: Tradeable, quantity: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createContract(otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val contracts: MutableList<Contract> = mutableListOf()
    override val consumes: MutableMap<Tradeable, Int> = mutableMapOf()
    override val produces: MutableMap<Tradeable, Int> = mutableMapOf()

    fun resetCounts() {
        // gotta read population from citymap...
        val population = cityMap.censusTaker.population
    }

    fun outsideEntity(coordinate: BlockCoordinate): TradeEntity {
        return OutsideTradeEntity(this, coordinate, cityMap)
    }
}