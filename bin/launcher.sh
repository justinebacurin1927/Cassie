#!/bin/bash

# Get directory of this script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

echo "Current directory: $(pwd)"
echo "Looking for virtual environment..."

# Check for venv in different possible locations
if [ -f "../venv/bin/activate" ]; then
    echo "Found venv in parent directory"
    source ../venv/bin/activate
elif [ -f "../../venv/bin/activate" ]; then
    echo "Found venv in ../.."
    source ../../venv/bin/activate
elif [ -f "venv/bin/activate" ]; then
    echo "Found venv in current directory"
    source venv/bin/activate
else
    echo "No virtual environment found. Creating one..."
    
    # Decide where to create it
    if [ -d "../venv" ] || [ -d "../../venv" ]; then
        echo "Using existing venv location..."
        # Find existing venv
        if [ -d "../venv" ]; then
            source ../venv/bin/activate
        else
            source ../../venv/bin/activate
        fi
    else
        # Create in parent directory (common structure)
        cd ..
        python -m venv venv
        source venv/bin/activate
        cd bin
    fi
    
    # Install packages
    pip install pygame pytube pydub mutagen requests customtkinter
fi

# Check if we're in venv
if [ -z "$VIRTUAL_ENV" ]; then
    echo "WARNING: Not in virtual environment. Using system Python may fail."
    echo "Trying to run anyway..."
fi

# Run the application
echo "Running MP3 player..."
python Mp3.py

# Keep terminal open
read -p "Press Enter to exit..."