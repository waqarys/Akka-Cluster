#!/usr/bin/env bash

set -euo pipefail
IFS=$'\n\t'

function help {
    echo ""
    echo "Syntax"
    echo "    loyalty.sh [option...] [award|deduct|retrieve] [VALUE]"
    echo ""
    echo "Operations"
    echo "    award - Awards VALUE points to the provided account."
    echo "    deduct - Deducts VALUE points from the provided account."
    echo "    retrieve - Retrieves the information for the provided account."
    echo ""
    echo "Options"
    echo "    -a <account> Assign an account Id (default=sample)"
    echo "    -p <port> Use a specific port (default=8000)"
    echo ""
}

while getopts "a:p:?" opt; do
  case $opt in
    a)
        ACCOUNT=${OPTARG}
        ;;
    p)
        PORT=${OPTARG}
        ;;
    *)
        help
        exit 0
        ;;
  esac
done

shift $((OPTIND-1))

ACCOUNT=${ACCOUNT:-sample}
PORT=${PORT:-8000}
OPERATION=${1:-retrieve}
VALUE=${2:-0}

function award {
    echo "AWARD $VALUE TO $ACCOUNT ON PORT $PORT"
    echo "+ curl -w '\n' -X POST http://localhost:$PORT/loyalty/$ACCOUNT/award/$VALUE"
    curl -w "\n" -X POST http://localhost:$PORT/loyalty/$ACCOUNT/award/$VALUE
}

function deduct {
    echo "DEDUCT $VALUE FROM $ACCOUNT ON PORT $PORT"
    echo "+ curl -w '\n' -X POST http://localhost:$PORT/loyalty/$ACCOUNT/deduct/$VALUE"
    curl -w "\n" -X POST http://localhost:$PORT/loyalty/$ACCOUNT/deduct/$VALUE
}

function retrieve {
    echo "RETRIEVE $ACCOUNT ON PORT $PORT"
    echo "+ curl -w '\n' -X GET http://localhost:$PORT/loyalty/$ACCOUNT"
    curl -w "\n" -X GET http://localhost:$PORT/loyalty/$ACCOUNT
}

case $OPERATION in
    award)
        award
        ;;
    deduct)
        deduct
        ;;
    retrieve)
        retrieve
        ;;
    *)
        echo "Invalid Operation"
        help
        ;;
esac
