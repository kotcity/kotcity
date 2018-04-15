package kotcity.data

import kotcity.data.buildings.Building
import kotcity.pathfinding.Path
import kotcity.util.Debuggable

enum class Tradeable {
    MONEY,
    GOODS,
    LABOR,
    RAW_MATERIALS,
    WHOLESALE_GOODS
}

object Prices {
    fun priceForGoods(tradeable: Tradeable, quantity: Int): Int {
        return when (tradeable) {
            Tradeable.MONEY -> quantity * 1
            Tradeable.GOODS -> quantity * 5
            Tradeable.LABOR -> quantity * 1
            Tradeable.RAW_MATERIALS -> quantity * 1
            Tradeable.WHOLESALE_GOODS -> quantity * 3
        }
    }
}

interface TradeEntity {
    fun description(): String?
    fun building(): Building?
    val coordinate: BlockCoordinate

    fun addContract(contract: Contract)
    fun createContract(coordinate: BlockCoordinate, otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int, path: Path)
    fun voidContractsWith(otherTradeEntity: TradeEntity)
    fun hasAnyContracts(): Boolean

    fun currentQuantityForSale(tradeable: Tradeable): Int
    fun currentQuantityWanted(tradeable: Tradeable): Int

    fun producesQuantity(tradeable: Tradeable): Int
    fun consumesQuantity(tradeable: Tradeable): Int
    fun quantityOnHand(tradeable: Tradeable): Int
}

data class CityTradeEntity(override val coordinate: BlockCoordinate, val building: Building) : TradeEntity {
    override fun hasAnyContracts(): Boolean {
        return building.hasAnyContracts()
    }

    override fun quantityOnHand(tradeable: Tradeable): Int {
        return building.quantityOnHand(tradeable)
    }

    override fun createContract(coordinate: BlockCoordinate, otherTradeEntity: TradeEntity, tradeable: Tradeable, quantity: Int, path: Path) {
        building.createContract(coordinate, otherTradeEntity, tradeable, quantity, path)
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

): Debuggable {
    override var debug: Boolean = false

    override fun toString(): String {
        return "Contract(to=${to.description()} from=${from.description()} tradeable=$tradeable quantity=$quantity)"
    }

    fun execute(): Boolean {

        // get the hash order so we can lock deterministically...
        val (first, second) = listOf(from, to).sortedBy { System.identityHashCode(it) }

        synchronized(first) {
            synchronized(second) {
                // here is where we actually do the transfer...
                val quantityWanted = to.currentQuantityWanted(tradeable)
                val quantityAvailable = from.quantityOnHand(tradeable)
                if (quantityAvailable >= quantityWanted) {
                    // we can actually do it...
                    val building = from.building() ?: return false
                    val otherBuilding = to.building() ?: return false

                    debug { "The customer (${otherBuilding.description}) naturally wants this many $tradeable: ${otherBuilding.consumesQuantity(tradeable)}" }
                    // try to put double what the customer wants there....
                    val doubleTradeable = otherBuilding.consumesQuantity(tradeable) * 2
                    debug { "So let's try and make sure they have $doubleTradeable" }
                    val customerWantsQuantity = doubleTradeable - otherBuilding.quantityOnHand(tradeable)
                    debug { "They already have ${otherBuilding.quantityOnHand(tradeable)}" }

                    debug { "So they want $customerWantsQuantity more..." }
                    // ok... pick whatever is least... how many we have in inventory OR how much the other guy wants...
                    val howManyToSend = listOf(customerWantsQuantity, building.quantityOnHand(tradeable)).min() ?: 0

                    if (howManyToSend > 0) {
                        debug { "We have ${building.quantityOnHand(tradeable)} and we will send them $howManyToSend" }
                        debug { "Before transfer... building has $${building.quantityOnHand(Tradeable.MONEY)}" }
                        val howManyTransferred = building.transferInventory(to, tradeable, howManyToSend)
                        building.addInventory(Tradeable.MONEY, Prices.priceForGoods(tradeable, howManyTransferred))
                        debug { "${building.description}: We transferred $howManyToSend $tradeable to ${to.description()}" }
                        debug { "After transfer... building has $${building.quantityOnHand(Tradeable.MONEY)}" }
                    } else {
                        debug { "${building.description } wanted to send $quantity $tradeable but it was out of stock..." }
                        return false
                    }
                }
            }
        }

        return true

    }

    fun void() {

        // get the hash order so we can lock deterministically...
        val (first, second) = listOf(from, to).sortedBy { System.identityHashCode(it) }

        synchronized(first) {
            synchronized(second) {
                first.voidContractsWith(second)
                second.voidContractsWith(first)
            }
        }


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
