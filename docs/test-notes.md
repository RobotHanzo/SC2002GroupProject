# Test notes

Run every command from the repository root with JDK 17 or newer. Build first:

```
javac --release 17 -d out -sourcepath src src/restaurantrush/Main.java
```

The recorded runs in `runs/` belong to the stage they are checked out with. The Stage 1 runs are in the `stage-1` tag (`git checkout stage-1`); this folder holds the Stage 2 runs.

## Stage 2 (tag `stage-2`)

### Automated checks

```
javac --release 17 -d out -sourcepath src test/restaurantrush/GameTests.java
java -cp out restaurantrush.GameTests
```

Expected: `108 checks passed, 0 failed`. The checks cover:

- **Stage 1 rules that must still hold**: the menu, legal and illegal order transitions, the Regular's loss of 8 per waiting turn (and nothing once served), the turn guards for eating and payment, the food rule for Regulars, tables, payment (revenue, PAID customer and order, table freed, no double payment) and abandonment (order cancelled, table freed, queue left);
- **VIP and Critic**: loss per waiting turn is 8, 5 and 12 through the common `Customer` type, a Critic reaches 0 on its 9th waiting turn, the VIP pays 90% (Burger $13.50, Salad $8.10, Pasta $16.20) while Regular and Critic pay full price, the Waiter locks the bill in the order and the Cashier collects it, the Critic always orders Pasta, the Critic loses a further 10 when served 3 turns after ordering but not after 2, VIP and Regular are unaffected by slow service;
- **Arrivals**: six arrivals, on turns 1, 4, 7, 10, 13 and 16, numbered in arrival order, including a VIP and a Critic;
- **Design**: `GameController` contains no `instanceof` and never names a customer type;
- **The two recorded runs below**, including that exactly six customers arrive and all 18 turns are played.

### Reproducible victory run

```
java -cp out restaurantrush.Main --input runs/victory.txt
```

Expected ending: `VICTORY`, 5 paid customers, revenue $76.50, average satisfaction 69.6, 5 customers served, 0 unhappy departures, 18 turns used.

| Turn | Waiter | Chef | What to look for |
|---|---|---|---|
| 1 | SEAT 1 | WAIT | VIP #1 seated, satisfaction 100 to 95 |
| 2 | TAKE_ORDER 1 | COOK 1 | the VIP orders a Burger: list price $15.00, bill locked at $13.50; the Burger is READY at the end of the turn |
| 3 | SERVE 1 | WAIT | READY to SERVED on a later turn |
| 4 | SEAT 2 | WAIT | #2 (Regular) arrives; #1 finishes eating and is READY_TO_PAY |
| 5 | TAKE_ORDER 2 | COOK 2 | Cashier collects $13.50 from the VIP (satisfaction 90) |
| 6 | WAIT | COOK 2 | the Pasta (2 units) is READY |
| 7 | SERVE 2 | WAIT | #3 arrives |
| 8 | SEAT 3 | WAIT | |
| 9 | TAKE_ORDER 3 | COOK 3 | Cashier collects $18.00 from #2 |
| 10 | SERVE 3 | WAIT | #4 (Critic) arrives |
| 11 | SEAT 4 | WAIT | |
| 12 | TAKE_ORDER 4 | COOK 4 | the Critic orders Pasta although the id rule would give a Burger; Cashier collects $9.00 from #3 |
| 13 | SEAT 5 | COOK 4 | manager choice: seat the Regular (#5 arrives) while the Critic's Pasta is already READY |
| 14 | TAKE_ORDER 5 | COOK 5 | the Critic keeps losing 12 per turn (40) |
| 15 | SERVE 4 | COOK 5 | the Critic is served 3 turns after ordering: -10, satisfaction 30 |
| 16 | SERVE 5 | WAIT | #6 arrives |
| 17 | SEAT 6 (rejected), WAIT | WAIT | "Both tables are occupied"; Cashier collects $18.00 from the Critic (satisfaction 30) |
| 18 | WAIT | WAIT | Cashier collects $18.00 from #5 (satisfaction 76): the fifth payment |

The choice in turns 13 to 15 changes the outcome: serving the Critic first (turn 14) would keep their satisfaction at 52 but leave the Regular too late to pay by turn 18, giving 4 paid customers, $58.50 and average 73.5 instead of 5, $76.50 and 69.6. It is still a victory here, but the revenue and the paid count change.

### Reproducible defeat run

```
java -cp out restaurantrush.Main --input runs/defeat.txt
```

The manager serves the VIP through to payment and then only chooses WAIT. Expected ending: `DEFEAT`, 1 paid customer, revenue $13.50, average satisfaction 90.0, 1 served, 2 unhappy departures (customer 2, a Regular, leaves at the end of turn 16; customer 4, the Critic, at the end of turn 18), 18 turns used. A happy VIP is not enough: all three targets must be met.

### Invalid input

`runs/victory.txt` contains a rejected Waiter choice in turn 17 (`SEAT 6` with both tables occupied). The reason is printed and the Waiter chooses again in the same turn. `GameTests` checks that the message appears and that a prompt follows in the same turn. More rejections (a dish that is not READY, an unknown customer, a customer with no order) are covered by the Stage 1 run in the `stage-1` tag.

## Stage 1 (tag `stage-1`)

Check out the tag (`git checkout stage-1`) and use its `runs/` and tests.

- Automated checks: 55 pass.
- Victory run: `VICTORY`, 4 paid, $57.00, average 78.0, 5 served, 0 unhappy departures. It includes four rejected choices (the Waiter in turns 2, 6 and 11, the Chef in turn 6).
- Defeat run: the manager only chooses WAIT. `DEFEAT`, 0 paid, $0.00, average N/A, 2 unhappy departures.
