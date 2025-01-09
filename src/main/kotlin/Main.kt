import java.util.*

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args: Array<String>) {

    val input = Scanner(System.`in`)
    val nbFloors = input.nextInt() // number of floors
    val width = input.nextInt() // width of the area
    val nbRounds = input.nextInt() // maximum number of rounds
    val exitFloor = input.nextInt() // floor on which the exit is found
    val exitPos = input.nextInt() // position of the exit on its floor
    val nbTotalClones = input.nextInt() // number of generated clones
    val nbAdditionalElevators = input.nextInt() // number of additional elevators that you can build
    val nbElevators = input.nextInt() // number of elevators

    /**
     * Количество этажей, с которыми имеет смысл работать.
     * Более верхние этажи можно отбросить, так как с них невозможно добраться до выхода
     * и клонов на эти этажи пускать не будем
     */
    val workFloorsCount = exitFloor + 1

    class Elevators {

        private val elevators: List<HashSet<Int>> = List(workFloorsCount) {
            hashSetOf()
        }

        fun registerElevator(
            floorIndex: Int,
            position: Int,
        ) {
            if (floorIndex <= exitFloor) {
                elevators[floorIndex] += position
            }
        }

        fun isElevator(
            floorIndex: Int,
            position: Int,
        ): Boolean {
            return position in elevators[floorIndex]
        }
    }

    val elevators = Elevators()

    for (i in 0 until nbElevators) {
        val elevatorFloor = input.nextInt() // floor on which this elevator is found
        val elevatorPos = input.nextInt() // position of the elevator on its floor

        elevators
            .registerElevator(
                floorIndex = elevatorFloor,
                position = elevatorPos,
            )
    }

    class Floor(
        val floorIndex: Int,
    ) {
        private val floorCases: List<MutableList<Case>> = List(size = width) {
            mutableListOf()
        }

        val cases: List<List<Case>> get() = floorCases

        fun addCases(
            position: Int,
            vararg newCases: Case,
        ) {
            addCases(
                position = position,
                newCases = newCases.toList(),
            )
        }

        fun optimizeCases() {
            TODO("оптимизировать кейсы - убрать менее быстрые при текущих или более жёстких ограничениях")
            // более жёсткие ограничения - по всем признакам, не по какому-то одному
        }

        fun addCases(
            position: Int,
            newCases: List<Case>,
        ) {
            floorCases[position] += newCases
        }
    }

    class Drive(
        val floors: List<Floor>,
    ) {

    }

    /**
     * Варианты движения для каждого этажа и каждой ячейки на этаже
     */
    fun buildDrive(): Drive {
        fun buildExitFloor(): Floor {
            return Floor(floorIndex = exitFloor)
                .apply {
                    addCases(
                        position = exitPos,
                        Case(direction = Direction.LEFT, distance = 0, elevatorsReserved = 0, clonesReserved = 0),
                        Case(direction = Direction.RIGHT, distance = 0, elevatorsReserved = 0, clonesReserved = 0),
                    )

                    (exitPos - 1 downTo 0)
                        .takeWhile { position ->
                            !elevators.isElevator(floorIndex = floorIndex, position = position)
                        }
                        .forEach { position ->
                            // так как этаж с выходом, то не нужен запас лифтов
                            addCases(
                                position = position,
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
                        }

                    (exitPos + 1 until width)
                        .takeWhile { position ->
                            !elevators.isElevator(floorIndex = floorIndex, position = position)
                        }
                        .forEach { position ->
                            // так как этаж с выходом, то не нужен запас лифтов
                            addCases(
                                position = position,
                                // если клон бежит вправо, а выход слева, то помимо расстояния до выхода
                                // надо ещё и развернуться - заблокировав одного клона из резерва
                                Case(
                                    direction = Direction.RIGHT,
                                    distance = position - exitPos + 1,
                                    elevatorsReserved = 0,
                                    clonesReserved = 1,
                                ),
                                // если клон бежит влево и выход слева, то нужно только
                                // пробежать расстояние до выхода без расходования клонов
                                Case(
                                    direction = Direction.LEFT,
                                    distance = position - exitPos,
                                    elevatorsReserved = 0,
                                    clonesReserved = 0,
                                ),
                            )
                        }
                }
        }

        val exitFloorCases = buildExitFloor()

        return Drive(
            floors = generateSequence(
                seed = exitFloorCases,
            ) { upperFloor ->
                Floor(
                    floorIndex = upperFloor.floorIndex - 1,
                ).apply {

                    val existingElevatorCasesByPosition = mutableMapOf<Int, List<Case>>()

                    (0 until width)
                        .forEach { position ->
                            val isElevator = elevators
                                .isElevator(
                                    floorIndex = floorIndex,
                                    position = position,
                                )

                            if (isElevator) {
                                // вариант 1: подняться на существующем лифте
                                addCases(
                                    position = position,
                                    newCases = upperFloor
                                        .cases[position]
                                        .map {
                                            it.copy(
                                                distance = it.distance + 1,
                                            )
                                        }
                                        .also {
                                            existingElevatorCasesByPosition[position] = it
                                        }
                                )
                            } else {
                                // вариант 2: построить лифт (уменьшив запасы лифтов и клонов)
                                addCases(
                                    position = position,
                                    newCases = upperFloor
                                        .cases[position]
                                        .map {
                                            it.copy(
                                                distance = it.distance + 1,
                                                elevatorsReserved = it.elevatorsReserved + 1,
                                                clonesReserved = it.clonesReserved + 1,
                                            )
                                        }
                                )
                            }
                        }

                    class HorizontalRunCase(
                        val position: Int,
                        val cases: List<Case>,
                        val isElevator: Boolean,
                    )

                    // вариант 3: бежать к лифту налево
                    val leftMostPosition = 0
                    generateSequence(
                        HorizontalRunCase(
                            position = leftMostPosition,
                            cases = emptyList(), // с левой позиции бесполезно бежать налево
                            isElevator = elevators
                                .isElevator(
                                    floorIndex = floorIndex,
                                    position = leftMostPosition,
                                )
                        )
                    ) { previousCase ->

                        val currentPosition = previousCase.position + 1

                        val isElevator = elevators
                            .isElevator(
                                floorIndex = floorIndex,
                                position = currentPosition,
                            )

                        if (isElevator) {
                            HorizontalRunCase(
                                position = currentPosition,
                                cases = emptyList(), // из лифта налево не убежишь - никуда не убежишь
                                isElevator = true,
                            )
                        } else {
                            if (previousCase.isElevator) {
                                HorizontalRunCase(
                                    position = currentPosition,
                                    // ранее для всех лифтов заполнили existingElevatorCasesByPosition (вариант 1)
                                    cases = existingElevatorCasesByPosition[previousCase.position]!!
                                        .map {
                                            it.copy(distance = it.distance + 1)
                                        },
                                    isElevator = false,
                                )
                            } else {
                                HorizontalRunCase(
                                    position = currentPosition,
                                    cases = previousCase
                                        .cases
                                        .map {
                                            it.copy(distance = it.distance + 1)
                                        },
                                    isElevator = false,
                                )
                            }
                        }
                    }
                        .take(width)
                        .forEach { horizontalRunCase ->
                            addCases(
                                position = horizontalRunCase.position,
                                newCases = horizontalRunCase.cases,
                            )
                        }

                    // вариант 4: бежать к лифту направо
                    val rightMostPosition = width - 1
                    generateSequence(
                        HorizontalRunCase(
                            position = rightMostPosition,
                            cases = emptyList(), // с правой позиции бесполезно бежать направо
                            isElevator = elevators
                                .isElevator(
                                    floorIndex = floorIndex,
                                    position = rightMostPosition,
                                )
                        )
                    ) { previousCase ->

                        val currentPosition = previousCase.position - 1

                        val isElevator = elevators
                            .isElevator(
                                floorIndex = floorIndex,
                                position = currentPosition,
                            )

                        if (isElevator) {
                            HorizontalRunCase(
                                position = currentPosition,
                                cases = emptyList(), // из лифта направо не убежишь - никуда не убежишь
                                isElevator = true,
                            )
                        } else {
                            if (previousCase.isElevator) {
                                HorizontalRunCase(
                                    position = currentPosition,
                                    // ранее для всех лифтов заполнили existingElevatorCasesByPosition (вариант 1)
                                    cases = existingElevatorCasesByPosition[previousCase.position]!!
                                        .map {
                                            it.copy(distance = it.distance + 1)
                                        },
                                    isElevator = false,
                                )
                            } else {
                                HorizontalRunCase(
                                    position = currentPosition,
                                    cases = previousCase
                                        .cases
                                        .map {
                                            it.copy(distance = it.distance + 1)
                                        },
                                    isElevator = false,
                                )
                            }
                        }
                    }
                        .take(width)
                        .forEach { horizontalRunCase ->
                            addCases(
                                position = horizontalRunCase.position,
                                newCases = horizontalRunCase.cases,
                            )
                        }

                    optimizeCases()
                }
            }
                .take(workFloorsCount)
                .toList()
                .reversed()
        )
    }

    val driveMap = buildDrive()

    // game loop
    while (true) {
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
