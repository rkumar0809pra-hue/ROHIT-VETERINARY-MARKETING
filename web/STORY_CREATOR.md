# RVH Story Creator

Adds a Story Creator shortcut to RVH Studio's sidebar and a specialist agent using the existing OpenAI connection. It creates saved, editable campaign drafts in the existing review workflow. It does not render media or publish campaigns.

## Use

1. Open Settings and review the saved clinic name, Hindi spelling, phone, address, services, hours and booking link. Owner-approved profile values remain authoritative.
2. Select Story Creator. The starting brief is already filled in; select Hindi, English or Hinglish and optionally specify a service, audience or goal.
3. Generate the draft. The server reads four fixed public RVH pages, then supplies their text alongside the saved clinic profile to the agent.
4. Review the establishment summary, conflicts and missing information, three ranked ideas, timed storyboard, poster copy, caption and WhatsApp draft.
5. Save and approve the content through the existing workflow. Use the handoff buttons to populate Make a video, the quick branded poster, Write a post or WhatsApp. This fills editable fields only; it does not generate paid media or publish. Buttons appear only on approved stories with valid handoff blocks. Older stories can be regenerated for this format.

## Gathering establishment details

The fixed source set covers the main website, services page, clinic app homepage and Vet Mart homepage. Every run reports retrieval time and each page's status. The existing website reader limits hosts, redirects, response size and request time and rejects private IPv4 targets. No arbitrary URL from a brief or retrieved page is fetched.

Research uses initial public HTML. It cannot guarantee a complete profile: JavaScript-only pages, unavailable sources, private records, social accounts and original media may be missing. Public-page claims are explicitly separated from approved profile facts. The agent must ask for confirmation before putting website-only facilities, services, prices, discounts, availability, ratings or results into advertising. This is a model instruction, not a deterministic claim validator; owner review remains necessary.

Check the following profile gaps when relevant: establishment history, doctor biography, services and species, current facilities, hours and emergency availability, contact and map details, current offers, logo and brand colours, original clinic photos, consented stories and approved booking links. The agent should ask the three most useful outstanding questions rather than blocking every draft.

## Starter creative direction

Proposed campaign: **पशुओं की देखभाल, अपने शहर में**. Audience: pet owners and livestock farmers in Lohardaga. Goal: consultation enquiries. Use original clinic footage only; the following is a draft, not a record of actual patient outcomes.

| Time | Visual direction | Hindi narration |
| --- | --- | --- |
| 0–5s | Owner with an animal, with filming permission | आपके पशु की सेहत, आपके परिवार की खुशी। |
| 5–12s | Actual RVH exterior and reception | लोहरदगा में रोहित भेटनरी हाउस से जुड़ें। |
| 12–21s | Dr. Rohit speaking or examining an animal, with permission | अपने पालतू पशु या पशुधन की देखभाल के लिए डॉ. रोहित कुमार से परामर्श लें। |
| 21–30s | Clinic name and approved contact card | अपॉइंटमेंट की जानकारी के लिए कॉल करें: नौ सात शून्य नौ शून्य नौ पाँच नौ नौ तीन। |

Poster headline: **पशुओं की देखभाल, अपने शहर में**.

Supporting copy: **रोहित भेटनरी हाउस, लोहरदगा। परामर्श और अपॉइंटमेंट की जानकारी के लिए संपर्क करें।**

Contact in the existing default profile: **9709095993**, **Barwatoli, Lohardaga, Jharkhand, India**. A saved owner-edited profile overrides defaults. Do not hardcode this example into future generated work.

## Verification and rollout

The implementation reuses the authenticated, rate-limited `/api/runs` endpoint and saved draft/approval workflow. Other agents do not fetch these pages. Source statuses are appended programmatically even if the model omits them.

Automated checks cover failed and limited sources, bounded excerpts, removal of scripts/styles, profile and research delivery, source receipts and isolation from other agents. A live paid model run and production deployment must be verified separately.

Source branch: `codex/desktop-android-marketing-agents`. Feature branch: `codex/rvh-story-creator`. The repository's `main` branch contains the Android project rather than this web Studio; verify the Render service's configured deployment branch before merging. No new API key or database migration is needed.

## Specialist upgrades

- Campaign memory: the latest eight story drafts (bounded excerpts, with approval status) help vary hooks and themes. Previous drafts are not evidence that their claims are approved.
- Asset checklist: up to thirty completed uploads contribute their labels and descriptions. The model does not inspect image bytes or assume consent. It identifies new footage needed.
- Hook comparison: two alternative opening hooks plus a suggested measurement. No predicted winner or invented performance data.
- Format handoff: separate bounded blocks isolate production briefs and public copy. Malformed or oversized blocks produce no handoff button. Nothing is silently truncated. Edits return drafts to review under the existing approval workflow.
