rootProject.name = "ktornea"

include(":utils", ":apache")

rootProject.children.forEach { child -> child.name = "ktornea-${child.name}"}