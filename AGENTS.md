## Mandatory UI Mapper Architecture

For every task that creates, modifies, refactors, reviews, or analyzes the UI mapper and its related pipeline, the `$ui-platform-architecture` skill is mandatory.

This applies to:

- UI discovery
- SPA discovery and state transitions
- PageModel construction
- semantic element and action mapping
- locator discovery, scoring, ranking, and promotion
- requirement-scoped interaction discovery
- live browser verification
- evidence lifecycle
- ConfirmedUiCatalog
- page ownership and page-scoped context
- knowledge persistence and retrieval related to UI evidence
- POM contracts
- POM generation
- deterministic Java generation
- self-healing locator flows
- mapper quality gates and validators

The canonical architecture is defined by:
`$ui-platform-architecture`

Before implementing any change in these areas:

1. Apply the `$ui-platform-architecture` skill.
2. Identify the affected canonical pipeline stage.
3. Identify the source of truth for the decision being changed.
4. Verify that the change does not introduce a parallel decision path.
5. Verify that it does not bypass evidence promotion or quality gates.
6. Apply the Change Admission Gate defined by the architecture skill.
7. Prefer modifying an existing stage over introducing a new Agent, Service, DTO, fallback, or configuration flag.
8. Preserve the canonical evidence lifecycle:

   Raw UI Evidence
   → Locator / Element / Action Candidates
   → Static Interaction Scoring
   → Requirement Relevance Filtering
   → Top-K Selection
   → Live Verification
   → SPA State/Postcondition Verification
   → Score Recalculation
   → Evidence Promotion or Rejection
   → Confirmed Locator Persistence
   → ConfirmedUiCatalog
   → POM Contract
   → Quality Gate
   → Deterministic Java Generation

No alternative path from raw, candidate, fallback, retrieved, or unverified evidence to the POM contract or generated Java is allowed.

If a requested implementation conflicts with `$ui-platform-architecture`, stop before implementation and explicitly report:

- the conflicting rule;
- the affected pipeline boundary;
- why the proposed change violates the architecture;
- the architecture-compliant alternative.

Architecture changes themselves must be explicit and versioned. They must not be introduced implicitly as part of feature implementation or bug fixing.
