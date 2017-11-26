package com.flaghacker.sttt.common

import java.io.Serializable

class KotlinBoard : Serializable {
    /*
    Each element represents a row of macros (3x9 tiles)
    The first 3 Ints hold the macros for Player
    The next 3 Ints hold the macros for Enemy

    In each Int the bit representation is as follows:
    aaaaaaaaabbbbbbbbbcccccccccABC with:
    a: elements of the first macro
    b: elements of the second macro
    c: elements of the third macro
    A: winner of first macro
    B: winner of second macro
    C: winner of third macro
     */
    private var rows: Array<Int> = Array(6, { 0 })

    private var wonBy = KotlinPlayer.NEUTRAL
    private var nextPlayer = KotlinPlayer.PLAYER

    private var macroMask = 0b111111111
    private var lastMove: Byte? = null

    fun nextPlayer() = nextPlayer
    fun isDone() = wonBy != KotlinPlayer.NEUTRAL
    fun wonBy() = wonBy
    fun getLastMove() = lastMove

    fun flip(): KotlinBoard {
        val board = copy()

        val newRows = Array(6) { 0 }
        for (i in 0..2)
            newRows[i] = board.rows[i + 3]
        for (i in 3..5)
            newRows[i] = board.rows[i + 3]
        board.rows = newRows

        board.wonBy = board.wonBy.otherWithNeutral()
        board.nextPlayer = board.nextPlayer.otherWithNeutral()

        return board
    }

    fun copy(): KotlinBoard {
        val copy = KotlinBoard()
        copy.rows = rows.copyOf()
        copy.wonBy = wonBy
        copy.nextPlayer = nextPlayer
        copy.macroMask = macroMask
        copy.lastMove = lastMove
        return copy
    }

    init {
        //println("start")
        //play(0)
        //play(1)
/*        play(27)
        play(53)
        play(30)
        play(31)
        play(40)
        play(39)
        play(50)*/
        //println(toString())
        //println(Integer.toBinaryString(getMacro(3,KotlinPlayer.PLAYER)))
    }

    fun play(index: Byte): Boolean {
        val row = index / 27 //Row 0,1,2
        val macroShift = (index / 9) % 3 * 9     //Shift to go to the right micro
        val moveShift = index % 9                //Shift required for index within matrix
        val shift = moveShift + macroShift

        //println("PLAY($nextPlayer) | index:$index row:$row shift:$shift") //TODO

        //If the move is not available throw exception
        if ((rows[row] or rows[row + 3]).getBit(shift) || !macroMask.getBit((index / 27) * 3 + (macroShift / 9)))
            throw RuntimeException("Position $index not available")
        else if (wonBy != KotlinPlayer.NEUTRAL)
            throw RuntimeException("The game is over")

        //Write the move to the board
        val pRow = (nextPlayer.value - 1) * 3 + row
        rows[pRow] += (1 shl shift)

        //bPrint(rows[pRow]) //TODO

        val macroWin = wonGrid((rows[pRow] shr macroShift) and 0b111111111, moveShift)
        var winGrid = 0
        if (macroWin) {
            rows[pRow] += (1 shl (27 + macroShift / 9)) //27 + macro number

            //Create the winGrid of the player
            winGrid = (rows[0 + 3 * (nextPlayer.value - 1)] shr 27)
                    .or((rows[1 + 3 * (nextPlayer.value - 1)] shr 27) shl 3)
                    .or((rows[2 + 3 * (nextPlayer.value - 1)] shr 27) shl 6)

            //println("winGrid: ${Integer.toBinaryString(winGrid)}")
            if (wonGrid(winGrid, index / 9))
                wonBy = nextPlayer

            //Add the winGrid of the enemy
            winGrid or (rows[0 + 3 * (nextPlayer.other().value - 1)] shr 27)
                    .or((rows[1 + 3 * (nextPlayer.other().value - 1)] shr 27) shl 3)
                    .or((rows[2 + 3 * (nextPlayer.other().value - 1)] shr 27) shl 6)
        } else {
            winGrid = ((rows[0] and rows[3]) shr 27)
                    .or(((rows[1] and rows[4]) shr 27) shl 3)
                    .or(((rows[2] and rows[5]) shr 27) shl 6)
        }

        //Prepare the board for the next player
        val freeMove = !winGrid.inv().getBit(moveShift) || macroFull(moveShift)
        //val freeMove = (rows[moveShift / 3] and rows[3 + moveShift / 3]).getBit(27 + moveShift % 3)
        macroMask =
                if (freeMove) (0b111111111 and winGrid.inv())
                else (1 shl moveShift)
        lastMove = index
        nextPlayer = nextPlayer.other()

        return macroWin
    }

