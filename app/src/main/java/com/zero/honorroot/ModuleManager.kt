package com.zero.honorroot

import android.content.Context
import java.io.File
import java.util.zip.ZipFile

class ModuleManager(private val ctx: Context) {
    private val base = File("/data/adb/modules")

    data class Module(val id: String, val name: String, val version: String, val enabled: Boolean)

    fun list(): List<Module> = base.takeIf { it.exists() }
        ?.listFiles()?.filter { it.isDirectory }
        ?.map { d ->
            val p = props(File(d,"module.prop"))
            Module(d.name, p["name"]?:d.name, p["version"]?:"?", !File(d,"disable").exists())
        }?.sortedBy { it.name } ?: emptyList()

    fun install(zip: String, log: (String)->Unit) = runCatching {
        ZipFile(zip).use { z ->
            val id = z.getEntry("module.prop")?.let{
                parseProps(z.getInputStream(it).bufferedReader().readText())["id"]
            } ?: "mod_${System.currentTimeMillis()}"
            val dst = File(base,id).also{it.mkdirs()}
            log("Installing: $id")
            z.entries().asSequence().forEach { e ->
                val t = File(dst,e.name)
                if(e.isDirectory) t.mkdirs()
                else { t.parentFile?.mkdirs(); z.getInputStream(e).use{it.copyTo(t.outputStream())} }
            }
            log("Module '$id' ready — activates on next root")
        }; true
    }.getOrElse { log("Error: ${it.message}"); false }

    fun enable(id: String)  = File(base,"$id/disable").delete()
    fun disable(id: String) = File(base,"$id/disable").createNewFile()
    fun remove(id: String)  = File(base,id).deleteRecursively()

    private fun props(f: File) = if(f.exists()) parseProps(f.readText()) else emptyMap()
    private fun parseProps(t: String) = t.lines()
        .filter{'=' in it && !it.startsWith('#')}
        .associate{it.substringBefore('=').trim() to it.substringAfter('=').trim()}
}
