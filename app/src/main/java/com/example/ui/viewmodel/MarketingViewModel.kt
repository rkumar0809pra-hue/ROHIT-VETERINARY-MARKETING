package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SampleDataProvider
import com.example.data.gemini.GeneratedContentBundle
import com.example.data.gemini.GeneratedVideoScriptBundle
import com.example.data.gemini.MarketingPostIdea
import com.example.data.gemini.SocialMediaCaption
import com.example.data.gemini.ThemeMarketingResult
import com.example.data.gemini.VeoVideoResult
import com.example.data.model.AudienceType
import com.example.data.model.ChatMessage
import com.example.data.model.ContentCategory
import com.example.data.model.ContentLanguage
import com.example.data.model.ContentTone
import com.example.data.model.GeneratedVeoVideo
import com.example.data.model.MarketingPost
import com.example.data.model.Platform
import com.example.data.model.PostStatus
import com.example.data.model.UserRole
import com.example.data.model.VideoScript
import com.example.data.model.WhatsAppCampaign
import com.example.data.repository.MarketingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Calendar

enum class NavTab(val title: String) {
    DASHBOARD("Dashboard"),
    CREATE("Create"),
    VIDEO_MAKER("Video Maker"),
    CALENDAR("Calendar"),
    CAMPAIGNS("Campaigns"),
    LIBRARY("Library"),
    ANALYTICS("Analytics"),
    ASSISTANT("AI Assistant"),
    SETTINGS("Settings")
}

data class DashboardMetrics(
    val draftsCount: Int = 0,
    val pendingApprovalCount: Int = 0,
    val approvedCount: Int = 0,
    val scheduledCount: Int = 0,
    val publishedCount: Int = 0,
    val totalLeads: Int = 0,
    val next7DaysCount: Int = 0,
    val approvalRatePercent: Int = 0,
    val publishRatePercent: Int = 0
)

