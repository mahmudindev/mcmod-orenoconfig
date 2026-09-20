package com.github.mahmudindev.mcmod.orenoconfig.network;

import com.github.mahmudindev.mcmod.orenocommons.network.UnifiedNetwork;
import com.github.mahmudindev.mcmod.orenoconfig.OrenoConfig;
import com.github.mahmudindev.mcmod.orenoconfig.config.network.ModCommonConfigPacket;
import com.github.mahmudindev.mcmod.orenoconfig.network.packet.ConfigPacket;
import com.github.mahmudindev.mcmod.orenoevents.event.events.PlayerEvents;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class ConfigNetwork {
    public static final ResourceLocation CHANNEL_NAME = new ResourceLocation(
            OrenoConfig.MOD_ID,
            "default"
    );
    private static final Map<ResourceLocation, ConfigPacket> PACKETS = new HashMap<>();

    public static void init() {
        registerPacket(
                new ResourceLocation(OrenoConfig.MOD_ID, "common"),
                new ModCommonConfigPacket()
        );

        UnifiedNetwork.registerServerPacketReceiver(CHANNEL_NAME, ConfigNetwork::handlePackets);

        PlayerEvents.DISCONNECT.register(ConfigNetwork::onServerPlayerDisconnect);
    }

    public static void registerPacket(ResourceLocation id, ConfigPacket packet) {
        if (PACKETS.containsKey(id)) {
            throw new IllegalStateException("Config packet already exists!");
        }

        PACKETS.put(id, packet);
    }

    public static ConfigPacket getPacket(ResourceLocation id) {
        return PACKETS.get(id);
    }

    public static Map<ResourceLocation, ConfigPacket> getPackets() {
        return PACKETS;
    }

    private static void handlePackets(
            MinecraftServer server,
            ServerPlayer serverPlayer,
            FriendlyByteBuf buf
    ) {
        Map<ResourceLocation, ConfigPacket> packets = new HashMap<>();

        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();

            int readableBytes = buf.readVarInt();
            FriendlyByteBuf bufX = new FriendlyByteBuf(buf.readSlice(readableBytes));

            ConfigPacket packet = PACKETS.get(id);
            if (packet != null) {
                packet.decodeRequirements(bufX, serverPlayer);
                packets.put(id, packet);
                continue;
            }

            bufX.release();
        }

        FriendlyByteBuf bufX = new FriendlyByteBuf(Unpooled.buffer());

        UnifiedNetwork.writeCompressedBuffer(bufX, bufZ -> {
            bufZ.writeVarInt(packets.size());
            packets.forEach((id, packet) -> {
                bufZ.writeResourceLocation(id);
                packet.encode(bufZ, server, serverPlayer);
            });
        });

        UnifiedNetwork.sendPacketToPlayer(serverPlayer, ConfigNetwork.CHANNEL_NAME, bufX);
    }

    private static void onServerPlayerDisconnect(ServerPlayer serverPlayer) {
        PACKETS.forEach((id, packet) -> {
            packet.onServerPlayerDisconnect(serverPlayer);
        });
    }
}
