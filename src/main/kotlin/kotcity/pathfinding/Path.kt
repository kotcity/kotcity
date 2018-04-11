package kotcity.pathfinding

import kotcity.data.BlockCoordinate

/**
 * Path of nodes, with conveniece methods.
 *
 * @nodes list of nodes along a path
 */
data class Path(
    internal val nodes: List<NavigationNode> = emptyList()
) {
    /**
     * Total heuristic score of the nodes in this Path.
     */
    fun totalScore(): Int = nodes.sumBy { it.score.toInt() }

    /**
     * Total length of this Path.
     */
    fun length(): Int = nodes.size

    /**
     * List of coordinates in this Path.
     */
    fun blocks(): List<BlockCoordinate> = nodes.map { it.coordinate }.toList()

    /**
     * Append another Path to the end of this Path
     */
    operator fun plus(otherPath: Path?): Path? {
        val otherNodes = otherPath?.nodes?.toList() ?: return this

        val firstOfOther = otherNodes.first()
        // chop the first node off the other list...
        // because we have to replace it with a parent that has the other list
        // of nodes as a parent to preserve the chain...
        val newFirst = NavigationNode(
            firstOfOther.coordinate,
            nodes.last(),
            firstOfOther.score,
            firstOfOther.transitType,
            firstOfOther.direction
        )

        // lop off the first one and shove our new one in there...
        val newOtherNodes = listOf(newFirst).plus(otherNodes.drop(1))

        return Path(nodes.plus(newOtherNodes).distinct())
    }
}
