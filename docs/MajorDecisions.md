# Major Decisions

## InvoiceStatus
I added a few new InvoiceStatus values. Their meaning is as follows:
- PENDING: Invoice has just arrived in the system and hasn't been touched yet
- READY: Invoice has been through the pre-validation process and passed
- PROCESSING: Invoice is currency in the process of being paid
- PAID: Invoice has been successfully paid
- FAILED: This invoice has failed either pre-validation or payment

## Notification Service
I added a notification service to handle notifying a system administrator and/or the account owner of any errors.

This notification service is just an interface. The idea is that in a real system it would integrate with some external system to queue and send messages.

## Job Scheduling
I set up a few recurring background jobs to accomplish the purpose of validating and paying invoices. I used [Quartz](https://github.com/quartz-scheduler/quartz) to create and run these jobs.

I created a separate project to handle all code related to batch jobs. I felt it made more sense as its own thing instead of merged in with one of the other projects.

I chose the Quartz library because it is the one I am most familiar with and has a rich set of features.

## Invoice Creation TIme
For the purpose of making sure invoices don't get charged before they are due, I added a `creationTime` field to each invoice. This gets set when the invoice is first saved to the database. The idea is that invoices can only be processed and paid when their creation time is in the previous month.

I implemented checks in the billing service to ensure that invoices that aren't yet due cannot be paid.

## Pre Validation Batch Job
I made the assumption that a couple of the errors that come up when paying invoices could be detected earlier in the process. With that in mind I created a pre-validation job that runs every working day at 09:00. This job will process all "PENDING" invoices and check that:
1. The account on the invoice exists
2. The currency on the invoice matches the currency on the customer

If both these conditions are true, the invoice is put in the "READY" state. Otherwise, it is flagged as "FAILED" and the admin is notified.

I figured this would allow for system admins to detect these failures early and resolved them well before the time the invoice is charged. Hopefully reducing the amount of failed invoices that get generated at the end of the month.

## Retrying Payments
In certain cases, invoices that fail to be charged will be retried automatically. I decided to set this up as follows:
- Network Errors: Can be retried after 1 hour
- Not enough funds errors: Can be retried after 1 day

All other errors are not automatically retried.

The retry mechanism works with a `retryPaymentTime` field on the invoice. This field specifies the time after which the invoice can be retried. If the field is not set, the invoice will not be automatically retried.

A recurring background job runs every hour to find any invoices that can be retried and tries to process them.

## Avoiding Duplicate Payments
I assume this system is the system-of-record for invoices and their state. So a big concern I had while working on this was trying to make sure that a given invoice would never be charged twice. I could have made the assumption that the payment service would be able to detect duplicate charges and prevent them, but I wanted to prevent them in this application tool.

The first thing I did was add validations before the invoice is processed to ensure it is not already in the PAID state. There are also several tests to ensure this and to ensure that the invoice state gets set correctly after successful payment.

The second, more challenging, issue was protecting the system against errors while storing the new invoice state after the invoice is paid. For example:
1. Invoice loaded from DB
2. Invoice sent to payment system (payment successful)
3. Save the invoice back to DB (with new PAID state)
4. DB write fails

In the above scenario, the invoice would remain in a PENDING state in the database. The next time the payment job runs, it will pick up this invoice again and try to charge it. If we assume that the payment system doesn't check for duplicates, this will charge the account twice for the same invoice - not good.

My solution was to set the invoice in a "PROCESSING" state before sending it over to the payment service. This way either the write fails before the invoice is charged (and the process exits), or the write fails after the invoice is chaged, and the invoice gets stuck in a PROCESSING state.

The payment retry job will check for any invoices that have been in the PROCESSING state for at least an hour and flip them over to FAILED. A system admin will have to manually review these to determine if the invoice was actually charged or not. If it was, the invoice can be manually put in the PAID status.

## Preventing Concurrent Jobs
One concern I had as soon as I added the second scheduled job was how they would interact if they both happened to run at the same time. Since there is generally some cross-over in terms of which invoices each job deals with, there is a risk that both jobs will try to change the same invoice at the same time - leading to a race condition.

The solution I came up with isn't great, but gets the job done. The idea is that all the jobs run their code in a synchronized function. This way they all take turns and don't run in parallel. Ideally, this would be done using for example a database lock, to allow jobs to run in parallel while they are working on different invoices.

There is also still the risk that invoices might be modified by the REST API while also being modified by the batch job.

## Testing Database Functions
I added some tests for the DAL (Database Access Layer) in the `pleo-antaeus-app` project. Since these tests are testing methods that come from the `pleo-antaeus-data` project, they would ideally go in the `data` project, to keep them closer to the code they are testing. However, I wrote them in the `app` project because it's the only place that has a real database.

I considered using mocking to test these functions, but I decided that would defeat the purpose of the tests. With these tests I want to ensure that the code interacts with the database correctly. Using a mock would only ensure that those functions work correctly with a given mock implementation, not the a database.

Another option would be to copy the code creating and initializing the database into the test package of the `data` project. I decided against that because I wanted to maintain the existing structure of keeping the *abstract* database code (in the `data` project) apart from the *actual* database creation (in the `app` project).

## Updating Kotlin/Javalin
I upgraded Kotlin and Javalin to the latest version to be able to use the `jsonMapper` function in the Javalin config. I did this simply to have joda's DateTime objects encoded to JSON as a nicer type. By default, these values get encoded as regular objects, which makes them much harder to process by the client.

For example, a default encoding of a `DateTime` instance:

```json
"creationTime":{"dayOfWeek":4,"dayOfYear":41,"era":1,"year":2022,"dayOfMonth":10,"weekyear":2022,"millisOfSecond":243,"millisOfDay":80566243,"weekOfWeekyear":6,"yearOfEra":2022,"yearOfCentury":22,"centuryOfEra":20,"secondOfDay":80566,"minuteOfDay":1342,"monthOfYear":2,"hourOfDay":22,"minuteOfHour":22,"secondOfMinute":46,"zone":{"fixed":true,"id":"Etc/UTC"},"millis":1644531766243,"chronology":{"zone":{"fixed":true,"id":"Etc/UTC"}},"afterNow":false,"beforeNow":true,"equalNow":false}
```

## Branching Strategy (or lack thereof)

I intentionally didn't use any branching strategy for this exercise. All commits were made directly to the `master` branch. Creating branches and pull requests for each change/step would add extra effort that I felt would not provide much value - I am the only person working on this.
