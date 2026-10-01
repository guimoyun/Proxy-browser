package com.shadowbrowser.app

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.shadowbrowser.app.proxy.ProxyManager
import com.shadowbrowser.app.ui.BrowserFragment
import java.io.File

/**
 * 唯一 Activity：承载浏览器主界面及其它功能页（节点/设置/书签/历史/标识/日志）。
 * 功能页通过 add + addToBackStack 压栈，返回键回到浏览器。
 * 若上次运行发生崩溃，启动时弹窗展示 crash.log，便于反馈与定位。
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showCrashReportIfAny()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, BrowserFragment(), "browser")
                .commit()
        }
    }

    private fun showCrashReportIfAny() {
        val crashFile = File(filesDir, ShadowApp.CRASH_LOG)
        if (!crashFile.exists()) return
        val text = try {
            crashFile.readText()
        } catch (_: Exception) {
            return
        }
        AlertDialog.Builder(this)
            .setTitle("上次运行崩溃")
            .setMessage(text.take(2500))
            .setPositiveButton("知道了") { _, _ -> crashFile.delete() }
            .setNegativeButton("复制给我") { _, _ ->
                val cm = getSystemService(android.content.ClipboardManager::class.java)
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("crash", text))
            }
            .setCancelable(true)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 退出时确保代理断开，避免残留进程
        ProxyManager.init(this).stop()
    }
}
