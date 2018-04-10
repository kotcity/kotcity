package kotcity.pathfinding

import kotcity.data.BlockCoordinate

/**
 * Node in found paths.
 */
data class NavigationNode(
    val coordinate: BlockCoordinate,
    val parent: NavigationNode? = null,
    val score: Double = 0.0,
    val transitType: TransitType = TransitType.ROAD,
    val direction: Direction
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
