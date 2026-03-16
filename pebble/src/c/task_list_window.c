#include "task_list_window.h"
#include "protocol.h"
#include "task_view_window.h"
#include "menu_window.h"
#include "settings_window.h"

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
static bool s_refresh_pending = false;
static bool s_first_load = true;
static MenuIndex s_saved_selection;

// Current filter
static char s_filter_id[MAX_FILTER_ID_LEN] = {0};
static char s_filter_name[MAX_TITLE_LEN] = "My Tasks";
static uint32_t s_filter_color = 0;
static uint32_t s_filter_text_color = 0;

// Chunk assembly for current page request
static int s_expected_chunks = 0;
static uint32_t s_received_chunk_mask = 0;
static uint8_t s_request_txn = 0;
static AppTimer *s_chunk_timer = NULL;

// Fixed rows at top of list
#define FILTER_ROW 0
#ifdef PBL_MICROPHONE
#define ADD_ROW 1
#define TASK_ROW_OFFSET 2
#else
#define TASK_ROW_OFFSET 1
#endif

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

#ifdef PBL_MICROPHONE
static DictationSession *s_dictation_session;
static void start_dictation(void);
static void dictation_callback(DictationSession *session, DictationSessionStatus status,
                               char *transcription, void *context);
#endif

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
    if (s_num_items > 0) return (uint16_t)(TASK_ROW_OFFSET + s_num_items);
    if (s_refreshing) return (uint16_t)TASK_ROW_OFFSET; // don't show "No tasks" during refresh
    return (uint16_t)(TASK_ROW_OFFSET + 1); // fixed rows + "No tasks"
}

static int16_t get_cell_height(MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
#ifdef PBL_MICROPHONE
    if (cell_index->row == ADD_ROW) return 28;
#endif
    if (cell_index->row == FILTER_ROW) return 28;
    if (s_num_items == 0) return 44; // "No tasks" row
    int item_idx = cell_index->row - TASK_ROW_OFFSET;
    if (item_idx >= 0 && item_idx < s_num_items) {
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
                       GRect(4, 5, bounds.size.w - 8, bounds.size.h - 5),
                       GTextOverflowModeTrailingEllipsis,
                       GTextAlignmentCenter, NULL);
}

static void draw_row(GContext *ctx, const Layer *cell_layer,
                     MenuIndex *cell_index, void *data) {
    bool selected = is_row_selected(cell_index);

#ifdef PBL_MICROPHONE
    if (cell_index->row == ADD_ROW) {
        GRect add_bounds = layer_get_bounds(cell_layer);
        GFont add_font = fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD);
        graphics_context_set_text_color(ctx,
            selected ? GColorWhite : PBL_IF_COLOR_ELSE(GColorDarkGray, GColorBlack));
        graphics_draw_text(ctx, "+ Add Task", add_font,
                           GRect(4, 5, add_bounds.size.w - 8, add_bounds.size.h - 5),
                           GTextOverflowModeTrailingEllipsis,
                           GTextAlignmentCenter, NULL);
        return;
    }
