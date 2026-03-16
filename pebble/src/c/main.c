/**
 * Tasks.org Pebble Watch App
 *
 * Companion app for Pebble watches. Communicates with the Tasks.org
 * Android app over Bluetooth via AppMessage (PebbleKit Android).
 *
 * Supports all Pebble hardware: aplite, basalt, chalk, diorite, emery.
 */

#include <pebble.h>
#include "protocol.h"
#include "task_list_window.h"
#include "menu_window.h"
#include "task_view_window.h"

static void inbox_received_handler(DictionaryIterator *iter, void *context) {
    Tuple *msg_type_t = dict_find(iter, KEY_MSG_TYPE);
    if (!msg_type_t) return;

    uint8_t msg_type = (uint8_t)msg_type_t->value->uint32;

    switch (msg_type) {
        case RESP_TASKS:
            task_list_handle_tasks_response(iter);
            break;
        case RESP_COMPLETE_TASK:
            task_list_handle_complete_response(iter);
            task_view_handle_complete_response(iter);
            break;
        case RESP_LISTS:
            menu_window_handle_lists_response(iter);
            break;
        case RESP_TASK:
            task_view_handle_task_response(iter);
            break;
        case RESP_SAVE_TASK:
            task_list_handle_save_response(iter);
            break;
        case RESP_TOGGLE_GROUP:
            task_list_handle_toggle_response(iter);
            break;
        case MSG_REFRESH:
            task_list_window_refresh();
            break;
        default:
            APP_LOG(APP_LOG_LEVEL_WARNING, "Unknown msg type: %d", msg_type);
            break;
    }
}

static void inbox_dropped_handler(AppMessageResult reason, void *context) {
    APP_LOG(APP_LOG_LEVEL_ERROR, "Message dropped: %d", (int)reason);
}

static void outbox_sent_handler(DictionaryIterator *iter, void *context) {
    APP_LOG(APP_LOG_LEVEL_DEBUG, "Outbox send succeeded");
}

static void outbox_failed_handler(DictionaryIterator *iter,
                                   AppMessageResult reason, void *context) {
    APP_LOG(APP_LOG_LEVEL_ERROR, "Outbox send failed: %d", (int)reason);
}

static void bluetooth_handler(bool connected) {
    if (connected) {
        APP_LOG(APP_LOG_LEVEL_INFO, "Bluetooth reconnected, refreshing");
        task_list_window_refresh();
    } else {
        APP_LOG(APP_LOG_LEVEL_INFO, "Bluetooth disconnected");
    }
}

static void init(void) {
    // Register AppMessage handlers
    app_message_register_inbox_received(inbox_received_handler);
    app_message_register_inbox_dropped(inbox_dropped_handler);
    app_message_register_outbox_sent(outbox_sent_handler);
    app_message_register_outbox_failed(outbox_failed_handler);

    // Open AppMessage with maximum inbox (for chunked responses)
    const uint32_t inbox_size = app_message_inbox_size_maximum();
    const uint32_t outbox_size = 256;
    app_message_open(inbox_size, outbox_size);

    // Bluetooth connection monitoring
    connection_service_subscribe((ConnectionHandlers){
        .pebble_app_connection_handler = bluetooth_handler,
    });

    // Show task list on launch
    task_list_window_push();
}

static void deinit(void) {
    connection_service_unsubscribe();
}

int main(void) {
    init();
    app_event_loop();
    deinit();
}
