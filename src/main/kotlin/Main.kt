import java.util.*

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args : Array<String>) {

    val input = Scanner(System.`in`)
    val nbFloors = input.nextInt() // number of floors
    val width = input.nextInt() // width of the area
    val nbRounds = input.nextInt() // maximum number of rounds
    val exitFloor = input.nextInt() // floor on which the exit is found
    val exitPos = input.nextInt() // position of the exit on its floor
    val nbTotalClones = input.nextInt() // number of generated clones
    val nbAdditionalElevators = input.nextInt() // number of additional elevators that you can build
    val nbElevators = input.nextInt() // number of elevators

    val elevators: List<HashSet<Int>> = List(exitFloor + 1) {
        hashSetOf()
    }

    for (i in 0 until nbElevators) {
        val elevatorFloor = input.nextInt() // floor on which this elevator is found
        val elevatorPos = input.nextInt() // position of the elevator on its floor

        if (elevatorFloor <= exitFloor) {
            elevators[elevatorFloor] += elevatorPos
        }
    }

    var mapIsBuilt = false

    fun ensureDriveMap() {
        if (mapIsBuilt) {
            return
        }

        fun buildEmptyCasesForFloor(): List<MutableList<Case>> {
            return List(size = width) {
                mutableListOf()
            }
        }

        fun buildExitFloorCases(): List<List<Case>> {
            val cases = buildEmptyCasesForFloor()
            var position = exitPos

            fun addCases(vararg newCases: Case) {
                cases[position] += newCases
            }

            addCases(
                Case(direction = Direction.LEFT, distance = 0, elevatorsReserved = 0, clonesReserved = 0),
                Case(direction = Direction.RIGHT, distance = 0, elevatorsReserved = 0, clonesReserved = 0),
            )

            position = exitPos - 1
            while (position >= 0 && position !in elevators[exitFloor]) {
                // так как этаж с выходом, то не нужен запас лифтов
                addCases(
                    // если клон бежит влево, а выход справа, то помимо расстояния до выхода
                    // надо ещё и развернуться - заблокировав одного клона из резерва
                    Case(
                        direction = Direction.LEFT,
                        distance = exitPos - position + 1,
                        elevatorsReserved = 0,
                        clonesReserved = 1,
                    ),
                    // если клон бежит вправо и выход справа, то нужно только
                    // пробежать расстояние до выхода без расходования клонов
                    Case(
                        direction = Direction.RIGHT,
                        distance = exitPos - position,
                        elevatorsReserved = 0,
                        clonesReserved = 0,
                    ),
                )
                position--
            }

            position = exitPos + 1
            while (position < width && position !in elevators[exitFloor]) {
                // так как этаж с выходом, то не нужен запас лифтов
                addCases(
                    // если клон бежит вправо, а выход слева, то помимо расстояния до выхода
                    // надо ещё и развернуться - заблокировав одного клона из резерва
                    Case(
                        direction = Direction.RIGHT,
                        distance = position - exitPos + 1,
                        elevatorsReserved = 0,
                        clonesReserved = 1,
                    ),
                    // если клон бежит влево и выход влево, то нужно только
                    // пробежать расстояние до выхода без расходования клонов
                    Case(
                        direction = Direction.LEFT,
                        distance = position - exitPos,
                        elevatorsReserved = 0,
                        clonesReserved = 0,
                    ),
                )
                position++
            }

            return cases
        }

        val exitFloorCases = buildExitFloorCases()



        mapIsBuilt = true
    }

    // game loop
    while (true) {

        ensureDriveMap()

        val cloneFloor = input.nextInt() // floor of the leading clone
        val clonePos = input.nextInt() // position of the leading clone on its floor
        val direction = input.next() // direction of the leading clone: LEFT or RIGHT

        fun noClone() = cloneFloor < 0

        if (noClone()) {
            wait()
        }

        wait()
    }
}

private fun debug(message: String) = System.err.println(message)

@JvmName("_wait")
private fun wait() = println("WAIT")

private fun block() = println("BLOCK")

private fun elevator() = println("ELEVATOR")

/**
 * Направление движения клона
 */
private enum class Direction {
    LEFT,
    RIGHT,
}

/**
 * Вариант дальнейшего движения - с указанием расстояния и требуемых ресурсов
 */
private data class Case(
    /**
     * Направление клона
     */
    val direction: Direction,
    /**
     * Количество раундов, необходимое для достижения выхода
     */
    val distance: Int,
    /**
     * Сколько лифтов минимум потребуется построить для достижения выхода
     */
    val elevatorsReserved: Int,
    /**
     * Сколько клонов должно быть в запасе для достижения выхода
     */
    val clonesReserved: Int,
)
