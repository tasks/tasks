#pragma once

#ifdef SCREENSHOT_MODE

#ifndef SCREENSHOT_SCENE
#define SCREENSHOT_SCENE 1
#endif

#include "protocol.h"

/* Scene 1: Task list (My Tasks, grouped by date)
 * Scene 2: Filter/list selection
 * Scene 3: Task detail view
 * Scene 4: Settings
 * Scene 5: Task list with colored filter (Shopping)
 */

int screenshot_populate_tasks(UiItem *items, int max);
int screenshot_populate_shopping_tasks(UiItem *items, int max);
int screenshot_populate_lists(ListItem *items, int max);
void screenshot_populate_task_detail(TaskDetail *detail);

#endif
