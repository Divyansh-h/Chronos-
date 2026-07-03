package com.taskflow

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class BasicSimulation extends Simulation {

  // 1. Configure the HTTP Protocol
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Engine")

  // 2. Define the User Scenario (Mimicking Frontend Client + Worker Node interactions)
  val scn = scenario("Frontend User Journey")
    
    // Step A: Load the initial Dashboard
    .exec(http("Load Dashboard")
      .get("/api/v1/workflows")
      .check(status.is(200))
      // Dynamically extract the first workflow ID from the JSON response and save it to the virtual user's session
      .check(jsonPath("$[0].id").optional.saveAs("extractedWorkflowId")))
    .pause(1) // Simulate human reading time (1 second)
    
    // Step B: Simulate the user clicking on the workflow in the UI (only if the DB wasn't empty)
    .doIf("${extractedWorkflowId.exists()}") {
      exec(http("View DAG Visualizer")
        .get("/api/v1/workflows/${extractedWorkflowId}")
        .check(status.is(200)))
    }
    .pause(2)

    // Step C: Simulate aggressive backend Worker Nodes polling the Redis Queue concurrently
    .exec(http("Worker Node Polling")
      .post("/api/v1/tasks/poll")
      .body(StringBody("""{"workerId": "gatling-performance-worker", "maxTasks": 1}"""))
      // Workers expect 200 (Task Claimed) or 204 (Queue Empty), but absolutely no 500s or 409s
      .check(status.in(200, 204)))

  // 3. Configure the Injection Profile (How aggressively to simulate users)
  setUp(
    scn.inject(
      // Ramp up from 0 to 100 concurrent users over a 15-second window
      rampUsers(100).during(15.seconds),
      // Then hold a steady state of 20 new users arriving per second for 30 seconds
      constantUsersPerSec(20).during(30.seconds)
    )
  ).protocols(httpProtocol)
}
