#include "task_list_window.h"
#include "protocol.h"
#include "task_view_window.h"
#include "menu_window.h"

static Window *s_window;
static MenuLayer *s_menu_layer;
static TextLayer *s_loading_layer;

// Active item array (what the menu reads from)
static UiItem *s_items = NULL;
static int s_items_capacity = 0;
static int s_num_items = 0;
static int s_total_items = 0;   // total available on phone
static bool s_loading = false;
static bool s_loading_more = false; // loading a subsequent page (not initial)

// Pending buffer for refresh (accumulated while old data stays visible)
static UiItem *s_pending = NULL;
static int s_pending_capacity = 0;
static int s_pending_count = 0;
static int s_pending_total = 0;
static bool s_refreshing = false;
static MenuIndex s_saved_selection;

// Current filter
static char s_filter_id[MAX_FILTER_ID_LEN] = {0};
static char s_filter_name[MAX_TITLE_LEN] = "My Tasks";
static uint32_t s_filter_color = 0;
static uint32_t s_filter_text_color = 0;

// Chunk assembly for current page request
static int s_expected_chunks = 0;
static int s_received_chunks = 0;
static uint8_t s_request_txn = 0;
static AppTimer *s_chunk_timer = NULL;

// Row 0 is the filter header; task items start at row 1
#define FILTER_ROW 0

// Persistent storage keys
#define PERSIST_FILTER_ID         1
#define PERSIST_FILTER_NAME       2
#define PERSIST_FILTER_COLOR      3
#define PERSIST_FILTER_TEXT_COLOR  4

static void show_loading(bool show);
static void request_tasks_page(int position, int limit);
static void page_complete(void);
static void chunk_timeout_handler(void *data);
static void request_next_page(void);
static void on_filter_selected(const char *filter_id, const char *filter_name, uint32_t color, uint32_t text_color);

// Ensure an item array can hold at least `needed` items.
static bool ensure_capacity_for(UiItem **items, int *capacity, int needed) {
    if (needed <= *capacity) return true;

    int new_cap = ((needed + PAGE_SIZE - 1) / PAGE_SIZE) * PAGE_SIZE;
    UiItem *new_items = (UiItem *)realloc(*items, new_cap * sizeof(UiItem));
    if (!new_items) {
        new_items = (UiItem *)realloc(*items, needed * sizeof(UiItem));
        if (!new_items) {
            APP_LOG(APP_LOG_LEVEL_ERROR, "Failed to allocate for %d items", needed);
            return false;
        }
        new_cap = needed;
    }
    *items = new_items;
    *capacity = new_cap;
    return true;
}

static void free_items(void) {
    free(s_items);
    s_items = NULL;
    s_items_capacity = 0;
    s_num_items = 0;
}

static void free_pending(void) {
    free(s_pending);
    s_pending = NULL;
    s_pending_capacity = 0;
    s_pending_count = 0;
    s_pending_total = 0;
}

// MenuLayer callbacks

static uint16_t get_num_rows(MenuLayer *menu_layer, uint16_t section_index, void *data) {
    if (s_loading && s_num_items == 0) return 0; // hidden during initial load
    if (s_num_items > 0) return (uint16_t)(1 + s_num_items); // filter row + items
    return 2; // filter row + "No tasks"
}

static int16_t get_cell_height(MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
    if (cell_index->row == FILTER_ROW) return 28;
    if (s_num_items == 0) return 44; // "No tasks" row
    int item_idx = cell_index->row - 1;
    if (item_idx < s_num_items) {
        return s_items[item_idx].type == UI_TYPE_HEADER ? 28 : 44;
    }
    return 44;
}

static bool is_row_selected(MenuIndex *cell_index) {
    MenuIndex sel = menu_layer_get_selected_index(s_menu_layer);
    return sel.section == cell_index->section && sel.row == cell_index->row;
}

static void draw_filter_row(GContext *ctx, const Layer *cell_layer, bool selected) {
    GRect bounds = layer_get_bounds(cell_layer);
    GFont font = fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD);

