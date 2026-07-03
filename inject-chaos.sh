#!/bin/bash

# Ensure script stops on first error
set -e

echo "⚠️  INJECTING CHAOS: Adding 2000ms latency to the Redis connection tunnel..."

# Use the Toxiproxy REST API to dynamically add a 'toxic' to our proxy tunnel
curl -s -X POST http://localhost:8474/proxies/redis_chaos_proxy/toxics \
  -H "Content-Type: application/json" \
  -d '{
    "name": "redis_latency",
    "type": "latency",
    "stream": "downstream",
    "attributes": {
      "latency": 2000,
      "jitter": 100
    }
  }'

echo -e "\n✅ Chaos injected. All Redis packets will now take ~2.1 seconds to process."
echo "Run './remove-chaos.sh' to restore the network."
