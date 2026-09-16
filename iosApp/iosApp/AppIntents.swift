import AppIntents
import ComposeApp
import CoreLocation
import Foundation
import GeoToolbox

@available(iOS 27.0, *)
@AppEntity(schema: .reminders.reminder)
struct TaskEntity {
    static let defaultQuery = TaskEntityQuery()

    let id: Int

    var title: String
    var note: AttributedString?
    var tags: Set<String>
    var urls: [URL]
    var dueDate: DateComponents?
    var recurrence: Calendar.RecurrenceRule?
    var isCompleted: Bool
    var isFlagged: Bool?
    var creationDate: Date?
    var completionDate: Date?
    var list: TaskListEntity
    var locationTrigger: LocationTriggerEntity?

    init(_ task: IntentTask) {
        id = Int(task.id)
        title = task.title
        note = task.notes.map { AttributedString($0) }
        tags = Set(task.tags)
        urls = []
        dueDate = task.due.map { dueComponents(millis: $0.int64Value, allDay: task.dueAllDay) }
        recurrence = task.recurrence.flatMap { Recurrence.rule(from: $0) }
        isCompleted = task.completed != nil
        isFlagged = task.priority == TaskPriority.high.rawValue
        creationDate = task.created.map(date(millis:))
        completionDate = task.completed.map(date(millis:))
        list = TaskListEntity(task.list)
        locationTrigger = task.locationTrigger.map(LocationTriggerEntity.init)
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(title)",
            subtitle: dueDate.flatMap { $0.date }.map {
                "\($0.formatted(date: .abbreviated, time: dueDate?.hour == nil ? .omitted : .shortened))"
            }
        )
    }
}

@available(iOS 27.0, *)
struct TaskEntityQuery: EntityStringQuery {
    func entities(for identifiers: [Int]) async throws -> [TaskEntity] {
        try await TasksIntents.shared.tasks(ids: identifiers.map { KotlinLong(longLong: Int64($0)) }).map(TaskEntity.init)
    }

    func entities(matching string: String) async throws -> [TaskEntity] {
        try await TasksIntents.shared.findTasks(search: string, limit: 25).map(TaskEntity.init)
    }

    func suggestedEntities() async throws -> [TaskEntity] {
        try await TasksIntents.shared.findTasks(search: nil, limit: 25).map(TaskEntity.init)
    }
}

@available(iOS 27.0, *)
@AppEntity(schema: .reminders.list)
struct TaskListEntity {
    static let defaultQuery = TaskListQuery()

    let id: Int

    var name: String
    var type: ListType

    init(_ list: IntentList) {
        id = Int(list.id)
        name = list.title
        type = .standard
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)")
    }
}

@available(iOS 27.0, *)
struct TaskListQuery: EntityStringQuery {
    func entities(for identifiers: [Int]) async throws -> [TaskListEntity] {
        try await TasksIntents.shared.lists(ids: identifiers.map { KotlinLong(longLong: Int64($0)) }).map(TaskListEntity.init)
    }

    func entities(matching string: String) async throws -> [TaskListEntity] {
        try await suggestedEntities().filter { $0.name.localizedCaseInsensitiveContains(string) }
    }

    func suggestedEntities() async throws -> [TaskListEntity] {
        try await TasksIntents.shared.writableLists().map(TaskListEntity.init)
    }

    func defaultResult() async -> TaskListEntity? {
        (try? await TasksIntents.shared.defaultList()).map(TaskListEntity.init)
    }
}

@available(iOS 27.0, *)
@AppEnum(schema: .reminders.listType)
enum ListType: String {
    case standard

    static let caseDisplayRepresentations: [ListType: DisplayRepresentation] = [.standard: "Standard"]
}

@available(iOS 27.0, *)
@AppEnum(schema: .reminders.locationTriggerEvent)
enum LocationTriggerEvent: String {
    case arrive
    case depart

    static let caseDisplayRepresentations: [LocationTriggerEvent: DisplayRepresentation] = [
        .arrive: "Arrive",
        .depart: "Depart",
    ]
}

@available(iOS 27.0, *)
@AppEntity(schema: .reminders.locationTrigger)
struct LocationTriggerEntity: TransientAppEntity {
    var place: PlaceDescriptor
    var event: LocationTriggerEvent

    init() {
        place = LocationTriggerEntity.unsetPlace
        event = .arrive
    }

    init(_ trigger: IntentLocationTrigger) {
        self.init(
            name: trigger.name,
            address: trigger.address,
            coordinate: CLLocationCoordinate2D(latitude: trigger.latitude, longitude: trigger.longitude),
            departing: trigger.departing
        )
    }

