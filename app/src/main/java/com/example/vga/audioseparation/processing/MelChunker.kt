package com.example.vga.audioseparation.processing

object MelChunker {

    private const val CHUNK_SIZE = 160
    private const val N_MELS = 40

    /**
     * Splits a mel spectrogram:
     *
     * [frames][40]
     *
     * into complete:
     *
     * [160][40]
     *
     * chunks.
     *
     * Incomplete final chunks are discarded.
     */

    fun prepareForEncoder(
        mel: Array<FloatArray>
    ): Array<FloatArray> {

        val result =
            Array(160) {
                FloatArray(40)
            }

        val frames =
            minOf(
                mel.size,
                160
            )

        for (i in 0 until frames) {

            System.arraycopy(
                mel[i],
                0,
                result[i],
                0,
                40
            )
        }

        return result
    }

    fun logFirstChunk(
        chunk: Array<FloatArray>
    ) {

        if (chunk.size != CHUNK_SIZE) {
            println(
                "MEL_CHUNKS: invalid chunk size=${chunk.size}"
            )
            return
        }

        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        var sum = 0.0

        for (row in chunk) {
            for (value in row) {

                if (value < min) min = value
                if (value > max) max = value

                sum += value
            }
        }

        val count =
            CHUNK_SIZE * N_MELS

        println(
            "MEL_CHUNK_TEST: shape=" +
                    "(${CHUNK_SIZE}, $N_MELS)"
        )

        println(
            "MEL_CHUNK_TEST: mean=${sum / count}"
        )

        println(
            "MEL_CHUNK_TEST: min=$min"
        )

        println(
            "MEL_CHUNK_TEST: max=$max"
        )

        print(
            "MEL_CHUNK_TEST: first10="
        )

        for (i in 0 until 10) {
            print("${chunk[0][i]} ")
        }

        println()
    }

    fun chunk(
        mel: Array<FloatArray>
    ): List<Array<FloatArray>> {

        if (mel.isEmpty()) {
            return emptyList()
        }

        val numberOfChunks =
            mel.size / CHUNK_SIZE

        val chunks =
            ArrayList<Array<FloatArray>>(
                numberOfChunks
            )

        for (chunkIndex in 0 until numberOfChunks) {

            val start =
                chunkIndex * CHUNK_SIZE

            val chunk =
                Array(CHUNK_SIZE) {
                    FloatArray(N_MELS)
                }

            for (frameIndex in 0 until CHUNK_SIZE) {

                for (melIndex in 0 until N_MELS) {

                    chunk[frameIndex][melIndex] =
                        mel[start + frameIndex][melIndex]
                }
            }

            chunks.add(chunk)
        }

        return chunks
    }

    fun logSummary(
        chunks: List<Array<FloatArray>>
    ) {

        if (chunks.isEmpty()) {

            println(
                "MEL_CHUNKS: no complete chunks"
            )

            return
        }

        println(
            "MEL_CHUNKS: count=${chunks.size}"
        )

        println(
            "MEL_CHUNKS: first shape=" +
                    "(${chunks[0].size}, " +
                    "${chunks[0][0].size})"
        )

        val lastChunkFrames =
            chunks.last().size

        println(
            "MEL_CHUNKS: last complete chunk frames=" +
                    lastChunkFrames
        )
    }
}