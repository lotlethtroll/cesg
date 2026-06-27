package com.cesg.upgrades;

/** Menu/screen geometry shared between {@link EnhancedShulkerMenu} and {@link EnhancedShulkerScreen}. */
public final class EnhancedShulkerGuiLayout {
    public static final int SLOT = 18;
    public static final int STORAGE_TOP = 18;
    public static final int STORAGE_TO_PLAYER_GAP = 14;
    public static final int BOTTOM_BORDER = 7;
    /** Storage never grows past six rows; extra capacity widens the grid instead (Sophisticated-Backpacks style). */
    public static final int MAX_STORAGE_ROWS = 6;
    public static final int PLAYER_COLUMNS = 9;
    /** Title bar + player section + bottom border — fixed overhead regardless of storage rows. */
    private static final int PLAYER_BAND_HEIGHT = 114;

    private EnhancedShulkerGuiLayout() {}

    /** Columns needed to hold {@code slots} within {@link #MAX_STORAGE_ROWS}, never narrower than the player grid. */
    public static int columns(int slots) {
        int needed = (slots + MAX_STORAGE_ROWS - 1) / MAX_STORAGE_ROWS;
        return Math.max(PLAYER_COLUMNS, needed);
    }

    public static int rows(int slots) {
        int columns = columns(slots);
        return (slots + columns - 1) / columns;
    }

    public static int storageGridHeight(int rows) {
        return SLOT * rows;
    }

    public static int playerInventoryTopY(int rows) {
        return STORAGE_TOP + storageGridHeight(rows) + STORAGE_TO_PLAYER_GAP;
    }

    public static int hotbarY(int rows) {
        return playerInventoryTopY(rows) + 58;
    }

    public static int imageHeight(int rows) {
        return PLAYER_BAND_HEIGHT + storageGridHeight(rows);
    }
}
