#!/bin/bash
# scripts/run_training_background.sh
# Launches training detached and prevents macOS from sleeping.

if [ "$#" -lt 1 ]; then
    echo "Usage: ./scripts/run_training_background.sh <path_to_config.yaml> [--resume] [--smoke]"
    exit 1
fi

CONFIG=$1
shift
ARGS="$@"

# Determine run name from config
RUN_NAME=$(grep "name:" $CONFIG | awk '{print $2}' | tr -d '"' | tr -d "'")
if [ -z "$RUN_NAME" ]; then
    RUN_NAME="train_run"
fi

if [[ "$ARGS" == *"--smoke"* ]]; then
    RUN_NAME="${RUN_NAME}_smoke"
    RUNS_DIR="ai-service/training/runs_smoke"
else
    RUNS_DIR="ai-service/training/runs"
fi

RUN_DIR="$RUNS_DIR/$RUN_NAME"
mkdir -p "$RUN_DIR"

LOG_FILE="$RUN_DIR/train.log"
PID_FILE="$RUN_DIR/train.pid"

echo "Starting training in background for run: $RUN_NAME"
echo "Log file: $LOG_FILE"
echo "PID file: $PID_FILE"

# caffeinate prevents sleep on macOS, nohup detaches the process
nohup caffeinate -i python3 ai-service/training/train.py --config "$CONFIG" $ARGS > "$LOG_FILE" 2>&1 &

PID=$!
echo $PID > "$PID_FILE"

echo ""
echo "✅ Training launched successfully with PID $PID."
echo "To check progress: tail -f $LOG_FILE"
echo "To stop: kill \$(cat $PID_FILE)"
