import Case.Companion.optimize
import java.time.Duration
import java.time.Instant
import java.util.*

fun main() {

    startedAt = Instant.now()
    lastMeasuredAt = startedAt

    val input = Scanner(System.`in`)

    val config = GameConfig
        .readFromStdIn(
            input = input,
        )

    debug("config: $config")
    debug("before buildArea")
    val area = Area.buildArea(config = config)
    debug("after buildArea")

    area.floors.reversed().forEach {
        debug(it)
    }

    var resources = StateConstraints(
        clonesLeft = config.totalClonesNumber,
        elevatorsLeft = config.additionalElevatorsNumber,
        roundsLeft = config.roundsNumber,
    )

    val newElevators = hashSetOf<AreaPoint>()
    val path = mutableListOf<Case>()

    fun finishRound(
        command: Command,
    ): Nothing {
        throw FinishRoundException(command)
    }

    fun doNothingAndWait(): Nothing {
        finishRound(Command.DO_NOTHING)
    }

    fun blockClone(): Nothing {
        path
            .forEach { case ->
                case.decrementRequiredClones()
            }

        resources = resources
            .copy(
                clonesLeft = resources.clonesLeft - 1,
            )
        finishRound(Command.BLOCK_CLONE)
    }

    fun buildElevator(
        elevator: AreaPoint,
    ): Nothing {

        path
            .forEach { case ->
                case.decrementRequiredElevators()
            }

        resources = resources.copy(
            clonesLeft = resources.clonesLeft - 1,
            elevatorsLeft = resources.elevatorsLeft - 1,
        )
        newElevators += elevator
        finishRound(Command.BUILD_ELEVATOR)
    }

    debug("loop start")

    // game loop
    while (true) {
        try {
            val clone = AreaPoint(
                floor = input.nextInt(), // floor of the leading clone
                position = input.nextInt(), // position of the leading clone on its floor
            )

            // direction of the leading clone: LEFT or RIGHT (or NONE)
            val direction = Direction.valueOf(input.next())

            debug("clone=$clone, $direction")
            debug("resources=$resources")

            fun noClone() = clone.position < 0

            if (noClone()) {
                doNothingAndWait()
            }

            val isElevator = area.isElevator(clone) || clone in newElevators

            // ждём подъёма на лифте
            if (isElevator) {
                doNothingAndWait()
            }

            val bestCase = area
                .getCasesFor(clone)
                .asSequence()
                .filter { case ->
                    resources.satisfies(case.constraints).also { satisfies ->
                        debug("$case ${if (satisfies) "+" else "-"}")
                    }
                }
                .minByOrNull {
                    it.constraints.roundsLeft
                }

            if (bestCase == null) {
                error("no suitable case")
            }

            debug("bestCase: $bestCase")

            path += bestCase

            if (bestCase.action.buildElevator) {
                buildElevator(
                    elevator = clone,
                )
            }

            if (bestCase.action.direction != direction) {
                // клон двигается в другом направлении, надо развернуть
                blockClone()
            }

            // клон двигается, как надо
            doNothingAndWait()
        } catch (e: FinishRoundException) {
            resources = resources.copy(roundsLeft = resources.roundsLeft - 1)
            debug("round end ${e.command}")
            println(e.command.message)
        } catch (e: Throwable) {
            debug("exception ${e.stackTraceToString()}")
            throw e
        }
    }
}

private lateinit var startedAt: Instant
private lateinit var lastMeasuredAt: Instant

@Suppress("unused")
private fun debug(message: Any) {
    val now = Instant.now()
    val elapsedTimeMs = Duration.between(startedAt, now).toMillis()
    val elapsedTimeSinceLastMeasureMs = Duration.between(lastMeasuredAt, now).toMillis()
    lastMeasuredAt = now
    System.err.println("$elapsedTimeMs(+$elapsedTimeSinceLastMeasureMs) ms: $message")
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
    JUST_RUN_TO_EXIT("JREx"),
    REVERSE_AND_RUN_TO_EXIT("RREx"),
    EXIT("Ex"),
    USE_EXISTING_ELEVATOR("El"),
    BUILD_NEW_ELEVATOR("BEl"),
    RUN_UNTIL_POSSIBILITY_TO_ELEVATE("RTEl"),
    REVERSE_AND_RUN_UNTIL_POSSIBILITY_TO_ELEVATE("RRTEl"),
}

