package com.flaghacker.sttt.common

import java.io.Serializable

class KotlinBoard : Serializable {
    /*
    Each element represents a row of macros (3x9 tiles)
    The first 3 elements are for Player
    The next 3 elements are for Enemy
     */
    private var rows: Array<Int> = Array(6, { 0 })

    private var wonBy = KotlinPlayer.NEUTRAL
    private var nextPlayer = KotlinPlayer.PLAYER

    private var lastMove: Int? = null

    init {
        play(2)
        play(3)
        play(40)
        play(9)
        println(toString())
    }

    fun play(index: Int){
        val mRow = index / 27
        val mNum = index % 27
        val free = ((rows[mRow] or rows[mRow + 3]).inv() shr mNum) and 1

        if (free != 1)
            throw RuntimeException("Position $index not available")

        rows[(nextPlayer.value - 1) * 3 + mRow] += (1 shl mNum)

        lastMove = index
        nextPlayer = nextPlayer.other()
    }

    private fun getVal(mRow: Int, mNum: Int): Int = (rows[mRow] shr mNum) and 1
    private fun putVal(mRow: Int, mNum: Int, value: Int) { //Value: 0,1
        rows[mRow] += (value shl mNum)
    }

    override fun toString(): String {
        var output = ""
        for (i in 0..80) {
            val mRox = i / 27
            val mNum = i % 27

            output +=
                    if (i == 0 || i == 80) ""
                    else if (i % 27 == 0) "\n---+---+---\n"
                    else if (i % 9 == 0) "\n"
                    else if (i%3==0 || i%6==0) "|"
                    else ""

            output += when {
                getVal(mRox, mNum) == 1 -> "X"
                getVal(mRox + 3, mNum) == 1 -> "O"
                else -> " "
            }

        }
        return output
    }
}