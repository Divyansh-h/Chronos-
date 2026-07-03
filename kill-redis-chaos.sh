#!/bin/bash
set -e

echo "⚠️  CHAOS ENGINEERING: Container Death Simulation"

# 1. Trigger the background engine to start aggressively polling for tasks
echo "Creating a workflow to trigger TaskQueueService..."
curl -s -X POST http://localhost:8080/api/v1/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Catastrophic Failure Workflow",
    "tasks": [{"name": "Orphaned Task"}]
  }' > /dev/null

echo -e "\n💥 Executing hard kill on the Redis container..."
# Assumes standard docker-compose naming convention (directory_service_index)
docker kill chronos-redis-1 > /dev/null || docker kill redis > /dev/null || echo "Failed to kill redis. Check container name."

echo "Waiting for Spring Boot background threads to encounter the dead connection..."
sleep 5

echo -e "\n🔍 Verifying Backend Resilience Logs:"
# We grep the logs for the specific Redis connection exception to prove the catch block triggered
docker logs chronos-backend-1 2>&1 | grep -i "Failed to poll task queue" || echo "Could not find expected graceful degradation log."

echo -e "\n✅ If you see the error log above without a thread stacktrace crash, the system successfully survived the container death."

echo -e "\n🩹 Restoring Redis container..."
docker start chronos-redis-1 > /dev/null || docker start redis > /dev/null
echo "Redis restored."
