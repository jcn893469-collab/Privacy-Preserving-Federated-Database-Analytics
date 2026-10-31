# Problem Statement

Centralizing organization-level datasets can expose sensitive records and
increase the impact of a data breach. Researchers still need population-level
statistics across participating data owners.

This project demonstrates a federated pattern: each participant processes its
own CSV locally, clips values and adds Gaussian noise, then sends protected
aggregates to a central service. The service composes privacy spending and
computes global analytics without storing the source rows.

This is an academic demonstration, not a clinical or production privacy
certification. Privacy guarantees depend on correct parameters, threat
assumptions, and implementation review.
