package io.github.lucaargolo.opticalnetworks.mixin;

import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalConfig;
import io.github.lucaargolo.opticalnetworks.mixed.ServerPlayerEntityMixed;
import io.github.lucaargolo.opticalnetworks.packets.PacketCompendiumKt;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.impl.networking.ServerSidePacketRegistryImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements ServerPlayerEntityMixed {

    @Inject(method = "writeCustomDataToTag", at = @At("RETURN"))
    private void writeAbilitiesToTag(CompoundTag tag, CallbackInfo ci) {
        tag.put("opticalnetworks:terminal_config", opticalNetworks$terminalConfig.toTag(new CompoundTag()));
    }

    @Inject(method = "readCustomDataFromTag", at = @At("RETURN"))
    private void readAbilitiesFromTag(CompoundTag tag, CallbackInfo ci) {
        CompoundTag tcTag = tag.getCompound("opticalnetworks:terminal_config");
        if(tcTag != null) {
            opticalNetworks$terminalConfig.fromTag(tcTag);
        }
    }

    @Override
    public TerminalConfig getOpticalNetworks$terminalConfig() {
        return opticalNetworks$terminalConfig;
    }
}
