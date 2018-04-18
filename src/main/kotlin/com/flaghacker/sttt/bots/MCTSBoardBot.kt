package com.flaghacker.sttt.bots

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Timer
import java.util.*

class MCTSBoardBot : Bot {
	private val rand = Random()
	override fun toString() = "MCTSBot"

	private class Node(@JvmField val board: Board) {
		@JvmField var children: List<Node>? = null
		@JvmField var visits = 0
		@JvmField var wins = 0

		fun print(prefix: String = "", isTail: Boolean = true, depth: Int) {
			if (depth == 0 || children == null) return
			println(prefix + (if (isTail) "└── " else "├── ") + "${board.lastMove?:"H"}:$wins/$visits")
			for (i in 0 until children!!.size - 1) children!![i].print(prefix + if (isTail) "    " else "│   ", false, depth - 1)
			if (children!!.isNotEmpty()) children!![children!!.size - 1].print(prefix + if (isTail) "    " else "│   ", true, depth - 1)
		}
	}

	override fun move(board: Board, timer: Timer): Byte? {
/*		var exploring = false
		lateinit var exploreHead: Node*/
		val visited = LinkedList<Node>()

		val head = Node(board)
		repeat(2000) {
/*			if (!exploring){

			}*/

			visited.clear()

			var cNode = head
			visited.add(cNode)

			while (!cNode.board.isDone) {
				//Init children
				if (cNode.children == null) {
					cNode.children = cNode.board.availableMoves { Node(cNode.board.copy().apply {play(it)}) }
				}

				//Exploration
				if (cNode.children!!.any { it.visits == 0 }) {
					val unexploredChildren = cNode.children!!.filter { it.visits == 0 }
					cNode = unexploredChildren[rand.nextInt(unexploredChildren.size)]
					visited.add(cNode)
					break
				}

				//Selection
				var selected = cNode.children!!.first()
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
				visited.add(cNode)
			}

			//Simulation
			val simBoard = cNode.board.copy()
			while (!simBoard.isDone) {
				val children = simBoard.availableMoves
				simBoard.play(children[rand.nextInt(children.size)])
			}

			//Update
			var won = if (simBoard.wonBy != Player.NEUTRAL) simBoard.wonBy == board.nextPlayer else rand.nextBoolean()
			for (node in visited) {
				won = !won
				node.visits++
				if (won) node.wins += 1
			}
		}

		return head.children!!.maxBy { it.visits }?.board?.lastMove!!
	}
}
