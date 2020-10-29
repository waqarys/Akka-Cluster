# Loyalty Service

This is a simple Akka Scala application for managing customer loyalty
points as part of the Reactive BBQ restaurant system. It is built using
Akka HTTP and Akka Cluster Sharding.

The restaurant includes a loyalty program that allows users to
accumulate points each time they order. Once they have accumulated
enough points they can deduct those points and cash them in for
discounts.

This is a skeleton of the Loyalty Service with a minimal feature set.
It allows us to award points to an account and deduct points from the
account. It also ensures that an account balance never goes below zero
(You can't deduct points that you don't have).

## Running the Application

Helper scripts have been included to allow you to run multiple
instances of the application. These scripts will take care of overriding
ports as required.

```
> runNode1.sh
> runNode2.sh
> runNode3.sh
```

These scripts override a variety of ports when running the application
to ensure there are no port conflicts. Some of those ports are in use
at the beginning, others will be enabled as you progress through the
exercises. Specifically we override:

- `akka.http.server.default-http-port` - Sets the port that the Akka 
  HTTP server is running on. This is the port you will use to interact
  with the application via the `loyalty.sh` script.
- `cinnamon.prometheus.http-server.port` - Sets the port that will be
  used to expose Telemetry for Prometheus. Prometheus is optional.
  Everything will work fine if you don't have it.
- `akka.remote.artery.canonical.port` - Sets the port that will be used
  by Akka Cluster. This will be enabled later on in the course.
- `akka.management.http.port` - Sets the port that will be used to
  inspect and manage your cluster through Akka Cluster HTTP Management.
  This will be enabled later on in the course.

## Using the Application

Once your application is running another helper script has been provided
to interact with the service (loyalty.sh). This script uses curl
commands to send HTTP requests to the service.

## Manual for loyalty.sh

Syntax

`loyalty.sh [option...] [award|deduct|retrieve] [VALUE]`

Operations

- `award` - Awards VALUE points to the provided account.
- `deduct` - Deducts VALUE points from the provided account.
- `retrieve` - Retrieves the information for the provided account.

Options

- `-a` <account> Assign an account Id (default=sample)
- `-p` <port> Use a specific port (default=8000)

## Award Loyalty Points

Loyalty points can be awarded with the following command:

```
> ./loyalty.sh award 10
```

This will award 10 points to the default account (sample) on the default port (8000).

If you want to specify the account and port you can use:

```
> ./loyalty.sh -a MyAccount -p 8001 award 10
```

This will award 10 points to the account MyAccount on port 8001.

## Deduct Loyalty Points

Loyalty points can be deducted with the following command:

```
> ./loyalty.sh deduct 10
```

This will deduct 10 points from the default account (sample) on the default port (8000).

If you want to specify the account and port you can use:

```
> ./loyalty.sh -a MyAccount -p 8001 deduct 10
```

This will deduct 10 points to the account MyAccount on port 8001.

Remember, you can't deduct points that you don't have.

## View Loyalty Account Details

To view the details for a Loyalty Account, you can use the following command:

```
> ./loyalty.sh retrieve
```

This will give you the details for the default account (sample) on the default port (8000).

If you want to specify the account and port you can use:

```
> ./loyalty.sh -a MyAccount -p 8001 retrieve
```

## Data Storage

The Loyalty Service stores data in a series of files. You will find
these files in the `exercises/tmp` directory.

Each account created will have it's own file. The file will contain a
series of comma separated adjustments to the account (positive numbers
award points, negative numbers deduct points).

You can clean up the data by deleting the `exercises/tmp` folder. Or
for convenience you can run the provided script:

```
> ./clearData.sh
```

## Running Tests

Tests can be run using the standard testing procedures for SBT:

```
> sbt test
```

## Classes

![Initial Application Structure](images/initial-application-structure.png)

### Main

This class is the application entrypoint. It constructs the other
application components, and injects them where needed.

### LoyaltyRoutes

This class defines the HTTP endpoints for the application. Endpoints are
included for awarding and deducting loyalty points, as well as
retrieving account information.

### LoyaltyInformation

This class defines the details about a loyalty account. It contains
a history of the account adjustments that have been made.

### LoyaltyActorSupervisor

This class creates and manages all the different LoyaltyActors. It will
be rendered obsolete in later exercises.

### LoyaltyActor

This class processes all the commands for the LoyaltyService including
awarding points, deducting points, and retrieving the
LoyaltyInformation for an account.

### LoyaltyRepository

This class acts as an interface to data storage. Currently there are two
implementations:

- InMemboryLoyaltyRepository
- FileBasedLoyaltyRepository

In a production application, you would likely provide an implementation
that is backed by a database instead. However for the purpose of this
course we will simply use the File Based version for most things. The
In Memory version is primarily used in tests.
