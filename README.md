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

Each stage is a separate tagged commit that builds and runs on its own. `runs/` and the tests in each tag belong to that stage, so the recorded runs of an earlier stage are in its tag (`git checkout stage-1`).

| Tag | Adds |
|---|---|
| `stage-1` | Basic operation: Regular customers, menu, Waiter, Chef, Cashier, Table, Order, the waiting queue, the full six-step turn, abandonment, victory and defeat |
| `stage-2` | Customer behaviour: VIP and Critic customers next to the Regulars |
| `stage-3` | New role and menu policy: the Host, ComboMeal and Happy Hour |

## How to play

The base setting is fixed: 18 turns, 2 tables, one Waiter, one Chef, one Cashier, one Host (Stage 3), and one new customer at the start of turns 1, 4, 7, 10, 13 and 16.

| Menu entry | Price | Preparation units |
|---|---|---|
| Salad | $9.00 | 1 |
| Burger | $15.00 | 1 |
| Pasta | $18.00 | 2 |
| Salad + Burger (combo) | $21.60 | 2 |
| Salad + Pasta (combo) | $24.30 | 3 |

A combo is made of two existing menu items: its price is 90% of the two prices added together (rounded to the nearest cent) and its preparation units are the sum. Customers pick their own food; the manager never does.

| # | Customer | Arrives | Loses per waiting turn | Bill | Orders |
|---|---|---|---|---|---|
| 1 | VIP | turn 1 | 5 | 10% off | Burger |
| 2 | Regular | turn 4 | 8 | full price | Pasta |
| 3 | Regular | turn 7 | 8 | full price | Salad + Burger |
| 4 | Critic | turn 10 | 12 | full price | Salad + Pasta (the most expensive entry); loses a further 10 satisfaction if served more than 2 turns after the order was taken |
| 5 | Regular | turn 13 | 8 | full price | Salad |
| 6 | Regular | turn 16 | 8 | full price | Burger |

Every turn the screen shows the turn number, revenue, the average satisfaction of paid customers, Happy Hour (and its remaining duration), the waiting queue, the tables, every customer's type, satisfaction and order status, and the tasks each staff member can do. Then you decide:

| Prompt | Commands | What happens |
|---|---|---|
| Happy Hour (only while unused) | `y`, `n` (or Enter) | `y` switches Happy Hour on for this turn and the next; it can be used once per game |
| Host (one task per turn) | `SEAT <#>`, `WAIT` | seat a queued customer at a free table, in addition to the Waiter's task |
| Waiter (one task per turn) | `SEAT <#>`, `TAKE_ORDER <#>`, `SERVE <#>`, `WAIT` | seat a queued customer, take a seated customer's order, or serve a READY order |
| Chef (one unit per turn) | `COOK <#>`, `WAIT` | advance a customer's order by one preparation unit |

`<#>` is the customer number shown on screen (`#2` and `2` both work). It may be left out when only one customer qualifies. A blank line means `WAIT`. A choice that cannot be done is explained and you choose again; it does not use up the staff task.

After your choices the turn finishes on its own: customers who were served earlier finish eating (READY_TO_PAY), the Cashier charges at most one customer who became READY_TO_PAY on an earlier turn, and every customer who is still waiting for service loses satisfaction (the amount depends on the customer type). A customer who reaches 0 leaves without paying, the order is cancelled and the table is freed.

After turn 18 you win if at least 4 customers have paid, revenue is at least $45.00 and the average satisfaction of the customers who paid is at least 60. Otherwise you lose. The game never ends early.

### Happy Hour

Switched on at the start of a turn, it lasts for that turn and the next, and the screen shows the turns left. While it is on:

- orders taken get 20% off their bill. The VIP discount is applied first, then Happy Hour: a VIP's Burger is $15.00 x 0.9 x 0.8 = $10.80; a Regular's Pasta is $14.40;
- at the start of each active turn, customers still waiting for a table, to order, or for food recover 15 satisfaction (never above 100).

The bill is locked when the order is taken, so Happy Hour never changes an order already placed and never touches customers who have left.

### Rules this implementation fixes where the brief leaves a choice

