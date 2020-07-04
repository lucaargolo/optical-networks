package io.github.lucaargolo.opticalnetworks.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

class IngredientHelper(json: String) {

    enum class Type {
        TAG,
        ITEM,
        NOTHING
    }

    var type: Type = Type.NOTHING
    var item: Item? = null
    var tag: String? = null

    init {
        val inputJson: JsonObject? = try { JsonParser().parse(json).asJsonObject } catch (ignored: Exception) { null }
        inputJson?.let { jsonObject ->
            val mcItem: String? = try { jsonObject.get("item").asString } catch (ignored: Exception) { null }
            val mcTag: String? = try { jsonObject.get("tag").asString } catch (ignored: Exception) { null }
            mcItem?.let { str -> item = Registry.ITEM.get(Identifier(str)) }
            mcTag?.let { str -> tag = str}
            if(item != null) type = Type.ITEM
            if(tag != null) type = Type.TAG
        }
    }

}