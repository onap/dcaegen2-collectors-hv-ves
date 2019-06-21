#!/usr/bin/env bash

function usage() {
    echo ""
    echo "Run HV-VES perfomance test locally"
    echo "Usage $0 setup|start|clean"
    echo "  - setup : generate certs and set up docker components"
    echo "  - start : run the performance test (also performs a setup)" # TODO
    echo "  - clean : remove generated certs and created docker components"
    exit 1
}

function setup_environment(){
    echo "Setting up"
    cd ../ssl
    ./gen-certs.sh
    cd ../performance
    docker-compose up -d
    exit 0
}

function start_performance_test(){
    setup_environment
    echo "Performance test started"
    exit 0
}

function clean(){
    echo "Cleaning up"
    cd ../ssl
    ../ssl/gen-certs.sh clean
    cd ../performance
    docker-compose down
    exit 0
}

if [[ $# -eq 0 ]]; then
    usage
else
    for arg in ${@}
    do
        case ${arg} in
            setup)
            setup_environment
            ;;
            start)
            start_performance_test
            ;;
            clean)
            clean
            ;;
            usage)
            usage
            ;;
            *)
            echo "Unknown action: ${arg}" >&2
            usage
            ;;
        esac
    done
fi