package com.ail.brick.sample.nav

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ail.brick.arch.nav.BrickNav
import com.ail.brick.arch.nav.NavAnim
import com.ail.brick.sample.R
import com.google.android.material.button.MaterialButton

// ── 首页 ──────────────────────────────────────────

class NavHomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_nav_demo_page, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvPageTitle).text = "首页"
        view.findViewById<TextView>(R.id.tvPageDesc).text = "BrickNav 零 XML 导航演示 — 从这里出发"
        val container = view.findViewById<LinearLayout>(R.id.buttonContainer)

        addButton(container, "去个人页 → 水平滑动（默认）") {
            BrickNav.from(this).navigate("profile")
        }
        addButton(container, "去设置页 → 垂直弹出") {
            BrickNav.from(this).navigate("settings") { anim = NavAnim.SLIDE_VERTICAL }
        }
        addButton(container, "去个人页 → 淡入淡出") {
            BrickNav.from(this).navigate("profile") { anim = NavAnim.FADE }
        }
        addButton(container, "去设置页 → 无动画") {
            BrickNav.from(this).navigate("settings") { anim = NavAnim.NONE }
        }
    }
}

// ── 个人页 ──────────────────────────────────────────

class NavProfileFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_nav_demo_page, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvPageTitle).text = "个人页"
        view.findViewById<TextView>(R.id.tvPageDesc).text = "第二层页面 — 可以继续向下或返回"
        val container = view.findViewById<LinearLayout>(R.id.buttonContainer)

        addButton(container, "继续去设置页 →") {
            BrickNav.from(this).navigate("settings")
        }
        addButton(container, "再压一个个人页 →") {
            BrickNav.from(this).navigate("profile")
        }
        addButton(container, "再去个人页 (SingleTop，栈顶不重复)") {
            BrickNav.from(this).navigate("profile") { singleTop = true }
        }
        addButton(container, "← 返回上一页") {
            BrickNav.from(this).back()
        }
    }
}

// ── 设置页 ──────────────────────────────────────────

class NavSettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_nav_demo_page, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvPageTitle).text = "设置页"
        view.findViewById<TextView>(R.id.tvPageDesc).text = "最深层页面 — 演示跳回和清栈"
        val container = view.findViewById<LinearLayout>(R.id.buttonContainer)

        addButton(container, "跳回个人页 (backTo)") {
            BrickNav.from(this).backTo("profile")
        }
        addButton(container, "清空返回栈 → 回首页") {
            BrickNav.from(this).clearStack()
        }
        addButton(container, "← 返回上一页") {
            BrickNav.from(this).back()
        }
    }
}

// ── 工具函数 ──────────────────────────────────────

private fun Fragment.addButton(container: LinearLayout, text: String, onClick: () -> Unit) {
    val button = MaterialButton(requireContext()).apply {
        this.text = text
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = 12 }
        setOnClickListener { onClick() }
    }
    container.addView(button)
}
