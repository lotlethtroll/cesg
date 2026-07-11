package com.cesg.potion;

import com.cesg.init.CESGEffects;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Teleportation effect: periodically blinks the holder to a random nearby spot (chorus-fruit style but
 * longer range). Respects Teleport Resistance — a resistant entity is never blinked.
 */
public class TeleportEffect extends MobEffect {
    private static final double RANGE = 24.0;

    public TeleportEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Base every 5s; higher amplifier blinks more often (down to ~1s).
        int interval = Math.max(20, 100 >> amplifier);
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide() && !entity.hasEffect(CESGEffects.TELEPORT_RESISTANCE))
            blink(entity);
        return true;
    }

    private static void blink(LivingEntity entity) {
        RandomSource random = entity.getRandom();
        double ox = entity.getX();
        double oy = entity.getY();
        double oz = entity.getZ();
        for (int attempt = 0; attempt < 16; attempt++) {
            double nx = ox + (random.nextDouble() - 0.5) * 2.0 * RANGE;
            double ny = Mth.clamp(oy + random.nextInt(16) - 8,
                    entity.level().getMinBuildHeight(), entity.level().getMaxBuildHeight() - 1);
            double nz = oz + (random.nextDouble() - 0.5) * 2.0 * RANGE;
            if (entity.isPassenger())
                entity.stopRiding();
            if (entity.randomTeleport(nx, ny, nz, true)) {
                entity.level().playSound(null, ox, oy, oz, SoundEvents.CHORUS_FRUIT_TELEPORT,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                entity.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                break;
            }
        }
    }
}
