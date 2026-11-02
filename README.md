# SC2002 Restaurant Rush

A turn-based restaurant management game for the command line, written in Java for the SC2002 group project.
The player is the manager: staff time is limited, so the manager decides which customer or order comes first.

Repository: https://github.com/RobotHanzo/SC2002GroupProject

## Build and run

Needs JDK 17 or newer. No other dependencies, no network. Run every command from the repository root.

```
javac --release 17 -d out -sourcepath src src/restaurantrush/Main.java
java -cp out restaurantrush.Main
```

Replay a recorded run instead of typing the choices (the file is read and echoed, so the screen shows a transcript):

```
java -cp out restaurantrush.Main --input runs/victory.txt
java -cp out restaurantrush.Main --input runs/defeat.txt
```

Run the automated checks (see [docs/test-notes.md](docs/test-notes.md) for what they cover and for the expected results of both runs):

```
javac --release 17 -d out -sourcepath src test/restaurantrush/GameTests.java
java -cp out restaurantrush.GameTests
```

## Stage tags

Each stage is a separate tagged commit that builds and runs on its own.

| Tag | Adds |
|---|---|
| `stage-1` | Basic operation: Regular customers, menu, Waiter, Chef, Cashier, Table, Order, the waiting queue, the full six-step turn, abandonment, victory and defeat |

## How to play (Stage 1)

The base setting is fixed: 18 turns, 2 tables, one Waiter, one Chef, one Cashier, and one new customer at the start of turns 1, 4, 7, 10, 13 and 16.

| Item | Price | Preparation units |
|---|---|---|
| Salad | $9.00 | 1 |
| Burger | $15.00 | 1 |
| Pasta | $18.00 | 2 |

Every turn the screen shows the turn number, revenue, the average satisfaction of paid customers, the waiting queue, the tables, every customer's satisfaction and order status, and the tasks the staff can do. Then you decide:

| Prompt | Commands | What happens |
|---|---|---|
| Waiter (one task per turn) | `SEAT <#>`, `TAKE_ORDER <#>`, `SERVE <#>`, `WAIT` | seat a queued customer at a free table, take a seated customer's order, or serve a READY order |
| Chef (one unit per turn) | `COOK <#>`, `WAIT` | advance a customer's order by one preparation unit |

`<#>` is the customer number shown on screen (`#2` and `2` both work). It may be left out when only one customer qualifies. A blank line means `WAIT`. A choice that cannot be done is explained and you choose again; it does not use up the staff task.

After your choices the turn finishes on its own: customers who were served earlier finish eating (READY_TO_PAY), the Cashier charges at most one customer who became READY_TO_PAY on an earlier turn, and every customer who is still waiting for service loses 8 satisfaction. A customer who reaches 0 leaves without paying, the order is cancelled and the table is freed.

After turn 18 you win if at least 4 customers have paid, revenue is at least $45.00 and the average satisfaction of the customers who paid is at least 60. Otherwise you lose. The game never ends early.

Rules this implementation fixes where the brief leaves a choice:

- **Food choice.** A customer takes menu entry number `customerId mod 3` (Salad is 0, Burger 1, Pasta 2), so customer 1 orders a Burger, 2 a Pasta, 3 a Salad, 4 a Burger, and so on. The manager never picks the food; the choice is shown when the order is taken.
- **Order of events in a turn.** Arrival, your Waiter choice, your Chef choice (so the Chef can work on an order taken this turn), eating and payment, then waiting and abandonment. A dish the Chef finishes this turn is READY only after the Waiter has acted, so it is served next turn at the earliest.
- **Eating and payment.** A customer served on turn *n* is READY_TO_PAY on turn *n*+1 and can pay on turn *n*+2. "Oldest first" means the customer who arrived first.

## Repository layout

| Path | Contents |
|---|---|
| `src/restaurantrush/` | Java source. `entity/` holds the domain classes, `control/` the turn controller, `boundary/` the command-line interface |
| `test/restaurantrush/` | `GameTests`, plain-Java checks |
| `runs/` | Recorded manager choices that replay a victory and a defeat |
| `docs/` | Assignment brief and briefing, test notes, UML diagrams (`docs/diagrams`, sources in `docs/diagrams/src`) |
| `docs/individual/<member>/` | Each member's `reflection/` and `chat-history/` folders |

The `individual` folders are directories in this repository, not Git branches.
