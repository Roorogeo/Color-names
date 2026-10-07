package io.github.roorogeo.colornames;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.TeamColor;

/**
 * Placeholders for Placeholder API, so tab list, chat and scoreboard mods can show name colors.
 *
 * <p>Only loaded when Placeholder API is installed.
 */
final class PlaceholderCompat {
	private PlaceholderCompat() {
	}

	static void register() {
		// %colornames:name% -> the player's name in their chosen color.
		Placeholders.registerServer(Identifier.fromNamespaceAndPath(ColorNames.MOD_ID, "name"), (context, argument) -> {
			if (!context.hasServerPlayer()) {
				return PlaceholderResult.invalid("No player!");
			}

			String name = context.serverPlayer().getScoreboardName();
			TeamColor color = ColorNames.nameColor(context.server().getScoreboard(), name);

			return PlaceholderResult.value(color == null ? Component.literal(name) : ColorNames.colored(name, color));
		});
	}
}
