package org.tasks.injection;

import android.content.Context;

import dagger.ObjectGraph;

public final class Dagger {
    private static ObjectGraph objectGraph;

    static ObjectGraph getObjectGraph(Context context) {
        if (objectGraph == null) {
            objectGraph = ObjectGraph.create(new TasksModule(context));
        }
        return objectGraph;
    }
}
