#include "task_view_window.h"
#include "protocol.h"

static Window *s_window;
static ScrollLayer *s_scroll_layer;
static TextLayer *s_title_layer;
static TextLayer *s_status_layer;
static TextLayer *s_desc_layer;
static TextLayer *s_loading_layer;

static TaskDetail s_task;
static uint32_t s_id_high;
static uint32_t s_id_low;
static bool s_loading = false;

static char s_status_text[80];

static void update_display(void);

static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
    if (s_loading) return;

    protocol_send_complete_task(s_id_high, s_id_low, !s_task.completed);
    // Optimistic update
    s_task.completed = !s_task.completed;
    update_display();
}

static void click_config_provider(void *context) {
    window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
}

static void window_load(Window *window) {
    Layer *window_layer = window_get_root_layer(window);
    GRect bounds = layer_get_bounds(window_layer);
    int margin = PBL_IF_ROUND_ELSE(10, 8);
    int top_y = PBL_IF_ROUND_ELSE(40, 5);
    int width = bounds.size.w - margin * 2;

    // Scroll layer for content
    s_scroll_layer = scroll_layer_create(bounds);
    scroll_layer_set_click_config_onto_window(s_scroll_layer, window);
    scroll_layer_set_callbacks(s_scroll_layer, (ScrollLayerCallbacks){
        .click_config_provider = click_config_provider,
    });
    layer_add_child(window_layer, scroll_layer_get_layer(s_scroll_layer));

    // Title
    s_title_layer = text_layer_create(GRect(margin, top_y, width, 60));
    text_layer_set_font(s_title_layer,
                        fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD));
    text_layer_set_overflow_mode(s_title_layer, GTextOverflowModeWordWrap);
    scroll_layer_add_child(s_scroll_layer, text_layer_get_layer(s_title_layer));

    // Status
    s_status_layer = text_layer_create(GRect(margin, 70, width, 50));
    text_layer_set_font(s_status_layer,
                        fonts_get_system_font(FONT_KEY_GOTHIC_18));
    text_layer_set_overflow_mode(s_status_layer, GTextOverflowModeWordWrap);
    scroll_layer_add_child(s_scroll_layer,
                           text_layer_get_layer(s_status_layer));

    // Description
    s_desc_layer = text_layer_create(GRect(margin, 125, width, 300));
    text_layer_set_font(s_desc_layer,
                        fonts_get_system_font(FONT_KEY_GOTHIC_18));
    text_layer_set_overflow_mode(s_desc_layer, GTextOverflowModeWordWrap);
    scroll_layer_add_child(s_scroll_layer,
                           text_layer_get_layer(s_desc_layer));

#ifdef PBL_ROUND
    text_layer_enable_screen_text_flow_and_paging(s_title_layer, 8);
    text_layer_enable_screen_text_flow_and_paging(s_status_layer, 8);
    text_layer_enable_screen_text_flow_and_paging(s_desc_layer, 8);
#endif

    // Loading indicator
    s_loading_layer = text_layer_create(
        GRect(0, bounds.size.h / 2 - 15, bounds.size.w, 30));
    text_layer_set_text(s_loading_layer, "Loading...");
    text_layer_set_text_alignment(s_loading_layer, GTextAlignmentCenter);
    text_layer_set_font(s_loading_layer,
                        fonts_get_system_font(FONT_KEY_GOTHIC_18));
    layer_add_child(window_layer, text_layer_get_layer(s_loading_layer));

    // Start loading
    s_loading = true;
    layer_set_hidden(scroll_layer_get_layer(s_scroll_layer), true);
    protocol_send_get_task(s_id_high, s_id_low);
}

static void window_unload(Window *window) {
    text_layer_destroy(s_title_layer);
    text_layer_destroy(s_status_layer);
    text_layer_destroy(s_desc_layer);
    text_layer_destroy(s_loading_layer);
    scroll_layer_destroy(s_scroll_layer);
    window_destroy(s_window);
    s_window = NULL;
}

static void update_display(void) {
    if (!s_window) return;

    text_layer_set_text(s_title_layer, s_task.title);

#ifdef PBL_COLOR
    GColor pcolor = protocol_priority_color(s_task.priority);
    text_layer_set_text_color(s_title_layer,
                              gcolor_equal(pcolor, GColorDarkGray) ? GColorBlack : pcolor);
#endif

    // Priority text
    const char *priority_str;
    switch (s_task.priority) {
        case PRIORITY_HIGH:   priority_str = "High"; break;
        case PRIORITY_MEDIUM: priority_str = "Medium"; break;
        case PRIORITY_LOW:    priority_str = "Low"; break;
        default:              priority_str = "None"; break;
    }

    snprintf(s_status_text, sizeof(s_status_text), "%s%s\nPriority: %s",
             s_task.completed ? "Completed" : "Active",
             s_task.repeating ? " (repeating)" : "",
             priority_str);
    text_layer_set_text(s_status_layer, s_status_text);

    if (s_task.description[0]) {
        text_layer_set_text(s_desc_layer, s_task.description);
    } else {
        text_layer_set_text(s_desc_layer, "");
    }

    // Recalculate layout based on content sizes
    Layer *window_layer = window_get_root_layer(s_window);
    GRect bounds = layer_get_bounds(window_layer);
    int margin = PBL_IF_ROUND_ELSE(10, 8);
    int top_y = PBL_IF_ROUND_ELSE(40, 5);
    int width = bounds.size.w - margin * 2;

    GSize title_size = text_layer_get_content_size(s_title_layer);
    int title_h = title_size.h + 10;

    layer_set_frame(text_layer_get_layer(s_status_layer),
                    GRect(margin, top_y + title_h, width, 50));

    GSize status_size = text_layer_get_content_size(s_status_layer);
    int status_h = status_size.h + 10;

    int desc_y = top_y + title_h + status_h;
    layer_set_frame(text_layer_get_layer(s_desc_layer),
                    GRect(margin, desc_y, width, 300));

    GSize desc_size = text_layer_get_content_size(s_desc_layer);
    int total_height = desc_y + desc_size.h + 20;

    scroll_layer_set_content_size(s_scroll_layer,
                                  GSize(bounds.size.w, total_height));
}

// Public API

void task_view_window_push(uint32_t id_high, uint32_t id_low) {
    s_id_high = id_high;
    s_id_low = id_low;

    s_window = window_create();
    window_set_window_handlers(s_window, (WindowHandlers){
        .load = window_load,
        .unload = window_unload,
    });
    window_stack_push(s_window, true);
}

void task_view_handle_task_response(DictionaryIterator *iter) {
    if (!s_window) return;

    protocol_parse_task_detail(iter, &s_task);
    s_loading = false;

    layer_set_hidden(text_layer_get_layer(s_loading_layer), true);
    layer_set_hidden(scroll_layer_get_layer(s_scroll_layer), false);

    update_display();
}

void task_view_handle_complete_response(DictionaryIterator *iter) {
    // Completion handled optimistically in select_click_handler
}
