# Wear-Phone Sync Architecture

## Overview

This document describes the sync architecture implemented for the Tasks Wear OS app.
The system uses the Wearable Data Layer API for reliable data synchronization between
the watch and phone, with per-field conflict resolution.

## Key Components

### Wear Module (Watch)

#### Data Layer (`wear/src/main/java/org/tasks/data/`)

1. **TaskEntity** - Room entity with sync fields:
   - `dirty`: true if local changes not yet synced
   - `titleUpdatedAt`, `notesUpdatedAt`, `completedUpdatedAt`: per-field timestamps for conflict resolution
   - `syncedAt`: timestamp of last successful sync

2. **OutboxOpEntity** - Queue of pending sync operations:
   - `opId`: unique operation ID
   - `taskId`: target task
   - `type`: CREATE, UPDATE, DELETE, COMPLETE
   - `payload`: JSON serialized operation data
   - `state`: PENDING, SENDING, SENT, ACKED, FAILED

3. **ProcessedOpEntity** - Tracks processed operations from phone (idempotency)

4. **WearDatabase** - Room database with all entities and DAOs

#### Sync Layer (`wear/src/main/java/org/tasks/data/sync/`)

1. **SyncProtocol.kt** - Defines paths and keys:
   - `/outbox/watch/{opId}` - Watch → Phone operations
   - `/ack/watch/{opId}` - Phone → Watch acknowledgments
   - `/outbox/phone/{opId}` - Phone → Watch operations
   - `/ack/phone/{opId}` - Watch → Phone acknowledgments
   - `/snapshot/tasks` - Full task snapshot
   - `/tasks/{taskId}` - Single task updates

2. **SyncRepository** - Handles transactional operations:
   - Creates tasks + outbox ops in single transaction
   - Applies incoming operations with conflict resolution
   - Per-field last-write-wins strategy

3. **DataLayerSyncManager** - Manages Wearable Data Layer:
   - Sends operations via DataItem
   - Receives and processes incoming data
   - Handles acks and cleanup

4. **WearSyncListenerService** - Background service for data changes

5. **SyncWorker** - Periodic WorkManager job for:
   - Processing pending operations
   - Cleanup of acknowledged ops
   - Retry of failed operations

### Phone Module (App)

#### Sync Layer (`app/src/main/java/org/tasks/wear/sync/`)

1. **SyncProtocol.kt** - Mirrors wear module paths/keys

2. **PhoneSyncManager** - Handles watch operations:
   - Receives CREATE/UPDATE/DELETE/COMPLETE ops
   - Applies to phone TaskDao with conflict resolution
   - Sends acks back to watch
   - Can send snapshots to watch

3. **PhoneSyncListenerService** - Background service with Hilt injection

## Sync Flow

### Watch → Phone (User creates/edits task on watch)

```
1. User saves task on watch
2. TaskRepository.saveTask() →
3. SyncRepository.updateTitleAndNotes():
   - Update task in Room with dirty=true
   - Insert OutboxOpEntity
   - (Transaction ensures atomicity)
4. DataLayerSyncManager.processPendingOps():
   - Create DataItem with operation data
   - Set urgent for immediate delivery
5. Phone receives onDataChanged()
6. PhoneSyncManager.handleWatchOperation():
   - Check idempotency (processedOps)
   - Apply to TaskDao with conflict resolution
   - Send ack via DataItem
7. Watch receives ack, marks op as ACKED
```

### Phone → Watch (User creates/edits task on phone)

```
1. Task changed on phone
2. PhoneSyncManager.sendTaskToWatch() or sendTasksToWatch()
3. Create DataItem with task data + field timestamps
4. Watch receives onDataChanged()
5. DataLayerSyncManager.handlePhoneOperation():
   - Check idempotency
   - SyncRepository.applyIncomingTask():
     - Per-field conflict resolution (last-write-wins)
     - Update only fields where incoming timestamp > local
6. Send ack via DataItem
```

## Conflict Resolution

Uses **per-field last-write-wins** strategy:

- Each field (title, notes, completed) has its own timestamp
- When merging, compare timestamps per-field
- Keep the value with the newer timestamp
- This minimizes data loss compared to whole-record LWW

Example:
```
Watch:  title="Buy milk" (12:00), notes="2 liters" (12:05)
Phone:  title="Buy groceries" (12:10), notes="1 liter" (12:01)
Result: title="Buy groceries" (from phone, newer)
        notes="2 liters" (from watch, newer)
```

## Retry & Error Handling

- Operations marked SENDING for >5 min are reset to PENDING
- WorkManager runs every 15 min to process pending ops
- Acked operations cleaned up periodically
- Processed ops cleaned up after 7 days

## DataItem Considerations

- `setUrgent()` used for user-initiated actions
- Non-urgent for batch syncs (battery friendly)
- DataItems persist and sync when connectivity available
- System handles retry automatically

## Files Created/Modified

### Wear Module
- `data/local/TaskEntity.kt` - Added sync fields
- `data/local/OutboxOpEntity.kt` - NEW
- `data/local/OutboxOpDao.kt` - NEW
- `data/local/ProcessedOpEntity.kt` - NEW
- `data/local/ProcessedOpDao.kt` - NEW
- `data/local/WearDatabase.kt` - Added new entities/DAOs
- `data/local/TaskDao.kt` - Added sync queries
- `data/local/TaskRepository.kt` - Integrated SyncRepository
- `data/sync/SyncProtocol.kt` - NEW
- `data/sync/SyncRepository.kt` - NEW
- `data/sync/DataLayerSyncManager.kt` - NEW
- `data/sync/WearSyncListenerService.kt` - NEW
- `data/sync/SyncWorker.kt` - NEW
- `WatchApp.kt` - Initialize sync
- `AndroidManifest.xml` - Register service

### App Module
- `wear/sync/SyncProtocol.kt` - NEW
- `wear/sync/PhoneSyncManager.kt` - NEW
- `wear/sync/PhoneSyncListenerService.kt` - NEW
- `injection/ApplicationModule.kt` - Provider for PhoneSyncManager
- `AndroidManifest.xml` - Register service

