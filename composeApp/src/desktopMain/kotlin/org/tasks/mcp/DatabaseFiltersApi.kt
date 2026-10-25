package org.tasks.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.tasks.api.ApiPage
import org.tasks.api.ApiQueryEngine
import org.tasks.api.FilterQuery
import org.tasks.api.TaskRow
import org.tasks.api.TasksContract
import org.tasks.api.filterTasks
import org.tasks.data.NO_ORDER
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.entity.Filter
import org.tasks.filters.BooleanCriterion
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.CustomFilterCriterion
import org.tasks.filters.CriterionInstance
import org.tasks.filters.FilterCriteriaProvider
import org.tasks.filters.FilterPreferenceCodec
import org.tasks.filters.MultipleSelectCriterion
import org.tasks.filters.PlaceFilter
import org.tasks.filters.TagFilter
import org.tasks.filters.TextInputCriterion
import org.tasks.filters.key
import org.tasks.filters.sql
import org.tasks.filters.values
import org.tasks.preferences.TasksPreferences

class DatabaseFiltersApi(
    private val filterDao: FilterDao,
    private val engine: ApiQueryEngine,
    private val preferences: TasksPreferences,
    private val codec: FilterPreferenceCodec,
    private val provider: FilterCriteriaProvider,
    private val taskListMetadataDao: TaskListMetadataDao,
) {

    companion object {
        /**
         * Upper bounds on agent-supplied filter input. Kept far below the sizes where the
         * stored SQL and the app's edit dialog start to misbehave (observed: a ~1.6MB
         * value broke execution and dropped the criteria on read-back; a 160KB title
         * pushed the dialog's delete button off screen). A 500-char title or text match
         * is already far beyond any realistic use.
         */
        const val MAX_TITLE_LENGTH = 500
        const val MAX_VALUE_LENGTH = 500
        const val MAX_CRITERIA_COUNT = 50
    }

    @Serializable
    data class FilterRow(
        val id: Long,
        val title: String,
        val icon: String?,
        val color: Int,
        val order: Int,
        val criteria: List<CriterionRow>,
        val sql: String?,
    )

    /**
     * One stored row of a filter, decoded through the app's own [FilterCriteriaProvider]
     * (the same path the edit UI uses) so the structured view cannot drift from what the
     * app shows. `value` is the raw stored value; [valueDisplay] is what the user sees.
     * The locked leading universe row is included as `type: "universe"` so a round-trip
     * of the array back to create/update is complete.
     */
    @Serializable
    data class CriterionRow(
        val identifier: String,
        val value: String?,
        @SerialName("value_display") val valueDisplay: String?,
        val type: String,
    )

    suspend fun list(
        includeSql: Boolean = false,
        ids: List<Long> = emptyList(),
        limit: Int = TasksContract.DEFAULT_LIMIT,
        offset: Int = 0,
    ): ApiPage<FilterRow> {
        val all = filterDao.getFilters().map { it.toRow(includeSql) }
        val matched = if (ids.isEmpty()) all else all.filter { it.id in ids }
        return ApiPage(matched.drop(offset).take(limit), matched.size, limit, offset)
    }

    suspend fun tasks(
        filterId: Long,
        query: FilterQuery,
    ): ApiPage<TaskRow> {
        val filter = requireNotNull(filterDao.getById(filterId)) {
            "No filter with id $filterId - read list_filters to see which filters exist"
        }
        val sql = filter.sql
        require(!sql.isNullOrBlank()) {
            "Filter $filterId ('${filter.title.orEmpty()}') has no stored SQL, so it cannot be run"
        }
        return engine.filterTasks(sql, query)
    }

    /**
     * The filter the user currently has selected in the app's navigation drawer. The app
     * mirrors every drawer selection into [TasksPreferences.lastViewedList], encoded by
     * [FilterPreferenceCodec], so reading and decoding that preference gives the live
     * selection without touching the UI. Search and the empty view are never recorded, so
     * while the user is searching this reports the last filter they selected.
     */
    suspend fun selected(includeSql: Boolean = false): SelectedFilter? {
        val stored = preferences.get(TasksPreferences.lastViewedList, "")
        if (stored.isEmpty()) return null
        return when (val filter = codec.decode(stored) ?: return null) {
            is CustomFilter -> SelectedFilter(
                type = "custom_filter",
                id = filter.id,
                title = filter.title,
                icon = filter.icon,
                color = filter.tint,
                order = filter.order,
                sql = if (includeSql) filter.sql else null,
            )
            is CaldavFilter -> SelectedFilter(
                type = "list",
                id = filter.calendar.id,
                title = filter.title,
                icon = filter.icon,
                color = filter.tint,
                order = filter.order,
            )
            is TagFilter -> SelectedFilter(
                type = "tag",
                id = filter.tagData.id,
                title = filter.title,
                icon = filter.icon,
                color = filter.tint,
                order = filter.order,
            )
            is PlaceFilter -> SelectedFilter(
                type = "place",
                id = filter.place.id,
                title = filter.title,
                icon = filter.icon,
                color = filter.tint,
                order = filter.order,
            )
            else -> SelectedFilter(
                type = "builtin",
                title = filter.title,
                key = filter.key(),
            )
        }
    }

    @Serializable
    data class SelectedFilter(
        val type: String,
        val id: Long? = null,
        val title: String,
        val icon: String? = null,
        val color: Int = 0,
        val order: Int = NO_ORDER,
        val key: String? = null,
        val sql: String? = null,
    )

    @Serializable
    data class CriterionEntry(
        val value: String,
        val label: String?,
    )

    @Serializable
    data class CriterionInfo(
        val identifier: String,
        val name: String?,
        val kind: String,
        val values: List<CriterionEntry>? = null,
    )

    /**
     * One row of a filter as the agent sends it. `type` is the app's row vocabulary
     * (intersect/add/subtract); the universe row is app-managed and normally omitted.
     */
    data class CriterionWrite(
        val identifier: String,
        val value: String? = null,
        val type: String = "intersect",
    )

    data class DeletionResult(
        val id: Long,
        val pinnedBy: List<Long>,
    )

    /**
     * The app's catalog of criterion types, as [FilterCriteriaProvider] sees it right now:
     * tag and list entries are built from the current database, so what the app's editor
     * offers is exactly what an agent may use. The universe criterion (identifier `active`)
     * is deliberately absent - the app always stores it as the locked leading row.
     */
    suspend fun criteriaCatalog(): List<CriterionInfo> = provider.all().map { criterion ->
        when (criterion) {
            is BooleanCriterion -> CriterionInfo(
                identifier = criterion.identifier.orEmpty(),
                name = criterion.name,
                kind = "boolean",
            )
            is TextInputCriterion -> CriterionInfo(
                identifier = criterion.identifier.orEmpty(),
                name = criterion.name,
                kind = "text_input",
            )
            is MultipleSelectCriterion -> CriterionInfo(
                identifier = criterion.identifier.orEmpty(),
                name = criterion.name,
                kind = "multi_select",
                values = criterion.entryValues
                    ?.mapIndexedNotNull { index, value ->
                        value?.let { CriterionEntry(it, criterion.entryTitles?.getOrNull(index)) }
                    },
            )
            else -> throw IllegalStateException("Unknown criterion kind: ${criterion::class}")
        }
    }

    suspend fun create(
        title: String,
        icon: String?,
        color: Int?,
        criteria: List<CriterionWrite>,
    ): FilterRow {
        require(title.isNotBlank()) { "title must not be blank" }
        require(title.length <= MAX_TITLE_LENGTH) {
            "title is ${title.length} characters; the limit is $MAX_TITLE_LENGTH"
        }
        val instances = buildCriteria(criteria)
        val id = filterDao.insert(
            Filter(
                title = title,
                icon = icon,
                color = color,
                sql = instances.sql,
                values = instances.values,
                criterion = CriterionInstance.serialize(instances),
            ),
        )
        return requireNotNull(filterDao.getById(id)).toRow(includeSql = false)
    }

    /**
     * Patch semantics: only the fields that are present change. `criteria`, when sent,
     * replaces the row set wholesale - the app's editor has no per-row addressing either.
     * The id is preserved, so is any list's default-view pin on it.
     */
    suspend fun update(
        id: Long,
        title: String?,
        icon: String?,
        color: Int?,
        criteria: List<CriterionWrite>?,
    ): FilterRow {
        val existing = requireNotNull(filterDao.getById(id)) {
            "No filter with id $id - read list_filters to see which filters exist"
        }
        require(title != null || icon != null || color != null || criteria != null) {
            "Nothing to change: send at least one of title, icon, color or criteria"
        }
        if (title != null) {
            require(title.isNotBlank()) { "title must not be blank" }
            require(title.length <= MAX_TITLE_LENGTH) {
                "title is ${title.length} characters; the limit is $MAX_TITLE_LENGTH"
            }
        }
        val instances = criteria?.let { buildCriteria(it) }
        val updated = existing.copy(
            title = title ?: existing.title,
            icon = icon ?: existing.icon,
            color = color ?: existing.color,
            sql = instances?.sql ?: existing.sql,
            values = instances?.values ?: existing.values,
            criterion = instances?.let { CriterionInstance.serialize(it) } ?: existing.criterion,
        )
        filterDao.update(updated)
        return updated.toRow(includeSql = false)
    }

    /**
     * Removes the device-local filters row only - no tasks, no caldav, no network. The
     * response carries the ids of any lists whose default view is pinned to this filter,
     * because those lists will silently fall back to their default view afterwards.
     */
    suspend fun delete(id: Long): DeletionResult {
        val existing = requireNotNull(filterDao.getById(id)) {
            "No filter with id $id - read list_filters to see which filters exist"
        }
        val pinnedBy = taskListMetadataDao.findPinnedTo(id.toString())
            .mapNotNull { it.id }
        filterDao.delete(existing)
        return DeletionResult(id, pinnedBy)
    }

    /**
     * Validates the agent's rows against the app's live catalog and builds the
     * [CriterionInstance] list exactly as the app's editor would: the locked universe row
     * leading, every value resolved through the criterion, ready for the app's own
     * [sql]/[values] renderers (which run inlined values through the app's sanitiser).
     */
    private suspend fun buildCriteria(criteria: List<CriterionWrite>): List<CriterionInstance> {
        require(criteria.isNotEmpty()) {
            "criteria must not be empty: a filter needs at least one criterion. " +
                "Read list_filter_criteria for what can be expressed."
        }
        require(criteria.size <= MAX_CRITERIA_COUNT) {
            "criteria has ${criteria.size} rows; the limit is $MAX_CRITERIA_COUNT"
        }
        val catalog = buildMap {
            put(FilterCriteriaProvider.IDENTIFIER_UNIVERSE, provider.startingUniverse())
            provider.all().forEach { put(it.identifier.orEmpty(), it) }
        }
        val instances = mutableListOf<CriterionInstance>()
        criteria.forEachIndexed { index, row ->
            val criterion: CustomFilterCriterion = catalog[row.identifier]
                ?: throw IllegalArgumentException(
                    "Unknown criterion identifier '${row.identifier}' - valid identifiers: " +
                        catalog.keys.filter { it != FilterCriteriaProvider.IDENTIFIER_UNIVERSE }
                            .sorted().joinToString() +
                        ". Read list_filter_criteria for what each one takes.",
                )
            val instance = CriterionInstance()
            instance.criterion = criterion
            instance.type = when (row.type) {
                "intersect" -> CriterionInstance.TYPE_INTERSECT
                "add" -> CriterionInstance.TYPE_ADD
                "subtract" -> CriterionInstance.TYPE_SUBTRACT
                "universe" -> {
                    require(index == 0 && !instances.any { it.type == CriterionInstance.TYPE_UNIVERSE }) {
                        "A 'universe' row is the app-managed leading row; " +
                            "it may only appear first, if at all"
                    }
                    CriterionInstance.TYPE_UNIVERSE
                }
                else -> throw IllegalArgumentException(
                    "Unknown type '${row.type}' on criterion '${row.identifier}' - " +
                        "use intersect (AND, the default), add (OR) or subtract (AND NOT)",
                )
            }
            when (criterion) {
                is BooleanCriterion -> require(row.value == null) {
                    "Criterion '${row.identifier}' is boolean and takes no value"
                }
                is TextInputCriterion -> {
                    val value = row.value
                    require(!value.isNullOrBlank()) {
                        "Criterion '${row.identifier}' needs a value"
                    }
                    require(value.length <= MAX_VALUE_LENGTH) {
                        "Criterion '${row.identifier}' value is ${value.length} characters; " +
                            "the limit is $MAX_VALUE_LENGTH"
                    }
                    instance.selectedText = value
                }
                is MultipleSelectCriterion -> {
                    val value = row.value
                    val values = criterion.entryValues ?: emptyArray()
                    val options = values
                        .mapIndexedNotNull { i, v ->
                            v?.let { "'${criterion.entryTitles?.getOrNull(i) ?: it}' ($it)" }
                        }
                    require(!value.isNullOrBlank() && value in values) {
                        "Criterion '${row.identifier}' needs one of its valid values, got " +
                            "${value ?: "(none)"}: ${options.joinToString()}. " +
                            "Read list_filter_criteria for the current options - they include " +
                            "the live tag and list names."
                    }
                    instance.selectedIndex = values.indexOf(value)
                }
                else -> throw IllegalStateException("Unknown criterion kind: ${criterion::class}")
            }
            instances.add(instance)
        }
        return if (instances.any { it.type == CriterionInstance.TYPE_UNIVERSE }) {
            instances
        } else {
            listOf(universeInstance()) + instances
        }
    }

    private suspend fun universeInstance(): CriterionInstance = CriterionInstance().apply {
        criterion = provider.startingUniverse()
        type = CriterionInstance.TYPE_UNIVERSE
    }

    private suspend fun Filter.toRow(includeSql: Boolean): FilterRow = FilterRow(
        id = id,
        title = title.orEmpty(),
        icon = icon,
        color = color ?: 0,
        order = order,
        criteria = provider.fromString(criterion).map { instance ->
            val criterionType = when (instance.type) {
                CriterionInstance.TYPE_ADD -> "add"
                CriterionInstance.TYPE_SUBTRACT -> "subtract"
                CriterionInstance.TYPE_UNIVERSE -> "universe"
                else -> "intersect"
            }
            CriterionRow(
                identifier = instance.criterion.identifier.orEmpty(),
                value = instance.valueFromCriterion,
                valueDisplay = instance.titleFromCriterion,
                type = criterionType,
            )
        },
        sql = if (includeSql) sql else null,
    )
}
