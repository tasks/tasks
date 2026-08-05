#include "menu_window.h"
#include "protocol.h"

#ifdef SCREENSHOT_MODE
#include "screenshot_data.h"
#endif

static Window *s_window;
static MenuLayer *s_menu_layer;
static TextLayer *s_loading_layer;

// Sliding window of filters — s_num_lists items starting at s_window_start
// within a full list of s_total_lists.
static ListItem *s_lists = NULL;
static int s_lists_capacity = 0;
static int s_num_lists = 0;
static int s_total_lists = 0;
static int s_window_start = 0;
static bool s_loading = false;

// Page-up chunks accumulate here, then get prepended once the page is complete
static ListItem *s_pending = NULL;
static int s_pending_capacity = 0;
static int s_pending_count = 0;

static FilterSelectedCallback s_callback = NULL;

static int s_expected_chunks = 0;
// Width of s_received_chunk_mask — a chunk index at or past this would shift
// out of the mask entirely
#define MAX_CHUNKS 32
static uint32_t s_received_chunk_mask = 0;
// Items parsed out of each chunk of the page in flight. A chunk that arrives
// ahead of an earlier one must not advance s_num_lists over the slots that
// earlier chunk will fill, or those rows draw whatever realloc handed back.
static uint8_t s_chunk_counts[MAX_CHUNKS];
static uint8_t s_request_txn = 0;
static AppTimer *s_chunk_timer = NULL;
static bool s_paging_up = false;
static int s_page_position = 0;
// Absolute list position shown at row 0 — see the same field in
// task_list_window.c. Rows are absolute and unloaded positions draw as
// placeholders, so sliding the window never renumbers rows under the cursor.
static int s_base = 0;
// Set while reloading/re-seating the selection — see the same flag in
// task_list_window.c for why re-entry here is destructive
static bool s_updating_selection = false;

// Absolute position of the header whose collapse we asked the phone to toggle,
// and the transaction it belongs to. Only one toggle can be outstanding: the
// position is what tells the response where to truncate, so a second tap would
// overwrite it and the first response would then cut the window at the wrong row.
static int s_toggle_position = -1;
static uint8_t s_toggle_txn = 0;
static AppTimer *s_toggle_timer = NULL;

static void show_loading(bool show);
// reset_view starts a fresh list from the top; otherwise the current base,
// total and selection are left alone and the window is refilled in place
static void request_lists_page(int position, int limit, bool reset_view);
static void chunk_timeout_handler(void *data);
static void toggle_timeout_handler(void *data);
static void page_complete(void);

static void clear_pending_toggle(void) {
    s_toggle_position = -1;
    if (s_toggle_timer) {
        app_timer_cancel(s_toggle_timer);
        s_toggle_timer = NULL;
    }
}

// Absolute list position for a MenuLayer row
static int row_to_list(int row) {
    return row < 0 ? -1 : s_base + row;
}

// Index into the loaded window for a MenuLayer row, or -1 if not loaded
static int row_to_window(int row) {
    int list_idx = row_to_list(row);
    if (list_idx < 0) return -1;
    int w = list_idx - s_window_start;
    if (w >= 0 && w < s_num_lists) return w;
    return -1;
}

// Rows the MenuLayer currently spans, clamped to the addressable span
static int visible_rows(void) {
    int visible = s_total_lists - s_base;
    if (visible < 0) visible = 0;
    if (visible > MAX_ROWS) visible = MAX_ROWS;
    return visible;
}

static bool ensure_capacity_for(ListItem **items, int *capacity, int needed) {
    if (needed <= *capacity) return true;

    int new_cap = ((needed + PAGE_SIZE - 1) / PAGE_SIZE) * PAGE_SIZE;
    ListItem *new_items = (ListItem *)realloc(*items, new_cap * sizeof(ListItem));
    if (!new_items) {
        new_items = (ListItem *)realloc(*items, needed * sizeof(ListItem));
        if (!new_items) {
            APP_LOG(APP_LOG_LEVEL_ERROR, "Failed to allocate for %d lists", needed);
            return false;
        }
        new_cap = needed;
    }
    *items = new_items;
    *capacity = new_cap;
    return true;
}

