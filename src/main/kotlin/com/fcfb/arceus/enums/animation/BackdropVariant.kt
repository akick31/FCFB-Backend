package com.fcfb.arceus.enums.animation

enum class BackdropVariant(
    val backdrop: StadiumBackdrop,
) {
    BUILDING_CITY(StadiumBackdrop.BUILDING),
    BUILDING_TOWN(StadiumBackdrop.BUILDING),
    BUILDING_NEIGHBORHOOD(StadiumBackdrop.BUILDING),
    CAMPUS_GEORGIAN(StadiumBackdrop.CAMPUS),
    CAMPUS_GOTHIC(StadiumBackdrop.CAMPUS),
    CAMPUS_MISSION(StadiumBackdrop.CAMPUS),
    CAMPUS_MODERN(StadiumBackdrop.CAMPUS),
    CAMPUS_ATHLETIC(StadiumBackdrop.CAMPUS),
    CAMPUS_ADMIN(StadiumBackdrop.CAMPUS),
    CAMPUS_FIELDHOUSE(StadiumBackdrop.CAMPUS),
    MOUNTAINS_ALPINE(StadiumBackdrop.MOUNTAINS),
    MOUNTAINS_DESERT(StadiumBackdrop.MOUNTAINS),
    MOUNTAINS_SMOKY(StadiumBackdrop.MOUNTAINS),
    MOUNTAINS_VOLCANO(StadiumBackdrop.MOUNTAINS),
    MOUNTAINS_HILLS(StadiumBackdrop.MOUNTAINS),
    MOUNTAINS_JAGGED(StadiumBackdrop.MOUNTAINS),
    FIELD_CROPS(StadiumBackdrop.FIELD),
    FIELD_PASTURE(StadiumBackdrop.FIELD),
    FIELD_HAY(StadiumBackdrop.FIELD),
    FIELD_ORCHARD(StadiumBackdrop.FIELD),
    LAKE_HILLS(StadiumBackdrop.LAKE),
    LAKE_OPEN(StadiumBackdrop.LAKE),
    LAKE_PINES(StadiumBackdrop.LAKE),
    LAKE_MOUNTAINS(StadiumBackdrop.LAKE),
    LAKE_FLAT(StadiumBackdrop.LAKE),
    FACILITY_BRICK(StadiumBackdrop.FACILITY),
    FACILITY_CONCRETE(StadiumBackdrop.FACILITY),
    FACILITY_TOWER(StadiumBackdrop.FACILITY),
    FACILITY_TOWER_STONE(StadiumBackdrop.FACILITY),
    FACILITY_TOWER_MODERN(StadiumBackdrop.FACILITY),
    FACILITY_NARROW_GEORGIAN(StadiumBackdrop.FACILITY),
    FACILITY_NARROW_MODERN(StadiumBackdrop.FACILITY),
    FACILITY_NARROW_COLLEGIATE(StadiumBackdrop.FACILITY),
    ;

    companion object {
        fun resolve(
            backdrop: StadiumBackdrop,
            stored: String?,
        ): BackdropVariant? =
            entries.firstOrNull { it.backdrop == backdrop && it.name == stored?.uppercase() }
                ?: entries.firstOrNull { it.backdrop == backdrop }
    }
}
