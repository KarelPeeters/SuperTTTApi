package newcarlo

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Timer
import java.util.*

private fun randomSimulationWinner(treeNode: CarlosBot.TreeNode): Player {
	var c = treeNode
	while (!c.board.isDone()) {
		c = c.children()[rand.nextInt(c.children().size)]
	}
	return if (c.board.wonBy() != Player.NEUTRAL) c.board.wonBy() else Player.values()[rand.nextInt(2)]
}

private var player = Player.NEUTRAL
private val rand = Random()

class CarlosBot : Bot {
	override fun toString() = "Senior Carlos"

	override fun move(board: Board, timer: Timer): Byte? {
		player = board.nextPlayer()

		val tree = TreeNode(board, .0, .0)
		while (timer.running()) {
			tree.searchIteration()
		}

		var mostPlayed = tree.children().first()
		var maxPlayCount = .0

		for (c in tree.children()) {
			if (c.totalVisits > maxPlayCount) {
				mostPlayed = c
				maxPlayCount = c.totalVisits
			}
		}

		return mostPlayed.board.lastMove()
	}

	class TreeNode(val board: Board, var score: Double, var totalVisits: Double) {
		private var str: List<TreeNode>? = null
		fun children(): List<TreeNode> {
			if (str == null) str = board.availableMoves().map { TreeNode(board.copy().apply { play(it) },.0,.0) }
			return str!!
		}

		fun searchIteration() {
			//Selection
			var cur: TreeNode = this
			val visited = mutableListOf(cur)
			while (true) { //Not a leave
				if (cur.children().any { it.totalVisits == .0}) {
					val unexploredChildren = cur.children().filter { it.totalVisits==.0 }
					cur = unexploredChildren[rand.nextInt(unexploredChildren.size)]
					visited.add(cur)
					break
				}
				else if(cur.board.isDone())break

				cur = cur.selectBestChild()
				visited.add(cur)
			}

			//Simulation
			var won = randomSimulationWinner(cur) == player

			//Update
			for (node in visited) {
				won = !won
				node.totalVisits++
				if (won) node.score += 1
			}
		}

		private fun selectBestChild(): TreeNode {
			var selected = children().first()
			var bestValue = Double.MIN_VALUE

			for (c in children().subList(0, children().size)) {
				val uctValue = c.score / (c.totalVisits) + Math.sqrt(2 * Math.log(totalVisits) / (c.totalVisits))

				if (uctValue > bestValue) {
					selected = c
					bestValue = uctValue
				}
			}
			return selected
		}
	}
}