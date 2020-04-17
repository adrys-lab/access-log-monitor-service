# Access Log Monitor Service - Adrian Rebollo

## Challenge

Create a simple console program that monitors HTTP traffic on your machine:

>- Display stats every 10s about the traffic during those 10s: the sections of the web site with the most hits, as well as interesting summary statistics on the traffic as a whole. A section is defined as being what's before the second '/' in the resource section of the log line. For example, the section for "/pages/create" is "/pages"
>- Make sure a user can keep the app running and monitor the log file continuously
>- Whenever total traffic for the past 2 minutes exceeds a certain number on average, print or display a message saying that “High traffic generated an alert - hits = {value}, triggered at {time}”. The default threshold should be 10 requests per second, and should be overridable
>- Whenever the total traffic drops again below that value on average for the past 2 minutes, print or display another message detailing when the alert recovered
>- Write a test for the alerting logic
>- Explain how you’d improve on this application design

## Pre-Requisites
* Java 11
* Docker
* Maven3

## Api Tech Stack and libraries
* Used Spring Boot (without WEB context -> `web-application-type: NONE`)
* AMQ for Internal Message Broker with Apache Camel.
* Lombok
* Spring statemachine for Alert stat Machine Pattern (explained later)
* Apache Commons (`io` for file pulling/reading, and `lang3`)
* Flyway for versioned Database Migrations
* Test containers and awaitility for integration tests
    * see its usage at: [here](./app/src/test/java/com/adrian/rebollo/config/ContainerEnvironment.java)

## API environments
* default Profile environment provided
* I configured `-dev` one for integration tests.

## Restrictions and Decisions
* Decided to run the application out of Docker environment
    * since it has to deal with system/local files (out of docker container), so it can work then with machine log file.
* First ideas I had for develop the service was to send the configuration arguments (such as threshold, log file path, stats report (default 10sec), alert time window (default 120sec)...) via JVM arguments, but finally they are in `application.yml` so they are configurable.
    * if they want to be override they should be replaced (`application.yml`) there and re-run the application. 
* Decided to NOT make use of any Optimistic Locking (or any other DDBB Row versioning strategy) for Database Persistence:
    * The type of data does not offer data collisions since there are not updates (just inserts), but also because a Log Access will differ always.
        * So not care in concurrent accesses or concurrent data modifications.
    * The only existing CONSTRAINT is for the full Log Line string (which is persisted in the `line` Database Column entirely too, and is not allowed the same exact log line).
* Firstly I thought about keep logs and stats in memory (such with some Blocking Queue), but finally decided to persist it into DDBB (logs and stats).
    * It makes easier to debug if such problem raises.
    * Also make the JVM and service not increase in java heap memory usage.
        * so I preferred to store in database instead of keep in JVM java heap memory.
        * Side-Effect of this decision is that it makes the process slower cause we have to trigger/query/insert data instead of getting/having it from the JVM.
        * but it has a lot of other benefits like keep historical and data integrity, also allows to process that data after any system/service shutdown.
            * I found more production-ready to have that persistence layer than keeping objects in memory.
* I decided to make the output/display of the service stats and alerts, via Log and/or Json file (more details below).
* I decided to make 3 types of Alert:
    * `HIGH TRAFFIC`
    * `RECOVER`
    * `NO ALERT`
        * It's transition would be:
            * `NO ALERT` -> `HIGH TRAFFIC` -> `RECOVER` -> `NO ALERT`
        * That's because Alerts are dispatched to be displayed every Xseconds (configurable report stats delay). 
        * So to allow the user to know when there is or no an Alert, i have  created a NO_ALERT type one. Which informs the user there is no alert (from, for example, a previous recover.)
            * with a UI, this would not be needed, cause when NO_ALERT is dispatched, we could simply remove the alert from the UI.
            * Another approach could be to constantly clean/remove the LOG and Json content and display just the last Xseconds of stats.
                * But in this assessment solution, the historic of both logs/json are kept, so we can see the transitions visually.

