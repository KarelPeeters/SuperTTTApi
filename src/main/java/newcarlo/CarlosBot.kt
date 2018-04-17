package newcarlo

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Timer
import java.util.*

class CarlosBot : Bot {
	private val rand = Random()
	override fun toString() = "Senior Carlos"

	class TreeNode(val coord: Byte) {
		var children: List<TreeNode>? = null
		var visits = .0
		var wins = .0
	}

	override fun move(board: Board, timer: Timer): Byte? {
		val head = TreeNode(-1)
		repeat(2000) {
			var cNode = head
			val cBoard = board.copy()
			val visited = mutableListOf(cNode)
			while (true) {
				//Check for exit condition
				if (cBoard.isDone()) break
				else if (cNode.children == null) {
					cNode.children = cBoard.availableMoves().map { TreeNode(it) }
				}

				//Exploration
				if (cNode.children!!.any { it.visits == .0 }) {
					val unexploredChildren = cNode.children!!.filter { it.visits == .0 }
					cNode = unexploredChildren[rand.nextInt(unexploredChildren.size)]
					visited.add(cNode)
					cBoard.play(cNode.coord)
					break
				}

				//Selection
				var selected = cNode.children?.first()!!
				var bestValue = Double.MIN_VALUE
				for (child in cNode.children!!) {
					val uctValue = child.wins / (child.visits) + Math.sqrt(2 * Math.log(cNode.visits) / (child.visits))
					if (uctValue > bestValue) {
						selected = child
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
			var won = if (cBoard.wonBy() != Player.NEUTRAL) cBoard.wonBy() == board.nextPlayer() else rand.nextBoolean()
			for (node in visited) {
				won = !won
				node.visits++
				if (won) node.wins += 1
			}
		}

		//Return most played move
		var mostPlayed: Byte = head.children?.first()?.coord ?: board.availableMoves().first()
		var maxPlayCount = .0
		for (c in head.children ?: mutableListOf()) {
			if (c.visits > maxPlayCount) {
				mostPlayed = c.coord
				maxPlayCount = c.visits
			}
		}
		return mostPlayed
	}
}