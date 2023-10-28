package bots

import common.*
import kotlin.math.ln
import kotlin.math.sqrt

open class MCTSBot(
    private val maxIterationsCfg: Int,             // Total moves to play
    private val rand: Xoroshiro = Xoroshiro()   // Random number generator
) : Bot {
    private val INIT_SIZE = 1024
    
    protected var maxIterations = maxIterationsCfg
    private val updateIterations = maxIterationsCfg/100

    override fun move(board: Board, percentDone: (Int) -> Unit): Coord {

        // Flattened tree structure
		var nodeCoords = ByteArray(INIT_SIZE)       //coord[N]
		var nodeVisits = IntArray(INIT_SIZE)        //visits[N]
		var nodeWins = IntArray(INIT_SIZE)          //wins[N]
		var nodeChildStart = IntArray(INIT_SIZE)    //indexFirstChild[N]
		var nodeChildCount = IntArray(INIT_SIZE)    //nbChildren[N]

        // Create (childless) head
        nodeCoords[0] = -1
        nodeVisits[0] = 0
        nodeWins[0] = 0

        // Bookkeeping vars
        var newIdx = 1
        var touchedCount: Int
        val touched = IntArray(81) // indices of visited nodes for iteration
        val nodeBoard = board.copy()

        var nextUpdate = updateIterations
        percentDone(0)

        while (nodeBoard.movesPlayed < maxIterations) {
            // Update status if needed
            if (nodeBoard.movesPlayed > nextUpdate){
                percentDone((100.0 * nodeBoard.movesPlayed.toDouble() /maxIterationsCfg.toDouble()).toInt())
                nextUpdate += updateIterations
            }

            var nodeIdx = 0
            nodeBoard.loadInstance(board)
            touchedCount = 0
            touched[touchedCount++] = nodeIdx

            while (!nodeBoard.isDone) {
                /** Init children **/
                if (nodeChildStart[nodeIdx] == 0) {
                    // Expand (double) arrays if less than 128 entries remain
                    if (nodeCoords.size - newIdx < 128){
                        val newSize = nodeCoords.size * 2
                        nodeCoords = nodeCoords.copyOf(newSize)
                        nodeVisits = nodeVisits.copyOf(newSize)
                        nodeWins = nodeWins.copyOf(newSize)
                        nodeChildStart = nodeChildStart.copyOf(newSize)
                        nodeChildCount =nodeChildCount.copyOf(newSize)
                    }

                    // Create children
                    nodeChildStart[nodeIdx] = newIdx
                    nodeBoard.forAvailableMoves { coord ->
                        nodeCoords[newIdx] = coord
                        nodeVisits[newIdx] = 0
                        nodeWins[newIdx] = 0
                        newIdx++
                    }
                    nodeChildCount[nodeIdx] = newIdx - nodeChildStart[nodeIdx]
                }

                /** Exploration (visit unvisited children, if any) **/
                var countUnexpanded = 0
                for (childIdx in nodeChildStart[nodeIdx]..< nodeChildStart[nodeIdx] + nodeChildCount[nodeIdx]){
                    if (nodeVisits[childIdx] == 0) countUnexpanded++
                }
                if (countUnexpanded > 0) {
                    var remaining = rand.nextInt(countUnexpanded)
                    for (childIdx in nodeChildStart[nodeIdx]..< nodeChildStart[nodeIdx] + nodeChildCount[nodeIdx]){
                        if (nodeVisits[childIdx] == 0) {
                            if (remaining == 0) {
                                nodeIdx = childIdx
                                nodeBoard.play(nodeCoords[nodeIdx])
                                touched[touchedCount++] = nodeIdx
                                break
                            }
                            remaining--
                        }
                    }
                    break
                }

                /** Selection: select best node based on win and visit rates **/
                var selected = nodeChildStart[nodeIdx]
                var bestValue = Double.NEGATIVE_INFINITY
                for (childIdx in nodeChildStart[nodeIdx]..< nodeChildStart[nodeIdx] + nodeChildCount[nodeIdx]){
                    val childWins = nodeWins[childIdx].toDouble()
                    val childVisits = nodeVisits[childIdx].toDouble()
                    val visits = nodeVisits[nodeIdx].toDouble()

                    val uctValue = (childWins / childVisits) + sqrt(2.0 * ln(visits) / childVisits) / 3
                    if (uctValue > bestValue) {
                        selected = childIdx
                        bestValue = uctValue
                    }
                }
                nodeIdx = selected
                nodeBoard.play(nodeCoords[nodeIdx])
                touched[touchedCount++] = nodeIdx
            }

            /** Simulation **/
            var won = if (nodeBoard.isDone) nodeBoard.nextPlayX == board.nextPlayX
            else nodeBoard.randomPlayWinner() == board.nextPlayX

            /** Update **/
            for (i in 0..<touchedCount) {
                val touchedIdx = touched[i]
                won = !won
                nodeVisits[touchedIdx] += 1
                if (won) nodeWins[touchedIdx] += 1
            }
        }

        var bestVisits = 0
        var bestMove: Byte = nodeCoords[nodeChildStart[0]]
        for (childIdx in nodeChildStart[0]..<nodeChildStart[0] + nodeChildCount[0]) {
            if (nodeVisits[childIdx] > bestVisits){
                bestMove   = nodeCoords[childIdx]
                bestVisits = nodeVisits[childIdx]
            }
        }

        percentDone(100)
        return bestMove
    }

    override fun reset() {maxIterations = maxIterationsCfg}
    override fun cancel() { maxIterations = 0 }
    override fun toString() = "MCTSBot($updateIterations)"
}