#ifdef PBL_COLOR
    if (!selected) {
        if (s_filter_color != 0) {
            graphics_context_set_fill_color(ctx,
                GColorFromHEX(s_filter_color & 0x00FFFFFF));
        } else {
            graphics_context_set_fill_color(ctx, GColorDarkGray);
        }
        graphics_fill_rect(ctx, bounds, 0, GCornerNone);
    }
    // Selected: MenuLayer fills with highlight color (CobaltBlue)
    if (selected) {
        graphics_context_set_text_color(ctx, GColorWhite);
    } else if (s_filter_text_color != 0) {
        graphics_context_set_text_color(ctx,
            GColorFromHEX(s_filter_text_color & 0x00FFFFFF));
    } else {
        graphics_context_set_text_color(ctx, GColorWhite);
    }
#else
    graphics_context_set_text_color(ctx, selected ? GColorWhite : GColorBlack);
#endif

    graphics_draw_text(ctx, s_filter_name, font,
                       GRect(4, 2, bounds.size.w - 8, bounds.size.h - 4),
                       GTextOverflowModeTrailingEllipsis,
                       GTextAlignmentCenter, NULL);
}

static void draw_row(GContext *ctx, const Layer *cell_layer,
                     MenuIndex *cell_index, void *data) {
    bool selected = is_row_selected(cell_index);

    if (cell_index->row == FILTER_ROW) {
        draw_filter_row(ctx, cell_layer, selected);
        return;
    }

    GRect bounds = layer_get_bounds(cell_layer);

    if (s_num_items == 0) {
        menu_cell_basic_draw(ctx, cell_layer, "No tasks", NULL, NULL);
        return;
    }

    int item_idx = cell_index->row - 1;
    if (item_idx >= s_num_items) return;
    UiItem *item = &s_items[item_idx];

    if (item->type == UI_TYPE_HEADER) {
        // Section header with collapse indicator
        char header[MAX_TITLE_LEN + 4];
        snprintf(header, sizeof(header), "%s %s",
                 item->collapsed ? ">" : "v", item->title);

        GFont font = fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD);

#ifdef PBL_COLOR
        if (!selected) {
            graphics_context_set_fill_color(ctx, GColorLightGray);
            graphics_fill_rect(ctx, bounds, 0, GCornerNone);
        }
        // Selected: MenuLayer highlight (CobaltBlue) shows through
        graphics_context_set_text_color(ctx, selected ? GColorWhite : GColorBlack);
#else
        graphics_context_set_text_color(ctx, selected ? GColorWhite : GColorBlack);
#endif
        graphics_draw_text(ctx, header, font,
                           GRect(4, 2, bounds.size.w - 8, bounds.size.h - 4),
                           GTextOverflowModeTrailingEllipsis,
                           GTextAlignmentLeft, NULL);
    } else {
        // Task row
#ifdef PBL_COLOR
        // Priority color bar on left edge
        GColor pcolor = protocol_priority_color(item->priority);
        graphics_context_set_fill_color(ctx, pcolor);
        graphics_fill_rect(ctx, GRect(0, 0, 6, bounds.size.h), 0, GCornerNone);
#endif

        // Build display string
        char display[80];
        const char *prefix = "";
#ifndef PBL_COLOR
        prefix = protocol_priority_prefix(item->priority);
#endif

        snprintf(display, sizeof(display), "%s%s%s",
                 item->completed ? "[x] " : "[ ] ",
                 prefix,
                 item->title);

        GFont font = fonts_get_system_font(FONT_KEY_GOTHIC_18);
        GColor text_color = PBL_IF_COLOR_ELSE(
            item->completed ? GColorDarkGray : GColorBlack,
            GColorBlack);
        graphics_context_set_text_color(ctx, text_color);

        int x_offset = PBL_IF_COLOR_ELSE(10, 4);
        graphics_draw_text(ctx, display, font,
                           GRect(x_offset, 0, bounds.size.w - x_offset - 4, 22),
                           GTextOverflowModeTrailingEllipsis,
                           GTextAlignmentLeft, NULL);

        // Subtitle (timestamp)
        if (item->extra[0]) {
            GFont small_font = fonts_get_system_font(FONT_KEY_GOTHIC_14);
            graphics_context_set_text_color(ctx, PBL_IF_COLOR_ELSE(GColorDarkGray, GColorBlack));
            graphics_draw_text(ctx, item->extra, small_font,
                               GRect(x_offset, 20, bounds.size.w - x_offset - 4, 18),
                               GTextOverflowModeTrailingEllipsis,
                               GTextAlignmentLeft, NULL);
        }
    }
}