static void free_lists(void) {
    free(s_lists);
    s_lists = NULL;
    s_lists_capacity = 0;
    s_num_lists = 0;
    s_window_start = 0;
}

static void free_pending(void) {
    free(s_pending);
    s_pending = NULL;
    s_pending_capacity = 0;
    s_pending_count = 0;
}

// Trim the window to MAX_LISTS items, dropping from the far end. Rows are
// absolute, so trimmed positions simply revert to placeholders.
static void trim_window(bool dropped_top) {
    if (s_num_lists <= MAX_LISTS) return;

    int excess = s_num_lists - MAX_LISTS;
    if (dropped_top) {
        memmove(s_lists, &s_lists[excess], MAX_LISTS * sizeof(ListItem));
        s_window_start += excess;
    }
    s_num_lists = MAX_LISTS;
}

// MenuLayer callbacks

static uint16_t get_num_rows(MenuLayer *menu_layer, uint16_t section_index,
                             void *data) {
    if (s_loading && s_num_lists == 0) return 0;
    if (s_total_lists > 0) return (uint16_t)visible_rows();
    return 1; // "No filters"
}

static int16_t get_cell_height(MenuLayer *menu_layer, MenuIndex *cell_index,
                               void *data) {
    if (s_total_lists == 0) return 44;
    int w = row_to_window(cell_index->row);
    if (w >= 0) {
        return s_lists[w].type == UI_TYPE_HEADER ? 28 : 44;
    }
    return 44; // placeholder row
}

static bool is_row_selected(MenuIndex *cell_index) {
    MenuIndex sel = menu_layer_get_selected_index(s_menu_layer);
    return sel.section == cell_index->section && sel.row == cell_index->row;
}

static void draw_row(GContext *ctx, const Layer *cell_layer,
                     MenuIndex *cell_index, void *data) {
    GRect bounds = layer_get_bounds(cell_layer);

    if (s_total_lists == 0) {
        menu_cell_basic_draw(ctx, cell_layer, "No filters", NULL, NULL);
        return;
    }

    int w = row_to_window(cell_index->row);
    if (w < 0) {
        // Position exists but isn't loaded yet — draw a placeholder
        GFont font = fonts_get_system_font(FONT_KEY_GOTHIC_14);
        graphics_context_set_text_color(ctx,
            PBL_IF_COLOR_ELSE(GColorDarkGray, GColorBlack));
        graphics_draw_text(ctx, "...", font,
                           GRect(4, 5, bounds.size.w - 8, bounds.size.h - 5),
                           GTextOverflowModeTrailingEllipsis,
                           GTextAlignmentCenter, NULL);
        return;
    }
    ListItem *item = &s_lists[w];
    bool selected = is_row_selected(cell_index);

    int inset = PBL_IF_ROUND_ELSE(14, 4);

    if (item->type == UI_TYPE_HEADER) {
        char header[MAX_TITLE_LEN + 4];
        snprintf(header, sizeof(header), "%s %s",
                 item->collapsed ? ">" : "v", item->title);

        GFont font = fonts_get_system_font(FONT_KEY_GOTHIC_14_BOLD);

#ifdef PBL_COLOR
        if (!selected) {
            graphics_context_set_fill_color(ctx, GColorLightGray);
            graphics_fill_rect(ctx, bounds, 0, GCornerNone);
        }
        graphics_context_set_text_color(ctx, selected ? GColorWhite : GColorBlack);
#else
        graphics_context_set_text_color(ctx, selected ? GColorWhite : GColorBlack);
#endif
        graphics_draw_text(ctx, header, font,
                           GRect(inset, 2, bounds.size.w - inset * 2, bounds.size.h - 4),
                           GTextOverflowModeTrailingEllipsis,
                           GTextAlignmentLeft, NULL);
    } else {
        // Filter item with color bar
#ifdef PBL_COLOR
        int color_bar_x = PBL_IF_ROUND_ELSE(inset - 6, 0);
        if (item->color != 0) {
            GColor color = GColorFromHEX(item->color & 0x00FFFFFF);
            graphics_context_set_fill_color(ctx, color);
            graphics_fill_rect(ctx, GRect(color_bar_x, 0, 6, bounds.size.h), 0, GCornerNone);
        }
#endif

        int x_offset = PBL_IF_ROUND_ELSE(inset + 4, PBL_IF_COLOR_ELSE(10, 4));
        GFont font = fonts_get_system_font(FONT_KEY_GOTHIC_18);
        GColor text_color = PBL_IF_COLOR_ELSE(
            selected ? GColorWhite : GColorBlack,
            selected ? GColorWhite : GColorBlack);
        graphics_context_set_text_color(ctx, text_color);
        graphics_draw_text(ctx, item->title, font,
                           GRect(x_offset, 8, bounds.size.w - x_offset - inset, 28),
                           GTextOverflowModeTrailingEllipsis,
                           GTextAlignmentLeft, NULL);
    }
}

