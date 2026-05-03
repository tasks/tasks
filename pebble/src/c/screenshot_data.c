#include <pebble.h>
#include "screenshot_data.h"
#include <string.h>

static int add_header(UiItem *items, int n, const char *title) {
    memset(&items[n], 0, sizeof(UiItem));
    items[n].type = UI_TYPE_HEADER;
    strncpy(items[n].title, title, MAX_TITLE_LEN - 1);
    items[n].id_high = 0;
    items[n].id_low = (uint32_t)(100 + n);
    return n + 1;
}

static int add_task(UiItem *items, int n, const char *title, uint8_t priority,
                    const char *extra, bool completed) {
    memset(&items[n], 0, sizeof(UiItem));
    items[n].type = UI_TYPE_TASK;
    items[n].priority = priority;
    items[n].completed = completed;
    items[n].id_high = 0;
    items[n].id_low = (uint32_t)(n + 1);
    strncpy(items[n].title, title, MAX_TITLE_LEN - 1);
    if (extra) {
        strncpy(items[n].extra, extra, MAX_EXTRA_LEN - 1);
    }
    return n + 1;
}

int screenshot_populate_tasks(UiItem *items, int max) {
    int n = 0;

    n = add_header(items, n, "Due today");
    n = add_task(items, n, "Buy groceries", PRIORITY_HIGH,
                 "3:00 PM", false);
    n = add_task(items, n, "Pick up prescription", PRIORITY_MEDIUM,
                 NULL, false);
    n = add_task(items, n, "Call dentist", PRIORITY_NONE,
                 NULL, true);

    n = add_header(items, n, "Due tomorrow");
    n = add_task(items, n, "Finish presentation", PRIORITY_HIGH,
                 "9:00 AM", false);
    n = add_task(items, n, "Pay electric bill", PRIORITY_MEDIUM,
                 NULL, false);

    n = add_header(items, n, "Due this week");
    n = add_task(items, n, "Schedule oil change", PRIORITY_LOW,
                 NULL, false);
    n = add_task(items, n, "Return library books", PRIORITY_NONE,
                 NULL, false);

    return n > max ? max : n;
}

int screenshot_populate_shopping_tasks(UiItem *items, int max) {
    int n = 0;

    n = add_task(items, n, "Milk", PRIORITY_NONE, NULL, false);
    n = add_task(items, n, "Eggs", PRIORITY_NONE, NULL, false);
    n = add_task(items, n, "Bread", PRIORITY_NONE, NULL, true);
    n = add_task(items, n, "Butter", PRIORITY_NONE, NULL, false);
    n = add_task(items, n, "Apples", PRIORITY_NONE, NULL, false);
    n = add_task(items, n, "Bananas", PRIORITY_NONE, NULL, true);
    n = add_task(items, n, "Coffee", PRIORITY_NONE, NULL, false);
    n = add_task(items, n, "Pasta", PRIORITY_NONE, NULL, false);

    return n > max ? max : n;
}

static int add_list_header(ListItem *items, int n, const char *title) {
    memset(&items[n], 0, sizeof(ListItem));
    items[n].type = UI_TYPE_HEADER;
    strncpy(items[n].title, title, MAX_TITLE_LEN - 1);
    return n + 1;
}

static int add_list_item(ListItem *items, int n, const char *title,
                         const char *filter_id, uint32_t color) {
    memset(&items[n], 0, sizeof(ListItem));
    items[n].type = UI_TYPE_TASK;
    strncpy(items[n].title, title, MAX_TITLE_LEN - 1);
    if (filter_id) {
        strncpy(items[n].filter_id, filter_id, MAX_FILTER_ID_LEN - 1);
    }
    items[n].color = color;
    return n + 1;
}

int screenshot_populate_lists(ListItem *items, int max) {
    int n = 0;

    n = add_list_item(items, n, "My Tasks", NULL, 0);

    n = add_list_header(items, n, "Filters");
    n = add_list_item(items, n, "Today", "today", 0);

    n = add_list_header(items, n, "Tags");
    n = add_list_item(items, n, "Home", "tag:home", 0xFF5500);
    n = add_list_item(items, n, "Work", "tag:work", 0x0055FF);

    return n > max ? max : n;
}

void screenshot_populate_task_detail(TaskDetail *detail) {
    memset(detail, 0, sizeof(TaskDetail));
    strncpy(detail->title, "Buy groceries", MAX_TITLE_LEN - 1);
    detail->priority = PRIORITY_HIGH;
    detail->completed = false;
    detail->repeating = false;
    strncpy(detail->description,
            "Milk, eggs, bread, butter, cheese, apples, bananas",
            MAX_DESC_LEN - 1);
}
