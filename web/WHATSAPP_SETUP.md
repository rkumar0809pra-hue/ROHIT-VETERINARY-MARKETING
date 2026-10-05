# RVH WhatsApp Cloud API

Open **Social & WhatsApp** after signing in as the owner. The legacy `/whatsapp` URL opens the same combined page. The saved RVH business number is **+91 9709095993**. Use **Connect WhatsApp** for setup and **Check business number** to verify the configured Meta number matches it. Incoming text messages are stored in the existing persistent SQLite database. ChatGPT creates reviewable reply drafts. The owner edits and explicitly approves each reply. Staff handoff disables sending for that message. No autonomous replies or bulk campaigns are enabled.

Set server environment variables in the hosting dashboard:

- `WHATSAPP_PHONE_NUMBER_ID`: Meta phone number ID (not the display telephone number).
- `WHATSAPP_ACCESS_TOKEN`: token authorized to send WhatsApp messages.
- `WHATSAPP_APP_SECRET`: Meta application secret used for signature verification.
- `WHATSAPP_VERIFY_TOKEN`: choose a random private verification token.
- `WHATSAPP_API_VERSION`: the Graph API version supported by your Meta application.
- Existing `OPENAI_API_KEY` and `OPENAI_MODEL` enable reply drafting.

Callback URL: `https://marketing.rohitveterinary.com/api/whatsapp/webhook`. Enter the same verify token in Meta and subscribe to the WhatsApp `messages` webhook field. Verify the number through Meta onboarding before enabling sending. Check coexistence eligibility before changing an existing WhatsApp Business app number; do not delete or unregister it as part of this setup.

Signed webhook events are bounded, matched to the configured number and deduplicated by message ID. Customer records require owner authentication. Only explicitly approved text replies inside 24 hours of the corresponding customer message are submitted. A Meta receipt indicates submission, not delivery. Uncertain submissions remain blocked to avoid accidental duplicates. Check Meta delivery logs for reconciliation. Non-text messages require staff review.

Test with a Meta test number and your own recipient before production. Tokens stay on the server; never share them in chat or commit them to GitHub. Keep DATA_FILE on persistent storage and include customer-message records in your retention and backup policy.

Campaign templates, delivery-status reconciliation, consent records, CRM matching, scheduled reminders and automatic customer replies are follow-up work. This first release provides the incoming inbox and owner-approved reply integration.
