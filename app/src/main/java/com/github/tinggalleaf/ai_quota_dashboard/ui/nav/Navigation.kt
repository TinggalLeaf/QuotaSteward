package com.github.tinggalleaf.ai_quota_dashboard.ui.nav

sealed class Destination(val route: String) {
    data object Dashboard : Destination("dashboard")
    data object Services : Destination("services")
    data object Settings : Destination("settings")

    data object AddService : Destination("service/edit/new") {
        const val PATH = "service/edit"
        const val ARG = "id"
        fun build(id: String) = "$PATH/$id"
    }

    data object EditService : Destination("service/edit/{id}") {
        fun build(id: String) = "service/edit/$id"
        const val PATH = "service/edit/{id}"
        const val ARG = "id"
    }

    data object AddFromPreset : Destination("preset") {
        fun build() = "preset"
    }
}