// Shift the addressable span when the cursor nears either edge — the only
// thing that renumbers rows, and never reached by an ordinary filter list
static void maybe_rebase(int list_idx) {
    if (s_total_lists <= MAX_ROWS) return;

    bool near_end = list_idx >= s_base + MAX_ROWS - REBASE_MARGIN;
    bool near_start = list_idx < s_base + REBASE_MARGIN;
    if (!near_end && !near_start) return;

    int max_base = s_total_lists - MAX_ROWS;
    int new_base = list_idx - MAX_ROWS / 2;
    if (new_base < 0) new_base = 0;
    if (new_base > max_base) new_base = max_base;
    if (new_base == s_base) return;

#ifdef PBL_ROUND
    MenuRowAlign align = MenuRowAlignCenter;
#else
    MenuRowAlign align = new_base > s_base ? MenuRowAlignBottom : MenuRowAlignTop;
#endif
    s_base = new_base;

    s_updating_selection = true;
    menu_layer_reload_data(s_menu_layer);
    int new_row = list_idx - s_base;
    int max_row = visible_rows() - 1;
    if (new_row < 0) new_row = 0;
    if (new_row > max_row) new_row = max_row;
    menu_layer_set_selected_index(s_menu_layer,
                                  (MenuIndex){0, (uint16_t)new_row}, align, false);
    s_updating_selection = false;
}

static void selection_changed(MenuLayer *menu_layer, MenuIndex new_index,
                              MenuIndex old_index, void *data) {
    if (s_updating_selection) return;

    int list_idx = row_to_list((int)new_index.row);
    if (list_idx < 0) return;
    int window_end = s_window_start + s_num_lists;

    if (s_loading) return;

    maybe_rebase(list_idx);

    // Page down: near the end of loaded data and more filters exist below
    if (list_idx >= window_end - PREFETCH_THRESHOLD && window_end < s_total_lists) {
        int remaining = s_total_lists - window_end;
        int limit = remaining < PAGE_SIZE ? remaining : PAGE_SIZE;
        s_paging_up = false;
        request_lists_page(window_end, limit, false);
        return;
    }

    // Page up: near the start of loaded data and filters exist above
    if (list_idx < s_window_start + PREFETCH_THRESHOLD && s_window_start > 0) {
        int limit = s_window_start < PAGE_SIZE ? s_window_start : PAGE_SIZE;
        s_paging_up = true;
        request_lists_page(s_window_start - limit, limit, false);
    }
}

