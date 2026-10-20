group = "com.willfp"
version = rootProject.version

dependencies {
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("com.willfp:libreforge:2026.27:all") { isTransitive = false }
    compileOnly("io.lumine:Mythic-Dist:5.13.0") { isTransitive = false }
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("com.willfp:eco:${rootProject.findProperty("eco-version")}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly(fileTree("../../lib") {
        include("*.jar")
    }
    )
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("net.essentialsx:EssentialsX:2.19.7") {
        exclude("*", "*")
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
    compileTestKotlin {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
    build {
        dependsOn(publishToMavenLocal)
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["java"])
            artifactId = rootProject.name
        }
    }

    publishing {
        repositories {
            maven {
                name = "Auxilor"
                url = uri("https://repo.auxilor.io/repository/maven-releases/")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }
}
