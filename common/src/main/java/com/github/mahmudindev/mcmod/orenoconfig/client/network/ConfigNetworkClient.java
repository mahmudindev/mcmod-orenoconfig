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
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class ConfigNetworkClient {
    public static void init() {
        ClientPlayerEvents.JOIN.register(ConfigNetworkClient::onClientPlayerJoin);

        UnifiedNetworkClient.registerClientPacketReceiver(
                ConfigNetwork.Packet.TYPE,
                (ctx, value) -> {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeBytes(value.bytes());
                    handlePackets(ctx, buf);
                    buf.release();
                }
        );

        ClientPlayerEvents.DISCONNECT.register(ConfigNetworkClient::onClientPlayerDisconnect);
    }

    private static void onClientPlayerJoin(LocalPlayer localPlayer) {
        if (localPlayer.getServer() != null) {
            return;
        }

        if (!UnifiedNetworkClient.canSendPacketToServer(ConfigNetwork.Packet.TYPE)) {
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

        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        UnifiedNetworkClient.sendPacketToServer(new ConfigNetwork.Packet(bytes));
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
}
