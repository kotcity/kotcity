package kotcity.automata

import kotcity.data.*
import kotcity.data.buildings.Building
import kotcity.util.Debuggable

class Shipper(val cityMap: CityMap) : Debuggable {

    override var debug = false

    fun tick() {
        // what we want to do here is find all industrial zones with WHOLESALE GOODS and ship them to commercial zones
        cityMap.locations().forEach { location ->
            val building = location.building

            val contractsToVoid = mutableListOf<Contract>()

            building.contracts.forEach { contract ->
                // we only want to deal with "to" and not to ourself...
                // we also don't SEND labor

                // let's try to "pull" what we need...
                if (contract.to.building() == building && contract.tradeable != Tradeable.LABOR) {
                    val wasSuccessful = contract.execute()
                    if (!wasSuccessful) {
                        contractsToVoid.add(contract)
                    }
                }

                // this is ourself sending stuff to the "nation"
                if (contract.from.building() == building && contract.to is OutsideTradeEntity) {
                    // how many can we push down?
                    // TODO: not sure if this is right...
                    exportToOutside(contract, building)
                }
            }

            contractsToVoid.forEach { it.void() }
        }
    }

    private fun exportToOutside(contract: Contract, building: Building) {
        if (contract.to.currentQuantityWanted(contract.tradeable) >= contract.quantity) {
            if (contract.tradeable == Tradeable.LABOR) {
                // don't actually SEND anything... we just get $$$
                building.addInventory(Tradeable.MONEY, contract.quantity)
                debug { "${building.description} sent some ${contract.tradeable} to ${contract.to.description()}" }
            } else if (contract.tradeable != Tradeable.MONEY) {
                val howManyTransferred = building.transferInventory(contract.to, contract.tradeable, contract.quantity)
                building.addInventory(Tradeable.MONEY, Prices.priceForGoods(contract.tradeable, howManyTransferred))
                debug {"${building.description}: We sent ${contract.quantity} ${contract.tradeable} to ${contract.to.description()}" }
            }
        }
    }
}
