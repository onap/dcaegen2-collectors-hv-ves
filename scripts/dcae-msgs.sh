#!/bin/bash

echo "All messages count currently consumed by dcae app simulator: "
curl --request GET localhost:6063/messages/all/count
echo
