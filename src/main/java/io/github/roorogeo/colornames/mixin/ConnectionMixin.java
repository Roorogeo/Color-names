package io.github.roorogeo.colornames.mixin;

import io.netty.channel.ChannelFutureListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

import io.github.roorogeo.colornames.TabListColors;

/**
 * Every outgoing packet passes through here, so recoloring at this point also covers
 * tab list names that other mods set or send themselves.
 */
@Mixin(Connection.class)
public abstract class ConnectionMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
	private void colornames$colorTabList(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
		TabListColors.apply(packet);
	}
}
