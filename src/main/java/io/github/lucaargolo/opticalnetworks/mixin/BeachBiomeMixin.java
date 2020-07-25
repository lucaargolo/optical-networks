package io.github.lucaargolo.opticalnetworks.mixin;

import com.google.common.collect.ImmutableList;
import io.github.lucaargolo.opticalnetworks.worldgen.FeatureCompendiumKt;
import net.minecraft.world.biome.BeachBiome;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.decorator.CountExtraChanceDecoratorConfig;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.RandomFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeachBiome.class)
public class BeachBiomeMixin extends Biome {

    protected BeachBiomeMixin(Settings settings) {
        super(settings);
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    public void init(CallbackInfo info) {

        this.addFeature(
                GenerationStep.Feature.VEGETAL_DECORATION,
                Feature.RANDOM_SELECTOR.configure(new RandomFeatureConfig(
                        ImmutableList.of(),
                        FeatureCompendiumKt
                                .getBANANA_TREE_FEATURE_CONFIG()
                                .createDecoratedFeature(
                                        Decorator.COUNT_EXTRA_HEIGHTMAP.configure(
                                                new CountExtraChanceDecoratorConfig(0, 0.1F, 1)
                                        )
                                )
                )
        ));

    }
}
