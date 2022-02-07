plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    // https://mvnrepository.com/artifact/org.quartz-scheduler/quartz
    implementation("org.quartz-scheduler:quartz:2.3.2")
}
