#!/bin/bash
echo "🩹 REMOVING CHAOS: Healing the Redis connection tunnel..."

curl -s -X DELETE http://localhost:8474/proxies/redis_chaos_proxy/toxics/redis_latency

echo -e "\n✅ Network restored to 0ms latency."
