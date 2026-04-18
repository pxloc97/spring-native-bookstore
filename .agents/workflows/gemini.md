---
description: How to interact with Gemini within this project
---

# Gemini Workflow & Execution Rules

This document outlines how Gemini should operate within the Spring Native Bookstore project. It adapts the core principles from `AGENTS.md` and tailors them specifically for Gemini's agentic capabilities and tools.

## 1. Tool Usage & Execution Strategy
- **Prioritize Specific Tools**: Always use specialized tools (`view_file`, `grep_search`, `list_dir`, `replace_file_content`) over generic bash equivalents (`cat`, `grep`, `ls`, `sed`).
- **Surgical Edits**: Use `replace_file_content` or `multi_replace_file_content` to surgically modify code. Never do complete file rewrites unless explicitly creating a new file. Touch only what is strictly required.
- **Continuous Verification**: 
  - Define success criteria before making code changes.
  - After making code changes, use `run_command` autonomously (with `SafeToAutoRun: true` when safe) to compile and test the changes (e.g., `./gradlew build` or specific test commands). Monitor status using `command_status`.

## 2. Interaction Mindset
- **Think Before Acting**: Output your assumptions. If requirements are ambiguous, pause and ask the USER for clarification rather than silently guessing.
- **Simplicity Over Setup**: Output the minimal code required. Do not add speculative abstractions, unnecessary flexibility, or boilerplate for "future use."
- **Multi-step Execution**: Provide a brief plan outlining verification points for multi-step tasks. Use the format: `[Step] → verify: [check]`.

## 3. Project Context Rules
- **Reference Architecture**: Treat `order-service` as the target hexagonal architecture. Isolate business logic in domain/application layers, separate from framework adapters.
- **Testing Approach**: Write a failing test first to reproduce an issue, then apply the fix. Ensure tests utilize `Testcontainers` appropriately for database/messaging services.
- **Build & Style Alignment**: Run project validation tools post-edit. Use `./gradlew spotlessApply spotlessCheck` for formatting in `catalog-service` and `order-service`.

## 4. Safe Refactoring
- Do not improve adjacent, unrelated code, comments, or formatting implicitly. Clean up only the orphans that YOUR specific changes introduced (e.g., unused imports, dead variables).
- Every modified line must clearly trace back to the USER's primary objective.

**When in doubt or dealing with high-risk infrastructure/database changes, stop and explain the situation to the USER.**
