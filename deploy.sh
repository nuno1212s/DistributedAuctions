#!/bin/bash

NODES=("ssd@51.103.29.195" "ssd2@20.199.110.1" "ssd3@20.199.104.254")

for NODE in "${NODES[@]}"; do
  echo "Deploying to the node: $NODE"
  scp ./build/libs/DistributedAuctions-*-all.jar "$NODE:"
  done