## API Design
* Maven Multi-Module
* Followed SOLID and KISS principles as well as TDD.
* I decided to make use of an Hexagonal architecture with following modules:
    * `app`
        * Is the service Wrapper which encapsulates the whole inter-dependencies and packages them (jar). 
        * It contains Integration tests too.
    * `domain`
        * Contains the domain business data 
        * Entities (persisted) 
        * data for wide-platform acknowledgment.
        * This module is imported by domain-services. 
    * `domain-service`
        * Contains all the services and business-logic which involves our main domain logic. 
        * It contains the DTOs too.
        * Log Parsers - Services - Exposed/Abstraction interfaces implemented around the platform.
        * This module is imported by adapters.
    * `primary-adapters`
        * Contains the adapters which dispatch/route data inside the service/application.
        * If this service would have any exposed REST layer it would be placed here (incoming-requests).
        * Basically handles the incoming requests or the intra-service requests.
        * In this service, there's just the `activemq` primary-adapter, which Dispatches and Routes internal Messages around the platform to be consumed by its subscribers (Event-Driven).
    * `secondary-adapters`
        * Composed by the interfaces which interact with external (out-of-service) platforms (Databases, 3rd parties, external Providers|Interfaces, Outgoing requests...)
        * In this service, there are:
            * `mariadb`
                * it handles the interaction with the Database.
            * `log` and `json`
                * Notifies the end-user about stats and alerts
                * Dispatching to output json and log files (explained below).  
* Notice it misses the 6th layer which would be the API, but we don't need to expose it as this service is completely background and not interacting with 3rd parties.    
* I decided to parse the Log File using the commons-io library which provides a Tailer object listener for     
* I decided to use Event-Driven approach with AMQ for internal Message Broker Consumption.     
* The entire Service and Database are configured to run with same Timezone (Europe/Paris) to avoid DateTime Zones mismatches.      
* There is a global exception handler configured via AOP for handling any occurred exception within the service.
    * [here](./app/src/main/java/com/adrian/rebollo/GlobalExceptionHandler.java) to see that Handler logic.
    
## Service decisions
* First step in the services is to handle Access input log file (`/tmp/access.log`) changes by pulling lines with Tailer (`commons-io`, explained below).
* These new Lines are parsed and dispatched to the AMQ.
* Therefore each LogLine is Routed to LogLineService, to be Handled to save into Database.
* Independently, it comes up the Schedulers logic for Log Stats and Log Alerts.
* Decided to use Schedulers to trigger the reports for:
    * Stats: 
        * see [here](./domain-services/src/main/java/com/adrian/rebollo/service/HttpAccessLogStatsServiceImpl.java) to see that scheduler logic.
        * Triggered each 10sec (initial delay of 10sec)
        * it aggregates Log Lines from last 10sec.
            * between scheduler trigger executions, it uses the sequential number from Database to get log ranges:
                * 1st - `seqId` > 0 (max seqId returned 450 for example)
                * 2nd - `seqId` > 450 (max seqId returned 830 for example)
                * 3rd - `seqId` > 830 ...
            * This scheduler process aggregate all the Log lines, and the final object is a Log Line Stats.
            * This process save the Log Line Stats in Database with a dateTime, and afterwards dispatches (`AMQ`) that Log Line Stats, which will be Routed to LogService, which displays/logs the data for the end-user.
    * Alerts: 
        * see [here](./domain-services/src/main/java/com/adrian/rebollo/service/HttpAccessLogAlertServiceImpl.java) to see that scheduler logic.
        * Triggered each 10sec (initial delay of 12sec, guarantees delay between stats and alerts reports)
        * it aggregates the Log Stats (persisted by the above scheduler) added during the last 120sec.
            * Between scheduler trigger executions, it uses a `time window` config param (default 120sec):
                * 1st - Log stats with `insertedTime` Between 10:18:00 and 10:20:00
                * 2nd - Log stats with `insertedTime` Between 10:18:10 and 10:20:10
                * 3rd - Log stats with `insertedTime` Between 10:18:20 and 10:20:20 ...
        * It computes the total requests triggered during that `time-window`
        * checks if the number requests per second is higher than threshold
        * it uses The Machine State Pattern to check in which Alert state the service is currently running:
            * see [here](./domain-services/src/main/java/com/adrian/rebollo/StateMachineConfiguration.java) to see that Alert Machine State Pattern.
