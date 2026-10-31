# Project Overview

Privacy-Preserving Federated Database Analytics is a VIT BCSE307L project for
aggregating statistics from participating organizations without sending raw
CSV rows to the central service. Organizations process data locally and submit
privacy-protected aggregate contributions.

The existing application consists of a Java 21 / Spring Boot REST backend,
PostgreSQL database, static HTML/CSS/JavaScript frontend, Flyway migrations,
JWT authentication, Gaussian differential privacy, RDP accounting, and
anomaly detection.

The implementation and its verified gaps are described in the
[requirements gap analysis](./REQUIREMENTS_GAP_ANALYSIS.md). Database design
artifacts are maintained under [`../database/`](../database/).
