package com.fcfb.arceus.model.appearance

data class StadiumSnapshot(
    val name: String = "",
    val city: String? = null,
    val state: String? = null,
    val country: String = Venue.DEFAULT_COUNTRY,
    val capacity: Int? = null,
    val active: Boolean = true,
    val design: StadiumDesign = StadiumDesign(),
) {
    companion object {
        fun of(venue: Venue): StadiumSnapshot =
            StadiumSnapshot(
                name = venue.name,
                city = venue.city,
                state = venue.state,
                country = venue.country,
                capacity = venue.capacity,
                active = venue.active,
                design = StadiumDesign().also { venue.design.copyInto(it) },
            )
    }
}
