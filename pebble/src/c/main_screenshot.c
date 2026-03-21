/**
 * Screenshot mode entry point.
 *
 * Compiled instead of main.c when SCREENSHOT_MODE is set.
 * Pushes the appropriate window for the current SCREENSHOT_SCENE
 * without any AppMessage or Bluetooth setup.
 */

#include <pebble.h>
#include "task_list_window.h"
#include "menu_window.h"
#include "task_view_window.h"
#include "settings_window.h"

#ifndef SCREENSHOT_SCENE
#define SCREENSHOT_SCENE 1
#endif

static void init(void) {
#if SCREENSHOT_SCENE == 2
    menu_window_push(NULL);
#elif SCREENSHOT_SCENE == 3
    task_view_window_push(0, 1);
#elif SCREENSHOT_SCENE == 4
    settings_window_push(NULL);
#else
    /* Scenes 1 and 5 both use the task list window */
    task_list_window_push();
#endif
}

static void deinit(void) {
}

int main(void) {
    init();
    app_event_loop();
    deinit();
}
