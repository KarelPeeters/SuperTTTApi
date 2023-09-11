package common

import java.util.concurrent.atomic.AtomicLong

// Xorshiro128++ implementation
class Xoroshiro @JvmOverloads constructor(seed: Long = defaultGen.getAndAdd(GOLDEN_RATIO_64)) {
    private var x0: Long
    private var x1: Long

    init {
        this.x0 = mixStafford13(seed xor SILVER_RATIO_64)
        this.x1 = mixStafford13(seed + GOLDEN_RATIO_64)
    }

    /*
     * The following two comments are quoted from http://prng.di.unimi.it/xoroshiro128plusplus.c
     */
    /*
     * To the extent possible under law, the author has dedicated all copyright
     * and related and neighboring rights to this software to the public domain
     * worldwide. This software is distributed without any warranty.
     * <p>
     * See http://creativecommons.org/publicdomain/zero/1.0/.
     */
    /*
     * This is xoroshiro128++ 1.0, one of our all-purpose, rock-solid,
     * small-state generators. It is extremely (sub-ns) fast and it passes all
     * tests we are aware of, but its state space is large enough only for
     * mild parallelism.
     * <p>
     * For generating just floating-point numbers, xoroshiro128+ is even
     * faster (but it has a very mild bias, see notes in the comments).
     * <p>
     * The state must be seeded so that it is not everywhere zero. If you have
     * a 64-bit seed, we suggest to seed a splitmix64 generator and use its
     * output to fill s.
     */
    fun nextLong(): Long {
        val s0 = x0
        var s1 = x1
        val result = java.lang.Long.rotateLeft(s0 + s1, 17) + s0 // "plusplus" scrambler
        s1 = s1 xor s0
        x0 = java.lang.Long.rotateLeft(s0, 49) xor s1 xor (s1 shl 21) // a, b
        x1 = java.lang.Long.rotateLeft(s1, 28) // c
        return result
    }

    // Derived random functions
    fun nextInt() = (nextLong() ushr 32).toInt()
    fun nextInt(bound: Int) = ((nextInt().toUInt().toULong() * bound.toULong()) shr 32).toInt()
    fun nextBoolean() = nextInt() < 0

    companion object {
        const val GOLDEN_RATIO_64 = -0x61c8864680b583ebL // (1 + sqrt(2))
        const val SILVER_RATIO_64 = 0x6A09E667F3BCC909L // (1 + sqrt(5))/2
        private val defaultGen = AtomicLong(initialSeed())

        // http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html
        private fun initialSeed() = mixStafford13(System.currentTimeMillis()) xor mixStafford13(System.nanoTime())
        private fun mixStafford13(z: Long): Long {
            var z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
            return z xor (z ushr 31)
        }
    }
}
