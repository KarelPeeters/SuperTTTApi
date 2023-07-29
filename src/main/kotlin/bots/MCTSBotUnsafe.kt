package bots

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Coord
import common.BoardUnsafe
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt

class MCTSBotUnsafe(
    private val rand: Random = Random(),
    private val maxIterations: Int,
    private val callback: ((Double) -> Unit)? = null
) : Bot {
    override fun toString() = "MCTSBotUnsafe"

    private class Node(@JvmField val coord: Coord) {
        @JvmField
        var children: Array<Node>? = null

        @JvmField
        var visits = 0

        @JvmField
        var wins = 0

        fun print(prefix: String = "", isTail: Boolean = true, depth: Int) {
            if (depth == 0 || children == null) return
            println(prefix + (if (isTail) "└── " else "├── ") + "$coord:$wins/$visits")
            for (i in 0 until children!!.size - 1) children!![i].print(
                prefix + if (isTail) "    " else "│   ",
                false,
                depth - 1
            )
            if (children!!.isNotEmpty()) children!![children!!.size - 1].print(
                prefix + if (isTail) "    " else "│   ",
                true,
                depth - 1
            )
        }
    }

    override fun move(board: Board): Coord? {
        callback?.invoke(0.0)
        var lastIterations = 0

        val visited = LinkedList<Node>()
        val head = Node(-1)
        val boardOrig = BoardUnsafe(board) // TODO REMOVE

        while (head.visits < maxIterations) {
            val iterations = head.visits
            if (iterations - lastIterations > maxIterations / 100) {
                callback?.invoke(iterations.toDouble() / maxIterations)
                lastIterations = iterations
            }

            var cNode = head
            val cBoard = boardOrig.copy()
            visited.clear()
            visited.add(cNode)

            var playResult = if (!cBoard.isDone) 0 else 999
            while (playResult == 0) {
                //Init children
                if (cNode.children == null) {
                    cNode.children = cBoard.availableMoves { Node(it) }
                }

                //Exploration
                var count = cNode.children!!.count { it.visits == 0 }
                if (count > 0) {
                    count = rand.nextInt(count)
                    for (node in cNode.children!!) {
                        if (node.visits == 0) {
                            if (count == 0) {
                                cNode = node
                                visited.add(cNode)
                                playResult = cBoard.play(cNode.coord)
                                break
                            }
                            count--
                        }
                    }
                    break
                }

                //Selection
                var selected = cNode.children?.first()!!
                var bestValue = Double.NEGATIVE_INFINITY
                for (child in cNode.children!!) {
                    val uctValue = (child.wins.toDouble() / child.visits.toDouble()) +
                            sqrt(2.0 * ln(cNode.visits.toDouble()) / (child.visits.toDouble()))
                    if (uctValue > bestValue) {
                        selected = child
                        bestValue = uctValue
                    }
                }
                cNode = selected
                playResult = cBoard.play(cNode.coord)
                visited.add(cNode)
            }

            //Simulation
            var won = if (playResult == 0) {
                cBoard.randomPlayWinner(rand)?.let { it == boardOrig.nextPlayX } ?: rand.nextBoolean()
            } else when (playResult) {
                1 -> boardOrig.nextPlayX    // X WINS
                2 -> !boardOrig.nextPlayX   // O WINS
                else -> rand.nextBoolean()  // TIE
            }

            //Update
            for (node in visited) {
                won = !won
                node.visits++
                if (won) node.wins += 1
            }
        }

//		println(head.visits)

//		callback?.invoke(1.0)
        // CONVERT BACK -- TODO STRIP
        val bestMove = (head.children?.maxBy { it.visits }?.coord) ?: throw Exception("NO MOVE FOUND") //TODO
        val bestMoveIdx = bestMove.toInt() and 0xFF // mask out sign extension
        val bestMoveOm = bestMoveIdx shr 4
        val bestMoveOs = bestMoveIdx and 0b1111
        return (bestMoveOm * 9 + bestMoveOs).toByte()
    }
}
