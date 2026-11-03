# Test notes

Run every command from the repository root with JDK 17 or newer. Build first:

```
javac --release 17 -d out -sourcepath src src/restaurantrush/Main.java
```

The recorded runs in `runs/` belong to the stage they are checked out with. This folder holds the Stage 3 runs; the Stage 1 and Stage 2 runs are in the `stage-1` and `stage-2` tags (`git checkout stage-1`).

## Stage 3 (tag `stage-3`)

### Automated checks

```
javac --release 17 -d out -sourcepath src test/restaurantrush/GameTests.java
java -cp out restaurantrush.GameTests
```

Expected: `189 checks passed, 0 failed`. The checks cover:

- **Stage 1 and 2 rules that must still hold**: order status rules, the Regular's loss of 8, the turn guards, tables, payment, abandonment, loss per waiting turn for Regular, VIP and Critic (8, 5, 12), the VIP's 10% discount, the locked bill, the Critic's food and late-service penalty, the six arrivals (including a VIP and a Critic), and that `GameController`, `StaffActions` and `ManagerCommand` contain no type checks. The food-rule checks that assume three dishes build that three-dish menu explicitly, because the Stage 3 menu has five entries;
- **ComboMeal**: Salad + Burger $21.60 and 2 units, Salad + Pasta $24.30 and 3 units, Burger + Pasta $29.70 and 3 units; a combo holds its two component items and is a `MenuItem`; a 3-unit combo is READY only after 3 units; the menu has five entries (the three dishes and two combos built from the menu's own dishes); Regular and VIP choose by `customerId mod 5`, the Critic takes the Salad + Pasta combo;
- **Happy Hour**: lasts exactly two turns, can be switched on once per game (a second activation is refused and does not restart it), VIP Burger $10.80, Regular Pasta $14.40, Critic's combo $19.44, no discount when off; an order placed before Happy Hour keeps its price, and an order taken during it keeps the discount after it ends; waiting customers recover 15 (capped at 100), served customers and customers who have left do not;
- **Host**: seats a queued customer, refuses an occupied table or a customer who is not waiting without changing anything, and the Waiter can still seat;
- **Stage 3 clean-up** (no behaviour change): the waiting queue is read-only and keeps arrival order; `seatFromQueue` removes a customer from it only after the Waiter or Host accepted the seating, and a rejected seating leaves the customer in the queue; `Restaurant` reports who can be seated, cooked for or served (and nobody can be seated while both tables are occupied); `hasMetObjective()` is false until 4 customers have paid and true at 4 paid, $72.00 and satisfaction 100; `RestaurantCLI` does not use the controller or recompute the staff-task rules; `Order` has no static mutable state. The replays of the recorded runs and an invalid-input session print the same transcripts as before the clean-up;
- **The recorded runs below**: results, Happy Hour offered only until used (and offered again after being declined), the Host seating while the Waiter takes the order in the same turn, rejected choices explained and followed by another prompt in the same turn.

### Reference values from the brief

| Case | Price | Prep units | Checked by |
|---|---|---|---|
| Combo: Salad + Burger | $21.60 | 2 | `GameTests` |
| Combo: Salad + Pasta | $24.30 | 3 | `GameTests` |
| Combo: Burger + Pasta | $29.70 | 3 | `GameTests` |
| VIP, Burger | $13.50 | 1 | `GameTests` |
| VIP, Burger, during Happy Hour | $10.80 | 1 | `GameTests` and the victory run (turn 1) |
| Regular, Pasta, during Happy Hour | $14.40 | 2 | `GameTests` |

### Reproducible victory run

```
java -cp out restaurantrush.Main --input runs/victory.txt
```

Expected ending: `VICTORY`, 5 paid customers, revenue $83.70, average satisfaction 81.2, 6 customers served, 0 unhappy departures, 18 turns used.

| Turn | Happy Hour | Host | Waiter | Chef | What to look for |
|---|---|---|---|---|---|
| 1 | `y` | SEAT 5 (rejected), SEAT 1 | TAKE_ORDER 1 | COOK 1 | Happy Hour on (2 turns left). The Host seats the VIP and the Waiter takes the order in the same turn: bill locked at $10.80 ($15.00 x 0.9 x 0.8). Burger READY |
| 2 | | WAIT | SERVE 1 | WAIT | Last Happy Hour turn: the VIP recovers 95 to 100. READY to SERVED on a later turn |
| 3 | | WAIT | WAIT | WAIT | VIP finishes eating (READY_TO_PAY) |
| 4 | | SEAT 2 | TAKE_ORDER 2 | COOK 2 | Cashier collects $10.80 from the VIP (satisfaction 100). #2 (Regular) arrives, is seated by the Host and orders a Pasta in the same turn |
| 5 | | WAIT | WAIT | COOK 2 | Pasta (2 units) READY |
| 6 | | WAIT | SERVE 3 (rejected), SERVE 2 | WAIT | "There is no customer #3" |
| 7 | | SEAT 3 | TAKE_ORDER 3 | COOK 3 | #3 arrives and orders the Salad + Burger combo ($21.60, 2 units) |
| 8 | | WAIT | WAIT | COOK 3 | Cashier collects $18.00 from #2; combo READY after 2 units |
| 9 | | WAIT | SERVE 3 | WAIT | |
| 10 | | SEAT 4 | TAKE_ORDER 4 | COOK 4 | #4 (Critic) arrives and orders the Salad + Pasta combo ($24.30, 3 units) |
| 11 | | WAIT | WAIT | COOK 4 | Cashier collects $21.60 from #3 |
| 12 | | WAIT | WAIT | COOK 4 | combo READY after 3 units |
| 13 | | SEAT 5 | SERVE 4 | WAIT | Manager choice: the Waiter serves the Critic (3 turns after ordering: -10, satisfaction 54) before taking #5's order |
| 14 | | WAIT | TAKE_ORDER 5 | COOK 5 | #5 orders a Salad |
| 15 | | WAIT | SERVE 5 | WAIT | Cashier collects $24.30 from the Critic |
| 16 | | SEAT 6 | TAKE_ORDER 6 | COOK 6 | #6 arrives (it cannot finish paying in time) |
| 17 | | WAIT | SERVE 6 | WAIT | Cashier collects $9.00 from #5 (the fifth payment) |
| 18 | | WAIT | WAIT | WAIT | #6 becomes READY_TO_PAY too late to pay |

Choices that change the outcome (checked by replaying the run with a different choice):

- **Happy Hour.** Never switching it on gives $86.40 revenue and average satisfaction 80.2 instead of $83.70 and 81.2: Happy Hour costs $2.70 of revenue and buys one satisfaction point on average here, which matters when the average is close to 60.
- **Turn 13.** The run serves the Critic first (satisfaction 54 at payment). Taking #5's order first and serving the Critic in turn 14 costs the Critic a further 12, so it pays with 42 and the average falls from 81.2 to 78.8 (still a victory, with the same revenue).

### Reproducible defeat run

```
java -cp out restaurantrush.Main --input runs/defeat.txt
```

The manager switches Happy Hour on in turn 1, serves the VIP through to payment, and then only chooses WAIT. Expected ending: `DEFEAT`, 1 paid customer, revenue $10.80, average satisfaction 100.0, 1 served, 2 unhappy departures (customer 2, a Regular, leaves at the end of turn 16; customer 4, the Critic, at the end of turn 18), 18 turns used. A perfectly happy customer is not enough: all three targets must be met.

### Invalid input

`runs/victory.txt` contains a rejected Host choice (turn 1, `SEAT 5` before customer 5 exists) and a rejected Waiter choice (turn 6, `SERVE 3`). Each prints the reason and asks again in the same turn. `GameTests` checks the messages and that the matching prompt follows. A Happy Hour answer other than `y` or `n` is rejected and asked again (checked by an inline input in `GameTests`).

## Stage 2 (tag `stage-2`)

Check out the tag and use its `runs/` and tests.

- Automated checks: 108 pass.
- Victory run: `VICTORY`, 5 paid, $76.50, average 69.6, 5 served, 0 unhappy departures. The VIP pays $13.50, the Critic orders Pasta and, served 3 turns after ordering, pays with satisfaction 30.
- Defeat run: the VIP is served, then only WAIT. `DEFEAT`, 1 paid, $13.50, average 90.0, 2 unhappy departures.

## Stage 1 (tag `stage-1`)

Check out the tag and use its `runs/` and tests.

- Automated checks: 55 pass.
- Victory run: `VICTORY`, 4 paid, $57.00, average 78.0, 5 served, 0 unhappy departures. It includes four rejected choices (the Waiter in turns 2, 6 and 11, the Chef in turn 6).
- Defeat run: the manager only chooses WAIT. `DEFEAT`, 0 paid, $0.00, average N/A, 2 unhappy departures.
