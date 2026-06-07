// Top-level build file — aquí NO van dependencias de la app,
// solo plugins que aplican a todos los módulos
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}