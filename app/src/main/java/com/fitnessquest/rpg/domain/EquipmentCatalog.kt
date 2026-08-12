package com.fitnessquest.rpg.domain

/**
 * Gym “stations” used for browse-by-equipment. Images live at
 * `assets/equipment/{id}.png` (see scripts/equipment_image_prompts.txt).
 */
enum class EquipmentFamily(val label: String) {
    FREE_WEIGHTS("Free weights"),
    MACHINES("Machines"),
    CABLES("Cables"),
    CARDIO("Cardio"),
    BODYWEIGHT("Bodyweight"),
    ACCESSORIES("Accessories")
}

data class EquipmentStation(
    val id: String,
    val label: String,
    val family: EquipmentFamily,
    /** Matches free-exercise-db `equipment` field (lowercase). */
    val dbTags: Set<String>,
    /**
     * When non-empty, exercise names must contain one of these phrases
     * (used to split generic "machine" into specific stations).
     */
    val nameContains: List<String> = emptyList(),
    /** Reject matches when the exercise name contains any of these phrases. */
    val nameExcludes: List<String> = emptyList(),
    val subtitle: String = "",
    val remoteImageUrl: String? = null
) {
    val imageUri: String get() = remoteImageUrl ?: "file:///android_asset/equipment/$id.png"
}

object EquipmentCatalog {
    val all: List<EquipmentStation> = listOf(
        EquipmentStation(
            id = "barbell",
            label = "Barbell",
            family = EquipmentFamily.FREE_WEIGHTS,
            dbTags = setOf("barbell"),
            subtitle = "Olympic bar + plates"
        ),
        EquipmentStation(
            id = "dumbbell",
            label = "Dumbbells",
            family = EquipmentFamily.FREE_WEIGHTS,
            dbTags = setOf("dumbbell"),
            subtitle = "Adjustable or fixed"
        ),
        EquipmentStation(
            id = "ez_bar",
            label = "EZ curl bar",
            family = EquipmentFamily.FREE_WEIGHTS,
            dbTags = setOf("e-z curl bar"),
            subtitle = "Angled curl bar"
        ),
        EquipmentStation(
            id = "kettlebell",
            label = "Kettlebell",
            family = EquipmentFamily.FREE_WEIGHTS,
            dbTags = setOf("kettlebells"),
            subtitle = "Swings, presses, carries"
        ),
        EquipmentStation(
            id = "cable_machine",
            label = "Cable machine",
            family = EquipmentFamily.CABLES,
            dbTags = setOf("cable"),
            subtitle = "Dual stack / functional trainer"
        ),
        EquipmentStation(
            id = "smith_machine",
            label = "Smith machine",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("smith"),
            subtitle = "Guided barbell path"
        ),
        EquipmentStation(
            id = "leg_press_plate",
            label = "Leg press",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("leg press"),
            subtitle = "Plate-loaded or sled"
        ),
        EquipmentStation(
            id = "hack_squat",
            label = "Hack squat",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("hack squat"),
            subtitle = "Angled squat machine"
        ),
        EquipmentStation(
            id = "chest_press_machine",
            label = "Chest press",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("chest press", "bench press machine", "machine bench"),
            subtitle = "Seated press machine"
        ),
        EquipmentStation(
            id = "shoulder_press_machine",
            label = "Shoulder press",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("shoulder press", "machine military", "machine shoulder"),
            subtitle = "Overhead press machine"
        ),
        EquipmentStation(
            id = "lat_pulldown",
            label = "Lat pulldown",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine", "cable"),
            nameContains = listOf("lat pulldown", "pulldown"),
            subtitle = "High cable pulldown"
        ),
        EquipmentStation(
            id = "seated_row_machine",
            label = "Seated row",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine", "cable"),
            nameContains = listOf("seated row", "cable row", "row machine"),
            subtitle = "Horizontal pull"
        ),
        EquipmentStation(
            id = "leg_curl_machine",
            label = "Leg curl",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("leg curl", "hamstring curl"),
            subtitle = "Lying or seated"
        ),
        EquipmentStation(
            id = "leg_extension_machine",
            label = "Leg extension",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("leg extension"),
            subtitle = "Quad isolation"
        ),
        EquipmentStation(
            id = "hip_adduction_machine",
            label = "Hip adduction",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("hip adduction", "thigh adductor", "adductor machine", "machine adduction"),
            nameExcludes = listOf("cable", "band", "foam", "stretch"),
            subtitle = "Inner-thigh adductors"
        ),
        EquipmentStation(
            id = "calf_raise_machine",
            label = "Calf raise",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("calf raise", "donkey calf"),
            subtitle = "Standing or seated"
        ),
        EquipmentStation(
            id = "calf_press_machine",
            label = "Calf press",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("calf press"),
            nameExcludes = listOf("leg press"),
            subtitle = "Seated ankle press machine"
        ),
        EquipmentStation(
            id = "pec_deck",
            label = "Pec deck / fly",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("pec deck", "butterfly", "machine fly", "chest fly"),
            subtitle = "Chest fly machine"
        ),
        EquipmentStation(
            id = "ab_crunch_machine",
            label = "Ab crunch machine",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf("ab crunch", "crunch machine", "machine crunch"),
            nameExcludes = listOf("vertical", "exercise ball", "hands overhead"),
            subtitle = "Seated straight crunch"
        ),
        EquipmentStation(
            id = "vertical_crunch_machine",
            label = "Vertical crunch",
            family = EquipmentFamily.MACHINES,
            dbTags = setOf("machine"),
            nameContains = listOf(
                "vertical crunch",
                "upright crunch",
                "swivel crunch",
                "captain chair crunch",
                "standing crunch machine",
                "upright ab crunch"
            ),
            subtitle = "Upright / swivel core machine"
        ),
        EquipmentStation(
            id = "treadmill",
            label = "Treadmill",
            family = EquipmentFamily.CARDIO,
            dbTags = setOf("machine"),
            nameContains = listOf("treadmill", "jogging", "walking, treadmill", "running, treadmill"),
            subtitle = "Walk / jog / run"
        ),
        EquipmentStation(
            id = "stationary_bike",
            label = "Stationary bike",
            family = EquipmentFamily.CARDIO,
            dbTags = setOf("machine"),
            nameContains = listOf("bicycling", "bike", "cycle"),
            subtitle = "Upright or spin"
        ),
        EquipmentStation(
            id = "rower",
            label = "Rowing machine",
            family = EquipmentFamily.CARDIO,
            dbTags = setOf("machine"),
            nameContains = listOf("rowing"),
            subtitle = "Erg rower"
        ),
        EquipmentStation(
            id = "elliptical",
            label = "Elliptical",
            family = EquipmentFamily.CARDIO,
            dbTags = setOf("machine"),
            nameContains = listOf("elliptical"),
            subtitle = "Low-impact cardio"
        ),
        EquipmentStation(
            id = "stair_climber",
            label = "Stair climber",
            family = EquipmentFamily.CARDIO,
            dbTags = setOf("machine"),
            nameContains = listOf("stair", "stepmill"),
            subtitle = "Stairmill / stepper"
        ),
        EquipmentStation(
            id = "pull_up_bar",
            label = "Pull-up bar",
            family = EquipmentFamily.BODYWEIGHT,
            dbTags = setOf("body only"),
            nameContains = listOf("pull-up", "pullup", "chin-up", "chinup"),
            subtitle = "Bar hang / pulls"
        ),
        EquipmentStation(
            id = "dip_station",
            label = "Dip station",
            family = EquipmentFamily.BODYWEIGHT,
            dbTags = setOf("body only", "other"),
            nameContains = listOf("dip"),
            subtitle = "Parallel bars"
        ),
        EquipmentStation(
            id = "bodyweight",
            label = "Bodyweight / floor",
            family = EquipmentFamily.BODYWEIGHT,
            dbTags = setOf("body only"),
            subtitle = "No equipment needed"
        ),
        EquipmentStation(
            id = "flat_bench",
            label = "Flat bench",
            family = EquipmentFamily.ACCESSORIES,
            dbTags = setOf("barbell", "dumbbell", "other"),
            nameContains = listOf("bench"),
            subtitle = "Press & support"
        ),
        EquipmentStation(
            id = "resistance_band",
            label = "Resistance bands",
            family = EquipmentFamily.ACCESSORIES,
            dbTags = setOf("bands"),
            subtitle = "Loop / tube bands"
        ),
        EquipmentStation(
            id = "foam_roller",
            label = "Foam roller",
            family = EquipmentFamily.ACCESSORIES,
            dbTags = setOf("foam roll"),
            subtitle = "Recovery"
        ),
        EquipmentStation(
            id = "medicine_ball",
            label = "Medicine ball",
            family = EquipmentFamily.ACCESSORIES,
            dbTags = setOf("medicine ball"),
            subtitle = "Throws & holds"
        ),
        EquipmentStation(
            id = "exercise_ball",
            label = "Exercise ball",
            family = EquipmentFamily.ACCESSORIES,
            dbTags = setOf("exercise ball"),
            subtitle = "Stability ball"
        )
    )

