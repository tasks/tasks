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

// Session ID (for replay detection)
#define KEY_SESSION_ID      8

// Settings keys
#define KEY_SHOW_HIDDEN     28
#define KEY_SHOW_COMPLETED  29
#define KEY_SORT_MODE       30
#define KEY_GROUP_MODE      31

// Capability key: every outgoing message carries this watch build's inbox
// size, and the phone packs its response chunks to fit a single message
// (see PROTOCOL_INBOX_SIZE below and PebbleProtocol.kt)
#define KEY_MAX_MSG_SIZE    32

// Watch -> Phone message types
#define MSG_GET_TASKS       1
#define MSG_COMPLETE_TASK   2
#define MSG_TOGGLE_GROUP    3
#define MSG_GET_LISTS       4
#define MSG_GET_TASK        5
#define MSG_SAVE_TASK       6
#define MSG_GET_TASK_COUNT  7
#define MSG_TOGGLE_LIST     8

// Phone -> Watch response types
#define RESP_TASKS          101
#define RESP_COMPLETE_TASK  102
#define RESP_TOGGLE_GROUP   103
#define RESP_LISTS          104
#define RESP_TASK           105
#define RESP_SAVE_TASK      106
#define RESP_TASK_COUNT     107
#define RESP_TOGGLE_LIST    108

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

// Sort modes (mirrors SortHelper.kt)
#define SORT_GROUP_NONE     -1
#define SORT_AUTO           0
#define SORT_ALPHA          1
#define SORT_DUE            2
#define SORT_IMPORTANCE     3
#define SORT_MODIFIED       4
#define SORT_CREATED        5
#define SORT_START          8
#define SORT_LIST           9

// Persistent storage keys for settings
#define PERSIST_SORT_GROUP       10
#define PERSIST_SORT_MODE        11
#define PERSIST_SHOW_HIDDEN      12
#define PERSIST_SHOW_COMPLETED   13

// Configuration
//
// Items per response chunk. The phone packs chunk_index * CHUNK_SIZE items
// into a single AppMessage, so PebbleMessageHandler must chunk to the same
// size per platform. aplite only has 124-byte AppMessages to work with
// (see PROTOCOL_INBOX_SIZE), so it gets one item per chunk.
#if defined(PBL_PLATFORM_APLITE)
#define CHUNK_SIZE          1
#else
#define CHUNK_SIZE          5
#endif
#define INITIAL_PAGE_SIZE   15
// A refresh replaces the whole window, so ask for half of it rather than a
// single page. Refreshing into 15 items mid-scroll drops the cursor straight
// into placeholder rows and forces a burst of catch-up pages.
#define REFRESH_PAGE_SIZE   (MAX_WINDOW / 2)
#define REFRESH_LIST_SIZE   (MAX_LISTS / 2)

// AppMessage buffer sizes. aplite shares one 24KB address space between the
// app binary and the heap, so app_message_open() with the maximum inbox
// (~2KB) can fail to allocate and leaves AppMessage completely dead -- and a
// single message has to fit the 124 bytes the SDK guarantees anyway.
//
// The inbox holds two of those messages: the phone-side pump does not wait
// for the previous chunk's ack, and with a one-message inbox a chunk
// arriving while the app is finishing the last page's reload is dropped with
// APP_MSG_BUSY -- the drop cascade then wedges the whole session. Heap use
// at 248 bytes is trivial (~8KB stays free at build time).
#if defined(PBL_PLATFORM_APLITE)
#define PROTOCOL_INBOX_SIZE   248
#define PROTOCOL_OUTBOX_SIZE  124
#else
#define PROTOCOL_INBOX_SIZE   app_message_inbox_size_maximum()
#define PROTOCOL_OUTBOX_SIZE  256
#endif

// Sliding-window caps for the task list and the filter picker.
//
// Trimming the window shifts every row underneath the cursor, and the SDK gives
// no way to compensate the scroll offset, so a trim is always a visible jump.
// Keep these big enough that an ordinary list fits entirely and never trims.
// aplite has only 24KB of app RAM and holds both windows at once while the
// picker is open, so it gets smaller ones and will trim sooner.
//
// PREFETCH_THRESHOLD is how many loaded rows must remain below the cursor
// before the next page is requested -- i.e. the runway the user has left. Too
// small and a fast scroller reaches the end of the window while the request is
// still in flight, which pins the cursor to the last row until it lands. It has
// to stay well clear of the page-up trigger (a cursor below the threshold at the
// top) or the two fight each other.
#if defined(PBL_PLATFORM_APLITE)
#define MAX_WINDOW          30
#define MAX_LISTS           30
#define PAGE_SIZE           10
#define PREFETCH_THRESHOLD  8
#else
#define MAX_WINDOW          100
#define MAX_LISTS           100
#define PAGE_SIZE           20
#define PREFETCH_THRESHOLD  25
#endif
// Rows a MenuLayer may span. ScrollLayer content height is int16 (GSize), so at
// 44px a row the hard ceiling is ~744; stay well clear of it. Longer lists are
// addressed through a base offset that shifts when the cursor nears the edge.
#define MAX_ROWS            600
#define REBASE_MARGIN       60

// Per-item buffers. aplite always runs against the phone's small-message
// encoding (titles byte-truncated to 24, timestamps omitted), so its rows
// can be correspondingly small: a 30-row window that ships 51+21-byte
// buffers per item needs a 3.6KB realloc to page further down a list, which
// fails on the ~7.7KB runtime heap ("Failed to allocate", from ocess_manager's
// Heap Usage log) and leaves the window scrolling placeholder rows forever.
#if defined(PBL_PLATFORM_APLITE)
#define MAX_TITLE_LEN       25
#define MAX_EXTRA_LEN       1
#else
#define MAX_TITLE_LEN       51
#define MAX_EXTRA_LEN       21
#endif
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

// Session and transaction tracking
void protocol_init_session(void);
uint8_t protocol_next_transaction_id(void);
uint8_t protocol_get_active_transaction_id(void);

// Reads a tuple's integer value regardless of wire width. The phone encodes
// small-message responses with uint8 tuples to fit aplite's 124-byte
// AppMessages, while older phone apps send uint32 -- this accepts both.
uint32_t protocol_tuple_value(Tuple *t);

// Message sending
bool protocol_send_get_tasks(const char *filter, int position, int limit,
                            int sort_mode, int group_mode,
                            bool show_hidden, bool show_completed);
void protocol_send_complete_task(uint32_t id_high, uint32_t id_low, bool completed);
void protocol_send_toggle_group(uint32_t id_high, uint32_t id_low, bool collapsed);
bool protocol_send_get_lists(int position, int limit);
// Returns false if the message could not be queued, in which case no
// transaction id was allocated and no response will arrive
bool protocol_send_toggle_list(const char *id, bool collapsed);
void protocol_send_get_task(uint32_t id_high, uint32_t id_low);
void protocol_send_save_task(const char *title, const char *filter);

// Chunk parsing -- returns number of items parsed
int protocol_parse_items(DictionaryIterator *iter, UiItem *items, int max_items);
int protocol_parse_list_items(DictionaryIterator *iter, ListItem *items, int max_items);
void protocol_parse_task_detail(DictionaryIterator *iter, TaskDetail *detail);

// Priority helpers
GColor protocol_priority_color(uint8_t priority);
const char *protocol_priority_prefix(uint8_t priority);
