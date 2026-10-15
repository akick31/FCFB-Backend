package com.fcfb.arceus.model.appearance

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_field")
class GameField : FieldAppearance() {
    @Id
    @Column(name = "game_id")
    var gameId: Int = 0
}
