package com.shadowbrowser.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shadowbrowser.app.R
import com.shadowbrowser.app.browser.FingerprintEngine
import com.shadowbrowser.app.databinding.FragmentIdentityBinding

/**
 * 指纹伪装页：UA（随机/电脑/自定义）、时区偏移、禁用 WebRTC。
 * 诚实说明：标准 WebView 仅能改这些可观测项，深度指纹需 Chromium 内核。
 */
class IdentityFragment : Fragment() {

    private var _b: FragmentIdentityBinding? = null
    private val b get() = _b!!
    private lateinit var fp: FingerprintEngine

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentIdentityBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fp = FingerprintEngine(requireContext())
        b.toolbarBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.toolbarTitle.text = getString(R.string.m_identity)

        renderUa()
        b.switchRandom.isChecked = fp.randomUaEnabled()
        b.switchRandom.setOnCheckedChangeListener { _, on ->
            fp.setRandomUa(on)
            if (on) fp.setDesktopMode(false)
            renderUa()
        }
        b.switchDesktop.isChecked = fp.desktopMode()
        b.switchDesktop.setOnCheckedChangeListener { _, on ->
            fp.setDesktopMode(on)
            if (on) fp.setRandomUa(false)
            renderUa()
        }
        b.switchWebrtc.isChecked = fp.webrtcDisabled()
        b.switchWebrtc.setOnCheckedChangeListener { _, on -> fp.setWebrtcDisabled(on) }

        b.btnCustomUa.setOnClickListener {
            val input = android.widget.EditText(requireContext()).apply {
                setText(fp.customUa()); setSingleLine(false)
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.custom_ua))
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定") { _, _ ->
                    fp.setCustomUa(input.text.toString().trim())
                    renderUa()
                }
                .show()
        }
        b.btnTz.setOnClickListener {
            val input = android.widget.EditText(requireContext()).apply {
                setHint("相对 UTC 的偏移分钟，如 480 表示 UTC+8")
                setText(fp.tzOffsetMinutes().toString())
                setSingleLine(true)
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.fake_timezone))
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定") { _, _ ->
                    val m = input.text.toString().toIntOrNull()
                    if (m != null) fp.setTzOffsetMinutes(m)
                }
                .show()
        }
    }

    private fun renderUa() {
        b.uaDisplay.text = fp.effectiveUserAgent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
