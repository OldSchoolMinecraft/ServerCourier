plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("libs/poseidon.jar"))
    implementation(files("libs/Essentials.jar"))

    implementation("dnsjava:dnsjava:3.5.2")
}