package com.flaghacker.sttt.games

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Coord
import java.util.*

class ConsoleGame(private val bot: Bot) {
	private var current = Board()
	private val history = mutableListOf<Board>()

	fun run() {
		println("You (X) vs $bot (O)")
		printHelp()

		while (!current.isDone) {
			val input = Scanner(System.`in`).nextLine()

			when (input.split(" ").first().toLowerCase()) {
				"undo" -> {
					if (history.size > 1) {
						history.removeAt(history.lastIndex)
						current = history.last().copy()
						printCurrent()
					} else {
						history.clear()
						current = Board()
						printCurrent()
					}
				}
				"exit" -> return
				"help" -> printHelp()
				"play" -> {
					val coord = coordFromInput(input)
					if (coord != null) {
						if (current.availableMoves.contains(coord)) {
							println("you played on $coord")
							current.play(coord)
							printCurrent()

							if (!current.isDone) {
								val botMove = bot.move(current.copy())!!
								println("bot played on $botMove")
								current.play(botMove)
								history.add(current.copy())
								printCurrent()
							}
						} else System.err.println("coord not available please choose out of ${Arrays.toString(current.availableMoves)}")
					}
				}
				else -> {
					System.err.println("invalid command")
					printCurrent()
				}
			}
		}
	}

	private fun printCurrent() {
		println("\n${current.toString(true)}\n")
	}

	private fun printHelp() {
		println("\nCOMMANDS:")
		println("help\t\t\t: display the commands")
		println("undo\t\t\t: undo your last move")
		println("play <os>\t\t: play os [0-8] in current macro")
		println("play <om> <os>\t: play os [0-8] in macro om [0-8]")
		printCurrent()
	}

	private fun coordFromInput(input: String): Coord? {
		when {
			input.matches(Regex("play \\d")) -> {
				val os = input.split(" ").last().toInt()

				val macros = current.availableMoves.map { it / 9 }
				return if (macros.all { it == macros.first() })
					(macros.first() * 9 + os).toByte()
				else {
					System.err.println("you are in freemove, please use \"play <om> <os>\"")
					null
				}
			}
			input.matches(Regex("play \\d \\d")) -> {
				return (input.split(" ")[1].toInt() * 9 + input.split(" ")[2].toInt()).toByte()
			}
			else -> {
				System.err.println("play command formatted incorrectly, enter help for command list")
				return null
			}
		}
	}
}
