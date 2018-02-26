package kotcity.automata

import kotcity.data.*
import kotcity.util.Debuggable

class Shipper(val cityMap: CityMap): Debuggable {

    override var debug = false

    private fun priceForGoods(tradeable: Tradeable, quantity: Int): Int {
        return when (tradeable) {
            Tradeable.MONEY -> quantity * 1
            Tradeable.GOODS -> quantity * 2
            Tradeable.LABOR -> quantity * 1
            Tradeable.RAW_MATERIALS -> quantity * 1
            Tradeable.WHOLESALE_GOODS -> quantity * 2
        }
    }

    //

    fun tick() {
        // what we want to do here is find all industrial zones with WHOLESALE GOODS and ship them to commercial zones
        cityMap.buildingLayer.forEach { _, building ->
            building.contracts.forEach { contract ->
                // we only want to deal with "to" and not to ourself...
                // we also don't SEND labor

                // THIS TOP CASE is building SENDING something to another building
                if (contract.to.building() != building && contract.tradeable != Tradeable.LABOR) {
                    exportToCity(contract, building)
                }

                // THIS IS THE OUTSIDE PULLING A QUANTITY
                if (contract.from.building() == building && contract.to is OutsideTradeEntity) {
                    // how many can we push down?
                    // TODO: not sure if this is right...
                    exportToOutside(contract, building)
                }
            }
        }
    }

    private fun exportToOutside(contract: Contract, building: Building) {
        if (contract.to.quantityWanted(contract.tradeable) >= contract.quantity) {
            if (contract.tradeable == Tradeable.LABOR) {
                // don't actually SEND anything... we just get $$$
                building.addInventory(Tradeable.MONEY, contract.quantity)
                debug("${building.description} sent some ${contract.tradeable} to ${contract.to.description()}")
            } else if (contract.tradeable != Tradeable.MONEY) {
                val howManyTransferred = building.transferInventory(contract.to, contract.tradeable, contract.quantity)
                building.addInventory(Tradeable.MONEY, priceForGoods(contract.tradeable, howManyTransferred))
                debug("${building.description}: We sent ${contract.quantity} ${contract.tradeable} to ${contract.to.description()}")
            }
        }
    }

    private fun exportToCity(contract: Contract, building: Building) {

        val tradeable = contract.tradeable
        val otherBuilding = contract.to.building() ?: return

        debug("The customer (${otherBuilding.description}) naturally wants this many $tradeable: ${otherBuilding.consumesQuantity(tradeable)}")
        // try to put double what the customer wants there....
        val doubleTradeable = otherBuilding.consumesQuantity(tradeable) * 2
        debug("So let's try and make sure they have $doubleTradeable")
        val customerWantsQuantity = doubleTradeable - otherBuilding.quantityOnHand(tradeable)
        debug("They already have ${otherBuilding.quantityOnHand(tradeable)}")

        debug("So they want $customerWantsQuantity more...")
        // ok... pick whatever is least... how many we have in inventory OR how much the other guy wants...
        val howManyToSend = listOf(customerWantsQuantity, building.quantityOnHand(tradeable)).min() ?: 0

        if (howManyToSend > 0) {
            debug("We have ${building.quantityOnHand(tradeable)} and we will send them $howManyToSend")
            debug("Before transfer... building has $${building.quantityOnHand(Tradeable.MONEY)}")
            val howManyTransferred = building.transferInventory(contract.to, contract.tradeable, howManyToSend)
            building.addInventory(Tradeable.MONEY, priceForGoods(contract.tradeable, howManyTransferred))
            debug("${building.description}: We transferred ${contract.quantity} ${contract.tradeable} to ${contract.to.description()}")
            debug("After transfer... building has $${building.quantityOnHand(Tradeable.MONEY)}")
        } else {
            debug("${building.description } wanted to send ${contract.quantity} ${contract.tradeable} but it was out of stock...")
        }
    }
}