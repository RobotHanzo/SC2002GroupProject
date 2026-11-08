# **Sequence Diagram Project Feedback** 

https://gemini.google.com/app/34a6049a7e78731d 

_User prompt: Analyse this and tell me if this is ok according to the project breifing and what areas I can improve on_ 

Response: Based on the project briefing and guidelines for **Stage 3** ( `Restaurant Rush` ), your sequence diagram ( `stage3_sequence_diagram_final.drawio.jpg` ) captures the complex lifecycle of the game well, but there are a few important areas to review and improve to ensure full compliance with the project brief and rubric. 

## 1. Alignment with the Project Briefing 

- **Included Stage 3 Features:** The brief requires your sequence diagram to show a complete scenario in which a manager choice changes an outcome, specifically incorporating a **Stage 2 or Stage 3 behavior** (such as VIP pricing or Happy Hour) alongside the standard arrival, ordering, preparation, serving, eating, payment, and result check. Make sure your diagram explicitly highlights how a Stage 3 feature (like a **Host** seating an extra customer, a **ComboMeal** being ordered, or **Happy Hour** activation affecting prices/satisfaction) is integrated into the workflow. 

- **Actors and Objects:** Ensure your lifelines clearly distinguish between the boundary/UI layer, controller/turn manager, staff ( `Waiter` , `Chef` , `Cashier` , `Host` ), and domain objects ( `Table` , `Order` , `Customer` , `Menu` ). The brief explicitly states that game rules belong in domain and control objects, not the UI. 

- **Control Flow & Guards:** Verify that your loops and guards accurately reflect the constraints in the brief— such as the Chef working on an order taken this turn, the 1-turn delay for serving a ready dish, and the cashier processing payments. 

## 2. Areas for Improvement 

### **Readability and Layout (Visual Clarity):** 

- The image uploaded has a very high aspect ratio and dense layout, making some text blocks and message arrows difficult to read when scaled down. For your final PDF report, consider splitting the scenario across two pages or adjusting the vertical/horizontal spacing in Draw.io so that message labels, activation boxes, and guard conditions are crystal clear. 

### **Explicit Representation of Stage 3 Mechanics:** 

- Check that the price calculation sequence clearly shows the order of discounts (e.g., applying the VIP discount first, then the Happy Hour 20% discount if active, as mandated in Section 4.3 of the brief). 

- If Happy Hour or the Host is featured in this diagram, ensure the state change (such as adding 15 satisfaction for waiting customers or updating the remaining duration) is visible in the message flow. 

### **Match to Code:** 

- As emphasized in the grading rubric, your diagram **must strictly match your Stage 3 source code** . Double-check that every method call shown in the sequence diagram (e.g., `takeOrder()` , `setReady()` , `calculateBill()` ) corresponds directly to actual method names in your Java classes. 

_User prompt: Tell me what else I can add or improve on_ 

Response: To ensure your **Stage 3 sequence diagram** , **source code** , and **report evidence** score maximum marks under the grading rubric, here is a targeted checklist of what you can add or improve across your project submission: 

## 1. Sequence Diagram Polish & Rubric Alignment 

- **Explicitly Feature Stage 3 Mechanics:** The rubric allots 20 marks for the sequence diagram, specifically evaluating the chosen scenario, food/payment flow, and object-oriented control notation. Ensure your diagram clearly illustrates either **Happy Hour** (showing the 20-percent discount applied _after_ any VIP discount, and the +15 satisfaction recovery for waiting customers) or the **Host** (showing concurrent seating alongside the Waiter). 

- **Verify Method Name Parity:** Cross-reference every message call in your diagram against your Java codebase to ensure 100% exact matching for method names (e.g., `calculateBill()` , `seatCustomer()` , `advancePreparation()` ). 

- **Improve Readability:** Given the complexity of the turn-based loop, make sure guard conditions (e.g., `[isReady == true]` , `[HappyHour active]` ) and alt/loop frames are cleanly delineated so graders can parse the control flow instantly without straining. 

## 2. Codebase & Stage Transition Evidence 

- **Clean Stage Separation:** Verify that your `stage-1` , `stage-2` , and `stage-3` git tags exist and that each stage is independently runnable. 

- **Document Refactoring & Design Patterns:** In your group report, include a clear change-evidence breakdown detailing what new classes were added (e.g., `Host` , `ComboMeal` , `HappyHourManager` ) and what existing classes were modified (e.g., updates to `Order` for combo calculation or `Customer` for satisfaction loss rates). 

- **Solid OOP Principles:** Ensure your design discussion table explicitly highlights how SRP, OCP, and polymorphism are implemented (e.g., dispatching customer behaviors through polymorphism rather than type-checking `instanceof` statements in the turn controller). 

## 3. Verification for Final Submission Package 

- **Video Timestamps:** Double-check your README file to ensure all required video segments (game launch, constrained staff choices, dish lifecycle, VIP/Critic behavior, Stage 3 feature, victory/defeat runs, and UML walkthrough) have accurate timestamps corresponding to your 15-minute recording. 

- **Signed Declarations:** Ensure both the Group Declaration of Original Work and individual GAI usage declarations are fully completed and included in the final zip archive ( `LABGroup_AssignmentGrp.zip` ). 

_User prompt: This is in drawio. So im going to transfer it to plantUML. Tell me what difference it will make_ 

Response: Transferring your sequence diagram from **Draw.io (visual/manual layout)** to **PlantUML (text-based generation)** will change how you maintain, version-control, and format your diagram for your NTU SC2002 software engineering project. 

