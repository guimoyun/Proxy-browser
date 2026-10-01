package com.shadowbrowser.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shadowbrowser.app.R
import com.shadowbrowser.app.browser.FingerprintEngine
import com.shadowbrowser.app.databinding.FragmentIdentityBinding

/**
 * 指纹伪装页（v2 / GeckoView）：
 *  - UA：随机 / 电脑模式 / 自定义（立即生效于当前与新建标签）；
 *  - 基线指纹保护（Firefox FPP）与禁用 WebRTC：写入引擎配置，重启应用后生效。
 * 诚实说明：完整指纹伪造（Canvas/WebGL/字体精确伪装）需改内核源码，本应用不做。
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
        b.switchWebrtc.setOnCheckedChangeListener { _, on ->
            fp.setWebrtcDisabled(on)
            Toast.makeText(requireContext(), "重启应用后生效", Toast.LENGTH_SHORT).show()
        }
        b.switchFpp.isChecked = fp.fppEnabled()
        b.switchFpp.setOnCheckedChangeListener { _, on ->
            fp.setFpp(on)
            Toast.makeText(requireContext(), "重启应用后生效", Toast.LENGTH_SHORT).show()
        }

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
    }

    private fun renderUa() {
        b.uaDisplay.text = fp.effectiveUserAgent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
