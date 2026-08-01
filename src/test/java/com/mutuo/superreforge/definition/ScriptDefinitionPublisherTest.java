package com.mutuo.superreforge.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mutuo.superreforge.api.ScriptDefinitionBundle;
import com.mutuo.superreforge.api.ScriptDefinitionPublisher;
import com.mutuo.superreforge.reforge.SelectorHooks;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 验证首次开服的延迟发布，以及 datapack 已就绪后的真实配置错误不会覆盖有效快照。 */
final class ScriptDefinitionPublisherTest {
    private static final ResourceLocation LEVEL = ResourceLocation.fromNamespaceAndPath("test", "level");
    private static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("test", "type");
    private static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath("test", "modifier");

    @BeforeEach
    void resetState() {
        DefinitionManager.resetForTests();
        ScriptDefinitionPublisher.clearServerSession();
    }

    @Test
    void retriesDeferredScriptBundleAfterDatapackPublication() {
        ModifierDefinition modifier = new ModifierDefinition(
                LEVEL, List.of(TYPE), Component.literal("等待数据包"), 1.0, List.of());
        ScriptDefinitionBundle bundle = new ScriptDefinitionBundle(
                DefinitionLayer.builder().modifier(MODIFIER, modifier).build(),
                Map.of("test:ready", stack -> true),
                Map.of());

        // 首轮校验缺少 datapack 中的等级和类型，因此保留整个 bundle，且任何伴随状态都不能提前生效。
        assertEquals(ScriptDefinitionPublisher.Result.DEFERRED, ScriptDefinitionPublisher.publish(bundle));
        assertFalse(DefinitionManager.snapshot().modifiers().containsKey(MODIFIER));
        assertFalse(SelectorHooks.testScript("test:ready", ItemStack.EMPTY));

        DefinitionSnapshot datapack = new DefinitionSnapshot(
                Map.of(LEVEL, new LevelDefinition(1, Component.literal("等级"))),
                Map.of(TYPE, new ItemTypeDefinition(List.of(), List.of())),
                Map.of(),
                Map.of());
        assertTrue(DefinitionManager.publishDatapack(datapack));
        assertTrue(ScriptDefinitionPublisher.retryPending());

        assertTrue(DefinitionManager.snapshot().modifiers().containsKey(MODIFIER));
        assertTrue(SelectorHooks.testScript("test:ready", ItemStack.EMPTY));
    }

    @Test
    void rejectsInvalidScriptImmediatelyOnceDatapackIsReady() {
        DefinitionSnapshot datapack = new DefinitionSnapshot(
                Map.of(LEVEL, new LevelDefinition(1, Component.literal("等级"))),
                Map.of(),
                Map.of(),
                Map.of());
        assertTrue(DefinitionManager.publishDatapack(datapack));

        ModifierDefinition invalidModifier = new ModifierDefinition(
                LEVEL, List.of(TYPE), Component.literal("缺少类型"), 1.0, List.of());
        ScriptDefinitionBundle invalidBundle = new ScriptDefinitionBundle(
                DefinitionLayer.builder().modifier(MODIFIER, invalidModifier).build(), Map.of(), Map.of());

        // 正常运行期的错误脚本不会被伪装成“等待首次加载”，也不会污染上一份有效快照。
        assertEquals(ScriptDefinitionPublisher.Result.REJECTED, ScriptDefinitionPublisher.publish(invalidBundle));
        assertFalse(DefinitionManager.snapshot().modifiers().containsKey(MODIFIER));
        assertFalse(ScriptDefinitionPublisher.retryPending());
    }
}
