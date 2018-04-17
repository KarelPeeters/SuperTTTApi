package newcarlo

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Timer
import java.util.*

private fun randomSimulationWinner(board: Board): Player {
	while (!board.isDone()) {
		val children = board.availableMoves()
		board.play(children[rand.nextInt(children.size)])
	}
	return if (board.wonBy() != Player.NEUTRAL) board.wonBy() else Player.values()[rand.nextInt(2)]
}

private var startBoard = Board()
private var player = Player.NEUTRAL
private val rand = Random()

class CarlosBot : Bot {
	override fun toString() = "Senior Carlos"

	override fun move(board: Board, timer: Timer): Byte? {
		player = board.nextPlayer()
		startBoard = board

		val tree = TreeNode(-1, .0, .0)
		while(timer.running()) {
			tree.searchIteration()
		}

		var mostPlayed = tree.knownChildren.first()
		var maxPlayCount = .0

		for (c in tree.knownChildren) {
			if (c.totalVisits > maxPlayCount) {
				mostPlayed = c
				maxPlayCount = c.totalVisits
			}
		}

		return mostPlayed.coord
	}

	class TreeNode(val coord: Byte, var score: Double, var totalVisits: Double) {
		val knownChildren = mutableListOf<TreeNode>()

		fun searchIteration() {
			//Selection
			var cNode = this
			val cBoard = startBoard.copy()
			val visited = mutableListOf(cNode)
			while (true) {
				//Expand
				if(cBoard.isDone())break
				else if (cNode.knownChildren.size != cBoard.availableMoves().size) {
					val children = cBoard.availableMoves().filterNot { c -> cNode.knownChildren.any { it.coord == c } }
					val randomMove = children[rand.nextInt(children.size)]

					val node = TreeNode(randomMove,.0,.0)
					cNode.knownChildren.add(node)
					visited.add(node)

					cBoard.play(randomMove)
					break
				}

				//Select
				var selected = cNode.knownChildren.first()
				var bestValue = Double.MIN_VALUE
				for (c in cNode.knownChildren) {
					val uctValue = c.score / (c.totalVisits) + Math.sqrt(2 * Math.log(cNode.totalVisits) / (c.totalVisits))

					if (uctValue > bestValue) {
						selected = c
						bestValue = uctValue
					}
				}
				cNode = selected
				cBoard.play(cNode.coord)
				visited.add(cNode)
			}

			//Simulation
			var won = randomSimulationWinner(cBoard) == player

			//Update
			for (node in visited) {
				won = !won
				node.totalVisits++
				if (won) node.score += 1
			}
		}

	}
}