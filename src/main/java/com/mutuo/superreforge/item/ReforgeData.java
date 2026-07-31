package com.mutuo.superreforge.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * 存放在目标物品上的最小持久数据。
 *
 * <p>不把名称或 Attribute 数值固化到物品；每次读取时用 {@code modifierId + seed} 对照最新
 * 定义重算，从而让 datapack reload 可以更新旧物品。
 */
public record ReforgeData(ResourceLocation modifierId, long seed, int schemaVersion) {
    public static final int CURRENT_SCHEMA = 1;

    /** 保存到物品 NBT/磁盘的数据 Codec。 */
    public static final Codec<ReforgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("modifier_id").forGetter(ReforgeData::modifierId),
                    Codec.LONG.fieldOf("seed").forGetter(ReforgeData::seed),
                    Codec.INT.validate(schema -> schema > 0
                                    ? com.mojang.serialization.DataResult.success(schema)
                                    : com.mojang.serialization.DataResult.error(
                                            () -> "schema_version 必须大于 0"))
                            .fieldOf("schema_version")
                            .forGetter(ReforgeData::schemaVersion))
            .apply(instance, ReforgeData::new));

    /** 菜单和物品同步使用的紧凑网络 Codec。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, ReforgeData> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    ReforgeData::modifierId,
                    ByteBufCodecs.VAR_LONG,
                    ReforgeData::seed,
                    ByteBufCodecs.VAR_INT,
                    ReforgeData::schemaVersion,
                    ReforgeData::new);

    public ReforgeData {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion 必须大于 0");
        }
    }
}
