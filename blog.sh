#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$SCRIPT_DIR/.jekyll.pid"
LOG_FILE="$SCRIPT_DIR/.jekyll.log"

start() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "Jekyll server is already running (PID: $PID)"
            exit 1
        fi
    fi

    echo "Starting Jekyll server..."
    cd "$SCRIPT_DIR"
    bundle exec jekyll serve > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"

    sleep 2
    if ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
        echo "Jekyll server started (PID: $(cat "$PID_FILE"))"
        echo "Access at http://127.0.0.1:4000"
    else
        echo "Failed to start Jekyll server. Check $LOG_FILE for details."
        rm -f "$PID_FILE"
        exit 1
    fi
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "Jekyll server is not running (no PID file found)"
        exit 1
    fi

    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "Stopping Jekyll server (PID: $PID)..."
        kill "$PID"
        rm -f "$PID_FILE"
        echo "Jekyll server stopped"
    else
        echo "Jekyll server is not running (stale PID file)"
        rm -f "$PID_FILE"
    fi
}

status() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "Jekyll server is running (PID: $PID)"
        else
            echo "Jekyll server is not running (stale PID file)"
        fi
    else
        echo "Jekyll server is not running"
    fi
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop 2>/dev/null
        sleep 1
        start
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
