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
                floor = input.nextInt(),
                position = input.nextInt(),
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

            if (clone == area.exit) {
                useExit()
            }

            val isElevator = area.elevators.isElevator(clone)

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
                .minByOrNull {
                    it.constraints.roundsLeft
                }

            requireNotNull(bestCase) {
                "No suitable case found"
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

private enum class Direction {
    LEFT,
    RIGHT,
    NONE,
}

private enum class CloneAction {
    KEEP_GOING,
    BUILD_ELEVATOR,
    BLOCK_CLONE,
}

private data class Case(
    val point: AreaPoint,
    val direction: Direction,
    val targetCase: Case? = null,
    val action: CloneAction,
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
    val elevatorsLeft: Int,
    val clonesLeft: Int,
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
    val floorsNumber: Int,
    val width: Int,
    val roundsNumber: Int,
    val exitFloor: Int,
    val exitPosition: Int,
    val totalClonesNumber: Int,
    val additionalElevatorsNumber: Int,
    val elevators: Elevators,
    val clonesEmissionPeriod: Int,
) {
    val cloneCostInRounds = clonesEmissionPeriod
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
                    val elevatorsNumber = input.nextInt()
                    for (i in 0 until elevatorsNumber) {
                        val elevatorFloor = input.nextInt()
                        val elevatorPos = input.nextInt()

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
                                    direction = Direction.LEFT,
                                    action = CloneAction.KEEP_GOING,
                                    constraints = StateConstraints(
                                        roundsLeft = roundsToExit,
                                        elevatorsLeft = 0,
                                        clonesLeft = 1,
                                    ),
                                ),
                                Case(
                                    point = floorPointAt(config.exitPosition),
                                    direction = Direction.RIGHT,
                                    action = CloneAction.KEEP_GOING,
                                    constraints = StateConstraints(
                                        roundsLeft = roundsToExit,
                                        elevatorsLeft = 0,
                                        clonesLeft = 1,
                                    ),
                                ),
                            ),
                        )

                        (config.exitPosition - 1 downTo 0)
                            .takeWhile { position ->
                                !elevators.isElevator(floorPointAt(position))
                            }
                            .forEach { position ->
                                addCases(
                                    position = position,
                                    newCases = listOf(
                                        Case(
                                            point = floorPointAt(position),
                                            direction = Direction.LEFT,
                                            action = CloneAction.BLOCK_CLONE,
                                            constraints = StateConstraints(
                                                roundsLeft = config.exitPosition - position + config.cloneCostInRounds + roundsToExit,
                                                elevatorsLeft = 0,
                                                clonesLeft = 2,
                                            ),
                                        ),
                                        Case(
                                            point = floorPointAt(position),
                                            direction = Direction.RIGHT,
                                            action = CloneAction.KEEP_GOING,
                                            constraints = StateConstraints(
                                                roundsLeft = config.exitPosition - position + roundsToExit,
                                                elevatorsLeft = 0,
                                                clonesLeft = 1,
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
                                addCases(
                                    position = position,
                                    newCases = listOf(
                                        Case(
                                            point = floorPointAt(position),
                                            direction = Direction.RIGHT,
                                            action = CloneAction.BLOCK_CLONE,
                                            constraints = StateConstraints(
                                                roundsLeft = position - config.exitPosition + config.cloneCostInRounds,
                                                elevatorsLeft = 0,
                                                clonesLeft = 2,
                                            ),
                                        ),
                                        Case(
                                            point = floorPointAt(position),
                                            direction = Direction.LEFT,
                                            action = CloneAction.KEEP_GOING,
                                            constraints = StateConstraints(
                                                roundsLeft = position - config.exitPosition,
                                                elevatorsLeft = 0,
                                                clonesLeft = 1,
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
                                        addCases(
                                            position = position,
                                            newCases = upperFloor
                                                .cases[position]
                                                .map { upperFloorCase ->
                                                    upperFloorCase.copy(
                                                        point = floorPointAt(position),
                                                        constraints = upperFloorCase.constraints.copy(
                                                            roundsLeft = upperFloorCase.constraints.roundsLeft + 1,
                                                        ),
                                                        action = CloneAction.KEEP_GOING,
                                                        targetCase = upperFloorCase,
                                                    )
                                                }
                                                .also {
                                                    existingElevatorCasesByPosition[position] = it
                                                }
                                        )
                                    } else {
                                        addCases(
                                            position = position,
                                            newCases = upperFloor
                                                .cases[position]
                                                .map { upperFloorCase ->
                                                    upperFloorCase.copy(
                                                        point = floorPointAt(position),
                                                        action = CloneAction.BUILD_ELEVATOR,
                                                        constraints = upperFloorCase.constraints.copy(
                                                            elevatorsLeft = upperFloorCase.constraints.elevatorsLeft + 1,
                                                            clonesLeft = upperFloorCase.constraints.clonesLeft + 1,
                                                            roundsLeft = upperFloorCase.constraints.roundsLeft + 1 + config.cloneCostInRounds,
                                                        ),
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

                            val leftMostPosition = 0
                            generateSequence(
                                HorizontalRunCase(
                                    position = leftMostPosition,
                                    cases = emptyList(),
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
                                        cases = emptyList(),
                                        isElevator = true,
                                    )
                                } else {
                                    val casesInLeftNeighbourPosition = if (previousCase.isElevator) {
                                        existingElevatorCasesByPosition[previousCase.position]!!
                                    } else {
                                        newElevatorCasesByPosition[previousCase.position]!!
                                    } + previousCase.cases

                                    HorizontalRunCase(
                                        position = currentPosition,

                                        cases = casesInLeftNeighbourPosition
                                            .filter {
                                                it.direction == Direction.LEFT
                                            }
                                            .flatMap { caseFromLeftNeighbourPosition ->
                                                listOf(
                                                    caseFromLeftNeighbourPosition.copy(
                                                        point = floorPointAt(currentPosition),
                                                        direction = Direction.LEFT,
                                                        action = CloneAction.KEEP_GOING,
                                                        constraints = caseFromLeftNeighbourPosition.constraints.copy(
                                                            roundsLeft = caseFromLeftNeighbourPosition.constraints.roundsLeft + 1,
                                                        ),
                                                        targetCase = caseFromLeftNeighbourPosition,
                                                    ),
                                                    caseFromLeftNeighbourPosition.copy(
                                                        point = floorPointAt(currentPosition),
                                                        direction = Direction.RIGHT,
                                                        action = CloneAction.BLOCK_CLONE,
                                                        constraints = caseFromLeftNeighbourPosition.constraints.copy(
                                                            clonesLeft = caseFromLeftNeighbourPosition.constraints.clonesLeft + 1,
                                                            roundsLeft = caseFromLeftNeighbourPosition.constraints.roundsLeft + 1 + config.cloneCostInRounds,
                                                        ),
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

                            val rightMostPosition = width - 1
                            generateSequence(
                                HorizontalRunCase(
                                    position = rightMostPosition,
                                    cases = emptyList(),
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
                                        cases = emptyList(),
                                        isElevator = true,
                                    )
                                } else {
                                    val casesInRightNeighbourPosition = if (previousCase.isElevator) {
                                        existingElevatorCasesByPosition[previousCase.position]!!
                                    } else {
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
