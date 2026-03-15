#pragma once
#include <pebble.h>

// Protocol constants -- mirrors PebbleProtocol.kt on the phone side

// AppMessage keys
#define KEY_MSG_TYPE        0
#define KEY_TRANSACTION_ID  1
#define KEY_TOTAL_ITEMS     2
#define KEY_CHUNK_INDEX     3
#define KEY_CHUNK_COUNT     4
#define KEY_POSITION        5
#define KEY_LIMIT           6
#define KEY_FILTER          7

// Per-item keys (offset from KEY_ITEM_BASE + index * ITEM_STRIDE)
#define KEY_ITEM_BASE       100
#define ITEM_STRIDE         10
#define ITEM_ID_HIGH        0
#define ITEM_ID_LOW         1
#define ITEM_TYPE           2
#define ITEM_TITLE          3
#define ITEM_PRIORITY       4
#define ITEM_COMPLETED      5
#define ITEM_INDENT         6
#define ITEM_COLLAPSED      7
#define ITEM_NUM_SUBTASKS   8
#define ITEM_EXTRA          9
#define ITEM_COLOR          24

// Single task keys
#define KEY_TASK_ID_HIGH        10
#define KEY_TASK_ID_LOW         11
#define KEY_TASK_TITLE          12
#define KEY_TASK_PRIORITY       13
#define KEY_TASK_COMPLETED      14
#define KEY_TASK_REPEATING      15
#define KEY_TASK_DESCRIPTION    16
#define KEY_TASK_COUNT          17
#define KEY_TASK_COMPLETED_COUNT 18

// Toggle group keys
#define KEY_GROUP_VALUE_HIGH  20
#define KEY_GROUP_VALUE_LOW   21
#define KEY_GROUP_COLLAPSED   22

// Watch -> Phone message types
#define MSG_GET_TASKS       1
#define MSG_COMPLETE_TASK   2
#define MSG_TOGGLE_GROUP    3
#define MSG_GET_LISTS       4
#define MSG_GET_TASK        5
#define MSG_SAVE_TASK       6
#define MSG_GET_TASK_COUNT  7

// Phone -> Watch response types
#define RESP_TASKS          101
#define RESP_COMPLETE_TASK  102
#define RESP_TOGGLE_GROUP   103
#define RESP_LISTS          104
#define RESP_TASK           105
#define RESP_SAVE_TASK      106
#define RESP_TASK_COUNT     107

// Push notification
#define MSG_REFRESH         200

// UiItem types
#define UI_TYPE_HEADER      0
#define UI_TYPE_TASK        1

// Priority values
#define PRIORITY_HIGH       0
#define PRIORITY_MEDIUM     1
#define PRIORITY_LOW        2
#define PRIORITY_NONE       3

// Configuration
#define CHUNK_SIZE          5
#define PAGE_SIZE           20
#define PREFETCH_THRESHOLD  5
#define MAX_LISTS           30
#define MAX_TITLE_LEN       51
#define MAX_EXTRA_LEN       21
#define MAX_FILTER_ID_LEN   48
#define MAX_DESC_LEN        201

// Data structures
typedef struct {
    uint32_t id_high;
    uint32_t id_low;
    char title[MAX_TITLE_LEN];
    char extra[MAX_EXTRA_LEN];
    uint8_t type;
    uint8_t priority;
    uint8_t indent;
    uint8_t num_subtasks;
    bool completed;
    bool collapsed;
} UiItem;

typedef struct {
    char title[MAX_TITLE_LEN];
    char filter_id[MAX_FILTER_ID_LEN];
    uint32_t color;
    uint32_t text_color;
    uint8_t type;
    uint8_t task_count;
    bool collapsed;
} ListItem;

typedef struct {
    char title[MAX_TITLE_LEN];
    char description[MAX_DESC_LEN];
    uint8_t priority;
    bool completed;
    bool repeating;
} TaskDetail;

// Transaction tracking
uint8_t protocol_next_transaction_id(void);
uint8_t protocol_get_active_transaction_id(void);

// Message sending
void protocol_send_get_tasks(const char *filter, int position, int limit);
void protocol_send_complete_task(uint32_t id_high, uint32_t id_low, bool completed);
void protocol_send_toggle_group(uint32_t id_high, uint32_t id_low, bool collapsed);
void protocol_send_get_lists(void);
void protocol_send_get_task(uint32_t id_high, uint32_t id_low);

// Chunk parsing -- returns number of items parsed
int protocol_parse_items(DictionaryIterator *iter, UiItem *items, int max_items);
int protocol_parse_list_items(DictionaryIterator *iter, ListItem *items, int max_items);
void protocol_parse_task_detail(DictionaryIterator *iter, TaskDetail *detail);

// Priority helpers
GColor protocol_priority_color(uint8_t priority);
const char *protocol_priority_prefix(uint8_t priority);