    init(name: String?, address: String?, coordinate: CLLocationCoordinate2D, departing: Bool) {
        var representations: [PlaceDescriptor.PlaceRepresentation] = [.coordinate(coordinate)]
        if let address { representations.append(.address(address)) }
        place = PlaceDescriptor(representations: representations, commonName: name)
        event = departing ? .depart : .arrive
    }

    private static let unsetPlace = PlaceDescriptor(
        representations: [.coordinate(CLLocationCoordinate2D(latitude: 0, longitude: 0))],
        commonName: nil
    )

    func write() async throws -> IntentLocationTriggerWrite {
        let coordinate: CLLocationCoordinate2D
        if let known = place.coordinate, place != LocationTriggerEntity.unsetPlace {
            coordinate = known
        } else if let address = place.address,
                  let located = try await CLGeocoder().geocodeAddressString(address).first?.location?.coordinate {
            coordinate = located
        } else {
            throw TasksIntentError.placeWithoutCoordinates
        }
        return IntentLocationTriggerWrite(
            name: place.commonName,
            address: place.address,
            latitude: coordinate.latitude,
            longitude: coordinate.longitude,
            departing: event == .depart
        )
    }

    var displayRepresentation: DisplayRepresentation {
        let name = place.commonName ?? place.address ?? "Location"
        return DisplayRepresentation(title: "\(event == .depart ? "Leaving" : "Arriving at") \(name)")
    }
}

@available(iOS 27.0, *)
@AppEntity(schema: .reminders.section)
struct SectionEntity {
    static let defaultQuery = SectionQuery()

    let id: Int

    var name: String
    var list: TaskListEntity

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)")
    }
}

@available(iOS 27.0, *)
struct SectionQuery: EntityStringQuery {
    func entities(for identifiers: [Int]) async throws -> [SectionEntity] { [] }

    func entities(matching string: String) async throws -> [SectionEntity] { [] }
}

enum TaskPriority: String, AppEnum {
    case high, medium, low
    case noPriority = "none"

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Priority"
    static var caseDisplayRepresentations: [TaskPriority: DisplayRepresentation] = [
        .high: "High",
        .medium: "Medium",
        .low: "Low",
        .noPriority: "None",
    ]
}

@available(iOS 27.0, *)
@AppIntent(schema: .reminders.createReminder)
struct CreateTaskIntent {
    var title: String
    var list: TaskListEntity?
    var note: AttributedString?
    var isFlagged: Bool?
    var images: [IntentFile]
    var tags: Set<String>
    var urls: [URL]
    var dueDate: DateComponents?
    var recurrence: Calendar.RecurrenceRule?
    var locationTrigger: LocationTriggerEntity?
    var section: SectionEntity?

    static var parameterSummary: some ParameterSummary {
        Summary("Create \(\.$title) in \(\.$list)") {
            \.$dueDate
            \.$recurrence
            \.$tags
            \.$note
            \.$locationTrigger
            \.$isFlagged
        }
    }

    func perform() async throws -> some ReturnsValue<TaskEntity> & ProvidesDialog {
        let due = dueDate.flatMap(dueMillis)
        let created = try await TasksIntents.shared.createTask(
            title: title,
            notes: note.map { String($0.characters) },
            due: due.map { KotlinLong(longLong: $0.millis) },
            dueAllDay: due?.allDay ?? false,
            priority: isFlagged == true ? TaskPriority.high.rawValue : nil,
            recurrence: recurrence.map(Recurrence.rrule(from:)),
            listId: list.map { KotlinLong(longLong: Int64($0.id)) },
            tags: Array(tags),
            locationTrigger: try await locationTrigger?.write()
        )
        return .result(value: TaskEntity(created), dialog: "Created “\(created.title)”")
    }
}

@available(iOS 27.0, *)
@AppIntent(schema: .reminders.updateReminder)
struct UpdateTaskIntent {
    var target: TaskEntity
    var title: String?
    var note: AttributedString?
    var tags: Set<String>?
    var urls: [URL]?
    var dueDate: DateComponents?
    var recurrence: Calendar.RecurrenceRule?
    var isCompleted: Bool?
    var isFlagged: Bool?
    var list: TaskListEntity?
    var locationTrigger: LocationTriggerEntity?

    static var parameterSummary: some ParameterSummary {
        Summary("Update \(\.$target)") {
            \.$isCompleted
            \.$dueDate
            \.$title
            \.$list
            \.$tags
            \.$note
            \.$recurrence
            \.$locationTrigger
            \.$isFlagged
        }
    }

