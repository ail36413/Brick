# brick-arch consumer ProGuard rules

# BaseViewModel 及其子类通过反射创建
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# FlowEventBus 使用 class name 作为 key
-keep class com.ail.brick.arch.event.FlowEventBus { *; }

# MVI 标记接口
-keep interface com.ail.brick.arch.mvi.UiState
-keep interface com.ail.brick.arch.mvi.UiEvent
-keep interface com.ail.brick.arch.mvi.UiIntent
-keep class * implements com.ail.brick.arch.mvi.UiState { *; }
-keep class * implements com.ail.brick.arch.mvi.UiEvent { *; }
-keep class * implements com.ail.brick.arch.mvi.UiIntent { *; }

# Kotlin 元数据
-keepattributes *Annotation*
-keepattributes Signature
