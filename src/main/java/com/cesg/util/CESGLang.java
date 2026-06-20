package com.cesg.util;

import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class CESGLang {
    private CESGLang() {}

    public static LangBuilder translate(String key, Object... args) {
        return CreateLang.builder().add(Component.translatable(key, args));
    }

    public static void forGoggles(List<Component> tooltip, String key, Object... args) {
        translate(key, args).forGoggles(tooltip);
    }

    public static void forGoggles(List<Component> tooltip, String key, ChatFormatting style, Object... args) {
        translate(key, args).style(style).forGoggles(tooltip);
    }
}
