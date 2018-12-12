#!/bin/bash

echo "Requesting DCAE app running on port 6063 to reset messages count"
curl --request DELETE localhost:6063/messages
echo