## Key Differences You Will Notice 

### **Version Control & Git Diff Clarity** 

- _Draw.io:_ Files are stored as large XML blobs ( `.drawio` or `.jpg` ). When collaborating with group members on GitHub, changes cause messy merge conflicts that are nearly impossible to read in pull requests. 

_PlantUML:_ Files are pure text ( `.puml` ). Every message, actor, and loop is written in code, allowing Git to show precise, line-by-line diffs so you can see exactly who changed a message or lifeline. 

### **Automatic Layout vs. Manual Positioning** 

- _Draw.io:_ You manually drag boxes, stretch arrows, align lifelines, and fix spacing every time you add a new message or method call. 

- _PlantUML:_ The layout engine calculates actor positioning, activation boxes, and message spacing automatically based on your script. If you insert a new step in the middle of a turn, PlantUML shifts everything else down automatically. 

### **Consistency with Codebase Parity** 

- _Draw.io:_ Typos in method names or class names require manual text-box editing. 

- _PlantUML:_ Because your sequence diagram represents the execution flow from your Java code, you can keep naming conventions closely aligned with your actual method signatures ( `takeOrder()` , `calculateBill()` , etc.). 

### **Styling and Theming** 

- _Draw.io:_ You have granular control over custom colors, drop shadows, and artistic formatting. 

- _PlantUML:_ Styling is governed by skinparams and themes. While it looks clean, structured, and professional out-of-the-box, heavy custom artistic tweaking is more constrained. 

## Advantages for Your Project Briefing 

For a university software engineering assignment, PlantUML is often preferred because it ensures the diagram strictly matches your final code structure. It also makes it much easier to embed the raw text into your documentation or generate crisp vector graphics ( `.png` / `.svg` ) directly for your final PDF report. 

_User prompt: Check through this and ensure it is alligned with the project breif and what we can change_ 

Response: Here is the audit of your updated sequence diagram ( `Sequence_Diagram (Stage 3)_v2.svg` ) measured directly against the **SC2002 Group Project Briefing** and **Assignment Brief** requirements. 

## 1. What Is Aligned With the Project Briefing 

- **Correct Structure & Lifecycle Flow:** The diagram accurately captures the full turn-based execution loop required by Section 3.2 of the brief: start-of-turn display, customer arrival, manager choice phase, kitchen/serving step, eating/payment step, customer abandonment/satisfaction update, and result check. 

- **Stage 3 Extensions Included:** The interaction shows Stage 3 mechanics (such as Happy Hour pricing/satisfaction recovery and combo meal ordering logic) integrated into the core sequence as specified in Section 4.3 and Section 5. 

- **Layering & Object Separation:** The lifelines properly separate User Interface (boundary), Controller/Engine (control), and Domain Entities (Customer, Order, Table, Staff, Menu), adhering to the rule that game logic must sit in domain/control objects rather than the UI. 

## 2. Critical Alignment Issues & Required Changes 

A. Sequence Diagram Specific Constraints (Section 5 & 7) 

**Explicit Guard Conditions:** Ensure every `alt` and `opt` fragment clearly labels its boolean condition in square brackets (e.g., `[turn == happyHourStart]` , `[customer.isVIP() == true]` , `[order.isReady() == true]` ). Graders deduct marks if control logic looks like non-deterministic branching. 

- **Exact Method Name Matching with Java Code:** The grading rubric explicitly tests for direct code parity. Check every arrow label to ensure it matches your Java source code signatures exactly (e.g., `calculateBill()` vs `getBill()` , `seatCustomer()` vs `assignTable()` ). 

- **Visual Density and Label Text:** In SVG/Rendered format, text labels inside nested interaction fragments ( `alt` , `loop` , `opt` ) must remain fully legible when exported at 100% zoom in your final PDF report. 

### B. Concrete Mechanics Checks (Section 3.2 & Section 4.3) 

- **Serving Turn-Delay Rule:** Section 3.2 explicitly states: _"A dish can be served only if it was already READY when the Waiter acts, so a dish finished this turn is served next turn at the earliest."_ Ensure your diagram shows the Waiter check checking existing `READY` dishes before the Chef advances preparation units, or clearly reflects that a dish set to `READY` by the Chef in Turn _N_ cannot be served by the Waiter until Turn _N_ + 1 . 

- **Stacked Discount Order:** Section 4.3 requires that VIP discounts are applied **first** , followed by the Happy Hour discount (e.g., $15 × 0.9 × 0.8 = $10.80 ). Verify that your calculation calls or note annotations inside the diagram explicitly reflect this sequence. 

- **Host Seating Distinction:** Section 4.3 notes that the Host seats a customer in _addition_ to the Waiter's action. If your Stage 3 flow demonstrates the Host lifeline, ensure it shows the Host operating independently without consuming the Waiter's single task limit for that turn. 

## 3. Recommended Action Steps Before Submission 

1. **PlantUML Script Verification:** Run a quick text search across your `.puml` file against your `.java` class files to confirm 100% signature alignment on all method calls and parameter lists. 

2. **Export Resolution:** Render the diagram as a high-resolution PNG or SVG embedded into the final PDF document, ensuring all note text boxes are readable without zooming past 100%. 

3. **Cross-Reference Report Text:** In your report's design discussion section, add 2–3 sentences explicitly pointing the marker to where Stage 3 features (Happy Hour / Combo / Host) appear in this sequence diagram. 