static void selection_changed(MenuLayer *menu_layer, MenuIndex new_index,
                              MenuIndex old_index, void *data) {
    // Prefetch next page when scrolling near the bottom (account for filter row offset)
    int item_idx = (int)new_index.row - 1;
    if (s_num_items > PREFETCH_THRESHOLD &&
        item_idx >= (s_num_items - PREFETCH_THRESHOLD) &&
        s_num_items < s_total_items &&
        !s_loading_more && !s_refreshing) {
        request_next_page();
    }
}

static void select_click(MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
    if (cell_index->row == FILTER_ROW) {
        menu_window_push(on_filter_selected);
        return;
    }

    int item_idx = cell_index->row - 1;
    if (item_idx < 0 || item_idx >= s_num_items) return;

    UiItem *item = &s_items[item_idx];

    if (item->type == UI_TYPE_HEADER) {
        protocol_send_toggle_group(item->id_high, item->id_low, !item->collapsed);
    } else {
        task_view_window_push(item->id_high, item->id_low);
    }
}

static void select_long_click(MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
    if (cell_index->row == FILTER_ROW) return;

    int item_idx = cell_index->row - 1;
    if (item_idx < 0 || item_idx >= s_num_items) return;

    UiItem *item = &s_items[item_idx];

    if (item->type == UI_TYPE_TASK) {
        // Complete/uncomplete task
        protocol_send_complete_task(item->id_high, item->id_low, !item->completed);
        item->completed = !item->completed;
        menu_layer_reload_data(s_menu_layer);
    }
}

static void save_filter(void) {
    persist_write_string(PERSIST_FILTER_ID, s_filter_id);
    persist_write_string(PERSIST_FILTER_NAME, s_filter_name);
    persist_write_int(PERSIST_FILTER_COLOR, (int32_t)s_filter_color);
    persist_write_int(PERSIST_FILTER_TEXT_COLOR, (int32_t)s_filter_text_color);
}

static void load_filter(void) {
    if (persist_exists(PERSIST_FILTER_NAME)) {
        persist_read_string(PERSIST_FILTER_ID, s_filter_id, MAX_FILTER_ID_LEN);
        persist_read_string(PERSIST_FILTER_NAME, s_filter_name, MAX_TITLE_LEN);
        s_filter_color = (uint32_t)persist_read_int(PERSIST_FILTER_COLOR);
        s_filter_text_color = (uint32_t)persist_read_int(PERSIST_FILTER_TEXT_COLOR);
    }
}

static void on_filter_selected(const char *filter_id, const char *filter_name, uint32_t color, uint32_t text_color) {
    if (filter_id) {
        strncpy(s_filter_id, filter_id, MAX_FILTER_ID_LEN - 1);
        s_filter_id[MAX_FILTER_ID_LEN - 1] = '\0';
    } else {
        s_filter_id[0] = '\0';
    }
    if (filter_name) {
        strncpy(s_filter_name, filter_name, MAX_TITLE_LEN - 1);
        s_filter_name[MAX_TITLE_LEN - 1] = '\0';
    }
    s_filter_color = color;
    s_filter_text_color = text_color;
    save_filter();
    // Full reload from position 0 with new filter
    request_tasks_page(0, PAGE_SIZE);
}

// Window handlers