* Finally both Stats and Alerts are dispatched through AMQ
    * Observer Pattern used to that dispatching
        * see [here](./domain-services/src/main/java/com/adrian/rebollo/service/ExternalDispatcherObserverImpl.java) the logic details. 
        * it `notifies` to all the ExternalDispatcher interface implementations (`LogExternalDispatcher` and `JsonExternalDispatcher`) which are responsible to write the output data.
    * Alerts and Stats are shown/displayed to the end-user by a log and/or json file:
        * `./logs/access-log-monitor-service.log`
            * configured with `./app/src/main/resources/logback-spring.xml`
        * `./json/access-log-monitor-service.json`
            * json file path and name is configurable via:
                * `${adapters.json.file-name}`
        * Both outputs are configurable to be enabled or not (explained below).
        * Firstly i thought to publish the metrics through spring-actuator, which micrometer acts as facade for several implementations (included datadog)
            * but i decided to not do that, as i felt a bit weird providing an assessment solution using the same tool that the interviewer (datadog) works with.
* More detailed architecture-overview and service-logic below.

## Configurable properties

* Most important business and logic configurable properties are:

```
service:
    reader:
          file-name: /tmp/access.log                        --> defines the input access log file.
    schedulers:
        alert:
          enabled: true                                     --> MILLIS - enables the alerts or not. If set to false, there will be no Alerts displays in the log/json
          delay: 10000                                      --> MILLIS - defines how often the alerts are dispatched (to be displayed).
          initial-delay: 12000                              --> MILLIS - the initial delay for the alerts. set to 12sec
          time-window: 120                                  --> SECONDS - defines the time window/range on which the alerts will be computed 
          threshold: 10                                     --> defines what is the threshold to determine/compute if there is hightraffic/recover or not.
        stats:
          enabled: true                                     --> enables the stats or not. If set to false, there will be no Stats displays in the log/json
          delay: 10000                                      --> MILLIS - defines how often the stats are dispatched (to be displayed)
adapters:
    log:
        enabled: true                                       --> enables the output in LOG format. if set to false, stats and alerts will not be dispatched/displayed to the LOG format.
    json:
        enabled: true                                       --> enables the output in JSON format. if set to false, stats and alerts will not be dispatched/displayed to the JSON format.
        file-name: './json/access-log-monitor-service.json' --> determines the ouput JSON filename/path.
```

## Model decisions
* Just the LogLines and LogStats are persisted, no need for persist Alerts (also thought to do it but not much benefit on it, just for debugging and historic purposes maybe).
* Decided to make UNIQUE CONSTRAINT in LogLine for the full Log Line -> no exactly/identical log lines are allowed.
* Sequence ID is autogenerated in the DDBB side
    * This allows to not consider a new Entity until its definetely persisted (so not considering log lines that are still in Hibernate-proxy memory, cause Sequence ID is generated on the pre-commit phase).
* Configured Database Index for Log Stats, column `insert_time`, to speed-up searches with dates between (used by `HttpAccessLogAlertServiceImpl` which retrieve Stats by `insert_time` between).
* Data is retrieved from Database Paginated and with Chunk to not overload the DataBase neither Java Heap.
* The Log Line data look like:

```
line,
host,
identifier,
user,
dateTime,
httpMethod,
resource,
protocol,
returnedStatus,
contentSize
```

