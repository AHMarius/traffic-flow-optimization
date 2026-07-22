package tmg

interface ISimulationObserver {
    fun onStep(
        stepIndex: Int,
        timestamp: Double,
        snapshot: MapSnapshot,
    )
}
