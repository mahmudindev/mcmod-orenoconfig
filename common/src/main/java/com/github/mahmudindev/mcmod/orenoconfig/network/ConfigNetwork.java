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

        UnifiedNetwork.registerClientPacketCodec(Packet.TYPE, Packet.STREAM_CODEC);
        UnifiedNetwork.registerServerPacketCodec(Packet.TYPE, Packet.STREAM_CODEC);
        UnifiedNetwork.registerServerPacketReceiver(
                Packet.TYPE,
                (ctx, value) -> {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeBytes(value.bytes());
                    handlePackets(ctx, buf);
                    buf.release();
                }
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

            FriendlyByteBuf bufX = new FriendlyByteBuf(Unpooled.buffer());

            int readableBytes = buf.readVarInt();
            buf.readBytes(bufX, readableBytes);

            ConfigPacket packet = PACKETS.get(id);
            if (packet != null) {
                packet.decodeRequirements(bufX, serverPlayer);
                packets.put(id, packet);
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

        byte[] bytes = new byte[bufX.readableBytes()];
        bufX.readBytes(bytes);
        UnifiedNetwork.sendPacketToPlayer(serverPlayer, new Packet(bytes));
    }

    private static void onServerPlayerDisconnect(ServerPlayer serverPlayer) {
        PACKETS.forEach((id, packet) -> {
            packet.onServerPlayerDisconnect(serverPlayer);
        });
    }

    public record Packet(byte[] bytes) implements CustomPacketPayload {
        public static final Type<Packet> TYPE = new Type<>(CHANNEL_NAME);
        public static final StreamCodec<RegistryFriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.composite(
                StreamCodec.of(
                        FriendlyByteBuf::writeBytes,
                        buf -> {
                            byte[] bytes = new byte[buf.readableBytes()];
                            buf.readBytes(bytes);
                            return bytes;
                        }
                ),
                Packet::bytes,
                Packet::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
