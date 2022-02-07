# Major Decisions

## Branching Strategy (or lack thereof)

I intentionally didn't use any branching strategy for this exercise. All commits were made directly to the `master` branch. Creating branches and pull requests for each change/step would add extra effort that I felt would not provide much value - I am the only person working on this.

## Testing Database Functions

I added some tests for the DAL (Database Access Layer) in the `pleo-antaeus-app` project. Since these tests are testing methods that come from the `pleo-antaeus-data` project, they would ideally go in the `data` project, to keep them closer to the code they are testing. However, I wrote them in the `app` project because it's the only place that has a real database.

I considered using mocking to test these functions, but I decided that would defeat the purpose of the tests. With these tests I want to ensure that the code interacts with the database correctly. Using a mock would only ensure that those functions work correctly with a given mock implementation, not the a database.

Another option would be to copy the code creating and initializing the database into the test package of the `data` project. I decided against that because I wanted to maintain the existing structure of keeping the *abstract* database code (in the `data` project) apart from the *actual* database creation (in the `app` project).

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