#endif

    if (cell_index->row == FILTER_ROW) {
        draw_filter_row(ctx, cell_layer, selected);
        return;
    }

    GRect bounds = layer_get_bounds(cell_layer);

    if (s_num_items == 0) {
        menu_cell_basic_draw(ctx, cell_layer, "No tasks", NULL, NULL);
        return;
    }

    int item_idx = cell_index->row - TASK_ROW_OFFSET;
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
                           GRect(4, 5, bounds.size.w - 8, bounds.size.h - 5),
                           GTextOverflowModeTrailingEllipsis,
                           GTextAlignmentLeft, NULL);
    } else {
        // Task row
        int padding = 6;
        int x_start = PBL_IF_COLOR_ELSE(padding, 4);
        bool has_subtitle = item->extra[0] != '\0';

        // Vertical layout: center content block in row
        int title_h = 22;
        int sub_h = has_subtitle ? 18 : 0;
        int content_h = title_h + sub_h;
        int y_off = (bounds.size.h - content_h) / 2;

        // Draw checkbox colored by priority
        int cb_size = 14;
        int cb_x = x_start;
        int cb_y = y_off + (title_h - cb_size) / 2;
        GColor cb_color = selected ? GColorWhite :
            PBL_IF_COLOR_ELSE(
                protocol_priority_color(item->priority),
                GColorBlack);
        graphics_context_set_stroke_color(ctx, cb_color);
        graphics_context_set_stroke_width(ctx, 3);
        graphics_draw_rect(ctx, GRect(cb_x, cb_y, cb_size, cb_size));

        if (item->completed) {
            // Draw checkmark
            graphics_context_set_stroke_width(ctx, 2);
            graphics_draw_line(ctx,
                GPoint(cb_x + 3, cb_y + cb_size / 2),
                GPoint(cb_x + cb_size / 2 - 1, cb_y + cb_size - 4));
            graphics_draw_line(ctx,
                GPoint(cb_x + cb_size / 2 - 1, cb_y + cb_size - 4),
                GPoint(cb_x + cb_size - 3, cb_y + 3));
        }
        graphics_context_set_stroke_width(ctx, 1);

        // Title text
        int text_x = cb_x + cb_size + padding;
        int text_w = bounds.size.w - text_x - 4;
        char display[80];
        const char *prefix = "";
#ifndef PBL_COLOR
        prefix = protocol_priority_prefix(item->priority);
#endif
        snprintf(display, sizeof(display), "%s%s", prefix, item->title);

        GFont font = fonts_get_system_font(FONT_KEY_GOTHIC_18);
        GColor text_color = selected ? GColorWhite :
            PBL_IF_COLOR_ELSE(
                item->completed ? GColorDarkGray : GColorBlack,
                GColorBlack);
        graphics_context_set_text_color(ctx, text_color);
        int text_y = y_off - 1; // compensate for font top padding
        graphics_draw_text(ctx, display, font,
                           GRect(text_x, text_y, text_w, title_h + 4),
                           GTextOverflowModeTrailingEllipsis,
                           GTextAlignmentLeft, NULL);

        // Strikethrough for completed tasks
        if (item->completed) {
            GSize text_size = graphics_text_layout_get_content_size(
                display, font,
                GRect(0, 0, text_w, title_h),
                GTextOverflowModeTrailingEllipsis,
                GTextAlignmentLeft);
            int strike_w = (int)text_size.w < text_w
                ? (int)text_size.w : text_w;
            int strike_y = y_off + title_h / 2;
            graphics_context_set_stroke_color(ctx, text_color);
            graphics_draw_line(ctx,
                GPoint(text_x, strike_y),
                GPoint(text_x + strike_w, strike_y));
        }

        // Subtitle (timestamp)
        if (has_subtitle) {
            GFont small_font = fonts_get_system_font(FONT_KEY_GOTHIC_14);
            GColor sub_color = selected ? GColorWhite :
                PBL_IF_COLOR_ELSE(GColorDarkGray, GColorBlack);
            graphics_context_set_text_color(ctx, sub_color);
            graphics_draw_text(ctx, item->extra, small_font,
                               GRect(text_x, y_off + title_h, text_w, sub_h),
                               GTextOverflowModeTrailingEllipsis,
                               GTextAlignmentLeft, NULL);
        }
    }
}

static void selection_changed(MenuLayer *menu_layer, MenuIndex new_index,
                              MenuIndex old_index, void *data) {
    // Prefetch next page when scrolling near the bottom
    int item_idx = (int)new_index.row - TASK_ROW_OFFSET;
    if (s_num_items > PREFETCH_THRESHOLD &&
        item_idx >= (s_num_items - PREFETCH_THRESHOLD) &&
        s_num_items < s_total_items &&
        !s_loading_more && !s_refreshing) {
        request_next_page();
    }
}

