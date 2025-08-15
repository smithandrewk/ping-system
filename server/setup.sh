#!/bin/bash
# Setup script for ping server deployment

set -e

echo "Setting up ping server..."

# Create system user
sudo useradd --system --create-home --home-dir /opt/ping-server --shell /bin/false ping-server

# Create directories
sudo mkdir -p /opt/ping-server
sudo chown ping-server:ping-server /opt/ping-server

# Copy files
sudo cp app.py requirements.txt .env.example /opt/ping-server/
sudo cp ping-server.service /etc/systemd/system/

# Create virtual environment
sudo -u ping-server python3 -m venv /opt/ping-server/venv
sudo -u ping-server /opt/ping-server/venv/bin/pip install -r /opt/ping-server/requirements.txt

# Create .env file from example
sudo cp /opt/ping-server/.env.example /opt/ping-server/.env
sudo chown ping-server:ping-server /opt/ping-server/.env
sudo chmod 600 /opt/ping-server/.env

echo "Please edit /opt/ping-server/.env with your database credentials"

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable ping-server.service

echo "Setup complete! To start the server:"
echo "sudo systemctl start ping-server"
echo "sudo systemctl status ping-server"