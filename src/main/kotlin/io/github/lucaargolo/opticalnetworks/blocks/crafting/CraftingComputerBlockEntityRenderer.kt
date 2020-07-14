package io.github.lucaargolo.opticalnetworks.blocks.crafting

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRackBlockEntity
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingMemory
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import java.util.*

class CraftingComputerBlockEntityRenderer(dispatcher: BlockEntityRenderDispatcher): BlockEntityRenderer<CraftingComputerBlockEntity>(dispatcher) {

    override fun render(entity: CraftingComputerBlockEntity, tickDelta: Float, matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, light: Int, overlay: Int) {
        val facing = entity.cachedState[Properties.HORIZONTAL_FACING]
        val inventory = entity.inventory

        (1..3).forEach {

            var state = "1"
            if(!entity.cachedState[Properties.ENABLED]) state = "off"
            if(Random().nextFloat() < 0.01f) state = "off"

            val modelManager = MinecraftClient.getInstance().bakedModelManager
            val backModel = modelManager.getModel(ModelIdentifier(Identifier(MOD_ID, "cd_back"), ""))
            val backQuads = backModel.getQuads(null, null, Random())

            val baseModel = modelManager.getModel(ModelIdentifier(Identifier(MOD_ID, "cd_base"), ""))
            val baseQuads = baseModel.getQuads(null, null, Random())

            val stateModel = modelManager.getModel(ModelIdentifier(Identifier(MOD_ID, "cd_$state"), ""))
            val stateQuads = stateModel.getQuads(null, null, Random())

            matrices.push()

            val d = if(inventory[it].item is CraftingMemory) 0.025 else 0.001

            if(facing == Direction.EAST) {
                if(it == 1) matrices.translate(1 + d, 5.0/16.0, 11.0/16.0)
                if(it == 2) matrices.translate(1 + d, 5.0/16.0, 6.5/16.0)
                if(it == 3) matrices.translate(1 + d, 5.0/16.0, 2.0/16.0)
                matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(270f))
            }

            if(facing == Direction.WEST) {
                if(it == 1) matrices.translate(-d, 5.0/16.0, 5.0/16.0)
                if(it == 2) matrices.translate(-d, 5.0/16.0, 9.5/16.0)
                if(it == 3) matrices.translate(-d, 5.0/16.0, 14.0/16.0)
                matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(90f))
            }

            if(facing == Direction.SOUTH) {
                if(it == 1) matrices.translate(5.0/16.0, 5.0/16.0, 1 + d)
                if(it == 2) matrices.translate(9.5/16.0, 5.0/16.0, 1 + d)
                if(it == 3) matrices.translate(14.0/16.0, 5.0/16.0, 1 + d)
                matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180f))
            }

            if(facing == Direction.NORTH) {
                if(it == 1) matrices.translate(11.0/16.0, 5.0/16.0, -d)
                if(it == 2) matrices.translate(6.5/16.0, 5.0/16.0, -d)
                if(it == 3) matrices.translate(2.0/16.0, 5.0/16.0, -d)
            }

            val matrixEntry = matrices.peek()
            val consumer = vertexConsumers.getBuffer(RenderLayer.getSolid())
            val lightFront = WorldRenderer.getLightmapCoordinates(entity.world, entity.pos.add(facing.vector))

            if(inventory[it].item is CraftingMemory) {
                baseQuads.forEach { quad ->
                    consumer.quad(matrixEntry, quad, 1f, 1f, 1f, lightFront, overlay)
                }
                stateQuads.forEach { quad ->
                    consumer.quad(matrixEntry, quad, 1f, 1f, 1f, if(!entity.cachedState[Properties.ENABLED]) lightFront else 15728880, overlay)
                }
            }else{
                backQuads.forEach { quad ->
                    consumer.quad(matrixEntry, quad, floatArrayOf(0.65f, 0.65f, 0.65f, 0.65f), 1f, 1f, 1f, intArrayOf(lightFront, lightFront, lightFront, lightFront), overlay, false)
                }
            }

            matrices.pop()

        }



    }

}