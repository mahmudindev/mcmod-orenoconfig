package com.github.mahmudindev.mcmod.orenoconfig.config.network;

import com.github.mahmudindev.mcmod.orenoconfig.config.ConfigNode;
import com.github.mahmudindev.mcmod.orenoconfig.network.packet.ConfigPacket;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

public abstract class ModConfigPacket implements ConfigPacket {
    protected void encodeNodeTree(FriendlyByteBuf buf, ConfigNode node) {
        Map<String, ConfigNode> children = node.getChildren();

        buf.writeVarInt(children.size());
        children.forEach((key, value) -> {
            buf.writeUtf(key);

            if (value.isLeaf()) {
                this.writeValue(buf, value);
            }

            this.encodeNodeTree(buf, value);
        });
    }

    protected void decodeNodeTree(FriendlyByteBuf buf, ConfigNode node) {
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();
            ConfigNode child = node.getOrCreatePath(new String[]{key}, 0);

            if (child.isLeaf()) {
                child.setValue(this.readValue(buf));
            }

            this.decodeNodeTree(buf, child);
        }
    }
}
