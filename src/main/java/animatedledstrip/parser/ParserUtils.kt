package animatedledstrip.parser

const val HELP_INDENT_LEN = 18

fun <T, A> CommandParser<T, A>.Command.action(action: T.(A, List<String>) -> Unit) {
    this.action = action
}

fun <T, A> CommandParser<T, A>.Command.subCommand(
    identifier: String,
    op: CommandParser<T, A>.Command.() -> Unit
): CommandParser<T, A>.Command {
    val newCommand = parser.Command(identifier).apply { op() }
    addSubCommand(newCommand)
    return newCommand
}

fun <T, A> CommandParser<T, A>.command(
    identifier: String,
    op: CommandParser<T, A>.Command.() -> Unit
): CommandParser<T, A>.Command {
    val newCommand = this.Command(identifier).apply { op() }
    addCommand(newCommand)
    return newCommand
}