    func perform() async throws -> some ReturnsValue<TaskEntity> & ProvidesDialog {
        let due = dueDate.flatMap(dueMillis)
        let updated = try await TasksIntents.shared.updateTask(
            id: Int64(target.id),
            title: title,
            notes: note.map { String($0.characters) },
            due: due.map { KotlinLong(longLong: $0.millis) },
            dueAllDay: due?.allDay ?? false,
            priority: isFlagged.map { $0 ? TaskPriority.high.rawValue : TaskPriority.noPriority.rawValue },
            recurrence: recurrence.map(Recurrence.rrule(from:)),
            listId: list.map { KotlinLong(longLong: Int64($0.id)) },
            completed: isCompleted.map { KotlinBoolean(bool: $0) },
            tags: tags.map(Array.init),
            locationTrigger: try await locationTrigger?.write()
        )
        let dialog: IntentDialog = isCompleted == true ? "Completed “\(updated.title)”" : "Updated “\(updated.title)”"
        return .result(value: TaskEntity(updated), dialog: dialog)
    }
}

@available(iOS 27.0, *)
@AppIntent(schema: .reminders.deleteReminders)
struct DeleteTasksIntent: DeleteIntent {
    var entities: [TaskEntity]

    static var parameterSummary: some ParameterSummary {
        Summary("Delete \(\.$entities)")
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let deleted = try await TasksIntents.shared.deleteTasks(ids: entities.map { KotlinLong(longLong: Int64($0.id)) })
        return .result(dialog: "Deleted \(deleted) task\(deleted == 1 ? "" : "s")")
    }
}

@available(iOS 27.0, *)
@AppIntent(schema: .reminders.createList)
struct CreateListIntent {
    var type: ListType
    var name: String

    static var parameterSummary: some ParameterSummary {
        Summary("Create list \(\.$name)")
    }

    func perform() async throws -> some ReturnsValue<TaskListEntity> & ProvidesDialog {
        let created = try await TasksIntents.shared.createList(title: name)
        return .result(value: TaskListEntity(created), dialog: "Created list “\(created.title)”")
    }
}

@available(iOS 27.0, *)
struct CreateLocationTriggerIntent: AppIntent {
    static var title: LocalizedStringResource = "Make Location Reminder"
    static var description = IntentDescription("Makes a location reminder to attach to a task, triggered on arriving at or leaving a place.")

    @Parameter(title: "Place") var place: CLPlacemark
    @Parameter(title: "When", default: .arrive) var event: LocationTriggerEvent

    static var parameterSummary: some ParameterSummary {
        Summary("Remind on \(\.$event) at \(\.$place)")
    }

    func perform() async throws -> some IntentResult & ReturnsValue<LocationTriggerEntity> {
        guard let coordinate = place.location?.coordinate else { throw TasksIntentError.placeWithoutCoordinates }
        let street = [place.subThoroughfare, place.thoroughfare].compactMap { $0 }.joined(separator: " ")
        let address = [street, place.locality, place.administrativeArea, place.postalCode]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
        let entity = LocationTriggerEntity(
            name: place.name,
            address: address.isEmpty ? nil : address,
            coordinate: coordinate,
            departing: event == .depart
        )
        return .result(value: entity)
    }
}

enum TasksIntentError: Error, CustomLocalizedStringResourceConvertible {
    case placeWithoutCoordinates

    var localizedStringResource: LocalizedStringResource {
        switch self {
        case .placeWithoutCoordinates: "That place has no coordinates."
        }
    }
}

private func date(millis: KotlinLong) -> Date {
    Date(timeIntervalSince1970: Double(millis.int64Value) / 1000)
}

private func dueComponents(millis: Int64, allDay: Bool) -> DateComponents {
    let date = Date(timeIntervalSince1970: Double(millis) / 1000)
    var components = Calendar.current.dateComponents(
        allDay ? [.year, .month, .day] : [.year, .month, .day, .hour, .minute],
        from: date
    )
    components.calendar = Calendar.current
    return components
}

private func dueMillis(_ components: DateComponents) -> (millis: Int64, allDay: Bool)? {
    let calendar = components.calendar ?? .current
    let timed = components.hour != nil || components.minute != nil
    var normalized = components
    normalized.calendar = nil
    normalized.timeZone = nil
    if !timed {
        normalized.hour = 0
        normalized.minute = 0
    }
    normalized.second = 0
    guard let date = calendar.date(from: normalized) else { return nil }
    return (Int64(date.timeIntervalSince1970 * 1000), !timed)
}