* the Log Stats data statistics look like:
```
start, --> stats start date range
end, --> stats end date range
totalRequest,
validRequests,
invalidRequests,
totalContent,
"topvisitsByMethod, --> 10 Max/Top
"topValidVisitedRequestsSections, --> 10 Max/Top
"topInvalidVisitedRequestsSections, --> 10 Max/Top
"topVisitsByHost, --> 10 Max/Top
"topVisitsByUser, --> 10 Max/Top
"topVisitsSection --> 10 Max/Top
```

## Design decisions
* After execute some Load Tests, i figured out that the service was not fast enough when ingesting thousands of log lines.
* That's why i decided to start using Parallel Log Parsing and Persist (through ThreadPoolTaskExecutor). to speed up the parsing and persist of logs.
    * see ThreadPoolTaskExecutors Configs [here](./domain-services/src/main/java/com/adrian/rebollo/DomainServicesConfig.java) for Log Parsing logic.
        * Parallel Log Line Parsing and Dispatching [here](./domain-services/src/main/java/com/adrian/rebollo/reader/CustomTailerListener.java).
    * see ThreadPoolTaskExecutors Configs [here](./primary-adapters/activemq/src/main/java/com/adrian/rebollo/AmqConfig.java) for Log Persisting logic.
        * Parallel Log Line Routing/Persisting at [here](./primary-adapters/activemq/src/main/java/com/adrian/rebollo/route/HttpAccessLogLineRoute.java).
* This involves also to properly configure AMQ to allow parallel consumers processing the messages.
    * see AMQ concurrent consumption Config [here](./primary-adapters/activemq/src/main/java/com/adrian/rebollo/AmqConfig.java).
    * Only needed concurrent consumption for Log Lines. Cause they are dispatched in parallel by `CustomTailerListener`.
        * see that parallel message consumption [here](./primary-adapters/activemq/src/main/java/com/adrian/rebollo/route/HttpAccessLogLineRoute.java).
    * Log Stats and Log Alerts are not dispatched in multi-thread behaviour.
        * There is no need for it, they are dispatched every 10sec, so there's not concurrent scenario here.

## High Level Architecture

![Architecture](./pic/high-level-architecture.png?raw=false "Architecture")

* TimeLine order:

    * The Yellow side is the Asynchronous and Multi-Threading input access log parsing and dispatching to AMQ.
    * The Green side is the Multi-Threading Routing and Persisting for each log line into DataBase.
    * The Red side is the schedulers logic part
        * Where log lines are retrieved (from DataBase), computed and Log Stats are created, to be saved into DataBase.
        * Where Those Log Stats are retrieved (from DataBase) and computed to create the Alerts.
        * Also both Stats and Alerts are dispatched to AMQ.
    * The Purple side Routes Stats and Alerts to the ExternalDispatchers implementations, which currently are Log and Json. 

## Low Level Architecture

![Schedulers](./pic/schedulers-timeline.png?raw=false "Schedulers")

## Other decisions taken
* Decided to not offer UI for alerts and stats cause i dedicated much time in the current solution (also thought the UI side was not the main purpose of the assesment.)
    * Therefore i decided to offer the output stats through a Log and Json files (I know they are not much user-friendly).    

## Output
* As mentioned above, the output of the service stats and alerts, are displayed via log and json file.
    * Log:
        * `./logs/access-log-monitor-service.log`
        * configured via `lopgback-spring.xml`
    * json:
        * `./json/access-log-monitor-service.json`
* As mentioned `Configurable properties`, they can independently be enabled or not via Configuration.

## Improvements
* Definetely add a UI to show on a better user-friendly way the results
* Scale up the application in a multi-jvm environment (Dockerized and Kubernetes Infrastructure)
    * with several service instances nodes, we could apply a distributed lock (DynamoDB Lock) to avoid read same loglines/stats from several nodes.
