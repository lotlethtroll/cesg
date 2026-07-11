package com.cesg.storage.enderbarrel;

import java.util.List;
import java.util.UUID;

import com.cesg.init.CESGBlockEntities;
import com.cesg.util.CESGLang;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/** One half of an Ender Barrel twin pair; the inventory lives in {@link EnderBarrelSharedStorage}. */
public class EnderBarrelBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    @Nullable
    private UUID pairId;

    public EnderBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.ENDER_BARREL.get(), pos, state);
    }

    @Nullable
    public UUID getPairId() {
        return pairId;
    }

    public void setPairId(UUID pairId) {
        this.pairId = pairId;
        setChanged();
    }

    /** Short human-readable pair code (matches the item tooltip) so twins can be identified. */
    public String pairCode() {
        return pairId == null ? "?" : pairId.toString().substring(0, 8);
    }

    /** The live shared inventory — server side only. */
    @Nullable
    public SimpleContainer sharedPool() {
        if (pairId == null || !(level instanceof ServerLevel serverLevel))
            return null;
        return EnderBarrelSharedStorage.get(serverLevel.getServer()).pool(pairId);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (pairId != null)
            tag.putUUID("PairId", pairId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pairId = tag.hasUUID("PairId") ? tag.getUUID("PairId") : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CESGLang.forGoggles(tooltip, "cesg.barrel.pair", ChatFormatting.AQUA, pairCode());
        return true;
    }
}
