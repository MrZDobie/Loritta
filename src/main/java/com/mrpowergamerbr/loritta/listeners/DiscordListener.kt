package com.mrpowergamerbr.loritta.listeners

import com.mongodb.client.model.Filters
import com.mrpowergamerbr.loritta.Loritta
import com.mrpowergamerbr.loritta.LorittaLauncher
import com.mrpowergamerbr.loritta.commands.CommandContext
import com.mrpowergamerbr.loritta.userdata.LorittaServerUserData
import com.mrpowergamerbr.loritta.utils.LorittaUtils
import com.mrpowergamerbr.loritta.utils.LorittaUtilsKotlin
import com.mrpowergamerbr.loritta.utils.escapeMentions
import com.mrpowergamerbr.loritta.utils.f
import com.mrpowergamerbr.loritta.utils.humanize
import com.mrpowergamerbr.loritta.utils.modules.InviteLinkUtils
import com.mrpowergamerbr.loritta.utils.save
import com.mrpowergamerbr.loritta.utils.substringIfNeeded
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.core.exceptions.ErrorResponseException
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.concurrent.thread

class DiscordListener(internal val loritta: Loritta) : ListenerAdapter() {
	override fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.author.isBot) { // Se uma mensagem de um bot, ignore a mensagem!
			return
		}
		if (event.isFromType(ChannelType.TEXT)) { // Mensagens em canais de texto
			if (event.textChannel.isNSFW) { // lol nope, I'm outta here
				return
			}
			thread {
				try {
					if (event.guild.id == "297732013006389252") {
						if (event.textChannel.id == "367359479877992449") {
							event.message.delete().complete()
							return@thread
						}
					}
					val serverConfig = loritta.getServerConfigForGuild(event.guild.id)
					val lorittaProfile = loritta.getLorittaProfileForUser(event.author.id)
					val ownerProfile = loritta.getLorittaProfileForUser(event.guild.owner.user.id)
					val locale = loritta.getLocaleById(serverConfig.localeId)

					if (ownerProfile.isBanned) { // Se o dono está banido...
						if (event.member.user.id != Loritta.config.ownerId) { // E ele não é o dono do bot!
							event.guild.leave().complete() // Então eu irei sair daqui, me recuso a ficar em um servidor que o dono está banido! ᕙ(⇀‸↼‶)ᕗ
							return@thread
						}
					}

					if (loritta.ignoreIds.contains(event.author.id)) { // Se o usuário está sendo ignorado...
						if (lorittaProfile.isBanned) { // E ele ainda está banido...
							return@thread // Então flw galerinha
						} else {
							// Se não, vamos remover ele da lista do ignoreIds
							loritta.ignoreIds.remove(event.author.id)
						}
					}

					if (event.message.rawContent.replace("!", "") == "<@297153970613387264>") {
						event.textChannel.sendMessage(locale.MENTION_RESPONSE.f(event.message.author.asMention, serverConfig.commandPrefix)).complete()
					}

					event.member.roles.forEach {
						if (it.name.equals("Inimigo da Loritta", ignoreCase = true)) {
							return@thread
						}
					}

					// ===[ VERIFICAR INVITE LINKS ]===
					if (serverConfig.inviteBlockerConfig.isEnabled) {
						InviteLinkUtils.checkForInviteLinks(event, serverConfig.inviteBlockerConfig)
					}

					// ===[ CÁLCULO DE XP ]===
					// (copyright Loritta™)

					// Primeiro iremos ver se a mensagem contém algo "interessante"
					if (event.message.strippedContent.length >= 5 && lorittaProfile.lastMessageSentHash != event.message.strippedContent.hashCode()) {
						// Primeiro iremos verificar se a mensagem é "válida"
						// 7 chars por millisegundo
						var calculatedMessageSpeed = event.message.strippedContent.toLowerCase().length.toDouble() / 7

						var diff = System.currentTimeMillis() - lorittaProfile.lastMessageSent

						if (diff > calculatedMessageSpeed * 1000) {
							var nonRepeatedCharsMessage = event.message.strippedContent.replace(Regex("(.)\\1{1,}"), "$1")

							if (nonRepeatedCharsMessage.length >= 12) {
								var gainedXp = Math.min(35, Loritta.random.nextInt(Math.max(1, nonRepeatedCharsMessage.length / 7), (Math.max(2, nonRepeatedCharsMessage.length / 4))))

								lorittaProfile.xp = lorittaProfile.xp + gainedXp
								lorittaProfile.lastMessageSentHash = event.message.strippedContent.hashCode()

								val userData = (serverConfig.userData as java.util.Map<String, LorittaServerUserData>).getOrDefault(event.member.user.id, LorittaServerUserData())
								userData.xp = userData.xp + gainedXp
								serverConfig.userData.put(event.member.user.id, userData)
								loritta save serverConfig
							}
						}
					}

					lorittaProfile.lastMessageSent = System.currentTimeMillis()
					loritta save lorittaProfile

					if (serverConfig.aminoConfig.fixAminoImages) {
						for (attachments in event.message.attachments) {
							if (attachments.fileName.endsWith(".Amino") || attachments.fileName == "Amino") {
								val bufferedImage = LorittaUtils.downloadImage(attachments.url)

								val os = ByteArrayOutputStream()
								ImageIO.write(bufferedImage!!, "png", os)
								val inputStream = ByteArrayInputStream(os.toByteArray())

								event.textChannel.sendFile(inputStream, "amino.png", MessageBuilder().append("(Por " + event.member.asMention + ") **Link para o \".Amino\":** " + attachments.url).build()).complete()
								event.message.delete().complete()
							}
						}
					}

					for (eventHandler in serverConfig.nashornEventHandlers) {
						eventHandler.handleMessageReceived(event)
					}

					// Primeiro os comandos vanilla da Loritta(tm)
					loritta.commandManager.commandMap.forEach { cmd ->
						if (serverConfig.debugOptions.enableAllModules || !serverConfig.disabledCommands.contains(cmd.javaClass.simpleName)) {
							if (cmd.handle(event, serverConfig, locale, lorittaProfile)) {
								return@thread
							}
						}
					}

					// E depois os comandos usando JavaScript (Nashorn)
					serverConfig.nashornCommands.forEach { cmd ->
						if (cmd.handle(event, serverConfig, locale, lorittaProfile)) {
							return@thread
						}
					}

					// Executar todos os onCommandMessageReceivedFeedback
					loritta.messageContextCache.values.filter {
						it.guild == event.guild
					}.forEach {
						commandContext -> commandContext.cmd.onCommandMessageReceivedFeedback(commandContext, event, event.message)
					}

					val toLearn = event.message.strippedContent.toLowerCase().escapeMentions()
					loritta.hal.add(toLearn) // TODO: Filtrar links
				} catch (e: Exception) {
					e.printStackTrace()
					LorittaUtilsKotlin.sendStackTrace(event.message, e)
				}
			}
		} else if (event.isFromType(ChannelType.PRIVATE)) { // Mensagens em DMs
			thread {
				val serverConfig = LorittaLauncher.loritta.dummyServerConfig
				val profile = loritta.getLorittaProfileForUser(event.author.id) // Carregar perfil do usuário
				if (event.message.rawContent.replace("!", "").trim() == "<@297153970613387264>") {
					event.channel.sendMessage("Olá " + event.message.author.asMention + "! Em DMs você não precisa usar nenhum prefixo para falar comigo! Para ver o que eu posso fazer, use `ajuda`!").complete()
					return@thread
				}

				// Comandos vanilla da Loritta
				loritta.commandManager.commandMap.forEach{ cmd ->
					if (cmd.handle(event, serverConfig, loritta.getLocaleById("default"), profile)) {
						return@thread
					}
				}
			}
		}
	}

	override fun onGenericMessageReaction(e: GenericMessageReactionEvent) {
		if (e.user.isBot) {
			return
		} // Ignorar reactions de bots

		if (loritta.messageContextCache.containsKey(e.messageId)) {
			val context = LorittaLauncher.getInstance().messageContextCache[e.messageId] as CommandContext
			val t = object : Thread() {
				override fun run() {
					try {
						val msg = e.channel.getMessageById(e.messageId).complete()
						if (msg != null) {
							context.cmd.onCommandReactionFeedback(context, e, msg)
						}
					} catch (exception: Exception) {
						exception.printStackTrace()
						LorittaUtilsKotlin.sendStackTrace("[`${e.guild.name}`] **onGenericMessageReaction ${e.member.user.name}**", exception)
					}
				}
			}
			t.start()
		}

		thread {
			// TODO: Isto deveria ser feito usando a API da Loritta
			if (e.guild.id == "297732013006389252") {
				if (e.textChannel.id == "367359479877992449") {
					var role: Role? = null
					if (e.reactionEmote.name == "\uD83C\uDDE7\uD83C\uDDF7") {
						role = e.guild.getRoleById("367359104320012288")
					} else if (e.reactionEmote.name == "\uD83C\uDDFA\uD83C\uDDF8") {
						role = e.guild.getRoleById("367359247891038209")
					}

					if (role != null) {
						if (e is MessageReactionAddEvent) {
							e.guild.controller.addSingleRoleToMember(e.member, role).complete()
						} else if (e is MessageReactionRemoveEvent) {
							e.guild.controller.removeSingleRoleFromMember(e.member, role).complete()
						}
					}
				}
			}

			try {
				val conf = LorittaLauncher.getInstance().getServerConfigForGuild(e.guild.id)
				val guild = e.guild
				// Sistema de Starboard
				if (conf.starboardConfig.isEnabled) {
					if (e.reactionEmote.name == "⭐") {
						val msg = e.textChannel.getMessageById(e.messageId).complete()
						if (msg != null) {
							val textChannel = guild.getTextChannelById(conf.starboardConfig.starboardId)

							if (textChannel != null && msg.textChannel != textChannel) { // Verificar se não é null e verificar se a reaction não foi na starboard
								var starboardMessageId = conf.starboardEmbeds[e.messageId]
								var starboardMessage: Message? = null
								if (starboardMessageId != null) {
									starboardMessage = textChannel.getMessageById(starboardMessageId).complete()
								}

								val embed = EmbedBuilder()
								val count = e.reaction.users.complete().size
								var content = msg.rawContent
								embed.setAuthor(msg.author.name, null, msg.author.effectiveAvatarUrl)
								embed.setFooter(msg.creationTime.humanize(), null)
								embed.setColor(Color(255, 255, 200 - (count * 20)))

								var emoji = "⭐"

								if (count >= 5) {
									emoji = "\uD83C\uDF1F"
								}
								if (count >= 10) {
									emoji = "\uD83C\uDF20"
								}
								if (count >= 15) {
									emoji = "\uD83D\uDCAB"
								}
								if (count >= 20) {
									emoji = "\uD83C\uDF0C"
								}

								var hasImage = false
								if (msg.attachments.isNotEmpty()) { // Se tem attachments...
									content += "\n**Arquivos:**\n"
									for (attach in msg.attachments) {
										if (attach.isImage && !hasImage) { // Se é uma imagem...
											embed.setImage(attach.url) // Então coloque isso como a imagem no embed!
											hasImage = true
										}
										content += attach.url + "\n"
									}
								}

								embed.setDescription(content)

								val starCountMessage = MessageBuilder()
								starCountMessage.append("$emoji **${count}** ${e.textChannel.asMention}")
								starCountMessage.setEmbed(embed.build())

								if (starboardMessage != null) {
									if (1 > count) { // Remover embed já que o número de stars é menos que 0
										starboardMessage.delete().complete()
										conf.starboardEmbeds.remove(msg.id)
										LorittaLauncher.loritta.ds.save(conf)
										return@thread
									}
									starboardMessage.editMessage(starCountMessage.build()).complete()
								} else {
									starboardMessage = textChannel.sendMessage(starCountMessage.build()).complete()
								}
								conf.starboardEmbeds.put(msg.id, starboardMessage?.id)
								LorittaLauncher.loritta.ds.save(conf)
							}
						}
					}
				}
			} catch (exception: Exception) {
				exception.printStackTrace()
				LorittaUtilsKotlin.sendStackTrace("[`${e.guild.name}`] **Starboard ${e.member.user.name}**", exception)
			}
		}
	}

	override fun onGuildLeave(e: GuildLeaveEvent) {
		// Quando a Loritta sair de uma guild, automaticamente remova o ServerConfig daquele servidor
		LorittaLauncher.loritta.mongo
				.getDatabase("loritta")
				.getCollection("servers")
				.deleteMany(Filters.eq("_id", e.guild.id)) // Tchau! :(
	}

	override fun onGuildJoin(event: GuildJoinEvent) {
		// Vamos alterar a minha linguagem quando eu entrar em um servidor, baseando na localização dele
		val region = event.guild.region
		val regionName = region.name
		val serverConfig = loritta.getServerConfigForGuild(event.guild.id)

		// EN-US
		if (regionName.startsWith("US") ||
				regionName.startsWith("EU") || // TODO: EN-UK
				regionName.startsWith("London")) {
			serverConfig.localeId = "en-us"
		}

		// E depois iremos salvar a configuração do servidor
		thread {
			loritta save serverConfig

			event.guild.members.forEach {
				if (!it.user.isBot && (it.hasPermission(Permission.MANAGE_SERVER) || it.hasPermission(Permission.ADMINISTRATOR))) {
					val message = loritta.getLocaleById(serverConfig.localeId)["LORITTA_ADDED_ON_SERVER", it.asMention, event.guild.name, "https://loritta.website/", "https://discord.gg/V7Kbh4z", loritta.commandManager.commandMap.size, "https://loritta.website/doar"]

					it.user.openPrivateChannel().queue({
						it.sendMessage(message).queue()
					})
				}
			}
		}
	}

	override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
		loritta.executor.execute {
			try {
				val conf = loritta.getServerConfigForGuild(event.guild.id)

				if (conf.autoroleConfig.isEnabled && event.guild.selfMember.hasPermission(Permission.MANAGE_ROLES)) { // Está ativado?
					val rolesId = conf.autoroleConfig.roles // Então vamos pegar todos os IDs...

					val roles = mutableListOf<Role>()

					rolesId.forEach { // E pegar a role dependendo do ID!
						val role = event.guild.getRoleById(it)

						if (role != null && !role.isPublicRole && !role.isManaged && event.guild.selfMember.canInteract(role)) {
							roles.add(role)
						}
					}

					if (roles.isNotEmpty()) {
						if (roles.size == 1) {
							event.guild.controller.addSingleRoleToMember(event.member, roles[0]).reason("Autorole").complete()
						} else {
							event.guild.controller.addRolesToMember(event.member, roles).reason("Autorole").complete()
						}
					}
				}

				if (conf.joinLeaveConfig.isEnabled) { // Está ativado?
					if (conf.joinLeaveConfig.tellOnJoin && conf.joinLeaveConfig.joinMessage.isNotEmpty()) { // E o sistema de avisar ao entrar está ativado?
						val guild = event.guild

						val textChannel = guild.getTextChannelById(conf.joinLeaveConfig.canalJoinId)

						if (textChannel != null) {
							if (textChannel.canTalk()) {
								val msg = LorittaUtils.replaceTokens(conf.joinLeaveConfig.joinMessage, event)
								textChannel.sendMessage(msg.substringIfNeeded()).complete()
							} else {
								LorittaUtils.warnOwnerNoPermission(guild, textChannel, conf)
							}
						}
					}

					if (conf.joinLeaveConfig.tellOnPrivate && conf.joinLeaveConfig.joinPrivateMessage.isNotEmpty()) { // Talvez o sistema de avisar no privado esteja ativado!
						if (!event.user.isBot) { // Mas antes precisamos verificar se o usuário que entrou não é um bot!
							val msg = LorittaUtils.replaceTokens(conf.joinLeaveConfig.joinPrivateMessage, event)
							try {
								event.user.openPrivateChannel().complete().sendMessage(msg.substringIfNeeded()).complete() // Pronto!
							} catch (e: ErrorResponseException) {
								if (e.errorResponse.code != 50007) { // Usuário tem as DMs desativadas
									throw e
								}
							}
						}
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
				LorittaUtilsKotlin.sendStackTrace("[`${event.guild.name}`] **Ao entrar no servidor ${event.user.name}**", e)
			}
		}
	}

	override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
		loritta.executor.execute {
			try {
				if (event.user.id == Loritta.config.clientId) {
					return@execute
				}

				val conf = loritta.getServerConfigForGuild(event.guild.id)

				if (conf.joinLeaveConfig.isEnabled) {
					if (conf.joinLeaveConfig.tellOnLeave && conf.joinLeaveConfig.leaveMessage.isNotEmpty()) {
						val guild = event.guild

						val textChannel = guild.getTextChannelById(conf.joinLeaveConfig.canalLeaveId)

						if (textChannel != null) {
							if (textChannel.canTalk()) {
								var msg = LorittaUtils.replaceTokens(conf.joinLeaveConfig.leaveMessage, event)

								// Para a mensagem de ban nós precisamos ter a permissão de banir membros
								if (event.guild.selfMember.hasPermission(Permission.BAN_MEMBERS)) {
									val banList = guild.bans.complete()
									if (banList.contains(event.user)) {
										if (!conf.joinLeaveConfig.tellOnBan)
											return@execute

										if (conf.joinLeaveConfig.banMessage.isNotEmpty()) {
											msg = LorittaUtils.replaceTokens(conf.joinLeaveConfig.banMessage, event)
										}
									}
								}
								textChannel.sendMessage(msg.substringIfNeeded()).complete()
							} else {
								LorittaUtils.warnOwnerNoPermission(guild, textChannel, conf)
							}
						}
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
				LorittaUtilsKotlin.sendStackTrace("[`${event.guild.name}`] **Ao sair do servidor ${event.user.name}**", e)
			}
		}
	}
}