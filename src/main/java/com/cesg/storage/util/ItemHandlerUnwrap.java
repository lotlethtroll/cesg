package com.cesg.storage.util;

import com.cesg.upgrades.EnhancedShulkerItemStackHandler;

import net.neoforged.neoforge.items.IItemHandler;

/** Walks common wrapper handlers to the underlying delegate. */
public final class ItemHandlerUnwrap {
    private ItemHandlerUnwrap() {}

    public static EnhancedShulkerItemStackHandler enhancedShulkerHandler(IItemHandler handler) {
        IItemHandler current = handler;
        while (current != null) {
            if (current instanceof EnhancedShulkerItemStackHandler enhanced)
                return enhanced;
            current = delegate(current);
        }
        return null;
    }

    private static IItemHandler delegate(IItemHandler handler) {
        if (handler instanceof NotifyingItemHandler notifying)
            return notifying.getDelegate();
        if (handler instanceof ShulkerContentsHandler contents)
            return contents.getDelegate();
        if (handler instanceof ShulkerExtractContentsHandler extract)
            return extract.getDelegate();
        return null;
    }
}
