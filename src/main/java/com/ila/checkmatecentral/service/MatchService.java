package com.ila.checkmatecentral.service;


import com.ila.checkmatecentral.entity.Match;
import com.ila.checkmatecentral.entity.MatchStatus;
import com.ila.checkmatecentral.entity.Player;
import com.ila.checkmatecentral.exceptions.MatchNotFoundException;
import com.ila.checkmatecentral.exceptions.TournamentNotFoundException;
import com.ila.checkmatecentral.repository.MatchRepository;
import com.ila.checkmatecentral.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MatchService {
    public final MatchRepository matchRepository;
    public final TournamentRepository tournamentRepository;
    public final GlickoService glickoService;

    public void createMatches(List<Player> players, int round, int tournamentId) {
        // Sort players by ELO in descending order
        Player[] sortedPlayers = players.toArray(Player[]::new);
        Arrays.sort(sortedPlayers, (p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));
        
        int n = sortedPlayers.length;

        // Pair highest with lowest, second highest with second lowest, etc.
        for (int i = 0; i < n / 2; i++) {
            Player player1 = sortedPlayers[i];
            Player player2 = sortedPlayers[n - 1 - i];

            Match match = new Match(player1, player2, LocalDateTime.now(), round, tournamentId);

            matchRepository.save(match);
        }
    }

    public void createSingleMatch(Player player1, Player player2, int round, int tournamentId) {
        Match match = new Match(player1, player2, LocalDateTime.now(), round, tournamentId);
        matchRepository.save(match);
    }

    public Match getMatch(Integer matchId) {
        return matchRepository.findByMatchIdWithoutPassword(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    }

    public List<Match> getMatches(int tournamentId) {
        return matchRepository.findByTournamentId(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
    }

    public Match updateMatchOutcome(Integer matchId, Match.MatchOutcome outcome) {
        //check if outcome is of valid input value

        Match currentMatch = matchRepository.findByMatchId(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        currentMatch.setOutcome(outcome);
        glickoService.updateRatings(currentMatch);
        currentMatch.setMatchStatus(MatchStatus.COMPLETED);
        return matchRepository.save(currentMatch);
    }
}
