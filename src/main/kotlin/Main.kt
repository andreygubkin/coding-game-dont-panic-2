import Case.Companion.optimize
import java.util.*

fun main() {

    val input = Scanner(System.`in`)

    val config = GameConfig
        .readFromStdIn(
            input = input,
        )

    var resources = config.getInitialResource()

    val area = Area
        .buildArea(
            config = config,
        )

    val path = hashSetOf<AreaPoint>()

    fun finishRound(
        command: Command,
    ): Nothing {
        resources = resources.copy(
            roundsLeft = resources.roundsLeft - 1,
        )

        throw FinishRoundException(command)
    }

    fun waitForNewClone(): Nothing {
        finishRound(Command.WAIT_FOR_NEW_CLONE)
    }

    fun keepGoing(): Nothing {
        finishRound(Command.KEEP_GOING)
    }

    fun useExit(): Nothing {
        finishRound(Command.USE_EXIT)
    }

    fun useElevator(): Nothing {
        finishRound(Command.USE_ELEVATOR)
    }

    fun blockClone(): Nothing {
        resources = resources.copy(
            clonesLeft = resources.clonesLeft - 1,
            // trade-off: расходуем клона ради ускорения по раундам
            roundsLeft = resources.roundsLeft + 1,
        )

        finishRound(Command.BLOCK_CLONE)
    }

    fun buildElevator(
        point: AreaPoint,
    ): Nothing {
        resources = resources.copy(
            clonesLeft = resources.clonesLeft - 1,
            elevatorsLeft = resources.elevatorsLeft - 1,
            // trade-off: расходуем клона ради ускорения по раундам
            roundsLeft = resources.roundsLeft + 1,
        )

        area
            .elevators
            .registerElevator(
                elevator = point,
            )

        finishRound(Command.BUILD_ELEVATOR)
    }

    while (true) {
        try {
            val clone = AreaPoint(
                floor = input.nextInt(), // floor of the leading clone
                position = input.nextInt(), // position of the leading clone on its floor
            )

            val direction = Direction.valueOf(input.next())

            fun noClone() = clone.position < 0

            if (noClone()) {
                waitForNewClone()
            }

            if (clone in path) {
                keepGoing()
            }

            path += clone

            // выходим
            if (clone == area.exit) {
                useExit()
            }

            val isElevator = area.elevators.isElevator(clone)

            // ждём подъёма на лифте
            if (isElevator) {
                useElevator()
            }

            val bestCase = area
                .getCasesFor(
                    point = clone,
                    direction = direction,
                )
                .filter { case ->
                    resources
                        .isEnoughFor(
                            constraints = case.constraints,
                        )
                }
                .minBy {
                    it.constraints.roundsLeft
                }

            when (bestCase.action) {
                CloneAction.KEEP_GOING -> {
                    keepGoing()
                }

                CloneAction.BUILD_ELEVATOR -> {
                    buildElevator(
                        point = clone,
                    )
                }

                CloneAction.BLOCK_CLONE -> {
                    blockClone()
                }
            }
        } catch (e: FinishRoundException) {
            println(e.command.message)
        }
    }
}

/**
 * Команда, завершающая раунд
 */
