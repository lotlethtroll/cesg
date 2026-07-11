package com.cesg.farming;

import java.util.List;
import java.util.Optional;

import com.cesg.init.CESGBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public class ShulkerCageBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    /** Block radius a powered cage searches for a partner cage to fire on. */
    private static final int FIRE_RANGE = 4;

    /** Harvest cooldown (default matches vanilla shulker duplication, ~5s) — configurable (Phase 6C). */
    public static int cooldownTicks() {
        return com.cesg.CESGConfig.shulkerCageCooldown();
    }

    /** A ready cage only scans for a partner this often, to bound the neighbour search cost. */
    private static int fireAttemptInterval() {
        return com.cesg.CESGConfig.shulkerCageFireInterval();
    }

    @Nullable
    private CompoundTag trappedShulkerTag;
    private int cooldown;

    public ShulkerCageBlockEntity(BlockPos pos, BlockState state) {
        super(CESGBlockEntities.SHULKER_CAGE.get(), pos, state);
    }

    public ShulkerCageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean hasTrappedShulker() {
        return trappedShulkerTag != null && !trappedShulkerTag.isEmpty();
    }

    public int getCooldown() {
        return cooldown;
    }

    public Component trappedDisplayName() {
        return Component.translatable("entity.minecraft.shulker");
    }

    public boolean trapShulker(Shulker shulker) {
        if (level == null || hasTrappedShulker())
            return false;
        CompoundTag tag = new CompoundTag();
        // save() (not saveWithoutId) keeps the entity "id", which EntityType.create needs to rebuild the
        // shulker on release and to construct the client display entity.
        if (!shulker.save(tag))
            return false;
        trappedShulkerTag = tag;
        shulker.discard();
        setChanged();
        syncToClients();
        return true;
    }

    /** Dye color of the trapped shulker, or null for an undyed shulker / empty cage (for rendering). */
    @Nullable
    public DyeColor getTrappedColor() {
        if (trappedShulkerTag == null || !trappedShulkerTag.contains("Color"))
            return null;
        int id = trappedShulkerTag.getByte("Color");
        return id >= 0 && id <= 15 ? DyeColor.byId(id) : null;
    }

    public boolean releaseShulker() {
        if (level == null || !hasTrappedShulker())
            return false;
        Optional<Entity> created = EntityType.create(trappedShulkerTag, level);
        trappedShulkerTag = null;
        setChanged();
        syncToClients();
        if (created.isEmpty() || !(created.get() instanceof Shulker shulker))
            return false;
        shulker.moveTo(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5,
                shulker.getYRot(), shulker.getXRot());
        level.addFreshEntity(shulker);
        return true;
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** An external shulker bullet (e.g. from a free shulker) striking the cage also yields a shell. */
    public void onProjectileHit(Projectile projectile) {
        if (!(projectile instanceof ShulkerBullet) || level == null || level.isClientSide)
            return;
        produceShell();
    }

    /**
     * Drops a single (vanilla) shulker shell if this cage holds a shulker, is ready, and conditions are
     * met. Shared by external bullet hits and the powered paired-cage loop. Returns true on success.
     */
    public boolean produceShell() {
        if (level == null || level.isClientSide || !hasTrappedShulker() || cooldown > 0)
            return false;
        if (!level.dimension().equals(Level.END) || level.getDifficulty() == Difficulty.PEACEFUL)
            return false;

        ItemStack drop = new ItemStack(Items.SHULKER_SHELL);
        ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, drop);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);

        if (level instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(ParticleTypes.PORTAL, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, worldPosition, SoundEvents.SHULKER_HURT, SoundSource.HOSTILE, 0.8F, 1.0F);
        cooldown = cooldownTicks();
        setChanged();
        return true;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (trappedShulkerTag != null)
            tag.put("TrappedShulker", trappedShulkerTag.copy());
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        trappedShulkerTag = tag.contains("TrappedShulker") ? tag.getCompound("TrappedShulker").copy() : null;
        cooldown = tag.getInt("Cooldown");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShulkerCageBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
            return;
        }
        if (level.getGameTime() % fireAttemptInterval() == 0)
            be.tryPoweredFire(level, pos);
    }

    /**
     * Powered paired-cage farm: when this cage holds a shulker, is redstone-powered, and sits within
     * range of another cage that also holds a shulker, the trapped shulker "fires" on its neighbour and
     * knocks a shell loose from it (the End / difficulty checks live in {@link #produceShell()}).
     */
    private void tryPoweredFire(Level level, BlockPos pos) {
        if (!hasTrappedShulker() || !level.hasNeighborSignal(pos))
            return;
        if (!level.dimension().equals(Level.END) || level.getDifficulty() == Difficulty.PEACEFUL)
            return;

        ShulkerCageBlockEntity target = findTargetCage(level, pos);
        if (target == null || !target.produceShell())
            return;

        level.playSound(null, pos, SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 0.7F, 1.0F);
        cooldown = cooldownTicks();
        setChanged();
    }

    @Nullable
    private ShulkerCageBlockEntity findTargetCage(Level level, BlockPos pos) {
        ShulkerCageBlockEntity nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-FIRE_RANGE, -FIRE_RANGE, -FIRE_RANGE),
                pos.offset(FIRE_RANGE, FIRE_RANGE, FIRE_RANGE))) {
            if (candidate.equals(pos))
                continue;
            if (level.getBlockEntity(candidate) instanceof ShulkerCageBlockEntity other
                    && other.hasTrappedShulker() && other.cooldown == 0) {
                double distSqr = pos.distSqr(candidate);
                if (distSqr < nearestDistSqr) {
                    nearestDistSqr = distSqr;
                    nearest = other;
                }
            }
        }
        return nearest;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ShulkerCageGoggleTooltip.append(this, tooltip);
        return true;
    }
}
