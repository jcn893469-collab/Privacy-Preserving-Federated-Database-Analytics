# Screenshot Evidence Checklist

Capture each item from the real running application or PostgreSQL client. Keep
timestamps/context visible where useful, redact credentials and sensitive
records, and do not fabricate results.

- [ ] 1. Login page and a genuine successful login.
- [ ] 2. Admin dashboard.
- [ ] 3. Data Owner dashboard.
- [ ] 4. Researcher dashboard at `/researcher-dashboard.html`, showing only federated summary and runs.
- [ ] 5. Organization registration (current UI/API maps this to federated database registration).
- [ ] 6. Federated database registration.
- [ ] 7. Dataset metadata CRUD and column schema CRUD, including create/update/delete responses.
- [ ] 8. CSV selection and local upload/processing flow; show no raw rows sent to the server.
- [ ] 9. Analytics execution.
- [ ] 10. Privacy budget processing and ledger entry.
- [ ] 11. Analytics results.
- [ ] 12. CRUD execution, showing the actual operation and response.
- [ ] 13. SQL INNER/LEFT/multi-table JOIN results.
- [ ] 14. Aggregate query with GROUP BY/HAVING results.
- [ ] 15. Database view query results.
- [ ] 16. Stored procedure `CALL` output.
- [ ] 17. Trigger-generated audit rows for INSERT, UPDATE, and DELETE.
- [ ] 18. Role-based authorization: allowed and forbidden API results.
- [ ] 19. Successful database backup command and generated archive.
- [ ] 20. Restore confirmation and post-restore verification query.
- [ ] 21. Docker Compose containers and health status.
- [ ] 22. Responsive owner and researcher dashboards at a narrow viewport and desktop width.

Items 9-10, 13-17, 19-21 require real database/container execution; none is
claimed as captured by this repository update.
