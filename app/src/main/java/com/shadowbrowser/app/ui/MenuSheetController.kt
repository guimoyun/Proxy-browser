package com.shadowbrowser.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.shadowbrowser.app.R

/**
 * 三页功能面板控制器（参考 Via 浏览器）。
 * sheet_menu.xml 内含 ViewPager2 + 分页指示点 + 电源/收起。
 * 每页 10 个菜单项（page_menu.xml 的 cell0..cell9），点击路由到 BrowserFragment 对应动作。
 */
object MenuSheetController {

    enum class Action {
        NIGHT, BOOKMARK, HISTORY, DOWNLOAD, INCOGNITO, SHARE, ADD_BOOKMARK, DESKTOP, TOOLBOX, SETTINGS,
        FIND, SAVE, OFFLINE, TRANSLATE, SOURCE, FULLSCREEN, IMAGE_MODE, SNIFF, IDENTITY, NETLOG,
        NODE, QR, HOME_SHORTCUT, TTS, AI, ORIENTATION, ADBLOCK, MARK_AD, FONTSIZE, REPORT
    }

    private data class Item(val icon: Int, val label: Int, val action: Action, val enabled: Boolean = true)

    private fun page0() = listOf(
        Item(R.drawable.ic_night, R.string.m_night, Action.NIGHT),
        Item(R.drawable.ic_bookmark, R.string.m_bookmark, Action.BOOKMARK),
        Item(R.drawable.ic_history, R.string.m_history, Action.HISTORY),
        Item(R.drawable.ic_download, R.string.m_download, Action.DOWNLOAD),
        Item(R.drawable.ic_incognito, R.string.m_incognito, Action.INCOGNITO),
        Item(R.drawable.ic_share, R.string.m_share, Action.SHARE),
        Item(R.drawable.ic_add_bookmark, R.string.m_add_bookmark, Action.ADD_BOOKMARK),
        Item(R.drawable.ic_desktop, R.string.m_desktop, Action.DESKTOP),
        Item(R.drawable.ic_toolbox, R.string.m_toolbox, Action.TOOLBOX),
        Item(R.drawable.ic_settings, R.string.m_settings, Action.SETTINGS)
    )

    private fun page1() = listOf(
        Item(R.drawable.ic_find, R.string.m_find, Action.FIND),
        Item(R.drawable.ic_save, R.string.m_save, Action.SAVE),
        Item(R.drawable.ic_offline, R.string.m_offline, Action.OFFLINE),
        Item(R.drawable.ic_translate, R.string.m_translate, Action.TRANSLATE),
        Item(R.drawable.ic_source, R.string.m_source, Action.SOURCE),
        Item(R.drawable.ic_fullscreen, R.string.m_fullscreen, Action.FULLSCREEN),
        Item(R.drawable.ic_image, R.string.m_image_mode, Action.IMAGE_MODE),
        Item(R.drawable.ic_sniff, R.string.m_sniff, Action.SNIFF),
        Item(R.drawable.ic_identity, R.string.m_identity, Action.IDENTITY),
        Item(R.drawable.ic_netlog, R.string.m_netlog, Action.NETLOG)
    )

    private fun page2() = listOf(
        Item(R.drawable.ic_node, R.string.m_proxy, Action.NODE),
        Item(R.drawable.ic_qr, R.string.m_qr, Action.QR),
        Item(R.drawable.ic_home_shortcut, R.string.m_home_shortcut, Action.HOME_SHORTCUT),
        Item(R.drawable.ic_tts, R.string.m_tts, Action.TTS),
        Item(R.drawable.ic_ai, R.string.m_ai, Action.AI),
        Item(R.drawable.ic_orientation, R.string.m_orientation, Action.ORIENTATION),
        Item(R.drawable.ic_adblock, R.string.m_adblock, Action.ADBLOCK),
        Item(R.drawable.ic_mark_ad, R.string.m_mark_ad, Action.MARK_AD),
        Item(R.drawable.ic_fontsize, R.string.m_fontsize, Action.FONTSIZE),
        Item(R.drawable.ic_report, R.string.m_report, Action.REPORT)
    )

    private val pages = listOf(page0(), page1(), page2())

