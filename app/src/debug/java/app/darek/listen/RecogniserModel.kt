package app.darek.listen

import android.content.Context
import android.content.res.AssetManager
import java.io.File

/**
 * Copies the recogniser's model out of the APK into `noBackupFilesDir` (ADR-0009). A directory with
 * the final name is complete, because it is only ever made by renaming a finished copy.
 */
object RecogniserModel {
    const val NAME = "vosk-model-small-pl-0.22"
    private const val PREFIX = "vosk-model-"

    fun unpack(context: Context): File {
        val root = context.noBackupFilesDir
        val model = File(root, NAME)
        if (model.isDirectory) return model
        root.listFiles()?.filter { it.name.startsWith(PREFIX) }?.forEach { it.deleteRecursively() }
        val partial = File(root, "$NAME.tmp")
        copy(context.assets, "vosk/$NAME", partial)
        check(partial.renameTo(model)) { "could not rename $partial to $model" }
        return model
    }

    private fun copy(
        assets: AssetManager,
        path: String,
        into: File,
    ) {
        val children = assets.list(path).orEmpty()
        if (children.isEmpty()) {
            into.parentFile?.mkdirs()
            assets.open(path).use { input -> into.outputStream().use { input.copyTo(it) } }
        } else {
            into.mkdirs()
            children.forEach { copy(assets, "$path/$it", File(into, it)) }
        }
    }
}
