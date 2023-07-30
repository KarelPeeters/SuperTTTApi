package bots

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Coord
import common.*
import java.util.random.RandomGenerator
import kotlin.math.ln
import kotlin.math.sqrt

class MCTSBotArray(
    private val rand: RandomGenerator = RandomGenerator.of("Xoroshiro128PlusPlus"),
    private val maxIterations: Int
) : Bot {
    override fun toString() = "MCTSBotArray"

    private val INIT_SIZE = 1024 * 8

    override fun move(boardOld: Board): Coord {
        val board = BoardUnsafe(boardOld) // TODO REMOVE

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

        while (nodeVisits[0] < maxIterations) {
            var nodeIdx = 0
            nodeBoard.loadInstance(board)
            touchedCount = 0
            touched[touchedCount++] = nodeIdx

            var playResult = if (!nodeBoard.isDone) 0 else 999
            while (playResult == 0) {
                /** Init children **/
                if (nodeChildStart[nodeIdx] == 0) {
                    nodeChildStart[nodeIdx] = newIdx

                    // Create playable macro mask
                    var macroMask = GRID_MASK
                    if (nodeBoard.lastMove != (-1).toByte()){
                        val tileLastMove = nodeBoard.lastMove.toInt() and 0xF
                        macroMask = (1 shl tileLastMove) and nodeBoard.openMacroMask
                        if (macroMask == 0) macroMask = nodeBoard.openMacroMask // free-move
                    }

                    // Find and store all children
                    macroMask.forEachBit { om ->
                        val osFree = ((nodeBoard.grids[om] shr GRID_BITS) or nodeBoard.grids[om]).inv() and GRID_MASK
                        osFree.forEachBit { os ->
                            // Create child
                            nodeCoords[newIdx] = ((om shl 4) + os).toByte()
                            nodeVisits[newIdx] = 0
                            nodeWins[newIdx] = 0
                            newIdx++

                            // Expand (double) arrays if necessary
                            if (newIdx == nodeCoords.size){
                                val newSize = nodeCoords.size * 2
                                nodeCoords = nodeCoords.copyOf(newSize)
                                nodeVisits = nodeVisits.copyOf(newSize)
                                nodeWins = nodeWins.copyOf(newSize)
                                nodeChildStart = nodeChildStart.copyOf(newSize)
                                nodeChildCount =nodeChildCount.copyOf(newSize)
                            }
                        }
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
                                playResult = nodeBoard.play(nodeCoords[nodeIdx])
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
                for (childIdx in nodeChildStart[nodeIdx]..< nodeChildStart[nodeIdx] + nodeChildCount[nodeIdx]){ // TODO move to INLINE function
                    val childWins = nodeWins[childIdx].toDouble()
                    val childVisits = nodeVisits[childIdx].toDouble()
                    val visits = nodeVisits[nodeIdx].toDouble()

                    val uctValue = (childWins / childVisits) + sqrt(2.0 * ln(visits) / childVisits)
                    if (uctValue > bestValue) {
                        selected = childIdx
                        bestValue = uctValue
                    }
                }
                nodeIdx = selected
                playResult = nodeBoard.play(nodeCoords[nodeIdx])
                touched[touchedCount++] = nodeIdx
            }

            /** Simulation **/
            var won = if (playResult == 0) nodeBoard.randomPlayWinner(rand) == board.nextPlayX else when (playResult) {
                1 -> board.nextPlayX    // X WINS
                2 -> !board.nextPlayX   // O WINS
                else -> rand.nextBoolean()  // TIE
            }

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


        // TODO remove conversion back
        //val bestMove = (head.children?.maxBy { it.visits }?.coord) ?: throw Exception("NO MOVE FOUND") //TODO
        val bestMoveIdx = bestMove.toInt() and 0xFF // mask out sign extension
        val bestMoveOm = bestMoveIdx shr 4
        val bestMoveOs = bestMoveIdx and 0b1111
        return (bestMoveOm * 9 + bestMoveOs).toByte()
    }
}
