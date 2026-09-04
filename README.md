# RecoverIQ — AI Revenue Recovery & Intervention Optimization Agent

**Razorpay AI Builder Internship 2026 Buildathon — Track 03: AI Revenue Recovery**

An AI-assisted agent that detects revenue at risk, prioritizes it by expected recoverable value, recommends the next-best intervention through an LLM, and executes only policy-approved, bounded actions — with a full audit trail and a measured, evidence-backed outcome.

---

## Table of Contents

- [The Problem](#the-problem)
- [Objectives](#objectives)
- [Architecture](#architecture)
- [Key Differentiators](#key-differentiators)
- [What's Implemented](#whats-implemented)
- [Measured Results](#measured-results-real-not-invented)
- [Guardrails](#guardrails-enforced-in-code-not-just-documented)
- [Tech Stack](#tech-stack)
- [Running Locally](#running-locally)
- [Demo Flow](#demo-flow)
- [API Reference](#api-reference)
- [Build Challenges & Technical Obstacles](#build-challenges--technical-obstacles)
- [Known Limitations & Honest Tradeoffs](#known-limitations--honest-tradeoffs)
- [What's Next](#whats-next)
- [Project Structure](#project-structure)

---

## The Problem

Most "AI revenue recovery" tooling falls into one of two failure modes.

The first is dumb but safe: blast every failed payment with the same generic retry or reminder, regardless of whether that case is actually recoverable. This wastes limited merchant capacity — support bandwidth, message quotas, customer goodwill — on cases that were never going to convert.

The second is smart but unsafe: hand an LLM open-ended authority to decide and execute financial actions, with no deterministic ceiling on retries, spend, or escalation. That's a real risk once actual money is involved.

RecoverIQ tries to avoid both. It treats revenue recovery as a constrained optimization problem rather than a blast campaign, and it treats the LLM as an advisor with no financial authority, not an autonomous actor.

---

## Objectives

- Detect revenue-risk events (failed payments, failed subscriptions) as they occur
- Estimate each case's recovery probability using a transparent, explainable heuristic
- Rank cases by expected recoverable value (`amount_at_risk × recovery_probability`), not raw amount
- Use an LLM to reason about root cause and recommend a next-best action, in strict structured output
- Enforce every financial and operational constraint through deterministic Java code, never the AI
- Execute only approved actions, with idempotency guarantees against duplicate execution
- Record a complete, queryable audit trail of every decision
- Prove the approach actually works with a measured treatment vs. control experiment, not a claimed number

### Why expected value, not raw amount

| Case | Amount at Risk | Recovery Probability | Expected Recovery |
| :--- | --------------: | ---------------------: | -------------------: |
| A    |         ₹20,000 |                    40% |               ₹8,000 |
| B    |         ₹10,000 |                    90% |               ₹9,000 |
| C    |         ₹50,000 |                     5% |               ₹2,500 |

A merchant with limited intervention capacity should chase Case B before Case A, and mostly leave Case C alone — despite it having the largest amount at risk. This is exactly what RecoverIQ's ranking does, verified live: a synthetic ₹50,000 `MANDATE_CANCELLED` case (low base recovery probability, no positive payment history) ranks below a ₹20,000 `NETWORK_FAILURE` case with strong history, in the actual running system.

---

## Architecture

The LLM recommends. It never authorizes.

```
Revenue-risk data
      │
      ▼
Expected-recovery scoring     (deterministic: amount × probability, informed by
      │                        failure category, payment history, retry count)
      ▼
LLM recommendation            (structured JSON: risk_level, root_cause,
      │                        recommended_action, reason, confidence)
      ▼
Schema validation gate        (rejects malformed/unsupported AI output —
      │                        bad output never reaches the policy engine)
      ▼
Deterministic Policy Engine   (Java, zero AI dependency — enforces retry
      │                        limits, contact limits, recovery window,
      │                        confidence threshold → ALLOW / BLOCK / ESCALATE / STOP)
      ▼
Action Executor                (executes only ALLOW-ed actions, idempotent —
      │                        duplicate execution is structurally impossible)
      ▼
Audit Log                      (every decision, every transition, recorded)
```

There's no code path from LLM output to money moving, or to a customer being contacted, without first passing through the deterministic Policy Engine. That's enforced structurally — separate `service.ai` and `service.policy` packages, with the policy engine taking zero AI-derived input as authoritative — not just drawn in a diagram.

---

## Key Differentiators

1. Expected-recovery prioritization — ranks by recoverable value, not face value
2. Bounded recovery budget — respects merchant-defined limits on retries, contacts, messages, and escalations
3. Treatment vs. control experiment — the AI-assisted path is measured against a naive baseline, not just assumed to be better
4. Deterministic financial guardrails around the LLM — structural, not advisory
5. Explicit stopping rules — a case stops the moment it's no longer worth pursuing (window expired, payment already succeeded, limits exhausted)
6. Full audit trail — every AI recommendation and every policy decision is persisted and queryable
7. Human escalation for low-confidence cases — the system knows when to defer to a person

---

## What's Implemented

| Component                                                                                                                    | Status                                        |
| :---------------------------------------------------------------------------------------------------------------------------- | :---------------------------------------------- |
| Domain model — 8 tables: customers, payments, subscriptions, recovery_cases, recovery_actions, policies, audit_logs, experiments | Done                                          |
| Expected-recovery scoring (failure category baseline + payment history + retry decay)                                       | Done                                          |
| LLM structured recommendation (mocked for this submission — interface-based, swappable to a real provider with zero downstream changes) | Done                                          |
| AI output schema validation — hard gate, 7 passing unit tests                                                               | Done                                          |
| Deterministic policy engine — 6 guardrail rules + confidence threshold, 8 passing unit tests                                | Done                                          |
| Action executor with idempotency guarantees                                                                                 | Done                                          |
| Full audit trail                                                                                                             | Done                                          |
| Treatment/control experiment at scale (5,000 synthetic cases)                                                               | Done                                          |
| React dashboard — executive summary + ranked recovery queue                                                                 | Done                                          |
| Razorpay test-mode webhooks                                                                                                 | Architected, not wired — see [What's Next](#whats-next) |

---

## Measured Results (real, not invented)

Run against 5,000 synthetic revenue-risk cases, 50/50 treatment/control split, reproducible via a single API call:

| Metric                         |          Value |
| :------------------------------ | ---------------: |
| Total revenue at risk          |  ₹12,78,15,499 |
| Treatment group recovery rate  |             47% |
| Control group recovery rate    |             21% |
| Incremental lift               |            +128% |
| Actual revenue recovered       |   ₹4,36,71,696 |
| Interventions executed         |           5,000 |
| Successful interventions       |           1,687 |
| Cost per successful recovery   |           ₹3.70 |

Reproduce this yourself:
```
POST /api/experiments/run?batchSize=5000
GET  /api/experiments/results
```

These numbers come directly from the experiment service's own aggregation logic over the batch it generates — there's no hardcoded or manually-entered result anywhere in this project.

---

## Guardrails (enforced in code, not just documented)

| Guardrail                | Value    |
| :------------------------- | ---------: |
| `MAX_RETRIES`             |        2 |
| `MAX_CONTACTS`            |        3 |
| `MAX_MESSAGES`            |        3 |
| `MAX_HUMAN_ESCALATIONS`   |        1 |
| `MAX_RECOVERY_WINDOW`     |   7 days |
| `CONFIDENCE_THRESHOLD`    |     0.50 |

| Condition                                              | Policy Decision                                                          |
| :--------------------------------------------------------| :---------------------------------------------------------------------- |
| Payment already succeeded                               | STOP                                                                     |
| Retry / contact / message / escalation limit reached     | STOP                                                                     |
| Recovery window (7 days) expired                         | STOP                                                                     |
| AI recommends an action outside the supported enum       | BLOCK (caught by schema validation before the policy engine even runs)  |
| Confidence below 0.50                                    | ESCALATE to human review                                                 |
| All checks pass                                          | ALLOW — action executes, guardrail counters increment                   |

Guardrail limits live in the `policies` database table, not application constants — there's no code path by which AI output can modify them.

---

## Tech Stack

**Backend:** Java 21, Spring Boot 3.3, Spring Data JPA, PostgreSQL 16, Flyway, Maven

**Frontend:** React, Vite, Axios

**AI:** LLM API accessed via a swappable `LlmClient` interface — currently backed by a deterministic mock implementation for this submission. Swapping in a live provider (OpenAI, Anthropic, Gemini) is a single new class, with zero changes to validation, policy, or execution logic.

---

## Running Locally

```bash
# 1. Start Postgres
docker run --name recoveriq-postgres \
  -e POSTGRES_USER=recoveriq -e POSTGRES_PASSWORD=recoveriq -e POSTGRES_DB=recoveriq \
  -p 5432:5432 -d postgres:16

# 2. Backend
cd backend
mvn clean compile
# run BackendApplication — Flyway migrates the schema automatically on boot

# 3. Frontend
cd frontend
npm install
npm run dev
```

Copy `.env.example` to `.env` and fill in real values only if wiring a live LLM provider or Razorpay test credentials. The app runs fully functional with defaults otherwise.

---

## Demo Flow

A clean, reproducible walkthrough — this is exactly what's shown in the pitch video.

1. Open the dashboard to see the live executive summary (open cases, amount at risk, expected recovery) and the latest experiment's treatment-vs-control results.
2. Review the Recovery Queue — cases ranked by expected value, not raw amount.
3. `POST /api/recovery/cases/{id}/analyze` — returns the AI recommendation with reasoning, risk level, confidence, and the policy engine's decision.
4. `POST /api/recovery/cases/{id}/execute` — executes the action, but only if the policy decision was ALLOW. Returns the execution result and idempotency key.
5. `POST /api/recovery/cases/{id}/stop` on a separate case — status becomes STOPPED.
6. Refresh the dashboard — both status changes persist and render correctly, confirming the full write → persist → re-fetch → render chain.

---

## API Reference

| Method | Endpoint                              | Description                                                          |
| :----- | :------------------------------------- | :--------------------------------------------------------------------- |
| GET    | `/api/dashboard/summary`              | Executive summary of open cases, amount at risk, expected recovery  |
| GET    | `/api/recovery/cases`                 | List all recovery cases                                             |
| POST   | `/api/recovery/cases/{id}/analyze`    | AI recommendation + policy decision                                 |
| POST   | `/api/recovery/cases/{id}/execute`    | Execute an ALLOW-ed action                                           |
| POST   | `/api/recovery/cases/{id}/stop`       | Manually stop a case                                                 |
| POST   | `/api/experiments/run?batchSize=N`    | Run a treatment/control experiment                                   |
| GET    | `/api/experiments/results`            | Fetch latest experiment results                                      |

---

## Build Challenges & Technical Obstacles

An honest account of what actually went wrong during development and how it got resolved. A few of these are worth flagging as engineering judgment calls, not just debugging.

**N+1 query problem at scale.** The recovery-case listing endpoint initially lazy-loaded each case's customer individually — fine at 5 rows, catastrophic at 5,000+ once the synthetic experiment ran (hundreds of sequential `SELECT` calls per request). Fixed with a `JOIN FETCH` query plus a result cap, instead of trying to eagerly load the entire dataset.

**Floating-point display noise.** The recovery-probability calculation, built from summed `double` adjustments, occasionally produced values like `0.9500000000000001` — standard IEEE 754 binary floating-point behavior. Rounded at the point of calculation rather than masking it at display time, so the underlying data stayed clean for the experiment aggregation too.

**Lombok/JDK version drift.** Running on a newer local JDK (23) than the project's compilation target (21) caused Lombok's annotation processor to silently fail under a plain Maven build, while still working fine inside the IDE — a classic "works on my machine" trap. Resolved by pinning an explicit Lombok version and annotation-processor path in `pom.xml`, rather than relying on IDE-specific tooling to paper over it.

**Idempotency as a database constraint, not just application logic.** Rather than trusting a service-layer check to prevent duplicate execution of the same recovery action, the `idempotency_key` column carries a database-level UNIQUE constraint — a duplicate execution attempt is structurally impossible, not just discouraged.

**Environment/timezone mismatch.** A Windows JVM reporting a legacy timezone alias (`Asia/Calcutta` vs. `Asia/Kolkata`) caused the Postgres JDBC handshake to fail on some machines but not others. Fixed by explicitly setting the JVM's default timezone in application startup code, instead of requiring an environment-specific VM argument that wouldn't travel with the repo.

---

## Known Limitations & Honest Tradeoffs

- **Synthetic data, not live Razorpay data.** Failure categories (`INSUFFICIENT_FUNDS`, `CARD_EXPIRED`, etc.) are realistic labels but explicitly not exact Razorpay production failure codes.
- **The LLM is mocked, not a live API call, for this submission** — a deliberate choice to control cost and iteration speed during development. The mock's reasoning logic mirrors what a real model would plausibly produce (failure category → root cause → recommended action, using the same signals as the scoring layer) and is fully swappable via the `LlmClient` interface without touching validation, policy, or execution code.
- **The 5,000-case experiment scores in bulk directly**, rather than routing every case through the full HTTP `/analyze → /execute` pipeline individually — a deliberate speed tradeoff for batch-scale measurement. The single-case `/analyze` and `/execute` endpoints remain the fully-traced, individually-audited path used in the live demo flow.
- **Razorpay test-mode webhook integration is architected but not wired** in this submission — prioritized the harder, more central problem (bounding an LLM's financial authority and proving a measurable lift) given time constraints.

---

## What's Next

- Wire Razorpay test-mode webhooks for live payment/subscription failure ingestion
- Swap the mocked `LlmClient` for a real provider (interface already supports this with no other code changes)
- Dedicated Case Detail, Agent Activity Timeline, and Audit Trail screens in the dashboard
- A lightweight ML model for recovery-probability estimation as an alternative to the current heuristic, behind the same interface

---

## Project Structure

```
recoveriq/
├── backend/
│   └── src/main/java/com/recoveriq/
│       ├── domain/          JPA entities
│       ├── repository/      Spring Data repositories
│       ├── dto/              request/response DTOs
│       ├── controller/      REST endpoints
│       ├── service/
│       │   ├── scoring/      expected-recovery calculation
│       │   ├── ai/           LLM client + schema validation
│       │   ├── policy/       deterministic guardrail engine
│       │   ├── execution/    action executor + idempotency
│       │   ├── audit/        audit log service
│       │   ├── data/         synthetic data generation
│       │   └── experiment/   treatment/control measurement
│       └── enums/, exception/, config/
├── frontend/                 React dashboard (Vite)
├── README.md
└── .env.example
```
