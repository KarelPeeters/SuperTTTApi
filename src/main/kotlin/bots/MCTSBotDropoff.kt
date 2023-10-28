package bots

import common.Board
import common.Coord
import common.Xoroshiro

/** This is a MCTS bot that starts at `iterationsBegin` and after `steps` moves only performs `iterationsEnd` */
class MCTSBotDropoff(
    private val iterationsBegin: Int,
    private val iterationsEnd: Int,
    private val steps: Int,
    rand: Xoroshiro = Xoroshiro()
) : MCTSBot(iterationsBegin, rand) {

    override fun move(board: Board, percentDone: (Int) -> Unit): Coord {
        // Set the correct number of iterations
        if (maxIterations > 0){
            val currentStep = board.grids.sumOf { it.countOneBits() }
            val iterationsDiff = iterationsEnd - iterationsBegin
            maxIterations = iterationsBegin + iterationsDiff * (steps - currentStep) / steps
        }

        return super.move(board, percentDone)
    }
}