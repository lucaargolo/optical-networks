package io.github.lucaargolo.opticalnetworks.blocks.drive_rack

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.items.basic.DiscDrive
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.model.BakedModelManager
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.state.property.DirectionProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import java.util.*

class DriveRackBlockEntityRenderer(dispatcher: BlockEntityRenderDispatcher): BlockEntityRenderer<DriveRackBlockEntity>(dispatcher) {

    override fun render(entity: DriveRackBlockEntity, tickDelta: Float, matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) {
        val facing = entity.cachedState[Properties.HORIZONTAL_FACING]
        val inventory = entity.inventory

        inventory.forEachIndexed { i, it ->
            if(it.item is DiscDrive) {
                val stackTag = it.orCreateTag
                var usedSpace = 0
                if(stackTag.contains("items")) {
                    val itemsTag = stackTag.get("items") as ListTag
                    itemsTag.forEach { tag ->
                        usedSpace += NetworkState.getStackFromTag(tag as CompoundTag).count
                    }
                }
                var state = when(usedSpace.toFloat()/(it.item as DiscDrive).bytes.toFloat()) {
                    1f -> "3"
                    in (0f..0.7f) -> "1"
                    in (0.7f..1f) -> "2"
                    else -> "off"
                }
                if(Random().nextFloat() < 0.01f) state = "off"
                val modelManager = MinecraftClient.getInstance().bakedModelManager
                val baseModel = modelManager.getModel(ModelIdentifier(Identifier(MOD_ID, "cd_base"), ""))
                val baseQuads = baseModel.getQuads(null, null, Random())
                val stateModel = modelManager.getModel(ModelIdentifier(Identifier(MOD_ID, "cd_$state"), ""))

                val stateQuads = stateModel.getQuads(null, null, Random())

                matrices.push()

                if(facing == Direction.EAST) {
                    matrices.translate(1.01, 0.8125-(0.1875*(i/2)), 0.8125+(if(i%2 == 0) -0.1875 else -0.625))
                    matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(270f))
                }

                if(facing == Direction.WEST) {
                    matrices.translate(-0.01, 0.8125-(0.1875*(i/2)), 0.375+(if(i%2 != 0) 0.4375 else 0.0))
                    matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(90f))
                }

                if(facing == Direction.SOUTH) {
                    matrices.translate(0.375+(if(i%2 != 0) 0.4375 else 0.0), 0.8125-(0.1875*(i/2)), 1.01)
                    matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180f))
                }

                if(facing == Direction.NORTH) {
                    matrices.translate(0.8125+(if(i%2 == 0) -0.1875 else -0.625), 0.8125-(0.1875*(i/2)), -0.01)
                }

                val matrixEntry = matrices.peek()
                val consumer = vertexConsumers.getBuffer(RenderLayer.getCutoutMipped())
                val lightFront = WorldRenderer.getLightmapCoordinates(entity.world, entity.pos.add(facing.vector))
                baseQuads.forEach {
                    consumer.quad(matrixEntry, it, 1f, 1f, 1f, lightFront, overlay)
                }
                stateQuads.forEach {
                    consumer.quad(matrixEntry, it, 1f, 1f, 1f, 15728880, overlay)
                }

                matrices.pop()
            }

        }



    }

}