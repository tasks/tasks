import ComposeApp
import CoreSpotlight
import Foundation

@available(iOS 27.0, *)
enum SpotlightIndex {
    private static let pageSize: Int32 = 500
    private static let completedWindow: TimeInterval = 30 * 24 * 60 * 60

    static func reindexAll() async throws {
        var entities: [TaskEntity] = []
        var offset: Int32 = 0
        while true {
            let page = try await TasksIntents.shared.openTasks(offset: offset, limit: pageSize)
            entities += page.map(TaskEntity.init)
            if page.count < Int(pageSize) { break }
            offset += pageSize
        }
        let since = Int64(Date().addingTimeInterval(-completedWindow).timeIntervalSince1970 * 1000)
        offset = 0
        while true {
            let page = try await TasksIntents.shared.completedTasks(after: since, offset: offset, limit: pageSize)
            entities += page.map(TaskEntity.init)
            if page.count < Int(pageSize) { break }
            offset += pageSize
        }
        let index = CSSearchableIndex.default()
        try await index.deleteAppEntities(ofType: TaskEntity.self)
        for start in stride(from: 0, to: entities.count, by: Int(pageSize)) {
            try await index.indexAppEntities(Array(entities[start..<min(start + Int(pageSize), entities.count)]))
        }
    }

    static func reindex(ids: [Int]) async throws {
        let found = try await TasksIntents.shared.tasks(ids: ids.map { KotlinLong(longLong: Int64($0)) }).map(TaskEntity.init)
        let index = CSSearchableIndex.default()
        let missing = Set(ids).subtracting(found.map(\.id))
        if !missing.isEmpty {
            try await index.deleteAppEntities(identifiedBy: Array(missing), ofType: TaskEntity.self)
        }
        if !found.isEmpty {
            try await index.indexAppEntities(found)
        }
    }

    static func index(_ entity: TaskEntity) async {
        try? await CSSearchableIndex.default().indexAppEntities([entity])
    }

    static func remove(ids: [Int]) async {
        try? await CSSearchableIndex.default().deleteAppEntities(identifiedBy: ids, ofType: TaskEntity.self)
    }
}
