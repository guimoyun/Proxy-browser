package com.shadowbrowser.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shadowbrowser.app.proxy.ProxyManager
import com.shadowbrowser.app.ui.BrowserFragment

/**
 * 唯一 Activity：承载浏览器主界面及其它功能页（节点/设置/书签/历史/标识/日志）。
 * 功能页通过 replace + addToBackStack 压栈，返回键回到浏览器。
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, BrowserFragment(), "browser")
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 退出时确保代理断开，避免残留进程
        ProxyManager.init(this).stop()
    }
}
