plugins {
    kotlin("jvm") version "1.9.20"
    `maven-publish`
    signing
}

group = "cn.chahuyun"
version = project.rootProject.version

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(project(":"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.20-1.0.14")
    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("com.squareup:kotlinpoet-ksp:1.14.2")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "HuYanAuthorize-ksp"
            
            pom {
                name.set("HuYanAuthorize KSP Processor")
                description.set("KSP Processor for HuYanAuthorize")
                url.set("https://github.com/moyuyanli/HuYanAuthorize")
                licenses {
                    license {
                        name.set("Apache-2.0 License")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("moyuyanli")
                        name.set("moyuyanli")
                        email.set("572490972@qq.com")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/moyuyanli/HuYanAuthorize.git")
                    developerConnection.set("scm:git:ssh://github.com/moyuyanli/HuYanAuthorize.git")
                    url.set("https://github.com/moyuyanli/HuYanAuthorize")
                }
            }
        }
    }
    repositories {
        maven {
            name = "local"
            url = rootProject.file("local").toURI()
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

