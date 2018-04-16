package newcarlo

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Timer
import java.util.*

private fun randomSimulationWinner(treeNode: CarlosBot.TreeNode): Player {
	var c = treeNode
	while (!c.board.isDone()) {
		c = c.children[rand.nextInt(c.children.size)]
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

		var mostPlayed = tree.children.first()
		var maxPlayCount = .0

		for (c in tree.children) {
			if (c.totalVisits > maxPlayCount) {
				mostPlayed = c
				maxPlayCount = c.totalVisits
			}
		}

		return mostPlayed.board.lastMove()
	}

	class TreeNode(val board: Board, var score: Double, var totalVisits: Double) {
		val children by lazy { board.availableMoves().map { TreeNode(board.copy().apply { play(it) }, .0, .0) } }

		fun searchIteration() {
			//Selection
			var cur: TreeNode = this
			val visited = mutableListOf(cur)
			while (cur.children.none { it.totalVisits == .0} && cur.children.isNotEmpty()) { //Not a leave
				cur = cur.selectBestChild()
				visited.add(cur)
			}

			//Expansion
			lateinit var newNode: TreeNode
			if (cur.children.isNotEmpty()) {
				val unexploredChildren = cur.children.filter { it.totalVisits==.0 }
				newNode = unexploredChildren[rand.nextInt(unexploredChildren.size)]
				visited.add(newNode)
			} else newNode = cur

			//Simulation
			var won = randomSimulationWinner(newNode) == player

			//Update
			for (node in visited) {
				won = !won
				node.totalVisits++
				if (won) node.score += 1
			}
		}

		private fun selectBestChild(): TreeNode {
			var selected = children.first()
			var bestValue = Double.MIN_VALUE

			for (c in children.subList(0, children.size)) {
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