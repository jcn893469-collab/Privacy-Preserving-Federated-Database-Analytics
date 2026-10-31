# Project Plan

The chart below is a **proposed remaining-work schedule**, not a record of
historical team activity. Dates are estimates and should be adjusted to the
course calendar. No individual assignments are implied.

```mermaid
gantt
    title Proposed Remaining BCSE307L Work
    dateFormat YYYY-MM-DD
    axisFormat %b %d
    section Database
    Validate V11/V12 migrations and procedure :dbtest, 2026-11-02, 2d
    Add database integration and transaction tests :dbtests, after dbtest, 3d
    section Application
    Complete dataset CRUD and validation :crud, 2026-11-04, 4d
    Verify role/API matrix and logout behavior :security, 2026-11-04, 3d
    section Operations
    Run backup/restore and Docker validation :ops, 2026-11-09, 3d
    Resolve repository visibility and deployment :deploy, 2026-11-12, 4d
    section Submission
    Capture genuine screenshots and results :evidence, 2026-11-16, 2d
    Final rubric review :review, after evidence, 2d
```

## Phase tracking

Actual repository work and validation status are maintained in
[docs/BUILD_PROGRESS.md](../BUILD_PROGRESS.md). Any schedule changes or team
assignments must be confirmed by the project team.
