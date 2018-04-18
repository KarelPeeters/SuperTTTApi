package newcarlo

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Timer
import java.util.*

class CarlosBot : Bot {
	private val rand = Random()
	override fun toString() = "Senior Carlos"

	private class Node(@JvmField val coord: Byte) {
		@JvmField var children: List<Node>? = null
		@JvmField var visits = 0
		@JvmField var wins = 0

		fun print(prefix: String = "", isTail: Boolean = true, depth: Int) {
			if (depth == 0 || children == null) return
			println(prefix + (if (isTail) "└── " else "├── ") + "$coord:$wins/$visits")
			for (i in 0 until children!!.size - 1) children!![i].print(prefix + if (isTail) "    " else "│   ", false, depth - 1)
			if (children!!.isNotEmpty()) children!![children!!.size - 1].print(prefix + if (isTail) "    " else "│   ", true, depth - 1)
		}
	}

	override fun move(board: Board, timer: Timer): Byte? {
		val head = Node(-1)
		repeat(2000) {
			var cNode = head
			val cBoard = board.copy()
			val visited = LinkedList<Node>()
			visited.add(cNode)

			while (!cBoard.isDone()) {
				//Init children
				if (cNode.children == null) {
					cNode.children = cBoard.availableMoves { Node(it) }
				}

				//Exploration
				if (cNode.children!!.any { it.visits == 0 }) {
					val unexploredChildren = cNode.children!!.filter { it.visits == 0 }
					cNode = unexploredChildren[rand.nextInt(unexploredChildren.size)]
					visited.add(cNode)
					cBoard.play(cNode.coord)
					break
				}

				//Selection
				var selected = cNode.children?.first()!!
				var bestValue = Double.NEGATIVE_INFINITY
				for (child in cNode.children!!) {
					val uctValue = (child.wins.toDouble() / child.visits.toDouble()) +
							Math.sqrt(2.0 * Math.log(cNode.visits.toDouble()) / (child.visits.toDouble()))
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

		return head.children!!.maxBy { it.visits }?.coord
	}
}