package turingmachine

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class TuringMachineSimulator : CliktCommand() {
    private val machineFile by argument(help = "Path to the machine description file")
    private val inputFile by argument(help = "Path to the input word file").optional()
    private val auto by option("--auto", help = "Run in automatic mode").flag(default = false)
    private val delay by option("--delay", help = "Delay between steps in automatic mode")
        .double().default(0.5)

    override fun run() {
        val machineDescription = parseMachineDescription(machineFile)
        val inputWord = if (inputFile != null) {
            File(inputFile!!).readText().trim()
        } else {
            print("Enter input word: ")
            readlnOrNull() ?: ""
        }

        val turingMachine = TuringMachine(
            startingState = machineDescription.start,
            acceptedState = machineDescription.accept,
            rejectedState = machineDescription.reject,
            transitions = machineDescription.transitionFunction,
        )
        simulateMachine(turingMachine, inputWord, machineDescription.blank)
    }

    private fun parseMachineDescription(filePath: String): MachineDescription {
        var start: String? = null
        var accept: String? = null
        var reject: String? = null
        var blank: Char? = null
        val transitionFunction: MutableList<TransitionFunction> = mutableListOf()
        val regex = """(\S+)\s+(\S+)\s*->\s*(\S+)\s+(\S+)\s+([><^])""".toRegex()
        val mapTransition = mapOf(
            '<' to TapeTransition.Left,
            '^' to TapeTransition.Stay,
            '>' to TapeTransition.Right,
        )
        val newBufferedReader = Files.newBufferedReader(Path.of(filePath))
        newBufferedReader.use { reader ->
            reader.forEachLine {
                when {
                    it.startsWith("start: ") -> start = it.trim().substringAfterLast(" ")
                    it.startsWith("accept: ") -> accept = it.trim().substringAfterLast(" ")
                    it.startsWith("reject: ") -> reject = it.trim().substringAfterLast(" ")
                    it.startsWith("blank: ") -> blank = it.trim().last()
                    regex.matches(it) -> {
                        val fromTo = it.split("->")
                        val from = fromTo[0].trim().split("\\s+".toRegex())
                        val to = fromTo[1].trim().split("\\s+".toRegex())
                        val move = mapTransition[to[2].last()]!!
                        transitionFunction.add(TransitionFunction(from[0], from[1].last(), move, to[1].last(), to[0]))
                    }

                    else -> throw IllegalArgumentException(it)
                }
            }
        }
        newBufferedReader.close()
        Objects.requireNonNull(start)
        Objects.requireNonNull(accept)
        Objects.requireNonNull(reject)
        Objects.requireNonNull(blank)
        return MachineDescription(start!!, accept!!, reject!!, blank!!, transitionFunction)
    }

    private fun simulateMachine(
        machine: TuringMachine,
        inputWord: String,
        blank: Char,
    ) {
        val simulate = machine.simulate(inputWord)
        for (snap in simulate) {
            if (!auto) {
                println("Print Enter to continue:")
                readln()
            }
            Thread.sleep((delay * 1000).toLong())
            println("State: " + snap.state)
            println(snap.tape.content.map { if (it == BLANK) blank else it }.joinToString(" "))
        }
    }

    class MachineDescription(
        val start: String,
        val accept: String,
        val reject: String,
        val blank: Char,
        val transitionFunction: List<TransitionFunction>,
    )
}

fun main(args: Array<String>) = TuringMachineSimulator().main(args)
