/*
 * Copyright 2016 riddles.io (developers@riddles.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     For the full copyright and license information, please view the LICENSE
 *     file that was distributed with this source code.
 */

package com.flaghacker.sttt.games

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Timer
import java.util.*

/**
 * com.flaghacker.sttt.games.RiddlesIOGame
 *
 * Main class that will keep reading output from the engine.
 * Will either update the bot state or get actions.
 *
 * @author Jim van Eeden - jim@riddles.io
 */

class RiddlesIOGame(private val bot: Bot) {

    private val scan: Scanner = Scanner(System.`in`)

    private var timePerMove: Int = 0
    private var maxTimebank: Int = 0
    private var timeBank: Int = 0

    private var roundNumber: Int = 0
    private var myName: String? = null
    private var myId = 0

    private var rows: Array<Int> = Array(6, { 0 })
    private var macroMask = 0b111111111

    fun run() {
        while (scan.hasNextLine()) {
            val line = scan.nextLine()

            if (line.isEmpty()) continue

            val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            when (parts[0]) {
                "settings" -> parseSettings(parts[1], parts[2])
                "update" -> if (parts[1] == "game") {
                    parseGameData(parts[2], parts[3])
                }
                "action" -> if (parts[1] == "move") { /* move requested */
/*                    timeBank = Integer.parseInt(parts[2])
                    val move = bot.move(Board(rows,macroMask), Timer(80))!!.toInt()

                    val os = move%9
                    val om = move/9

                    val x = (om%3)*3 + os%3
                    val y = (om/3)*3 + os/3

                    println("place_move $x $y")*/
                }
                else -> println("unknown command")
            }
        }
    }

    private fun parseSettings(key: String, value: String) {
        when (key) {
            "timebank" -> {
                val time = Integer.parseInt(value)
                maxTimebank = time
                timeBank = time
            }
            "time_per_move" -> timePerMove = Integer.parseInt(value)
            "your_bot" -> myName = value
            "your_botid" -> myId = Integer.parseInt(value)
            else -> throw RuntimeException("Cannot parse game data input with key $key")
        }

    }

    private fun parseGameData(key: String, value: String) {
        when (key) {
            "round" -> roundNumber = Integer.parseInt(value)
            "field" -> {
                val parsed = value.replace(",", "")

                if (parsed.length != 81 || parsed.replace("X", "").replace("O", "").isBlank())
                    throw IllegalArgumentException("board string formatted incorrectly (input: $parsed)")

                rows = Array(6, { 0 })
                for (i in 0 until 81) {
                    val char = parsed[i]
                    if (char != '.') {
                        //Calculate row
                        val row = (if (Character.getNumericValue(char) == myId) 0 else 1) * 3 + i / 27

                        //Calculate shift
                        val ir = i % 27
                        val x = ir % 9
                        val macroShift = (x / 3) * 9
                        val moveShift = (x % 3) + (ir / 9) * 3
                        val shift = macroShift + moveShift

                        //Put the 1 in the correct spot
                        rows[row] += (1 shl shift)
                    }
                }
            }
            "macroboard" -> {
                val parsed = value.split(',')

                if (parsed.size != 9)
                    throw IllegalArgumentException("macro mask formatted incorrectly (input: $parsed)")

                macroMask = 0
                for (i in 0 until 9) {
                    val macro = parsed[i]
                    when (macro) {
                        "." -> { //Available but unplayable
                        }
                        "-1" -> macroMask += 1 shl i //Available and playable
                        myId.toString() -> rows[i / 3] += (1 shl ((i % 3) + 27)) //Won
                        else -> rows[3 + (i / 3)] += (1 shl ((i % 3) + 27)) //Lost
                    }
                }
            }
            else -> throw RuntimeException("Cannot parse game data input with key $key")
        }

    }
}