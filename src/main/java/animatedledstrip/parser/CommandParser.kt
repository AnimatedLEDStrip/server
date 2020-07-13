package animatedledstrip.parser

import org.pmw.tinylog.Logger

class CommandParser<T, A>(private val context: T) {

    val commandNeedsSubCommandAction: T.(Command) -> Unit = { cmd ->
        Logger.error("Command ${cmd.identifier} requires a sub-command: ${cmd.subCommandNames}")
    }

    inner class Command(
        var identifier: String
    ) {
        var shortIdentifier: String? = null
        var description: String? = null
        var action: (T.(A, List<String>) -> Unit)? = null
        var argStr: String? = null
        var argOptional: Boolean = false

        val parser: CommandParser<T, A>
            get() = this@CommandParser

        val subCommands: MutableList<Command> = mutableListOf()
        val subCommandNames: List<String>
            get() = subCommands.map { it.identifier }

        fun addSubCommand(newCommand: Command) {
            subCommands.add(newCommand)
        }

        fun helpMessage(parentIdentifier: String = ""): String {
            var msg = ""

            fun addDescription() {
                if (msg.length < HELP_INDENT_LEN)
                    msg = msg.padEnd(HELP_INDENT_LEN) + "$description"
                else
                    msg += "\n${" ".repeat(HELP_INDENT_LEN)}$description"
            }

            fun addSubCommands() {
                msg += "\n" + subCommands.sortedBy { it.identifier }
                    .joinToString("\n") {
                        it.helpMessage("${if (parentIdentifier != "") "$parentIdentifier " else ""}$identifier")
                    }
            }


            if (parentIdentifier != "") msg += "$parentIdentifier "

            msg += identifier

            when {
                subCommands.isEmpty() && argStr == null -> {
                    addDescription()
                }
                subCommands.isEmpty() && argStr != null && !argOptional -> {
                    msg += " $argStr"
                    addDescription()
                }
                subCommands.isEmpty() && argStr != null && argOptional -> {
                    msg += " [$argStr]"
                    addDescription()
                }
                action == null -> {
                    msg += if (argStr != null)
                        " {$argStr, ${subCommands.sortedBy { it.identifier }.joinToString(", ") { it.identifier }}}"
                    else
                        " {${subCommands.sortedBy { it.identifier }.joinToString(", ") { it.identifier }}}"

                    addDescription()
                    addSubCommands()
                }
                action != null -> {
                    msg += when {
                        argStr != null && !argOptional ->
                            " {$argStr, ${subCommands.sortedBy { it.identifier }.joinToString(", ") { it.identifier }}}"
                        argStr != null && argOptional ->
                            " [$argStr, ${subCommands.sortedBy { it.identifier }.joinToString(", ") { it.identifier }}]"
                        else -> " [${subCommands.sortedBy { it.identifier }.joinToString(", ") { it.identifier }}]"
                    }

                    addDescription()
                    addSubCommands()
                }
            }

            return msg
        }

        fun parseArgs(args: List<String>, arg: A): ParseResult {
            check(action != null || subCommands.isNotEmpty()) { "Bad Command Definition" }
            val firstIdentifier = args.getOrNull(0)
            if (firstIdentifier != null && subCommands.isNotEmpty()) {
                val subCommand = subCommands.firstOrNull {
                    firstIdentifier.equals(it.identifier, ignoreCase = true)
                            || firstIdentifier.equals(it.shortIdentifier, ignoreCase = true)
                }
                if (subCommand != null) return subCommand.parseArgs(args.drop(1), arg)
                if (action == null) return ParseResult.NEEDS_SUBCOMMAND
            }

            return if (action == null) ParseResult.INVALID_COMMAND
            else {
                action?.invoke(context, arg, args)
                ParseResult.SUCCESS
            }
        }
    }

    private val commands = mutableListOf<Command>()

    init {
        command("help") {
            description = "Print this help message"

            action { arg, _ ->
                val msg = commands.sortedBy { it.identifier }.joinToString("\n") {
                    it.helpMessage()
                }
                helpMessageAction(arg, msg)
            }
        }
    }

    fun addCommand(newCommand: Command) {
        commands.add(newCommand)
    }

    var helpMessageAction: T.(A, String) -> Unit = { _, msg ->
        Logger.info(msg)
    }

    var invalidCommandAction: T.(A, String) -> Unit = { _, cmd ->
        Logger.error("Bad command: $cmd")
    }

    fun parseCommand(command: String, arg: A) {
        parseCommand(command.removePrefix("CMD :").toUpperCase().split(" "), arg)
    }

    private fun parseCommand(args: List<String>, arg: A) {
        val firstIdentifier = args.getOrNull(0) ?: return
        val cmd =
            commands
                .firstOrNull {
                    firstIdentifier.equals(it.identifier, ignoreCase = true)
                            || firstIdentifier.equals(it.shortIdentifier, ignoreCase = true)
                }
                ?: run { invalidCommandAction.invoke(context, arg, firstIdentifier); return }
        when (cmd.parseArgs(args.drop(1), arg)) {
            ParseResult.SUCCESS -> return
            ParseResult.NEEDS_SUBCOMMAND -> commandNeedsSubCommandAction(context, cmd)
            ParseResult.INVALID_COMMAND -> invalidCommandAction(context, arg, firstIdentifier)
        }
    }
}
