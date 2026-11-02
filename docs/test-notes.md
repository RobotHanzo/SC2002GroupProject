# Test notes

Run every command from the repository root with JDK 17 or newer. Build first:

```
javac --release 17 -d out -sourcepath src src/restaurantrush/Main.java
```

## Stage 1 (tag `stage-1`)

### Automated checks

```
javac --release 17 -d out -sourcepath src test/restaurantrush/GameTests.java
java -cp out restaurantrush.GameTests
```

Expected: `55 checks passed, 0 failed`. The checks cover:

- the menu (prices and preparation units from the brief);
- legal and illegal order transitions (a 1-unit dish goes PLACED to READY, a 2-unit dish goes through PREPARING, serving a dish that is not READY is rejected, a PAID order cannot be cancelled);
- the Regular customer's satisfaction (8 lost per waiting turn while waiting, seated or ordered; nothing lost once served; 0 is the floor and triggers abandonment);
- the turn guards (eating needs a turn after serving, payment needs a turn after READY_TO_PAY);
- the food rule, tables, payment (revenue, PAID customer and order, table freed, no double payment) and abandonment (order cancelled, table freed, queue left);
- the two recorded runs below, including that exactly six customers arrive and that all 18 turns are played.

### Reproducible victory run

```
java -cp out restaurantrush.Main --input runs/victory.txt
```

Expected ending: `VICTORY`, 4 paid customers, revenue $57.00, average satisfaction 78.0, 5 customers served, 0 unhappy departures, 18 turns used.

| Turn | Waiter | Chef | What to look for |
|---|---|---|---|
| 1 | SEAT 1 | WAIT | #1 seated, satisfaction 100 to 92 |
| 2 | SERVE 1 (rejected), TAKE_ORDER 1 | COOK 1 | rejected choice is explained and the Waiter chooses again; Burger is READY at the end of the turn |
| 3 | SERVE 1 | WAIT | READY to SERVED on a later turn; #1 stops losing satisfaction (84) |
| 4 | SEAT 2 | WAIT | #2 arrives; #1 finishes eating and is READY_TO_PAY |
| 5 | TAKE_ORDER 2 | COOK 2 | #2 orders Pasta (2 units, 1/2 done); Cashier collects $15.00 from #1 |
| 6 | SERVE 2 (rejected), WAIT | COOK 1 (rejected), COOK 2 | the Pasta is only PREPARING, so it cannot be served; it becomes READY |
| 7 | SERVE 2 | WAIT | #3 arrives |
| 8 | SEAT 3 | WAIT | #2 is READY_TO_PAY |
| 9 | TAKE_ORDER 3 | COOK 3 | Cashier collects $18.00 from #2 |
| 10 | SERVE 3 | WAIT | #4 arrives |
| 11 | SEAT 9 (rejected), SEAT 4 | WAIT | "There is no customer #9"; #3 is READY_TO_PAY |
| 12 | TAKE_ORDER 4 | COOK 4 | Cashier collects $9.00 from #3 |
| 13 | SERVE 4 | WAIT | #5 arrives |
| 14 | SEAT 5 | WAIT | #4 is READY_TO_PAY |
| 15 | TAKE_ORDER 5 | COOK 5 | Cashier collects $15.00 from #4 (4th payment) |
| 16 | WAIT | COOK 5 | #6 arrives (the turn-16 customer cannot finish paying in time) |
| 17 | SERVE 5 | WAIT | |
| 18 | WAIT | WAIT | #5 becomes READY_TO_PAY too late to pay; unpaid orders add no revenue |

### Reproducible defeat run

```
java -cp out restaurantrush.Main --input runs/defeat.txt
```

The manager only ever chooses WAIT. Expected ending: `DEFEAT`, 0 paid customers, revenue $0.00, average satisfaction N/A, 0 served, 2 unhappy departures (customer 1 leaves at the end of turn 13 and customer 2 at the end of turn 16), 18 turns used.

### Invalid input

`runs/victory.txt` contains four rejected choices (the Waiter in turns 2, 6 and 11, the Chef in turn 6). Each prints the reason and asks again in the same turn. `GameTests` checks that these messages appear and that the run still ends with the expected result, which shows that a rejected choice neither uses up the staff task nor changes any order.
