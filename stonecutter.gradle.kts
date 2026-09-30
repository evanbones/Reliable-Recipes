plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.141" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.141" apply false
    id("dev.kikugie.postprocess.jsonlang") version "2.1-beta.4" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.1" apply false
}

stonecutter active "26.2-fabric"

stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "forge", "neoforge")
    filters.include("**/*.fsh", "**/*.vsh")

    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")
    }

    replacements.regex(current.parsed < "1.21") {
        fun swap(from: String, to: String) = replace(Regex.escape(from), to, Regex.escape(to), from)
        val legacyNet = "com.evandev.reliable_recipes.networking.legacy"
        swap("import net.minecraft.network.protocol.common.custom.CustomPacketPayload;", "import $legacyNet.CustomPacketPayload;")
        swap("import net.minecraft.network.codec.StreamCodec;", "import $legacyNet.StreamCodec;")
        swap("import net.minecraft.network.RegistryFriendlyByteBuf;", "import $legacyNet.RegistryFriendlyByteBuf;")
        swap("import net.minecraft.world.item.crafting.RecipeHolder;", "import com.evandev.reliable_recipes.util.legacy.RecipeHolder;")
        swap("ItemStack.isSameItemSameComponents(", "ItemStack.isSameItemSameTags(")

        val idClass = if (current.parsed >= "1.21.11") "Identifier" else "ResourceLocation"
        val idCompat = "com.evandev.reliable_recipes.util.legacy.IdCompat"
        for (factory in listOf("fromNamespaceAndPath", "parse", "withDefaultNamespace")) {
            replace("\\b(?:ResourceLocation|Identifier)\\.$factory\\(", "$idCompat.$factory(", Regex.escape("$idCompat.$factory("), "$idClass.$factory(")
        }
    }
}

stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}
