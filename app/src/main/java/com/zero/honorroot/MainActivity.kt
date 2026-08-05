package com.zero.honorroot

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var btnRoot   : Button
    private lateinit var tvLog     : TextView
    private lateinit var svLog     : ScrollView
    private lateinit var lvMods    : ListView
    private lateinit var btnAddMod : Button
    private lateinit var tvState   : TextView

    private val exploit by lazy { ExploitEngine(this) }
    private val su      by lazy { SuInstaller(this) }
    private val overlay by lazy { OverlayMounter() }
    private val mods    by lazy { ModuleManager(this) }
    private val exec    = Executors.newSingleThreadExecutor()
    private val main    = Handler(Looper.getMainLooper())

    companion object { private const val PICK = 42 }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        btnRoot   = findViewById(R.id.btn_root)
        tvLog     = findViewById(R.id.tv_log)
        svLog     = findViewById(R.id.sv_log)
        lvMods    = findViewById(R.id.lv_mods)
        btnAddMod = findViewById(R.id.btn_add_mod)
        tvState   = findViewById(R.id.tv_state)

        refreshState(); refreshMods()

        btnRoot.setOnClickListener {
            btnRoot.isEnabled = false
            tvLog.text = ""
            exec.submit { doRoot() }
        }
        btnAddMod.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/zip" }, PICK)
        }
        lvMods.setOnItemLongClickListener { _, _, pos, _ ->
            val m = mods.list().getOrNull(pos) ?: return@setOnItemLongClickListener true
            AlertDialog.Builder(this)
                .setTitle(m.name)
                .setItems(arrayOf("Enable","Disable","Remove")) { _, w ->
                    when(w) { 0->mods.enable(m.id); 1->mods.disable(m.id); 2->mods.remove(m.id) }
                    refreshMods()
                }.show(); true
        }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == PICK && res == RESULT_OK) {
            val uri = data?.data ?: return
            val tmp = File(cacheDir,"mod_${System.currentTimeMillis()}.zip")
            contentResolver.openInputStream(uri)?.use { it.copyTo(tmp.outputStream()) }
            exec.submit { mods.install(tmp.absolutePath){log(it)}; main.post{refreshMods()} }
        }
    }

    private fun doRoot() {
        val ok = exploit.run { log(it) }
        if (!ok) { main.post { btnRoot.isEnabled=true; refreshState() }; return }
        su.install { log(it) }
        overlay.mount { log(it) }
        log("═══ SESSION ACTIVE ═══")
        log("Temp root — lost on reboot")
        log("Modules persist — re-run to reload")
        main.post { btnRoot.isEnabled=true; btnRoot.text="RE-ROOT"; refreshState(); refreshMods() }
    }

    private fun refreshState() {
        val r = try { exploit.isRooted() } catch(e: Throwable) { false }
        tvState.text = if(r) "● ROOTED" else "● NOT ROOTED"
        tvState.setTextColor(getColor(if(r) R.color.green else R.color.red))
    }

    private fun refreshMods() {
        val items = try { mods.list().map { "${it.name}  v${it.version}  [${if(it.enabled)"ON" else "OFF"}]" } }
                    catch(e: Throwable) { listOf("(no modules)") }
        lvMods.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun log(m: String) = main.post {
        tvLog.append("$m\n")
        svLog.post { svLog.fullScroll(View.FOCUS_DOWN) }
    }
}
