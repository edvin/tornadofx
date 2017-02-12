package tornadofx

class MessageEvent(val message: String, val severity: ValidationSeverity) : FXEvent() {
    enum class Severity { Info, Warning, Error }
}

class EventLogView