    private fun macroFull(om: Int): Boolean = (rows[om / 3] or rows[3 + om / 3]).shr((om % 3)*9).isMaskSet(0b111111111)

    fun availableMoves(): List<Byte> {
        val output = ArrayList<Byte>()

        for (macro in 0..8) {
            if (macroMask.getBit(macro)) {
                val row = rows[macro / 3] or rows[macro / 3 + 3]
                (0..8).map { it + macro * 9 }.filter { !row.getBit(it % 27) }.mapTo(output) { it.toByte() }
            }
        }

        return output
    }

    fun macro(om: Int): KotlinPlayer = when {
        rows[om / 3].getBit(27 + om % 3) -> KotlinPlayer.PLAYER
        rows[3 + om / 3].getBit(27 + om % 3) -> KotlinPlayer.ENEMY
        else -> KotlinPlayer.NEUTRAL
    }

    fun tile(o: Int): KotlinPlayer = when {
        rows[o / 27].getBit(o % 27) -> KotlinPlayer.PLAYER
        rows[3 + o / 27].getBit(o % 27) -> KotlinPlayer.ENEMY
        else -> KotlinPlayer.NEUTRAL
    }

    private fun bPrint(int: Int): String {
        var out = ""
        for (i in 1..(27 - Integer.toBinaryString(int).length)) {
            out += "0"
        }
        out += Integer.toBinaryString(int)
        var inv = ""
        for (i in out.length - 1 downTo 0) {
            inv += out[i]
        }
        println(inv)
        println(toString())
        return inv
    }

    private fun getVal(mRow: Int, mNum: Int): Int = (rows[mRow] shr mNum) and 1
    private fun Int.getBit(index: Int) = ((this shr index) and 1) == 1
    private fun Int.print() = Integer.toBinaryString(this)
    private fun Int.isMaskSet(mask: Int) = this and mask == mask

    private fun wonGrid(grid: Int, index: Int): Boolean {
        when (index) {
            4 ->                                                        //Center
                return grid.getBit(1) && grid.getBit(7)                 //line |
                        || grid.getBit(3) && grid.getBit(5)             //line -
                        || grid.getBit(0) && grid.getBit(8)             //line \
                        || grid.getBit(6) && grid.getBit(2)             //line /
            3, 5 ->                                                     //Horizontal sides
                return grid.getBit(index - 3) && grid.getBit(index + 3) //line |
                        || grid.getBit(4) && grid.getBit(8 - index)     //line -
            1, 7 ->                                                     //Vertical sides
                return grid.getBit(index - 1) && grid.getBit(index + 1) //line |
                        || grid.getBit(4) && grid.getBit(8 - index)     //line -
            else -> {   //corners
                val x = index % 3
                val y = index / 3
                return grid.getBit(4) && grid.getBit(8 - index)                                    //line \ or /
                        || grid.getBit(3 * y + (x + 1) % 2) && grid.getBit(3 * y + (x + 2) % 4)    //line -
                        || grid.getBit(x + ((y + 1) % 2) * 3) && grid.getBit(x + ((y + 2) % 4) * 3)//line |
            }
        }
    }

    override fun toString(): String {
        var output = ""
        for (i in 0..80) {
            val row = i / 27 //Row 0,1,2

            val xs = i % 3
            val ys = (i / 9) % 3
            val tileShift = xs + 3 * ys
            val macroShift = (i%9)/3 * 9


            val shift = tileShift + macroShift

            output +=
                    if (i == 0 || i == 80) ""
                    else if (i % 27 == 0) "\n---+---+---\n"
                    else if (i % 9 == 0) "\n"
                    else if (i % 3 == 0 || i % 6 == 0) "|"
                    else ""

            output += when {
                getVal(row, shift) == 1 -> "X"
                getVal(row + 3, shift) == 1 -> "O"
                else -> " "
            }

        }
        return output
    }
}