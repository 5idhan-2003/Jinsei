# Jinsei

Minimal dot-grid wallpapers: **Life** (weeks), **Year** (days) and **Goal** (countdown). Every field is editable.

- `android/` – the Jinsei app. Edit the fields, preview live, tap one button; it sets the wallpaper and refreshes it every midnight. No MacroDroid needed.
- `web/` – optional website + `/api/wallpaper` PNG endpoint (deploy on Vercel).

## Get the APK (no Android Studio needed)
1. Push this repo to GitHub (`main` branch).
2. Open the **Actions** tab → **Build APK** → latest run → download the `Jinsei-apk` artifact.
3. Unzip, copy `app-debug.apk` to your phone, install (allow "install unknown apps" when asked).
4. Open Jinsei, choose Life / Year / Goal, fill the fields, tap **Set wallpaper & start daily updates**.
5. In phone settings → Apps → Jinsei → Battery, choose **Unrestricted** so the midnight refresh is never skipped.

## Deploy the website on Vercel
Import the GitHub repo in Vercel and set **Root Directory** to `web`. Example URL:
`https://YOUR-APP.vercel.app/api/wallpaper?type=goal&title=Marathon&start=2026-01-01&end=2026-12-31&device=oneplus13s`

Parameters: `type` (life|year|goal), `dob`, `lifespan`, `year`, `title`, `start`, `end`, `device` (oneplus13s|oneplus13|WxH), `accent` (hex, no #), `tz` (default Asia/Kolkata).

## Local test of the renderer
```
cd web && npm install && npm run test:render   # writes web/out/*.png
```

