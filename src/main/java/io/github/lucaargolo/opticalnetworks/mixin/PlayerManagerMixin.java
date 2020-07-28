package io.github.lucaargolo.opticalnetworks.mixin;

import io.github.lucaargolo.opticalnetworks.mixed.ServerPlayerEntityMixed;
import io.github.lucaargolo.opticalnetworks.packets.PacketCompendiumKt;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(at = @At("TAIL"), method = "onPlayerConnect")
    public void onPlayerConnect(ClientConnection connection, ServerPlayerEntity playerEntity, CallbackInfo info) {
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
        passedData.writeCompoundTag(((ServerPlayerEntityMixed) playerEntity).getOpticalNetworks$terminalConfig().toTag(new CompoundTag()));
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerEntity, PacketCompendiumKt.getUPDATE_TERMINAL_CONFIG_S2C_PACKET(), passedData);
    }

}