static void select_click(MenuLayer *menu_layer, MenuIndex *cell_index,
                         void *data) {
    int w = row_to_window(cell_index->row);
    if (w < 0) return;

    ListItem *item = &s_lists[w];

    if (item->type == UI_TYPE_HEADER) {
        // Collapse state lives on the phone so it can fold the section out of
        // the paged results — the watch can't count children it hasn't loaded.
        // Ignore the tap if a page or an earlier toggle is still outstanding,
        // rather than clobbering the position that response is going to need.
        if (s_loading || s_toggle_position >= 0) return;
        int pos = row_to_list(cell_index->row);
        if (!protocol_send_toggle_list(item->filter_id, !item->collapsed)) return;
        s_toggle_position = pos;
        s_toggle_txn = protocol_get_active_transaction_id();
        // Without this a dropped response would leave the toggle pending forever
        // and every later header tap would be ignored
        s_toggle_timer = app_timer_register(5000, toggle_timeout_handler, NULL);
    } else if (item->type == UI_TYPE_TASK) {
        if (s_callback) {
            s_callback(item->filter_id[0] ? item->filter_id : NULL,
                       item->title, item->color, item->text_color);
        }
        window_stack_pop(true);
    }
}

#ifdef SCREENSHOT_MODE
static void load_screenshot_lists(void) {
    ensure_capacity_for(&s_lists, &s_lists_capacity, MAX_LISTS);
    s_num_lists = screenshot_populate_lists(s_lists, MAX_LISTS);
    s_total_lists = s_num_lists;
    s_window_start = 0;
    show_loading(false);
    menu_layer_reload_data(s_menu_layer);
    menu_layer_set_selected_index(s_menu_layer,
        (MenuIndex){0, 1}, MenuRowAlignNone, false);
}
#endif

// Window handlers

static void window_load(Window *window) {
    Layer *window_layer = window_get_root_layer(window);
    GRect bounds = layer_get_bounds(window_layer);

    s_menu_layer = menu_layer_create(bounds);
    menu_layer_set_callbacks(s_menu_layer, NULL, (MenuLayerCallbacks){
        .get_num_rows = get_num_rows,
        .get_cell_height = get_cell_height,
        .draw_row = draw_row,
        .select_click = select_click,
        .selection_changed = selection_changed,
    });
    menu_layer_set_click_config_onto_window(s_menu_layer, window);

#ifdef PBL_COLOR
    menu_layer_set_normal_colors(s_menu_layer, GColorWhite, GColorBlack);
    menu_layer_set_highlight_colors(s_menu_layer, GColorCobaltBlue, GColorWhite);
#endif

    layer_add_child(window_layer, menu_layer_get_layer(s_menu_layer));

    s_loading_layer = text_layer_create(
        GRect(0, bounds.size.h / 2 - 15, bounds.size.w, 30));
    text_layer_set_text(s_loading_layer, "Loading...");
    text_layer_set_text_alignment(s_loading_layer, GTextAlignmentCenter);
    text_layer_set_font(s_loading_layer,
                        fonts_get_system_font(FONT_KEY_GOTHIC_18));
    layer_add_child(window_layer, text_layer_get_layer(s_loading_layer));

    free_lists();
    s_total_lists = 0;
    // A window closed mid-page-up leaves this set, which would route the initial
    // page's chunks through the prepend path — see the same reset in
    // task_list_window.c
    s_paging_up = false;
#ifdef SCREENSHOT_MODE
    load_screenshot_lists();
#else
    request_lists_page(0, INITIAL_PAGE_SIZE, true);
#endif
}

static void window_unload(Window *window) {
    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }
    clear_pending_toggle();
    menu_layer_destroy(s_menu_layer);
    text_layer_destroy(s_loading_layer);
    free_lists();
    free_pending();
    s_loading = false;
    window_destroy(s_window);
    s_window = NULL;
}

// Internal helpers

