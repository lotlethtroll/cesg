package com.cesg.gateways;

import java.util.List;

import com.cesg.gateways.teleport.GatewayPartner;
import com.cesg.gateways.teleport.GatewaySideState;
import com.cesg.init.CESGBlockEntities;
import com.cesg.util.CESGLang;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CrossDimensionalGatewayCoreBlockEntity extends KineticBlockEntity {
    public static final int TANK_CAPACITY = 4000;
    public static final int TRAVEL_COST = 250;
    public static final int ESSENCE_FUEL_MB = 500;
    public static final int LIQUID_EYE_FUEL_MB = 1000;

    private GatewayPartner partner = GatewayPartner.EMPTY;
    private int fuelMb;

    public CrossDimensionalGatewayCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CrossDimensionalGatewayCoreBlockEntity(BlockPos pos, BlockState state) {
        this(CESGBlockEntities.CROSS_DIMENSIONAL_GATEWAY_CORE.get(), pos, state);
    }

    public GatewayPartner getPartner() {
        return partner;
    }

    public void setPartner(GatewayPartner partner) {
        this.partner = partner;
        setChanged();
    }

    public boolean addFuel(int amount) {
        int before = fuelMb;
        fuelMb = Math.min(TANK_CAPACITY, fuelMb + amount);
        if (fuelMb != before)
            setChanged();
        return fuelMb > before;
    }

    public boolean canTravel() {
        return getTravelBlockReason() == null;
    }

    /** @return translation key suffix for cesg.gateway.*, or null when travel is allowed */
    public String getTravelBlockReason() {
        if (getSpeed() == 0)
            return "unpowered";
        if (!partner.isBound())
            return "unbound";
        if (fuelMb < TRAVEL_COST)
            return "need_fuel";
        return null;
    }

    public boolean consumeFuel() {
        if (fuelMb < TRAVEL_COST)
            return false;
        fuelMb -= TRAVEL_COST;
        setChanged();
        return true;
    }

    public GatewaySideState createSideState() {
        return new GatewaySideState(level.dimension(), worldPosition, getSpeed() != 0, fuelMb >= TRAVEL_COST,
                level.dimension().equals(Level.END));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Partner", partner.save());
        tag.putInt("Fuel", fuelMb);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        partner = GatewayPartner.load(tag.getCompound("Partner"));
        fuelMb = tag.getInt("Fuel");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.fuel", ChatFormatting.AQUA, fuelMb, TANK_CAPACITY);
        if (getSpeed() == 0)
            CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.unpowered", ChatFormatting.GRAY);
        if (partner.isBound())
            CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.bound", ChatFormatting.GREEN);
        else
            CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.unbound", ChatFormatting.GRAY);
        CESGLang.forGoggles(tooltip, "cesg.goggles.gateway.travel_cost", ChatFormatting.WHITE, TRAVEL_COST);
        return true;
    }
}