static void window_load(Window *window) {
    load_filter();

    Layer *window_layer = window_get_root_layer(window);
    GRect bounds = layer_get_bounds(window_layer);

    s_menu_layer = menu_layer_create(bounds);
    menu_layer_set_callbacks(s_menu_layer, NULL, (MenuLayerCallbacks){
        .get_num_rows = get_num_rows,
        .get_cell_height = get_cell_height,
        .draw_row = draw_row,
        .select_click = select_click,
        .select_long_click = select_long_click,
        .selection_changed = selection_changed,
    });
    menu_layer_set_click_config_onto_window(s_menu_layer, window);

#ifdef PBL_COLOR
    menu_layer_set_normal_colors(s_menu_layer, GColorWhite, GColorBlack);
    menu_layer_set_highlight_colors(s_menu_layer, GColorCobaltBlue, GColorWhite);
#endif

    layer_add_child(window_layer, menu_layer_get_layer(s_menu_layer));

    // Loading indicator
    s_loading_layer = text_layer_create(
        GRect(0, bounds.size.h / 2 - 15, bounds.size.w, 30));
    text_layer_set_text(s_loading_layer, "Loading...");
    text_layer_set_text_alignment(s_loading_layer, GTextAlignmentCenter);
    text_layer_set_font(s_loading_layer,
                        fonts_get_system_font(FONT_KEY_GOTHIC_18));
    layer_add_child(window_layer, text_layer_get_layer(s_loading_layer));

    // Request first page
    free_items();
    s_total_items = 0;
    request_tasks_page(0, PAGE_SIZE);
}

static void window_unload(Window *window) {
    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }
    menu_layer_destroy(s_menu_layer);
    text_layer_destroy(s_loading_layer);
    free_items();
    free_pending();
    s_refreshing = false;
}

// Internal helpers

static void show_loading(bool show) {
    s_loading = show;
    layer_set_hidden(text_layer_get_layer(s_loading_layer), !show);
    layer_set_hidden(menu_layer_get_layer(s_menu_layer), show && s_num_items == 0);
}

static void request_tasks_page(int position, int limit) {
    s_expected_chunks = 0;
    s_received_chunks = 0;

    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }

    if (position == 0) {
        free_items();
        s_total_items = 0;
        s_loading_more = false;
        show_loading(true);
    } else {
        // Loading more
        s_loading_more = true;
    }

    protocol_send_get_tasks(s_filter_id[0] ? s_filter_id : NULL, position, limit);
    s_request_txn = protocol_get_active_transaction_id();

    s_chunk_timer = app_timer_register(5000, chunk_timeout_handler, NULL);
}

static void request_next_page(void) {
    if (s_loading_more || s_loading || s_num_items >= s_total_items) {
        return;
    }
    int remaining = s_total_items - s_num_items;
    int limit = remaining < PAGE_SIZE ? remaining : PAGE_SIZE;
    APP_LOG(APP_LOG_LEVEL_DEBUG, "Fetching next page: pos=%d limit=%d", s_num_items, limit);
    request_tasks_page(s_num_items, limit);
}

static void page_complete(void) {
    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }

    if (s_refreshing) {
        // Swap pending buffer into active
        free_items();
        s_items = s_pending;
        s_items_capacity = s_pending_capacity;
        s_num_items = s_pending_count;
        s_total_items = s_pending_total;

        // Detach pending (now owned by s_items)
        s_pending = NULL;
        s_pending_capacity = 0;
        s_pending_count = 0;
        s_pending_total = 0;
        s_refreshing = false;

        menu_layer_reload_data(s_menu_layer);

        // Restore scroll position, clamped to new bounds (account for filter row)
        uint16_t max_row = s_num_items > 0 ? (uint16_t)s_num_items : 1;
        if (s_saved_selection.row > max_row) {
            s_saved_selection.row = max_row;
        }
        menu_layer_set_selected_index(s_menu_layer, s_saved_selection,
                                      MenuRowAlignCenter, false);
        return;
    }

    bool was_initial = !s_loading_more;
    s_loading_more = false;

    if (was_initial) {
        show_loading(false);
    }

    menu_layer_reload_data(s_menu_layer);

    if (was_initial && s_num_items > 0) {
        // Select first task item (row 1), not the filter header (row 0)
        // Use MenuRowAlignNone so the filter header at row 0 stays visible
        menu_layer_set_selected_index(s_menu_layer,
                                      (MenuIndex){0, 1},
                                      MenuRowAlignNone, false);
    }
}

