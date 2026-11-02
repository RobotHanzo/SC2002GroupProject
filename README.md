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

Each stage is a separate tagged commit that builds and runs on its own. `runs/` and the tests in each tag belong to that stage.

| Tag | Adds |
|---|---|
| `stage-1` | Basic operation: Regular customers, menu, Waiter, Chef, Cashier, Table, Order, the waiting queue, the full six-step turn, abandonment, victory and defeat |
| `stage-2` | Customer behaviour: VIP and Critic customers next to the Regulars |

## How to play

The base setting is fixed: 18 turns, 2 tables, one Waiter, one Chef, one Cashier, and one new customer at the start of turns 1, 4, 7, 10, 13 and 16.

| Item | Price | Preparation units |
|---|---|---|
| Salad | $9.00 | 1 |
| Burger | $15.00 | 1 |
| Pasta | $18.00 | 2 |

| Customer | Arrives | Loses per waiting turn | Bill | Special behaviour |
|---|---|---|---|---|
| Regular | turns 4, 7, 13, 16 (customers 2, 3, 5, 6) | 8 | full price | none |
| VIP | turn 1 (customer 1) | 5 | 10% off | none |
| Critic | turn 10 (customer 4) | 12 | full price | always orders the most expensive dish (Pasta); loses a further 10 satisfaction if served more than 2 turns after the order was taken |

Every turn the screen shows the turn number, revenue, the average satisfaction of paid customers, the waiting queue, the tables, every customer's type, satisfaction and order status, and the tasks the staff can do. Then you decide:

| Prompt | Commands | What happens |
|---|---|---|
| Waiter (one task per turn) | `SEAT <#>`, `TAKE_ORDER <#>`, `SERVE <#>`, `WAIT` | seat a queued customer at a free table, take a seated customer's order, or serve a READY order |
| Chef (one unit per turn) | `COOK <#>`, `WAIT` | advance a customer's order by one preparation unit |

`<#>` is the customer number shown on screen (`#2` and `2` both work). It may be left out when only one customer qualifies. A blank line means `WAIT`. A choice that cannot be done is explained and you choose again; it does not use up the staff task.

After your choices the turn finishes on its own: customers who were served earlier finish eating (READY_TO_PAY), the Cashier charges at most one customer who became READY_TO_PAY on an earlier turn, and every customer who is still waiting for service loses satisfaction (the amount depends on the customer type). A customer who reaches 0 leaves without paying, the order is cancelled and the table is freed.

After turn 18 you win if at least 4 customers have paid, revenue is at least $45.00 and the average satisfaction of the customers who paid is at least 60. Otherwise you lose. The game never ends early.

Rules this implementation fixes where the brief leaves a choice:

- **Food choice.** Regular and VIP customers take menu entry number `customerId mod 3` (Salad is 0, Burger 1, Pasta 2), so customer 1 orders a Burger, 2 a Pasta, 3 a Salad, 4 a Burger, and so on. The Critic ignores that rule and takes the most expensive dish. The manager never picks the food; the choice is shown when the order is taken.
- **Price lock.** The bill is worked out when the Waiter takes the order (a VIP gets 10% off, rounded to the cent) and is stored in the Order; the Cashier collects that amount.
- **Critic reaction.** "Served more than 2 turns after the order was taken" means the serving turn minus the order turn is 3 or more. A Pasta ordered on turn *n* is READY at the end of turn *n*+1 at the earliest and served on turn *n*+2, so a prompt Waiter never upsets the Critic.
- **Order of events in a turn.** Arrival, your Waiter choice, your Chef choice (so the Chef can work on an order taken this turn), eating and payment, then waiting and abandonment. A dish the Chef finishes this turn is READY only after the Waiter has acted, so it is served next turn at the earliest.
- **Eating and payment.** A customer served on turn *n* is READY_TO_PAY on turn *n*+1 and can pay on turn *n*+2. "Oldest first" means the customer who arrived first.

