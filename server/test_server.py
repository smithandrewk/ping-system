#!/usr/bin/env python3
"""
Test script for the ping server API.
Run this to verify server functionality before deploying.
"""

import requests
import json
import time
from datetime import datetime

SERVER_URL = "http://localhost:5000"

def test_health_endpoint():
    """Test the health check endpoint."""
    print("Testing /health endpoint...")
    try:
        response = requests.get(f"{SERVER_URL}/health", timeout=5)
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
        return response.status_code == 200
    except Exception as e:
        print(f"Health check failed: {e}")
        return False

def test_ping_endpoint():
    """Test the ping endpoint with valid data."""
    print("\nTesting /ping endpoint...")
    test_data = {"device_id": f"test_device_{int(time.time())}"}
    
    try:
        response = requests.post(
            f"{SERVER_URL}/ping",
            json=test_data,
            headers={"Content-Type": "application/json"},
            timeout=5
        )
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
        return response.status_code == 200
    except Exception as e:
        print(f"Ping test failed: {e}")
        return False

def test_invalid_ping():
    """Test ping endpoint with invalid data."""
    print("\nTesting /ping with invalid data...")
    test_cases = [
        {},  # Empty data
        {"wrong_field": "value"},  # Wrong field name
        {"device_id": ""},  # Empty device_id
        {"device_id": None},  # Null device_id
    ]
    
    for i, test_data in enumerate(test_cases):
        try:
            response = requests.post(
                f"{SERVER_URL}/ping",
                json=test_data,
                headers={"Content-Type": "application/json"},
                timeout=5
            )
            print(f"Test case {i+1}: Status {response.status_code}")
            if response.status_code == 400:
                print(f"  Expected error: {response.json()}")
        except Exception as e:
            print(f"Test case {i+1} failed: {e}")

def test_multiple_pings():
    """Test sending multiple pings rapidly."""
    print("\nTesting multiple rapid pings...")
    device_id = f"stress_test_{int(time.time())}"
    
    for i in range(5):
        test_data = {"device_id": f"{device_id}_{i}"}
        try:
            response = requests.post(
                f"{SERVER_URL}/ping",
                json=test_data,
                timeout=5
            )
            print(f"Ping {i+1}: {response.status_code}")
            time.sleep(0.1)  # Small delay
        except Exception as e:
            print(f"Ping {i+1} failed: {e}")

def main():
    """Run all tests."""
    print(f"Starting ping server tests at {datetime.now()}")
    print(f"Server URL: {SERVER_URL}")
    print("=" * 50)
    
    # Run tests
    health_ok = test_health_endpoint()
    ping_ok = test_ping_endpoint()
    test_invalid_ping()
    test_multiple_pings()
    
    print("\n" + "=" * 50)
    print("Test Summary:")
    print(f"Health check: {'PASS' if health_ok else 'FAIL'}")
    print(f"Ping endpoint: {'PASS' if ping_ok else 'FAIL'}")
    
    if health_ok and ping_ok:
        print("\n✅ All critical tests passed! Server is ready.")
    else:
        print("\n❌ Some tests failed. Check server configuration.")

if __name__ == "__main__":
    main()