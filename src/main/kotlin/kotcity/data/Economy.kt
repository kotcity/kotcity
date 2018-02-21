package kotcity.data

enum class Tradeable {
    MONEY,
    GOODS,
    LABOR,
    RAW_MATERIALS,
    WHOLESALE_GOODS
}

interface TradeEntity {
    fun description(): String?
    fun building(): Building?
    val coordinate: BlockCoordinate

    fun addContract(contract: Contract)
    fun voidContractsWith(otherTradeEntity: TradeEntity)

    fun quantityForSale(tradeable: Tradeable): Int
    fun wantsHowMany(tradeable: Tradeable): Int
}

data class CityTradeEntity(override val coordinate: BlockCoordinate, val building: Building) : TradeEntity {

    override fun voidContractsWith(otherTradeEntity: TradeEntity) {
        building.voidContractsWith(otherTradeEntity)
    }

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

    override fun wantsHowMany(tradeable: Tradeable): Int {
        return building.wantsHowMany(tradeable)
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

    fun forEach(action: (Tradeable, Int) -> Unit) {
        inventory.forEach { entry ->
            action(entry.key, entry.value)
        }
    }

    fun put(tradeable: Tradeable, quantity: Int): Int {
        return inventory.put(tradeable, quantity) ?: 0
    }
}