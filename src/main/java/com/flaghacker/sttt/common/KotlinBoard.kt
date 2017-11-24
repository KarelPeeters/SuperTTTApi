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

    private var lastMove: Int? = null

    init {
        play(0)
        play(1)
        play(10)
        play(2)
        play(20)
        play(21)
        play(30)
        play(22)
        play(40)
        play(23)
        play(50)
        play(24)
        play(60)
        play(25)
        play(70)
        play(26)
        play(80)
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

    fun play(index: Int): Boolean {
        val row = index / 27 //Row 0,1,2

        val macroShift = (index / 3) % 3 * 9             //Shift to go to the right micro (9om)
        val moveShift = index % 3 + (index / 9) % 3 * 3  //Shift required for index within matrix (xs + 3ys)
        val shift = moveShift + macroShift               //xs + 3ys + 9om

        println("PLAY($nextPlayer) | index:$index row:$row shift:$shift") //TODO

        //If the move is not available throw exception
        if (nthBitIs1((rows[row] or rows[row + 3]).inv(),shift))
            throw RuntimeException("Position $index not available")
        else if (wonBy!=KotlinPlayer.NEUTRAL)
            throw RuntimeException("The game is over")

        //Write the move to the board
        val pRow = (nextPlayer.value - 1) * 3 + row
        rows[pRow] += (1 shl shift)

        //bPrint(rows[pRow]) //TODO

        val macroWin = wonGrid((rows[pRow] shr macroShift) and 0b111111111, moveShift)
        if (macroWin) {
            rows[pRow] += (1 shl (27 + (index / 3) % 3)) //27 + macro number

            val wingrid = (rows[0 + 3 * (nextPlayer.value-1)] shr 27)
                    .or((rows[1 + 3 * (nextPlayer.value-1)] shr 27) shl 3)
                    .or((rows[2 + 3 * (nextPlayer.value-1)] shr 27) shl 6)

            println("wingrid: ${Integer.toBinaryString(wingrid)}")
            if (wonGrid(wingrid, index/9))
                wonBy=nextPlayer
        }

        //Update last move and set next player
        lastMove = shift
        nextPlayer = nextPlayer.other()

        return macroWin
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
    private fun nthBitIs1(input: Int, n: Int): Boolean = ((input shr n) and 1) == 1

    private fun wonGrid(grid: Int, index: Int): Boolean {
        println("board: ${Integer.toBinaryString(grid)} index:$index")
        when (index) {
            4 ->                                                                //Center
                return nthBitIs1(grid, 1) && nthBitIs1(grid, 7)                 //line |
                        || nthBitIs1(grid, 3) && nthBitIs1(grid, 5)             //line -
                        || nthBitIs1(grid, 0) && nthBitIs1(grid, 8)             //line \
                        || nthBitIs1(grid, 6) && nthBitIs1(grid, 2)             //line /
            3, 5 ->                                                             //Horizontal sides
                return nthBitIs1(grid, index - 3) && nthBitIs1(grid, index + 3) //line |
                        || nthBitIs1(grid, 4) && nthBitIs1(grid, 8 - index)     //line -
            1, 7 ->                                                             //Vertical sides
                return nthBitIs1(grid, index - 1) && nthBitIs1(grid, index + 1) //line |
                        || nthBitIs1(grid, 4) && nthBitIs1(grid, 8 - index)     //line -
            else -> {   //corners
                val x = index % 3
                val y = index / 3
                return nthBitIs1(grid, 4) && nthBitIs1(grid, 8 - index)                                    //line \ or /
                        || nthBitIs1(grid, 3 * y + (x + 1) % 2) && nthBitIs1(grid, 3 * y + (x + 2) % 4)    //line -
                        || nthBitIs1(grid, x + ((y + 1) % 2) * 3) && nthBitIs1(grid, x + ((y + 2) % 4) * 3)//line |
            }
        }
    }

    override fun toString(): String {
        var output = ""
        for (i in 0..80) {
            val row = i / 27 //Row 0,1,2

            val macro = (i % 9) / 3
            val xpos = (i % 9) % 3
            val ypos = (i / 9) % 3

            val shift = xpos + ypos * 3 + macro * 9

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