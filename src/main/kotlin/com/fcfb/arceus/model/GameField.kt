package com.fcfb.arceus.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/** One game's frozen field appearance: a per-game override and, once the game is final, a time capsule. */
@Entity
@Table(name = "game_field")
class GameField : FieldAppearance() {
    @Id
    @Column(name = "game_id")
    var gameId: Int = 0
}
