plugins {
    application
    id("org.graalvm.buildtools.native") version "0.10.6"
    id("com.gradleup.shadow") version "8.3.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.guava)
    implementation("io.github.bonede:tree-sitter:0.25.3")
    implementation("io.github.bonede:tree-sitter-java:0.23.4")
    implementation("info.picocli:picocli:4.7.7")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("net.java.dev.inflector:inflector:0.7.0")

    testImplementation(libs.junit.jupiter)
    testImplementation("org.slf4j:slf4j-simple:2.0.17")

    annotationProcessor("org.projectlombok:lombok:1.18.38")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")
	
    compileOnly("org.projectlombok:lombok:1.18.38")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

application {
    mainClass = "io.github.syntaxpresso.core.Core"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

graalvmNative {
    testSupport.set(true)
    binaries {
        named("main") {
            mainClass.set("io.github.syntaxpresso.core.Core")
            buildArgs.add("--enable-native-access=ALL-UNNAMED")
        }
    }
    toolchainDetection.set(true)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
}

tasks.register<JavaExec>("generateNativeConfig") {
    group = "GraalVM Native"
    description = "Generates GraalVM native-image configuration using the agent."
    if (gradle.startParameter.taskNames.contains("generateNativeConfig")) {
        val nativeImageConfigDir = file("src/main/resources/META-INF/native-image")
        doFirst {
            nativeImageConfigDir.mkdirs()
        }
        classpath(
            tasks.shadowJar
                .get()
                .outputs.files,
        )
        mainClass.set(application.mainClass)
        jvmArgs(
            "-agentlib:native-image-agent=config-output-dir=${nativeImageConfigDir.absolutePath}",
        )
        if (project.hasProperty("appArgs")) {
            val appArgs = (project.property("appArgs") as String).split(" ")
            args(appArgs)
        } else {
            throw GradleException(
                "Missing required project property 'appArgs'.\nUsage: ./gradlew generateNativeConfig -PappArgs=\"<your arguments>\"",
            )
        }
    }
}
