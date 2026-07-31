package com.mutuo.superreforge.mixin;

import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.item.ModifierNameService;
import com.mutuo.superreforge.item.ModifierResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 ItemStack 已经算出原版/铁砧名称后动态添加前缀。
 *
 * <p>注入返回点可保证再次重命名只改变基础名称，且不会把前缀写入 CUSTOM_NAME。
 */
@Mixin(ItemStack.class)
abstract class ItemStackNameMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void superreforge$prefixDisplayName(CallbackInfoReturnable<Component> callback) {
        ItemStack self = (ItemStack) (Object) this;
        ModifierResolver.resolve(self, DefinitionManager.snapshot()).ifPresent(modifier ->
                callback.setReturnValue(ModifierNameService.prefix(modifier.name(), callback.getReturnValue())));
    }
}
