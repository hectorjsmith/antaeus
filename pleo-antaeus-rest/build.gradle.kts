plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))

    implementation("io.javalin:javalin:4.3.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:2.1.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.3")
}
