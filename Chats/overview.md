# Cisco AI-Assisted Interview Prep Guide
**Target Problem:** SI26_P12: College Event Certificate Eligibility Board

## 1. Interview Overview & Strategy
This is a single-round, 30-40 minute interview evaluating engineering judgment and AI-assisted workflow[cite: 3, 4].
*   **Focus:** Functionality over perfection, clear AI documentation, and simple architecture[cite: 3].
*   **Deliverables Required:** A working solution, AI prompt history, design summary, test evidence, and a live-ready development environment[cite: 3]. 
*   **The Live Run:** The interview will include a live modification requirement, necessitating a highly modular and easily adjustable codebase[cite: 1, 3].

## 2. Problem Selection & Rationale
**Selected Problem:** College Event Certificate Eligibility Board[cite: 1, 4].
*   **Why this problem:** It emphasizes robust backend data design (aggregating participant points and categorizing activities) rather than complex string-matching[cite: 1]. It maps beautifully to object-oriented or structured programming paradigms.
*   **Project Name Ideas:** *Event Certification Validator* or *CertiTrack Engine*.

## 3. Technology Stack & Constraints Interpretation
The prompt states: *"You may use in-memory data structures... or a CLI that produces a clear visual or tabular report."*[cite: 1]
*   **Database:** No external databases (MySQL, MongoDB) are allowed; data must live in code (arrays, lists, maps)[cite: 1].
*   **Interface:** A graphical UI is strictly optional[cite: 1]. A Command Line Interface (CLI) in Java or C that prints formatted tables is highly recommended to ensure completion within the 24-hour window[cite: 1, 3].

---

## 4. GitHub Copilot Master Prompt (Plan Mode)
*Use this prompt to establish the architecture before generating code.*

> **Context:**
> I am building the "College Event Certificate Eligibility Board" (SI26_P12) for a Cisco AI-Assisted Coding Interview. I will be building a CLI application in Java (or C) using purely in-memory data structures. 
> 
> **Task:**
> Generate a comprehensive, start-to-finish project plan. Do not write the full code yet. Instead, give me a detailed architectural blueprint and a strict step-by-step development guide.
> 
> **Deliverable 1: Complete Architecture & Component Design**
> *   List every Class (or Struct) I need to create.
> *   List the exact fields/variables for each data model.
> *   List the core methods/functions needed (e.g., evaluation, validation, reset).
> *   **Crucial:** For every single component and method, provide a brief explanation of *why* we are designing it this way (trade-offs and engineering justification).
> 
> **Deliverable 2: The 3-5 Step Implementation Plan**
> Provide the required 3-5 ordered step plan for my interview presentation. 
> 
> **Deliverable 3: Step-by-Step Development & Git Workflow**
> Provide a detailed guide on how to actually build this, including suggested Git branches and the exact sequence of implementation.
> 
> **Deliverable 4: Detailed Solution Logic**
> Explain the exact sequence for the evaluation loop. How exactly will the logic check that the user has >= 6 points AND all three categories (LEARN, BUILD, SHARE) without stopping at the first failure? How will it sort the missing categories in the exact contracted order?

---

## 5. Detailed Project Plan & Architecture

### Data Models
*   **`Activity`**: Contains `activityId`, `name`, `category` (LEARN, BUILD, SHARE), and `points`[cite: 1]. 
    *   *Why?* Encapsulates the fixed activity definitions.
*   **`Participant`**: Contains `participantId`, `name`, and a list of `completedActivityIds`[cite: 1].
    *   *Why?* Keeps the user profile separate from the evaluation results.
*   **`EvaluationResult`**: Contains `participantId`, `totalPoints`, a boolean `isEligible`, and a list of `failureReasons`[cite: 1].
    *   *Why?* Decouples raw data from the business logic output.

### Core Components
*   **`DataStore`**: In-memory repository holding the built-in participants (C01-C05) and activities (A01-A04)[cite: 1]. Allows for a trivial `Reset` implementation[cite: 1].
*   **`EligibilityEvaluator`**: The core logic engine. Maps a participant's activity IDs to the `DataStore`, calculates points, and checks category coverage[cite: 1].

### Implementation Plan (4-Step Roadmap)
1.  **Data Scaffolding & State**: Implement the in-memory `DataStore` and the `Reset` functionality to load the built-in oracle data[cite: 1].
2.  **Core Evaluation Engine**: Build the point aggregation and category validation logic, ensuring all requirements are evaluated completely without short-circuiting[cite: 1].
3.  **Input Validation**: Implement checks for `DUPLICATE_PARTICIPATION`, `UNKNOWN_ACTIVITY`, and clear result rows on error[cite: 1].
4.  **CLI Reporting & Sorting**: Format the terminal output to show eligible participants first, ineligible second (sorted by ID), and display counts[cite: 1].

### Core Evaluation Logic Map
1.  **Map & Aggregate**: Iterate through `completedActivityIds`. Fetch the corresponding `Activity`, sum points, and record categories seen[cite: 1].
2.  **Complete Evaluation**: Do not stop if a rule fails[cite: 1]. Check category presence strictly in order: `LEARN`, `BUILD`, `SHARE`[cite: 1]. Append `MISSING_CATEGORY: [NAME]` if missing[cite: 1].
3.  **Point Check**: Evaluate if total points >= 6. If not, append `POINTS_BELOW_6` to the error list[cite: 1].
4.  **Determine Status**: If error list is empty, `isEligible = true`.