class MarketingViewModel(
    private val repository: MarketingRepository
) : ViewModel() {

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(NavTab.DASHBOARD)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    fun navigateTo(tab: NavTab) {
        _currentTab.value = tab
    }

    // Active User Role
    private val _currentRole = MutableStateFlow(UserRole.ADMIN)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun switchRole(role: UserRole) {
        _currentRole.value = role
    }

    // Responsive Preview Device Simulation Mode (AUTO, MOBILE_PREVIEW, TABLET_PREVIEW, DESKTOP_PREVIEW)
    // Allows preview-friendly testing inside the Google AI Studio emulator
    val previewDeviceMode = MutableStateFlow("AUTO") // "AUTO", "MOBILE", "TABLET", "DESKTOP"
    fun setPreviewDeviceMode(mode: String) {
        previewDeviceMode.value = mode
    }

    // Data streams from Room with instant sample initial values
    val allPosts: StateFlow<List<MarketingPost>> = repository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleDataProvider.getSamplePosts())

    val allVideoScripts: StateFlow<List<VideoScript>> = repository.allVideoScripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleDataProvider.getSampleVideoScripts())

    val allCampaigns: StateFlow<List<WhatsAppCampaign>> = repository.allCampaigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleDataProvider.getSampleCampaigns())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allPosts,
        allCampaigns
    ) { posts, campaigns ->
        val effectivePosts = if (posts.isEmpty()) SampleDataProvider.getSamplePosts() else posts
        val effectiveCampaigns = if (campaigns.isEmpty()) SampleDataProvider.getSampleCampaigns() else campaigns

        val drafts = effectivePosts.count { it.status == PostStatus.DRAFT.name }
        val pendingApproval = effectivePosts.count { it.status == PostStatus.PENDING_APPROVAL.name }
        val approved = effectivePosts.count { it.status == PostStatus.APPROVED.name }
        val scheduled = effectivePosts.count { it.status == PostStatus.SCHEDULED.name }
        val published = effectivePosts.count { it.status == PostStatus.PUBLISHED.name }
        val totalLeads = effectivePosts.sumOf { it.leadsGenerated } + effectiveCampaigns.sumOf { it.responseCount }

        val now = System.currentTimeMillis()
        val sevenDaysAhead = now + (7 * 86400000L)
        val next7Days = effectivePosts.count {
            it.scheduledDateMillis != null && it.scheduledDateMillis in now..sevenDaysAhead
        } + effectiveCampaigns.count {
            it.scheduledDateMillis != null && it.scheduledDateMillis in now..sevenDaysAhead
        }

        val totalDecided = approved + scheduled + published + effectivePosts.count { it.status == PostStatus.REJECTED.name }
        val approvalRate = if (totalDecided > 0) ((approved + scheduled + published) * 100) / totalDecided else 85
        val publishRate = if (effectivePosts.isNotEmpty()) (published * 100) / effectivePosts.size else 40

        DashboardMetrics(
            draftsCount = drafts,
            pendingApprovalCount = pendingApproval,
            approvedCount = approved,
            scheduledCount = scheduled,
            publishedCount = published,
            totalLeads = totalLeads,
            next7DaysCount = next7Days,
            approvalRatePercent = approvalRate,
            publishRatePercent = publishRate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleDataProvider.getInitialMetrics())

    // --- Theme Marketing State (Captions & Post Ideas) ---
    val themeInput = MutableStateFlow("Monsoon Foot Rot & Tick Prevention")
    val themeAudience = MutableStateFlow(AudienceType.CATTLE_OWNERS.name)
    val themePlatform = MutableStateFlow(Platform.FACEBOOK.name)
    val themeTone = MutableStateFlow(ContentTone.EDUCATIONAL.name)
    val themeLanguage = MutableStateFlow(ContentLanguage.HINGLISH.name)

    private val _isGeneratingTheme = MutableStateFlow(false)
    val isGeneratingTheme: StateFlow<Boolean> = _isGeneratingTheme.asStateFlow()

    private val _themeResult = MutableStateFlow<ThemeMarketingResult?>(null)
    val themeResult: StateFlow<ThemeMarketingResult?> = _themeResult.asStateFlow()

    private val _themeStatusMessage = MutableStateFlow<String?>(null)
    val themeStatusMessage: StateFlow<String?> = _themeStatusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _themeResult.value = repository.generateThemeCaptionsAndPostIdeas(
                    theme = themeInput.value,
                    audience = themeAudience.value,
                    platform = themePlatform.value,
                    language = themeLanguage.value,
                    tone = themeTone.value
                )
            } catch (_: Exception) {}
        }
    }

    fun generateThemeCaptionsAndPostIdeas() {
        val theme = themeInput.value.trim()
        if (theme.isBlank()) {
            _themeStatusMessage.value = "Please enter a theme or topic."
            return
        }
        viewModelScope.launch {
            _isGeneratingTheme.value = true
            _themeStatusMessage.value = "Generating captions & marketing post ideas with Gemini..."
            try {
                val result = repository.generateThemeCaptionsAndPostIdeas(
                    theme = theme,
                    audience = themeAudience.value,
                    platform = themePlatform.value,
                    language = themeLanguage.value,
                    tone = themeTone.value
                )
                _themeResult.value = result
                _themeStatusMessage.value = "Generated ${result.captions.size} captions and ${result.postIdeas.size} post ideas!"
            } catch (e: Exception) {
                _themeStatusMessage.value = "Generation failed: ${e.message}"
            } finally {
                _isGeneratingTheme.value = false
            }
        }
    }

    fun saveCaptionAsPost(caption: SocialMediaCaption, status: PostStatus = PostStatus.DRAFT) {
        viewModelScope.launch {
            val post = MarketingPost(
                title = "${themeInput.value} (${caption.style})",
                platform = caption.recommendedPlatform,
                contentType = "Caption Post",
                category = ContentCategory.SEASONAL.name,
                audience = themeAudience.value,
                serviceOrProduct = themeInput.value,
                tone = themeTone.value,
                language = themeLanguage.value,
                ctaText = caption.callToAction,
                status = status.name,
                contentText = caption.captionText,
                hashtags = caption.hashtags.joinToString(" "),
                authorRole = _currentRole.value.name
            )
            repository.savePost(post)
            _themeStatusMessage.value = "Caption saved to Library as ${status.label}!"
        }
    }

    fun savePostIdeaAsPost(idea: MarketingPostIdea, status: PostStatus = PostStatus.DRAFT) {
        viewModelScope.launch {
            val post = MarketingPost(
                title = idea.title,
                platform = themePlatform.value,
                contentType = idea.format,
                category = ContentCategory.SEASONAL.name,
                audience = idea.targetAudience,
                serviceOrProduct = themeInput.value,
                tone = idea.angle,
                language = themeLanguage.value,
                ctaText = idea.callToAction,
                status = status.name,
                contentText = "Angle: ${idea.angle}\n\nKey Takeaway: ${idea.keyTakeaway}\n\nBest Time: ${idea.bestTimeToPost}\n\nCreative Prompt: ${idea.visualCreativePrompt}",
                imagePrompt = idea.visualCreativePrompt,
                authorRole = _currentRole.value.name
            )
            repository.savePost(post)
            _themeStatusMessage.value = "Post Idea saved to Library as ${status.label}!"
        }
    }

    // --- Content Creator State ---
    val creatorPlatform = MutableStateFlow(Platform.WHATSAPP.name)
    val creatorGoal = MutableStateFlow(ContentCategory.VACCINATION.name)
    val creatorAudience = MutableStateFlow(AudienceType.PET_OWNERS.name)
    val creatorService = MutableStateFlow("Anti-Rabies & 7-in-1 Vaccination")
    val creatorTone = MutableStateFlow(ContentTone.PROMOTIONAL.name)
    val creatorLanguage = MutableStateFlow(ContentLanguage.HINGLISH.name)
    val creatorCta = MutableStateFlow("Book Consultation")

    private val _isGeneratingContent = MutableStateFlow(false)
    val isGeneratingContent: StateFlow<Boolean> = _isGeneratingContent.asStateFlow()

    private val _generatedContentBundle = MutableStateFlow<GeneratedContentBundle?>(null)
    val generatedContentBundle: StateFlow<GeneratedContentBundle?> = _generatedContentBundle.asStateFlow()

    private val _contentStatusMessage = MutableStateFlow<String?>(null)
    val contentStatusMessage: StateFlow<String?> = _contentStatusMessage.asStateFlow()

    fun generateMarketingContent() {
        viewModelScope.launch {
            _isGeneratingContent.value = true
            _contentStatusMessage.value = null
            try {
                val bundle = repository.generateMarketingContent(
                    platform = creatorPlatform.value,
                    goal = creatorGoal.value,
                    audience = creatorAudience.value,
                    service = creatorService.value,
                    tone = creatorTone.value,
                    language = creatorLanguage.value,
                    cta = creatorCta.value
                )
                _generatedContentBundle.value = bundle
                _contentStatusMessage.value = "Marketing content generated successfully!"
            } catch (e: Exception) {
                _contentStatusMessage.value = "Generation failed: ${e.message}"
            } finally {
                _isGeneratingContent.value = false
            }
        }
    }

    fun saveCurrentGeneratedPost(status: PostStatus) {
        val bundle = _generatedContentBundle.value ?: return
        viewModelScope.launch {
            val post = MarketingPost(
                title = "${creatorService.value} - ${creatorAudience.value}",
                platform = creatorPlatform.value,
                contentType = if (creatorPlatform.value == Platform.WHATSAPP.name) "WhatsApp Message" else "Post",
                category = creatorGoal.value,
                audience = creatorAudience.value,
                serviceOrProduct = creatorService.value,
                tone = creatorTone.value,
                language = creatorLanguage.value,
                ctaText = creatorCta.value,
                status = status.name,
                contentText = bundle.primaryText,
                shortVersion = bundle.shortVersion,
                longVersion = bundle.longVersion,
                alternate1 = bundle.alternate1,
                alternate2 = bundle.alternate2,
                alternate3 = bundle.alternate3,
                hashtags = bundle.hashtags,
                imagePrompt = bundle.imagePrompt,
                videoPrompt = bundle.videoPrompt,
                authorRole = _currentRole.value.name
            )
            repository.savePost(post)
            _contentStatusMessage.value = "Post saved as ${status.label} in Library!"
        }
    }

    // --- Video Maker State ---
    val videoTitle = MutableStateFlow("Stop Cow Milk Drop in Summer")
    val videoCategory = MutableStateFlow(ContentCategory.CATTLE_HEALTH.name)
    val videoDurationSec = MutableStateFlow(30)
    val videoAspectRatio = MutableStateFlow("9:16")
    val videoLanguage = MutableStateFlow(ContentLanguage.HINDI.name)

    private val _isGeneratingVideo = MutableStateFlow(false)
    val isGeneratingVideo: StateFlow<Boolean> = _isGeneratingVideo.asStateFlow()

    private val _generatedVideoBundle = MutableStateFlow<GeneratedVideoScriptBundle?>(null)
    val generatedVideoBundle: StateFlow<GeneratedVideoScriptBundle?> = _generatedVideoBundle.asStateFlow()

    private val _videoStatusMessage = MutableStateFlow<String?>(null)
    val videoStatusMessage: StateFlow<String?> = _videoStatusMessage.asStateFlow()

    fun generateVideoScript() {
        viewModelScope.launch {
            _isGeneratingVideo.value = true
            _videoStatusMessage.value = null
            try {
                val script = repository.generateVideoScript(
                    title = videoTitle.value,
                    category = videoCategory.value,
                    durationSec = videoDurationSec.value,
                    aspectRatio = videoAspectRatio.value,
                    language = videoLanguage.value
                )
                _generatedVideoBundle.value = script
                _videoStatusMessage.value = "Video script generated successfully!"
            } catch (e: Exception) {
                _videoStatusMessage.value = "Script generation failed: ${e.message}"
            } finally {
                _isGeneratingVideo.value = false
            }
        }
    }

    fun saveCurrentVideoScript(status: PostStatus = PostStatus.DRAFT) {
        val bundle = _generatedVideoBundle.value ?: return
        viewModelScope.launch {
            val script = VideoScript(
                title = bundle.title,
                category = videoCategory.value,
                durationSec = videoDurationSec.value,
                hookText = bundle.hookText,
                sceneBreakdownJson = bundle.scenesJson,
                voiceoverText = bundle.voiceoverText,
                onScreenText = bundle.onScreenText,
                ctaEnding = bundle.ctaEnding,
                aspectRatio = videoAspectRatio.value,
                status = status.name
            )
            repository.saveVideoScript(script)
            _videoStatusMessage.value = "Video script saved to Library!"
        }
    }

    // --- Veo 3 Video Generation State & Operations ---
    val allVeoVideos: StateFlow<List<GeneratedVeoVideo>> = repository.allVeoVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val veoPrompt = MutableStateFlow("Veterinarian examining a healthy dairy cow in modern Indian cattle shed with morning sunlight and lush background")
    val veoAspectRatio = MutableStateFlow("9:16") // "9:16" or "16:9"

    val veoSelectedImageUri = MutableStateFlow<String?>(null)
    val veoImagePrompt = MutableStateFlow("Cinematic slow motion animation of dairy cow grazing in green meadow, subtle breeze, camera gentle dolly push")
    val veoImageAspectRatio = MutableStateFlow("9:16")

    private val _isGeneratingVeoVideo = MutableStateFlow(false)
    val isGeneratingVeoVideo: StateFlow<Boolean> = _isGeneratingVeoVideo.asStateFlow()

    private val _lastGeneratedVeoVideo = MutableStateFlow<VeoVideoResult?>(null)
    val lastGeneratedVeoVideo: StateFlow<VeoVideoResult?> = _lastGeneratedVeoVideo.asStateFlow()

    private val _veoStatusMessage = MutableStateFlow<String?>(null)
    val veoStatusMessage: StateFlow<String?> = _veoStatusMessage.asStateFlow()

    fun generateVeoTextToVideo() {
        val prompt = veoPrompt.value.trim()
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isGeneratingVeoVideo.value = true
            _veoStatusMessage.value = "Generating video with Veo 3 (veo-3.1-fast-generate-preview)..."
            try {
                val result = repository.generateVeoTextToVideo(
                    prompt = prompt,
                    aspectRatio = veoAspectRatio.value
                )
                _lastGeneratedVeoVideo.value = result
                _veoStatusMessage.value = result.message
            } catch (e: Exception) {
                _veoStatusMessage.value = "Veo generation failed: ${e.message}"
            } finally {
                _isGeneratingVeoVideo.value = false
            }
        }
    }

    fun generateVeoImageToVideo(imageBytes: ByteArray?, mimeType: String?) {
        val prompt = veoImagePrompt.value.trim()
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isGeneratingVeoVideo.value = true
            _veoStatusMessage.value = "Animating image with Veo 3 (veo-3.1-fast-generate-preview)..."
            try {
                val result = repository.generateVeoImageToVideo(
                    imageBytes = imageBytes,
                    mimeType = mimeType,
                    prompt = prompt,
                    aspectRatio = veoImageAspectRatio.value,
                    sourceImageUri = veoSelectedImageUri.value
                )
                _lastGeneratedVeoVideo.value = result
                _veoStatusMessage.value = result.message
            } catch (e: Exception) {
                _veoStatusMessage.value = "Veo image animation failed: ${e.message}"
            } finally {
                _isGeneratingVeoVideo.value = false
            }
        }
    }

    fun deleteVeoVideo(id: Long) {
        viewModelScope.launch {
            repository.deleteVeoVideo(id)
        }
    }

    // --- WhatsApp Campaign Management ---
    fun saveNewCampaign(
        name: String,
        targetGroup: String,
        template: String,
        ctaType: String,
        ctaValue: String,
        scheduledMillis: Long? = null
    ) {
        viewModelScope.launch {
            val campaign = WhatsAppCampaign(
                campaignName = name,
                targetGroup = targetGroup,
                templateBody = template,
                buttonCtaType = ctaType,
                buttonCtaValue = ctaValue,
                totalRecipients = when (targetGroup) {
                    AudienceType.CATTLE_OWNERS.name -> 280
                    AudienceType.PET_OWNERS.name -> 140
                    AudienceType.GOAT_FARMERS.name -> 95
                    else -> 60
                },
                status = if (scheduledMillis != null) PostStatus.SCHEDULED.name else PostStatus.DRAFT.name,
                scheduledDateMillis = scheduledMillis
            )
            repository.saveCampaign(campaign)
        }
    }

    fun approveCampaign(campaign: WhatsAppCampaign) {
        viewModelScope.launch {
            repository.updateCampaign(campaign.copy(status = PostStatus.APPROVED.name))
        }
    }

    fun launchWhatsAppCampaign(campaign: WhatsAppCampaign, context: Context) {
        viewModelScope.launch {
            // Update status to sent / completed
            val updated = campaign.copy(
                status = PostStatus.PUBLISHED.name,
                sentCount = campaign.totalRecipients - 3,
                failedCount = 3,
                responseCount = (campaign.totalRecipients * 0.18).toInt()
            )
            repository.updateCampaign(updated)

            // Direct WhatsApp intent launch
            val sampleMessage = campaign.templateBody
                .replace("{{CustomerName}}", "Rohit Veterinary Client")
                .replace("{{AnimalType}}", "Animal")
                .replace("{{ClinicPhone}}", "+91 98765 43210")

            try {
                val encoded = URLEncoder.encode(sampleMessage, "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?text=$encoded")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to generic share
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, sampleMessage)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Campaign via"))
            }
        }
    }

    // --- Post Approval Workflow ---
    fun submitForApproval(postId: Long) {
        viewModelScope.launch {
            repository.submitForApproval(postId)
            _contentStatusMessage.value = "Post submitted for approval!"
        }
    }

    fun approvePost(postId: Long) {
        viewModelScope.launch {
            repository.approvePost(postId)
            _contentStatusMessage.value = "Post approved successfully!"
        }
    }

    fun rejectPost(postId: Long, reason: String) {
        viewModelScope.launch {
            repository.rejectPost(postId, reason)
            _contentStatusMessage.value = "Post rejected with feedback."
        }
    }

    fun schedulePost(postId: Long, scheduledMillis: Long) {
        viewModelScope.launch {
            repository.schedulePost(postId, scheduledMillis)
        }
    }

    fun publishPost(postId: Long, context: Context? = null) {
        viewModelScope.launch {
            repository.markPostPublished(postId)
            if (context != null) {
                val post = allPosts.value.find { it.id == postId }
                if (post != null) {
                    sharePostContent(post, context)
                }
            }
        }
    }

    fun duplicatePost(post: MarketingPost) {
        viewModelScope.launch {
            repository.duplicatePost(post)
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun sharePostContent(post: MarketingPost, context: Context) {
        try {
            val shareText = "${post.contentText}\n\n${post.hashtags}"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, post.title)
                type = "text/plain"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(sendIntent, "Publish / Share via"))
        } catch (e: Exception) {
            // Ignored or logged
        }
    }

    // --- Chat Assistant State & Operations ---
    private val _isSendingChatMessage = MutableStateFlow(false)
    val isSendingChatMessage: StateFlow<Boolean> = _isSendingChatMessage.asStateFlow()

    // Model choices: "gemini-3.1-pro-preview", "gemini-3.5-flash", "gemini-3.1-flash-lite"
    val selectedChatModel = MutableStateFlow("gemini-3.5-flash")

    // Role choices: "Compliance & Strategy", "Viral Social & WhatsApp", "Rapid Copy & Slogans", "Live Intel & Outbreaks"
    val selectedChatRole = MutableStateFlow("Compliance & Strategy")

    // Search Grounding toggle (uses gemini-3.5-flash with googleSearch tool)
    val isSearchGroundingEnabled = MutableStateFlow(true)

    fun setChatModel(model: String) {
        selectedChatModel.value = model
    }

    fun setChatRole(role: String) {
        selectedChatRole.value = role
    }

    fun toggleSearchGrounding(enabled: Boolean) {
        isSearchGroundingEnabled.value = enabled
        if (enabled && selectedChatModel.value != "gemini-3.5-flash") {
            // Search grounding is supported on gemini-3.5-flash
            selectedChatModel.value = "gemini-3.5-flash"
        }
    }

    private fun getSystemInstructionForRole(role: String): String {
        return when (role) {
            "Compliance & Strategy" ->
                "You are the Chief Veterinary Marketing & Compliance Officer for Rohit Veterinary House (+91 98765 43210). Adhere strictly to Indian clinical veterinary guidelines: never make exaggerated or false medical promises, never guarantee cures, and emphasize qualified veterinary consultation, timely prevention, and proper medicine cold-chain storage."
            "Viral Social & WhatsApp" ->
                "You are the Viral Social & WhatsApp Growth Strategist for Rohit Veterinary House. Craft engaging WhatsApp broadcasts, Facebook posts, and 9:16 vertical Reel hooks for Indian farmers and pet owners in natural Hinglish, Hindi, and English with high engagement, authentic village/clinic context, and direct call-to-actions."
            "Rapid Copy & Slogans" ->
                "You are the Rapid Copywriter for Rohit Veterinary House. Deliver concise, catchy 1-line slogans, SMS reminders, snappy social headlines, and polished copy immediately. Optimize for high speed, impact, and brevity."
            "Live Intel & Outbreaks" ->
                "You are the Veterinary Market Intelligence & Outbreak Research Specialist for Rohit Veterinary House. You provide up-to-date information on seasonal livestock diseases, government veterinary schemes (e.g. Rashtriya Gokul Mission, FMD drives), current livestock feed trends, and regional outbreak alerts using real-time search data."
            else ->
                "You are the dedicated Marketing AI Assistant for Rohit Veterinary House, a premier veterinary care clinic and animal health center in India."
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isSendingChatMessage.value = true
            try {
                val systemRole = getSystemInstructionForRole(selectedChatRole.value)
                repository.sendChatMessage(
                    userText = text,
                    modelName = selectedChatModel.value,
                    systemRole = systemRole,
                    enableSearchGrounding = isSearchGroundingEnabled.value
                )
            } finally {
                _isSendingChatMessage.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            _guidedStep.value = GuidedStep.PLATFORM
            _guidedInputs.value = GuidedInputs()
            _guidedResult.value = null
        }
    }

    // --- Guided Marketing Assistant Workflow ---
    private val _guidedStep = MutableStateFlow(GuidedStep.PLATFORM)
    val guidedStep: StateFlow<GuidedStep> = _guidedStep.asStateFlow()

    private val _guidedInputs = MutableStateFlow(GuidedInputs())
    val guidedInputs: StateFlow<GuidedInputs> = _guidedInputs.asStateFlow()

    private val _isGeneratingGuided = MutableStateFlow(false)
    val isGeneratingGuided: StateFlow<Boolean> = _isGeneratingGuided.asStateFlow()

    private val _guidedResult = MutableStateFlow<GuidedContentResult?>(null)
    val guidedResult: StateFlow<GuidedContentResult?> = _guidedResult.asStateFlow()

    private val _isGuidedMode = MutableStateFlow(true)
    val isGuidedMode: StateFlow<Boolean> = _isGuidedMode.asStateFlow()

    fun setGuidedMode(guided: Boolean) {
        _isGuidedMode.value = guided
    }

    fun selectGuidedPlatform(platform: String) {
        _guidedInputs.value = _guidedInputs.value.copy(platform = platform)
        recordGuidedTurn("Platform: $platform", GuidedStep.AUDIENCE)
    }

    fun selectGuidedAudience(audience: String) {
        _guidedInputs.value = _guidedInputs.value.copy(audience = audience)
        recordGuidedTurn("Target Audience: $audience", GuidedStep.GOAL)
    }

    fun selectGuidedGoal(goal: String) {
        _guidedInputs.value = _guidedInputs.value.copy(goal = goal)
        recordGuidedTurn("Campaign Goal: $goal", GuidedStep.SERVICE)
    }

    fun submitGuidedService(service: String) {
        if (service.isBlank()) return
        _guidedInputs.value = _guidedInputs.value.copy(serviceOrProduct = service.trim())
        recordGuidedTurn("Service/Product: ${service.trim()}", GuidedStep.LANGUAGE)
    }

    fun selectGuidedLanguage(language: String) {
        _guidedInputs.value = _guidedInputs.value.copy(language = language)
        recordGuidedTurn("Language: $language", GuidedStep.TONE)
    }

    fun selectGuidedTone(tone: String) {
        _guidedInputs.value = _guidedInputs.value.copy(tone = tone)
        recordGuidedTurn("Tone: $tone", GuidedStep.CTA)
    }

    fun selectGuidedCta(cta: String) {
        _guidedInputs.value = _guidedInputs.value.copy(cta = cta)
        recordGuidedTurn("Call to Action: $cta", GuidedStep.LENGTH)
    }

    fun selectGuidedLength(length: String) {
        _guidedInputs.value = _guidedInputs.value.copy(length = length)
        recordGuidedTurn("Length: $length", GuidedStep.RESULT)
        executeGuidedGeneration()
    }

    private fun recordGuidedTurn(userChoiceText: String, nextStep: GuidedStep) {
        viewModelScope.launch {
            repository.insertChatMessage(ChatMessage(sender = "USER", text = userChoiceText))
            _guidedStep.value = nextStep
        }
    }

    fun executeGuidedGeneration() {
        viewModelScope.launch {
            _isGeneratingGuided.value = true
            try {
                val inp = _guidedInputs.value
                val bundle = repository.generateGuidedMarketingContent(
                    platform = inp.platform.ifBlank { "WhatsApp" },
                    audience = inp.audience.ifBlank { "General animal owners" },
                    goal = inp.goal.ifBlank { "Awareness" },
                    service = inp.serviceOrProduct.ifBlank { "Veterinary Care & Supplements" },
                    language = inp.language.ifBlank { "Hindi" },
                    tone = inp.tone.ifBlank { "professional" },
                    cta = inp.cta.ifBlank { "Call now" },
                    length = inp.length.ifBlank { "medium" }
                )

                val result = GuidedContentResult(
                    mainContent = bundle.mainContent,
                    alternate1 = bundle.alternate1,
                    alternate2 = bundle.alternate2,
                    alternate3 = bundle.alternate3,
                    hashtags = bundle.hashtags,
                    shortCta = bundle.shortCta,
                    imagePrompt = bundle.imagePrompt,
                    videoPrompt = bundle.videoPrompt,
                    platform = inp.platform.ifBlank { "WhatsApp" },
                    audience = inp.audience.ifBlank { "General animal owners" },
                    service = inp.serviceOrProduct.ifBlank { "Veterinary Care" },
                    language = inp.language.ifBlank { "Hindi" },
                    tone = inp.tone.ifBlank { "professional" },
                    cta = inp.cta.ifBlank { "Call now" },
                    length = inp.length.ifBlank { "medium" },
                    savedPostId = null,
                    currentStatus = null
                )
                _guidedResult.value = result
                _guidedStep.value = GuidedStep.RESULT

                repository.insertChatMessage(
                    ChatMessage(
                        sender = "ASSISTANT",
                        text = "🎉 I have generated your customized marketing package for Rohit Veterinary House!\n\n${bundle.mainContent}\n\n${bundle.shortCta}"
                    )
                )
            } finally {
                _isGeneratingGuided.value = false
            }
        }
    }

    fun resetGuidedFlow() {
        _guidedStep.value = GuidedStep.PLATFORM
        _guidedInputs.value = GuidedInputs()
        _guidedResult.value = null
        viewModelScope.launch {
            repository.insertChatMessage(
                ChatMessage(
                    sender = "ASSISTANT",
                    text = "Welcome to Guided Marketing Assistant! 🐾\nLet's craft high-converting content for Rohit Veterinary House.\n\n👉 Step 1: Which platform would you like to create content for?"
                )
            )
        }
    }

    fun saveGuidedResultToDraft() {
        val current = _guidedResult.value ?: return
        viewModelScope.launch {
            val title = "${current.platform} - ${current.service.ifBlank { "Veterinary Post" }}"
            val post = MarketingPost(
                id = current.savedPostId ?: 0,
                title = title,
                platform = current.platform,
                category = "Veterinary Healthcare",
                audience = current.audience,
                serviceOrProduct = current.service,
                tone = current.tone,
                language = current.language,
                ctaText = current.cta,
                status = PostStatus.DRAFT.name,
                contentText = current.mainContent,
                shortVersion = current.shortCta,
                longVersion = current.alternate1,
                alternate1 = current.alternate1,
                alternate2 = current.alternate2,
                alternate3 = current.alternate3,
                hashtags = current.hashtags,
                imagePrompt = current.imagePrompt,
                videoPrompt = current.videoPrompt,
                authorRole = _currentRole.value.name
            )
            val id = repository.savePost(post)
            _guidedResult.value = current.copy(savedPostId = id, currentStatus = PostStatus.DRAFT)
            _contentStatusMessage.value = "Saved to Drafts successfully!"
        }
    }

    fun approveGuidedResult() {
        val current = _guidedResult.value ?: return
        viewModelScope.launch {
            val title = "${current.platform} - ${current.service.ifBlank { "Veterinary Post" }}"
            val post = MarketingPost(
                id = current.savedPostId ?: 0,
                title = title,
                platform = current.platform,
                category = "Veterinary Healthcare",
                audience = current.audience,
                serviceOrProduct = current.service,
                tone = current.tone,
                language = current.language,
                ctaText = current.cta,
                status = PostStatus.APPROVED.name,
                contentText = current.mainContent,
                shortVersion = current.shortCta,
                longVersion = current.alternate1,
                alternate1 = current.alternate1,
                alternate2 = current.alternate2,
                alternate3 = current.alternate3,
                hashtags = current.hashtags,
                imagePrompt = current.imagePrompt,
                videoPrompt = current.videoPrompt,
                authorRole = _currentRole.value.name
            )
            val id = repository.savePost(post)
            _guidedResult.value = current.copy(savedPostId = id, currentStatus = PostStatus.APPROVED)
            _contentStatusMessage.value = "Post Approved successfully!"
        }
    }

    fun scheduleGuidedResult(scheduledTimeMillis: Long) {
        val current = _guidedResult.value ?: return
        viewModelScope.launch {
            val title = "${current.platform} - ${current.service.ifBlank { "Veterinary Post" }}"
            val post = MarketingPost(
                id = current.savedPostId ?: 0,
                title = title,
                platform = current.platform,
                category = "Veterinary Healthcare",
                audience = current.audience,
                serviceOrProduct = current.service,
                tone = current.tone,
                language = current.language,
                ctaText = current.cta,
                status = PostStatus.SCHEDULED.name,
                contentText = current.mainContent,
                shortVersion = current.shortCta,
                longVersion = current.alternate1,
                alternate1 = current.alternate1,
                alternate2 = current.alternate2,
                alternate3 = current.alternate3,
                hashtags = current.hashtags,
                imagePrompt = current.imagePrompt,
                videoPrompt = current.videoPrompt,
                scheduledDateMillis = scheduledTimeMillis,
                authorRole = _currentRole.value.name
            )
            val id = repository.savePost(post)
            _guidedResult.value = current.copy(savedPostId = id, currentStatus = PostStatus.SCHEDULED)
            _contentStatusMessage.value = "Post Scheduled successfully!"
        }
    }

    fun updateGuidedResult(result: GuidedContentResult) {
        _guidedResult.value = result
    }

    fun regenerateGuidedResult() {
        executeGuidedGeneration()
    }

    // --- Structured Facebook Post Builder ---
    val fbHeadline = MutableStateFlow("Special Pet Health & Deworming Camp")
    val fbCaption = MutableStateFlow("Protect your furry companions! This week at Rohit Veterinary House, get complete clinical checkup and seasonal booster vaccines with guaranteed cold-chain safety. Expert veterinary doctors available.")
    val fbCta = MutableStateFlow("Send WhatsApp Message")
    val fbHashtags = MutableStateFlow("#RohitVeterinaryHouse #VeterinaryClinic #PetCare #DogVaccination #AnimalHospital")
    val fbImagePrompt = MutableStateFlow("Smiling veterinarian examining a healthy Golden Retriever in clean clinic, warm professional lighting, high resolution banner.")
    val fbAudience = MutableStateFlow("Pet owners")
    val fbLanguage = MutableStateFlow("Hinglish")

    fun saveStructuredFacebookPost(asStatus: PostStatus) {
        viewModelScope.launch {
            val headline = fbHeadline.value.ifBlank { "Rohit Veterinary House" }
            val post = MarketingPost(
                title = headline,
                platform = Platform.FACEBOOK.name,
                contentType = "Post",
                category = "Veterinary Healthcare",
                audience = fbAudience.value,
                serviceOrProduct = headline,
                tone = "Professional & Inviting",
                language = fbLanguage.value,
                ctaText = fbCta.value,
                status = asStatus.name,
                contentText = "${headline}\n\n${fbCaption.value}",
                shortVersion = headline,
                longVersion = fbCaption.value,
                alternate1 = "",
                alternate2 = "",
                alternate3 = "",
                hashtags = fbHashtags.value,
                imagePrompt = fbImagePrompt.value,
                videoPrompt = "",
                authorRole = _currentRole.value.name
            )
            repository.savePost(post)
            _contentStatusMessage.value = "Facebook post saved as ${asStatus.name.replace("_", " ")}!"
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetDemoData()
            _contentStatusMessage.value = "Demo data reset successfully!"
        }
    }

    fun isGeminiOnline(): Boolean = repository.isGeminiConfigured()
}

