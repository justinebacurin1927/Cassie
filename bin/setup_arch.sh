#!/bin/bash

echo "📦 Setting up MP3 Downloader & Player on Arch Linux..."

# Update system
echo "🔄 Updating system..."
sudo pacman -Syu --noconfirm

# Install dependencies
echo "📥 Installing system dependencies..."
sudo pacman -S --noconfirm python python-pip tk ffmpeg base-devel
sudo pacman -S --noconfirm sdl2 sdl2_image sdl2_mixer sdl2_ttf

# Create project directory
echo "📁 Creating project structure..."
mkdir -p ~/mp3_app
cd ~/mp3_app

# Create virtual environment
echo "🐍 Setting up Python virtual environment..."
python -m venv venv
source venv/bin/activate

# Upgrade pip
pip install --upgrade pip

# Create requirements.txt
echo "📝 Creating requirements.txt..."
cat > requirements.txt << 'EOF'
pygame==2.5.0
pytube==15.0.0
mutagen==1.47.0
pydub==0.25.1
requests==2.31.0
customtkinter==5.2.0  # Optional
EOF

# Install Python packages
echo "📦 Installing Python packages..."
pip install -r requirements.txt

# Create project structure
mkdir -p downloads temp

# Create a simple test script
echo "🧪 Creating test script..."
cat > test_setup.py << 'EOF'
import sys
print("Python version:", sys.version)

try:
    import pygame
    print("✅ Pygame installed")
except ImportError:
    print("❌ Pygame missing")

try:
    import tkinter
    print("✅ Tkinter installed")
except ImportError:
    print("❌ Tkinter missing")

try:
    from pytube import YouTube
    print("✅ Pytube installed")
except ImportError:
    print("❌ Pytube missing")

print("\n🎉 Setup complete! You can now run main.py")
EOF

# Run test
python test_setup.py

echo ""
echo "=========================================="
echo "✅ Setup complete!"
echo "📂 Project location: ~/mp3_app"
echo ""
echo "To activate virtual environment:"
echo "  cd ~/mp3_app && source venv/bin/activate"
echo ""
echo "To run the app:"
echo "  python main.py"
echo "=========================================="