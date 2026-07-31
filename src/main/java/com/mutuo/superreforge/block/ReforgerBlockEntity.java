package com.mutuo.superreforge.block;

import com.mutuo.superreforge.config.GlobalSettings;
import com.mutuo.superreforge.config.SuperReforgeConfig;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.progress.ProgressService;
import com.mutuo.superreforge.reforge.ExperienceService;
import com.mutuo.superreforge.reforge.PreparedReforge;
import com.mutuo.superreforge.reforge.ReforgeFailure;
import com.mutuo.superreforge.reforge.ReforgeTransaction;
import com.mutuo.superreforge.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** 保存输入、媒介、动画进度和待揭晓结果，并执行服务端原子重铸。 */
public final class ReforgerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TARGET_SLOT = 0;
    public static final int CATALYST_SLOT = 1;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public int getSlotLimit(int slot) {
            return slot == TARGET_SLOT ? 1 : super.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return pending == null;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return pending == null ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private PendingReforge pending;
    private ReforgeFailure lastFailure;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> pending == null ? 0 : 1;
                case 1 -> pending == null ? 0 : pending.remainingTicks();
                case 2 -> pending == null ? 0 : pending.totalTicks();
                case 3 -> lastFailure == null ? -1 : lastFailure.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端菜单只接收同步值，不允许反向修改服务端方块状态。
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ReforgerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFORGER.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public ContainerData menuData() {
        return menuData;
    }

    /** 客户端渲染器只读访问同步后的动画状态。 */
    public Optional<PendingReforge> pending() {
        return Optional.ofNullable(pending);
    }

    public boolean startReforge(ServerPlayer player) {
        if (pending != null || level == null) {
            lastFailure = ReforgeFailure.BUSY;
            return false;
        }
        GlobalSettings settings = SuperReforgeConfig.snapshot();
        var quoteResult = ReforgeTransaction.quote(
                inventory.getStackInSlot(TARGET_SLOT),
                inventory.getStackInSlot(CATALYST_SLOT),
                DefinitionManager.snapshot(),
                settings,
                ProgressService.highestActive(player.getServer()));
        if (quoteResult.quote().isEmpty()) {
            lastFailure = quoteResult.failure().orElse(ReforgeFailure.STALE_STATE);
            return false;
        }
        var quote = quoteResult.quote().orElseThrow();
        boolean paymentRequired = !player.isCreative() || settings.creativePlayersPay();
        if (paymentRequired
                && !ExperienceService.canPay(player, quote.cost().experience(), settings.experienceMode())) {
            lastFailure = ReforgeFailure.EXPERIENCE;
            return false;
        }

        PreparedReforge prepared = ReforgeTransaction.prepare(
                inventory.getStackInSlot(TARGET_SLOT),
                inventory.getStackInSlot(CATALYST_SLOT),
                quote,
                player.getRandom().nextLong());
        inventory.setStackInSlot(TARGET_SLOT, ItemStack.EMPTY);
        inventory.setStackInSlot(CATALYST_SLOT, prepared.catalystRemainder());
        if (paymentRequired) {
            ExperienceService.pay(player, quote.cost().experience(), settings.experienceMode());
        }
        pending = new PendingReforge(prepared.result(), settings.animationTicks(), settings.animationTicks());
        lastFailure = null;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReforgerBlockEntity entity) {
        if (entity.pending == null) {
            return;
        }
        int before = entity.pending.remainingTicks();
        entity.pending = entity.pending.tick();
        int impactTick = entity.pending.totalTicks() / 2;
        if (before > impactTick && entity.pending.remainingTicks() <= impactTick
                && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.LAVA,
                    pos.getX() + 0.5,
                    pos.getY() + 0.72,
                    pos.getZ() + 0.5,
                    14,
                    0.22,
                    0.10,
                    0.22,
                    0.02);
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.85F, 1.12F);
        }
        if (entity.pending.ready()) {
            entity.inventory.setStackInSlot(TARGET_SLOT, entity.pending.result());
            entity.pending = null;
        }
        entity.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    /** 破坏方块时 pending 结果和普通库存只会各掉落一次。 */
    public List<ItemStack> drainForDrop() {
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        if (pending != null && !pending.result().isEmpty()) {
            drops.add(pending.result().copy());
            pending = null;
        }
        setChanged();
        return drops;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        if (pending != null) {
            CompoundTag pendingTag = new CompoundTag();
            pending.result().save(registries, pendingTag);
            tag.put("pending_result", pendingTag);
            tag.putInt("pending_total", pending.totalTicks());
            tag.putInt("pending_remaining", pending.remainingTicks());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        if (tag.contains("pending_result")) {
            ItemStack result = ItemStack.parseOptional(registries, tag.getCompound("pending_result"));
            int total = Math.max(1, tag.getInt("pending_total"));
            int remaining = Math.clamp(tag.getInt("pending_remaining"), 0, total);
            pending = result.isEmpty() ? null : new PendingReforge(result, total, remaining);
        } else {
            pending = null;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.superreforge.reforger");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReforgerMenu(containerId, playerInventory, this);
    }
}
