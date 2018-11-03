package com.mrpowergamerbr.loritta.utils.locale

import com.mrpowergamerbr.loritta.utils.f
import mu.KotlinLogging
import java.text.MessageFormat

/**
 * Localization class, this is partly generated by the LocaleGenerator
 */
open class BaseLocale {
	companion object {
		@JvmStatic
		private val logger = KotlinLogging.logger {}
	}

	@Transient
	@Deprecated("Please use the inner classes")
	var strings = mutableMapOf<String, String>()
	@Transient
	var commands = Commands()
	@Transient
	var loritta = Loritta()

	@Deprecated("Please use the inner classes")
	operator fun get(key: String, vararg arguments: Any?): String {
		if (!strings.containsKey(key)) {
			logger.warn {"Missing translation key! $key" }
			return key
		}
		return strings[key]!!.f(*arguments)
	}

	/**
	 * Gets and formats an message
	 *
	 * @param args  the arguments
	 * @param block the message
	 *
	 * @return      the formatted message
	 */
	fun <T> format(vararg args: Any?, block: BaseLocale.() -> T): T {
		val result = block.invoke(this)
		return when (result) {
			is String -> MessageFormat.format(result, *args) as T
			is List<*> -> result.map { MessageFormat.format(it.toString(), *args) } as T
			else -> throw UnsupportedOperationException("Can't parse $result in BaseLocale!")
		}
	}

	// Generic
	lateinit var SHIP_valor90: List<String>

	lateinit var SHIP_valor80: List<String>

	lateinit var SHIP_valor70: List<String>

	lateinit var SHIP_valor60: List<String>

	lateinit var SHIP_valor50: List<String>

	lateinit var SHIP_valor40: List<String>

	lateinit var SHIP_valor30: List<String>

	lateinit var SHIP_valor20: List<String>

	lateinit var SHIP_valor10: List<String>

	lateinit var SHIP_valor0: List<String>

	// !!!       DO NOT EDIT        !!!
	// ===[ AUTO GENEREATED LOCALE ]===
	class Loritta {
		lateinit var translationAuthors: List<String>
	}
	class Commands {
		lateinit var pleaseWaitCooldown: String
		lateinit var errorWhileExecutingCommand: String
		lateinit var cantUseInPrivate: String
		lateinit var userDoesNotExists: String
		class Arguments {
			lateinit var text: String
			lateinit var number: String
			lateinit var user: String
			lateinit var image: String
		}
		var arguments = Arguments()

		class Language {
			lateinit var description: String
			lateinit var pleaseSelectYourLanguage: String
			lateinit var translatedBy: String
			lateinit var languageChanged: String
		}
		var language = Language()

		class Ajuda {
			lateinit var errorWhileOpeningDm: String
		}
		var ajuda = Ajuda()

		class Roll {
			lateinit var description: String
			lateinit var howMuchSides: String
		}
		var roll = Roll()

		class Vieirinha {
			lateinit var description: String
			lateinit var examples: List<String>
			lateinit var responses: List<String>
		}
		var vieirinha = Vieirinha()

		class Vemdezap {
			lateinit var description: String
			lateinit var examples: List<String>
		}
		var vemdezap = Vemdezap()

		class Actions {
			lateinit var examples: List<String>
			class Slap {
				lateinit var description: String
				lateinit var response: String
				lateinit var responseAntiIdiot: String
			}
			var slap = Slap()

			class Kiss {
				lateinit var description: String
				lateinit var response: String
				lateinit var responseAntiIdiot: String
			}
			var kiss = Kiss()

			class Attack {
				lateinit var description: String
				lateinit var response: String
				lateinit var responseAntiIdiot: String
			}
			var attack = Attack()

			class Hug {
				lateinit var description: String
				lateinit var response: String
			}
			var hug = Hug()

			class Dance {
				lateinit var description: String
				lateinit var response: String
			}
			var dance = Dance()

		}
		var actions = Actions()
	}
	// ===[ END  GENEREATED LOCALE ]===
	// !!!       DO NOT EDIT        !!!
}