import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class GiantMapBenchmark {
    @Test
    fun measureTime() {
        fun measureTime(
            iterations: Int,
        ): Long {
            return measureTimeMillis {
                repeat(iterations) {
                    val input = PlayerInput(
                        inputs = ("13, 69, 109, 11, 47, 100, 4, 36, 3, 17, 8, 1, 4, 9, 4, 23, 2, 23, 1, 50, 2," +
                                " 24, 6, 9, 2, 56, 8, 9, 1, 24, 2, 43, 7, 48, 10, 23, 1, 4, 11, 45, 6, 23, 1, 62," +
                                " 1, 17, 11, 50, 2, 9, 1, 36, 9, 2, 8, 63, 3, 30, 6, 3, 10, 45, 3, 60, 9, 17, 3, 24," +
                                " 10, 3, 8, 23, 6, 35, 2, 3, 11, 4, 5, 4, 0, 6, RIGHT, -1, -1, NONE, -1, -1, NONE, 0, 6, " +
                                "RIGHT, 1, 6, RIGHT, 1, 7, RIGHT, 1, 8, RIGHT, 1, 9, RIGHT, 1, 10, RIGHT, 1, 11, RIGHT, 1, 12," +
                                " RIGHT, 1, 13, RIGHT, 1, 14, RIGHT, 1, 15, RIGHT, 1, 16, RIGHT, 1, 17, RIGHT, 2, 17, RIGHT, 2, 18," +
                                " RIGHT, 2, 19, RIGHT, 2, 20, RIGHT, 2, 21, RIGHT, 2, 22, RIGHT, 2, 23, RIGHT, 3, 23, RIGHT," +
                                " 3, 24, RIGHT, 4, 24, RIGHT, 3, 23, RIGHT, 3, 24, RIGHT, 4, 24, LEFT, 4, 23, LEFT, 5, 23, LEFT," +
                                " 4, 24, LEFT, 4, 23, LEFT, 5, 23, LEFT, 6, 23, LEFT, 7, 23, LEFT, 5, 23, LEFT, 6, 23, LEFT," +
                                " 7, 23, RIGHT, 7, 24, RIGHT, 7, 25, RIGHT, 7, 26, RIGHT, 7, 27, RIGHT, 7, 28, RIGHT, 7, 29," +
                                " RIGHT, 7, 30, RIGHT, 7, 31, RIGHT, 7, 32, RIGHT, 7, 33, RIGHT, 7, 34, RIGHT, 7, 35, RIGHT," +
                                " 7, 36, RIGHT, 7, 37, RIGHT, 7, 38, RIGHT, 7, 39, RIGHT, 7, 40, RIGHT, 7, 41, RIGHT, 7, 42, " +
                                "RIGHT, 7, 43, RIGHT, 7, 44, RIGHT, 7, 45, RIGHT, 7, 46, RIGHT, 7, 47, RIGHT, 7, 48, RIGHT, " +
                                "8, 48, RIGHT, 8, 49, RIGHT, 8, 50, RIGHT, 8, 51, RIGHT, 8, 52, RIGHT, 8, 53, RIGHT, 8, 54, " +
                                "RIGHT, 8, 55, RIGHT, 8, 56, RIGHT, 8, 57, RIGHT, 8, 58, RIGHT, 8, 59, RIGHT, 8, 60, RIGHT," +
                                " 8, 61, RIGHT, 8, 62, RIGHT, 8, 63, RIGHT, 9, 63, RIGHT, 8, 62, RIGHT, 8, 63, RIGHT, 9, 63," +
                                " RIGHT, 10, 63, RIGHT, 8, 63, RIGHT, 9, 63, RIGHT, 10, 63, LEFT, 10, 62, LEFT, 10, 61, LEFT," +
                                " 10, 60, LEFT, 10, 59, LEFT, 10, 58, LEFT, 10, 57, LEFT, 10, 56, LEFT, 10, 55, LEFT, 10, 54," +
                                " LEFT, 10, 53, LEFT, 10, 52, LEFT, 10, 51, LEFT, 10, 50, LEFT, 10, 49, LEFT, 10, 51, LEFT, 10," +
                                " 50, LEFT, 10, 49, LEFT, 11, 49, LEFT, 11, 48, LEFT, 11, 47, LEFT").split(", ")
                    )

                    runGame(input)
                }
            } / iterations
        }

        fun warmUp() {
            measureTime(iterations = 1)
        }

        warmUp()

        val ms = measureTime(iterations = 100)

        println("ms: $ms")
    }
}
