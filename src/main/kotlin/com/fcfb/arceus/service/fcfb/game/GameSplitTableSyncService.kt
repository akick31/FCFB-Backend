package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.game.GameCoinToss
import com.fcfb.arceus.model.game.GamePresentation
import com.fcfb.arceus.model.game.GameRecordSnapshot
import com.fcfb.arceus.model.game.GameVegasOdds
import com.fcfb.arceus.model.game.PlatformTransport
import com.fcfb.arceus.repositories.game.GameCoinTossRepository
import com.fcfb.arceus.repositories.game.GamePresentationRepository
import com.fcfb.arceus.repositories.game.GameRecordSnapshotRepository
import com.fcfb.arceus.repositories.game.GameVegasOddsRepository
import com.fcfb.arceus.repositories.game.PlatformTransportRepository
import org.springframework.stereotype.Service

/**
 * Dual-writes [Game]'s split-out columns into their new tables alongside every save,
 * so the new tables stay populated while `game` still owns the columns.
 */
@Service
class GameSplitTableSyncService(
    private val platformTransportRepository: PlatformTransportRepository,
    private val gamePresentationRepository: GamePresentationRepository,
    private val gameRecordSnapshotRepository: GameRecordSnapshotRepository,
    private val gameCoinTossRepository: GameCoinTossRepository,
    private val gameVegasOddsRepository: GameVegasOddsRepository,
) {
    fun sync(game: Game) {
        platformTransportRepository.save(
            PlatformTransport(
                gameId = game.gameId,
                homePlatform = game.homePlatform,
                homePlatformId = game.homePlatformId,
                awayPlatform = game.awayPlatform,
                awayPlatformId = game.awayPlatformId,
                requestMessageId = game.requestMessageId,
                lastMessageTimestamp = game.lastMessageTimestamp,
            ),
        )
        gamePresentationRepository.save(
            GamePresentation(
                gameId = game.gameId,
                tvChannel = game.tvChannel,
                postseasonGameLogo = game.postseasonGameLogo,
                postseasonGameName = game.postseasonGameName,
                neutralSite = game.neutralSite,
                venue = game.venue,
            ),
        )
        gameRecordSnapshotRepository.save(
            GameRecordSnapshot(
                gameId = game.gameId,
                homeTeamRank = game.homeTeamRank,
                awayTeamRank = game.awayTeamRank,
                homeWins = game.homeWins,
                homeLosses = game.homeLosses,
                awayWins = game.awayWins,
                awayLosses = game.awayLosses,
            ),
        )
        gameCoinTossRepository.save(
            GameCoinToss(
                gameId = game.gameId,
                coinTossWinner = game.coinTossWinner,
                coinTossChoice = game.coinTossChoice,
                overtimeCoinTossWinner = game.overtimeCoinTossWinner,
                overtimeCoinTossChoice = game.overtimeCoinTossChoice,
            ),
        )
        gameVegasOddsRepository.save(
            GameVegasOdds(
                gameId = game.gameId,
                homeVegasSpread = game.homeVegasSpread,
                awayVegasSpread = game.awayVegasSpread,
            ),
        )
    }
}
