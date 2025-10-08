@SouthboundQueuerSteps
Feature: ensure the southbound queuer preprocesses its waiting queue correctly

  Scenario: Make sure duplicate tasks are not added to the queue
    Given I clear waiting and running queues
    When I submit multiple duplicate tasks to the queue preprocesor
    Then the processed queue does not have duplicate tasks for the same connection id

  Scenario: Make sure tasks already in the running queue are not added to the queue
    Given I clear waiting and running queues
    When I submit a task that is already in the running queue to the queue preprocesor
    Then the processed queue does not contain any tasks that are in the running queue

  Scenario: Make sure dismantle tasks at the head of the queue
    Given I clear waiting and running queues
    When I submit a mix of tasks to the queue preprocesor
    Then the processed queue does not have any non-dismantle tasks ahead of any dismantle task
