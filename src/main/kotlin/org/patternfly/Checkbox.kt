package org.patternfly

import dev.fritz2.dom.html.RenderContext
import dev.fritz2.dom.html.Span
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// ------------------------------------------------------ factory

public fun RenderContext.checkbox(
    name: String,
    standalone: Boolean = false,
    reversed: Boolean = false,
    baseClass: String? = null,
    id: String? = null,
    context: Checkbox.() -> Unit = {}
) {
    Checkbox(
        name = name,
        standalone = standalone,
        reversed = reversed
    ).apply(context).render(this, baseClass, id)
}

// ------------------------------------------------------ component

public class Checkbox(
    private val name: String,
    private val standalone: Boolean,
    private val reversed: Boolean
) : PatternFlyComponent<Unit>,
    WithElement by ElementMixin(),
    WithEvents by EventMixin(),
    WithTitle by TitleMixin() {

    private var description: SubComponent<Span>? = null
    private var content: SubComponent<Span>? = null
    private var checked: Flow<Boolean> = emptyFlow()
    private var disabled: Flow<Boolean> = emptyFlow()

    public fun description(baseClass: String? = null, id: String? = null, context: Span.() -> Unit) {
        this.description = SubComponent(baseClass, id, context)
    }

    public fun content(baseClass: String? = null, id: String? = null, context: Span.() -> Unit) {
        this.content = SubComponent(baseClass, id, context)
    }

    /**
     * Controls the checked state of the checkbox.
     */
    public fun checked(value: Boolean) {
        checked = flowOf(value)
    }

    /**
     * Controls the checked state of the checkbox.
     */
    public fun checked(value: Flow<Boolean>) {
        checked = value
    }

    /**
     * Disables the checkbox.
     */
    public fun disabled(value: Boolean) {
        disabled = flowOf(value)
    }

    /**
     * Disables the checkbox based on the values in the specified [Flow].
     */
    public fun disabled(value: Flow<Boolean>) {
        disabled = value
    }

    override fun render(context: RenderContext, baseClass: String?, id: String?) {
        with(context) {
            div(
                baseClass = classes {
                    +ComponentType.Checkbox
                    +("standalone".modifier() `when` standalone)
                    +baseClass
                },
                id = id ?: name
            ) {
                markAs(ComponentType.Checkbox)

                if (hasTitle && reversed) {
                    renderLabel(this)
                }
                input(baseClass = "check".component("input")) {
                    applyElement(this)
                    applyEvents(this)
                    type("checkbox")
                    name(name)
                    checked(checked)
                    disabled(disabled)
                }
                if (hasTitle && !reversed) {
                    renderLabel(this)
                }
                description?.let { description ->
                    span(
                        baseClass = classes("check".component("description"), description.baseClass),
                        id = description.id
                    ) {
                        description.context(this)
                    }
                }
                content?.let { content ->
                    span(
                        baseClass = classes("check".component("body"), content.baseClass),
                        id = content.id
                    ) {
                        content.context(this)
                    }
                }
            }
        }
    }

    private fun renderLabel(context: RenderContext) {
        with(context) {
            label(baseClass = "check".component("label")) {
                classMap(disabled.map { mapOf("disabled".modifier() to it) })
                `for`(name)
                applyTitle(this)
            }
        }
    }
}