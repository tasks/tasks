package org.tasks.extensions

import android.view.MenuItem
import androidx.appcompat.widget.SearchView

fun MenuItem.setOnQueryTextListener(listener: SearchView.OnQueryTextListener?) =
    (actionView as? SearchView)?.setOnQueryTextListener(listener)