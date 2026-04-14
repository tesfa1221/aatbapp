#!/bin/bash
# Install Tesseract OCR system package
apt-get update && apt-get install -y tesseract-ocr tesseract-ocr-eng libgl1

# Install Python dependencies
pip install -r api/requirements.txt
