import Foundation
import PostHog
import ComposeApp

final class PostHogBridge: AnalyticsBridge {
    func setup(apiKey: String, host: String) {
        let config = PostHogConfig(projectToken: apiKey, host: host)
        config.preloadFeatureFlags = false
        config.sendFeatureFlagEvent = false
        config.captureScreenViews = false
        PostHogSDK.shared.setup(config)
    }

    func capture(event: String, properties: [String: Any]) {
        PostHogSDK.shared.capture(
            event,
            properties: properties.mapValues { ($0 as? KotlinBoolean)?.boolValue ?? $0 }
        )
    }

    func register(key: String, value: String) {
        PostHogSDK.shared.register([key: value])
    }

    func unregister(key: String) {
        PostHogSDK.shared.unregister(key)
    }
}
