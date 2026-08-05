package com.zero.honorroot

class OverlayMounter {
    companion object { init { System.loadLibrary("honorroot") } }
    private external fun nativeMountModules(): Int

    fun mount(onLog: (String) -> Unit): Boolean {
        onLog("Mounting module overlays...")
        return if (nativeMountModules() == 0) { onLog("Module overlays active"); true }
        else { onLog("WARN: overlay partial"); false }
    }
}
