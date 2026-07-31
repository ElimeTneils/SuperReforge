package com.mutuo.superreforge.item;

import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.reforge.DeterministicValue;
import com.mutuo.superreforge.reforge.ItemTypeResolver;
import com.mutuo.superreforge.registry.ModDataComponents;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 将物品保存的 ID/seed 对照当前 reload 快照解析，不读取旧名称或旧数值。 */
public final class ModifierResolver {
    private ModifierResolver() {}

    public static Optional<ResolvedModifier> resolve(ItemStack stack, DefinitionSnapshot snapshot) {
        ReforgeData data = stack.get(ModDataComponents.REFORGE_DATA.get());
        if (data == null) {
            return Optional.empty();
        }
        var definition = snapshot.modifiers().get(data.modifierId());
        if (definition == null) {
            return Optional.empty();
        }
        Set<ResourceLocation> currentTypes = ItemTypeResolver.resolve(stack, snapshot);
        if (definition.itemTypes().stream().noneMatch(currentTypes::contains)) {
            return Optional.empty();
        }
        var effects = definition.attributes().stream()
                .map(effect -> new ResolvedEffect(
                        effect.id(),
                        effect.attribute(),
                        DeterministicValue.resolve(effect.amount(), data.seed(), effect.id()),
                        effect.operation(),
                        effect.slots(),
                        effect.showInTooltip()))
                .toList();
        return Optional.of(new ResolvedModifier(data.modifierId(), definition.name(), effects));
    }
}
