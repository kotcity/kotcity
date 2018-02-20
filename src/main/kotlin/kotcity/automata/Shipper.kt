package kotcity.automata

import kotcity.data.CityMap
import kotcity.data.OutsideTradeEntity
import kotcity.data.Tradeable

class Shipper(val cityMap: CityMap) {

    var debug = false

    fun tick() {
        // what we want to do here is find all industrial zones with WHOLESALE GOODS and ship them to commercial zones
        cityMap.buildingLayer.forEach { coordinate, building ->
            building.contracts.forEach { contract ->
                // we only want to deal with "to" and not to ourself...
                // we also don't SEND labor
                if (contract.to.building() != building && contract.tradeable != Tradeable.LABOR) {
                    val howManyTransferred = building.transferInventory(contract.to, contract.tradeable, contract.quantity)
                    building.addInventory(Tradeable.MONEY, howManyTransferred)
                    if (debug) {
                        println("${building.description}: We transferred ${contract.quantity} ${contract.tradeable} to ${contract.to.description()}")
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
                            if (debug) {
                                println("${building.description} sent some ${contract.tradeable} to ${contract.to.description()}")
                            }
                        } else if (contract.tradeable != Tradeable.MONEY) {
                            val howManyTransferred = building.transferInventory(contract.to, contract.tradeable, contract.quantity)
                            building.addInventory(Tradeable.MONEY, howManyTransferred)
                            if (debug) {
                                println("${building.description}: We sent ${contract.quantity} ${contract.tradeable} to ${contract.to.description()}")
                            }
                        }
                    }

                }
            }
        }
    }
}