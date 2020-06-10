package io.github.lucaargolo.opticalnetworks.mixin;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotMixin {

    @SuppressWarnings("AccessorTarget")
    @Mutable @Accessor
    void setY(int y);

}
