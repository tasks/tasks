import Foundation

@available(iOS 26.0, macOS 26.0, *)
enum Recurrence {
    static func rule(from rrule: String, calendar: Calendar = .current) -> Calendar.RecurrenceRule? {
        var parts: [String: String] = [:]
        for part in rrule.replacingOccurrences(of: "RRULE:", with: "").split(separator: ";") {
            let pair = part.split(separator: "=", maxSplits: 1)
            guard pair.count == 2 else { continue }
            parts[pair[0].uppercased()] = String(pair[1])
        }
        guard let frequency = parts["FREQ"].flatMap(frequency(named:)) else { return nil }
        var calendar = calendar
        if let start = parts["WKST"].flatMap(weekday(named:)) {
            calendar.firstWeekday = weekdayNumbers[start] ?? calendar.firstWeekday
        }
        var rule = Calendar.RecurrenceRule(calendar: calendar, frequency: frequency)
        rule.interval = parts["INTERVAL"].flatMap(Int.init) ?? 1
        if let count = parts["COUNT"].flatMap(Int.init) {
            rule.end = .afterOccurrences(count)
        } else if let until = parts["UNTIL"].flatMap({ untilDate($0, calendar: calendar) }) {
            rule.end = .afterDate(until)
        }
        rule.weekdays = parts["BYDAY"].map(weekdays(from:)) ?? []
        rule.daysOfTheMonth = ints(parts["BYMONTHDAY"])
        rule.months = ints(parts["BYMONTH"]).map { Calendar.RecurrenceRule.Month($0) }
        rule.seconds = ints(parts["BYSECOND"])
        rule.minutes = ints(parts["BYMINUTE"])
        rule.hours = ints(parts["BYHOUR"])
        rule.daysOfTheYear = ints(parts["BYYEARDAY"])
        rule.weeks = ints(parts["BYWEEKNO"])
        rule.setPositions = ints(parts["BYSETPOS"])
        return rule
    }

    static func rrule(from rule: Calendar.RecurrenceRule) -> String {
        var parts = ["FREQ=\(name(of: rule.frequency))"]
        if rule.calendar.firstWeekday != 2, let start = weekdayNames.first(where: { $0.value == rule.calendar.firstWeekday }) {
            parts.append("WKST=\(start.key)")
        }
        if let date = rule.end.date {
            parts.append("UNTIL=\(untilString(date, calendar: rule.calendar))")
        } else if let count = rule.end.occurrences {
            parts.append("COUNT=\(count)")
        }
        if rule.interval != 1 { parts.append("INTERVAL=\(rule.interval)") }
        func append(_ key: String, _ values: [Int]) {
            if !values.isEmpty { parts.append("\(key)=\(values.map(String.init).joined(separator: ","))") }
        }
        append("BYMONTH", rule.months.map(\.index))
        append("BYWEEKNO", rule.weeks)
        append("BYYEARDAY", rule.daysOfTheYear)
        append("BYMONTHDAY", rule.daysOfTheMonth)
        if !rule.weekdays.isEmpty {
            parts.append("BYDAY=\(rule.weekdays.map(name(of:)).joined(separator: ","))")
        }
        append("BYHOUR", rule.hours)
        append("BYMINUTE", rule.minutes)
        append("BYSECOND", rule.seconds)
        append("BYSETPOS", rule.setPositions)
        return parts.joined(separator: ";")
    }

    private static let weekdayCodes: [String: Locale.Weekday] = [
        "SU": .sunday, "MO": .monday, "TU": .tuesday, "WE": .wednesday,
        "TH": .thursday, "FR": .friday, "SA": .saturday,
    ]

    private static let weekdayNumbers: [Locale.Weekday: Int] = [
        .sunday: 1, .monday: 2, .tuesday: 3, .wednesday: 4, .thursday: 5, .friday: 6, .saturday: 7,
    ]

    private static let weekdayNames: [String: Int] = [
        "SU": 1, "MO": 2, "TU": 3, "WE": 4, "TH": 5, "FR": 6, "SA": 7,
    ]

    private static func frequency(named name: String) -> Calendar.RecurrenceRule.Frequency? {
        switch name.uppercased() {
        case "MINUTELY": .minutely
        case "HOURLY": .hourly
        case "DAILY": .daily
        case "WEEKLY": .weekly
        case "MONTHLY": .monthly
        case "YEARLY": .yearly
        default: nil
        }
    }

    private static func name(of frequency: Calendar.RecurrenceRule.Frequency) -> String {
        switch frequency {
        case .minutely: "MINUTELY"
        case .hourly: "HOURLY"
        case .daily: "DAILY"
        case .weekly: "WEEKLY"
        case .monthly: "MONTHLY"
        case .yearly: "YEARLY"
        @unknown default: "DAILY"
        }
    }

    private static func weekday(named name: String) -> Locale.Weekday? {
        weekdayCodes[name.uppercased()]
    }

    private static func name(of weekday: Locale.Weekday) -> String {
        weekdayCodes.first { $0.value == weekday }?.key ?? "MO"
    }

    private static func weekdays(from value: String) -> [Calendar.RecurrenceRule.Weekday] {
        value.split(separator: ",").compactMap { entry in
            let text = String(entry).uppercased()
            guard text.count >= 2, let day = weekday(named: String(text.suffix(2))) else { return nil }
            let ordinal = text.dropLast(2)
            if ordinal.isEmpty { return .every(day) }
            guard let n = Int(ordinal.hasPrefix("+") ? String(ordinal.dropFirst()) : String(ordinal)), n != 0 else {
                return nil
            }
            return .nth(n, day)
        }
    }

    private static func name(of weekday: Calendar.RecurrenceRule.Weekday) -> String {
        switch weekday {
        case .every(let day): name(of: day)
        case .nth(let n, let day): "\(n)\(name(of: day))"
        @unknown default: "MO"
        }
    }

    private static func ints(_ value: String?) -> [Int] {
        value?.split(separator: ",").compactMap { Int($0) } ?? []
    }

    private static func untilDate(_ value: String, calendar: Calendar) -> Date? {
        let digits = value.filter(\.isNumber)
        guard digits.count >= 8, let year = Int(digits.prefix(4)),
              let month = Int(digits.dropFirst(4).prefix(2)), let day = Int(digits.dropFirst(6).prefix(2))
        else { return nil }
        var components = DateComponents(year: year, month: month, day: day)
        if digits.count >= 14 {
            components.hour = Int(digits.dropFirst(8).prefix(2))
            components.minute = Int(digits.dropFirst(10).prefix(2))
            components.second = Int(digits.dropFirst(12).prefix(2))
        } else {
            components.hour = 23
            components.minute = 59
            components.second = 59
        }
        var target = calendar
        if value.hasSuffix("Z") { target.timeZone = TimeZone(identifier: "UTC")! }
        return target.date(from: components)
    }

    private static func untilString(_ date: Date, calendar: Calendar) -> String {
        let local = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
        if local.hour == 23, local.minute == 59, local.second == 59 {
            return String(format: "%04d%02d%02d", local.year!, local.month!, local.day!)
        }
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        let c = utc.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
        return String(
            format: "%04d%02d%02dT%02d%02d%02dZ",
            c.year!, c.month!, c.day!, c.hour!, c.minute!, c.second!
        )
    }
}
