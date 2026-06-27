package com.cesg.fluids;

import com.cesg.CESG;
import com.cesg.init.CESGFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import org.jetbrains.annotations.Nullable;

/**
 * Liquid Ender Pearl rejects living things the way water rejects endermen: a creature caught in the
 * fluid is teleported to nearby dry ground, so it can't be stood in. We search outward for the nearest
 * non-fluid foothold rather than teleporting at random — a plain random hop only escapes near an edge,
 * because vanilla refuses to land an entity in a fluid, so the middle of a large pool would never clear.
 */
@EventBusSubscriber(modid = CESG.MOD_ID)
public final class LiquidEnderPearlEffects {
    private static final String COOLDOWN_KEY = "cesg:ender_pearl_teleport";
    private static final int COOLDOWN_TICKS = 20;
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_UP = 5;
    private static final int SEARCH_DOWN = 3;
    private static final double FALLBACK_RANGE = 16.0;

    private LiquidEnderPearlEffects() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide || !(entity instanceof LivingEntity living) || entity instanceof EnderMan)
            return;
        if (!living.isInFluidType(CESGFluids.LIQUID_ENDER_PEARL.get().getFluidType()))
            return;

        long now = level.getGameTime();
        CompoundTag data = living.getPersistentData();
        if (now < data.getLong(COOLDOWN_KEY))
            return;
        data.putLong(COOLDOWN_KEY, now + COOLDOWN_TICKS);

        double fromX = living.getX();
        double fromY = living.getY();
        double fromZ = living.getZ();

        boolean teleported;
        BlockPos spot = findDrySpot(level, living.blockPosition());
        if (spot != null) {
            teleported = living.randomTeleport(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, true);
        } else {
            RandomSource random = living.getRandom();
            teleported = living.randomTeleport(fromX + (random.nextDouble() * 2.0 - 1.0) * FALLBACK_RANGE,
                    fromY, fromZ + (random.nextDouble() * 2.0 - 1.0) * FALLBACK_RANGE, true);
        }

        if (teleported) {
            level.playSound(null, fromX, fromY, fromZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            living.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
    }

    /** Nearest column (searched in expanding rings) where an entity can stand clear of the fluid. */
    @Nullable
    private static BlockPos findDrySpot(Level level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int ring = 1; ring <= SEARCH_RADIUS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring)
                        continue; // only the outer shell of this ring
                    for (int dy = SEARCH_UP; dy >= -SEARCH_DOWN; dy--) {
                        cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (isStandable(level, cursor))
                            return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static boolean isStandable(Level level, BlockPos feet) {
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty())
            return false;
        if (level.getBlockState(feet).blocksMotion() || level.getBlockState(feet.above()).blocksMotion())
            return false;
        BlockState ground = level.getBlockState(feet.below());
        return ground.blocksMotion() && level.getFluidState(feet.below()).isEmpty();
    }
}
