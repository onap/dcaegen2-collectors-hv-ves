#!/bin/bash
set -eou pipefail

if [[ $# < 2 ]]; then
  echo "Please provide a key file prefix and a target host:port"
  exit 1
fi

key_prefix=$1
host_and_port=$2

cert_file="$key_prefix.crt"
key_file="$key_prefix.key"

if [[ ! -r "$cert_file" ]]; then
  echo "$cert_file is not readable"
  exit 2
fi
    
if [[ ! -r "$key_file" ]]; then
  echo "$key_file is not readable"
  exit 2
fi

openssl s_client -connect $host_and_port -cert "$cert_file" -key "$key_file" -CAfile onap.crt

