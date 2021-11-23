package org.patternfly

import dev.fritz2.lenses.IdProvider
import org.patternfly.ItemSelection.MULTIPLE
import org.patternfly.ItemSelection.SINGLE
import org.patternfly.ItemSelection.SINGLE_PER_GROUP

/**
 * Immutable collection of [entries][Entry] used in different stores such as [MenuStore] or [OptionsMenuStore]. Every modification leads to a new instance with changed properties. Each item has to be uniquely identifiable using the specified [idProvider].
 *
 * This class holds instances of class [Entry]. Each Entry is either an [Item], a [Group] or a [Separator]. A group can contain nested [Item]s or [Separator]s, but must **not** contain nested groups. An [Item] can have additional properties such as an [icon][Item.icon], a [description][Item.description] or a [disabled][Item.disabled] state. A [Separator] has no additional properties and is only used to visually separate groups and items.
 *
 * Entries can be obtained using different properties:
 *
 * - [all]: All entries given when this instance was created. This collection never changes.
 * - [entries]: List of entries after an optional [filter] has been applied to [all].
 * - [items]: A flat list of all [Item]s based on [entries].
 * - [selection]: A flat list of selected [Item]s based on [all].
 **
 * @param idProvider used to uniquely identify each item
 * @param itemSelection defines how to select items
 * @param all all entries managed by this instance
 * @param filter a predicate applied to [all]
 */
public data class Entries<T>(
    val idProvider: IdProvider<T, String>,
    val itemSelection: ItemSelection,
    val all: List<Entry<T>> = emptyList(),
    private val filter: ItemFilter<T>? = null
) {

    /**
     * List after an optional [filter] has been applied to [all]. Empty [Group]s w/o items are omitted.
     */
    @Suppress("MemberNameEqualsClassName")
    public val entries: List<Entry<T>>
        get() = if (filter != null) {
            all.map { entry -> // (1) filter items in groups
                if (entry is Group<T>) {
                    entry.copy(
                        entries = entry.entries.filter { groupEntry ->
                            when (groupEntry) {
                                is Group<T> -> {
                                    warnAboutNestedGroups(entry, groupEntry)
                                    true
                                }
                                is Item<T> -> filter.invoke(groupEntry.item)
                                is Separator -> true
                            }
                        }
                    )
                } else {
                    entry
                }
            }.filter { entry -> // (2) filter top level items
                if (entry is Item<T>) {
                    filter.invoke(entry.item)
                } else {
                    true
                }
            }.filter { entry -> // (3) filter empty groups
                !(entry is Group<T> && entry.items.isEmpty())
            }
        } else {
            all
        }

    /**
     * A list of all [Group]s based on [entries].
     */
    public val groups: List<Group<T>>
        get() = entries.filterIsInstance<Group<T>>()

    /**
     * A flat list of all [Item]s based on [entries].
     */
    public val items: List<Item<T>>
        get() = flatItems(entries)

    /**
     * The selected items based on [all].
     */
    public val selection: List<Item<T>>
        get() = flatItems(all).filter { it.selected }

    /**
     * First selected item (if any)
     */
    public val singleSelection: Item<T>?
        get() = selection.firstOrNull()

    /**
     * Applies the specified filter.
     */
    public fun filter(filter: ItemFilter<T>): Entries<T> = copy(filter = filter)

    /**
     * Removes any filter.
     */
    public fun clearFilter(): Entries<T> = copy(filter = null)

    /**
     * Selects the specified data and returns a new instance. Selecting an item might lead to unselecting other items depending on the value of [itemSelection].
     */
    public fun select(data: T): Entries<T> {
        var groupWithSelection = false
        val itemId = idProvider(data)
        val item = flatItems(all).find { idProvider(it.item) == itemId }

        return if (item != null) {
            copy(
                all = all.map { entry ->
                    when (entry) {
                        is Group<T> -> {
                            val groupCopy = selectInGroup(entry, item, itemId, groupWithSelection)
                            groupWithSelection = groupCopy.hasSelection
                            groupCopy
                        }
                        is Item<T> -> entry.copy(
                            selected = when (itemSelection) {
                                SINGLE, SINGLE_PER_GROUP -> {
                                    idProvider(entry.item) == itemId
                                }
                                MULTIPLE -> {
                                    if (idProvider(entry.item) == itemId) {
                                        !entry.selected
                                    } else {
                                        entry.selected
                                    }
                                }
                            }
                        )
                        is Separator<T> -> entry
                    }
                }
            )
        } else {
            this
        }
    }

    private fun selectInGroup(
        group: Group<T>,
        item: Item<T>,
        itemId: String,
        groupWithSelection: Boolean
    ): Group<T> = group.copy(
        entries = group.entries.map { groupEntry ->
            when (groupEntry) {
                is Group<T> -> {
                    warnAboutNestedGroups(group, groupEntry)
                    groupEntry
                }
                is Item<T> -> groupEntry.copy(
                    selected = when (itemSelection) {
                        SINGLE -> {
                            if (groupWithSelection) {
                                false
                            } else {
                                idProvider(groupEntry.item) == itemId
                            }
                        }
                        SINGLE_PER_GROUP -> {
                            if (idProvider(groupEntry.item) == itemId) {
                                true
                            } else {
                                if (groupEntry.group?.id == item.group?.id) {
                                    false
                                } else {
                                    groupEntry.selected
                                }
                            }
                        }
                        MULTIPLE -> {
                            if (idProvider(groupEntry.item) == itemId) {
                                !groupEntry.selected
                            } else {
                                groupEntry.selected
                            }
                        }
                    }
                )
                is Separator -> groupEntry
            }
        }
    )

    private fun <T> flatItems(entries: List<Entry<T>>): List<Item<T>> = entries.flatMap { entry ->
        when (entry) {
            is Group<T> -> entry.entries.filterIsInstance<Item<T>>()
            is Item<T> -> listOf(entry)
            is Separator<T> -> emptyList()
        }
    }

    private fun <T> warnAboutNestedGroups(parent: Group<T>, child: Group<T>) {
        console.warn(
            "Nested group detected: " +
                "Parent group with id ${parent.id} contains nested group with id ${child.id}. " +
                "Nested groups are not supported!"
        )
    }
}

/**
 * Enum which controls how to select an [Item] in [Entries].
 */
@Suppress("unused")
public enum class ItemSelection {

    /**
     * Only a single item (across all groups) can be selected at a time.
     */
    SINGLE,

    /**
     * Only one item per group can be selected at a time.
     */
    SINGLE_PER_GROUP,

    /**
     * Multiple items can be selected.
     */
    MULTIPLE
}
