#pragma once
#include <pebble.h>

typedef void (*SettingsChangedCallback)(void);

void settings_window_push(SettingsChangedCallback callback);

// Read current settings (from persistent storage)
int settings_get_group_mode(void);
int settings_get_sort_mode(void);
bool settings_get_show_hidden(void);
bool settings_get_show_completed(void);
