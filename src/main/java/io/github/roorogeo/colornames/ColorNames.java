package io.github.roorogeo.colornames;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Adds {@code /name color}, which shows a clickable chat menu of every name color.
 *
 * <p>Colors are applied through scoreboard teams (one team per color), so the colored
 * name shows up in chat, the tab list and above the player's head without any client mod.
 */
public class ColorNames implements ModInitializer {
	public static final String MOD_ID = "colornames";

	private static final String TEAM_PREFIX = "namecolor_";
	private static final int BUTTONS_PER_ROW = 4;
	private static final List<ChatFormatting> COLORS = Arrays.stream(ChatFormatting.values())
			.filter(ChatFormatting::isColor)
			.toList();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> color = Commands.literal("color")
				.executes(context -> openMenu(context.getSource()));

		for (ChatFormatting format : COLORS) {
			color.then(Commands.literal(format.getName())
					.executes(context -> setColor(context.getSource(), format)));
		}

		color.then(Commands.literal("reset").executes(context -> resetColor(context.getSource())));

		dispatcher.register(Commands.literal("name").then(color));
	}

	private static int openMenu(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String name = player.getScoreboardName();
		ChatFormatting current = currentColor(source, name);

		player.sendSystemMessage(Component.literal("------- Choose your name color -------").withStyle(ChatFormatting.GOLD));

		MutableComponent row = Component.empty();

		for (int i = 0; i < COLORS.size(); i++) {
			row.append(colorButton(COLORS.get(i), name, COLORS.get(i) == current)).append(" ");

			if ((i + 1) % BUTTONS_PER_ROW == 0) {
				player.sendSystemMessage(row);
				row = Component.empty();
			}
		}

		if (!row.getSiblings().isEmpty()) {
			player.sendSystemMessage(row);
		}

		MutableComponent reset = Component.literal("[Reset]").withStyle(style -> style
				.withColor(ChatFormatting.GRAY)
				.withClickEvent(new ClickEvent.RunCommand("/name color reset"))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Remove your name color"))));

		MutableComponent currentText = current == null
				? Component.literal(name)
				: Component.literal(name).withStyle(current);

		player.sendSystemMessage(Component.literal("Current: ").withStyle(ChatFormatting.GRAY)
				.append(currentText)
				.append("  ")
				.append(reset));

		return 1;
	}

	private static MutableComponent colorButton(ChatFormatting format, String playerName, boolean selected) {
		String label = (selected ? "✔ " : "") + prettyName(format);

		return Component.literal("[" + label + "]").withStyle(style -> style
				.withColor(format)
				.withBold(selected)
				.withClickEvent(new ClickEvent.RunCommand("/name color " + format.getName()))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to set your name to ")
						.append(Component.literal(playerName).withStyle(format)))));
	}

	private static int setColor(CommandSourceStack source, ChatFormatting format) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String name = player.getScoreboardName();
		Scoreboard scoreboard = source.getServer().getScoreboard();
		String teamName = TEAM_PREFIX + format.getName();

		PlayerTeam team = scoreboard.getPlayerTeam(teamName);

		if (team == null) {
			team = scoreboard.addPlayerTeam(teamName);
			team.setDisplayName(Component.literal(prettyName(format)));
			// Teams normally let members see each other while invisible; a cosmetic color should not do that.
			team.setSeeFriendlyInvisibles(false);
		}

		team.setColor(format);
		scoreboard.addPlayerToTeam(name, team);

		source.sendSuccess(() -> Component.literal("Your name color is now ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(name).withStyle(format)), false);
		return 1;
	}

	private static int resetColor(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String name = player.getScoreboardName();
		Scoreboard scoreboard = source.getServer().getScoreboard();
		PlayerTeam team = scoreboard.getPlayersTeam(name);

		if (team == null || !team.getName().startsWith(TEAM_PREFIX)) {
			source.sendFailure(Component.literal("You don't have a name color set."));
			return 0;
		}

		scoreboard.removePlayerFromTeam(name, team);
		source.sendSuccess(() -> Component.literal("Your name color has been reset.").withStyle(ChatFormatting.GRAY), false);
		return 1;
	}

	private static ChatFormatting currentColor(CommandSourceStack source, String playerName) {
		PlayerTeam team = source.getServer().getScoreboard().getPlayersTeam(playerName);

		if (team == null || !team.getName().startsWith(TEAM_PREFIX)) {
			return null;
		}

		return team.getColor();
	}

	private static String prettyName(ChatFormatting format) {
		StringBuilder builder = new StringBuilder();

		for (String word : format.getName().split("_")) {
			if (!builder.isEmpty()) {
				builder.append(' ');
			}

			builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
		}

		return builder.toString();
	}
}