- **Food choice.** Regular and VIP customers take menu entry number `customerId mod 5` (the menu has five entries: Salad 0, Burger 1, Pasta 2, Salad + Burger 3, Salad + Pasta 4), so customer 1 orders a Burger, 2 a Pasta, 3 the Salad + Burger combo, 4 the Salad + Pasta combo, 5 a Salad. The Critic ignores that rule and takes the most expensive entry. The choice is shown when the order is taken.
- **Price lock.** The bill is worked out when the Waiter takes the order (customer's own pricing first, then Happy Hour) and is stored in the Order; the Cashier collects that amount.
- **Critic reaction.** "Served more than 2 turns after the order was taken" means the serving turn minus the order turn is 3 or more. A 3-unit combo ordered on turn *n* is READY at the end of turn *n*+2 at the earliest and served on turn *n*+3, so the Critic's combo always costs the extra 10 satisfaction; with a 2-unit dish a prompt Waiter avoids it.
- **Order of events in a turn.** Arrival, Happy Hour choice and recovery, your Host choice, your Waiter choice, your Chef choice (so the Chef can work on an order taken this turn, and the Waiter can take the order of a customer the Host has just seated), eating and payment, then waiting and abandonment. A dish the Chef finishes this turn is READY only after the Waiter has acted, so it is served next turn at the earliest.
- **Eating and payment.** A customer served on turn *n* is READY_TO_PAY on turn *n*+1 and can pay on turn *n*+2. "Oldest first" means the customer who arrived first.

## From stage-1 to stage-2

Compare: https://github.com/RobotHanzo/SC2002GroupProject/compare/stage-1...stage-2

The Stage 2 commits are `efec627`, `69a34cb`, `bc1cd9f` and `10f4b5e` plus the diagram and documentation commits after them.

**Class change map:** 5 classes added, 7 existing classes modified, 5 classes unchanged, 0 classes deleted. The two enums are also unchanged, giving 7 unchanged Java source files when enums are included. Counts cover production code in `src/`, including `Main`, and exclude tests and documentation.

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

**What still works.** `GameTests` keeps the Stage 1 checks that still apply (menu, order status rules, the Regular's loss of 8, the turn guards for eating and payment, the Table, payment and abandonment), rewritten only where a constructor changed, and adds the Stage 2 checks. It also checks that `GameController` contains no `instanceof` and never names a customer type.

## From stage-2 to stage-3

Compare: https://github.com/RobotHanzo/SC2002GroupProject/compare/stage-2...stage-3

The Stage 3 feature commits are `d6fbec7`, `0231d21`, `9ed8d12`, `9225d1b`, `7d210d3` and `a6e64d3`. They are followed by the diagram and documentation commits and by one clean-up commit, `74c50eb`, with its diagram commit `ba84062` (see "Stage 3 clean-up" below).

**Class change map:** 5 classes added, 8 existing classes modified, 9 classes unchanged, 0 classes deleted. The two enums remain unchanged, giving 11 unchanged Java source files when enums are included. Three added classes implement the Stage 3 features; two are extracted by the separate clean-up refactor. These counts compare the complete submitted stage tags, including that refactor.

**New files**

| File | Responsibility |
|---|---|
| `entity/ComboMeal.java` | A menu item made of two existing menu items (composition); computes the 90% price and the summed preparation units |
| `entity/HappyHour.java` | The pricing policy: once per game, two turns, 20% off new orders, +15 satisfaction for waiting customers |
| `entity/Host.java` | The new staff role: seats one queued customer per turn on top of the Waiter's task |
| `control/ManagerCommand.java` | Clean-up: reads one typed manager choice (a verb and an optional customer number) and picks the customer it refers to |
| `control/StaffActions.java` | Clean-up: carries out a Host, Waiter or Chef command and returns the sentence to show; an impossible command throws before anything changes |

**Existing files changed, and why**

| File | Change | Reason |
|---|---|---|
| `entity/MenuItem.java` | No longer `final` (one word) | `ComboMeal` extends it, so Waiter, Chef, Cashier, Menu and Order treat a combo like any dish and need no change |
| `entity/Customer.java` | Added `recoverSatisfaction`; `isAwaitingService()` became public; clean-up: added `getDisplayName()` | Happy Hour recovery and the controller select customers who are still waiting. The existing satisfaction clamp (`adjustSatisfaction`) is reused. `getDisplayName()` gives every message one way to name a customer |
| `entity/Waiter.java` | `takeOrder` also takes the `HappyHour` and applies it after the customer's own price | Happy Hour is applied after the VIP discount and locked in the Order. Seating, serving and the Critic hook are untouched |
| `entity/Restaurant.java` | Owns a `Host` and a `HappyHour` (with getters); the menu now lists two combos. Clean-up: `getWaitingQueue()` is read-only and `seatFromQueue` is the only way out of the queue; `getCustomersWithStatus`, `getSeatableCustomers`, `getCustomersToServe` and `getCustomersToCook` say what each staff member can do now; the three victory targets and `hasMetObjective()` | Registration of the new classes and of the combos, once, at set-up. The clean-up gives the queue and the staff-task rules one owner: the controller used to remove customers from the live queue itself, and the screen and the controller each worked out what a staff member can do. Payment and abandonment logic is untouched |
| `entity/Order.java` | Clean-up: removed the unused `orderId` and its static counter | Nothing read the id, and a static counter is global state shared by every game. Status rules and the locked bill are untouched |
| `control/GameController.java` | New steps before the staff choices: Happy Hour activation and recovery, and the Host choice; `HappyHour.endTurn()` at the end of the turn; the waiting loop uses `isAwaitingService()`. Clean-up: command parsing and task execution moved to `ManagerCommand` and `StaffActions`; the result check moved to `Restaurant.hasMetObjective()` | The Stage 3 turn needs the new steps. The clean-up leaves the controller sequencing the six steps (324 to 150 lines). The Waiter and Chef logic, payment and result rule are unchanged |
| `boundary/RestaurantCLI.java` | Host and Happy Hour prompts, Happy Hour status line, Host task line. Clean-up: no longer references `GameController`; the turn number and targets are passed in; the task lines print what `Restaurant` reports | Display and input only. Boundary and control no longer depend on each other |
| `Main.java` | Clean-up: creates the CLI and the controller (`Main.play`) and handles the end of the input | So that `RestaurantCLI` does not need the controller |
| `runs/victory.txt`, `runs/defeat.txt` | Replaced | There are two more prompts (Happy Hour and Host), and the menu changes what customers order. The Stage 2 runs are in the `stage-2` tag |
| `test/restaurantrush/GameTests.java` | Extended | See below |

**Unchanged:** `Menu`, `Table`, `Chef`, `Cashier`, `ArrivalSchedule`, `RegularCustomer`, `VIPCustomer`, `CriticCustomer`, the enums, `Money`.

**Deleted:** nothing.

**Stage 3 clean-up (no behaviour change).** After the Stage 3 features worked, review found structure the features did not need but the design should not keep: a controller that also parsed commands and picked customers, the screen repeating the staff-task rules, a live queue handed out for the controller to edit, a static order counter, and a controller and screen that referenced each other. The clean-up commit `74c50eb` fixes these in the files listed above and touches no rule: the Waiter, Host, Chef, Cashier, `HappyHour`, `ComboMeal` and the customer types are unchanged. Evidence that behaviour is the same: `runs/victory.txt`, `runs/defeat.txt` and an invalid-input session print byte-identical transcripts before and after, and every earlier check still passes.

**What still works.** `GameTests` keeps every Stage 1 and Stage 2 check (the Stage 1 checks that used the three-dish menu now build that menu explicitly, because the game menu has five entries) and adds the combo reference values, Happy Hour timing, discount, price lock and recovery, the Host, and (clean-up) the queue protection, staff task lists and objective check. The recorded runs replay the whole game. See [docs/test-notes.md](docs/test-notes.md).

## Design evidence

| Item | Location |
|---|---|
| Stage 1, 2 and 3 class diagrams | `docs/diagrams/stage-1-class.pdf`, `stage-2-class.pdf`, `stage-3-class.pdf` |
| Stage 3 sequence diagram (a VIP orders a Burger with Happy Hour on, turns 1 to 5) | `docs/diagrams/stage-3-sequence.pdf` |
| Diagram sources | `docs/diagrams/src/*.puml` |

## Video timestamps

The finished presentation is 15:19, with the group's own recorded voices fitted to the existing scene timing and quiet background music ducked under speech. Use the [full narrated video](docs/video/restaurant-rush-human-vo.mp4) or [smaller narrated video](docs/video/restaurant-rush-human-vo-small.mp4). Optional script-assisted English SRT/VTT sidecars, presenter scripts, edit details and the preserved silent masters are in [docs/video](docs/video).

| # | Segment | Time |
|---|---|---|
| 1 | The game launching from the submitted project | 1:45 |
| 2 | Meaningful manager choices, including a decision where staff are constrained | 2:07 (turn 13, Waiter serves the Critic or takes an order), 2:42 (rejected choices) |
| 3 | A dish going READY, SERVED, then payment | 2:57 |
| 4 | A VIP or Critic behaviour | 3:21 |
| 5 | A Stage 3 feature | 3:45 (Host), 4:00 (combo meals), 4:19 (Happy Hour) |
| 6 | One victory and one defeat | 4:49 (victory), 5:15 (defeat) |
| 7 | The stage tag comparisons and the diagrams | 5:39 (class diagrams), 6:51 (stage-1 to stage-2), 7:53 (stage-2 to stage-3), 8:23 (checks), 10:19 (sequence diagram) |
| 8 | Individual contributions and key learning | 13:08 |

## AI history

Each member's AI chats are in their own folder: [Li MU-EN](docs/individual/Li%20MU-EN/chat-history), [Peramudhan](docs/individual/Peramudhan/chat-history), [Ryan](docs/individual/Ryan/chat-history), [Ye Fei](docs/individual/Ye%20Fei/chat-history).

## Repository layout

| Path | Contents |
|---|---|
| `src/restaurantrush/` | Java source. `entity/` holds the domain classes, `control/` the turn controller, the command parser and staff actions and the arrival schedule, `boundary/` the command-line interface; `Main` wires the two |
| `test/restaurantrush/` | `GameTests`, plain-Java checks |
| `runs/` | Recorded manager choices that replay a victory and a defeat |
| `docs/` | Assignment brief and briefing, test notes, UML diagrams (`docs/diagrams`, sources in `docs/diagrams/src`) |
| `docs/individual/<member>/` | Each member's `reflection/` and `chat-history/` folders |

The `individual` folders are directories in this repository, not Git branches.
