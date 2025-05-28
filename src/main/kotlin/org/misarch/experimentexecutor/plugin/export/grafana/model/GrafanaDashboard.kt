package org.misarch.experimentexecutor.plugin.export.grafana.model

data class GrafanaDashboardConfig(
    val dashboard: GrafanaDashboard,
    val folderId: Int? = null,
    val overwrite: Boolean = false
)

data class GrafanaDashboard(
    val annotations: Annotations,
    val editable: Boolean,
    val fiscalYearStartMonth: Int,
    val graphTooltip: Int,
    val id: Int? = null,
    val links: List<String>? = null,
    val panels: List<Panel>? = null,
    val preload: Boolean = false,
    val refresh: String? = null,
    val schemaVersion: Int,
    val tags: List<String>? = null,
    val templating: Templating? = null,
    val time: Time,
    val timepicker: Timepicker,
    val timezone: String? = null,
    val title: String,
    val uid: String? = null,
    val version: Int,
    val weekStart: String? = null,
)

data class Panel(
    val collapsed: Boolean = false,
    val gridPos: GridPos? = null,
    val id: Int,
    val panels: List<Panel>? = null,
    val title: String,
    val type: String,
    val datasource: Any? = null,
    val targets: List<Target>? = null,
    val pluginVersion: String? = null,
    val options: Map<String, Any>? = null,
    val fieldConfig: FieldConfig? = null,
    val repeat: String? = null,
    val interval: String? = null,
    val maxDataPoints: Int? = null,
)

data class GridPos(
    val h: Int,
    val w: Int,
    val x: Int,
    val y: Int
)

data class Target(
    val expr: String? = null,
    val refId: String,
    val format: String? = null,
    val interval: String? = null,
    val legendFormat: String? = null,
    val datasource: Any? = null,
    val query: Any? = null,
    val editorMode: String? = null,
    val instant: Boolean? = null,
    val range: Boolean? = null,
    val disableTextWrap: Boolean? = null,
    val fullMetaSearch: Boolean? = null,
    val includeNullMetadata: Boolean? = null,
    val useBackend: Boolean? = null,
)

data class Templating(
    val list: List<TemplateVar>
)

data class TemplateVar(
    val name: String,
    val type: String,
    val label: String? = null,
    val query: Any? = null,
    val current: CurrentSelection? = null,
    val options: List<Option>? = null,
    val description: String? = null,
    val definition: String? = null,
    val refresh: String? = null,
    val regex: String? = null,
    val sort: Int? = null,
)

data class CurrentSelection(
    val text: String,
    val value: String
)

data class Option(
    val text: String,
    val value: String,
    val selected: Boolean
)

data class FieldConfig(
    val defaults: Defaults? = null,
    val overrides: List<Any>? = null
)

data class Defaults(
    val color: Color? = null,
    val custom: Map<Any, Any>? = null,
    val mappings: List<Any>? = null,
    val thresholds: Thresholds? = null,
    val unit: String? = null
)

data class Color(
    val mode: String
)

data class Thresholds(
    val mode: String,
    val steps: List<Step>
)

data class Step(
    val color: String,
    val value: Double? = null
)

data class Annotations(
    val list: List<Any>? = null
)

data class Time(
    val from: String,
    val to: String
)

data class Timepicker(
    val refreshIntervals: List<String>? = null,
    val timeOptions: List<String>? = null
)
