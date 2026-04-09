package com.ail.brick.sample.anim

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ail.brick.sample.databinding.ActivityAnimDemoBinding
import com.ail.brick.ui.anim.*

/**
 * brick-ui 动画演示页面
 *
 * 展示功能：
 * - 淡入淡出（fadeIn / fadeOut）
 * - 滑入滑出（slideInFromBottom / slideOutToTop / slideInFromLeft / slideInFromRight）
 * - 缩放弹入弹出（scaleIn / scaleOut / pulse）
 * - 抖动 / 弹跳 / 旋转（shake / bounce / rotate）
 * - 组合动画（fadeSlideIn / fadeSlideOut）
 */
class AnimDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupButtons()
    }

    private fun setupButtons() {
        val target = binding.cardTarget

        // ---- 淡入淡出 ----
        binding.btnFadeIn.setOnClickListener {
            target.fadeIn(300L) { showResult("fadeIn 完成") }
        }
        binding.btnFadeOut.setOnClickListener {
            target.fadeOut(300L) { showResult("fadeOut 完成（GONE）") }
        }

        // ---- 滑入滑出 ----
        binding.btnSlideBottom.setOnClickListener {
            target.slideInFromBottom(300L, 200f) { showResult("slideInFromBottom 完成") }
        }
        binding.btnSlideTop.setOnClickListener {
            target.slideOutToTop(300L) { showResult("slideOutToTop 完成（GONE）") }
        }
        binding.btnSlideLeft.setOnClickListener {
            target.slideInFromLeft(300L, 200f) { showResult("slideInFromLeft 完成") }
        }
        binding.btnSlideRight.setOnClickListener {
            target.slideInFromRight(300L, 200f) { showResult("slideInFromRight 完成") }
        }

        // ---- 缩放 ----
        binding.btnScaleIn.setOnClickListener {
            target.scaleIn(300L) { showResult("scaleIn 完成（OvershootInterpolator）") }
        }
        binding.btnScaleOut.setOnClickListener {
            target.scaleOut(300L) { showResult("scaleOut 完成（GONE）") }
        }
        binding.btnPulse.setOnClickListener {
            target.pulse(1.2f, 400L) { showResult("pulse 完成（1.0 → 1.2 → 1.0）") }
        }

        // ---- 抖动/弹跳/旋转 ----
        binding.btnShake.setOnClickListener {
            target.shake { showResult("shake 完成（常用于表单校验）") }
        }
        binding.btnBounce.setOnClickListener {
            target.bounce { showResult("bounce 完成") }
        }
        binding.btnRotate.setOnClickListener {
            target.rotate(360f, 500L) { showResult("rotate 360° 完成") }
        }

        // ---- 组合动画 ----
        binding.btnFadeSlideIn.setOnClickListener {
            target.fadeSlideIn(400L) { showResult("fadeSlideIn 完成（淡入 + 上滑）") }
        }
        binding.btnFadeSlideOut.setOnClickListener {
            target.fadeSlideOut(400L) { showResult("fadeSlideOut 完成（淡出 + 下滑，GONE）") }
        }
    }

    private fun showResult(text: String) {
        binding.tvResult.text = text
    }
}
