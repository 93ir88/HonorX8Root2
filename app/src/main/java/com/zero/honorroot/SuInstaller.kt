package com.zero.honorroot

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class SuInstaller(private val ctx: Context) {
    companion object { init { System.loadLibrary("honorroot") } }
    private external fun nativeInstall(p: String): Int

    fun install(onLog: (String) -> Unit): Boolean {
        onLog("Extracting su binary...")
        val f = File(ctx.filesDir, "su_bin")
        ctx.assets.open("su").use { s -> FileOutputStream(f).use { s.copyTo(it) } }
        f.setExecutable(true, false)
        onLog("Bind-mounting su (temp/RAM)...")
        return when (val r = nativeInstall(f.absolutePath)) {
            0    -> { onLog("su installed at /system/bin/su"); true }
            -1   -> { onLog("ERROR: stage failed ($r)"); false }
            -2   -> { onLog("ERROR: bind-mount failed ($r)"); false }
            else -> { onLog("ERROR: code $r"); false }
        }
    }
}
