package com.fitnessquest.rpg.domain

/**
 * Advanced set types for workout logging.
 */
enum class SetType(val label: String, val shortLabel: String, val xpMultiplier: Float = 1.0f) {
    /** Standard working set. */
    NORMAL("Normal", "N"),
    
    /** Lower intensity set to prepare for working sets. Reduced XP. */
    WARM_UP("Warm-up", "W", 0.5f),
    
    /** Set performed immediately after another set with reduced weight. Bonus XP. */
    DROP_SET("Drop Set", "D", 1.2f),
    
    /** Set taken to absolute muscular failure. Bonus XP but higher energy cost. */
    FAILURE("Failure", "F", 1.25f),
    
    /** The heaviest or most intense set of an exercise. */
    TOP_SET("Top Set", "T", 1.1f),
    
    /** Lower weight set after a top set to add volume. */
    BACK_OFF("Back-off", "B", 1.0f)
}
