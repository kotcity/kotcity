package kotcity.data

import kotcity.pathfinding.Path

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
    fun createContract(otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int, path: Path)
    fun voidContractsWith(otherTradeEntity: TradeEntity)
    fun hasAnyContracts(): Boolean

    fun currentQuantityForSale(tradeable: Tradeable): Int
    fun currentQuantityWanted(tradeable: Tradeable): Int

    fun producesQuantity(tradeable: Tradeable): Int
    fun consumesQuantity(tradeable: Tradeable): Int
}

data class CityTradeEntity(override val coordinate: BlockCoordinate, val building: Building) : TradeEntity {
    override fun hasAnyContracts(): Boolean {
        return building.hasAnyContracts()
    }

    override fun createContract(otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int, path: Path) {
        building.createContract(otherTradeEntity, tradeable, quantity, path)
    }

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

    override fun producesQuantity(tradeable: Tradeable): Int {
        return building.producesQuantity(tradeable)
    }

    override fun consumesQuantity(tradeable: Tradeable): Int {
        return building.consumesQuantity(tradeable)
    }

    override fun currentQuantityForSale(tradeable: Tradeable): Int {
        return building.currentQuantityForSale(tradeable)
    }

    override fun currentQuantityWanted(tradeable: Tradeable): Int {
        return building.currentQuantityWanted(tradeable)
    }
}

data class Contract(
        val from: TradeEntity,
        val to: TradeEntity,
        val tradeable: Tradeable,
        val quantity: Int,
        val path: kotcity.pathfinding.Path?

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