private data class CaseAction(
    /**
     * Направление клона
     */
    val direction: Direction,
    /**
     * Предполагает обязательную постройку лифта
     */
    val buildElevator: Boolean,
)

/**
 * Вариант дальнейшего движения - с указанием расстояния и требуемых ресурсов
 */
private data class Case(
    /**
     * Идея кейса
     */
    val idea: CaseIdea,
    /**
     * Указание к действию
     */
    val action: CaseAction,
    /**
     * Минимальные условия для использования данного варианта
     */
    var constraints: StateConstraints,
) {
    override fun toString(): String {
        return "${idea.code}:${if (action.direction == Direction.LEFT) "L" else "R"}${constraints.roundsLeft}${if (action.buildElevator) "^" else ""}e${constraints.elevatorsLeft}c${constraints.clonesLeft}"
    }

    private fun hasLessStrictAnalogueAmong(
        cases: List<Case>,
    ): Boolean {
        return cases
            .any {
                it.constraints.isLessStrictAnalogueOf(constraints)
            }
    }

    fun decrementRequiredClones() {
        constraints = constraints
            .copy(
                clonesLeft = constraints.clonesLeft - 1,
            )
    }

    fun decrementRequiredElevators() {
        constraints = constraints
            .copy(
                elevatorsLeft = constraints.elevatorsLeft - 1,
            )
    }

    companion object {
        /**
         * Отбросить варианты с более жёсткими ограничениями,
         * но с теми же или худшими результатами (distance)
         */
        fun List<Case>.optimize(
            canOmit: Boolean,
        ): List<Case> {
            if (canOmit) {
                return this
            }

            return this
                .groupBy {
                    it.action.direction
                }
                .flatMap {
                    it.value.optimizeDirection()
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
) : Exception()

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

private data class GameConfig(
    /**
     * number of floors in the area. A clone can move between floor 0 and floor floorsNumber - 1
     */
    val floorsNumber: Int,
    /**
     * the width of the area. The clone can move without being destroyed between position 0 and position width - 1
     */
    val width: Int,
    /**
     * maximum number of rounds before the end of the game
     * клон может не добежать 1 шаг, так как раунды кончились
     * так что недостаточно только считать клонов
     */
    val roundsNumber: Int,
    /**
     * the floor on which the exit is located
     */
    val exitFloor: Int,
    /**
     * the position of the exit on its floor
     */
    val exitPosition: Int,
    /**
     * the number of clones that will come out of the generator during the game
     */
    val totalClonesNumber: Int,
    /**
     * number of additional elevators that you can build
     */
    val additionalElevatorsNumber: Int,
    /**
     * elevators in the area
     */
    val elevators: Elevators,
    /**
     * период выпуска новых клонов
     */
    val clonesEmissionPeriod: Int,
) {
    /**
     * На это время увеличивается число необходимых раундов при расходовании
     * клона (блокировке или постройке лифта).
     *
     * Почему такая формула?
     * Пусть clonesEmissionPeriod = 1, т. е. клоны бегут друг за другом.
     * При расходовании клона, следующий клон в этом же раунде попадает на место расходования.
     * А значит, снова находится на 1 позицию позади места, где был бы первый клон, если бы его не израсходовали.
     */
    val cloneCostInRounds = clonesEmissionPeriod

    /**
     * Количество этажей, с которыми имеет смысл работать.
     * Более верхние этажи можно отбросить, так как с них невозможно добраться до выхода
     * и клонов на эти этажи пускать не будем
     */
    val workFloorsNumber = exitFloor + 1

    companion object {
        fun readFromStdIn(
            input: Scanner,
        ): GameConfig {
            fun readElevators(
                floorsNumber: Int,
            ): Elevators {

                return Elevators(
                    floorsNumber = floorsNumber,
                )
                    .apply {
                        // number of elevators in the area
                        val elevatorsNumber = input.nextInt()
                        for (i in 0 until elevatorsNumber) {
                            val elevatorFloor = input.nextInt() // floor on which this elevator is found
                            val elevatorPos = input.nextInt() // position of the elevator on its floor

                            registerElevator(
                                elevator = AreaPoint(
                                    floor = elevatorFloor,
                                    position = elevatorPos,
                                ),
                            )
                        }
                    }
            }

            val floorsNumber = input.nextInt()
            return GameConfig(
                floorsNumber = floorsNumber,
                width = input.nextInt(),
                roundsNumber = input.nextInt(),
                exitFloor = input.nextInt(),
                exitPosition = input.nextInt(),
                totalClonesNumber = input.nextInt(),
                additionalElevatorsNumber = input.nextInt(),
                elevators = readElevators(floorsNumber),
                clonesEmissionPeriod = CLONES_EMISSION_PERIOD,
            )
        }

        private const val CLONES_EMISSION_PERIOD = 3
    }
}

private class Elevators(
    floorsNumber: Int,
) {
    private val elevators: List<HashSet<Int>> = List(floorsNumber) {
        hashSetOf()
    }

    fun registerElevator(
        elevator: AreaPoint,
    ) {
        elevators[elevator.floor] += elevator.position
    }

    fun isElevator(
        floorIndex: Int,
        position: Int,
    ): Boolean {
        return position in elevators[floorIndex]
    }

    override fun toString(): String {
        return elevators
            .withIndex()
            .filter {
                it.value.isNotEmpty()
            }
            .joinToString(separator = "; ") {
                "${it.index}:${it.value.sorted()}"
            }
    }
}

private data class AreaPoint(
    val floor: Int,
    val position: Int,
)

private class Floor(
    val floorIndex: Int,
    val width: Int,
) {
    private val floorCases: List<MutableList<Case>> = List(size = width) {
        mutableListOf()
    }

    val cases: List<List<Case>> get() = floorCases

    fun optimizeCases() {
        floorCases
            .forEach { positionCases ->
                val optimizedPositionCases = positionCases.optimize(canOmit = false)
                positionCases.clear()
                positionCases.addAll(optimizedPositionCases)
            }
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

private class Area(
    val floors: List<Floor>,
    val elevators: Elevators,
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

    fun isElevator(
        point: AreaPoint,
    ): Boolean {
        return elevators
            .isElevator(
                floorIndex = point.floor,
                position = point.position,
            )
    }

    companion object {
        /**
         * Варианты движения для каждого этажа и каждой ячейки на этаже
         */
        fun buildArea(
            config: GameConfig,
        ): Area {

            val elevators = config.elevators

            fun AreaPoint.isElevator(): Boolean {
                return elevators.isElevator(floorIndex = floor, position = position)
            }

            fun buildExitFloor(): Floor {
                return Floor(floorIndex = config.exitFloor, width = config.width)
                    .apply {
                        addCases(
                            position = config.exitPosition,
                            newCases = listOf(
                                Case(
                                    idea = CaseIdea.EXIT,
                                    action = CaseAction(
                                        direction = Direction.LEFT,
                                        buildElevator = false,
                                    ),
                                    constraints = StateConstraints(
                                        roundsLeft = 0,
                                        elevatorsLeft = 0,
                                        clonesLeft = 1, // текущий - кто будет спасён
                                    ),
                                ),
                                Case(
                                    idea = CaseIdea.EXIT,
                                    action = CaseAction(
                                        direction = Direction.RIGHT,
                                        buildElevator = false,
                                    ),
                                    constraints = StateConstraints(
                                        roundsLeft = 0,
                                        elevatorsLeft = 0,
                                        clonesLeft = 1, // текущий - кто будет спасён
                                    ),
                                ),
                            ),
                        )

                        (config.exitPosition - 1 downTo 0)
                            .takeWhile { position ->
                                !AreaPoint(floor = floorIndex, position = position).isElevator()
                            }
                            .forEach { position ->
                                // так как этаж с выходом, то не нужен запас лифтов
                                addCases(
                                    position = position,
                                    // если клон бежит влево, а выход справа, то помимо расстояния до выхода
                                    // надо ещё и развернуться - заблокировав одного клона из резерва
                                    newCases = listOf(
                                        Case(
                                            idea = CaseIdea.REVERSE_AND_RUN_TO_EXIT,
                                            action = CaseAction(
                                                direction = Direction.LEFT,
                                                buildElevator = false,
                                            ),
                                            constraints = StateConstraints(
                                                roundsLeft = config.exitPosition - position + config.cloneCostInRounds,
                                                elevatorsLeft = 0,
                                                clonesLeft = 2, // текущий - кого заблокируем, и следующий - кто будет спасён
                                            ),
                                        ),
                                        // если клон бежит вправо и выход справа, то нужно только
                                        // пробежать расстояние до выхода без расходования клонов
                                        Case(
                                            idea = CaseIdea.JUST_RUN_TO_EXIT,
                                            action = CaseAction(
                                                direction = Direction.RIGHT,
                                                buildElevator = false,
                                            ),
                                            constraints = StateConstraints(
                                                roundsLeft = config.exitPosition - position,
                                                elevatorsLeft = 0,
                                                clonesLeft = 1, // текущий - кто будет спасён
                                            ),
                                        ),
                                    ),
                                )
                            }

                        (config.exitPosition + 1 until width)
                            .takeWhile { position ->
                                !AreaPoint(floor = floorIndex, position = position).isElevator()
                            }
                            .forEach { position ->
                                // так как этаж с выходом, то не нужен запас лифтов
                                addCases(
                                    position = position,
                                    // если клон бежит вправо, а выход слева, то помимо расстояния до выхода
                                    // надо ещё и развернуться - заблокировав одного клона из резерва
                                    newCases = listOf(
                                        Case(
                                            idea = CaseIdea.REVERSE_AND_RUN_TO_EXIT,
                                            action = CaseAction(
                                                direction = Direction.RIGHT,
                                                buildElevator = false,
                                            ),
                                            constraints = StateConstraints(
                                                roundsLeft = position - config.exitPosition + config.cloneCostInRounds,
                                                elevatorsLeft = 0,
                                                clonesLeft = 2, // текущий - кого заблокируем, и следующий - кто будет спасён
                                            ),
                                        ),
                                        // если клон бежит влево и выход слева, то нужно только
                                        // пробежать расстояние до выхода без расходования клонов
                                        Case(
                                            idea = CaseIdea.JUST_RUN_TO_EXIT,
                                            action = CaseAction(
                                                direction = Direction.LEFT,
                                                buildElevator = false,
                                            ),
                                            constraints = StateConstraints(
                                                roundsLeft = position - config.exitPosition,
                                                elevatorsLeft = 0,
                                                clonesLeft = 1, // текущий - кто будет спасён
                                            ),
                                        ),
                                    ),
                                )
                            }
                    }
            }

            val exitFloorCases = buildExitFloor()
            debug("exit floor ready")

            return Area(
                elevators = elevators,
                floors = generateSequence(
                    seed = exitFloorCases,
                ) { upperFloor ->
                    Floor(
                        floorIndex = upperFloor.floorIndex - 1,
                        width = upperFloor.width,
                    )
                        .apply {

                            val existingElevatorCasesByPosition = mutableMapOf<Int, List<Case>>()
                            val newElevatorCasesByPosition = mutableMapOf<Int, List<Case>>()

                            (0 until width)
                                .forEach { position ->
                                    val isElevator = config
                                        .elevators
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
                                                            action = it.action.copy(
                                                                buildElevator = false,
                                                            ),
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
                                                .map { upperFloorCase ->
                                                    upperFloorCase.copy(
                                                        action = upperFloorCase.action.copy(
                                                            buildElevator = true,
                                                        ),
                                                        // построить лифт и подняться на нём
                                                        constraints = upperFloorCase.constraints.copy(
                                                            elevatorsLeft = upperFloorCase.constraints.elevatorsLeft + 1,
                                                            clonesLeft = upperFloorCase.constraints.clonesLeft + 1,
                                                            roundsLeft = upperFloorCase.constraints.roundsLeft + 1 + config.cloneCostInRounds,
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
                                    isElevator = config
                                        .elevators
                                        .isElevator(
                                            floorIndex = floorIndex,
                                            position = leftMostPosition,
                                        ),
                                )
                            ) { previousCase ->

                                val currentPosition = previousCase.position + 1

                                val isElevator = config
                                    .elevators
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
                                    }.optimize(canOmit = true)

                                    // здесь нужно взять все варианты ресурсов с предыдущего шага
                                    // и добавить +1/+2 к дистанции в зависимости от направления

                                    HorizontalRunCase(
                                        position = currentPosition,

                                        cases = previousCases
                                            .filter {
                                                it.action.direction == Direction.LEFT
                                            }
                                            .flatMap {
                                                listOf(
                                                    // просто бежим налево к предыдущим кейсам
                                                    it.copy(
                                                        action = CaseAction(
                                                            direction = Direction.LEFT,
                                                            buildElevator = false,
                                                        ),
                                                        idea = CaseIdea.RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                                        // движение к предыдущему кейсу
                                                        constraints = it.constraints.copy(
                                                            roundsLeft = it.constraints.roundsLeft + 1,
                                                        ),
                                                    ),
                                                    // разворачиваемся и бежим налево к предыдущим кейсам
                                                    it.copy(
                                                        action = CaseAction(
                                                            direction = Direction.RIGHT,
                                                            buildElevator = false,
                                                        ),
                                                        // блок и движение к предыдущему кейсу
                                                        constraints = it.constraints.copy(
                                                            clonesLeft = it.constraints.clonesLeft + 1,
                                                            roundsLeft = it.constraints.roundsLeft + config.cloneCostInRounds,
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
                                    isElevator = config
                                        .elevators
                                        .isElevator(
                                            floorIndex = floorIndex,
                                            position = rightMostPosition,
                                        ),
                                )
                            ) { previousCase ->

                                val currentPosition = previousCase.position - 1

                                val isElevator = config
                                    .elevators
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
                                                it.action.direction == Direction.RIGHT
                                            }
                                            .flatMap {
                                                listOf(
                                                    it.copy(
                                                        action = CaseAction(
                                                            direction = Direction.RIGHT,
                                                            buildElevator = false,
                                                        ),
                                                        idea = CaseIdea.RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                                        constraints = it.constraints.copy(
                                                            roundsLeft = it.constraints.roundsLeft + 1,
                                                        ),
                                                    ),
                                                    it.copy(
                                                        action = CaseAction(
                                                            direction = Direction.LEFT,
                                                            buildElevator = false,
                                                        ),
                                                        constraints = it.constraints.copy(
                                                            clonesLeft = it.constraints.clonesLeft + 1,
                                                            roundsLeft = it.constraints.roundsLeft + config.cloneCostInRounds,
                                                        ),
                                                        idea = CaseIdea.REVERSE_AND_RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                                    ),
                                                )
                                            }
                                            .optimize(canOmit = true),
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
                        .also {
                            debug("floor ${it.floorIndex} ready")
                        }
                }
                    .take(config.workFloorsNumber)
                    .toList()
                    .reversed()
            )
        }
    }
}
