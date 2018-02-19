package kotcity.automata

import kotcity.data.CityMap
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
            }
        }
    }
}