static void show_loading(bool show) {
    s_loading = show;
    layer_set_hidden(text_layer_get_layer(s_loading_layer), !show);
    layer_set_hidden(menu_layer_get_layer(s_menu_layer), show && s_num_lists == 0);
}

static void request_lists_page(int position, int limit, bool reset_view) {
    if (s_loading) return;

    s_expected_chunks = 0;
    s_received_chunk_mask = 0;
    memset(s_chunk_counts, 0, sizeof(s_chunk_counts));
    // Chunks overwrite from offset 0, so a count left over from a page that
    // timed out part-way would prepend stale tail items on the next page up
    s_pending_count = 0;
    s_page_position = position;

    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }

    if (reset_view && s_num_lists == 0) {
        free_lists();
        s_total_lists = 0;
        s_window_start = position;
        s_base = 0;
        show_loading(true);
    } else {
        s_loading = true;
    }

    if (protocol_send_get_lists(position, limit)) {
        s_request_txn = protocol_get_active_transaction_id();
        s_chunk_timer = app_timer_register(5000, chunk_timeout_handler, NULL);
    } else {
        s_loading = false;
        if (s_num_lists == 0) {
            show_loading(false);
        }
    }
}

static void page_complete(void) {
    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }

    s_loading = false;
    show_loading(false);

    // Rows are absolute, so newly loaded positions just stop being
    // placeholders — the selection does not move and needs no correction
    s_updating_selection = true;
    menu_layer_reload_data(s_menu_layer);
    s_updating_selection = false;
}

static void chunk_timeout_handler(void *data) {
    s_chunk_timer = NULL;
    if (s_loading) {
        APP_LOG(APP_LOG_LEVEL_WARNING, "List chunk timeout, showing %d filters",
                s_num_lists);
        page_complete();
    }
}

// The phone never answered. Drop the pending toggle so header taps work again —
// the collapse state stays whatever the phone decided, and the next page request
// picks it up.
static void toggle_timeout_handler(void *data) {
    s_toggle_timer = NULL;
    if (s_toggle_position >= 0) {
        APP_LOG(APP_LOG_LEVEL_WARNING, "Toggle response timeout at %d",
                s_toggle_position);
        s_toggle_position = -1;
    }
}

// Public API

void menu_window_push(FilterSelectedCallback callback) {
    s_callback = callback;
    s_window = window_create();
    window_set_window_handlers(s_window, (WindowHandlers){
        .load = window_load,
        .unload = window_unload,
    });
    window_stack_push(s_window, true);
}

