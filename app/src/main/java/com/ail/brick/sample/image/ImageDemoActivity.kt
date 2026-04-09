package com.ail.brick.sample.image

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ail.brick.image.BlurTransformation
import com.ail.brick.image.BorderTransformation
import com.ail.brick.image.ColorFilterTransformation
import com.ail.brick.image.GrayscaleTransformation
import com.ail.brick.image.loadCircle
import com.ail.brick.image.loadImage
import com.ail.brick.image.loadRounded
import com.ail.brick.sample.databinding.ActivityImageDemoBinding
import com.ail.brick.utils.dp

/**
 * brick-image 演示页面
 *
 * 展示图片加载、圆形/圆角、变换（灰度/滤镜/边框/模糊）及加载状态监听功能。
 */
class ImageDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageDemoBinding

    private val sampleUrl = "https://picsum.photos/800/600"
    private val avatarUrl = "https://picsum.photos/200"

    /** 当前变换模式，用于循环切换 */
    private var transformIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupButtons()
    }

    private fun setupButtons() {
        binding.btnLoadNormal.setOnClickListener {
            binding.ivNormal.loadImage(sampleUrl)
            showResult("加载普通图片")
        }

        binding.btnLoadShapes.setOnClickListener {
            binding.ivCircle.loadCircle(avatarUrl)
            binding.ivRounded.loadRounded(avatarUrl, radiusPx = 16f * resources.displayMetrics.density)
            showResult("圆形 + 圆角（16dp）图片")
        }

        binding.btnGrayscale.setOnClickListener {
            val transforms = listOf(
                "灰度" to listOf(GrayscaleTransformation()),
                "红色滤镜" to listOf(ColorFilterTransformation(0x66FF0000.toInt())),
                "边框" to listOf(BorderTransformation(2f.dp.toFloat(), Color.BLUE)),
                "模糊" to listOf(BlurTransformation()),
                "灰度 + 边框" to listOf(GrayscaleTransformation(), BorderTransformation(3f.dp.toFloat(), Color.RED)),
            )
            val (name, t) = transforms[transformIndex % transforms.size]
            binding.ivTransform.loadImage(sampleUrl) {
                transform(*t.toTypedArray())
                listener(
                    onStart = { showResult("变换: $name 加载中…") },
                    onSuccess = { showResult("变换: $name（点击切换）") },
                    onError = { showResult("变换: $name 加载失败") }
                )
            }
            transformIndex++
        }
    }

    private fun showResult(text: String) {
        binding.tvResult.text = text
    }
}
