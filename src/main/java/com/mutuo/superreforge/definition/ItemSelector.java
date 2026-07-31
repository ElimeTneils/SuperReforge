package com.mutuo.superreforge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * 数据驱动物品选择器。
 *
 * <p>同一个对象可写单个 ID、ID 列表、物品标签、Curios 通用匹配或 KubeJS 谓词。
 * 至少一个字段必须存在，这一约束由 {@link DefinitionValidator} 集中报告。
 */
public record ItemSelector(
        Optional<ResourceLocation> item,
        List<ResourceLocation> items,
        Optional<ResourceLocation> tag,
        Optional<String> curios,
        Optional<String> kubejsPredicate) {
    public static final Codec<ItemSelector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("item").forGetter(ItemSelector::item),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(ItemSelector::items),
                    ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(ItemSelector::tag),
                    Codec.STRING.optionalFieldOf("curios").forGetter(ItemSelector::curios),
                    Codec.STRING.optionalFieldOf("kubejs_predicate").forGetter(ItemSelector::kubejsPredicate))
            .apply(instance, ItemSelector::new));

    public ItemSelector {
        items = List.copyOf(items);
    }

    /** 供内置媒介和测试快速创建精确物品 ID 选择器。 */
    public static ItemSelector item(ResourceLocation itemId) {
        return new ItemSelector(Optional.of(itemId), List.of(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** 判断这个对象是否没有提供任何可执行的匹配规则。 */
    public boolean isEmpty() {
        return item.isEmpty() && items.isEmpty() && tag.isEmpty() && curios.isEmpty() && kubejsPredicate.isEmpty();
    }
}
