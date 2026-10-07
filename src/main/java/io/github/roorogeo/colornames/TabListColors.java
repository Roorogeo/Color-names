package io.github.roorogeo.colornames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.TeamColor;

import io.github.roorogeo.colornames.mixin.ClientboundPlayerInfoUpdatePacketAccessor;

/**
 * Applies name colors to tab list entries.
 *
 * <p>The client only uses a team's color in the tab list when the entry has no custom display
 * name. Tab list and rank mods usually set one, which hides the team color, so the color is
 * applied to the display name itself right before the packet is sent.
 */
public final class TabListColors {
	// The same packet object is sent to every player, so only rewrite it once.
	private static final Set<ClientboundPlayerInfoUpdatePacket> PROCESSED =
			Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	private static volatile MinecraftServer server;

	private TabListColors() {
	}

	static void setServer(MinecraftServer server) {
		TabListColors.server = server;
	}

	public static void apply(Packet<?> packet) {
		MinecraftServer server = TabListColors.server;

		if (server == null
				|| !(packet instanceof ClientboundPlayerInfoUpdatePacket info)
				|| !info.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME)
				|| !PROCESSED.add(info)) {
			return;
		}

		List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>(info.entries());
		boolean changed = false;

		for (int i = 0; i < entries.size(); i++) {
			ClientboundPlayerInfoUpdatePacket.Entry entry = entries.get(i);
			ServerPlayer player = server.getPlayerList().getPlayer(entry.profileId());

			if (player == null) {
				continue;
			}

			String name = player.getScoreboardName();
			TeamColor color = ColorNames.nameColor(server.getScoreboard(), name);

			if (color == null) {
				continue;
			}

			entries.set(i, new ClientboundPlayerInfoUpdatePacket.Entry(entry.profileId(), entry.profile(), entry.listed(),
					entry.latency(), entry.gameMode(), colorDisplayName(entry.displayName(), name, color), entry.showHat(),
					entry.listOrder(), entry.chatSession()));
			changed = true;
		}

		if (changed) {
			((ClientboundPlayerInfoUpdatePacketAccessor) info).colornames$setEntries(List.copyOf(entries));
		}
	}

	private static Component colorDisplayName(Component displayName, String name, TeamColor color) {
		if (displayName == null || displayName.getString().equals(name)) {
			return Component.literal(name).withStyle(style -> style.withColor(color.textColor()));
		}

		// Keep extra text such as rank prefixes; parts without their own color take the name color.
		return Component.empty().withStyle(style -> style.withColor(color.textColor())).append(displayName);
	}
}
