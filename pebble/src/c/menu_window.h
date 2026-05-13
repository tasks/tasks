#pragma once
#include <pebble.h>

typedef void (*FilterSelectedCallback)(const char *filter_id, const char *filter_name, uint32_t color, uint32_t text_color);

void menu_window_push(FilterSelectedCallback callback);
void menu_window_handle_lists_response(DictionaryIterator *iter);
