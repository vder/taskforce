# Taskforce App

## Usage

```shell
> docker-compose up
> . ./env.sh
> sbt
```

## Dockerize

```shell
> sbt 'Docker / publishLocal'
> cd app
> docker-compose up
```

## Available endpoints

* "http://{host}:{port}/api/v1/projects" /GET /POST
* "http://{host}:{port}/api/v1/projects/{projectId}" /GET /PUT
* "http://{host}:{port}/api/v1/projects/{projectId}/totalTime" /GET
* "http://{host}:{port}/api/v1/projects/{projectId}/tasks" /GET /POST
* "http://{host}:{port}/api/v1/projects/{projectId}/tasks/{taskId}" /GET /PUT
* "http://{host}:{port}/api/v1/filters" /GET /POST
* "http://{host}:{port}/api/v1/filters/{filterId}" /GET
* "http://{host}:{port}/api/v1/filters/{filterId}/data" /GET
* "http://{host}:{port}/api/v1/stats" /GET