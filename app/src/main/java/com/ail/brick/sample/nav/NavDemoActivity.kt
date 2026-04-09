package com.ail.brick.sample.nav

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ail.brick.arch.nav.BrickNav
import com.ail.brick.sample.R
import com.ail.brick.sample.databinding.ActivityNavDemoBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NavDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavDemoBinding
    private lateinit var nav: BrickNav

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            if (!nav.back()) finish()
        }

        nav = BrickNav.init(this, R.id.navContainer)
            .register<NavHomeFragment>("home")
            .register<NavProfileFragment>("profile")
            .register<NavSettingsFragment>("settings")
            .addInterceptor { from, to, _ ->
                Log.d("BrickNav", "navigate: $from → $to")
                updateStackInfo()
                true
            }

        if (savedInstanceState == null) {
            nav.navigate("home") { addToBackStack = false }
        }

        // 监听返回栈变化来更新 UI
        supportFragmentManager.addOnBackStackChangedListener {
            updateStackInfo()
        }
        binding.root.post { updateStackInfo() }
    }

    internal fun updateStackInfo() {
        binding.tvStackInfo.text = "当前路由: ${nav.currentRoute ?: "-"} | 栈深度: ${nav.stackDepth}"
    }
}
