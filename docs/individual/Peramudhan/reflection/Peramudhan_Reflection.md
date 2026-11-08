# Individual Reflection: Murugesan Peramudhan

SC2002 Restaurant Rush · Role: Stage 3 sequence diagram

## 1. My Contribution

My main responsibility was the Stage 3 sequence diagram. I chose the use case of a VIP customer ordering a Burger while Happy Hour is active, because it combines a Stage 2 behaviour (the VIP discount) with a Stage 3 feature (Happy Hour) and goes through the full food and payment flow within five turns.

I wrote the first complete version of the diagram in PlantUML. It covered the customer's arrival, the Happy Hour choice and recovery, the Waiter's choices, the Chef preparing the order, eating, payment and the final result check. I also worked out the numbers for the scenario note, such as the locked bill of $15.00 x 0.9 x 0.8 = $10.80 and the different outcome if Happy Hour is not used ($13.50 and satisfaction 90), and checked them against the reference values in the brief.

Besides drafting the diagram, I exported it as SVG and PNG so the group could review it. After the Stage 3 clean-up changed parts of the code, Mu-En reworked the diagram into the final three-page version, and parts of my draft were carried over into it.

## 2. What I Learned

Before this project, I mainly saw sequence diagrams as a way to show the order in which things happen. While working on the Stage 3 diagram, I realised that each arrow is really a statement about which object is responsible for an action. Deciding who calls whom was more challenging than simply listing the steps.

One concept that became clearer to me was polymorphism. When I traced how the VIP's bill is calculated, I saw that the Waiter calls calculateBillCents() on the customer, and the VIPCustomer class applies its own 10% discount. The controller does not need to check what type of customer it is. Drawing this message going directly to the VIP object helped me understand polymorphism better than just reading the definition in lectures.

Polymorphism also made me understand inheritance and abstraction better. Customer is an abstract class, so the game can never create a plain "Customer"; it has to be a RegularCustomer, VIPCustomer or CriticCustomer. The shared lifecycle (WAITING, SEATED, ORDERED and so on) is written once in Customer, while each subclass overrides only what is different, such as getSatisfactionLoss() returning 8, 5 or 12. In lectures I thought of inheritance mainly as a way to reuse code. In this project I saw that its bigger purpose is to give every customer the same contract, so other classes can treat them all the same way.

I also learned that encapsulation is about protecting rules, not just making fields private. In Order, the status can only change through methods like advancePreparation(), markServed() and markPaid(), and an illegal change, such as paying for an order that was never served, throws an exception. The bill is set once in the constructor and has no setter, which is why the price cannot change after the order is taken. When I drew the sequence diagram, this meant every state change had to be a message to the object that owns that state, rather than another class changing it directly.

Another concept I understood better was the difference between inheritance and composition. ComboMeal uses both: it extends MenuItem, so a combo can be used anywhere a normal dish can, and it also holds two existing MenuItems, which is where its price and preparation units come from. Restaurant, on the other hand, uses composition for its tables and staff, which it creates and owns, and aggregation for customers, who arrive and leave. Before this project I found it hard to tell these relationships apart, but seeing them in our own classes made the difference clearer.

Finally, the project showed me why the Open/Closed Principle matters. When the VIP and Critic customers were added in Stage 2, the controller's turn logic did not need to change, and when ComboMeal was added in Stage 3, the Chef, Cashier and Order classes did not need any change to handle combos. Our tests even check that the controller never uses instanceof or mentions a customer type. This helped me see that good use of abstraction and polymorphism is what lets a program grow without rewriting parts that already work.

I also gained a better understanding of UML control notation. A loop, opt or alt fragment is not just decoration; each one should match a loop or condition that actually exists in the code. If a guard is in the code but missing from the diagram, the diagram is not accurate.

## 3. How I Applied What I Learned

When drawing the diagram, I tried to show the price locking rule clearly. The brief requires the price to be locked when the order is taken. In my diagram, the Waiter creates the Order with the bill already calculated, and on turn 5 the Cashier only reads the stored amount instead of calculating the price again. This showed why the Cashier does not need to know about customer types or Happy Hour, which is an example of the Single Responsibility Principle.

I also applied the game's turn rules to place each message on the correct turn. The Chef finishes the Burger on turn 2, but since the Waiter has already acted that turn, the order can only be served on turn 3. The customer then finishes eating on turn 4 and pays on turn 5. I went through the turn order in the README step by step to make sure the timing in the diagram was correct.

## 4. Challenges and Solutions

One challenge was keeping the diagram consistent with the code while the code was still changing. I drew my version before the Stage 3 clean-up, which moved command handling into ManagerCommand and StaffActions and changed how customers are removed from the waiting queue. After that, my diagram no longer matched the code, even though it was correct when I first drew it. I realised that a diagram that disagrees with the code is not useful, so the final version had to be redrawn based on the updated classes. From this, I learned that the final sequence diagram should be completed only after the code for that stage is stable.

Another challenge was readability. My first version showed one general turn inside a large loop, with an alt fragment for each of the Waiter's choices. Although it was compact, it was difficult to follow which branch happened on which turn, and the diagram became very large. The final version solved this by showing each turn separately across three pages, which made the flow much easier to read.

I also had some difficulty with the PlantUML tool. The online renderer ignored some of the formatting settings I used and printed a warning box onto the exported image. I replaced those settings with a style block, rendered the diagram again and checked the exported image before sharing it with my group.

## 5. Key Takeaway

Overall, this project helped me understand how object-oriented concepts such as polymorphism, encapsulation and the Single Responsibility Principle appear in the interactions between objects, not just in the class structure. I learned that a sequence diagram is only useful if it accurately reflects the code and the rules of the system. I also realised that it is important to coordinate with teammates when the code changes, so that the diagrams and implementation stay consistent.
