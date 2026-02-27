package com.cryptodash.service;

import com.cryptodash.dto.LeaderboardEntryDto;
import com.cryptodash.dto.PnlSummaryDto;
import com.cryptodash.dto.WalletPositionDto;
import com.cryptodash.dto.WalletSummaryDto;
import com.cryptodash.entity.Transaction;
import com.cryptodash.entity.User;
import com.cryptodash.entity.WalletPosition;
import com.cryptodash.repository.TransactionRepository;
import com.cryptodash.repository.UserRepository;
import com.cryptodash.repository.WalletPositionRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final String USDT = "USDT";
    private static final BigDecimal INITIAL_USDT = new BigDecimal("10000.00");

    private final WalletPositionRepository positionRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PriceService priceService;

    public WalletService(WalletPositionRepository positionRepository, UserRepository userRepository,
            TransactionRepository transactionRepository, PriceService priceService) {
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.priceService = priceService;
    }

    public WalletSummaryDto getWallet(@NonNull UUID userId) {
        List<WalletPositionDto> positions = positionRepository.findByUserIdOrderBySymbol(userId).stream()
                .map(p -> new WalletPositionDto(p.getSymbol(), p.getAmount()))
                .collect(Collectors.toList());
        return new WalletSummaryDto(positions);
    }

    /**
     * Calcule le classement mondial des traders fictifs.
     */
    public List<LeaderboardEntryDto> getLeaderboard() {
        List<User> users = userRepository.findAll();
        List<LeaderboardEntryDto> entries = new ArrayList<>();

        for (User user : users) {
            BigDecimal totalValue = computeCurrentTotalUsdt(user.getId());
            entries.add(new LeaderboardEntryDto(
                    user.getAccountName(),
                    user.getEmail(),
                    totalValue,
                    0));
        }

        // Tri par valeur décroissante
        entries.sort(Comparator.comparing(LeaderboardEntryDto::totalValueUsdt).reversed());

        // Attribution des rangs
        List<LeaderboardEntryDto> rankedEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDto e = entries.get(i);
            rankedEntries.add(new LeaderboardEntryDto(e.accountName(), e.email(), e.totalValueUsdt(), i + 1));
        }

        return rankedEntries;
    }

    /**
     * Calcule la valeur totale du portefeuille en USDT (positions × prix actuels).
     * USDT compte pour sa quantité ; les autres actifs utilisent le dernier prix
     * Binance.
     */
    public BigDecimal computeCurrentTotalUsdt(@NonNull UUID userId) {
        List<WalletPosition> positions = positionRepository.findByUserIdOrderBySymbol(userId);
        BigDecimal total = BigDecimal.ZERO;
        for (WalletPosition p : positions) {
            if (USDT.equals(p.getSymbol())) {
                total = total.add(p.getAmount());
            } else {
                BigDecimal price = priceService.getLastPriceUsdt(p.getSymbol() + USDT);
                if (price != null) {
                    total = total.add(p.getAmount().multiply(price));
                }
            }
        }
        return total;
    }

    /**
     * Calcule le P&L réalisé (ventes - coût des ventes) et non réalisé (valeur
     * actuelle - coût des positions).
     * Coût moyen par symbole en rejouant les transactions (BUY/RECEIVE ajoutent au
     * coût, SELL/SEND retirent au prorata).
     */
    public PnlSummaryDto computePnl(@NonNull UUID userId) {
        BigDecimal totalValue = computeCurrentTotalUsdt(userId);
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtAsc(userId);
        Map<String, BigDecimal> totalAmountBySymbol = new HashMap<>();
        Map<String, BigDecimal> totalCostBySymbol = new HashMap<>();
        BigDecimal realisedPnl = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            String symbol = tx.getSymbol();
            if (USDT.equals(symbol))
                continue;

            totalAmountBySymbol.putIfAbsent(symbol, BigDecimal.ZERO);
            totalCostBySymbol.putIfAbsent(symbol, BigDecimal.ZERO);

            BigDecimal amount = tx.getAmount();
            BigDecimal totalUsdt = tx.getTotalUsdt();

            switch (tx.getType()) {
                case BUY, RECEIVE -> {
                    totalAmountBySymbol.put(symbol, totalAmountBySymbol.get(symbol).add(amount));
                    totalCostBySymbol.put(symbol, totalCostBySymbol.get(symbol).add(totalUsdt));
                }
                case SELL, SEND -> {
                    BigDecimal prevAmount = totalAmountBySymbol.get(symbol);
                    BigDecimal prevCost = totalCostBySymbol.get(symbol);
                    if (prevAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        totalAmountBySymbol.put(symbol, prevAmount.subtract(amount));
                        totalCostBySymbol.put(symbol, prevCost);
                        realisedPnl = realisedPnl.add(totalUsdt);
                        break;
                    }
                    BigDecimal costOfDisposal = prevCost.multiply(amount).divide(prevAmount, 8, RoundingMode.HALF_UP);
                    realisedPnl = realisedPnl.add(totalUsdt.subtract(costOfDisposal));
                    totalAmountBySymbol.put(symbol, prevAmount.subtract(amount));
                    totalCostBySymbol.put(symbol, prevCost.subtract(costOfDisposal));
                }
                case DEPOSIT -> {
                } // Dépôt USDT : ignoré dans le calcul PnL
            }
        }

        List<WalletPosition> positions = positionRepository.findByUserIdOrderBySymbol(userId);
        BigDecimal costBasis = BigDecimal.ZERO;
        for (WalletPosition p : positions) {
            if (USDT.equals(p.getSymbol()))
                continue;
            BigDecimal amt = totalAmountBySymbol.getOrDefault(p.getSymbol(), BigDecimal.ZERO);
            BigDecimal cost = totalCostBySymbol.getOrDefault(p.getSymbol(), BigDecimal.ZERO);
            if (amt.compareTo(BigDecimal.ZERO) > 0 && p.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                costBasis = costBasis.add(cost.multiply(p.getAmount()).divide(amt, 8, RoundingMode.HALF_UP));
            }
        }
        BigDecimal unrealisedPnl = totalValue.subtract(costBasis);

        return new PnlSummaryDto(realisedPnl, unrealisedPnl, totalValue);
    }

    /**
     * Initialise le portefeuille avec un solde USDT fictif (appelé à la première
     * consultation si vide).
     */
    @Transactional
    public void ensureInitialBalance(@NonNull UUID userId) {
        if (positionRepository.findByUserIdAndSymbol(userId, USDT).isEmpty()) {
            User user = userRepository.getReferenceById(userId);
            WalletPosition usdt = new WalletPosition();
            usdt.setUser(user);
            usdt.setSymbol(USDT);
            usdt.setAmount(INITIAL_USDT);
            positionRepository.save(usdt);
        }
    }

    @Transactional
    public void buy(UUID userId, String symbol, BigDecimal amount, BigDecimal priceUsdt) {
        ensureInitialBalance(userId);
        String baseSymbol = symbol.replace(USDT, "");
        BigDecimal cost = amount.multiply(priceUsdt);

        WalletPosition usdtPos = positionRepository.findByUserIdAndSymbol(userId, USDT)
                .orElseThrow(() -> new IllegalStateException("Portefeuille non initialisé."));
        if (usdtPos.getAmount().compareTo(cost) < 0) {
            throw new IllegalArgumentException("Solde USDT insuffisant.");
        }
        usdtPos.setAmount(usdtPos.getAmount().subtract(cost));

        WalletPosition cryptoPos = positionRepository.findByUserIdAndSymbol(userId, baseSymbol).orElse(null);
        if (cryptoPos == null) {
            User user = userRepository.getReferenceById(userId);
            cryptoPos = new WalletPosition();
            cryptoPos.setUser(user);
            cryptoPos.setSymbol(baseSymbol);
            cryptoPos.setAmount(BigDecimal.ZERO);
        }
        cryptoPos.setAmount(cryptoPos.getAmount().add(amount));
        positionRepository.save(usdtPos);
        positionRepository.save(cryptoPos);

        Transaction tx = new Transaction();
        tx.setUser(userRepository.getReferenceById(userId));
        tx.setType(Transaction.Type.BUY);
        tx.setSymbol(baseSymbol);
        tx.setAmount(amount);
        tx.setPriceUsdt(priceUsdt);
        tx.setTotalUsdt(cost);
        transactionRepository.save(tx);
    }

    @Transactional
    public void sell(@NonNull UUID userId, String symbol, BigDecimal amount, BigDecimal priceUsdt) {
        ensureInitialBalance(userId);
        String baseSymbol = symbol.replace(USDT, "");
        BigDecimal proceeds = amount.multiply(priceUsdt);

        WalletPosition cryptoPos = positionRepository.findByUserIdAndSymbol(userId, baseSymbol)
                .orElseThrow(() -> new IllegalArgumentException("Vous ne détenez pas cet actif."));
        if (cryptoPos.getAmount().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Quantité insuffisante à vendre.");
        }
        cryptoPos.setAmount(cryptoPos.getAmount().subtract(amount));

        WalletPosition usdtPos = positionRepository.findByUserIdAndSymbol(userId, USDT).orElseThrow();
        usdtPos.setAmount(usdtPos.getAmount().add(proceeds));

        positionRepository.save(cryptoPos);
        positionRepository.save(usdtPos);

        Transaction tx = new Transaction();
        tx.setUser(userRepository.getReferenceById(userId));
        tx.setType(Transaction.Type.SELL);
        tx.setSymbol(baseSymbol);
        tx.setAmount(amount);
        tx.setPriceUsdt(priceUsdt);
        tx.setTotalUsdt(proceeds);
        transactionRepository.save(tx);
    }

    /**
     * Envoie de la crypto du portefeuille de l'utilisateur vers un autre compte
     * (identifié par email ou nom de compte).
     */
    @Transactional
    public void sendCrypto(@NonNull UUID senderUserId, String recipientIdentifier, String symbol, BigDecimal amount) {
        String baseSymbol = symbol.toUpperCase().replace(USDT, "");
        if (baseSymbol.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez pas envoyer USDT par virement interne. Utilisez un actif (BTC, ETH, etc.).");
        }
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        User recipient = resolveRecipient(recipientIdentifier.trim());
        if (recipient.getId().equals(senderUserId)) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous envoyer des fonds à vous-même.");
        }

        ensureInitialBalance(senderUserId);
        ensureInitialBalance(recipient.getId());

        WalletPosition senderPos = positionRepository.findByUserIdAndSymbol(senderUserId, baseSymbol)
                .orElseThrow(() -> new IllegalArgumentException("Vous ne détenez pas cet actif (" + baseSymbol + ")."));
        if (senderPos.getAmount().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Solde insuffisant. Vous avez " + senderPos.getAmount() + " " + baseSymbol + ".");
        }
        senderPos.setAmount(senderPos.getAmount().subtract(amount));
        positionRepository.save(senderPos);

        WalletPosition recipientPos = positionRepository.findByUserIdAndSymbol(recipient.getId(), baseSymbol)
                .orElse(null);
        if (recipientPos == null) {
            recipientPos = new WalletPosition();
            recipientPos.setUser(recipient);
            recipientPos.setSymbol(baseSymbol);
            recipientPos.setAmount(BigDecimal.ZERO);
        }
        recipientPos.setAmount(recipientPos.getAmount().add(amount));
        positionRepository.save(recipientPos);

        BigDecimal priceUsdt = Optional.ofNullable(priceService.getLastPriceUsdt(baseSymbol + USDT))
                .orElse(BigDecimal.ZERO);
        BigDecimal totalUsdt = priceUsdt.multiply(amount);

        String senderAccountName = sender.getAccountName() != null ? sender.getAccountName() : sender.getEmail();
        String recipientAccountName = recipient.getAccountName() != null ? recipient.getAccountName()
                : recipient.getEmail();

        Transaction sendTx = new Transaction();
        sendTx.setUser(sender);
        sendTx.setType(Transaction.Type.SEND);
        sendTx.setSymbol(baseSymbol);
        sendTx.setAmount(amount);
        sendTx.setPriceUsdt(priceUsdt);
        sendTx.setTotalUsdt(totalUsdt);
        sendTx.setCounterpartyUserId(recipient.getId());
        sendTx.setCounterpartyAccountName(recipientAccountName);
        transactionRepository.save(sendTx);

        Transaction receiveTx = new Transaction();
        receiveTx.setUser(recipient);
        receiveTx.setType(Transaction.Type.RECEIVE);
        receiveTx.setSymbol(baseSymbol);
        receiveTx.setAmount(amount);
        receiveTx.setPriceUsdt(priceUsdt);
        receiveTx.setTotalUsdt(totalUsdt);
        receiveTx.setCounterpartyUserId(senderUserId);
        receiveTx.setCounterpartyAccountName(senderAccountName);
        transactionRepository.save(receiveTx);
    }

    /**
     * Dépôt fictif de USDT sur le portefeuille de l'utilisateur.
     * Simule un paiement réel (carte bancaire, virement) sans API externe.
     */
    @Transactional
    public void deposit(@NonNull UUID userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du dépôt doit être positif.");
        }
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("Le montant maximum par dépôt est de 1 000 000 USDT.");
        }

        ensureInitialBalance(userId);
        User user = userRepository.getReferenceById(userId);

        WalletPosition usdtPos = positionRepository.findByUserIdAndSymbol(userId, USDT)
                .orElseThrow(() -> new IllegalStateException("Portefeuille non initialisé."));
        usdtPos.setAmount(usdtPos.getAmount().add(amount));
        positionRepository.save(usdtPos);

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setType(Transaction.Type.DEPOSIT);
        tx.setSymbol(USDT);
        tx.setAmount(amount);
        tx.setPriceUsdt(BigDecimal.ONE);
        tx.setTotalUsdt(amount);
        transactionRepository.save(tx);
    }

    private User resolveRecipient(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier.toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Aucun compte trouvé avec l'email « " + identifier + " »."));
        }
        return userRepository.findByAccountName(identifier.toLowerCase())
                .orElseThrow(
                        () -> new IllegalArgumentException("Aucun compte trouvé avec le nom « " + identifier + " »."));
    }
}
