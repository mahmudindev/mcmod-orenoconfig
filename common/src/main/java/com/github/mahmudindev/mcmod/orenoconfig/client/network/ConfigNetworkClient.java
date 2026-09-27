package com.github.mahmudindev.mcmod.orenoconfig.client.network;

import com.github.mahmudindev.mcmod.orenocommons.client.network.UnifiedNetworkClient;
import com.github.mahmudindev.mcmod.orenocommons.client.network.UnifiedNetworkPacketClient;
import com.github.mahmudindev.mcmod.orenocommons.network.UnifiedNetworkUtility;
import com.github.mahmudindev.mcmod.orenoconfig.network.ConfigNetwork;
import com.github.mahmudindev.mcmod.orenoconfig.network.packet.ConfigPacket;
import com.github.mahmudindev.mcmod.orenoevents.client.event.events.ClientPlayerEvents;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class ConfigNetworkClient {
    public static void init() {
        ClientPlayerEvents.JOIN.register(ConfigNetworkClient::onClientPlayerJoin);

        UnifiedNetworkClient.registerClientPacketReceiver(
                Packet.TYPE,
                Packet.STREAM_CODEC,
                (ctx, value) -> handlePackets(ctx, value.buf())
        );

        ClientPlayerEvents.DISCONNECT.register(ConfigNetworkClient::onClientPlayerDisconnect);
    }

    private static void onClientPlayerJoin(LocalPlayer localPlayer) {
        if (localPlayer.getServer() != null) {
            return;
        }

        if (!UnifiedNetworkClient.canSendPacketToServer(Packet.TYPE)) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        Map<ResourceLocation, ConfigPacket> packets = ConfigNetwork.getPackets();
        buf.writeVarInt(packets.size());
        packets.forEach((id, packet) -> {
            buf.writeResourceLocation(id);

            FriendlyByteBuf bufX = new FriendlyByteBuf(Unpooled.buffer());

            packet.encodeRequirements(bufX, localPlayer);
            buf.writeVarInt(bufX.readableBytes());
            buf.writeBytes(bufX);

            bufX.release();
        });

        UnifiedNetworkClient.sendPacketToServer(new ConfigNetworkClient.Packet(buf));
    }

    private static void handlePackets(
            UnifiedNetworkPacketClient.Context ctx,
            FriendlyByteBuf buf
    ) {
        UnifiedNetworkUtility.readCompressedBuffer(buf, bufX -> {
            Minecraft client = ctx.client();

            int count = bufX.readVarInt();
            for (int i = 0; i < count; i++) {
                ResourceLocation id = bufX.readResourceLocation();
                ConfigPacket packet = ConfigNetwork.getPacket(id);
                packet.decode(bufX, client);
            }
        }, 2_048_000);
    }

    private static void onClientPlayerDisconnect(LocalPlayer localPlayer) {
        Map<ResourceLocation, ConfigPacket> packets = ConfigNetwork.getPackets();
        packets.forEach((id, packet) -> {
            packet.onClientPlayerDisconnect(localPlayer);
        });
    }

    public record Packet(FriendlyByteBuf buf) implements CustomPacketPayload {
        public static final Type<Packet> TYPE = new Type<>(ConfigNetwork.CHANNEL_NAME);
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
