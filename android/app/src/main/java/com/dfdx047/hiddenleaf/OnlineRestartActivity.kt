package com.dfdx047.hiddenleaf

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.dfdx047.hiddenleaf.engine.GameActivity

/**
 * The bridge from the game's own online menu (engine/OnlineMenu.kt) into the match: the game's process wrote the session file
 * and goes away; this activity, in the launcher's process and with nothing on screen but black, waits until that process is gone
 * and opens the game again, which then starts in step with the other phone (the engine reads the session file at its start).
 */
class OnlineRestartActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var tries = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler.post(wait)
    }

    private val wait = object : Runnable {
        override fun run() {
            val gameAlive = runCatching {
                getSystemService(ActivityManager::class.java).runningAppProcesses.orEmpty().any { it.processName == "$packageName:game" }
            }.getOrDefault(false)
            // (a few seconds at most: if the old process lingers, the new start still goes)
            if (gameAlive && ++tries < 40) {
                handler.postDelayed(this, 100)
                return
            }
            startActivity(Intent(this@OnlineRestartActivity, GameActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
