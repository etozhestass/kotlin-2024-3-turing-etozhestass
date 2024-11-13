package turingmachine

class TuringMachine(
    private val startingState: String,
    private val acceptedState: String,
    private val rejectedState: String,
    transitions: Collection<TransitionFunction>,
) {
    private val transitionMap: Map<Pair<String, Char>, TransitionFunction> =
        transitions.associateBy { it.state to it.symbol }

    fun initialSnapshot(input: String): Snapshot = Snapshot(startingState, Tape(input))

    fun simulateStep(snapshot: Snapshot): Snapshot {
        val currentSymbol = snapshot.tape.getCurrentSymbol()
        val key = snapshot.state to currentSymbol
        val transition = transitionMap[key]

        if (transition != null) {
            val newSnapshot = snapshot.copy()
            newSnapshot.applyTransition(transition.transition)
            return newSnapshot
        } else {
            val newSnapshot = snapshot.copy()
            newSnapshot.state = rejectedState
            return newSnapshot
        }
    }

    fun simulate(input: String): Sequence<Snapshot> = sequence {
        var currentSnapshot = initialSnapshot(input)
        yield(currentSnapshot)
        while (currentSnapshot.state != acceptedState && currentSnapshot.state != rejectedState) {
            currentSnapshot = simulateStep(currentSnapshot)
            yield(currentSnapshot)
        }
    }

    class Snapshot(var state: String, val tape: Tape) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Snapshot

            if (state != other.state) return false
            if (tape != other.tape) return false

            return true
        }

        override fun hashCode(): Int {
            var result = state.hashCode()
            result = 31 * result + tape.hashCode()
            return result
        }

        override fun toString(): String {
            return "Snapshot(state='$state', tape=$tape)"
        }

        fun copy(): Snapshot = Snapshot(state, tape.copy())
        fun applyTransition(transition: Transition): Snapshot {
            state = transition.newState
            tape.applyTransition(transition.newSymbol, transition.move)
            return this
        }
    }

    class Tape {
        private val tape: ArrayDeque<Char>
        private var headPosition: Int

        constructor(input: String) {
            tape = ArrayDeque(input.toList())
            headPosition = 0
        }

        private constructor(tape: ArrayDeque<Char>, headPosition: Int) {
            this.tape = ArrayDeque(tape)
            this.headPosition = headPosition
        }

        val content: CharArray
            get() {
                val firstNonBlankIndex = tape.indexOfFirst { it != BLANK }.let { if (it == -1) headPosition else it }
                val lastNonBlankIndex = tape.indexOfLast { it != BLANK }.let { if (it == -1) headPosition else it }

                val firstIndex = minOf(firstNonBlankIndex, headPosition)
                val lastIndex = maxOf(lastNonBlankIndex, headPosition)

                val contentList = (firstIndex..lastIndex).map { idx ->
                    if (idx in tape.indices) tape[idx] else BLANK
                }
                return contentList.toCharArray()
            }

        val position: Int
            get() {
                val firstNonBlankIndex = tape.indexOfFirst { it != BLANK }.let { if (it == -1) headPosition else it }
                val firstIndex = minOf(firstNonBlankIndex, headPosition)
                return headPosition - firstIndex
            }

        fun applyTransition(char: Char, move: TapeTransition): Tape {
            if (headPosition == -1) {
                tape.addFirst(BLANK)
                headPosition = 0
            } else if (headPosition == tape.size) {
                tape.addLast(BLANK)
            }
            tape[headPosition] = char
            when (move) {
                TapeTransition.Left -> headPosition--
                TapeTransition.Right -> headPosition++
                TapeTransition.Stay -> {}
            }
            return this
        }

        fun getCurrentSymbol(): Char =
            if (headPosition < 0 || headPosition >= tape.size) {
                BLANK
            } else {
                tape[headPosition]
            }

        fun copy(): Tape = Tape(ArrayDeque(tape), headPosition)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Tape) return false
            return content.contentEquals(other.content) && position == other.position
        }

        override fun hashCode(): Int {
            var result = content.contentHashCode()
            result = 31 * result + position
            return result
        }

        override fun toString(): String {
            val contentArray = content
            val pos = position
            return contentArray.mapIndexed { index, c ->
                if (index == pos) "[$c]" else "$c"
            }.joinToString("")
        }
    }
}
