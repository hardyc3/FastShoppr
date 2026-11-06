plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
}

spotless {
    java {
        target("app/src/**/*.java")
        licenseHeaderFile(rootProject.file("spotless/license-header.txt"))
        googleJavaFormat("1.28.0")
        removeUnusedImports()
        importOrder()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
