package org.tasks.filters

data object SignInPrompt : FilterListItem {
    override val itemType = FilterListItem.Type.SUBHEADER

    override fun areItemsTheSame(other: FilterListItem): Boolean = other is SignInPrompt
}