    fun bind(
        sheet: View,
        frag: BrowserFragment,
        onClose: () -> Unit,
        onProxyToggle: () -> Unit,
        onOpenNodes: () -> Unit
    ) {
        val pager = sheet.findViewById<ViewPager2>(R.id.menuPager)
        val dots = sheet.findViewById<LinearLayout>(R.id.menuDots)
        pager.adapter = MenuPagerAdapter(frag, onProxyToggle, onOpenNodes, onClose)

        fun renderDots(pos: Int) {
            dots.removeAllViews()
            for (i in pages.indices) {
                val dot = View(sheet.context)
                val size = if (i == pos) 18 else 14
                dot.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = 12
                }
                dot.setBackgroundResource(if (i == pos) R.drawable.dot_active else R.drawable.dot_inactive)
                dots.addView(dot)
            }
        }
        renderDots(0)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = renderDots(position)
        })

        sheet.findViewById<View>(R.id.menuPower).setOnClickListener { onClose() }
        sheet.findViewById<View>(R.id.menuCollapse).setOnClickListener { onClose() }
    }

    private class MenuPagerAdapter(
        private val frag: BrowserFragment,
        private val onProxyToggle: () -> Unit,
        private val onOpenNodes: () -> Unit,
        private val onDismiss: () -> Unit
    ) : RecyclerView.Adapter<MenuPagerAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            // 直接引用 R.id（编译期检查），不用 getIdentifier 反射查找（部分 ROM 返回 0 会导致闪退）
            private val cellViews = arrayOf(
                v.findViewById<View>(R.id.cell0), v.findViewById<View>(R.id.cell1),
                v.findViewById<View>(R.id.cell2), v.findViewById<View>(R.id.cell3),
                v.findViewById<View>(R.id.cell4), v.findViewById<View>(R.id.cell5),
                v.findViewById<View>(R.id.cell6), v.findViewById<View>(R.id.cell7),
                v.findViewById<View>(R.id.cell8), v.findViewById<View>(R.id.cell9)
            )
            private val iconViews = arrayOf(
                v.findViewById<ImageView>(R.id.cellIcon0), v.findViewById<ImageView>(R.id.cellIcon1),
                v.findViewById<ImageView>(R.id.cellIcon2), v.findViewById<ImageView>(R.id.cellIcon3),
                v.findViewById<ImageView>(R.id.cellIcon4), v.findViewById<ImageView>(R.id.cellIcon5),
                v.findViewById<ImageView>(R.id.cellIcon6), v.findViewById<ImageView>(R.id.cellIcon7),
                v.findViewById<ImageView>(R.id.cellIcon8), v.findViewById<ImageView>(R.id.cellIcon9)
            )
            private val labelViews = arrayOf(
                v.findViewById<TextView>(R.id.cellLabel0), v.findViewById<TextView>(R.id.cellLabel1),
                v.findViewById<TextView>(R.id.cellLabel2), v.findViewById<TextView>(R.id.cellLabel3),
                v.findViewById<TextView>(R.id.cellLabel4), v.findViewById<TextView>(R.id.cellLabel5),
                v.findViewById<TextView>(R.id.cellLabel6), v.findViewById<TextView>(R.id.cellLabel7),
                v.findViewById<TextView>(R.id.cellLabel8), v.findViewById<TextView>(R.id.cellLabel9)
            )

            fun cell(i: Int): View = cellViews[i]
            fun icon(i: Int): ImageView = iconViews[i]
            fun label(i: Int): TextView = labelViews[i]
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.page_menu, parent, false)
            return VH(v)
        }

        override fun getItemCount() = pages.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val items = pages[position]
            items.forEachIndexed { i, item ->
                val cell = holder.cell(i)
                holder.icon(i).setImageResource(item.icon)
                holder.label(i).setText(item.label)
                cell.alpha = if (item.enabled) 1f else 0.35f
                cell.setOnClickListener {
                    if (!item.enabled) return@setOnClickListener
                    route(item.action)
                }
            }
        }

        private fun route(a: Action) {
            when (a) {
                Action.BOOKMARK, Action.HISTORY, Action.SETTINGS, Action.IDENTITY, Action.NETLOG,
                Action.FIND, Action.FULLSCREEN, Action.TOOLBOX, Action.FONTSIZE, Action.INCOGNITO,
                Action.TRANSLATE, Action.SOURCE, Action.SAVE, Action.OFFLINE, Action.IMAGE_MODE,
                Action.DESKTOP, Action.SHARE, Action.ADD_BOOKMARK, Action.TTS, Action.ADBLOCK,
                Action.ORIENTATION, Action.MARK_AD, Action.REPORT, Action.HOME_SHORTCUT,
                Action.QR, Action.AI, Action.SNIFF, Action.DOWNLOAD, Action.NIGHT ->
                    onDismiss()
                else -> Unit
            }
            when (a) {
                Action.BOOKMARK -> frag.openBookmarks()
                Action.HISTORY -> frag.openHistory()
                Action.SETTINGS -> frag.openSettings()
                Action.IDENTITY -> frag.openIdentity()
                Action.NETLOG -> frag.openNetLog()
                Action.NODE -> onOpenNodes()
                Action.FIND -> frag.showFindBarSafe()
                Action.FULLSCREEN -> frag.toggleFullscreenSafe()
                Action.TOOLBOX -> frag.openToolboxSafe()
                Action.TTS -> frag.readAloudSafe()
                Action.FONTSIZE -> frag.showFontDialogSafe()
                Action.INCOGNITO -> frag.toggleIncognito()
                Action.SHARE -> frag.shareCurrent()
                Action.ADD_BOOKMARK -> frag.addBookmark()
                Action.DESKTOP -> frag.toggleDesktopMode()
                Action.TRANSLATE -> frag.translatePage()
                Action.SOURCE -> frag.openViewSource()
                Action.SAVE -> frag.savePage()
                Action.OFFLINE -> frag.savePage()
                Action.IMAGE_MODE -> frag.toggleImageMode()
                Action.ADBLOCK -> frag.toggleAdBlock()
                Action.ORIENTATION -> frag.toggleOrientation()
                Action.NIGHT -> {
                    // 夜间模式：主题 DayNight，随系统切换；此处提示
                    Toast.makeText(frag.requireContext(), "夜间模式：请在系统设置切换深色主题", Toast.LENGTH_SHORT).show()
                }
                else -> Toast.makeText(frag.requireContext(), "该功能即将支持", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
