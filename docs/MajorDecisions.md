# Major Decisions

## Branching Strategy (or lack thereof)

I intentionally didn't use any branching strategy for this exercise. All commits were made directly to the `master` branch. Creating branches and pull requests for each change/step would add extra effort that I felt would not provide much value - I am the only person working on this.

## Pre Validation Batch Job

TODO: Better explain thought process

- The purpose of this job is simply to alert admins of any obviously invalid invoices early. That way they can resolve them as they come in (possibly weeks before they are due), instead of having to deal with all of them at the same time.
    - Note: The same validations are done again before trying to pay the invoice
- This pre-validation job does not set up invoices to be retried. If the invoice fails this validation the `nextRetry` field remains null.

## Error Handling

TODO: Cleanup notes

- Log any errors with the invoice ID, that way logs can be used to determine why the invoice failed
- All errors trigger a notification to an admin
- Notifications are tied to an invoice ID
 
- "CustomerNotFoundException"
    - Should be detected at the validation stage
    - Status set to "failed"
    - Not automatically retried
        - Assumption being that this requires a manual change by the admin. A manual retry can be done later.
- "CurrencyMismatchException"
    - Should be detected at the validation stage
    - Status set to "failed"
    - Not automatically retried
        - Assumption being that this requires a manual change by the admin to correct the issue. Once that is done, the admin can trigger this invoice again.
- "NetworkException"
    - Mark invoice as "failed"
    - Set next retry time to +1 hour
- Missing funds
    - Status set to "failed"
    - Notification sent to admin and account owner
    - Automatic retry a day later
