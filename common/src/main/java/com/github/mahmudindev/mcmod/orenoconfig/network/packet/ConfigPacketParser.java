package com.github.mahmudindev.mcmod.orenoconfig.network.packet;

import com.github.mahmudindev.mcmod.orenoconfig.OrenoConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class ConfigPacketParser {
    private static final Map<ResourceLocation, ValueParser<?>> VALUE_PARSERS = new HashMap<>();
    private static final Map<Class<?>, ResourceLocation> VALUE_PARSER_ID_CLASSES = new HashMap<>();
    private static final ResourceLocation OBJ_ARR_VALUE_PARSER_ID;

    public static <T> void registerValueParser(
            ResourceLocation id,
            Class<T> clazz,
            ValueParser<T> valueParser
    ) {
        VALUE_PARSERS.put(id, valueParser);
        VALUE_PARSER_ID_CLASSES.put(clazz, id);
    }

    public static void writeValue(FriendlyByteBuf buf, Object value) {
        if (value == null) {
            buf.writeBoolean(false);
            return;
        }

        FriendlyByteBuf bufX = new FriendlyByteBuf(Unpooled.buffer());

        ResourceLocation id = getValueParserId(value.getClass());
        if (id == null) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        bufX.writeResourceLocation(id);
        ((ValueParser) getValueParser(id)).encode(bufX, value);

        buf.writeVarInt(bufX.readableBytes());
        buf.writeBytes(bufX);
        bufX.release();
    }

    public static Object readValue(FriendlyByteBuf buf) {
        boolean hasValue = buf.readBoolean();
        if (!hasValue) {
            return null;
        }

        int readableBytes = buf.readVarInt();
        FriendlyByteBuf bufX = new FriendlyByteBuf(buf.readSlice(readableBytes));

        ResourceLocation id = bufX.readResourceLocation();
        ValueParser valueParser = getValueParser(id);
        if (valueParser != null) {
            Object o = valueParser.decode(bufX);
            bufX.release();
            return o;
        }

        bufX.release();
        return null;
    }

    private static ValueParser<?> getValueParser(ResourceLocation id) {
        return VALUE_PARSERS.get(id);
    }

    private static ResourceLocation getValueParserId(Class<?> clazz) {
        ResourceLocation id = VALUE_PARSER_ID_CLASSES.get(clazz);
        if (id != null) {
            return id;
        }

        if (clazz.isArray() && !clazz.getComponentType().isPrimitive()) {
            return OBJ_ARR_VALUE_PARSER_ID;
        }

        for (Class<?> claxx : VALUE_PARSER_ID_CLASSES.keySet()) {
            if (claxx.isAssignableFrom(clazz)) {
                ResourceLocation idX = VALUE_PARSER_ID_CLASSES.get(claxx);
                VALUE_PARSER_ID_CLASSES.put(claxx, idX);
                return idX;
            }
        }

        return null;
    }

    public interface ValueParser<T> {
        void encode(FriendlyByteBuf buf, T value);

        T decode(FriendlyByteBuf buf);
    }

    static {
        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "byte"),
                Byte.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Byte value) {
                        buf.writeByte(value);
                    }

                    @Override
                    public Byte decode(FriendlyByteBuf buf) {
                        return buf.readByte();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "byte_arr"),
                byte[].class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, byte[] value) {
                        buf.writeByteArray(value);
                    }

                    @Override
                    public byte[] decode(FriendlyByteBuf buf) {
                        return buf.readByteArray();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "short"),
                Short.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Short value) {
                        buf.writeShort(value);
                    }

                    @Override
                    public Short decode(FriendlyByteBuf buf) {
                        return buf.readShort();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "short_arr"),
                short[].class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, short[] value) {
                        buf.writeVarInt(value.length);

                        for (short v : value) {
                            buf.writeShort(v);
                        }
                    }

                    @Override
                    public short[] decode(FriendlyByteBuf buf) {
                        int length = buf.readVarInt();

                        short[] arr = new short[length];
                        for (int i = 0; i < length; i++) {
                            arr[i] = buf.readShort();
                        }
                        return arr;
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "int"),
                Integer.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Integer value) {
                        buf.writeVarInt(value);
                    }

                    @Override
                    public Integer decode(FriendlyByteBuf buf) {
                        return buf.readVarInt();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "int_arr"),
                int[].class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, int[] value) {
                        buf.writeVarIntArray(value);
                    }

                    @Override
                    public int[] decode(FriendlyByteBuf buf) {
                        return buf.readVarIntArray();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "long"),
                Long.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Long value) {
                        buf.writeVarLong(value);
                    }

                    @Override
                    public Long decode(FriendlyByteBuf buf) {
                        return buf.readVarLong();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "long_arr"),
                long[].class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, long[] value) {
                        buf.writeVarInt(value.length);

                        for (long v : value) {
                            buf.writeVarLong(v);
                        }
                    }

                    @Override
                    public long[] decode(FriendlyByteBuf buf) {
                        int length = buf.readVarInt();

                        long[] arr = new long[length];
                        for (int i = 0; i < length; i++) {
                            arr[i] = buf.readVarLong();
                        }
                        return arr;
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "float"),
                Float.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Float value) {
                        buf.writeFloat(value);
                    }

                    @Override
                    public Float decode(FriendlyByteBuf buf) {
                        return buf.readFloat();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "float_arr"),
                float[].class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, float[] value) {
                        buf.writeVarInt(value.length);

                        for (float v : value) {
                            buf.writeFloat(v);
                        }
                    }

                    @Override
                    public float[] decode(FriendlyByteBuf buf) {
                        int length = buf.readVarInt();

                        float[] arr = new float[length];
                        for (int i = 0; i < length; i++) {
                            arr[i] = buf.readFloat();
                        }
                        return arr;
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "double"),
                Double.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Double value) {
                        buf.writeDouble(value);
                    }

                    @Override
                    public Double decode(FriendlyByteBuf buf) {
                        return buf.readDouble();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "double_arr"),
                double[].class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, double[] value) {
                        buf.writeVarInt(value.length);

                        for (double v : value) {
                            buf.writeDouble(v);
                        }
                    }

                    @Override
                    public double[] decode(FriendlyByteBuf buf) {
                        int length = buf.readVarInt();

                        double[] arr = new double[length];
                        for (int i = 0; i < length; i++) {
                            arr[i] = buf.readDouble();
                        }
                        return arr;
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "char"),
                Character.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Character value) {
                        buf.writeChar(value);
                    }

                    @Override
                    public Character decode(FriendlyByteBuf buf) {
                        return buf.readChar();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "char_arr"),
                char[].class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, char[] value) {
                        buf.writeVarInt(value.length);

                        for (char v : value) {
                            buf.writeChar(v);
                        }
                    }

                    @Override
                    public char[] decode(FriendlyByteBuf buf) {
                        int length = buf.readVarInt();

                        char[] arr = new char[length];
                        for (int i = 0; i < length; i++) {
                            arr[i] = buf.readChar();
                        }
                        return arr;
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "boolean"),
                Boolean.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Boolean value) {
                        buf.writeBoolean(value);
                    }

                    @Override
                    public Boolean decode(FriendlyByteBuf buf) {
                        return buf.readBoolean();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "boolean_arr"),
                boolean[].class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, boolean[] value) {
                        buf.writeVarInt(value.length);

                        for (boolean v : value) {
                            buf.writeBoolean(v);
                        }
                    }

                    @Override
                    public boolean[] decode(FriendlyByteBuf buf) {
                        int length = buf.readVarInt();

                        boolean[] arr = new boolean[length];
                        for (int i = 0; i < length; i++) {
                            arr[i] = buf.readBoolean();
                        }
                        return arr;
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "string"),
                String.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, String value) {
                        buf.writeUtf(value);
                    }

                    @Override
                    public String decode(FriendlyByteBuf buf) {
                        return buf.readUtf();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "list"),
                List.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, List value) {
                        buf.writeVarInt(value.size());

                        for (Object v : value) {
                            writeValue(buf, v);
                        }
                    }

                    @Override
                    public List decode(FriendlyByteBuf buf) {
                        int size = buf.readVarInt();

                        List<Object> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            list.add(readValue(buf));
                        }
                        return list;
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "set"),
                Set.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Set value) {
                        buf.writeVarInt(value.size());

                        for (Object v : value) {
                            writeValue(buf, v);
                        }
                    }

                    @Override
                    public Set decode(FriendlyByteBuf buf) {
                        int size = buf.readVarInt();

                        Set<Object> set = new HashSet<>(size);
                        for (int i = 0; i < size; i++) {
                            set.add(readValue(buf));
                        }
                        return set;
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "map"),
                Map.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Map value) {
                        buf.writeVarInt(value.size());

                        for (Object entry : value.entrySet()) {
                            writeValue(buf, ((Map.Entry) entry).getKey());
                            writeValue(buf, ((Map.Entry) entry).getValue());
                        }
                    }

                    @Override
                    public Map decode(FriendlyByteBuf buf) {
                        int size = buf.readVarInt();

                        Map<Object, Object> map = new HashMap<>(size);
                        for (int i = 0; i < size; i++) {
                            Object k = readValue(buf);
                            Object v = readValue(buf);
                            map.put(k, v);
                        }
                        return map;
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "uuid"),
                UUID.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, UUID value) {
                        buf.writeUUID(value);
                    }

                    @Override
                    public UUID decode(FriendlyByteBuf buf) {
                        return buf.readUUID();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "vector3f"),
                Vector3f.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Vector3f value) {
                        buf.writeVector3f(value);
                    }

                    @Override
                    public Vector3f decode(FriendlyByteBuf buf) {
                        return buf.readVector3f();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "quaternionf"),
                Quaternionf.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Quaternionf value) {
                        buf.writeQuaternion(value);
                    }

                    @Override
                    public Quaternionf decode(FriendlyByteBuf buf) {
                        return buf.readQuaternion();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "resourceLocation"),
                ResourceLocation.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, ResourceLocation value) {
                        buf.writeResourceLocation(value);
                    }

                    @Override
                    public ResourceLocation decode(FriendlyByteBuf buf) {
                        return buf.readResourceLocation();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "blockPos"),
                BlockPos.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, BlockPos value) {
                        buf.writeBlockPos(value);
                    }

                    @Override
                    public BlockPos decode(FriendlyByteBuf buf) {
                        return buf.readBlockPos();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "chunkPos"),
                ChunkPos.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, ChunkPos value) {
                        buf.writeChunkPos(value);
                    }

                    @Override
                    public ChunkPos decode(FriendlyByteBuf buf) {
                        return buf.readChunkPos();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "sectionPos"),
                SectionPos.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, SectionPos value) {
                        buf.writeSectionPos(value);
                    }

                    @Override
                    public SectionPos decode(FriendlyByteBuf buf) {
                        return buf.readSectionPos();
                    }
                }
        );

        registerValueParser(
                new ResourceLocation(OrenoConfig.MOD_ID, "nbt"),
                CompoundTag.class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, CompoundTag value) {
                        buf.writeNbt(value);
                    }

                    @Override
                    public CompoundTag decode(FriendlyByteBuf buf) {
                        return buf.readNbt();
                    }
                }
        );

        OBJ_ARR_VALUE_PARSER_ID = new ResourceLocation(OrenoConfig.MOD_ID, "obj_arr");
        registerValueParser(
                OBJ_ARR_VALUE_PARSER_ID,
                Object[].class,
                new ValueParser<>() {
                    @Override
                    public void encode(FriendlyByteBuf buf, Object[] value) {
                        buf.writeVarInt(value.length);

                        for (Object v : value) {
                            writeValue(buf, v);
                        }
                    }

                    @Override
                    public Object[] decode(FriendlyByteBuf buf) {
                        int length = buf.readVarInt();

                        Object[] arr = new Object[length];
                        for (int i = 0; i < length; i++) {
                            arr[i] = readValue(buf);
                        }
                        return arr;
                    }
                }
        );
    }
}
