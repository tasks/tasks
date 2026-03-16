#include "protocol.h"

static uint8_t s_next_transaction_id = 1;
static uint8_t s_active_transaction_id = 0;

uint8_t protocol_next_transaction_id(void) {
    s_active_transaction_id = s_next_transaction_id++;
    return s_active_transaction_id;
}

uint8_t protocol_get_active_transaction_id(void) {
    return s_active_transaction_id;
}

static void send_message(void) {
    AppMessageResult result = app_message_outbox_send();
    if (result != APP_MSG_OK) {
        APP_LOG(APP_LOG_LEVEL_ERROR, "Outbox send failed: %d", (int)result);
    }
}

void protocol_send_get_tasks(const char *filter, int position, int limit,
                            int sort_mode, int group_mode,
                            bool show_hidden, bool show_completed) {
    DictionaryIterator *out;
    AppMessageResult result = app_message_outbox_begin(&out);
    if (result != APP_MSG_OK) {
        APP_LOG(APP_LOG_LEVEL_ERROR, "Outbox begin failed: %d", (int)result);
        return;
    }

    uint8_t txn = protocol_next_transaction_id();
    dict_write_uint8(out, KEY_MSG_TYPE, MSG_GET_TASKS);
    dict_write_uint8(out, KEY_TRANSACTION_ID, txn);
    dict_write_uint32(out, KEY_POSITION, (uint32_t)position);
    dict_write_uint32(out, KEY_LIMIT, (uint32_t)limit);
    if (filter) {
        dict_write_cstring(out, KEY_FILTER, filter);
    }
    dict_write_uint32(out, KEY_SORT_MODE, (uint32_t)sort_mode);
    dict_write_uint32(out, KEY_GROUP_MODE, (uint32_t)group_mode);
    dict_write_uint8(out, KEY_SHOW_HIDDEN, show_hidden ? 1 : 0);
    dict_write_uint8(out, KEY_SHOW_COMPLETED, show_completed ? 1 : 0);

    send_message();
}

void protocol_send_complete_task(uint32_t id_high, uint32_t id_low, bool completed) {
    DictionaryIterator *out;
    AppMessageResult result = app_message_outbox_begin(&out);
    if (result != APP_MSG_OK) return;

    dict_write_uint8(out, KEY_MSG_TYPE, MSG_COMPLETE_TASK);
    dict_write_uint8(out, KEY_TRANSACTION_ID, protocol_next_transaction_id());
    dict_write_uint32(out, KEY_TASK_ID_HIGH, id_high);
    dict_write_uint32(out, KEY_TASK_ID_LOW, id_low);
    dict_write_uint8(out, KEY_TASK_COMPLETED, completed ? 1 : 0);

    send_message();
}

void protocol_send_toggle_group(uint32_t id_high, uint32_t id_low, bool collapsed) {
    DictionaryIterator *out;
    AppMessageResult result = app_message_outbox_begin(&out);
    if (result != APP_MSG_OK) return;

    dict_write_uint8(out, KEY_MSG_TYPE, MSG_TOGGLE_GROUP);
    dict_write_uint8(out, KEY_TRANSACTION_ID, protocol_next_transaction_id());
    dict_write_uint32(out, KEY_GROUP_VALUE_HIGH, id_high);
    dict_write_uint32(out, KEY_GROUP_VALUE_LOW, id_low);
    dict_write_uint8(out, KEY_GROUP_COLLAPSED, collapsed ? 1 : 0);

    send_message();
}

void protocol_send_get_lists(void) {
    DictionaryIterator *out;
    AppMessageResult result = app_message_outbox_begin(&out);
    if (result != APP_MSG_OK) return;

    dict_write_uint8(out, KEY_MSG_TYPE, MSG_GET_LISTS);
    dict_write_uint8(out, KEY_TRANSACTION_ID, protocol_next_transaction_id());
    dict_write_uint32(out, KEY_POSITION, 0);
    dict_write_uint32(out, KEY_LIMIT, 0);

    send_message();
}

void protocol_send_get_task(uint32_t id_high, uint32_t id_low) {
    DictionaryIterator *out;
    AppMessageResult result = app_message_outbox_begin(&out);
    if (result != APP_MSG_OK) return;

    dict_write_uint8(out, KEY_MSG_TYPE, MSG_GET_TASK);
    dict_write_uint8(out, KEY_TRANSACTION_ID, protocol_next_transaction_id());
    dict_write_uint32(out, KEY_TASK_ID_HIGH, id_high);
    dict_write_uint32(out, KEY_TASK_ID_LOW, id_low);

    send_message();
}

void protocol_send_save_task(const char *title, const char *filter) {
    DictionaryIterator *out;
    AppMessageResult result = app_message_outbox_begin(&out);
    if (result != APP_MSG_OK) return;

    dict_write_uint8(out, KEY_MSG_TYPE, MSG_SAVE_TASK);
    dict_write_uint8(out, KEY_TRANSACTION_ID, protocol_next_transaction_id());
    dict_write_uint32(out, KEY_TASK_ID_HIGH, 0);
    dict_write_uint32(out, KEY_TASK_ID_LOW, 0);
    dict_write_cstring(out, KEY_TASK_TITLE, title);
    dict_write_uint8(out, KEY_TASK_COMPLETED, 0);
    if (filter) {
        dict_write_cstring(out, KEY_FILTER, filter);
    }

    send_message();
}

