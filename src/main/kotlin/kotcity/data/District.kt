package kotcity.data

data class District(
    var name: String,
    val blocks: MutableList<BlockCoordinate> = mutableListOf()
)
