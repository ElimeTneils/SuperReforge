package com.mutuo.superreforge.item;

import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.definition.ModifierDisplayDefinition;
import com.mutuo.superreforge.definition.ModifierDefinition;
import com.mutuo.superreforge.reforge.DeterministicValue;
import com.mutuo.superreforge.reforge.ItemTypeResolver;
import com.mutuo.superreforge.registry.ModDataComponents;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 将物品保存的 ID/seed 对照当前 reload 快照解析，不读取旧名称或旧数值。 */
public final class ModifierResolver {
    private ModifierResolver() {}

    public static Optional<ResolvedModifier> resolve(ItemStack stack, DefinitionSnapshot snapshot) {
        ReforgeData data = stack.get(ModDataComponents.REFORGE_DATA.get());
        if (data == null || !data.active()) {
            return Optional.empty();
        }
        var definition = snapshot.modifiers().get(data.modifierId());
        if (definition == null || !definitionMatches(stack, snapshot, data)) {
            return Optional.empty();
        }
        return resolveDefinition(data, definition);
    }

    /**
     * 远程客户端只接收显示快照，不接收 KubeJS 谓词或服务端选择器。
     * 它信任服务端写入并同步到物品组件的 active 标志，只负责按 ID/seed 生成名称与效果。
     */
    public static Optional<ResolvedModifier> resolveMirrored(
            ItemStack stack, Map<ResourceLocation, ModifierDisplayDefinition> definitions) {
        ReforgeData data = stack.get(ModDataComponents.REFORGE_DATA.get());
        if (data == null || !data.active()) {
            return Optional.empty();
        }
        ModifierDisplayDefinition definition = definitions.get(data.modifierId());
        return definition == null ? Optional.empty() : resolveDefinition(data, definition);
    }

    /** 服务端使用完整快照；远程客户端在本地服务端快照为空时使用已同步显示镜像。 */
    public static Optional<ResolvedModifier> resolveCurrent(ItemStack stack) {
        DefinitionSnapshot serverSnapshot = DefinitionManager.snapshot();
        return serverSnapshot.modifiers().isEmpty()
                ? resolveMirrored(stack, DefinitionManager.clientModifiers())
                : resolve(stack, serverSnapshot);
    }

    static boolean definitionMatches(ItemStack stack, DefinitionSnapshot snapshot, ReforgeData data) {
        ModifierDefinition definition = snapshot.modifiers().get(data.modifierId());
        if (definition == null) {
            return false;
        }
        Set<ResourceLocation> currentTypes = ItemTypeResolver.resolve(stack, snapshot);
        return definition.itemTypes().stream().anyMatch(currentTypes::contains);
    }

    private static Optional<ResolvedModifier> resolveDefinition(
            ReforgeData data, ModifierDefinition definition) {
        return resolveDefinition(data, definition.name(), definition.attributes());
    }

    private static Optional<ResolvedModifier> resolveDefinition(
            ReforgeData data, ModifierDisplayDefinition definition) {
        return resolveDefinition(data, definition.name(), definition.attributes());
    }

    private static Optional<ResolvedModifier> resolveDefinition(
            ReforgeData data,
            net.minecraft.network.chat.Component name,
            java.util.List<com.mutuo.superreforge.definition.AttributeEffectDefinition> attributes) {
        var effects = attributes.stream()
                .map(effect -> new ResolvedEffect(
                        effect.id(),
                        effect.attribute(),
                        DeterministicValue.resolve(effect.amount(), data.seed(), effect.id()),
                        effect.operation(),
                        effect.slots(),
                        effect.showInTooltip()))
                .toList();
        return Optional.of(new ResolvedModifier(data.modifierId(), name, effects));
    }
}