* Accept more Log Formats cause currently only Apache Common is allowed.
* Thinking on how to properly inform the end-user about the traffic, an Slack webhook could be configured too to publish ALERT (HIGH TRAFFIC, RECOVERED) events (others like, create a JIRA ticket, send email....etc)
* Provide proper DASHBOARDS visualizations with Timeline Traffic and Alerts stats.
* Allow the end-user to configure and tag the logs monitoring alerts and stats to bring a more grained and adaptable details.
* Allow the end-user to group the data.
* Add machine learning for alert automatic detection and anomalies detection. 
* Provide external Webhooks where to trigger alerts and stats to integrate with external 3rd party's API's.  
* Configure Sentry for Error Reporting

## Unit Testing
* Code Developed using TDD No adding new logics without being tested.
* Added Unit Tests for critical logic parts.
* Added Jacoco Plugin Configuration for COVERAGE and INSTRUCTIONS to keep code-quality and minnimum acceptance criteria.
* Added Maven plugin configuration to run or not the Integration Tests  `-DskipITTests=false` o `-DskipITTests=true` (default true)
* Total tests coverage:

![Coverage](./pic/coverage.png?raw=false "Coverage")

### Integration Testing
* located [here](app/src/test/java/com/adrian/rebollo).
* Ensured whole process by load logs ingestion and end-to-end testing (from log ingestion to Alerts creation.)

### Load Tests
* Executed Load tests with FLOG -> https://github.com/mingrammer/flog
* Thanks to this tool i saw some hot-spots during log processing and that's why i decided to start using Multi-Thread/Parallel Log Parsing and Persist (through ThreadPoolTaskExecutor).

## Build

### Compile with Maven
* `mvn clean compile -P[<empty>|<dev>`

### Package Artifact with Maven
* `mvn clean package -P[<empty>|<dev>`

### Install Artifacts with Maven
* `mvn clean install -P[<empty>|<dev>`

### Unit Test with Maven
* `mvn test -P[<empty>|<dev> {-Dskip.unit.tests=false}`

### Integration Tests with Maven
* `mvn test -P[<empty>|<dev> -DskipITTests=false {-Dskip.unit.tests=false}`
* `mvn clean verify -DskipITTests=false -Dskip.unit.tests=true|false`

### Run API with Java
* `java -jar ./app/target/access-log-monitor-service.jar --spring.profiles.active=[<empty>|<dev>`
* `java -jar ./app/target/access-log-monitor-service.jar`


### Docker container bootstrapping
* mariadb volume is created to not loose data between deployments/executions.
* build/run container dependencies and build/run project

* `mvn clean package -P[<empty>|<dev>`
* `docker-compose build`
* `docker-compose up`
* `java -jar ./app/target/access-log-monitor-service.jar`

* I have provided a Dockerfile in case of wanted to execute within docker environment too.

## Usage and How to Test end-to-end
* I found a useful tool to generate fake http access logs, called FLOG -> https://github.com/mingrammer/flog
* tested locally with `flog -f apache_common -o /var/log/access.log -t log -n 4000 -w` generating 4000 log lines.
    *  step 1 - package the service with `mvn clean package -DskipITTests=true -DskipTests=true`
    *  step 2 - start docker dependencies with `docker-compose up`
    *  step 3 - execute the service such as with IntelliJ or open a terminal and executing `java -jar ./app/target/access-log-monitor-service.jar`
    *  step 4 - open a terminal and execute `flog -f apache_common -o /var/log/access.log -t log -n 4000 -w` for generate log files. it should respond `/var/log/access.log is created.`
    *  step 5 - go to see the stats/alerts generated log at: `./logs/access-log-monitor-service.log`
    *  step 6 - go to see the stats/alerts generated json at: `,/json/access-log-monitor-service.json`
    *  step 7 - see how the alerts are being displayed and at some point it will reach HIGH TRAFFIC, after, descending to RECOVER and finally NO ALERT.
