package com.github.alexthe666.citadel.util;

import com.github.alexthe666.citadel.server.generation.CitadelSurfaceRuleWrapper;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.SurfaceRules;

public class CitadelSurfaceRules {

    public static final Codec<? extends SurfaceRules.RuleSource> CITADEL_WRAPPER =
            register(BuiltInRegistries.MATERIAL_RULE, "citadel:citadel_wrapper", CitadelSurfaceRuleWrapper.CODEC.codec());

    private static <T> T register(Registry<T> registry, String name, T value) {
        return Registry.register(registry, new ResourceLocation(name), value);
    }

    public static void registerAll() {
        // Innit class
    }
}
