package tmg

object NoOpSimulationObserver : ISimulationObserver {
    override fun onStep(
        stepIndex: Int,
        timestamp: Double,
        snapshot: MapSnapshot,
    ) {
        // Intentionally does nothing — used wherever an ISimulationObserver
        // is required but the caller doesn't need per-step snapshots.
    }
}