static void chunk_timeout_handler(void *data) {
    s_chunk_timer = NULL;
    if (s_loading || s_loading_more || s_refreshing) {
        APP_LOG(APP_LOG_LEVEL_WARNING, "Chunk timeout, showing %d items",
                s_refreshing ? s_pending_count : s_num_items);
        page_complete();
    }
}

// Public API

void task_list_window_push(void) {
    s_window = window_create();
    window_set_window_handlers(s_window, (WindowHandlers){
        .load = window_load,
        .unload = window_unload,
    });
    window_stack_push(s_window, true);
}

void task_list_window_refresh(void) {
    if (s_loading || s_loading_more || s_refreshing) return;

    // If we already have data, do a background refresh
    if (s_num_items > 0) {
        s_saved_selection = menu_layer_get_selected_index(s_menu_layer);
        s_refreshing = true;

        free_pending();
        s_expected_chunks = 0;
        s_received_chunks = 0;

        if (s_chunk_timer) {
            app_timer_cancel(s_chunk_timer);
            s_chunk_timer = NULL;
        }

        protocol_send_get_tasks(s_filter_id[0] ? s_filter_id : NULL, 0, PAGE_SIZE);
        s_request_txn = protocol_get_active_transaction_id();
        s_chunk_timer = app_timer_register(5000, chunk_timeout_handler, NULL);
    } else {
        // No existing data -- normal initial load
        request_tasks_page(0, PAGE_SIZE);
    }
}

void task_list_handle_tasks_response(DictionaryIterator *iter) {
    Tuple *txn_t = dict_find(iter, KEY_TRANSACTION_ID);
    if (txn_t && (uint8_t)txn_t->value->uint32 != s_request_txn) {
        APP_LOG(APP_LOG_LEVEL_DEBUG, "Discarding stale tasks response");
        return;
    }

    Tuple *chunk_count_t = dict_find(iter, KEY_CHUNK_COUNT);
    s_expected_chunks = chunk_count_t ? (int)chunk_count_t->value->uint32 : 1;

    if (s_refreshing) {
        // Accumulate into pending buffer
        Tuple *total_t = dict_find(iter, KEY_TOTAL_ITEMS);
        if (total_t) {
            s_pending_total = (int)total_t->value->uint32;
        }

        if (!ensure_capacity_for(&s_pending, &s_pending_capacity,
                                 s_pending_count + CHUNK_SIZE)) {
            page_complete();
            return;
        }

        int space = s_pending_capacity - s_pending_count;
        int parsed = protocol_parse_items(iter, &s_pending[s_pending_count], space);
        s_pending_count += parsed;
    } else {
        // Normal load (initial or next page)
        Tuple *total_t = dict_find(iter, KEY_TOTAL_ITEMS);
        if (total_t) {
            s_total_items = (int)total_t->value->uint32;
        }

        if (!ensure_capacity_for(&s_items, &s_items_capacity,
                                 s_num_items + CHUNK_SIZE)) {
            page_complete();
            return;
        }

        int space = s_items_capacity - s_num_items;
        int parsed = protocol_parse_items(iter, &s_items[s_num_items], space);
        s_num_items += parsed;
    }

    s_received_chunks++;

    // Reset chunk timeout
    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
    }
    s_chunk_timer = app_timer_register(5000, chunk_timeout_handler, NULL);

    if (s_received_chunks >= s_expected_chunks) {
        page_complete();
    }
}

void task_list_handle_complete_response(DictionaryIterator *iter) {
    task_list_window_refresh();
}

void task_list_handle_toggle_response(DictionaryIterator *iter) {
    task_list_window_refresh();
}
