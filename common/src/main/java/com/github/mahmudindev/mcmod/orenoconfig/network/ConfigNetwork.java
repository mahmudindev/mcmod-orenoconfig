package com.github.mahmudindev.mcmod.orenoconfig.network;

import com.github.mahmudindev.mcmod.orenocommons.network.UnifiedNetwork;
import com.github.mahmudindev.mcmod.orenocommons.network.UnifiedNetworkPacket;
import com.github.mahmudindev.mcmod.orenocommons.network.UnifiedNetworkUtility;
import com.github.mahmudindev.mcmod.orenoconfig.OrenoConfig;
import com.github.mahmudindev.mcmod.orenoconfig.config.network.ModCommonConfigPacket;
import com.github.mahmudindev.mcmod.orenoconfig.network.packet.ConfigPacket;
import com.github.mahmudindev.mcmod.orenoevents.event.events.PlayerEvents;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class ConfigNetwork {
    public static final ResourceLocation CHANNEL_NAME = ResourceLocation.fromNamespaceAndPath(
            OrenoConfig.MOD_ID,
            "default"
    );
    private static final Map<ResourceLocation, ConfigPacket> PACKETS = new HashMap<>();

    public static void init() {
        registerPacket(
                ResourceLocation.fromNamespaceAndPath(OrenoConfig.MOD_ID, "common"),
                new ModCommonConfigPacket()
        );

        UnifiedNetwork.registerServerPacketReceiver(
                Packet.TYPE,
                Packet.STREAM_CODEC,
                (ctx, value) -> handlePackets(ctx, value.buf())
        );

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
            UnifiedNetworkPacket.Context ctx,
            FriendlyByteBuf buf
    ) {
        Map<ResourceLocation, ConfigPacket> packets = new HashMap<>();

        ServerPlayer serverPlayer = ctx.player();

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

        UnifiedNetworkUtility.writeCompressedBuffer(bufX, bufZ -> {
            MinecraftServer server = ctx.server();

            bufZ.writeVarInt(packets.size());
            packets.forEach((id, packet) -> {
                bufZ.writeResourceLocation(id);
                packet.encode(bufZ, server, serverPlayer);
            });
        });

        UnifiedNetwork.sendPacketToPlayer(serverPlayer, new ConfigNetwork.Packet(bufX));
    }

    private static void onServerPlayerDisconnect(ServerPlayer serverPlayer) {
        PACKETS.forEach((id, packet) -> {
            packet.onServerPlayerDisconnect(serverPlayer);
        });
    }

    public record Packet(FriendlyByteBuf buf) implements CustomPacketPayload {
        public static final Type<Packet> TYPE = new Type<>(CHANNEL_NAME);
        public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                StreamCodec.of(
                        (buf, value) -> {
                            buf.writeBytes(value);
                            value.release();
                        },
                        buf -> buf
                ),
                Packet::buf,
                Packet::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
