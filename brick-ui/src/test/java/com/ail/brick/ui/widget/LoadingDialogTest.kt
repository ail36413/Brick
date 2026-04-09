package com.ail.brick.ui.widget

import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LoadingDialogTest {

    private lateinit var activity: AppCompatActivity

    @Before
    fun setup() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        activity = controller.get()
        activity.setTheme(com.google.android.material.R.style.Theme_Material3_DayNight)
        controller.create().start().resume()
    }

    @Test
    fun `show returns a dialog`() {
        val dialog = LoadingDialog.show(activity, "Loading…")
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
        LoadingDialog.dismiss()
    }

    @Test
    fun `dismiss closes dialog`() {
        val dialog = LoadingDialog.show(activity)
        assertTrue(dialog.isShowing)
        LoadingDialog.dismiss()
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `show twice dismisses previous dialog`() {
        val first = LoadingDialog.show(activity, "First")
        val second = LoadingDialog.show(activity, "Second")
        assertFalse(first.isShowing)
        assertTrue(second.isShowing)
        LoadingDialog.dismiss()
    }

    @Test
    fun `dismiss when nothing shown does not crash`() {
        LoadingDialog.dismiss()
        LoadingDialog.dismiss() // double dismiss
    }

    @Test
    fun `cancelable dialog can be cancelled`() {
        val dialog = LoadingDialog.show(activity, cancelable = true)
        assertTrue(dialog.isShowing)
        dialog.cancel()
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `onCancel callback fires when cancelled`() {
        var cancelled = false
        val dialog = LoadingDialog.show(activity, cancelable = true) {
            cancelled = true
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        dialog.cancel()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(cancelled)
    }

    @Test
    fun `default message is used`() {
        val dialog = LoadingDialog.show(activity)
        assertNotNull(dialog)
        LoadingDialog.dismiss()
    }
}
