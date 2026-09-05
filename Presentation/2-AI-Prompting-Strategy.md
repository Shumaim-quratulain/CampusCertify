# 2. AI Prompting Strategy

## How the prompts evolved — broad, then increasingly specific

The full raw conversation is in `Chats/Idea.md`. Below is the actual prompt trail, showing the broad→specific pattern the rubric asks to demonstrate.

### Stage 1 — Broad exploration
> *"...give me detailed explanation of the solution, give me [what's] needed to make this project, give me full detailed architecture, overview needed how to start how to make... i know java, spring boot, i have made backend api... now tell me which method best for me... don't write code yet, i just need to explore things now"*

This established the constraint set (Java/Spring Boot background, no code yet) before any design happened.

### Stage 2 — Narrowing the architecture
> *"can we use database instead of in memory... and tell me why you are using this architecture"*
> *"what about using mysql"*
> *"so what you prefer"*

Three follow-ups in a row on one decision point (storage). Each answer was pushed back on until a final, defended choice (in-memory) was locked in — not accepted on the first answer.

### Stage 3 — Verifying against the source
> *"did they have mention in memory data structure?"*

A direct fact-check against the original problem statement, rather than trusting the AI's summary.

### Stage 4 — Requesting the full build plan
> *"ohket so now tell me from starting what do i need to build this project... all the implementation method component architecture"*
> *"explain me how to build this step by step"*

The request moved from "what" to "how," asking for an ordered execution plan only once the architecture was settled.

### Stage 5 — Adversarial/critical follow-ups
> *"why you didnt added security?"*
> *"did in the problem statement it is mention to not add security?"*

Directly challenging a design omission and asking for the exact textual justification — not accepting "the spec forbids it" without checking.

### Stage 6 — Tooling decisions
> *"for implementation what should be use vs code or intellji"*

A late-stage practical question, asked only after the architecture and plan were both finalized.

## What this demonstrates

- Prompts got progressively more specific, not looser — each stage built on the answer to the previous one
- Multiple prompts (Stage 2) were used to interrogate a single decision rather than accepting the first answer
- At least one prompt (Stage 3, Stage 5) explicitly asked the AI to justify itself against the source document, catching a case where the AI's phrasing ("the spec forbids security") was **corrected** to the more precise "the spec forbids accounts, which removes the basis for auth" — see `4-AI-Influenced-Decision-Making.md`