### Testing & Acceptance Criteria
*   **Standard Oracle**: C01 (7 pts) and C02 (6 pts) are eligible[cite: 1].
*   **Missing Categories**: C03 reports missing SHARE; C04 reports missing LEARN[cite: 1].
*   **Compound Failure**: C05 reports missing BUILD followed by POINTS_BELOW_6 (4 pts)[cite: 1].
*   **Edge Case**: Empty completion list yields 0 points, missing all three categories + points below 6[cite: 1].
*   **Validation Check**: Attempting to add a second A01 to C01 throws `DUPLICATE_PARTICIPATION` and clears the board[cite: 1].

## User
SI26_P12: College Event Certificate Eligibility Board
AI-Assisted Coding Interview Problem

Problem Statement
Build a compact College Event Certificate Eligibility Board for an organizing team reviewing activity participation. It should look up the points and category of each completed activity, aggregate them per participant, and show who has earned enough points while covering every required activity category.

[... Full prompt details provided in original message ...]

i didnt understand the problme statemnet ..can you mak eme undertsand

---

## Assistant
It is completely understandable that this problem statement feels overwhelming. It is dense and packs a lot of specific rules into a single document. 

Here is the plain-English breakdown of what you are actually being asked to build.

### The Big Picture

You need to build a simple application (it can be a web page, a desktop app, or even a command-line script) that tracks students participating in college events. 

Your app will look at the activities each student completed and decide if they have earned a **Certificate of Eligibility**. 

### The Two Main Rules for Eligibility

To get a certificate, a participant must meet **both** of these conditions:

1. **Category Coverage:** They must complete at least one activity from all three categories: **LEARN**, **BUILD**, and **SHARE**.
2. **Point Minimum:** The total points from their completed activities must add up to **6 or more**.

If a student misses even one category, or gets 5 points or fewer, they are not eligible. 

### What Your App Needs to Do

Your application needs a few specific features to pass the test:

* **The Data:** It must start with the exact default list of Activities (A01, A02, A03, A04) and Participants (Asha, Bilal, Chen, Divya, Eshan) provided in the prompt.
* **The "Evaluate" Action:** A button or command that calculates everyone's score and category coverage.
* **The Display:** A clear list showing who is eligible and who is not. 
* **The Sorting:** Eligible people are listed first, ineligible people second. Within those two groups, sort them alphabetically by their ID (C01, C02, etc.).
* **The "Reset" Action:** A button or command that wipes any changes you made and restores the default starting data.

### Handling Failures and Errors

The grading criteria places a heavy emphasis on how you handle bad data and failures. 

**When a student is ineligible**, you must tell them exactly why, in this exact order:
* `MISSING_CATEGORY: LEARN` (if applicable)
* `MISSING_CATEGORY: BUILD` (if applicable)
* `MISSING_CATEGORY: SHARE` (if applicable)
* `POINTS_BELOW_6` (if applicable)

**When the user inputs bad data**, your app needs to stop evaluating and throw a specific error message. The required errors are:
* `INVALID_PARTICIPANT`: If an ID or name is left blank.
* `DUPLICATE_PARTICIPANT_ID`: If two people have the same ID.
* `UNKNOWN_ACTIVITY`: If someone tries to claim an activity that doesn't exist (like "A99").
* `DUPLICATE_PARTICIPATION`: If someone tries to claim points for the exact same activity twice (e.g., listing "A01, A01").

### The Interview Catch

Because this is an **AI-Assisted Coding Interview**, the interviewers don't just care about the final code. They want to see **how you use AI** to get there. You will be judged on how well you write prompts for ChatGPT/Claude/Gemini to generate the code, and how well you can explain the code the AI gives you.

---

## User
Evaluate local records only; do not add certificate document generation, event registration, payments, accounts, or scheduling."..explain this 

---

## Assistant
This sentence is the interviewer’s way of saying: **"Keep it simple and do not overcomplicate the project."** 

In software engineering interviews, candidates sometimes try to impress the evaluators by building massive, fully featured applications. This instruction is a strict boundary telling you exactly what **not** to build so you can focus entirely on the core logic.

Here is the breakdown of exactly what that sentence means for your code:

### 1. "Evaluate local records only"
* **What it means:** Your application should just use simple, hardcoded data structures (like arrays, lists, or JSON objects) stored directly in your computer's memory while the app is running. 
* **What you should NOT do:** Do not set up a database (like MySQL, MongoDB, or Postgres). Do not build a backend server to fetch data from an external API. 

### 2. "Do not add certificate document generation"
* **What it means:** The prompt calls this a "Certificate Eligibility Board," but you only need to show the *word* "ELIGIBLE" on the screen.
* **What you should NOT do:** Do not write code that generates downloadable PDF certificates, creates image files, or formats a printable document for the student. 

### 3. "Do not add event registration, payments, accounts, or scheduling"
* **What it means:** Treat this app as a simple internal tool for an admin who already has all the data. 
* **What you should NOT do:** 
    * **No Accounts:** Do not build a login page, password authentication, or user roles.
    * **No Registration/Scheduling:** Do not build a calendar view or a form for students to "sign up" for future events. 
    * **No Payments:** Do not integrate Stripe, PayPal, or any cart system.

### Summary
You are building a **calculator and a dashboard**, not a full-scale university management platform. The interviewers want to see if you can nail the specific logic rules (the categories, the 6-point minimum, and the strict error messages) rather than seeing if you know how to connect a database or generate a PDF.