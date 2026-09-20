with open('app/src/main/java/com/example/ui/screens/WhatsAppCampaignScreen.kt', 'r') as f:
    content = f.read()

# Wrap the LazyColumn in ResponsiveContentContainer
old_col_start = '''    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("whatsapp_campaign_screen"),'''

new_col_start = '''    com.example.ui.components.ResponsiveContentContainer(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("whatsapp_campaign_screen"),'''

if old_col_start in content:
    content = content.replace(old_col_start, new_col_start)
    # Add closing brace at the end of WhatsAppCampaignScreen composable
    # Find fun WhatsAppLivePreviewCard or fun WhatsAppCampaignCard
    marker = '\n@Composable\nfun WhatsAppLivePreviewCard('
    if marker in content:
        idx = content.find(marker)
        content = content[:idx] + '\n    }' + content[idx:]
        with open('app/src/main/java/com/example/ui/screens/WhatsAppCampaignScreen.kt', 'w') as f:
            f.write(content)
        print("SUCCESS_WHATSAPP")
    else:
        print("MARKER NOT FOUND")
else:
    print("OLD_COL_START NOT FOUND")
