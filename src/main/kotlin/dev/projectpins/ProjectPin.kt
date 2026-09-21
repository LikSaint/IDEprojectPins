package dev.projectpins

/** Mutable XML beans never escape the service without a defensive copy. List position is order. */
data class ProjectPin(
    var id: String = "",
    var name: String = "",
    var targetPath: String = "",
    var directory: Boolean = false,
    var openInEditor: Boolean = false,
)

data class PinsState(var pins: MutableList<ProjectPin> = mutableListOf())