static void select_click(MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
#ifdef PBL_MICROPHONE
    if (cell_index->row == ADD_ROW) {
        start_dictation();
        return;
    }
#endif

    if (cell_index->row == FILTER_ROW) {
        menu_window_push(on_filter_selected);
        return;
    }

    int item_idx = cell_index->row - TASK_ROW_OFFSET;
    if (item_idx < 0 || item_idx >= s_num_items) return;

    UiItem *item = &s_items[item_idx];

    if (item->type == UI_TYPE_HEADER) {
        protocol_send_toggle_group(item->id_high, item->id_low, !item->collapsed);
    } else {
        task_view_window_push(item->id_high, item->id_low);
    }
}

static void on_settings_changed(void);

static void select_long_click(MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
    if (cell_index->row == FILTER_ROW) {
        settings_window_push(on_settings_changed);
        return;
    }
    if (cell_index->row < TASK_ROW_OFFSET) return;

    int item_idx = cell_index->row - TASK_ROW_OFFSET;
    if (item_idx < 0 || item_idx >= s_num_items) return;

    UiItem *item = &s_items[item_idx];

    if (item->type == UI_TYPE_TASK) {
        protocol_send_complete_task(item->id_high, item->id_low, !item->completed);
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
    s_first_load = true;
    request_tasks_page(0, PAGE_SIZE);
}

static void window_unload(Window *window) {
    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }
#ifdef PBL_MICROPHONE
    if (s_dictation_session) {
        dictation_session_destroy(s_dictation_session);
        s_dictation_session = NULL;
    }
#endif
    menu_layer_destroy(s_menu_layer);
    text_layer_destroy(s_loading_layer);
    free_items();
    free_pending();
    s_refreshing = false;
    s_refresh_pending = false;
}

// Internal helpers

static void show_loading(bool show) {
    s_loading = show;
    layer_set_hidden(text_layer_get_layer(s_loading_layer), !show);
    layer_set_hidden(menu_layer_get_layer(s_menu_layer), show && s_num_items == 0);
}

