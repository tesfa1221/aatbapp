# Deployment Guide

## Architecture
```
Android App → cPanel (PHP + MySQL) → Render.com (Python AI)
```

---

## Step 1 — Deploy Python AI to Render.com

1. Push this project to GitHub (create a repo if you don't have one)
2. Go to https://render.com and sign up (free)
3. Click **New → Web Service**
4. Connect your GitHub repo
5. Render auto-detects `render.yaml` — just click **Deploy**
6. Wait ~5 minutes for first deploy (downloads EasyOCR models)
7. Copy your service URL — looks like: `https://aatb-ai-service.onrender.com`

---

## Step 2 — Upload PHP to cPanel

1. In cPanel → File Manager → go to `public_html` (or your subdomain folder)
2. Upload everything inside the `api/` folder
3. In cPanel → MySQL Databases:
   - Create database: `aatb_db`
   - Create user and assign ALL PRIVILEGES
4. In cPanel → phpMyAdmin → select `aatb_db` → SQL tab → paste contents of `api/setup_db.sql` → Go
5. Edit `db.php` with your cPanel DB credentials:
   ```php
   $host = 'localhost';
   $db   = 'youraccount_aatb_db';  // cPanel prefixes DB names
   $user = 'youraccount_dbuser';
   $pass = 'yourpassword';
   ```

---

## Step 3 — Connect cPanel to Render

In cPanel → **Environment Variables** (or edit `scan.php` directly):

Set:
```
AI_SERVICE_URL = https://aatb-ai-service.onrender.com/analyze
```

Or just edit line in `scan.php`:
```php
$ai_url = 'https://aatb-ai-service.onrender.com/analyze';
```

---

## Step 4 — Update Android App

In `MainActivity.java`, change BASE_URL to your cPanel domain:
```java
public static final String BASE_URL = "https://yourdomain.com/api/";
```

Rebuild and install the APK on your phone.

---

## Important Notes

- **Render free tier sleeps after 15 min** of no requests — first scan after idle takes ~30 seconds to wake up. Subsequent scans are fast.
- **To avoid sleep**: upgrade to Render's $7/month paid plan, or use Oracle Cloud free VPS instead.
- **cPanel DB name**: cPanel always prefixes DB names with your account username e.g. `myuser_aatb_db`
