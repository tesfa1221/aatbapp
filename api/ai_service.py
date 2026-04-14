from fastapi import FastAPI, File, UploadFile
from typing import List
import cv2
import numpy as np
import easyocr
import uvicorn

app = FastAPI()
reader = easyocr.Reader(['en'], gpu=False)

# ── Spoofing Detection Helpers ──────────────────────────────────────────────

def detect_moire(gray: np.ndarray) -> bool:
    """Detect moire/banding patterns typical of screen captures."""
    f = np.fft.fft2(gray)
    fshift = np.fft.fftshift(f)
    magnitude = 20 * np.log(np.abs(fshift) + 1)
    h, w = magnitude.shape
    cx, cy = w // 2, h // 2
    # Mask out DC component
    magnitude[cy-10:cy+10, cx-10:cx+10] = 0
    # High peaks in frequency domain = periodic pattern = screen/print artifact
    threshold = np.mean(magnitude) + 4 * np.std(magnitude)
    peaks = np.sum(magnitude > threshold)
    return int(peaks) > 8


def detect_screen_glare(bgr: np.ndarray) -> bool:
    """Detect uniform high-brightness regions typical of screens."""
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    _, bright = cv2.threshold(gray, 240, 255, cv2.THRESH_BINARY)
    bright_ratio = np.sum(bright > 0) / bright.size
    return bright_ratio > 0.25  # >25% of image is blown-out bright


def detect_flat_texture(gray: np.ndarray) -> bool:
    """Printed paper / screens have very low local texture variance."""
    laplacian_var = cv2.Laplacian(gray, cv2.CV_64F).var()
    return laplacian_var < 40  # Real plates have embossing/depth


def is_spoofed(bgr: np.ndarray) -> tuple[bool, str]:
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    if detect_screen_glare(bgr):
        return True, "Screen glare detected"
    if detect_moire(gray):
        return True, "Moire pattern detected (screen)"
    if detect_flat_texture(gray):
        return True, "Flat texture detected (printed paper)"
    return False, ""

# ── OCR ─────────────────────────────────────────────────────────────────────

def extract_plate(bgr: np.ndarray) -> tuple[str, float]:
    rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
    results = reader.readtext(rgb)
    if not results:
        return "", 0.0
    # Pick result with highest confidence
    best = max(results, key=lambda r: r[2])
    return best[1].strip().upper(), float(best[2])

# ── Endpoint ─────────────────────────────────────────────────────────────────

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

    # Return first result (single-plate flow); extend if multi-plate needed
    return results[0] if results else {"status": "error", "reason": "No images received"}


if __name__ == "__main__":
    import os
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
