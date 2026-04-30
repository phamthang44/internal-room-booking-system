# Skill: Controlled Unit Test Generation

## Purpose
Generate unit tests that are concise, readable, behavior-focused, and easy for humans to review.

This skill prevents:
- Overly verbose tests
- Excessive mocking
- Testing implementation details
- Low-readability generated test suites
- Large unreviewable test files

The AI must prioritize maintainability over coverage volume.

---

## Core Principle

The human defines **what to test**.

The AI implements **how to write the test code**.

Never invent large test suites autonomously.

---

## Workflow

Follow this process strictly.

### Step 1 — Analyze Before Writing

Before generating any test code, use graphify to understand the class and its dependencies cheaply:

```
/graphify explain "<TargetClass>"
```

1. Inspect the target class's graph output
2. Identify:
   - Public behaviors
   - Business rules
   - Edge cases
   - External dependencies (from the graph relationships)
   - Potential testability issues

Then output:

## Behavior Analysis
- Behavior A
- Behavior B
- Behavior C

## Risks
- Risk 1
- Risk 2

## Testability Concerns
- Concern 1
- Concern 2

Do NOT write tests yet.

Wait for confirmation.

---

### Step 2 — Propose Test Cases

Generate a small prioritized list.

Format:

## Proposed Test Cases

Priority 1:
- should_x_when_y

Priority 2:
- should_x_when_y

Priority 3:
- should_x_when_y

Rules:
- Maximum 7 test cases
- Focus on business-critical behavior
- Prefer fewer meaningful tests over many shallow tests

Wait for approval.

---

### Step 3 — Implement Incrementally

Generate only approved test cases.

Never generate the entire file unless explicitly requested.

Default batch size:
- 1 to 3 tests

---

## Test Writing Rules

### Structure

Use AAA pattern:

Arrange  
Act  
Assert

---

### Naming

Use:

should_<expected_behavior>_when_<condition>

Example:

should_reject_booking_when_room_overlaps

---

### Length

Each test should ideally remain under 25 lines.

If longer:
- explain why
- suggest refactor opportunities

---

### Assertions

Each test should verify ONE behavior.

Avoid mixed assertions.

Bad:

assert status
assert response
assert repository
assert event

Good:

One behavioral contract per test

---

### Mocking Rules

Mock only direct collaborators required for the scenario.

Avoid:
- unnecessary stubbing
- chain mocking
- deep mocks

If many mocks are required, flag:

## Refactor Suggestion
This class may have low testability because ...

---

### Forbidden Patterns

Do NOT:

- Test private methods
- Test implementation details
- Assert internal call order unless behaviorally required
- Generate snapshot-style giant assertions
- Create huge setup blocks
- Add unnecessary helper abstractions

---

## Refactoring Feedback

If the target code is hard to test, provide:

## Design Feedback
- Problem
- Why it hurts testing
- Suggested refactor

Example:
Extract pricing logic into PricingPolicy

---

## Output Format

When generating tests:

1. Brief explanation
2. Test code
3. Notes (if needed)

---

## Quality Checklist

Before final output verify:

- Readable in under 30 seconds
- Clear intent from test name
- Minimal mocks
- Behavior-focused
- Reviewable by human
- No unnecessary complexity

If any check fails, simplify.

---

## Preferred Mindset

Optimize for:

clarity > cleverness  
maintainability > completeness  
reviewability > automation speed

---

## Example Invocation

When asked:

"Generate tests for BookingService"

You must respond with:

Step 1 analysis only.

Never jump directly to code.