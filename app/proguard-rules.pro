# Keep Room DAOs and Entities
-keep class com.fitnessquest.rpg.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Domain models (Enums used in DB and AI parsing)
-keep class com.fitnessquest.rpg.domain.** { *; }

# Keep Shared protocol models between phone and wear
-keep class com.fitnessquest.shared.** { *; }

# Keep Firebase Auth & Firestore models
-keepclassmembers class * {
    @com.google.firebase.database.IgnoreExtraProperties <fields>;
}

# Ignore test classes pulled into runtime classpath
-dontwarn org.junit.**
-dontwarn org.graalvm.**
