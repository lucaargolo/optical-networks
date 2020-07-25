package io.github.lucaargolo.opticalnetworks.mixin;

import io.github.lucaargolo.opticalnetworks.worldgen.FeatureCompendiumKt;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TreeFeature.class)
public abstract class TreeFeatureMixin {

    @SuppressWarnings("ShadowModifiers")
    @Shadow
    static boolean isDirtOrGrass(TestableWorld world, BlockPos pos) {
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/TreeFeature;isDirtOrGrass(Lnet/minecraft/world/TestableWorld;Lnet/minecraft/util/math/BlockPos;)Z"), method = "generate(Lnet/minecraft/world/ModifiableTestableWorld;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Ljava/util/Set;Ljava/util/Set;Lnet/minecraft/util/math/BlockBox;Lnet/minecraft/world/gen/feature/TreeFeatureConfig;)Z")
    public boolean redirectedMethod(TestableWorld world, BlockPos pos) {
        if(((Object) this) == FeatureCompendiumKt.getBANANA_TREE_FEATURE()) {
            return world.testBlockState(pos, (state) -> {
                Block block = state.getBlock();
                return isDirtOrGrass(world, pos) || block == Blocks.SAND;
            });
        }
        return isDirtOrGrass(world, pos);
    }
}
