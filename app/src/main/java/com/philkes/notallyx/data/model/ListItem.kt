package com.philkes.notallyx.data.model

data class ListItem(
    var body: String,
    var checked: Boolean,
    var isChild: Boolean,
    var order: Int?,
    var children: MutableList<ListItem>,
    var id: Int = -1,
    var checkedTimestamp: Long? = null,
) : Cloneable {

    public override fun clone(): Any {
        // Deep clone!
        return ListItem(
            body,
            checked,
            isChild,
            order,
            children.map { it.clone() as ListItem }.toMutableList(),
            id,
            checkedTimestamp,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ListItem) {
            return false
        }
        return (this.body == other.body &&
            this.order == other.order &&
            this.checked == other.checked &&
            this.isChild == other.isChild &&
            this.checkedTimestamp == other.checkedTimestamp)
    }

    val itemCount: Int
        get() = children.size + 1

    fun isChildOf(other: ListItem): Boolean {
        return !other.isChild && other.children.contains(this)
    }

    override fun toString(): String {
        return "${if (isChild) " >" else ""}${if (checked) "x" else ""} ${body}${
            if (children.isNotEmpty()) "(${
                children.map { it.body }.joinToString(",")
            })" else ""
        }"
    }
}
