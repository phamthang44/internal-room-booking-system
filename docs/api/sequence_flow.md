```
sequenceDiagram
    participant Admin as Admin/Staff
    participant App as Spring Boot Service
    participant MQ as RabbitMQ (The Post Office)
    participant DB as PostgreSQL (The Mailbox)
    participant WS as WebSocket (The Phone Call)
    participant Student as Student's Browser

    Admin->>App: Approve/Reject Booking
    App-->>App: Fire Internal Event (BookingStatusChanged)
    
    Note over App: "Postman" picks up the event
    App->>MQ: Send In-App Notification Message
    
    Note over MQ: Message waits in the "In-App" Queue
    
    MQ->>App: Consumer picks up message
    App->>DB: Save Notification record (is_read = false)
    App->>WS: Push real-time data to /topic/notifications/{userId}
    
    WS-->>Student: Popup Toast Notification (Right Now!)
    
    Note over Student: Later, Student opens Bell Icon
    Student->>App: GET /api/v1/notifications
    App->>DB: Fetch unread history
    DB-->>Student: Display list of past notifications

```
1. The Trigger (Action)
When you (Admin) click Approve or Reject, the system finishes its database work and then shouts internally: "Hey, this booking just changed status!" (This is the Spring Event).

2. The Post Office (RabbitMQ)
Instead of making the Admin wait while we do notification work, we throw that "shout" into RabbitMQ. RabbitMQ acts like a post office—it stores the intended message safely in a queue so the main system can move on immediately.

3. The Worker (Consumer)
We have a "Worker" (the InAppNotificationConsumer) that sits and waits for new mail. When a message arrives from RabbitMQ:

It writes to the DB: It puts a physical record in the notifications table. This is the Bell Icon data. Even if the student is offline, it will be there when they return.
It makes a phone call: It checks if the student is currently online. If they are, it uses WebSockets to "call" their browser and show that popup toast instantly.
4. The Bell Icon (History)
When the student returns later and clicks the Bell Icon, their browser asks the server: "Give me everything in my 'Mailbox' (DB)". The server reads from the notifications table and sends back the list of what happened while they were away.

Why is this good?
Speed: The Admin doesn't wait for notifications to be saved or sent.
Reliability: If the WebSocket fails or the user is offline, the record is still safe in the Database.
Scale: RabbitMQ can handle thousands of notifications per second without slowing down the booking process.
