package com.thegarden.game;

import java.util.*;

public class GameSession {
    public String gameId;
    public Map<String, Player> players = new LinkedHashMap<>();
    public int currentRound = 0;
    public int totalRounds = 8;
    public String currentEvent = "nothing";
    public boolean eventRevealedToAll = false;
    public double waterCarryover = 0;
    public boolean biddingOpen = false;
    public boolean actionsOpen = false;
    public Set<String> auctionWinnerIds = new HashSet<>();
    public List<String> log = new ArrayList<>();
    public int lastRoundScale = 0;

    private static final String[] EVENT_TYPES = {
        "double_water","half_water","double_fert_odds","half_fert_odds","nothing"
    };
    private static final int TEMP_SCALE_CAP = 2;
    private final Random random = new Random();

    public GameSession(String gameId, int totalRounds) {
        this.gameId = gameId;
        this.totalRounds = totalRounds;
    }

    public Player addPlayer(String name) {
        String seed = random.nextBoolean() ? "hot" : "cold";
        String id = UUID.randomUUID().toString().substring(0, 8);
        Player p = new Player(id, name, seed);
        players.put(id, p);
        return p;
    }

    public boolean isLastRound() {
        return currentRound >= totalRounds;
    }

    public void startRound() {
        currentRound++;
        currentEvent = "nothing";
        eventRevealedToAll = false;
        auctionWinnerIds.clear();
        for (Player p : players.values()) {
            p.currentAction = null; p.waterChoice = null; p.actionSubmitted = false;
            p.currentBid = 0; p.bidSubmitted = false; p.bidEventChoice = null;
            p.actionCardsBought = 0; p.actionCardTarget = null; p.actionCardWaterChoice = null;
        }
        biddingOpen = true;
        actionsOpen = false;
        log.add("Round " + currentRound + " of " + totalRounds + " started. Bidding phase open.");
    }

    public boolean allBidsSubmitted() {
        for (Player p : players.values()) if (!p.bidSubmitted) return false;
        return true;
    }

    public void submitBid(String playerId, int amount, String eventChoice) {
        Player p = players.get(playerId);
        if (p == null) return;
        amount = Math.max(0, Math.min(amount, p.coins));
        p.currentBid = amount;
        p.bidEventChoice = eventChoice;
        p.bidSubmitted = true;
    }

    public void resolveBidding() {
        List<String> pool = new ArrayList<>(Arrays.asList(EVENT_TYPES));
        int maxContrib = -1;
        List<String> topContributors = new ArrayList<>();
        Map<String, String> contributedType = new HashMap<>();

        for (Player p : players.values()) {
            int copies = p.currentBid;
            if (copies > 0 && p.bidEventChoice != null) {
                p.coins -= copies;
                for (int i = 0; i < copies; i++) pool.add(p.bidEventChoice);
                contributedType.put(p.id, p.bidEventChoice);
            }
            if (copies > maxContrib) { maxContrib = copies; topContributors.clear(); topContributors.add(p.id); }
            else if (copies == maxContrib && maxContrib > 0) topContributors.add(p.id);
        }

        currentEvent = pool.get(random.nextInt(pool.size()));

        if (maxContrib > 0) {
            auctionWinnerIds.addAll(topContributors);
            log.add("Event Auction: " + topContributors.size() + " player(s) contributed the most (" + maxContrib + " copies) and can see the event.");
        } else {
            log.add("No one contributed to the Event Pool this round.");
        }

        for (Map.Entry<String,String> e : contributedType.entrySet()) {
            if (e.getValue().equals(currentEvent)) {
                players.get(e.getKey()).coins += 1;
            }
        }

        biddingOpen = false;
        actionsOpen = true;
    }

    public boolean allActionsSubmitted() {
        for (Player p : players.values()) if (!p.actionSubmitted) return false;
        return true;
    }

    public void submitAction(String playerId, String action, String waterChoice) {
        Player p = players.get(playerId);
        if (p == null) return;
        p.currentAction = action;
        p.waterChoice = waterChoice;
        p.actionSubmitted = true;
    }

    public void buyActionCard(String playerId, String target, String waterChoiceForCard) {
        Player p = players.get(playerId);
        if (p == null || p.coins < 1) return;
        p.coins -= 1;
        p.actionCardsBought += 1;
        p.actionCardTarget = target;
        if ("water".equals(target)) p.actionCardWaterChoice = waterChoiceForCard;
        log.add(p.name + " bought an Action Card targeting " + target + ".");
    }

    public void sacrifice(String playerId, String stat, int amount) {
        Player p = players.get(playerId);
        if (p == null || amount <= 0) return;
        if (stat.equals("temp")) {
            amount = Math.min(amount, p.temp);
            if (amount <= 0) return;
            p.temp -= amount;
            p.coins += amount;
            log.add(p.name + " sacrificed " + amount + " Temperature point(s) for " + amount + " coin(s).");
        } else if (stat.equals("water")) {
            amount = Math.min(amount, p.water);
            if (amount <= 0) return;
            p.water -= amount;
            p.coins += amount;
            log.add(p.name + " sacrificed " + amount + " Water point(s) for " + amount + " coin(s).");
        } else if (stat.equals("fert")) {
            double a = Math.min(amount, p.fertBonus);
            if (a <= 0) return;
            p.fertBonus -= a;
            p.coins += (int) a;
            log.add(p.name + " sacrificed " + (int) a + " Fertilizer Bonus point(s) for " + (int) a + " coin(s).");
        }
    }

