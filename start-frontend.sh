#!/bin/bash

echo "========================================="
echo "Starting GCPulse Frontend..."
echo "========================================="

cd frontend

# 检查Node版本
echo "Node version:"
node -v
echo "npm version:"
npm -v

echo ""
echo "Installing dependencies..."
npm install

echo ""
echo "Starting Vite dev server..."
npm run dev

