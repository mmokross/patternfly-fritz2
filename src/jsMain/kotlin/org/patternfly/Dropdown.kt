package org.patternfly

import dev.fritz2.binding.OfferingHandler
import dev.fritz2.binding.RootStore
import dev.fritz2.binding.action
import dev.fritz2.binding.each
import dev.fritz2.binding.handledBy
import dev.fritz2.dom.html.Button
import dev.fritz2.dom.html.Div
import dev.fritz2.dom.html.HtmlElements
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.patternfly.DividerVariant.DIV
import org.w3c.dom.HTMLDivElement

// ------------------------------------------------------ dsl

fun <T> HtmlElements.pfDropdown(
    store: DropdownStore<T> = DropdownStore(),
    text: String,
    align: Align? = null,
    up: Boolean = false,
    classes: String? = null,
    content: Dropdown<T>.() -> Unit = {}
): Dropdown<T> = register(Dropdown(store, Either.Left(text), align, up, classes), content)

fun <T> HtmlElements.pfDropdown(
    store: DropdownStore<T> = DropdownStore(),
    text: String,
    align: Align? = null,
    up: Boolean = false,
    modifier: Modifier,
    content: Dropdown<T>.() -> Unit = {}
): Dropdown<T> = register(Dropdown(store, Either.Left(text), align, up, modifier.value), content)

fun <T> HtmlElements.pfDropdown(
    store: DropdownStore<T> = DropdownStore(),
    icon: Icon,
    align: Align? = null,
    up: Boolean = false,
    classes: String? = null,
    content: Dropdown<T>.() -> Unit = {}
): Dropdown<T> = register(Dropdown(store, Either.Right(icon), align, up, classes), content)

fun <T> HtmlElements.pfDropdown(
    store: DropdownStore<T> = DropdownStore(),
    icon: Icon,
    align: Align? = null,
    up: Boolean = false,
    modifier: Modifier,
    content: Dropdown<T>.() -> Unit = {}
): Dropdown<T> = register(Dropdown(store, Either.Right(icon), align, up, modifier.value), content)

fun <T> HtmlElements.pfDropdownKebab(
    store: DropdownStore<T> = DropdownStore(),
    align: Align? = null,
    up: Boolean = false,
    classes: String? = null,
    content: Dropdown<T>.() -> Unit = {}
): Dropdown<T> =
    register(Dropdown(store, Either.Right(pfIcon("ellipsis-v".fas())), align, up, classes), content)

fun <T> HtmlElements.pfDropdownKebab(
    store: DropdownStore<T> = DropdownStore(),
    align: Align? = null,
    up: Boolean = false,
    modifier: Modifier,
    content: Dropdown<T>.() -> Unit = {}
): Dropdown<T> =
    register(Dropdown(store, Either.Right(pfIcon("ellipsis-v".fas())), align, up, modifier.value), content)

fun <T> Dropdown<T>.pfEntries(block: EntryBuilder<T>.() -> Unit) {
    val entries = EntryBuilder<T>().apply(block).build()
    action(entries) handledBy this.store.update
}

// ------------------------------------------------------ tag

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class Dropdown<T> internal constructor(
    val store: DropdownStore<T>,
    private val textOrIcon: Either<String, Icon>,
    align: Align?,
    up: Boolean,
    classes: String?
) : PatternFlyComponent<HTMLDivElement>, Div(baseClass = classes {
    +ComponentType.Dropdown
    +align?.modifier
    +("top".modifier() `when` up)
    +classes
}) {

    private val button: Button
    val ces = CollapseExpandStore { target -> !domNode.contains(target) }
    var asText: AsText<T> = { it.toString() }
    var display: ComponentDisplay<Button, T> = {
        {
            +this@Dropdown.asText(it)
        }
    }
    var disabled: Flow<Boolean>
        get() = button.disabled
        set(value) {
            button.disabled = value
        }

    init {
        markAs(ComponentType.Dropdown)
        classMap = ces.data.map { expanded -> mapOf(Modifier.expanded.value to expanded) }
        val buttonId = Id.unique(ComponentType.Dropdown.id, "btn")
        button = button(id = buttonId, baseClass = "dropdown".component("toggle")) {
            aria["haspopup"] = true
            clicks handledBy this@Dropdown.ces.toggle
            this@Dropdown.ces.data.map { it.toString() }.bindAttr("aria-expanded")
            when (this@Dropdown.textOrIcon) {
                is Either.Left -> {
                    span(baseClass = "dropdown".component("toggle", "text")) {
                        +this@Dropdown.textOrIcon.value
                    }
                    span(baseClass = "dropdown".component("toggle", "icon")) {
                        pfIcon("caret-down".fas())
                    }
                }
                is Either.Right -> {
                    domNode.classList += Modifier.plain
                    register(this@Dropdown.textOrIcon.value) {}
                }
            }
        }
        ul(baseClass = classes {
            +"dropdown".component("menu")
            +align?.modifier
        }) {
            aria["labelledby"] = buttonId
            attr("role", "menu")
            this@Dropdown.ces.data.map { !it }.bindAttr("hidden")
            this@Dropdown.store.data.each().render { entry ->
                when (entry) {
                    is Item<T> -> {
                        li {
                            attr("role", "menuitem")
                            button(baseClass = "dropdown".component("menu-item")) {
                                attr("tabindex", "-1")
                                if (entry.disabled) {
                                    attr("disabled", "true")
                                    domNode.classList += Modifier.disabled
                                }
                                if (entry.selected) {
                                    domNode.autofocus = true
                                }
                                val content = this@Dropdown.display(entry.item)
                                content.invoke(this)

                                clicks.map { entry.item } handledBy this@Dropdown.store.clicked
                                clicks handledBy this@Dropdown.ces.collapse
                            }
                        }
                    }
                    is Group<T> -> {
                        li(baseClass = "display-none".util()) {
                            !"Dropdown groups not yet implemented"
                        }
                    }
                    is Separator<T> -> {
                        li {
                            attr("role", "separator")
                            pfDivider(DIV)
                        }
                    }
                }
            }.bind()
        }
    }
}

// ------------------------------------------------------ store

class DropdownStore<T> : RootStore<List<Entry<T>>>(listOf()) {

    internal val clicked: OfferingHandler<T, T> = handleAndOffer { items, item ->
        offer(item)
        items
    }

    val clicks: Flow<T> = clicked.map { it }

    val items: Flow<List<T>> = data.map {
        it.filterIsInstance<Item<T>>()
    }.map { it.map { item -> item.item } }

    val groups: Flow<List<Group<T>>> = data.map { it.filterIsInstance<Group<T>>() }
}
