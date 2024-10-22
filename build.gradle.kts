plugins {
    id("net.neoforged.moddev") version "2.0.30-beta"
    id("com.almostreliable.almostgradle") version "1.1.1"
}

almostgradle.setup {
    withSourcesJar = false
}

repositories {
    // CC: Tweaked
    maven("https://maven.squiddev.cc/")

    mavenLocal()
}

dependencies {
    // CC: Tweaked
    compileOnly("cc.tweaked:cc-tweaked-${almostgradle.minecraftVersion}-common-api:${almostgradle.getProperty("cctVersion")}")
}
