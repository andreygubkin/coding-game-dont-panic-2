import Case.Companion.optimize
import java.util.*

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {

    val input = Scanner(System.`in`)

    @Suppress("UNUSED_VARIABLE")
    val nbFloors = input.nextInt() // number of floors in the area. A clone can move between floor 0 and floor nbFloors - 1

    val width = input.nextInt() // the width of the area. The clone can move without being destroyed between position 0 and position width - 1

    // клон может не добежать 1 шаг, так как раунды кончились
    // так что недостаточно только считать клонов
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

    fun AreaPoint.isElevator(): Boolean {
        return elevators.isElevator(floorIndex = floor, position = position)
    }

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

        override fun toString(): String {
            return "Floor #$floorIndex:\n${
                cases
                    .mapIndexed { index, cases -> index to cases }
                    .joinToString("\n") {
                        "\t${it.first}: ${it.second.joinToString(", ")}" 
                    }
            }"
        }
    }

    class Area(
        val floors: List<Floor>,
    ) {
        override fun toString(): String {
            return "Area:\n${floors.reversed().joinToString("\n")}"
        }

        fun getCasesFor(
            point: AreaPoint,
        ): List<Case> {
            return this
                .floors[point.floor]
                .cases[point.position]
        }
    }

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
                            idea = CaseIdea.EXIT,
                            direction = Direction.LEFT,
                            haveToBuildElevator = false,
                            constraints = StateConstraints(
                                roundsLeft = 0,
                                elevatorsLeft = 0,
                                clonesLeft = 1, // текущий - кто будет спасён
                            ),
                        ),
                        Case(
                            idea = CaseIdea.EXIT,
                            direction = Direction.RIGHT,
                            haveToBuildElevator = false,
                            constraints = StateConstraints(
                                roundsLeft = 0,
                                elevatorsLeft = 0,
                                clonesLeft = 1, // текущий - кто будет спасён
                            ),
                        ),
                    )

                    (exitPos - 1 downTo 0)
                        .takeWhile { position ->
                            !AreaPoint(floor = floorIndex, position = position).isElevator()
                        }
                        .forEach { position ->
                            // так как этаж с выходом, то не нужен запас лифтов
                            addCases(
                                position = position,
                                // если клон бежит влево, а выход справа, то помимо расстояния до выхода
                                // надо ещё и развернуться - заблокировав одного клона из резерва
                                Case(
                                    idea = CaseIdea.REVERSE_AND_RUN_TO_EXIT,
                                    direction = Direction.LEFT,
                                    haveToBuildElevator = false,
                                    constraints = StateConstraints(
                                        roundsLeft = exitPos - position + 1,
                                        elevatorsLeft = 0,
                                        clonesLeft = 2, // текущий - кого заблокируем, и следующий - кто будет спасён
                                    ),
                                ),
                                // если клон бежит вправо и выход справа, то нужно только
                                // пробежать расстояние до выхода без расходования клонов
                                Case(
                                    idea = CaseIdea.JUST_RUN_TO_EXIT,
                                    direction = Direction.RIGHT,
                                    haveToBuildElevator = false,
                                    constraints = StateConstraints(
                                        roundsLeft = exitPos - position,
                                        elevatorsLeft = 0,
                                        clonesLeft = 1, // текущий - кто будет спасён
                                    ),
                                ),
                            )
                        }

                    (exitPos + 1 until width)
                        .takeWhile { position ->
                            !AreaPoint(floor = floorIndex, position = position).isElevator()
                        }
                        .forEach { position ->
                            // так как этаж с выходом, то не нужен запас лифтов
                            addCases(
                                position = position,
                                // если клон бежит вправо, а выход слева, то помимо расстояния до выхода
                                // надо ещё и развернуться - заблокировав одного клона из резерва
                                Case(
                                    idea = CaseIdea.REVERSE_AND_RUN_TO_EXIT,
                                    direction = Direction.RIGHT,
                                    haveToBuildElevator = false,
                                    constraints = StateConstraints(
                                        roundsLeft = position - exitPos + 1,
                                        elevatorsLeft = 0,
                                        clonesLeft = 2, // текущий - кого заблокируем, и следующий - кто будет спасён
                                    ),
                                ),
                                // если клон бежит влево и выход слева, то нужно только
                                // пробежать расстояние до выхода без расходования клонов
                                Case(
                                    idea = CaseIdea.JUST_RUN_TO_EXIT,
                                    direction = Direction.LEFT,
                                    haveToBuildElevator = false,
                                    constraints = StateConstraints(
                                        roundsLeft = position - exitPos,
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
                                            it
                                                .copy(
                                                    // так как лифт уже есть, то добавляется раунд
                                                    // на подъём на лифте, а дополнительные ресурсы не нужны
                                                    constraints = it.constraints.copy(roundsLeft = it.constraints.roundsLeft + 1),
                                                    idea = CaseIdea.USE_EXISTING_ELEVATOR,
                                                )
                                        }
                                        .also {
                                            existingElevatorCasesByPosition[position] = it
                                        }
                                )
                            } else {
                                // вариант 2: построить лифт (уменьшив запасы лифтов и клонов = увеличив требуемые запасы)
                                addCases(
                                    position = position,
                                    newCases = upperFloor
                                        .cases[position]
                                        .map {
                                            it.copy(
                                                haveToBuildElevator = true,
                                                // построить лифт и подняться на нём
                                                constraints = it.constraints.copy(
                                                    elevatorsLeft = it.constraints.elevatorsLeft + 1,
                                                    clonesLeft = it.constraints.clonesLeft + 1,
                                                    roundsLeft = it.constraints.roundsLeft + 2,
                                                ),
                                                idea = CaseIdea.BUILD_NEW_ELEVATOR,
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
                            }.optimize()

                            // здесь нужно взять все варианты ресурсов с предыдущего шага
                            // и добавить +1/+2 к дистанции в зависимости от направления

                            HorizontalRunCase(
                                position = currentPosition,

                                cases = previousCases
                                    .filter {
                                        it.direction == Direction.LEFT
                                    }
                                    .flatMap {
                                        listOf(
                                            // просто бежим налево к предыдущим кейсам
                                            it.copy(
                                                direction = Direction.LEFT,
                                                haveToBuildElevator = false,
                                                idea = CaseIdea.RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                                // движение к предыдущему кейсу
                                                constraints = it.constraints.copy(
                                                    roundsLeft = it.constraints.roundsLeft + 1,
                                                ),
                                            ),
                                            // разворачиваемся и бежим налево к предыдущим кейсам
                                            it.copy(
                                                direction = Direction.RIGHT,
                                                haveToBuildElevator = false,
                                                // блок и движение к предыдущему кейсу
                                                constraints = it.constraints.copy(
                                                    clonesLeft = it.constraints.clonesLeft + 1,
                                                    roundsLeft = it.constraints.roundsLeft + 2,
                                                ),
                                                idea = CaseIdea.REVERSE_AND_RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                            ),
                                        )
                                    },
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
                                    .filter {
                                        it.direction == Direction.RIGHT
                                    }
                                    .flatMap {
                                        listOf(
                                            it.copy(
                                                direction = Direction.RIGHT,
                                                haveToBuildElevator = false,
                                                idea = CaseIdea.RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                                constraints = it.constraints.copy(
                                                    roundsLeft = it.constraints.roundsLeft + 1,
                                                ),
                                            ),
                                            it.copy(
                                                direction = Direction.LEFT,
                                                haveToBuildElevator = false,
                                                constraints = it.constraints.copy(
                                                    clonesLeft = it.constraints.clonesLeft + 1,
                                                    roundsLeft = it.constraints.roundsLeft + 2,
                                                ),
                                                idea = CaseIdea.REVERSE_AND_RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
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

    area.floors.reversed().forEach { debug(it) }

    var resources = StateConstraints(
        clonesLeft = nbTotalClones,
        elevatorsLeft = nbAdditionalElevators,
        roundsLeft = nbRounds,
    )

    fun finishRound(
        command: Command,
    ): Nothing {
        throw FinishRoundException(command)
    }

    fun doNothingAndWait(): Nothing {
        finishRound(Command.DO_NOTHING)
    }

    fun blockClone(): Nothing {
        resources = resources
            .copy(
                clonesLeft = resources.clonesLeft - 1,
            )
        finishRound(Command.BLOCK_CLONE)
    }

    val newElevators = hashSetOf<AreaPoint>()

    // game loop
    while (true) {
        try {
            val clone = AreaPoint(
                floor = input.nextInt(), // floor of the leading clone
                position = input.nextInt(), // position of the leading clone on its floor
            )

            // direction of the leading clone: LEFT or RIGHT (or NONE)
            val direction = Direction.valueOf(input.next())

            fun noClone() = clone.position < 0

            if (noClone()) {
                doNothingAndWait()
            }

            fun buildElevator(): Nothing {
                resources = resources.copy(
                        clonesLeft = resources.clonesLeft - 1,
                        elevatorsLeft = resources.elevatorsLeft - 1,
                    )
                newElevators += clone
                finishRound(Command.BUILD_ELEVATOR)
            }

            val isElevator = clone.isElevator() || clone in newElevators

            // ждём подъёма на лифте
            if (isElevator) {
                doNothingAndWait()
            }

            val bestCase = area
                .getCasesFor(clone)
                .asSequence()
                .filter {
                    resources.satisfies(it.constraints)
                }
                .minBy {
                    it.constraints.roundsLeft
                }

            if (bestCase.haveToBuildElevator) {
                buildElevator()
            }

            if (bestCase.direction == direction) {
                // клон двигается, как надо
                doNothingAndWait()
            } else {
                // клон двигается в другом направлении, надо развернуть
                blockClone()
            }
        } catch(e: FinishRoundException) {
            resources = resources.copy(roundsLeft = resources.roundsLeft - 1)
            println(e.command.message)
        }
    }
}

@Suppress("unused")
private fun debug(message: Any) {
    System.err.println(message.toString())
}

/**
 * Направление движения клона
 */
private enum class Command(
    val message: String,
) {
    DO_NOTHING(message = "WAIT"),
    BUILD_ELEVATOR(message = "ELEVATOR"),
    BLOCK_CLONE(message = "BLOCK"),
}

/**
 * Направление движения клона
 */
private enum class Direction {
    LEFT,
    RIGHT,
    NONE,
}

private enum class CaseIdea(
    val code: String,
) {
    JUST_RUN_TO_EXIT("JRE"),
    REVERSE_AND_RUN_TO_EXIT("RRE"),
    EXIT("E"),
    USE_EXISTING_ELEVATOR("EE"),
    BUILD_NEW_ELEVATOR("BE"),
    RUN_UNTIL_POSSIBILITY_TO_ELEVATE("RTL"),
    REVERSE_AND_RUN_UNTIL_POSSIBILITY_TO_ELEVATE("RRTL"),
}

/**
 * Вариант дальнейшего движения - с указанием расстояния и требуемых ресурсов
 */
private data class Case(
    /**
     * Идея кейса
     */
    val idea: CaseIdea,
    /**
     * Направление клона
     */
    val direction: Direction,
    /**
     * Предполагает обязательную постройку лифта
     */
    val haveToBuildElevator: Boolean,
    /**
     * Минимальные условия для использования данного варианта
     */
    val constraints: StateConstraints,
) {
    override fun toString(): String {
        return "${idea.code}:${if (direction == Direction.LEFT) "L" else "R"}${constraints.roundsLeft}${if (haveToBuildElevator) "^" else ""}e${constraints.elevatorsLeft}c${constraints.clonesLeft}"
    }

    private fun hasLessStrictAnalogueAmong(
        cases: List<Case>,
    ): Boolean {
        return cases
            .any {
                it.constraints.isLessStrictAnalogueOf(constraints)
            }
    }

    companion object {
        /**
         * Отбросить варианты с более жёсткими ограничениями,
         * но с теми же или худшими результатами (distance)
         */
        fun List<Case>.optimize(): List<Case> {
            return this
                .partition {
                    it.direction == Direction.LEFT
                }
                .toList()
                .flatMap {
                    it.optimizeDirection()
                }
        }

        /**
         * Отбросить варианты с более жёсткими ограничениями,
         * но с теми же или худшими результатами (distance) для однонаправленных кейсов
         */
        private fun List<Case>.optimizeDirection(): List<Case> {
            val cases = this

            return cases
                .filter {
                    !it.hasLessStrictAnalogueAmong(cases)
                }
                .groupBy {
                    it.constraints
                }
                .mapValues { (_, cases) ->
                    cases
                        .minBy {
                            it.constraints.roundsLeft
                        }
                }
                .values
                .toList()
        }
    }
}

private class FinishRoundException(
    val command: Command,
): Exception()

private data class StateConstraints(
    /**
     * Сколько лифтов минимум потребуется построить для достижения выхода
     */
    val elevatorsLeft: Int,
    /**
     * Сколько клонов должно быть в запасе для достижения выхода
     */
    val clonesLeft: Int,
    /**
     * Сколько раундов должно быть в запасе для достижения выхода
     */
    val roundsLeft: Int,
) {
    fun satisfies(
        constraints: StateConstraints,
    ): Boolean {
        val resources = this
        return resources.clonesLeft >= constraints.clonesLeft
                && resources.elevatorsLeft >= constraints.elevatorsLeft
                && resources.roundsLeft >= constraints.roundsLeft
    }

    /**
     * Тот же результат при меньших требованиях
     * или лучше результат при не больших требованиях
     */
    fun isLessStrictAnalogueOf(
        constraints: StateConstraints,
    ): Boolean {
        return when {
            roundsLeft == constraints.roundsLeft -> {
                clonesLeft < constraints.clonesLeft && elevatorsLeft <= constraints.elevatorsLeft
                        || clonesLeft <= constraints.clonesLeft && elevatorsLeft < constraints.elevatorsLeft
            }

            roundsLeft < constraints.roundsLeft -> {
                clonesLeft <= constraints.clonesLeft && elevatorsLeft <= constraints.elevatorsLeft
            }

            else -> false
        }
    }
}