    public void giftCoins(String fromId, String toId, int amount) {
        Player from = players.get(fromId);
        Player to = players.get(toId);
        if (from == null || to == null || amount <= 0 || from.coins < amount) return;
        from.coins -= amount;
        to.coins += amount;
        log.add(from.name + " gifted " + amount + " coin(s) to " + to.name + ".");
    }

    private List<Long> fibSequence(int n) {
        List<Long> fibs = new ArrayList<>();
        fibs.add(1L); fibs.add(1L);
        while (fibs.size() < n) {
            int s = fibs.size();
            fibs.add(fibs.get(s-1) + fibs.get(s-2));
        }
        return fibs.subList(0, Math.max(n,1));
    }

    public void resolveRound() {
        eventRevealedToAll = true;

        int roundScale = 0;
        List<Player> tempActors = new ArrayList<>();
        for (Player p : players.values()) {
            if ("temp".equals(p.currentAction)) {
                tempActors.add(p);
                roundScale += p.seed.equals("hot") ? 1 : -1;
            }
            if ("temp".equals(p.actionCardTarget)) {
                roundScale += (p.seed.equals("hot") ? 1 : -1);
            }
        }
        lastRoundScale = roundScale;
        int stakes = Math.min(Math.abs(roundScale), TEMP_SCALE_CAP);
        for (Player p : tempActors) {
            if (roundScale == 0 || stakes == 0) continue;
            boolean favored = p.seed.equals("hot") ? roundScale > 0 : roundScale < 0;
            if (favored) { p.temp += stakes; p.coins += 1; }
        }

        List<Player> waterActors = new ArrayList<>();
        for (Player p : players.values()) if ("water".equals(p.currentAction)) waterActors.add(p);
        double extraWaterWeight = 0;
        for (Player p : players.values()) {
            if ("water".equals(p.actionCardTarget)) extraWaterWeight += p.actionCardsBought;
        }

        if (!waterActors.isEmpty()) {
            double multiplier = 1.0;
            if (currentEvent.equals("double_water")) multiplier = 2.0;
            if (currentEvent.equals("half_water")) multiplier = 0.5;
            int poolSize = (int)(waterActors.size() * multiplier + extraWaterWeight) + (int)waterCarryover;

            List<Player> stealers = new ArrayList<>();
            List<Player> sharers = new ArrayList<>();
            for (Player p : waterActors) {
                if ("steal".equals(p.waterChoice)) stealers.add(p); else sharers.add(p);
            }
            boolean stealersWin = !stealers.isEmpty() && stealers.size() < sharers.size()
                    && poolSize > 0 && poolSize % stealers.size() == 0;
            if (stealersWin) {
                int each = poolSize / stealers.size();
                for (Player p : stealers) { p.water += each; p.coins += 1; }
                waterCarryover = 0;
            } else {
                if (!sharers.isEmpty() && poolSize > 0) {
                    int each = poolSize / sharers.size();
                    int rem = poolSize % sharers.size();
                    for (Player p : sharers) { p.water += each; p.coins += 1; }
                    waterCarryover = rem;
                } else {
                    waterCarryover = poolSize;
                }
            }
        }

        for (Player p : players.values()) {
            if ("fert".equals(p.currentAction)) {
                p.fertActions += 1;
                List<Long> fibs = fibSequence(p.fertActions);
                long fibSum = 0; for (long f : fibs) fibSum += f;
                double[] probs = {0.5,0.3,0.2};
                if (currentEvent.equals("double_fert_odds")) probs = new double[]{0.2,0.35,0.45};
                if (currentEvent.equals("half_fert_odds")) probs = new double[]{0.7,0.22,0.08};
                double roll = random.nextDouble();
                int drawVal = roll < probs[0] ? 1 : (roll < probs[0]+probs[1] ? 2 : 3);
                p.fertBonus += fibSum * (drawVal / 1.7);
            }
        }

        if (isLastRound()) {
            for (Player p : players.values()) {
                while (p.coins > 0) {
                    double gainTemp = (p.temp+1)*p.water - p.temp*p.water;
                    double gainWater = p.temp*(p.water+1) - p.temp*p.water;
                    double gainFert = 1.5;
                    if (gainTemp >= gainWater && gainTemp >= gainFert) p.temp += 1;
                    else if (gainWater >= gainTemp && gainWater >= gainFert) p.water += 1;
                    else p.fertBonus += 1;
                    p.coins -= 1;
                }
            }
            log.add("Final round resolved. Leftover coins converted to score automatically.");
        }

        actionsOpen = false;
        log.add("Round " + currentRound + " resolved. Event was: " + currentEvent + ". Temp Scale: " + roundScale + " (stakes paid: " + stakes + ")");
    }

    public List<Player> getLeaderboard() {
        List<Player> sorted = new ArrayList<>(players.values());
        sorted.sort((a,b) -> Double.compare(b.getScore(), a.getScore()));
        return sorted;
    }
}
