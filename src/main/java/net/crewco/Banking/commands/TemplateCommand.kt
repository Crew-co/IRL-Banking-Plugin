package net.crewco.Banking.commands

import com.google.inject.Inject
import com.mojang.brigadier.context.CommandContext
import net.crewco.Banking.Startup
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import java.util.stream.Stream

class TemplateCommand @Inject constructor(private val plugin: Startup) {
	@Command("templateCommand <item>")
	@CommandDescription("This is a template command")
	@Permission("template.command.use")
	suspend fun template(player: Player, @Argument("args") args: Array<String>) {
	}
}

@Suggestions("args")
fun containerSuggestions(
	context: CommandContext<Player>,
	input: String
): Stream<String> {
	val CommandList = mutableListOf<String>()
	CommandList.add("")
	return CommandList.stream()
}