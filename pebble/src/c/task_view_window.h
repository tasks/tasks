#pragma once
#include <pebble.h>

void task_view_window_push(uint32_t id_high, uint32_t id_low);
void task_view_handle_task_response(DictionaryIterator *iter);
void task_view_handle_complete_response(DictionaryIterator *iter);
