package org.tasks.injection

import androidx.fragment.app.Fragment
import dagger.Module
import dagger.Provides

@Module
class FragmentModule(@get:Provides val fragment: Fragment)