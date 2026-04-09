package com.ail.brick.arch.ext

import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Activity ViewBinding 属性委托。
 *
 * 使用 [Lazy] 延迟初始化，线程安全模式为 [LazyThreadSafetyMode.NONE]
 * （UI 线程单线程访问无需同步）。
 *
 * ```kotlin
 * class HomeActivity : AppCompatActivity() {
 *     private val binding by viewBinding(ActivityHomeBinding::inflate)
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(binding.root)
 *     }
 * }
 * ```
 */
inline fun <reified VB : ViewBinding> ComponentActivity.viewBinding(
    crossinline inflate: (LayoutInflater) -> VB
): Lazy<VB> = lazy(LazyThreadSafetyMode.NONE) {
    inflate(layoutInflater)
}

/**
 * Fragment ViewBinding 属性委托（自动在 onDestroyView 时释放 binding 引用）。
 *
 * ```kotlin
 * class HomeFragment : Fragment(R.layout.fragment_home) {
 *     private val binding by viewBinding(FragmentHomeBinding::bind)
 * }
 * ```
 */
fun <VB : ViewBinding> Fragment.viewBinding(
    bind: (View) -> VB
): ReadOnlyProperty<Fragment, VB> = FragmentViewBindingDelegate(bind)

private class FragmentViewBindingDelegate<VB : ViewBinding>(
    private val bind: (View) -> VB
) : ReadOnlyProperty<Fragment, VB> {

    private var binding: VB? = null
    private var observerRegistered = false

    override fun getValue(thisRef: Fragment, property: KProperty<*>): VB {
        binding?.let { return it }

        val view = thisRef.requireView()
        val newBinding = bind(view)
        binding = newBinding

        if (!observerRegistered) {
            observerRegistered = true
            thisRef.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    binding = null
                    observerRegistered = false
                }
            })
        }

        return newBinding
    }
}
