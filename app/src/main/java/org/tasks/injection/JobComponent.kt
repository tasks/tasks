package org.tasks.injection

import dagger.Subcomponent
import org.tasks.jobs.*

@Subcomponent(modules = [WorkModule::class])
interface JobComponent {
    fun inject(work: SyncWork)
    fun inject(work: BackupWork)
    fun inject(work: RefreshWork)
    fun inject(work: CleanupWork)
    fun inject(work: MidnightRefreshWork)
    fun inject(work: AfterSaveWork)
    fun inject(work: DriveUploader)
    fun inject(work: ReverseGeocodeWork)
    fun inject(work: RemoteConfigWork)
}