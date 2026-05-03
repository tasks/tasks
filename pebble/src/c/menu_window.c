#include "menu_window.h"
#include "protocol.h"

#ifdef SCREENSHOT_MODE
#include "screenshot_data.h"
#endif

static Window *s_window;
static MenuLayer *s_menu_layer;
static TextLayer *s_loading_layer;

static ListItem s_lists[MAX_LISTS];
static int s_num_lists = 0;
static bool s_loading = false;

// Visible row mapping (collapsed headers hide their children)
static int s_visible_map[MAX_LISTS];
static int s_num_visible = 0;

static FilterSelectedCallback s_callback = NULL;

static int s_expected_chunks = 0;
static int s_received_chunks = 0;
static uint8_t s_request_txn = 0;
static AppTimer *s_chunk_timer = NULL;

static void show_loading(bool show);
static void request_lists(void);
static void chunk_timeout_handler(void *data);
static void lists_complete(void);
static void rebuild_visible_map(void);

// Rebuild the visible item list after collapse state changes
static void rebuild_visible_map(void) {
    s_num_visible = 0;
    bool hidden = false;
    for (int i = 0; i < s_num_lists; i++) {
        if (s_lists[i].type == UI_TYPE_HEADER) {
            hidden = s_lists[i].collapsed;
            s_visible_map[s_num_visible++] = i;
        } else if (!hidden) {
            s_visible_map[s_num_visible++] = i;
        }
    }
}

// MenuLayer callbacks

static uint16_t get_num_rows(MenuLayer *menu_layer, uint16_t section_index,
                             void *data) {
    if (s_num_visible > 0) return (uint16_t)s_num_visible;
    return s_loading ? 0 : 1;
}

static int16_t get_cell_height(MenuLayer *menu_layer, MenuIndex *cell_index,
                               void *data) {
    if (s_num_visible == 0) return 44;
    if (cell_index->row < (uint16_t)s_num_visible) {
        int idx = s_visible_map[cell_index->row];
        return s_lists[idx].type == UI_TYPE_HEADER ? 28 : 44;
    }
    return 44;
}

static bool is_row_selected(MenuIndex *cell_index) {
    MenuIndex sel = menu_layer_get_selected_index(s_menu_layer);
    return sel.section == cell_index->section && sel.row == cell_index->row;
}

static void draw_row(GContext *ctx, const Layer *cell_layer,
                     MenuIndex *cell_index, void *data) {
    GRect bounds = layer_get_bounds(cell_layer);

    if (s_num_visible == 0) {
        menu_cell_basic_draw(ctx, cell_layer, "No filters", NULL, NULL);
        return;
    }

    if (cell_index->row >= (uint16_t)s_num_visible) return;
    int idx = s_visible_map[cell_index->row];
    ListItem *item = &s_lists[idx];
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
        int color_bar_x = PBL_IF_ROUND_ELSE(inset - 6, 0);
#ifdef PBL_COLOR
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

static void select_click(MenuLayer *menu_layer, MenuIndex *cell_index,
                         void *data) {
    if (s_num_visible == 0 || cell_index->row >= (uint16_t)s_num_visible) return;

    int idx = s_visible_map[cell_index->row];
    ListItem *item = &s_lists[idx];

    if (item->type == UI_TYPE_HEADER) {
        item->collapsed = !item->collapsed;
        rebuild_visible_map();
        menu_layer_reload_data(s_menu_layer);
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
    s_num_lists = screenshot_populate_lists(s_lists, MAX_LISTS);
    rebuild_visible_map();
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

#ifdef SCREENSHOT_MODE
    load_screenshot_lists();
#else
    request_lists();
#endif
}

static void window_unload(Window *window) {
    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }
    menu_layer_destroy(s_menu_layer);
    text_layer_destroy(s_loading_layer);
    window_destroy(s_window);
    s_window = NULL;
}

// Internal helpers

static void show_loading(bool show) {
    s_loading = show;
    layer_set_hidden(text_layer_get_layer(s_loading_layer), !show);
    layer_set_hidden(menu_layer_get_layer(s_menu_layer), show && s_num_visible == 0);
}

static void request_lists(void) {
    s_num_lists = 0;
    s_num_visible = 0;
    s_expected_chunks = 0;
    s_received_chunks = 0;

    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }

    show_loading(true);
    protocol_send_get_lists();
    s_request_txn = protocol_get_active_transaction_id();
    s_chunk_timer = app_timer_register(5000, chunk_timeout_handler, NULL);
}

static void lists_complete(void) {
    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
        s_chunk_timer = NULL;
    }
    rebuild_visible_map();
    show_loading(false);
    menu_layer_reload_data(s_menu_layer);
}

static void chunk_timeout_handler(void *data) {
    s_chunk_timer = NULL;
    if (s_loading) {
        lists_complete();
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

    Tuple *chunk_count_t = dict_find(iter, KEY_CHUNK_COUNT);
    s_expected_chunks = chunk_count_t ? (int)chunk_count_t->value->uint32 : 1;

    int parsed = protocol_parse_list_items(iter,
                                           &s_lists[s_num_lists],
                                           MAX_LISTS - s_num_lists);
    s_num_lists += parsed;
    s_received_chunks++;

    if (s_chunk_timer) {
        app_timer_cancel(s_chunk_timer);
    }
    s_chunk_timer = app_timer_register(5000, chunk_timeout_handler, NULL);

    if (s_received_chunks >= s_expected_chunks) {
        lists_complete();
    }
}
