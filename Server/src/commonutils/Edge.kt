package commonutils

class Edge(
    var sourceNode: Node,
    var targetNode: Node,
    var distance: Double,
    var flags: Boolean,
    var density: Double,
    var averageSpeed: Double,
    var congestionFactor: Double,
    var weight: Double,
)
