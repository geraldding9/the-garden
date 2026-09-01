# The Garden

A social strategy game for 9-12 players, played live with a host dashboard and per-player phones.

## Ruleset (v2.1.1)
- No death mechanic (Temp/Water only gain, never penalized for losing)
- **Temperature "Scale" mechanic**: net Hot/Cold pushes each round determine a Scale value;
  winners gain Temperature points equal to the Scale's magnitude, capped at 2 (not a flat bonus)
- Water Split-or-Steal with default multiplier 1 (Double=2, Half=0.5)
- Fibonacci-scaled Fertilizer bonus
- Event Pool bidding (contribute coins to stack event types; top contributor(s) see the event early)
- Action Cards (1 coin each, extend influence into Temp/Water without personal score benefit)
- **Sacrifice**: convert 1 point of Temperature, Water, or Fertilizer Bonus into 1 coin, at any time —
  logged in the game log for full visibility
- Coin gifting between players (also logged)
- Banking allowed across rounds; end-game conversion for final-round resolution income only

## Run Locally
Requires Java 17+ and Maven.

```
mvn spring-boot:run
```

- Host: `http://localhost:8080/host.html` → set round count → Create New Game → get Game ID
- Players: `http://localhost:8080/player.html` → enter Game ID + name → Join

## Deploy

1. On [Render.com](https://render.com): New → Web Service → connect this GitHub repo.
   Render auto-detects `render.yaml` and `Dockerfile` and builds/deploys automatically.
2. Alternatively: `docker build -t the-garden . && docker run -p 8080:8080 the-garden`

## Known Simplification
- Action Cards currently track one target per player per round (temp OR water), not multiple
  simultaneous targets.
- Sacrifice does not enforce a "never go to 0" floor in code — left as a house rule.