enum class GuidedStep(val title: String, val stepNumber: Int) {
    PLATFORM("Platform", 1),
    AUDIENCE("Audience", 2),
    GOAL("Goal", 3),
    SERVICE("Service/Product", 4),
    LANGUAGE("Language", 5),
    TONE("Tone", 6),
    CTA("CTA", 7),
    LENGTH("Length", 8),
    RESULT("Generated Content", 9)
}

data class GuidedInputs(
    val platform: String = "",
    val audience: String = "",
    val goal: String = "",
    val serviceOrProduct: String = "",
    val language: String = "",
    val tone: String = "",
    val cta: String = "",
    val length: String = ""
)

data class GuidedContentResult(
    val mainContent: String = "",
    val alternate1: String = "",
    val alternate2: String = "",
    val alternate3: String = "",
    val hashtags: String = "",
    val shortCta: String = "",
    val imagePrompt: String = "",
    val videoPrompt: String = "",
    val platform: String = "",
    val audience: String = "",
    val service: String = "",
    val language: String = "",
    val tone: String = "",
    val cta: String = "",
    val length: String = "",
    val savedPostId: Long? = null,
    val currentStatus: PostStatus? = null
)

class MarketingViewModelFactory(
    private val repository: MarketingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarketingViewModel::class.java)) {
            return MarketingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
