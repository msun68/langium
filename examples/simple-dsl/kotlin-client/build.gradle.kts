import java.net.URI

plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
    useJUnitPlatform()
    
    // Start the DSL parser server before tests
    dependsOn("startDslParser")
    
    // Stop the server after tests
    finalizedBy("stopDslParser")
}

val dslParserDir = file("..")
val dslParserPidFile = file("build/dsl-parser.pid")

// Task to install npm dependencies
tasks.register<Exec>("npmInstall") {
    workingDir = dslParserDir
    commandLine("npm", "install")
}

// Task to build the DSL parser
tasks.register<Exec>("buildDslParser") {
    dependsOn("npmInstall")
    workingDir = dslParserDir
    commandLine("npm", "run", "build")
}

// Task to start the DSL parser server
tasks.register<Exec>("startDslParser") {
    dependsOn("buildDslParser")
    workingDir = dslParserDir
    
    // Create build directory if it doesn't exist
    doFirst {
        file("build").mkdirs()
    }
    
    // Start the server in the background
    commandLine("bash", "-c", """
        npm start > ${file("build/dsl-parser.log").absolutePath} 2>&1 & echo $! > ${dslParserPidFile.absolutePath}
    """.trimIndent())
    
    doLast {
        // Wait for server to be ready
        println("Waiting for DSL parser server to start...")
        Thread.sleep(3000)
        
        // Verify server is running
        try {
            val url = URI("http://localhost:3000/health").toURL()
            val response = url.readText()
            if (response.contains("ok")) {
                println("DSL parser server started successfully")
            } else {
                throw GradleException("DSL parser server failed to start")
            }
        } catch (e: Exception) {
            throw GradleException("Failed to connect to DSL parser server: ${e.message}")
        }
    }
}

// Task to stop the DSL parser server
tasks.register("stopDslParser") {
    doLast {
        if (dslParserPidFile.exists()) {
            val pid = dslParserPidFile.readText().trim()
            try {
                Runtime.getRuntime().exec(arrayOf("kill", pid))
                println("Stopped DSL parser server (PID: $pid)")
            } catch (e: Exception) {
                println("Warning: Could not stop DSL parser server: ${e.message}")
            } finally {
                dslParserPidFile.delete()
            }
        }
    }
}

// Clean up pid file
tasks.clean {
    doLast {
        dslParserPidFile.delete()
    }
}
