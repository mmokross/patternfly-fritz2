package org.patternfly

import dev.fritz2.dom.html.Span
import dev.fritz2.lenses.IdProvider
import org.patternfly.dom.Id

// ------------------------------------------------------ dsl

/**
 * Creates an instance of [Entries] containing [Item]s from the specified code block.
 *
 * @param idProvider used to uniquely identify each item
 * @param itemSelection defines how to select items
 * @param block function executed in the context of an [ItemsBuilder]
 */
@Deprecated("Deprecated API")
public fun <T> items(
    idProvider: IdProvider<T, String>,
    itemSelection: ItemSelection,
    block: ItemsBuilder<T>.() -> Unit = {}
): Entries<T> = ItemsBuilder(idProvider, itemSelection).apply(block).build()

/**
 * Creates an instance of [Entries] containing [Group]s from the specified code block.
 *
 * @param idProvider used to uniquely identify each item
 * @param itemSelection defines how to select items
 * @param block function executed in the context of an [GroupsBuilder]
 */
@Deprecated("Deprecated API")
public fun <T> groups(
    idProvider: IdProvider<T, String>,
    itemSelection: ItemSelection,
    block: GroupsBuilder<T>.() -> Unit = {}
): Entries<T> = GroupsBuilder(idProvider, itemSelection).apply(block).build()

/**
 * Adds an item to the enclosing [ItemsBuilder].
 *
 * @receiver an items builder this item is added to
 *
 * @param text an optional text for the visual representation
 * @param block function executed in the context of an [ItemBuilder]
 */
@Deprecated("Deprecated API")
public fun <T> ItemsBuilder<T>.item(item: T, text: String? = null, block: ItemBuilder<T>.() -> Unit = {}) {
    entries.add(ItemBuilder(item, text).apply(block).build())
}

/**
 * Adds a separator to the enclosing [ItemsBuilder].
 *
 * @receiver an items builder this separator is added to
 */
@Deprecated("Deprecated API")
public fun <T> ItemsBuilder<T>.separator() {
    entries.add(Separator())
}

/**
 * Adds a group to the enclosing [GroupsBuilder].
 *
 * @receiver a groups builder this group is added to
 *
 * @param text an optional text for the visual representation
 * @param block function executed in the context of an [GroupBuilder]
 */
@Deprecated("Deprecated API")
public fun <T> GroupsBuilder<T>.group(text: String? = null, block: GroupBuilder<T>.() -> Unit) {
    entries.add(GroupBuilder<T>(text).apply(block).build())
}

/**
 * Adds a separator to the enclosing [GroupsBuilder].
 *
 * @receiver a groups builder this separator is added to
 */
@Deprecated("Deprecated API")
public fun <T> GroupsBuilder<T>.separator() {
    entries.add(Separator())
}

/**
 * Adds an item to the enclosing [GroupBuilder].
 *
 * @receiver a group builder this item is added to
 *
 * @param text an optional text for the visual representation
 * @param block function executed in the context of an [ItemBuilder]
 */
@Deprecated("Deprecated API")
public fun <T> GroupBuilder<T>.item(item: T, text: String? = null, block: ItemBuilder<T>.() -> Unit = {}) {
    entries.add(ItemBuilder(item, text).apply(block).build())
}

/**
 * Adds a separator to the enclosing [GroupBuilder].
 *
 * @receiver a group builder this separator is added to
 */
@Deprecated("Deprecated API")
public fun <T> GroupBuilder<T>.separator() {
    entries.add(Separator())
}

// ------------------------------------------------------ types

/**
 * An [Entry] is either an [Item], a [Group] or a [Separator].
 */
@Deprecated("Deprecated API")
public sealed class Entry<T>

/**
 * Group containing a list of nested [entries][Entry] and an optional group heading. A group can contain nested [Item]s and [Separator]s, but must **not** contain nested groups.
 */
@Deprecated("Deprecated API")
public data class Group<T> internal constructor(
    internal val id: String = Id.unique("grp"),
    val text: String?,
    val entries: List<Entry<T>>
) : Entry<T>() {

    /**
     * A flat list of all [Item]s based on [entries].
     */
    public val items: List<Item<T>>
        get() = entries.filterIsInstance<Item<T>>()

    internal val hasSelection: Boolean
        get() = entries.any { it is Item<T> && it.selected }
}

/**
 * Item containing the actual data and additional properties.
 *
 * @param item the actual data
 * @param text an optional text for the visual representation
 * @param disabled whether this item is disabled
 * @param selected whether this item is selected
 * @param favorite whether this item has been marked as a favorite
 * @param href an optional link
 * @param description an optional description
 * @param icon an optional icon content function
 */
@Deprecated("Deprecated API")
public data class Item<T> internal constructor(
    override val item: T,
    val text: String? = null,
    val disabled: Boolean = false,
    val selected: Boolean = false,
    val favorite: Boolean = false,
    val href: String? = null,
    val description: String? = null,
    val icon: (Span.() -> Unit)? = null,
    internal var group: Group<T>? = null
) : Entry<T>(), HasItem<T>

/**
 * Separator used for visual purposes only.
 */
@Deprecated("Deprecated API")
public class Separator<T> : Entry<T>()

// ------------------------------------------------------ builder

/**
 * Builder for a list of [Item]s.
 */
@Deprecated("Deprecated API")
public class ItemsBuilder<T> internal constructor(
    private val idProvider: IdProvider<T, String>,
    private val itemSelection: ItemSelection
) {

    internal val entries: MutableList<Entry<T>> = mutableListOf()
    internal fun build(): Entries<T> = Entries(
        idProvider = idProvider,
        itemSelection = itemSelection,
        all = entries
    )
}

/**
 * Builder for a list of [Group]s.
 */
@Deprecated("Deprecated API")
public class GroupsBuilder<T> internal constructor(
    private val idProvider: IdProvider<T, String>,
    private val itemSelection: ItemSelection
) {

    internal val entries: MutableList<Entry<T>> = mutableListOf()
    internal fun build(): Entries<T> = Entries(
        idProvider = idProvider,
        itemSelection = itemSelection,
        all = entries
    )
}

/**
 * Builder for a [Group].
 */
@Deprecated("Deprecated API")
public class GroupBuilder<T> internal constructor(private val text: String?) {
    internal val entries: MutableList<Entry<T>> = mutableListOf()
    internal fun build(): Group<T> = Group(text = text, entries = entries).apply {
        entries.filterIsInstance<Item<T>>().forEach { it.group = this }
    }
}

/**
 * Builder for an [Item].
 */
@Deprecated("Deprecated API")
public class ItemBuilder<T> internal constructor(private val item: T, public var text: String?) {
    public var disabled: Boolean = false
    public var selected: Boolean = false
    public var favorite: Boolean = false
    public var href: String? = null
    public var description: String? = null
    public var icon: (Span.() -> Unit)? = null

    internal fun build(): Item<T> = Item(
        item = item,
        text = text,
        disabled = disabled,
        selected = selected,
        favorite = favorite,
        href = href,
        description = description,
        icon = icon,
        group = null
    )
}
