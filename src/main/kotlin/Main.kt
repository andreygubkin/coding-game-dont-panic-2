import Case.Companion.optimize
import java.util.*

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {

    val input = Scanner(System.`in`)

    @Suppress("UNUSED_VARIABLE")
    val nbFloors =
        input.nextInt() // number of floors in the area. A clone can move between floor 0 and floor nbFloors - 1

    val width =
        input.nextInt() // the width of the area. The clone can move without being destroyed between position 0 and position width - 1

    @Suppress("UNUSED_VARIABLE")
    val nbRounds = input.nextInt() // maximum number of rounds before the end of the game

    val exitFloor = input.nextInt() // the floor on which the exit is located
    val exitPos = input.nextInt() // the position of the exit on its floor
    val nbTotalClones = input.nextInt() // the number of clones that will come out of the generator during the game
    val nbAdditionalElevators = input.nextInt() // number of additional elevators that you can build
    val nbElevators = input.nextInt() // number of elevators in the area

    /**
     * Количество этажей, с которыми имеет смысл работать.
     * Более верхние этажи можно отбросить, так как с них невозможно добраться до выхода
     * и клонов на эти этажи пускать не будем
     */
    val workFloorsCount = exitFloor + 1

    data class AreaPoint(
        val floor: Int,
        val position: Int,
    )

    class Elevators {

        private val elevators: List<HashSet<Int>> = List(workFloorsCount) {
            hashSetOf()
        }

        fun registerElevator(
            elevator: AreaPoint,
        ) {
            if (elevator.floor <= exitFloor) {
                elevators[elevator.floor] += elevator.position
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
                elevator = AreaPoint(
                    floor = elevatorFloor,
                    position = elevatorPos,
                ),
            )
    }

    class Floor(
        val floorIndex: Int,
    ) {
        private val floorCases: List<MutableList<Case>> = List(size = width) {
            mutableListOf()
        }

        val cases: List<List<Case>> get() = floorCases

        fun optimizeCases() {
            floorCases
                .forEach { positionCases ->
                    val optimizedPositionCases = positionCases.optimize()
                    positionCases.clear()
                    positionCases.addAll(optimizedPositionCases)
                }
        }

        fun addCases(
            position: Int,
            vararg newCases: Case,
        ) {
            addCases(
                position = position,
                newCases = newCases.toList(),
            )
        }

        fun addCases(
            position: Int,
            newCases: List<Case>,
        ) {
            floorCases[position] += newCases
        }
    }

    class Area(
        val floors: List<Floor>,
    )

    /**
     * Варианты движения для каждого этажа и каждой ячейки на этаже
     */
    fun buildArea(): Area {
        fun buildExitFloor(): Floor {
            return Floor(floorIndex = exitFloor)
                .apply {
                    addCases(
                        position = exitPos,
                        Case(
                            direction = Direction.LEFT,
                            distance = 0,
                            haveToBuildElevator = false,
                            constraints = StateConstraints(
                                elevatorsLeft = 0,
                                clonesLeft = 1, // текущий - кто будет спасён
                            ),
                        ),
                        Case(
                            direction = Direction.RIGHT,
                            distance = 0,
                            haveToBuildElevator = false,
                            constraints = StateConstraints(
                                elevatorsLeft = 0,
                                clonesLeft = 1, // текущий - кто будет спасён
                            ),
                        ),
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
                                    haveToBuildElevator = false,
                                    constraints = StateConstraints(
                                        elevatorsLeft = 0,
                                        clonesLeft = 2, // текущий - кого заблокируем, и следующий - кто будет спасён
                                    ),
                                ),
                                // если клон бежит вправо и выход справа, то нужно только
                                // пробежать расстояние до выхода без расходования клонов
                                Case(
                                    direction = Direction.RIGHT,
                                    distance = exitPos - position,
                                    haveToBuildElevator = false,
                                    constraints = StateConstraints(
                                        elevatorsLeft = 0,
                                        clonesLeft = 1, // текущий - кто будет спасён
                                    ),
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
                                    haveToBuildElevator = false,
                                    constraints = StateConstraints(
                                        elevatorsLeft = 0,
                                        clonesLeft = 2, // текущий - кого заблокируем, и следующий - кто будет спасён
                                    ),
                                ),
                                // если клон бежит влево и выход слева, то нужно только
                                // пробежать расстояние до выхода без расходования клонов
                                Case(
                                    direction = Direction.LEFT,
                                    distance = position - exitPos,
                                    haveToBuildElevator = false,
                                    constraints = StateConstraints(
                                        elevatorsLeft = 0,
                                        clonesLeft = 1, // текущий - кто будет спасён
                                    ),
                                ),
                            )
                        }
                }
        }

        val exitFloorCases = buildExitFloor()

        return Area(
            floors = generateSequence(
                seed = exitFloorCases,
            ) { upperFloor ->
                Floor(
                    floorIndex = upperFloor.floorIndex - 1,
                ).apply {

                    val existingElevatorCasesByPosition = mutableMapOf<Int, List<Case>>()
                    val newElevatorCasesByPosition = mutableMapOf<Int, List<Case>>()

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
                                                haveToBuildElevator = true,
                                                constraints = it.constraints.copy(
                                                    elevatorsLeft = it.constraints.elevatorsLeft + 1,
                                                    clonesLeft = it.constraints.clonesLeft + 1,
                                                ),
                                            )
                                        }
                                        .also {
                                            newElevatorCasesByPosition[position] = it
                                        }
                                )
                            }
                        }

                    class HorizontalRunCase(
                        val position: Int,
                        val cases: List<Case>,
                        val isElevator: Boolean,
                    )

                    // вариант 3: бежать налево к ближнему лифту или к лучшей возможной точке построения нового лифта
                    val leftMostPosition = 0
                    generateSequence(
                        HorizontalRunCase(
                            position = leftMostPosition,
                            cases = emptyList(), // с левой позиции бесполезно бежать налево
                            isElevator = elevators
                                .isElevator(
                                    floorIndex = floorIndex,
                                    position = leftMostPosition,
                                ),
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
                            val previousCases: List<Case> = if (previousCase.isElevator) {
                                // ранее для всех лифтов заполнили existingElevatorCasesByPosition (вариант 1)
                                existingElevatorCasesByPosition[previousCase.position]!!
                            } else {
                                // предыдущая позиция - не лифт и текущая - не лифт
                                // на предыдущей позиции могли быть кейсы на построение нового лифта
                                // или бег влево к существующему лифту
                                // и надо понять, стоит ли в текущей позиции бежать влево ради этих кейсов

                                newElevatorCasesByPosition[previousCase.position]!! + previousCase.cases
                            }

                            HorizontalRunCase(
                                position = currentPosition,
                                cases = previousCases
                                    .flatMap {
                                        listOf(
                                            it.copy(
                                                direction = Direction.LEFT,
                                                distance = it.distance + 1,
                                                haveToBuildElevator = false,
                                            ),
                                            it.copy(
                                                direction = Direction.RIGHT,
                                                distance = it.distance + 2,
                                                haveToBuildElevator = false,
                                                constraints = it.constraints.copy(
                                                    clonesLeft = it.constraints.clonesLeft - 1,
                                                ),
                                            ),
                                        )
                                    }
                                    .optimize(),
                                isElevator = false,
                            )
                        }
                    }
                        .take(width)
                        .forEach { horizontalRunCase ->
                            addCases(
                                position = horizontalRunCase.position,
                                newCases = horizontalRunCase.cases,
                            )
                        }

                    // вариант 4: бежать направо к ближнему лифту или к лучшей возможной точке построения нового лифта
                    val rightMostPosition = width - 1
                    generateSequence(
                        HorizontalRunCase(
                            position = rightMostPosition,
                            cases = emptyList(), // с правой позиции бесполезно бежать вправо
                            isElevator = elevators
                                .isElevator(
                                    floorIndex = floorIndex,
                                    position = rightMostPosition,
                                ),
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
                            val previousCases: List<Case> = if (previousCase.isElevator) {
                                // ранее для всех лифтов заполнили existingElevatorCasesByPosition (вариант 1)
                                existingElevatorCasesByPosition[previousCase.position]!!
                            } else {
                                // предыдущая позиция - не лифт и текущая - не лифт
                                // на предыдущей позиции могли быть кейсы на построение нового лифта
                                // или бег вправо к существующему лифту
                                // и надо понять, стоит ли в текущей позиции бежать вправо ради этих кейсов

                                newElevatorCasesByPosition[previousCase.position]!! + previousCase.cases
                            }

                            HorizontalRunCase(
                                position = currentPosition,
                                cases = previousCases
                                    .flatMap {
                                        listOf(
                                            it.copy(
                                                direction = Direction.RIGHT,
                                                distance = it.distance + 1,
                                                haveToBuildElevator = false,
                                            ),
                                            it.copy(
                                                direction = Direction.LEFT,
                                                distance = it.distance + 2,
                                                haveToBuildElevator = false,
                                                constraints = it.constraints.copy(
                                                    clonesLeft = it.constraints.clonesLeft - 1,
                                                ),
                                            ),
                                        )
                                    }
                                    .optimize(),
                                isElevator = false,
                            )
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

    val area = buildArea()

    var resources = StateConstraints(
        clonesLeft = nbTotalClones,
        elevatorsLeft = nbAdditionalElevators,
    )

    fun doNothingAndWait() {
        println("WAIT")
    }

    fun blockClone() {
        resources = resources
            .let {
                it.copy(
                    clonesLeft = it.clonesLeft - 1,
                )
            }
        println("BLOCK")
    }

    val newElevators = hashSetOf<AreaPoint>()

    // game loop
    while (true) {
        val cloneFloor = input.nextInt() // floor of the leading clone
        val clonePos = input.nextInt() // position of the leading clone on its floor
        // direction of the leading clone: LEFT or RIGHT
        val direction = input.next().let { Direction.valueOf(it) }

        val clone = AreaPoint(
            floor = cloneFloor,
            position = clonePos,
        )

        fun noClone() = cloneFloor < 0

        if (noClone()) {
            doNothingAndWait()
            continue
        }

        fun buildElevator() {
            resources = resources
                .let {
                    it.copy(
                        clonesLeft = it.clonesLeft - 1,
                        elevatorsLeft = it.elevatorsLeft - 1,
                    )
                }
            newElevators += clone
            println("ELEVATOR")
        }

        val isElevator = elevators
            .isElevator(
                floorIndex = cloneFloor,
                position = clonePos,
            ) || clone in newElevators

        // ждём подъёма на лифте
        if (isElevator) {
            doNothingAndWait()
            continue
        }

        val bestCase = area
            .floors[cloneFloor]
            .cases[clonePos]
            .filter {
                resources.satisfies(it.constraints)
            }
            .minBy {
                it.distance
            }

        if (bestCase.haveToBuildElevator) {
            buildElevator()
            continue
        }

        if (bestCase.direction != direction) {
            blockClone()
            continue
        }

        // клон двигается, как надо
        doNothingAndWait()
    }
}

@Suppress("unused")
private fun debug(message: String) = System.err.println(message)

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
     * Предполагает обязательную постройку лифта
     */
    val haveToBuildElevator: Boolean,
    /**
     * Минимальные условия для использования данного варианта
     */
    val constraints: StateConstraints,
) {
    companion object {
        /**
         * Отбросить варианты с более жёсткими ограничениями,
         * но с теми же или худшими результатами (distance)
         */
        fun List<Case>.optimize(): List<Case> {
            // для начала выкинем лишние кейсы с одинаковыми ограничениями
            // TODO: также выкидывать более жёсткие ограничения с меньшим эффектом
            return this
                .groupBy {
                    it.constraints
                }
                .mapValues { (_, cases) ->
                    cases
                        .minBy {
                            it.distance
                        }
                }
                .values
                .toList()
        }
    }
}

private data class StateConstraints(
    /**
     * Сколько лифтов минимум потребуется построить для достижения выхода
     */
    val elevatorsLeft: Int,
    /**
     * Сколько клонов должно быть в запасе для достижения выхода
     */
    val clonesLeft: Int,
) {
    fun satisfies(
        constraints: StateConstraints,
    ): Boolean {
        val resources = this
        return resources.clonesLeft >= constraints.clonesLeft
                && resources.elevatorsLeft >= constraints.elevatorsLeft
    }
}
