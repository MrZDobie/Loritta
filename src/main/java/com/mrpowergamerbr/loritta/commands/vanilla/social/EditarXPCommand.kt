package com.mrpowergamerbr.loritta.commands.vanilla.social

import com.mrpowergamerbr.loritta.commands.AbstractCommand
import com.mrpowergamerbr.loritta.commands.CommandCategory
import com.mrpowergamerbr.loritta.commands.CommandContext
import com.mrpowergamerbr.loritta.network.Databases
import com.mrpowergamerbr.loritta.utils.Constants
import com.mrpowergamerbr.loritta.utils.locale.BaseLocale
import net.dv8tion.jda.core.Permission
import org.jetbrains.exposed.sql.transactions.transaction

class EditarXPCommand : AbstractCommand("editxp", listOf("editarxp"), category = CommandCategory.SOCIAL) {
	override fun getDescription(locale: BaseLocale): String {
		return locale["EDITARXP_DESCRIPTION"]
	}

	override fun canUseInPrivateChannel(): Boolean {
		return false
	}

	override fun getUsage(): String {
		return "usuário quantidade"
	}

	override fun getDiscordPermissions(): List<Permission> {
		return listOf(Permission.MANAGE_SERVER)
	}

	override suspend fun run(context: CommandContext,locale: BaseLocale) {
		val user = context.getUserAt(0)
		if (user != null && context.rawArgs.size == 2) {
			val newXp = context.rawArgs[1].toLongOrNull()

			if (newXp == null) {
				context.sendMessage("${Constants.ERROR} **|** ${context.getAsMention(true)}${context.locale["INVALID_NUMBER", context.rawArgs[1]]}")
				return
			}

			if (0 > newXp) {
				context.sendMessage(Constants.ERROR + " **|** " + context.getAsMention(true) + context.locale["EDITARXP_MORE_THAN_ZERO"])
				return
			}

			val userData = context.config.getUserData(user.idLong)

			transaction(Databases.loritta) {
				userData.xp = newXp
			}

			context.sendMessage(context.getAsMention(true) + context.locale["EDITARXP_SUCCESS", user.asMention])
		} else {
			context.explain()
		}
	}
}