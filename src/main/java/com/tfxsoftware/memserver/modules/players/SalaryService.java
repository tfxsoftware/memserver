package com.tfxsoftware.memserver.modules.players;

import com.tfxsoftware.memserver.modules.bootcamps.BootcampService;
import com.tfxsoftware.memserver.modules.rosters.Roster;
import com.tfxsoftware.memserver.modules.users.User;
import com.tfxsoftware.memserver.modules.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryService {

    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final BootcampService bootcampService;

    /**
     * Runs every hour to check if salaries need to be paid.
     * In a production environment, this could run once a day, 
     * but for the game's pace, we check more frequently.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processSalaries() {
        LocalDateTime now = LocalDateTime.now();
        List<Player> playersToPay = playerRepository.findAllByNextSalaryPaymentDateBefore(now);

        if (playersToPay.isEmpty()) {
            return;
        }

        log.info("Processing salaries for {} players.", playersToPay.size());

        // Group players by owner for efficient balance deduction
        Map<User, List<Player>> playersByOwner = playersToPay.stream()
                .filter(p -> p.getOwner() != null)
                .collect(Collectors.groupingBy(Player::getOwner));

        for (Map.Entry<User, List<Player>> entry : playersByOwner.entrySet()) {
            User owner = entry.getKey();
            List<Player> ownerPlayers = entry.getValue();

            BigDecimal totalSalaryDue = ownerPlayers.stream()
                    .map(Player::getSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (owner.getBalance().compareTo(BigDecimal.ZERO) >= 0) {
                // If balance is positive or zero, we allow it to go negative
                owner.setBalance(owner.getBalance().subtract(totalSalaryDue));
                userRepository.save(owner);

                // Update players' next payment date
                for (Player player : ownerPlayers) {
                    player.setNextSalaryPaymentDate(player.getNextSalaryPaymentDate().plusDays(7));
                    playerRepository.save(player);
                }

                log.info("Paid {} in salaries for owner {}. New balance: {}", 
                        totalSalaryDue, owner.getUsername(), owner.getBalance());
            } else {
                // If balance is already negative, we release the players
                log.warn("Owner {} has negative balance ({}). Releasing {} players.",
                        owner.getUsername(), owner.getBalance(), ownerPlayers.size());
                
                Set<Roster> rostersToStop = new HashSet<>();
                for (Player player : ownerPlayers) {
                    if (player.getRoster() != null) {
                        rostersToStop.add(player.getRoster());
                    }
                    player.setOwner(null);
                    player.setRoster(null);
                    playerRepository.save(player);
                }

                for (Roster roster : rostersToStop) {
                    if (roster.getActivity() == Roster.RosterActivity.BOOTCAMP) {
                        log.info("Stopping bootcamp for roster {} due to released players.", roster.getId());
                        bootcampService.stopBootcampInternal(roster);
                    }
                }
            }
        }
    }
}
