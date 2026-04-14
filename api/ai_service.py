from fastapi import FastAPI, File, UploadFile
from typing import List
import cv2
import numpy as np
import pytesseract
from PIL import Image
import io
import os
import uvicorn

app = FastAPI()

# ── Spoofing Detection ────────────────────────────────────────────────────────

def detect_screen_glare(bgr: np.ndarray) -> bool:
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    _, bright = cv2.threshold(gray, 240, 255, cv2.THRESH_BINARY)
    return (np.sum(bright > 0) / bright.size) > 0.25

def detect_flat_texture(bgr: np.ndarray) -> bool:
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    return cv2.Laplacian(gray, cv2.CV_64F).var() < 40

def is_spoofed(bgr: np.ndarray) -> tuple:
    if detect_screen_glare(bgr):
        return True, "Screen glare detected"
    if detect_flat_texture(bgr):
        return True, "Flat texture detected (printed paper)"
    return False, ""

# ── OCR with Tesseract ────────────────────────────────────────────────────────

def preprocess(bgr: np.ndarray) -> np.ndarray:
    """Enhance image for better plate OCR."""
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    # Resize to improve OCR accuracy
    gray = cv2.resize(gray, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC)
    # Denoise
    gray = cv2.fastNlMeansDenoising(gray, h=10)
    # Threshold
    _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    return thresh

def extract_plate(bgr: np.ndarray) -> tuple:
    processed = preprocess(bgr)
    pil_img = Image.fromarray(processed)

    # Try alphanumeric only first (numbers + latin letters on Ethiopian plates)
    config_alpha = r'--oem 3 --psm 8 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
    text = pytesseract.image_to_string(pil_img, config=config_alpha).strip().upper()

    import re
    text = re.sub(r'[^A-Z0-9]', '', text)

    # If nothing found, try without whitelist (catches mixed scripts)
    if not text:
        config_free = r'--oem 3 --psm 8'
        text = pytesseract.image_to_string(pil_img, config=config_free).strip()
        text = re.sub(r'\s+', '', text)

    if not text:
        return "", 0.0

    data = pytesseract.image_to_data(pil_img, config=config_alpha, output_type=pytesseract.Output.DICT)
    confidences = [int(c) for c in data['conf'] if str(c).isdigit() and int(c) > 0]
    confidence = (sum(confidences) / len(confidences) / 100.0) if confidences else 0.5

    return text, confidence

# ── Endpoint ──────────────────────────────────────────────────────────────────

@app.post("/analyze")
async def analyze(images: List[UploadFile] = File(...)):
    results = []
    for upload in images:
        data = await upload.read()
        arr = np.frombuffer(data, np.uint8)
        bgr = cv2.imdecode(arr, cv2.IMREAD_COLOR)

        if bgr is None:
            results.append({"status": "error", "reason": "Could not decode image"})
            continue

        spoofed, spoof_reason = is_spoofed(bgr)
        if spoofed:
            results.append({
                "status": "rejected",
                "spoofing_detected": True,
                "reason": spoof_reason
            })
            continue

        plate_text, confidence = extract_plate(bgr)
        if not plate_text:
            results.append({"status": "error", "reason": "No plate detected"})
            continue

        results.append({
            "status": "success",
            "plate_text": plate_text,
            "confidence": confidence,
            "spoofing_detected": False
        })

    return results[0] if results else {"status": "error", "reason": "No images received"}

@app.get("/health")
def health():
    return {"status": "ok"}

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
