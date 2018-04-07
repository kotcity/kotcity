package kotcity.pathfinding

import kotcity.data.BlockCoordinate
import kotcity.data.CityMap

/**
 * Node in found paths.
 */
data class NavigationNode(
    val cityMap: CityMap,
    val coordinate: BlockCoordinate,
    val parent: NavigationNode?,
    val score: Double,
    val transitType: TransitType = TransitType.ROAD,
    val direction: Direction,
    val isOnRail: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NavigationNode
        return this.coordinate == that.coordinate
    }

    override fun hashCode(): Int {
        return this.coordinate.hashCode()
    }
}
