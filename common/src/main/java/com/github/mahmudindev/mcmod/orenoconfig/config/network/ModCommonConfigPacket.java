package com.github.mahmudindev.mcmod.orenoconfig.config.network;

import com.github.mahmudindev.mcmod.orenoconfig.config.ConfigNode;
import com.github.mahmudindev.mcmod.orenoconfig.config.configs.ModCommonConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class ModCommonConfigPacket extends ModConfigPacket {
    private static final Map<String, Handler> HANDLERS = new HashMap<>();
    private static final Map<ServerPlayer, Set<String>> PLAYER_CONFIGS = new HashMap<>();

    @Override
    public void encodeRequirements(FriendlyByteBuf buf, LocalPlayer localPlayer) {
        buf.writeVarInt(HANDLERS.size());
        HANDLERS.forEach((key, handler) -> {
            buf.writeUtf(key);
        });
    }

    @Override
    public void decodeRequirements(FriendlyByteBuf buf, ServerPlayer serverPlayer) {
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();

            Set<String> configs = PLAYER_CONFIGS.computeIfAbsent(
                    serverPlayer,
                    keyX -> new HashSet<>()
            );

            if (HANDLERS.containsKey(key)) {
                configs.add(key);
            }

        }
    }

    @Override
    public void encode(FriendlyByteBuf buf, MinecraftServer server, ServerPlayer player) {
        Map<String, Handler> handlers = new HashMap<>();

        HANDLERS.forEach((key, handler) -> {
            Set<String> configs = PLAYER_CONFIGS.get(player);
            if (configs == null || !configs.contains(key)) {
                return;
            }

            handlers.put(key, handler);
        });

        buf.writeVarInt(handlers.size());
        handlers.forEach((key, handler) -> {
            buf.writeUtf(key);

            FriendlyByteBuf bufX = new FriendlyByteBuf(Unpooled.buffer());

            this.encodeNodeTree(bufX, handler.getGetterNode());
            buf.writeVarInt(bufX.readableBytes());
            buf.writeBytes(bufX);

            bufX.release();
        });
    }

    @Override
    public void decode(FriendlyByteBuf buf, Minecraft client) {
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();

            int readableBytes = buf.readVarInt();
            FriendlyByteBuf bufX = new FriendlyByteBuf(buf.readSlice(readableBytes));

            Handler handler = HANDLERS.get(key);
            if (handler != null) {
                this.decodeNodeTree(bufX, handler.getSynchronizedGetterNode());
            }

            bufX.release();
        }
    }

    @Override
    public void onClientPlayerDisconnect(LocalPlayer localPlayer) {
        ModCommonConfig.clearServerSync();
    }

    @Override
    public void onServerPlayerDisconnect(ServerPlayer serverPlayer) {
        PLAYER_CONFIGS.remove(serverPlayer);
    }

    public static void registerHandler(String name, Handler handler) {
        HANDLERS.put(name, handler);
    }

    public interface Handler {
        ConfigNode getGetterNode();

        ConfigNode getSynchronizedGetterNode();
    }
}
