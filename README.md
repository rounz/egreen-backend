# eGreen Backend

This project contains a proof of concept showing how easy it is to make
Functional Programming, HTTP, CQRS, and eventsourcing on top of NoSQL
datastores work together. It is intended to serve as HTTP backend of a
web-app to sale fresh vegetables in real life.

This project is under development, but the architecture and a minimal
usecase are implemented with said technologies. The code base can be
used as a skeletal starting point for similar services.

The eGreen frontend is not hosted in this repository.

# Architecture

The platform is implemented as a HTTP service which handles requests
from the web frontend.

## HTTP API

HTTP layer is built on top of [http4s](https://http4s.org/).

The service exposes several endpoints:
- `GET /health` returns service health status.
- `GET /version` returns various information including the build version,
Scala version, etc.
- `POST /c` is the entry point of platform commands.

Instead of dedicating a specific endpoint for each request, the service
expects all commands posted to the `/c` endpoint for full-control of
request routing and error handling. The request body for a command is a
JSON object consisting of the command name and command payload. Command
payload is in turn another JSON object encoded as string value to be
parsed by the service's handler. For example, the request body of the
command to create a customer:

```json
{
  "commandName": "com.round.egreen.cqrs.command.CreateCustomer",
  "json": "{ \"username\": \"meof\", \"encryptedPassword\": \"abc\", \"fullName\": \"Jon Snow\", \"phoneNumber\": \"098767545321\", \"address\": \"australia\", \"district\": { \"GoVap\": {} } }"
}
```

## Eventsourcing and CQRS

The service uses [Redis](https://redis.io/) as the denormalized read-only
cache due to its super fast in-memory datastructure store. The platform
commands, after validation, are stored as a series of [protocol buffer](
https://developers.google.com/protocol-buffers/) events in [MongoDB](
https://www.mongodb.com/). Utilizing eventsourcing with protocol buffer
allows the platform to constantly grow and evolve without the burden of
migrating legacy data. When business models change, rebuilding database is
as simple as flushing Redis and streaming events from MongoDB.

The choice of MongoDB and Redis is mostly driven by the fact that they
are two free supported datastores offered by Heroku. Other than that,
Redis is an in-memory datastore without permanent persistence (limitation
is due to Heroku's free plan), which is suitable to be used as
denormalized read-side, while MongoDB offers key-value storage and a
[streaming driver](http://mongodb.github.io/mongo-scala-driver/) which
makes it easy to integrate with the streaming nature of `fs2` and `http4s`.

# Deployment

The service requires MongoDB and Redis to run. It's easy to have sandbox
MongoDB and Redis connection strings via Heroku when you register for a
free account and activate the respective add-ons.

## Run the service locally
Publish MongoDB and Redis connection strings:
```bash
export MONGODB_URI=CHANGE_ME
export REDIS_URL=CHANGE_ME
```
Change [mongodb.dbname](src/main/resources/application.conf) according
to your settings, then simply
```bash
sbt run
```
The service is served at `http://localhost:9000` by default.

## Deploy to Heroku

Register your Heroku account with MongoDB and Redis activated. Deployment
is done by pushing `master` branch to Heroku git repository.

[Heroku docs](https://devcenter.heroku.com/articles/git).

# Minimal usecase

## Bootstrap the platform

The very first user of the platform needs to be created using a special
username `egreen` with an arbitrary password. Please be informed that at
the current state, *user passwords are stored as plaintext*.

Create first user `egreen` with role `Developer` using a Postman example
from [docs](docs), or with CURL:
```bash
curl -X POST \
  http://CHANGE_ME:CHANGE_ME/c \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -d '{
    "commandName": "com.round.egreen.cqrs.command.CreateUser",
    "json": "{ \"username\": \"egreen\", \"encryptedPassword\": \"CHANGE_ME\", \"roles\": [ {\"Developer\": {} } ] }"
  }'
```
The service will return a JSON object together with an Athorization token
to use for further requests.

## Create users with role

There are three main roles that a user can have one in the platform:
`Developer`, `Admin`, and `Customer`. Most users have `Customer` role,
while a few others can be `Developer` or `Admin`. An `Admin` can create
new `Customer` users and then give the login information to their owners,
thus it requires human-human interaction. `Developer` can do pretty much
everything including creating new `Developer`s or new `Admin`s.

To create a new user, one will need an `Admin` or `Developer`
Authorization token obtained by logging into the platform (or from
creating special user `egreen`).

```bash
curl -X POST \
  http://CHANGE_ME:CHANGE_ME/c \
  -H 'Authorization: CHANGE_ME' \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -d '{
    "commandName": "com.round.egreen.cqrs.command.CreateUser",
    "json": "{ \"username\": \"CHANGE_ME\", \"encryptedPassword\": \"CHANGE_ME\", \"roles\": [ {\"Customer\": {} } ] }"
  }'
```
Several other platform commands have been implemented and documented as
a Postman collection in [docs](docs).
