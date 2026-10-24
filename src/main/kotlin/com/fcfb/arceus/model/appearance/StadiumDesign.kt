package com.fcfb.arceus.model.appearance

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class StadiumDesign {
    @Basic
    @Column(name = "stadium_backdrop")
    var stadiumBackdrop: String? = null

    @Basic
    @Column(name = "stadium_size")
    var stadiumSize: Double = 1.0

    @Basic
    @Column(name = "right_stadium_backdrop")
    var rightStadiumBackdrop: String? = null

    @Basic
    @Column(name = "right_stadium_size")
    var rightStadiumSize: Double? = null

    @Basic
    @Column(name = "jumbotron_enabled")
    var jumbotronEnabled: Boolean = false

    @Basic
    @Column(name = "right_jumbotron_enabled")
    var rightJumbotronEnabled: Boolean = false

    @Basic
    @Column(name = "jumbotron_pylon_mount")
    var jumbotronPylonMount: Boolean = false

    @Basic
    @Column(name = "right_jumbotron_pylon_mount")
    var rightJumbotronPylonMount: Boolean = false

    @Basic
    @Column(name = "jumbotron_size")
    var jumbotronSize: Double = 1.0

    @Basic
    @Column(name = "right_jumbotron_size")
    var rightJumbotronSize: Double = 1.0

    @Basic
    @Column(name = "wall_enabled")
    var wallEnabled: Boolean = false

    @Basic
    @Column(name = "right_wall_enabled")
    var rightWallEnabled: Boolean = false

    @Basic
    @Column(name = "jumbotron_elevation")
    var jumbotronElevation: Double = 0.0

    @Basic
    @Column(name = "right_jumbotron_elevation")
    var rightJumbotronElevation: Double = 0.0

    @Basic
    @Column(name = "skyline_backdrop")
    var skylineBackdrop: String? = null

    @Basic
    @Column(name = "right_skyline_backdrop")
    var rightSkylineBackdrop: String? = null

    @Basic
    @Column(name = "jumbotron_shape")
    var jumbotronShape: String? = null

    @Basic
    @Column(name = "right_jumbotron_shape")
    var rightJumbotronShape: String? = null

    @Basic
    @Column(name = "jumbotron_pylons_outside")
    var jumbotronPylonsOutside: Boolean = false

    @Basic
    @Column(name = "right_jumbotron_pylons_outside")
    var rightJumbotronPylonsOutside: Boolean = false

    @Basic
    @Column(name = "jumbotron_color")
    var jumbotronColor: String? = null

    @Basic
    @Column(name = "right_jumbotron_color")
    var rightJumbotronColor: String? = null

    @Basic
    @Column(name = "topper_color")
    var topperColor: String? = null

    @Basic
    @Column(name = "right_topper_color")
    var rightTopperColor: String? = null

    @Basic
    @Column(name = "facility_full_width")
    var facilityFullWidth: Boolean = false

    @Basic
    @Column(name = "right_facility_full_width")
    var rightFacilityFullWidth: Boolean = false

    @Basic
    @Column(name = "jumbotron_over_building")
    var jumbotronOverBuilding: Boolean = false

    @Basic
    @Column(name = "right_jumbotron_over_building")
    var rightJumbotronOverBuilding: Boolean = false

    @Basic
    @Column(name = "scoreboard_logo")
    var scoreboardLogo: String? = null

    @Basic
    @Column(name = "right_scoreboard_logo")
    var rightScoreboardLogo: String? = null

    @Basic
    @Column(name = "jumbotron_width")
    var jumbotronWidth: Double = 1.0

    @Basic
    @Column(name = "right_jumbotron_width")
    var rightJumbotronWidth: Double = 1.0

    @Basic
    @Column(name = "jumbotron_pylon_style")
    var jumbotronPylonStyle: String? = null

    @Basic
    @Column(name = "right_jumbotron_pylon_style")
    var rightJumbotronPylonStyle: String? = null

    @Basic
    @Column(name = "backdrop_style")
    var backdropStyle: String? = null

    @Basic
    @Column(name = "right_backdrop_style")
    var rightBackdropStyle: String? = null

    @Basic
    @Column(name = "jumbotron_style")
    var jumbotronStyle: String? = null

    @Basic
    @Column(name = "right_jumbotron_style")
    var rightJumbotronStyle: String? = null

    fun copyInto(target: StadiumDesign) {
        target.stadiumBackdrop = stadiumBackdrop
        target.stadiumSize = stadiumSize
        target.rightStadiumBackdrop = rightStadiumBackdrop
        target.rightStadiumSize = rightStadiumSize
        target.jumbotronEnabled = jumbotronEnabled
        target.rightJumbotronEnabled = rightJumbotronEnabled
        target.jumbotronPylonMount = jumbotronPylonMount
        target.rightJumbotronPylonMount = rightJumbotronPylonMount
        target.jumbotronSize = jumbotronSize
        target.rightJumbotronSize = rightJumbotronSize
        target.wallEnabled = wallEnabled
        target.rightWallEnabled = rightWallEnabled
        target.jumbotronElevation = jumbotronElevation
        target.rightJumbotronElevation = rightJumbotronElevation
        target.skylineBackdrop = skylineBackdrop
        target.rightSkylineBackdrop = rightSkylineBackdrop
        target.jumbotronShape = jumbotronShape
        target.rightJumbotronShape = rightJumbotronShape
        target.jumbotronPylonsOutside = jumbotronPylonsOutside
        target.rightJumbotronPylonsOutside = rightJumbotronPylonsOutside
        target.jumbotronColor = jumbotronColor
        target.rightJumbotronColor = rightJumbotronColor
        target.topperColor = topperColor
        target.rightTopperColor = rightTopperColor
        target.facilityFullWidth = facilityFullWidth
        target.rightFacilityFullWidth = rightFacilityFullWidth
        target.jumbotronOverBuilding = jumbotronOverBuilding
        target.rightJumbotronOverBuilding = rightJumbotronOverBuilding
        target.scoreboardLogo = scoreboardLogo
        target.rightScoreboardLogo = rightScoreboardLogo
        target.jumbotronWidth = jumbotronWidth
        target.rightJumbotronWidth = rightJumbotronWidth
        target.jumbotronPylonStyle = jumbotronPylonStyle
        target.rightJumbotronPylonStyle = rightJumbotronPylonStyle
        target.backdropStyle = backdropStyle
        target.rightBackdropStyle = rightBackdropStyle
        target.jumbotronStyle = jumbotronStyle
        target.rightJumbotronStyle = rightJumbotronStyle
    }
}
