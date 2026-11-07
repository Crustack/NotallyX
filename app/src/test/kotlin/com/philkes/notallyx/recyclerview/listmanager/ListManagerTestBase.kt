package com.philkes.notallyx.recyclerview.listmanager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.philkes.notallyx.core.common.extensions.find
import com.philkes.notallyx.core.preference.EnumPreference
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.callback
import com.philkes.notallyx.core.preference.isAutoSortChecked
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.listitem.ListManager
import com.philkes.notallyx.feature.editor.listitem.init
import com.philkes.notallyx.feature.editor.listitem.sorting.SortedItemsList
import com.philkes.notallyx.test.assertChildren
import com.philkes.notallyx.test.createListItem
import com.philkes.notallyx.test.mockAndroidLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

open class ListManagerTestBase {

    protected lateinit var changeHistory: ChangeHistory
    protected lateinit var preferences: NotallyXPreferences
    private var itemsInternal: MutableList<ListItem> = mutableListOf()
    protected var itemsChecked: SortedItemsList? = null

    lateinit var listManager: ListManager

    @get:Rule val rule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        mockAndroidLog()
        changeHistory = ChangeHistory()
        preferences = mock(NotallyXPreferences::class.java)
        listManager = ListManager(changeHistory = changeHistory, preferences = preferences)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun setSorting(sorting: ListItemSort) {
        itemsInternal = mutableListOf()
        val listItemSortingPreference = mock(EnumPreference::class.java)
        `when`(listItemSortingPreference.value).thenReturn(sorting)
        `when`(preferences.listItemSorting)
            .thenReturn(listItemSortingPreference as EnumPreference<ListItemSort>)

        if (sorting.isAutoSortChecked) {
            itemsChecked = SortedItemsList(sorting.callback())
        }
        itemsInternal.addAll(
            listOf(
                    createListItem("A", id = 0, order = 0),
                    createListItem("B", id = 1, order = 1),
                    createListItem("C", id = 2, order = 2),
                    createListItem("D", id = 3, order = 3),
                    createListItem("E", id = 4, order = 4),
                    createListItem("F", id = 5, order = 5),
                )
                .init()
        )
        listManager.init(itemsInternal, itemsChecked)
    }

    protected val items: MutableList<ListItem>
        get() = listManager.items

    protected operator fun List<ListItem>.get(body: String): ListItem {
        return this.find { it.body == body }!!
    }

    protected fun findItem(function: (item: ListItem) -> Boolean): ListItem? {
        return items.find(function) ?: itemsChecked?.find(function)
    }

    protected fun String.assertChildren(vararg childrenBodies: String) {
        assertIsParent()
        findItem { it.body == this }!!.assertChildren(*childrenBodies)
    }

    protected fun String.assertIsChecked() {
        assertTrue("checked", findItem { it.body == this }!!.checked)
    }

    protected fun String.assertIsNotChecked() {
        assertFalse("checked", findItem { it.body == this }!!.checked)
    }

    protected fun String.assertOrder(expected: Int) {
        assertEquals("order", expected, findItem { it.body == this }!!.order)
    }

    protected fun String.assertPosition(expected: Int) {
        assertEquals("position in items", expected, items.indexOfFirst { it.body == this })
    }

    protected fun String.assertIsParent() {
        assertFalse(findItem { it.body == this }!!.isChild)
    }

    protected fun String.assertIsChild() {
        assertTrue(findItem { it.body == this }!!.isChild)
    }

    protected val String.itemCount: Int
        get() {
            return findItem { it.body == this }!!.itemCount
        }

    protected fun ListManager.addWithChildren(
        position: Int = items.size,
        parentBody: String,
        vararg childrenBodies: String,
    ) {
        val children =
            childrenBodies.map { createListItem(body = it, isChild = true) }.toMutableList()
        this.add(position, createListItem(body = parentBody, children = children))
    }
}
