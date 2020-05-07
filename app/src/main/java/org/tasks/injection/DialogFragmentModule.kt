package org.tasks.injection

import androidx.fragment.app.Fragment
import dagger.Module
import dagger.Provides

@Module
class DialogFragmentModule internal constructor(@get:Provides val fragment: Fragment)