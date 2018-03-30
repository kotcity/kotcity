package kotcity.data

class Zot(val type: ZotType) {
    var age: Int = 1

    override fun toString(): String {
        return "Zot(type=$type age=$age)"
    }
}

enum class ZotType {
    TOO_MUCH_TRAFFIC,
    UNHAPPY_NEIGHBORS,
    NO_GOODS,
    NO_WORKERS,
    NO_DEMAND,
    NO_POWER,
    NO_CUSTOMERS,
    TOO_MUCH_POLLUTION
}