static uint32_t safe_uint(Tuple *t) {
    return t ? t->value->uint32 : 0;
}

int protocol_parse_items(DictionaryIterator *iter, UiItem *items, int max_items) {
    int count = 0;
    for (int i = 0; i < CHUNK_SIZE && count < max_items; i++) {
        uint32_t base = KEY_ITEM_BASE + i * ITEM_STRIDE;

        Tuple *type_t = dict_find(iter, base + ITEM_TYPE);
        if (!type_t) break;

        UiItem *item = &items[count];
        memset(item, 0, sizeof(UiItem));

        item->type = (uint8_t)type_t->value->uint32;
        item->id_high = safe_uint(dict_find(iter, base + ITEM_ID_HIGH));
        item->id_low = safe_uint(dict_find(iter, base + ITEM_ID_LOW));

        Tuple *title_t = dict_find(iter, base + ITEM_TITLE);
        if (title_t && title_t->type == TUPLE_CSTRING) {
            strncpy(item->title, title_t->value->cstring, MAX_TITLE_LEN - 1);
        }

        item->collapsed = safe_uint(dict_find(iter, base + ITEM_COLLAPSED)) != 0;

        if (item->type == UI_TYPE_TASK) {
            item->priority = (uint8_t)safe_uint(dict_find(iter, base + ITEM_PRIORITY));
            item->completed = safe_uint(dict_find(iter, base + ITEM_COMPLETED)) != 0;
            item->indent = (uint8_t)safe_uint(dict_find(iter, base + ITEM_INDENT));
            item->num_subtasks = (uint8_t)safe_uint(dict_find(iter, base + ITEM_NUM_SUBTASKS));

            Tuple *extra_t = dict_find(iter, base + ITEM_EXTRA);
            if (extra_t && extra_t->type == TUPLE_CSTRING) {
                strncpy(item->extra, extra_t->value->cstring, MAX_EXTRA_LEN - 1);
            }
        }

        count++;
    }
    return count;
}

int protocol_parse_list_items(DictionaryIterator *iter, ListItem *items, int max_items) {
    int count = 0;
    for (int i = 0; i < CHUNK_SIZE && count < max_items; i++) {
        uint32_t base = KEY_ITEM_BASE + i * ITEM_STRIDE;

        Tuple *type_t = dict_find(iter, base + ITEM_TYPE);
        if (!type_t) break;

        ListItem *item = &items[count];
        memset(item, 0, sizeof(ListItem));

        item->type = (uint8_t)type_t->value->uint32;

        Tuple *title_t = dict_find(iter, base + ITEM_TITLE);
        if (title_t && title_t->type == TUPLE_CSTRING) {
            strncpy(item->title, title_t->value->cstring, MAX_TITLE_LEN - 1);
        }

        Tuple *extra_t = dict_find(iter, base + ITEM_EXTRA);
        if (extra_t && extra_t->type == TUPLE_CSTRING) {
            strncpy(item->filter_id, extra_t->value->cstring, MAX_FILTER_ID_LEN - 1);
        }

        item->color = safe_uint(dict_find(iter, base + ITEM_COLOR));
        item->text_color = safe_uint(dict_find(iter, base + ITEM_COMPLETED));

        count++;
    }
    return count;
}

void protocol_parse_task_detail(DictionaryIterator *iter, TaskDetail *detail) {
    memset(detail, 0, sizeof(TaskDetail));

    Tuple *title_t = dict_find(iter, KEY_TASK_TITLE);
    if (title_t && title_t->type == TUPLE_CSTRING) {
        strncpy(detail->title, title_t->value->cstring, MAX_TITLE_LEN - 1);
    }

    Tuple *priority_t = dict_find(iter, KEY_TASK_PRIORITY);
    detail->priority = priority_t ? (uint8_t)priority_t->value->uint32 : PRIORITY_NONE;

    Tuple *completed_t = dict_find(iter, KEY_TASK_COMPLETED);
    detail->completed = completed_t ? completed_t->value->uint32 != 0 : false;

    Tuple *repeating_t = dict_find(iter, KEY_TASK_REPEATING);
    detail->repeating = repeating_t ? repeating_t->value->uint32 != 0 : false;

    Tuple *desc_t = dict_find(iter, KEY_TASK_DESCRIPTION);
    if (desc_t && desc_t->type == TUPLE_CSTRING) {
        strncpy(detail->description, desc_t->value->cstring, MAX_DESC_LEN - 1);
    }
}

GColor protocol_priority_color(uint8_t priority) {
#ifdef PBL_COLOR
    switch (priority) {
        case PRIORITY_HIGH:   return GColorRed;
        case PRIORITY_MEDIUM: return GColorOrange;
        case PRIORITY_LOW:    return GColorVividCerulean;
        default:              return GColorDarkGray;
    }
#else
    (void)priority;
    return GColorBlack;
#endif
}

const char *protocol_priority_prefix(uint8_t priority) {
    switch (priority) {
        case PRIORITY_HIGH:   return "!!! ";
        case PRIORITY_MEDIUM: return "!! ";
        case PRIORITY_LOW:    return "! ";
        default:              return "";
    }
}