* I provided a bundled `jar` already just in case too.

## Integrations
* Added Travis CI integration config file at:
    * `./.travis.yml`
* Configured pipeline with several stages

## Stats appearance:
```
requests=517,
validRequests=122,
invalidRequests=369,
totalContent=7245704,
"topvisitsByMethod="{
   HEAD=97,
   DELETE=85,
   POST=91,
   GET=77,
   PATCH=90,
   PUT=77
},
"topValidVisitedRequestsSections="{
   /grow=3,
   /web-enabled=2,
   /portals=2,
   /incubate=2,
   /maximize=2,
   /repurpose=3,
   /bricks-and-clicks=2,
   /frictionless=2,
   /distributed=3,
   /one-to-one=3
},
"topInvalidVisitedRequestsSections="{
   /e-services=7,
   /holistic=9,
   /productize=5,
   /e-markets=5,
   /frictionless=5,
   /exploit=5,
   /wireless=5,
   /out-of-the-box=7,
   /synergistic=8,
   /vertical=6
},
"topVisitsByHost="{
   101.126.111.41=1,
   47.136.3.8=1,
   38.224.176.249=1,
   97.50.210.136=1,
   93.64.231.97=1,
   58.119.10.17=1,
   208.28.247.50=1,
   136.225.54.116=1,
   123.36.125.29=1,
   122.216.227.158=1
},
"topVisitsByUser="{
   NO_USER=262,
   steuber3327=1,
   adams2248=1,
   kerluke4166=1,
   bernhard2143=1,
   kiehn5401=1,
   mills1457=1,
   bartell3571=1,
   lemke7636=1,
   hills1671=1
},
"topVisitsSection="{
   /holistic=10,
   /synergistic=9,
   /e-services=8,
   /out-of-the-box=8,
   /frictionless=7,
   /world-class=7,
   /one-to-one=7,
   /distributed=6,
   /wireless=6,
   /enable=6
})
```

## Alert appearance:
```
2020-04-03 09:12:45,175 INFO ALERT - HIGH TRAFFIC generated an alert - hits = {1035}, hits/s = {35.0}, triggered at {2020-04-03T09:12:45.169960} , with stats from {2020-04-03T09:12:15.160481}, to {2020-04-03T09:12:45.160481}
2020-04-03 09:12:35,153 INFO ALERT - HIGH TRAFFIC generated an alert - hits = {847}, hits/s = {28.0}, triggered at {2020-04-03T09:12:35.146784} , with stats from {2020-04-03T09:12:05.134119}, to {2020-04-03T09:12:35.134119}
2020-04-03 09:12:55,192 INFO ALERT - HIGH TRAFFIC generated an alert - hits = {763}, hits/s = {25.0}, triggered at {2020-04-03T09:12:55.189020} , with stats from {2020-04-03T09:12:25.181767}, to {2020-04-03T09:12:55.181767}
2020-04-03 09:13:15,217 INFO ALERT - RECOVERED from a previous HIGH TRAFFIC alert - hits = {0}, hits/s = {0.0}, triggered at {2020-04-03T09:13:15.212400}, with stats from {2020-04-03T09:12:45.206831}, to {2020-04-03T09:13:15.206831}
2020-04-03 09:13:25,230 INFO ALERT - NO ALERT status - hits = {0}, hits/s = {0.0}, triggered at {2020-04-03T09:13:25.226232}, with stats from {2020-04-03T09:12:55.219338}, to {2020-04-03T09:13:25.219338}
```

## Output access-log-monitor-service.log appearance:

* Example log output from a load test ingesting 4000 access log lines.

[here](./example-access-log-monitor-service.log)

## Output access-log-monitor-service.json appearance:

* Example json output from a load test ingesting 4000 access log lines.

[here](./example-access-log-monitor-service.json)