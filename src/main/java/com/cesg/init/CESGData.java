package com.cesg.init;

import com.cesg.CESG;
import com.cesg.datagen.CESGLangProvider;
import com.cesg.datagen.CESGRecipeProvider;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public class CESGData {
    public static void gather(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new CESGLangProvider(output));
        generator.addProvider(event.includeServer(), new CESGRecipeProvider(output, lookup));
    }
}
