# Taskforce App

## Requirements

The task is to write a REST API for the time logging system. 
### Functional requirements:
1. The system can be used by logged in users. Users are assumed to be system authenticated with a JWT that includes a user ID in UUID format. No registration or login mechanism is required, a only authentication. 
2. The system allows each user to create a new project. Project consists of the: 
   * Unique identifier of the project in text form, given by user. 
   * Timestamp of project creation for accounting purposes. 
3. The system enables the author of the project to change the project ID. 
4. The system allows each user to log the time spent on project (hereinafter referred to as a task). The task consists of: 
   * Project start time stamp.
   * The duration of the task.
   * Optional: volume expressed as a natural number.
   * Optional: comment. 
5. The given user cannot save two tasks which overlap in time according to the attributes of these tasks.
6. The system allows the author of the task to delete it, but it must be a "soft delete" timestamped with removal.

7. The system allows the author of the task to change all of the above-mentioned task attributes while validating from point 5. This should be done as deleting a task (as in point 6) and creating a new one in its place. 
8. The system allows the author of the project to delete it, but it must be a "soft delete" timestamped with removal. All existing deleted tasks the project is considered deleted with the same timestamp. 
9. The system allows you to return information about a given project together with the associated ones tasks and the total duration of the project.
10. The system allows you to list projects together with the associated tasks from using conjugation of the following filters (each is optional):
    * list of identifiers, 
    * from - creation timestamp, 
    * to - creation timestamp, 
    * removed / not removed,

and sort using at most one of the following criteria:
   * Creation timestamp - descending / ascending, 
   * update timestamp - the latest added task (in case of an empty project - the creation date should be taken) - descending / ascending,

and simple pagination based on size and page number. 

11. The system allows you to display the following statistics (they only count for the statistics unremoved projects and tasks):
    * Total number of tasks.
    * Average duration of the task.
    * Average volume of the job (for jobs with the specified volume).
    * Volume weighted average task duration (for tasks from given volume).

Access to statistics should be parameterized with a list of identifiers users whose tasks should be included and the from-to dates in the form year-month.


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
