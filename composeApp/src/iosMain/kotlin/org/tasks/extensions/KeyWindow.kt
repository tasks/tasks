package org.tasks.extensions

import platform.UIKit.UIApplication
import platform.UIKit.UIWindow

internal fun keyWindow(): UIWindow =
    UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>().firstOrNull { it.isKeyWindow() }
        ?: UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>().first()