void menu_window_handle_lists_response(DictionaryIterator *iter) {
    if (!s_window) return;

    Tuple *txn_t = dict_find(iter, KEY_TRANSACTION_ID);
    if (txn_t && (uint8_t)txn_t->value->uint32 != s_request_txn) {
        return;
    }

    if (!s_loading) return;

    Tuple *chunk_count_t = dict_find(iter, KEY_CHUNK_COUNT);
    s_expected_chunks = chunk_count_t ? (int)chunk_count_t->value->uint32 : 1;

    Tuple *chunk_index_t = dict_find(iter, KEY_CHUNK_INDEX);
    int chunk_index = chunk_index_t ? (int)chunk_index_t->value->uint32 : 0;

    if (chunk_index < 0 || chunk_index >= MAX_CHUNKS) {
        APP_LOG(APP_LOG_LEVEL_WARNING, "Ignoring chunk index %d", chunk_index);
        return;
    }

    if (s_received_chunk_mask & ((uint32_t)1 << chunk_index)) {
        return;
    }
    s_received_chunk_mask |= (uint32_t)1 << chunk_index;

    Tuple *total_t = dict_find(iter, KEY_TOTAL_ITEMS);
    if (total_t) {
        s_total_lists = (int)total_t->value->uint32;
    }

    int chunk_offset = chunk_index * CHUNK_SIZE;

    if (s_paging_up) {
        int needed = chunk_offset + CHUNK_SIZE;
        if (!ensure_capacity_for(&s_pending, &s_pending_capacity, needed)) {
            page_complete();
            return;
        }
        int space = s_pending_capacity - chunk_offset;
        int parsed = protocol_parse_list_items(iter, &s_pending[chunk_offset], space);
        int end = chunk_offset + parsed;
        if (end > s_pending_count) {
            s_pending_count = end;
        }
    } else {
        // Append: items land after the current window
        int base_offset = s_page_position - s_window_start;
        if (base_offset < 0) base_offset = 0;
        int abs_offset = base_offset + chunk_offset;

        int needed = abs_offset + CHUNK_SIZE;
        if (!ensure_capacity_for(&s_lists, &s_lists_capacity, needed)) {
            page_complete();
            return;
        }

        int space = s_lists_capacity - abs_offset;
        int parsed = protocol_parse_list_items(iter, &s_lists[abs_offset], space);
        s_chunk_counts[chunk_index] = (uint8_t)parsed;

        // Extend only over the leading run of chunks that have actually landed.
        // Rows past it stay outside the window and draw as placeholders until
        // the missing chunk fills them in.
        int contiguous = 0;
        while (contiguous < s_expected_chunks &&
               (s_received_chunk_mask & ((uint32_t)1 << contiguous))) {
            contiguous++;
        }
        if (contiguous > 0) {
            int end = base_offset + (contiguous - 1) * CHUNK_SIZE +
                      s_chunk_counts[contiguous - 1];
            if (end > s_num_lists) {
                s_num_lists = end;
            }
        }

        trim_window(true);
    }

    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
    }
    s_chunk_timer = app_timer_register(5000, chunk_timeout_handler, NULL);

    uint32_t expected_mask = ((uint32_t)1 << s_expected_chunks) - 1;
    if ((s_received_chunk_mask & expected_mask) == expected_mask) {
        if (s_paging_up && s_pending_count > 0) {
            int total = s_pending_count + s_num_lists;
            if (ensure_capacity_for(&s_lists, &s_lists_capacity, total)) {
                memmove(&s_lists[s_pending_count], s_lists,
                        s_num_lists * sizeof(ListItem));
                memcpy(s_lists, s_pending, s_pending_count * sizeof(ListItem));
                s_num_lists = total;
                s_window_start = s_page_position;
                trim_window(false);
            }
            free_pending();
        }
        page_complete();
    }
}

void menu_window_handle_toggle_response(DictionaryIterator *iter) {
    if (!s_window) return;

    // A stale or duplicated response carries an old transaction — acting on it
    // would truncate the window using a position that no longer applies
    Tuple *txn_t = dict_find(iter, KEY_TRANSACTION_ID);
    if (txn_t && (uint8_t)txn_t->value->uint32 != s_toggle_txn) {
        return;
    }

    // select_click only ever stores a position >= 0, so a negative one means the
    // toggle already timed out and there is nothing left to reconcile. Falling
    // through would throw the user back to the top of the list, and would clear
    // s_loading out from under whatever page request is now in flight. The
    // phone's collapse state arrives with the next page either way.
    if (s_toggle_position < 0) {
        return;
    }

    int pos = s_toggle_position;
    clear_pending_toggle();

    free_pending();
    s_paging_up = false;
    s_loading = false;

    // Folding a section only moves the positions below its header — the header
    // itself and everything above it stay exactly where they were. Keep that
    // part of the window, drop the rest, and refetch from the header down. The
    // base doesn't move, so the cursor stays on the header the user just hit.
    // The header is refetched too, to pick up its new collapsed flag.
    int keep = pos - s_window_start;
    if (keep <= 0) {
        keep = 0;
        s_window_start = pos;
    }
    if (keep > s_num_lists) keep = s_num_lists;
    s_num_lists = keep;

    request_lists_page(pos, REFRESH_LIST_SIZE, false);
}
