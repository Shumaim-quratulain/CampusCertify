# 2. AI Prompting Strategy

**Tool used:** GitHub Copilot Chat (agent mode), model Claude Sonnet 4.5. Full unedited transcript in `Chats/Idea.md` (exploration phase) and `Chats/Implementation.md` / `Chats/execution.md` (build phase).

## Translating the problem statement into technical specifications — the exact sequence

The problem statement itself was pasted in full as the very first message. From there, the strategy was to **never let the AI generate code until the architecture was fully interrogated and approved.** The literal words used to enforce this:

> *"dont write code yet ...i just need to explore things now"*

That single constraint shaped the entire first phase of the conversation — every subsequent prompt was a design question, not an implementation request.

## Stage-by-stage prompt trail (broad → specific)

### Stage 1 — Broad exploration, establishing context
> *"...i want you to give detailed explanation of the solution, give me [what's] needed to make this project, give me full detailed architecture, overview needed how to start how to make... what to use and why we are using this each and every detailed explanation... Explain the architecture, what components we will need, and why this approach is best for this specific problem. Give me a checklist for each step so I can track progress."*
>
> *"i know java, spring boot, i have made backend api... now tell me which method best for me"*

**Why this framing:** stating existing skills up front (Java/Spring Boot) meant the AI's recommendation had to be justified against that background specifically, rather than giving a generic "best practices" answer.

### Stage 2 — Narrowing one decision at a time, with repeated pushback
> *"can we use databse instead of in memeory ...and tell me why you are using this architechture"*
> *"what about uding mysql"*
> *"so what you prefer"*

Three consecutive prompts on a **single** decision point (storage). Each AI answer was treated as a first draft to be pressure-tested, not a final answer — the third prompt ("so what you prefer") explicitly forced a committed recommendation instead of another comparison table.

### Stage 3 — Fact-checking the AI against the source document
> *"did they ahve mention in memeory data structire ?"*

Rather than trusting the AI's own justification for its recommendation, this directly asked it to quote the problem statement. It responded with the exact sentence: *"You may use in-memory data structures, a spreadsheet or notebook, a browser..."* — confirming the recommendation wasn't invented.

### Stage 4 — Escalating from "what" to "how"
> *"ohket so now tell me from starting what what do i need to build this project ...all the implememntation mnethod component architecture"*
> *"explain me how to build this step by step"*

Only once the architecture itself was settled did the prompts shift to requesting an ordered execution plan — deliberately sequenced so the "how" request wouldn't have to be redone if the "what" changed.

### Stage 5 — Adversarial follow-ups that caught an overstatement
> *"why you didnt added security?"*
> *"did in the problem statemnet it is mention to not add security ?"*

The first AI answer claimed *"the spec explicitly forbids [security]"* — which was **not accurate**. The second prompt directly demanded the exact source text, which forced a correction: the spec forbids **accounts**, not security broadly. This is the clearest example in the whole transcript of a prompt catching an AI overstatement rather than accepting it. Full detail in `4-AI-Influenced-Decision-Making.md`.

### Stage 6 — Re-reading the spec's own constraints in plain language
> *"This sentence is the interviewer's way of saying: 'Keep it simple and do not overcomplicate the project.' ... are you following this?"*

This prompt supplied the user's own interpretation of a spec sentence and asked the AI to **audit the existing plan against it**, rather than asking the AI to interpret from scratch — a way of using the AI as a checklist-compliance reviewer, not just a designer.

### Stage 7 — Committing to a final architecture, then asking for tooling
> *"so what you prefer"* (repeated, at the DB-vs-server decision point)
> *"for implemnetation what should be use vs code or intellji"*

A late-stage, practical question — asked only after the architecture, storage, security posture, and plan were all already finalized, so the answer could be judged purely on workflow merits (existing AI-assistant setup, existing chat history) rather than technical capability.

### Stage 8 — Moving from planning into execution
> *"now lets start implementing step by step by step full organizined make folder fpr each pakage make file for like category activity an all ...amd add code in that start and i want to store this chat in chat2.md from now"*

A single prompt that did three things at once: authorized moving from planning to code, specified the exact folder-per-package structure expected, and requested a persistent running log — setting up the audit trail used throughout the rest of the build (see `Chats/Implementation.md`).

## Prompting principles demonstrated across the whole session

1. **No code before architecture approval** — enforced explicitly, more than once
2. **Never accept the first answer on a consequential decision** — the storage question alone took 3 rounds
3. **Fact-check AI claims against the primary source** — twice: once for "in-memory is allowed," once for "security is forbidden"
4. **Ask the AI to audit its own prior work against a new constraint** — the STUDENT_GUIDE compliance pass
5. **Sequence prompts to avoid rework** — architecture settled before plan requested, plan settled before code requested
6. **Use the AI conversationally as a running project log**, not just a code generator — explicit requests to persist decisions into `chat1.md`/`chat2.md`/`execution.md` so the reasoning trail itself became a deliverable
