// Fix SSL through corporate proxy
System.setProperty("javax.net.ssl.trustStoreType", "WINDOWS-ROOT")

pluginManagement {
    repositories {
        // repo1.maven.org — зеркало Maven Central, не блокируется антивирусом
        maven { url = uri("https://repo1.maven.org/maven2") }
        maven { url = uri("https://plugins.gradle.org/m2") }
    }
}
