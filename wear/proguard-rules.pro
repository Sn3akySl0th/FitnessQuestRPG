# Keep Shared protocol models between phone and wear
-keep class com.fitnessquest.shared.** { *; }
-keep class com.fitnessquest.rpg.wear.** { *; }

# Ignore test classes pulled into runtime classpath
-dontwarn org.junit.**
-dontwarn org.graalvm.**