## From stage-1 to stage-2

Compare: https://github.com/RobotHanzo/SC2002GroupProject/compare/stage-1...stage-2

The Stage 2 commits are `efec627`, `69a34cb`, `bc1cd9f` and `10f4b5e` plus the diagram and documentation commits after them.

**New files**

| File | Responsibility |
|---|---|
| `entity/Menu.java` | Owns the dishes and finds the most expensive one (the Critic needs it) |
| `entity/RegularCustomer.java` | Baseline customer: loses 8, full price |
| `entity/VIPCustomer.java` | Loses 5, bill is 90% of the list price |
| `entity/CriticCustomer.java` | Loses 12, orders the most expensive dish, upset by slow service |
| `control/ArrivalSchedule.java` | Creates the six base-setting arrivals and decides their type (VIP on turn 1, Critic on turn 10) |

**Existing files changed, and why**

| File | Change | Reason |
|---|---|---|
| `entity/Customer.java` | Became abstract; added the hooks `getTypeLabel`, `getSatisfactionLoss`, `calculateBillCents`, `onServed` and `adjustSatisfaction`; `chooseMenuItem` takes a `Menu`; the fixed loss of 8 moved to `RegularCustomer` | Customer types must differ without the controller testing the type. All turn guards (`canFinishEating`, `canPay`) and the status rules are untouched |
| `entity/Order.java` | Constructor takes the bill and the turn it was taken; `getPriceCents()` became `getBillCents()` | The VIP discount means the price is no longer the item's list price, so the bill is locked in the order when it is taken. The status transitions are untouched |
| `entity/Waiter.java` | `takeOrder` takes a `Menu` and the turn, asks the customer for the dish and the bill, and returns the `Order`; `serveOrder` calls `customer.onServed` | This is where type-specific behaviour is dispatched: the Waiter calls the methods on `Customer` and never checks the type |
| `entity/Cashier.java` | Reads `getBillCents()` | Follows the Order change; one line |
| `entity/Restaurant.java` | Holds a `Menu` instead of a list; `registerArrival(Customer)` receives the customer instead of creating one | The schedule now decides which kind of customer arrives |
| `control/GameController.java` | Holds an `ArrivalSchedule` and asks it for arrivals; passes the `Menu` and turn to the Waiter; messages show the customer type | Registration of the new classes. The six-step turn, the guards and the result rule are unchanged |
| `boundary/RestaurantCLI.java` | Shows the type, and the locked bill of each order | Display only |
| `runs/victory.txt`, `runs/defeat.txt` | Replaced | The base-setting arrivals now include a VIP and a Critic, so the Stage 1 inputs no longer fit. The Stage 1 runs are still in the `stage-1` tag |
| `test/restaurantrush/GameTests.java` | Extended | See below |

**Unchanged:** `Chef`, `Table`, `MenuItem`, `CustomerStatus`, `OrderStatus`, `Money`, `Main`.

**Deleted:** nothing.

**What still works.** `GameTests` keeps the Stage 1 checks that still apply (menu, order status rules, the Regular's loss of 8, the turn guards for eating and payment, the Table, payment and abandonment), rewritten only where a constructor changed, and adds the Stage 2 checks. It also checks that `GameController` contains no `instanceof` and never names a customer type. See [docs/test-notes.md](docs/test-notes.md).

## Repository layout

| Path | Contents |
|---|---|
| `src/restaurantrush/` | Java source. `entity/` holds the domain classes, `control/` the turn controller and the arrival schedule, `boundary/` the command-line interface |
| `test/restaurantrush/` | `GameTests`, plain-Java checks |
| `runs/` | Recorded manager choices that replay a victory and a defeat |
| `docs/` | Assignment brief and briefing, test notes, UML diagrams (`docs/diagrams`, sources in `docs/diagrams/src`) |
| `docs/individual/<member>/` | Each member's `reflection/` and `chat-history/` folders |

The `individual` folders are directories in this repository, not Git branches.
