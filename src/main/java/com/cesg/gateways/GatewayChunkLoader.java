package com.cesg.gateways;

import com.cesg.CESG;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/**
 * Opt-in chunk loading for gateway pairs (per-core toggle in the destination picker). While active,
 * the core keeps its OWN chunk and the bound partner's chunk ticking, so Gateway Port transfers keep
 * flowing with nobody on either side. Tickets are block-owned and persist across restarts; the core
 * releases them on toggle-off, channel switch, rebind, and removal.
 */
public final class GatewayChunkLoader {
    private static final TicketController CONTROLLER = new TicketController(CESG.id("gateway"));

    private GatewayChunkLoader() {}

    public static void registerControllers(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /** Adds or removes the ticket pair: the owner core's own chunk + the target's chunk. */
    public static void setForced(MinecraftServer server, ServerLevel ownLevel, BlockPos owner,
            GlobalPos target, boolean add) {
        CONTROLLER.forceChunk(ownLevel, owner,
                SectionPos.blockToSectionCoord(owner.getX()),
                SectionPos.blockToSectionCoord(owner.getZ()), add, true);
        ServerLevel targetLevel = server.getLevel(target.dimension());
        if (targetLevel != null)
            CONTROLLER.forceChunk(targetLevel, owner,
                    SectionPos.blockToSectionCoord(target.pos().getX()),
                    SectionPos.blockToSectionCoord(target.pos().getZ()), add, true);
    }
}
