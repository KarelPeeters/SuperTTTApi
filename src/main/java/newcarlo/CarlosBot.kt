package newcarlo

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Timer
import java.util.*

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

		var mostPlayed: Byte = tree.children?.first()?.coord ?: startBoard.availableMoves().first()
		var maxPlayCount = .0

		for (c in tree.children?: mutableListOf()) {
			if (c.totalVisits > maxPlayCount) {
				mostPlayed = c.coord
				maxPlayCount = c.totalVisits
			}
		}

		return mostPlayed
	}

	class TreeNode(val coord: Byte, var score: Double, var totalVisits: Double) {
		var children: List<TreeNode>? = null

		fun searchIteration() {
			var cNode = this
			val cBoard = startBoard.copy()
			val visited = mutableListOf(cNode)
			while (true) {
				//Check for exit condition
				if(cBoard.isDone()) break
				else if (cNode.children==null){
					cNode.children=cBoard.availableMoves().map { TreeNode(it,.0,.0) }
				}

				//Exploration
				if (cNode.children!!.any { it.totalVisits==.0 }) {
					val unexploredChildren = cNode.children!!.filter { it.totalVisits==.0 }

					cNode = unexploredChildren[rand.nextInt(unexploredChildren.size)]
					visited.add(cNode)
					cBoard.play(cNode.coord)
					break
				}

				//Select
				var selected = cNode.children?.first()!!
				var bestValue = Double.MIN_VALUE
				for (c in cNode.children!!) {
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
			while (!cBoard.isDone()) {
				val children = cBoard.availableMoves()
				cBoard.play(children[rand.nextInt(children.size)])
			}

			//Update
			var won = (if (cBoard.wonBy() != Player.NEUTRAL) cBoard.wonBy() else Player.values()[rand.nextInt(2)]) == player
			for (node in visited) {
				won = !won
				node.totalVisits++
				if (won) node.score += 1
			}
		}
	}
}