    fun byId(id: String): EquipmentStation? = all.find { it.id == id }

    fun byFamily(family: EquipmentFamily): List<EquipmentStation> =
        all.filter { it.family == family }

    fun matches(station: EquipmentStation, exerciseName: String, equipmentField: String): Boolean {
        val eq = equipmentField.trim().lowercase()
        val name = exerciseName.lowercase()
        val tagHit = station.dbTags.any { tag ->
            eq == tag || eq.contains(tag) || tag.contains(eq)
        }
        if (station.nameContains.isEmpty()) {
            // Broad categories: require equipment tag match only.
            return tagHit && eq.isNotBlank()
        }
        val nameHit = station.nameContains.any { needle -> name.contains(needle) }
        val excluded = station.nameExcludes.any { needle -> name.contains(needle) }
        if (!nameHit || excluded) return false
        // Specific stations: name is the primary signal; tag helps when present.
        return tagHit || eq.isBlank() || eq == "other" || eq == "machine" || eq == "cable"
    }

    /** Stations an exercise is likely performed on (for detail UI). Prefer specific name hits. */
    fun stationsFor(exerciseName: String, equipmentField: String): List<EquipmentStation> {
        val hits = all.filter { matches(it, exerciseName, equipmentField) }
        if (hits.isEmpty()) return emptyList()
        val specific = hits.filter { it.nameContains.isNotEmpty() }
        return (specific.ifEmpty { hits }).distinctBy { it.id }
    }
}
