package com.mutuo.superreforge.block;

import com.mutuo.superreforge.registry.ModBlocks;
import com.mutuo.superreforge.registry.ModMenus;
import com.mutuo.superreforge.config.SuperReforgeConfig;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.network.ReforgePreviewPayload;
import com.mutuo.superreforge.progress.ProgressService;
import com.mutuo.superreforge.reforge.ReforgeTransaction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/** 两个机器槽加玩家背包，并通过菜单按钮发送无参数“开始重铸”意图。 */
public final class ReforgerMenu extends AbstractContainerMenu {
    public static final int START_BUTTON = 0;

    private final ReforgerBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Player owner;
    private ReforgePreviewPayload lastPreview;

    /** 客户端工厂构造器，从服务端附加数据读取方块位置。 */
    public ReforgerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(
                containerId,
                inventory,
                inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof ReforgerBlockEntity reforger
                        ? reforger
                        : null);
    }

    public ReforgerMenu(int containerId, Inventory playerInventory, ReforgerBlockEntity blockEntity) {
        super(ModMenus.REFORGER.get(), containerId);
        this.blockEntity = blockEntity;
        this.owner = playerInventory.player;
        this.access = blockEntity == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = blockEntity == null ? new SimpleContainerData(4) : blockEntity.menuData();
        ItemStackHandler handler = blockEntity == null ? new ItemStackHandler(2) : blockEntity.inventory();

        addSlot(new SlotItemHandler(handler, ReforgerBlockEntity.TARGET_SLOT, 35, 44));
        addSlot(new SlotItemHandler(handler, ReforgerBlockEntity.CATALYST_SLOT, 35, 80));
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    /** 输入或定义变化后向当前服务端玩家同步真实概率，不接受客户端报价。 */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity == null || !(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        var snapshot = DefinitionManager.snapshot();
        var result = ReforgeTransaction.quote(
                blockEntity.inventory().getStackInSlot(ReforgerBlockEntity.TARGET_SLOT),
                blockEntity.inventory().getStackInSlot(ReforgerBlockEntity.CATALYST_SLOT),
                snapshot,
                SuperReforgeConfig.snapshot(),
                ProgressService.highestActive(serverPlayer.getServer()));
        ReforgePreviewPayload preview = ReforgePreviewPayload.from(containerId, result, snapshot);
        if (!preview.equals(lastPreview)) {
            lastPreview = preview;
            PacketDistributor.sendToPlayer(serverPlayer, preview);
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 116 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 174));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return id == START_BUTTON
                && blockEntity != null
                && player instanceof ServerPlayer serverPlayer
                && blockEntity.startReforge(serverPlayer);
    }

    public boolean pending() {
        return data.get(0) != 0;
    }

    public int remainingTicks() {
        return data.get(1);
    }

    public int totalTicks() {
        return data.get(2);
    }

    public int failureOrdinal() {
        return data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return empty;
        }
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < 2) {
            if (!moveItemStackTo(original, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(original, 0, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.REFORGER.get());
    }
}