private enum class Command(
    val message: String,
) {
    USE_ELEVATOR(message = "WAIT"),
    USE_EXIT(message = "WAIT"),
    KEEP_GOING(message = "WAIT"),
    WAIT_FOR_NEW_CLONE(message = "WAIT"),
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

/**
 * Действие клона на раунде
 */
private enum class CloneAction {
    /**
     * Не расходовать клона - клон двигается в нужном направлении
     */
    KEEP_GOING,

    /**
     * Предполагает обязательную постройку лифта
     */
    BUILD_ELEVATOR,

    /**
     * Предполагает обязательную блокировку клона
     */
    BLOCK_CLONE,
}

/**
 * Вариант дальнейшего движения - с указанием расстояния до выхода и требуемых ресурсов
 */
private data class Case(
    /**
     * Позиция в зоне, для которой вычислен данный кейс
     */
    val point: AreaPoint,
    /**
     * Для какого текущего направления клона этот кейс
     */
    val direction: Direction,
    /**
     * Кейс, к которому бежим
     */
    val targetCase: Case? = null,
    /**
     * Указание к действию
     */
    val action: CloneAction,
    /**
     * Минимальные условия для использования данного варианта
     */
    var constraints: StateConstraints,
) {
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
                .groupBy {
                    it.direction
                }
                .flatMap { entry ->
                    entry
                        .value
                        .optimizeDirection()
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
    fun isEnoughFor(
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
     * Number of floors in the area. A clone can move between floor 0 and floor floorsNumber - 1
     */
    val floorsNumber: Int,
    /**
     * The width of the area. The clone can move without being destroyed between position 0 and position width - 1
     */
    val width: Int,
    /**
     * Maximum number of rounds before the end of the game
     */
    val roundsNumber: Int,
    /**
     * The floor on which the exit is located
     */
    val exitFloor: Int,
    /**
     * The position of the exit on its floor
     */
    val exitPosition: Int,
    /**
     * The number of clones that will come out of the generator during the game
     */
    val totalClonesNumber: Int,
    /**
     * Number of additional elevators that you can build
     */
    val additionalElevatorsNumber: Int,
    /**
     * Elevators in the area
     */
    val elevators: Elevators,
    /**
     * Период выпуска новых клонов
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

    fun getInitialResource(): StateConstraints {
        return StateConstraints(
            clonesLeft = totalClonesNumber,
            elevatorsLeft = additionalElevatorsNumber,
            roundsLeft = roundsNumber,
        )
    }

    companion object {
        fun readFromStdIn(
            input: Scanner,
        ): GameConfig {
            val floorsNumber = input.nextInt()
            return GameConfig(
                floorsNumber = floorsNumber,
                width = input.nextInt(),
                roundsNumber = input.nextInt(),
                exitFloor = input.nextInt(),
                exitPosition = input.nextInt(),
                totalClonesNumber = input.nextInt(),
                additionalElevatorsNumber = input.nextInt(),
                elevators = Elevators.readFromStdIn(input),
                clonesEmissionPeriod = CLONES_EMISSION_PERIOD,
            )
        }

        /**
         * Периодичность выпуска новых клонов генератором
         */
        private const val CLONES_EMISSION_PERIOD = 3
    }
}

private class Elevators {
    private val elevators = HashSet<AreaPoint>()

    fun registerElevator(
        elevator: AreaPoint,
    ) {
        elevators += elevator
    }

    fun isElevator(
        point: AreaPoint,
    ): Boolean {
        return point in elevators
    }

    companion object {
        fun readFromStdIn(
            input: Scanner,
        ): Elevators {
            return Elevators()
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
    fun floorPointAt(
        position: Int,
    ): AreaPoint {
        return AreaPoint(
            floor = floorIndex,
            position = position,
        )
    }

    private var floorCases: List<MutableList<Case>> = List(size = width) {
        mutableListOf()
    }

    val cases: List<List<Case>> get() = floorCases

    fun optimizeCases() {
        floorCases = floorCases
            .map { positionCases ->
                positionCases
                    .optimize()
                    .toMutableList()
            }
    }

    fun addCases(
        position: Int,
        newCases: List<Case>,
    ) {
        floorCases[position] += newCases
    }
}

private data class Area(
    val exit: AreaPoint,
    val elevators: Elevators,
    val floors: List<Floor>,
) {
    fun getCasesFor(
        point: AreaPoint,
        direction: Direction,
    ): List<Case> {
        return this
            .floors[point.floor]
            .cases[point.position]
            .filter { case ->
                case.direction == direction
            }
    }

    companion object {
        fun buildArea(
            config: GameConfig,
        ): Area {

            val elevators = config.elevators

            fun buildExitFloor(): Floor {
                return Floor(
                    floorIndex = config.exitFloor,
                    width = config.width,
                )
                    .apply {

                        val roundsToExit = 1

                        addCases(
                            position = config.exitPosition,
                            newCases = listOf(
                                Case(
                                    point = floorPointAt(config.exitPosition),
                                    //idea = CaseIdea.EXIT,
                                    direction = Direction.LEFT,
                                    action = CloneAction.KEEP_GOING,
                                    constraints = StateConstraints(
                                        roundsLeft = roundsToExit,
                                        elevatorsLeft = 0,
                                        clonesLeft = 1, // текущий - кто будет спасён
                                    ),
                                ),
                                Case(
                                    point = floorPointAt(config.exitPosition),
                                    //idea = CaseIdea.EXIT,
                                    direction = Direction.RIGHT,
                                    action = CloneAction.KEEP_GOING,
                                    constraints = StateConstraints(
                                        roundsLeft = roundsToExit,
                                        elevatorsLeft = 0,
                                        clonesLeft = 1, // текущий - кто будет спасён
                                    ),
                                ),
                            ),
                        )

                        (config.exitPosition - 1 downTo 0)
                            .takeWhile { position ->
                                !elevators.isElevator(floorPointAt(position))
                            }
                            .forEach { position ->
                                // так как этаж с выходом, то не нужен запас лифтов
                                addCases(
                                    position = position,
                                    // если клон бежит влево, а выход справа, то помимо расстояния до выхода
                                    // надо ещё и развернуться - заблокировав одного клона из резерва
                                    newCases = listOf(
                                        Case(
                                            point = floorPointAt(position),
                                            //idea = CaseIdea.REVERSE_AND_RUN_TO_EXIT,
                                            direction = Direction.LEFT,
                                            action = CloneAction.BLOCK_CLONE,
                                            constraints = StateConstraints(
                                                roundsLeft = config.exitPosition - position + config.cloneCostInRounds + roundsToExit,
                                                elevatorsLeft = 0,
                                                clonesLeft = 2, // текущий - кого заблокируем, и следующий - кто будет спасён
                                            ),
                                        ),
                                        // если клон бежит вправо и выход справа, то нужно только
                                        // пробежать расстояние до выхода без расходования клонов
                                        Case(
                                            point = floorPointAt(position),
                                            //idea = CaseIdea.JUST_RUN_TO_EXIT,
                                            direction = Direction.RIGHT,
                                            action = CloneAction.KEEP_GOING,
                                            constraints = StateConstraints(
                                                roundsLeft = config.exitPosition - position + roundsToExit,
                                                elevatorsLeft = 0,
                                                clonesLeft = 1, // текущий - кто будет спасён
                                            ),
                                        ),
                                    ),
                                )
                            }

                        (config.exitPosition + 1 until width)
                            .takeWhile { position ->
                                !elevators.isElevator(floorPointAt(position))
                            }
                            .forEach { position ->
                                // так как этаж с выходом, то не нужен запас лифтов
                                addCases(
                                    position = position,
                                    // если клон бежит вправо, а выход слева, то помимо расстояния до выхода
                                    // надо ещё и развернуться - заблокировав одного клона из резерва
                                    newCases = listOf(
                                        Case(
                                            point = floorPointAt(position),
                                            //idea = CaseIdea.REVERSE_AND_RUN_TO_EXIT,
                                            direction = Direction.RIGHT,
                                            action = CloneAction.BLOCK_CLONE,
                                            constraints = StateConstraints(
                                                roundsLeft = position - config.exitPosition + config.cloneCostInRounds,
                                                elevatorsLeft = 0,
                                                clonesLeft = 2, // текущий - кого заблокируем, и следующий - кто будет спасён
                                            ),
                                        ),
                                        // если клон бежит влево и выход слева, то нужно только
                                        // пробежать расстояние до выхода без расходования клонов
                                        Case(
                                            point = floorPointAt(position),
                                            //idea = CaseIdea.JUST_RUN_TO_EXIT,
                                            direction = Direction.LEFT,
                                            action = CloneAction.KEEP_GOING,
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

            return Area(
                exit = AreaPoint(
                    floor = config.exitFloor,
                    position = config.exitPosition,
                ),
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
                                            point = AreaPoint(
                                                floor = floorIndex,
                                                position = position,
                                            ),
                                        )

                                    if (isElevator) {
                                        // вариант 1: подняться на существующем лифте
                                        addCases(
                                            position = position,
                                            newCases = upperFloor
                                                .cases[position]
                                                .map { upperFloorCase ->
                                                    upperFloorCase.copy(
                                                        point = floorPointAt(position),
                                                        // так как лифт уже есть, то добавляется раунд
                                                        // на подъём на лифте, а дополнительные ресурсы не нужны
                                                        constraints = upperFloorCase.constraints.copy(
                                                            roundsLeft = upperFloorCase.constraints.roundsLeft + 1,
                                                        ),
                                                        action = CloneAction.KEEP_GOING,
                                                        //idea = CaseIdea.USE_EXISTING_ELEVATOR,
                                                        targetCase = upperFloorCase,
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
                                                        point = floorPointAt(position),
                                                        action = CloneAction.BUILD_ELEVATOR,
                                                        // построить лифт и подняться на нём
                                                        constraints = upperFloorCase.constraints.copy(
                                                            elevatorsLeft = upperFloorCase.constraints.elevatorsLeft + 1,
                                                            clonesLeft = upperFloorCase.constraints.clonesLeft + 1,
                                                            roundsLeft = upperFloorCase.constraints.roundsLeft + 1 + config.cloneCostInRounds,
                                                        ),
                                                        //idea = CaseIdea.BUILD_NEW_ELEVATOR,
                                                        targetCase = upperFloorCase,
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
                                            point = AreaPoint(
                                                floor = floorIndex,
                                                position = leftMostPosition,
                                            ),
                                        ),
                                )
                            ) { previousCase ->

                                val currentPosition = previousCase.position + 1

                                val isElevator = config
                                    .elevators
                                    .isElevator(
                                        point = AreaPoint(
                                            floor = floorIndex,
                                            position = currentPosition,
                                        ),
                                    )

                                if (isElevator) {
                                    HorizontalRunCase(
                                        position = currentPosition,
                                        cases = emptyList(), // из лифта налево не убежишь - никуда не убежишь
                                        isElevator = true,
                                    )
                                } else {
                                    val casesInLeftNeighbourPosition = if (previousCase.isElevator) {
                                        // ранее для всех лифтов заполнили existingElevatorCasesByPosition (вариант 1)
                                        existingElevatorCasesByPosition[previousCase.position]!!
                                    } else {
                                        // предыдущая позиция - не лифт и текущая - не лифт
                                        // на предыдущей позиции могли быть кейсы на построение нового лифта
                                        // или бег влево к существующему лифту
                                        // и надо понять, стоит ли в текущей позиции бежать влево ради этих кейсов

                                        newElevatorCasesByPosition[previousCase.position]!!
                                    } + previousCase.cases

                                    // здесь нужно взять все варианты требуемых ресурсов с предыдущего шага
                                    // и добавить раунды в зависимости от направления

                                    HorizontalRunCase(
                                        position = currentPosition,

                                        cases = casesInLeftNeighbourPosition
                                            .filter {
                                                it.direction == Direction.LEFT
                                            }
                                            .flatMap { caseFromLeftNeighbourPosition ->
                                                listOf(
                                                    // просто бежим налево к предыдущим кейсам
                                                    caseFromLeftNeighbourPosition.copy(
                                                        point = floorPointAt(currentPosition),
                                                        direction = Direction.LEFT,
                                                        action = CloneAction.KEEP_GOING,
                                                        // движение к предыдущему кейсу
                                                        constraints = caseFromLeftNeighbourPosition.constraints.copy(
                                                            roundsLeft = caseFromLeftNeighbourPosition.constraints.roundsLeft + 1,
                                                        ),
                                                        //idea = CaseIdea.RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                                        targetCase = caseFromLeftNeighbourPosition,
                                                    ),
                                                    // разворачиваемся и бежим налево к предыдущим кейсам

                                                    caseFromLeftNeighbourPosition.copy(
                                                        point = floorPointAt(currentPosition),
                                                        direction = Direction.RIGHT,
                                                        action = CloneAction.BLOCK_CLONE,
                                                        // блок и движение к предыдущему кейсу
                                                        constraints = caseFromLeftNeighbourPosition.constraints.copy(
                                                            clonesLeft = caseFromLeftNeighbourPosition.constraints.clonesLeft + 1,
                                                            roundsLeft = caseFromLeftNeighbourPosition.constraints.roundsLeft + 1 + config.cloneCostInRounds,
                                                        ),
                                                        //idea = CaseIdea.REVERSE_AND_RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                                        targetCase = caseFromLeftNeighbourPosition,
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
                                            point = AreaPoint(
                                                floor = floorIndex,
                                                position = rightMostPosition,
                                            ),
                                        ),
                                )
                            ) { previousCase ->

                                val currentPosition = previousCase.position - 1

                                val isElevator = config
                                    .elevators
                                    .isElevator(
                                        point = AreaPoint(
                                            floor = floorIndex,
                                            position = currentPosition,
                                        ),
                                    )

                                if (isElevator) {
                                    HorizontalRunCase(
                                        position = currentPosition,
                                        cases = emptyList(), // из лифта направо не убежишь - никуда не убежишь
                                        isElevator = true,
                                    )
                                } else {
                                    val casesInRightNeighbourPosition = if (previousCase.isElevator) {
                                        // ранее для всех лифтов заполнили existingElevatorCasesByPosition (вариант 1)
                                        existingElevatorCasesByPosition[previousCase.position]!!
                                    } else {
                                        // предыдущая позиция - не лифт и текущая - не лифт
                                        // на предыдущей позиции могли быть кейсы на построение нового лифта
                                        // или бег вправо к существующему лифту
                                        // и надо понять, стоит ли в текущей позиции бежать вправо ради этих кейсов

                                        newElevatorCasesByPosition[previousCase.position]!!
                                    } + previousCase.cases

                                    HorizontalRunCase(
                                        position = currentPosition,
                                        cases = casesInRightNeighbourPosition
                                            .filter {
                                                it.direction == Direction.RIGHT
                                            }
                                            .flatMap { caseFromRightNeighbourPosition ->
                                                listOf(
                                                    caseFromRightNeighbourPosition.copy(
                                                        point = floorPointAt(currentPosition),
                                                        direction = Direction.RIGHT,
                                                        action = CloneAction.KEEP_GOING,
                                                        constraints = caseFromRightNeighbourPosition.constraints.copy(
                                                            roundsLeft = caseFromRightNeighbourPosition.constraints.roundsLeft + 1,
                                                        ),
                                                        //idea = CaseIdea.RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                                        targetCase = caseFromRightNeighbourPosition,
                                                    ),
                                                    caseFromRightNeighbourPosition.copy(
                                                        point = floorPointAt(currentPosition),
                                                        direction = Direction.LEFT,
                                                        action = CloneAction.BLOCK_CLONE,
                                                        constraints = caseFromRightNeighbourPosition.constraints.copy(
                                                            clonesLeft = caseFromRightNeighbourPosition.constraints.clonesLeft + 1,
                                                            roundsLeft = caseFromRightNeighbourPosition.constraints.roundsLeft + 1 + config.cloneCostInRounds,
                                                        ),
                                                        //idea = CaseIdea.REVERSE_AND_RUN_UNTIL_POSSIBILITY_TO_ELEVATE,
                                                        targetCase = caseFromRightNeighbourPosition,
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

                            optimizeCases()
                        }
                }
                    .take(config.workFloorsNumber)
                    .toList()
                    .reversed()
            )
        }
    }
}
