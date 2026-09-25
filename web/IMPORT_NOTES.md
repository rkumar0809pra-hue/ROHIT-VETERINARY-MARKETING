# AI Studio source port — September 2026

The supplied AI Studio ZIP contains the existing Kotlin/Jetpack Compose Android project. It contains no exported Room database or user-created media. The web app ports its workflows to the existing Render deployment; it does not replace the native source or erase the existing web database.

| Source workflow | Render implementation |
| --- | --- |
| Dashboard and desktop/mobile navigation | Responsive PWA navigation, counts and creative shortcuts |
| Content Creator | Category, audience, tone, language, service/product, CTA, captions, alternatives, creative directions, editable social preview |
| Video Maker | Real server-side Veo requests, text-to-video and saved-photo animation, 4/6/8 second clips, persisted operation polling, authenticated gallery/downloads |
| Longer reels | 15/30/60 second storyboard and voiceover drafts; clips are not automatically stitched |
| Images | OpenAI image generation, PNG/JPEG uploads, downloadable branded PNG template |
| WhatsApp | Audience briefs, personalization placeholders, drafts, review, manual sharing and recorded campaign results |
| Calendar | Month/week view and date filtering in India Standard Time, approval-gated plans, manual published records |
| Library | Search, status/channel filters, edit, remix, text download, approved text sharing, owner JSON export |
| Analytics | Aggregate results, per-draft campaign results and ROAS; all numbers are owner/staff supplied |
| Search & AI discoverability | New (not in the original source): SEO checklist and keyword tracker, AIO checklist and target-question tracker, both with a live OpenAI web-search "Check now" against configured domains |
| AI Assistant | Persistent shared conversation using clinic identity; save replies as drafts |
| Settings | Editable shared clinic identity, provider status and role setup |
| Roles | Owner approval/settings; staff planning/results; creator drafting/submission. Server-enforced role passwords, not individual employee accounts |
| Installation | Existing desktop and Android PWA installation and offline shell |

## Brand identity

Default profile: रोहित भेटनरी हाउस; Rohit Veterinary House; 9709095993; Barwatoli, Lohardaga, Jharkhand, India. Every text/media agent receives the profile. Generated image lettering and Hindi video speech must still be reviewed. Settings edits apply to future generation, not previously saved drafts.

## Deliberately not imported as real activity

The source video service uses sample video URLs, including an error fallback. Those were replaced with actual provider operation polling, never presented as successful generation. Demo metrics, simulated sends and fabricated sample drafts are not seeded. Existing native-device data must be separately exported if it is needed.

Actual Facebook/Instagram publishing and WhatsApp delivery still require provider account connections, approved templates where applicable, recipient consent and a delivery worker. This version prepares content and records manual publication; it never reports an automatic send. No connected contacts/CRM, video stitching, spoken chat or custom domain automation was added.

## Operations

Keep DATA_FILE=/var/data/marketing.sqlite and media on the persistent Render disk. Additive migration preserves previous drafts, runs and metrics. MEDIA_DIR defaults to /var/data/media with that database setting. Limit: 600 MB stored media, 6 MB per upload, 64 MB video download, two active media jobs and 12 generation requests per rolling 24 hours. Upload and chat limits are separate. Failed submissions count against limits to avoid uncontrolled retries.

Veo requires GEMINI_API_KEY and paid model access. Optional VEO_MODEL defaults to veo-3.1-fast-generate-preview. Image generation uses OPENAI_API_KEY and OPENAI_IMAGE_MODEL (default gpt-image-1). Text generation continues using OPENAI_MODEL. No key is returned to browsers. Provider credentials/model eligibility cannot be proven by local stub tests.

Video operation IDs survive process restarts and resume polling. A restart during initial submission or image generation is marked failed with an explicit unknown-completion warning; check provider usage before retrying. Already completed files are local and private. JSON export includes metadata, not media bytes; download media separately. Back up the SQLite database and media directory.

## Verification

Automated server tests cover auth/origin protection, old database persistence, approval and stale-edit gates, profile propagation, role permissions, private media/range delivery, failed jobs, Veo REST payload/operation parsing and download credential isolation. AI tests use deterministic stubs; no billable requests are made by testing.

Browser verification completed in Chromium at desktop and 390px mobile widths: Content Creator generation (stub), draft save/approval/IST planning/manual publication, image upload, photo-to-video job submission (stub), assistant, profile save and navigation through every screen. No JavaScript errors or horizontal overflow were observed. Live provider billing/access and device installation still require the deployed accounts/devices.
