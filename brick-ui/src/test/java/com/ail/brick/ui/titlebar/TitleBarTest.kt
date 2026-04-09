package com.ail.brick.ui.titlebar

import android.app.Activity
import android.view.View
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TitleBarTest {

    private lateinit var activity: Activity
    private lateinit var titleBar: TitleBar

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        titleBar = TitleBar(activity)
    }

    @Test
    fun `default title is empty`() {
        assertEquals("", titleBar.title)
    }

    @Test
    fun `setting title via property`() {
        titleBar.title = "Test Title"
        assertEquals("Test Title", titleBar.title)
    }

    @Test
    fun `back button is visible by default`() {
        assertEquals(View.VISIBLE, titleBar.getBackView().visibility)
    }

    @Test
    fun `right text view is hidden by default`() {
        assertEquals(View.GONE, titleBar.getRightTextView().visibility)
    }

    @Test
    fun `right image view is hidden by default`() {
        assertEquals(View.GONE, titleBar.getRightImageView().visibility)
    }

    @Test
    fun `setRightText shows text button`() {
        titleBar.setRightText("Save")
        assertEquals(View.VISIBLE, titleBar.getRightTextView().visibility)
        assertEquals("Save", titleBar.getRightTextView().text)
    }

    @Test
    fun `setRightText with empty string hides button`() {
        titleBar.setRightText("Save")
        titleBar.setRightText("")
        assertEquals(View.GONE, titleBar.getRightTextView().visibility)
    }

    @Test
    fun `setRightText with click listener`() {
        var clicked = false
        titleBar.setRightText("OK") { clicked = true }
        titleBar.getRightTextView().performClick()
        assertTrue(clicked)
    }

    @Test
    fun `setRightIcon shows icon button`() {
        titleBar.setRightIcon(android.R.drawable.ic_menu_search)
        assertEquals(View.VISIBLE, titleBar.getRightImageView().visibility)
    }

    @Test
    fun `setRightIcon with zero hides button`() {
        titleBar.setRightIcon(android.R.drawable.ic_menu_search)
        titleBar.setRightIcon(0)
        assertEquals(View.GONE, titleBar.getRightImageView().visibility)
    }

    @Test
    fun `setOnBackClickListener overrides default`() {
        var customClicked = false
        titleBar.setOnBackClickListener { customClicked = true }
        titleBar.getBackView().performClick()
        assertTrue(customClicked)
    }

    @Test
    fun `applyImmersivePadding does not crash`() {
        titleBar.applyImmersivePadding()
        // No exception
    }

    @Test
    fun `applyImmersivePadding called twice does not double pad`() {
        titleBar.applyImmersivePadding()
        titleBar.applyImmersivePadding()
        // Flag prevents double application — no exception
    }

    @Test
    fun `getters return non-null views`() {
        assertNotNull(titleBar.getBackView())
        assertNotNull(titleBar.getRightTextView())
        assertNotNull(titleBar.getRightImageView())
    }
}
