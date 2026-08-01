package com.mutuo.superreforge.progress;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 保存整台服务器共享的激活阶段 ID。
 *
 * <p>数据挂在 Overworld 的 data storage 上，因此即使末地/下界卸载或服务器重启也不会丢失。
 */
public final class ServerProgressData extends SavedData {
    public static final String FILE_ID = "superreforge_progress";
    public static final Factory<ServerProgressData> FACTORY =
            new Factory<>(ServerProgressData::new, ServerProgressData::load, DataFixTypes.LEVEL);

    private final Set<String> activeStages = new HashSet<>();

    public static ServerProgressData load(CompoundTag tag, HolderLookup.Provider registries) {
        ServerProgressData data = new ServerProgressData();
        ListTag list = tag.getList("active_stages", Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            data.activeStages.add(list.getString(index));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        activeStages.stream().sorted().map(StringTag::valueOf).forEach(list::add);
        tag.put("active_stages", list);
        return tag;
    }

    public Set<String> activeStages() {
        return Set.copyOf(activeStages);
    }

    public void setActive(String id, boolean active) {
        boolean changed = active ? activeStages.add(id) : activeStages.remove(id);
        if (changed) {
            setDirty();
        }
    }
}
