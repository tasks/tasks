#pragma once
#include <pebble.h>

void task_list_window_push(void);
void task_list_window_refresh(void);
void task_list_handle_tasks_response(DictionaryIterator *iter);
void task_list_handle_complete_response(DictionaryIterator *iter);
void task_list_handle_save_response(DictionaryIterator *iter);
void task_list_handle_toggle_response(DictionaryIterator *iter);
