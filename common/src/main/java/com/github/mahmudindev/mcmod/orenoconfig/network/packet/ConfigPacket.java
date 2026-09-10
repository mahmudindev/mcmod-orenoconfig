package com.github.mahmudindev.mcmod.orenoconfig.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public interface ConfigPacket {
    default void encodeRequirements(FriendlyByteBuf buf, LocalPlayer localPlayer) {
    }

    default void decodeRequirements(FriendlyByteBuf buf, ServerPlayer serverPlayer) {
    }

    void encode(FriendlyByteBuf buf, MinecraftServer server, ServerPlayer player);

    default void writeValue(FriendlyByteBuf buf, Object value) {
        ConfigPacketParser.writeValue(buf, value);
    }

    void decode(FriendlyByteBuf buf, Minecraft client);

    default Object readValue(FriendlyByteBuf buf) {
        return ConfigPacketParser.readValue(buf);
    }

    default void onClientPlayerDisconnect(LocalPlayer localPlayer) {
    }

    default void onServerPlayerDisconnect(ServerPlayer serverPlayer) {
    }
}
