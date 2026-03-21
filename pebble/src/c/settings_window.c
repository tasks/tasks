#include "settings_window.h"
#include "protocol.h"

static Window *s_window;
static MenuLayer *s_menu_layer;
static SettingsChangedCallback s_callback;

// Sort/group mode table for cycling
typedef struct {
    int value;
    const char *label;
} SortOption;

static const SortOption s_group_options[] = {
    { SORT_GROUP_NONE, "None" },
    { SORT_DUE,        "Due date" },
    { SORT_START,      "Start date" },
    { SORT_IMPORTANCE, "Priority" },
    { SORT_MODIFIED,   "Modified" },
    { SORT_CREATED,    "Created" },
    { SORT_LIST,       "List" },
};
#define NUM_GROUP_OPTIONS ((int)(sizeof(s_group_options) / sizeof(s_group_options[0])))

static const SortOption s_sort_options[] = {
    { SORT_DUE,        "Due date" },
    { SORT_START,      "Start date" },
    { SORT_IMPORTANCE, "Priority" },
    { SORT_ALPHA,      "Title" },
    { SORT_MODIFIED,   "Modified" },
    { SORT_CREATED,    "Created" },
    { SORT_AUTO,       "Smart" },
};
#define NUM_SORT_OPTIONS ((int)(sizeof(s_sort_options) / sizeof(s_sort_options[0])))

// Current settings (loaded from persistent storage)
static int s_group_mode;
static int s_sort_mode;
static bool s_show_hidden;
static bool s_show_completed;

// Formatted row text
static char s_group_text[40];
static char s_sort_text[40];

static const char *find_label(const SortOption *options, int count, int value) {
    for (int i = 0; i < count; i++) {
        if (options[i].value == value) return options[i].label;
    }
    return "Unknown";
}

static int find_index(const SortOption *options, int count, int value) {
    for (int i = 0; i < count; i++) {
        if (options[i].value == value) return i;
    }
    return 0;
}

static void save_settings(void) {
    persist_write_int(PERSIST_SORT_GROUP, s_group_mode);
    persist_write_int(PERSIST_SORT_MODE, s_sort_mode);
    persist_write_bool(PERSIST_SHOW_HIDDEN, s_show_hidden);
    persist_write_bool(PERSIST_SHOW_COMPLETED, s_show_completed);
}

static void load_settings(void) {
    s_group_mode = persist_exists(PERSIST_SORT_GROUP)
        ? (int)persist_read_int(PERSIST_SORT_GROUP) : SORT_DUE;
    s_sort_mode = persist_exists(PERSIST_SORT_MODE)
        ? (int)persist_read_int(PERSIST_SORT_MODE) : SORT_DUE;
    s_show_hidden = persist_exists(PERSIST_SHOW_HIDDEN)
        ? persist_read_bool(PERSIST_SHOW_HIDDEN) : false;
    s_show_completed = persist_exists(PERSIST_SHOW_COMPLETED)
        ? persist_read_bool(PERSIST_SHOW_COMPLETED) : true;
}

static void update_labels(void) {
    snprintf(s_group_text, sizeof(s_group_text), "Group: %s",
             find_label(s_group_options, NUM_GROUP_OPTIONS, s_group_mode));
    snprintf(s_sort_text, sizeof(s_sort_text), "Sort: %s",
             find_label(s_sort_options, NUM_SORT_OPTIONS, s_sort_mode));
}

// MenuLayer callbacks

static uint16_t get_num_rows(MenuLayer *menu_layer, uint16_t section_index,
                             void *data) {
    return 4;
}

static int16_t get_cell_height(MenuLayer *menu_layer, MenuIndex *cell_index,
                               void *data) {
    return 36;
}

static void draw_row(GContext *ctx, const Layer *cell_layer,
                     MenuIndex *cell_index, void *data) {
    const char *text = NULL;
    switch (cell_index->row) {
        case 0: text = s_group_text; break;
        case 1: text = s_sort_text; break;
        case 2: text = s_show_hidden ? "Unstarted: On" : "Unstarted: Off"; break;
        case 3: text = s_show_completed ? "Completed: On" : "Completed: Off"; break;
    }
    if (text) {
        menu_cell_basic_draw(ctx, cell_layer, text, NULL, NULL);
    }
}

static void select_click(MenuLayer *menu_layer, MenuIndex *cell_index,
                         void *data) {
    switch (cell_index->row) {
        case 0: {
            int idx = find_index(s_group_options, NUM_GROUP_OPTIONS, s_group_mode);
            idx = (idx + 1) % NUM_GROUP_OPTIONS;
            s_group_mode = s_group_options[idx].value;
            break;
        }
        case 1: {
            int idx = find_index(s_sort_options, NUM_SORT_OPTIONS, s_sort_mode);
            idx = (idx + 1) % NUM_SORT_OPTIONS;
            s_sort_mode = s_sort_options[idx].value;
            break;
        }
        case 2:
            s_show_hidden = !s_show_hidden;
            break;
        case 3:
            s_show_completed = !s_show_completed;
            break;
    }
    save_settings();
    update_labels();
    menu_layer_reload_data(s_menu_layer);
}

// Window handlers

static void window_load(Window *window) {
    load_settings();
    update_labels();

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

#ifdef PBL_ROUND
    menu_layer_set_center_focused(s_menu_layer, true);
#endif
#ifdef PBL_COLOR
    menu_layer_set_normal_colors(s_menu_layer, GColorWhite, GColorBlack);
    menu_layer_set_highlight_colors(s_menu_layer, GColorCobaltBlue, GColorWhite);
#endif

    layer_add_child(window_layer, menu_layer_get_layer(s_menu_layer));
}

static void window_unload(Window *window) {
    menu_layer_destroy(s_menu_layer);
    window_destroy(s_window);
    s_window = NULL;
    if (s_callback) {
        s_callback();
    }
}

// Public API

void settings_window_push(SettingsChangedCallback callback) {
    s_callback = callback;
    s_window = window_create();
    window_set_window_handlers(s_window, (WindowHandlers){
        .load = window_load,
        .unload = window_unload,
    });
    window_stack_push(s_window, true);
}

int settings_get_group_mode(void) {
    if (persist_exists(PERSIST_SORT_GROUP))
        return (int)persist_read_int(PERSIST_SORT_GROUP);
    return SORT_DUE;
}

int settings_get_sort_mode(void) {
    if (persist_exists(PERSIST_SORT_MODE))
        return (int)persist_read_int(PERSIST_SORT_MODE);
    return SORT_DUE;
}

bool settings_get_show_hidden(void) {
    if (persist_exists(PERSIST_SHOW_HIDDEN))
        return persist_read_bool(PERSIST_SHOW_HIDDEN);
    return false;
}

bool settings_get_show_completed(void) {
    if (persist_exists(PERSIST_SHOW_COMPLETED))
        return persist_read_bool(PERSIST_SHOW_COMPLETED);
    return true;
}
