#!/usr/bin/env bash

set -eu -o pipefail -o xtrace

STORE_PASS=onaponap
DNAME_PREFIX="C=PL,ST=DL,L=Wroclaw,O=Nokia,OU=MANO,CN=dcaegen2-hvves"
CA=ca
TRUST=trust

store_opts="-storetype PKCS12 -storepass ${STORE_PASS} -noprompt"

function gen_key() {
  local keystore="-keystore $1.p12 ${store_opts}"
  keytool -genkey -alias $1 \
      ${keystore} \
      -keyalg RSA \
      -validity 730 \
      -keysize 2048 \
      -dname "${DNAME_PREFIX}-$1"
  keytool -import -trustcacerts -alias ca -file ${CA}.crt ${keystore}

  keytool -certreq -alias $1 -keyalg RSA ${keystore} | \
      keytool -alias ca -gencert -ext san=dns:${CA} ${store_opts} -keystore ${CA}.p12 | \
      keytool -alias $1 -importcert ${keystore}
}


function gen_ca() {
  keytool -genkeypair ${store_opts} -alias ${CA} -dname "${DNAME_PREFIX}-${CA}" -keystore ${CA}.p12
  keytool -export -alias ca -file ${CA}.crt ${store_opts} -keystore ${CA}.p12
}

function gen_trust_ca() {
  keytool -import -trustcacerts -alias ca -file ${CA}.crt ${store_opts} -keystore ${TRUST}.p12
}

function clean() {
  rm -f *.crt *.p12
}

if [[ $# -eq 0 ]]; then
  gen_ca
  gen_trust_ca
  gen_key client
  gen_key server
elif [[ $1 == "clean" ]]; then
  clean
else
  echo "usage: $0 [clean]"
  exit 1
fi