static void request_tasks_page(int position, int limit) {
    s_expected_chunks = 0;
    s_received_chunk_mask = 0;

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

    protocol_send_get_tasks(s_filter_id[0] ? s_filter_id : NULL, position, limit,
                            settings_get_sort_mode(), settings_get_group_mode(),
                            settings_get_show_hidden(), settings_get_show_completed());
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
        // If we timed out before receiving any chunks, keep old data and retry
        if (s_pending_count == 0 && s_pending_total != 0) {
            free_pending();
            s_refreshing = false;
            if (s_refresh_pending) {
                s_refresh_pending = false;
                task_list_window_refresh();
            }
            return;
        }

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

        // Restore scroll position, clamped to new bounds
        uint16_t max_row = s_num_items > 0
            ? (uint16_t)(TASK_ROW_OFFSET - 1 + s_num_items)
            : (uint16_t)FILTER_ROW;
        if (s_saved_selection.row > max_row) {
            s_saved_selection.row = max_row;
        }
        menu_layer_set_selected_index(s_menu_layer, s_saved_selection,
                                      MenuRowAlignCenter, false);

        if (s_refresh_pending) {
            s_refresh_pending = false;
            task_list_window_refresh();
        }
        return;
    }

    bool was_initial = !s_loading_more;
    s_loading_more = false;

    if (was_initial) {
        show_loading(false);
    }

    menu_layer_reload_data(s_menu_layer);

    if (was_initial && s_first_load) {
        s_first_load = false;
        menu_layer_set_selected_index(s_menu_layer,
                                      (MenuIndex){0, FILTER_ROW},
                                      MenuRowAlignNone, false);
    }

    if (s_refresh_pending) {
        s_refresh_pending = false;
        task_list_window_refresh();
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
    if (s_loading || s_loading_more || s_refreshing) {
        s_refresh_pending = true;
        return;
    }

    // If we already have data, do a background refresh
    if (s_num_items > 0) {
        s_saved_selection = menu_layer_get_selected_index(s_menu_layer);
        s_refreshing = true;

        free_pending();
        s_expected_chunks = 0;
        s_received_chunk_mask = 0;

        if (s_chunk_timer) {
            app_timer_cancel(s_chunk_timer);
            s_chunk_timer = NULL;
        }

        protocol_send_get_tasks(s_filter_id[0] ? s_filter_id : NULL, 0, PAGE_SIZE,
                                settings_get_sort_mode(), settings_get_group_mode(),
                                settings_get_show_hidden(), settings_get_show_completed());
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
        return;
    }

    if (!s_loading && !s_loading_more && !s_refreshing) {
        return;
    }

    Tuple *chunk_count_t = dict_find(iter, KEY_CHUNK_COUNT);
    s_expected_chunks = chunk_count_t ? (int)chunk_count_t->value->uint32 : 1;

    Tuple *chunk_index_t = dict_find(iter, KEY_CHUNK_INDEX);
    int chunk_index = chunk_index_t ? (int)chunk_index_t->value->uint32 : 0;

    // Skip already-received chunks (handles duplicate broadcasts)
    if (s_received_chunk_mask & (1 << chunk_index)) {
        return;
    }
    s_received_chunk_mask |= (1 << chunk_index);

    // Position items by chunk index, not by appending
    int chunk_offset = chunk_index * CHUNK_SIZE;

    if (s_refreshing) {
        Tuple *total_t = dict_find(iter, KEY_TOTAL_ITEMS);
        if (total_t) {
            s_pending_total = (int)total_t->value->uint32;
        }

        int needed = chunk_offset + CHUNK_SIZE;
        if (!ensure_capacity_for(&s_pending, &s_pending_capacity, needed)) {
            page_complete();
            return;
        }

        int space = s_pending_capacity - chunk_offset;
        int parsed = protocol_parse_items(iter, &s_pending[chunk_offset], space);
        int end = chunk_offset + parsed;
        if (end > s_pending_count) {
            s_pending_count = end;
        }
    } else {
        Tuple *total_t = dict_find(iter, KEY_TOTAL_ITEMS);
        if (total_t) {
            s_total_items = (int)total_t->value->uint32;
        }

        // For pagination (loading_more), offset relative to existing items
        int base = s_loading_more ? s_num_items : 0;
        int abs_offset = base + chunk_offset;

        int needed = abs_offset + CHUNK_SIZE;
        if (!ensure_capacity_for(&s_items, &s_items_capacity, needed)) {
            page_complete();
            return;
        }

        int space = s_items_capacity - abs_offset;
        int parsed = protocol_parse_items(iter, &s_items[abs_offset], space);
        int end = abs_offset + parsed;
        if (end > s_num_items) {
            s_num_items = end;
        }
    }

    // Reset chunk timeout
    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
    }
    s_chunk_timer = app_timer_register(5000, chunk_timeout_handler, NULL);

    // Check if all expected chunks received
    uint32_t expected_mask = ((uint32_t)1 << s_expected_chunks) - 1;
    if ((s_received_chunk_mask & expected_mask) == expected_mask) {
        page_complete();
    }
}

static void on_settings_changed(void) {
    task_list_window_refresh();
}

void task_list_handle_complete_response(DictionaryIterator *iter) {
    task_list_window_refresh();
}


void task_list_handle_save_response(DictionaryIterator *iter) {
    task_list_window_refresh();
}

void task_list_handle_toggle_response(DictionaryIterator *iter) {
    task_list_window_refresh();
}

#ifdef PBL_MICROPHONE
static void dictation_callback(DictationSession *session, DictationSessionStatus status,
                               char *transcription, void *context) {
    if (status == DictationSessionStatusSuccess && transcription) {
        protocol_send_save_task(transcription,
                                s_filter_id[0] ? s_filter_id : NULL);
    }
}

static void start_dictation(void) {
    if (!s_dictation_session) {
        s_dictation_session = dictation_session_create(MAX_TITLE_LEN,
                                                       dictation_callback, NULL);
    }
    if (s_dictation_session) {
        dictation_session_start(s_dictation_session);
    }
}
#endif
