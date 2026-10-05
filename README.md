# Rohit Veterinary Marketing

This repository contains two separate clients:

- `app/`: the existing native Android / Jetpack Compose project.
- `web/`: RVH Marketing Studio, an installable responsive web app for desktop and Android, with a shared server and six on-demand OpenAI marketing specialists.

Start with [web setup and deployment](web/README.md). The native Android database and Gemini implementation are not migrated or synchronised with the new web workspace. To use the shared workspace on both devices, install the **web app** on each device using the same deployed URL.

The Android project's existing “Web PWA” dialog points to AI Studio preview URLs. Those links are not deployment URLs for the new `web/` app. Deploy the web server separately before installing it.
