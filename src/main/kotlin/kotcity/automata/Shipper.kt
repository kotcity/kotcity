package kotcity.automata

import kotcity.data.CityMap
import kotcity.data.OutsideTradeEntity
import kotcity.data.Tradeable
import kotcity.util.Debuggable

class Shipper(val cityMap: CityMap): Debuggable {

    override var debug = false

    private fun priceForGoods(tradeable: Tradeable, quantity: Int): Int {
        return when (tradeable) {
            Tradeable.MONEY -> quantity * 1
            Tradeable.GOODS -> quantity * 3
            Tradeable.LABOR -> quantity * 1
            Tradeable.RAW_MATERIALS -> quantity * 1
            Tradeable.WHOLESALE_GOODS -> quantity * 2
        }
    }

    fun tick() {
        // what we want to do here is find all industrial zones with WHOLESALE GOODS and ship them to commercial zones
        cityMap.buildingLayer.forEach { _, building ->
            building.contracts.forEach { contract ->
                // we only want to deal with "to" and not to ourself...
                // we also don't SEND labor
                if (contract.to.building() != building && contract.tradeable != Tradeable.LABOR) {
                    if (building.quantityOnHand(contract.tradeable) > 0) {
                        debug("Before transfer... building has $${building.quantityOnHand(Tradeable.MONEY)}")
                        val howManyTransferred = building.transferInventory(contract.to, contract.tradeable, contract.quantity)
                        building.addInventory(Tradeable.MONEY, priceForGoods(contract.tradeable, howManyTransferred))
                        debug("${building.description}: We transferred ${contract.quantity} ${contract.tradeable} to ${contract.to.description()}")
                        debug("After transfer... building has $${building.quantityOnHand(Tradeable.MONEY)}")
                    } else {
                        debug("Wanted to send ${contract.quantity} ${contract.tradeable} but it was out of stock...")
                    }
                }

                // let's get the FROM contracts and rig up export...
                if (contract.from.building() == building && contract.to is OutsideTradeEntity) {
                    // how many can we push down?
                    val customerWantsQuantity: Int = contract.to.wantsHowMany(contract.tradeable)
                    if (customerWantsQuantity >= contract.quantity) {
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
            }
        }
    }
}