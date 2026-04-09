package com.ail.brick.ui.dialog

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BrickDialogTest {

    private lateinit var activity: AppCompatActivity

    @Before
    fun setup() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        activity = controller.get()
        activity.setTheme(com.google.android.material.R.style.Theme_Material3_DayNight)
        controller.create().start().resume()
    }

    @Test
    fun `confirm shows a dialog`() {
        BrickDialog.confirm(activity, "Title", "Message") {}
        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }

    @Test
    fun `alert shows a dialog`() {
        BrickDialog.alert(activity, "Alert", "Content")
        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }

    @Test
    fun `input shows a dialog with text field`() {
        BrickDialog.input(activity, "Input", hint = "Enter text") {}
        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }

    @Test
    fun `list shows a dialog`() {
        BrickDialog.list(activity, "Pick", listOf("A", "B", "C")) {}
        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }

    @Test
    fun `bottomList shows a dialog`() {
        BrickDialog.bottomList(activity, "Choose", listOf("X", "Y")) {}
        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }

    @Test
    fun `custom with view shows a dialog`() {
        val view = TextView(activity).apply { text = "Custom" }
        BrickDialog.custom(activity, title = "Custom", view = view)
        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }

    @Test
    fun `confirm onConfirm callback fires`() {
        var confirmed = false
        BrickDialog.confirm(activity, "T", "M") { confirmed = true }
        val dialog = ShadowDialog.getLatestDialog() as? androidx.appcompat.app.AlertDialog
        assertNotNull("Dialog should be AlertDialog", dialog)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val button = dialog!!.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
        assertNotNull("Positive button should exist", button)
        button.performClick()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(confirmed)
    }

    @Test
    fun `confirm onCancel callback fires`() {
        var cancelled = false
        BrickDialog.confirm(activity, "T", "M", onCancel = { cancelled = true }) {}
        val dialog = ShadowDialog.getLatestDialog() as? androidx.appcompat.app.AlertDialog
        assertNotNull("Dialog should be AlertDialog", dialog)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val button = dialog!!.getButton(android.content.DialogInterface.BUTTON_NEGATIVE)
        assertNotNull("Negative button should exist", button)
        button.performClick()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(cancelled)
    }
}
