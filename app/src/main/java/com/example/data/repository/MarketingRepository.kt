package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.gemini.GeminiMarketingService
import com.example.data.gemini.GeneratedContentBundle
import com.example.data.gemini.GeneratedVideoScriptBundle
import com.example.data.gemini.ThemeMarketingResult
import com.example.data.gemini.VeoVideoResult
import com.example.data.model.ChatMessage
import com.example.data.model.GeneratedVeoVideo
import com.example.data.model.MarketingPost
import com.example.data.model.PostStatus
import com.example.data.model.VideoScript
import com.example.data.model.WhatsAppCampaign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class MarketingRepository(
    private val database: AppDatabase,
    private val geminiService: GeminiMarketingService
) {
    val allPosts: Flow<List<MarketingPost>> = database.postDao().getAllPosts()
    val allVideoScripts: Flow<List<VideoScript>> = database.videoScriptDao().getAllVideoScripts()
    val allCampaigns: Flow<List<WhatsAppCampaign>> = database.whatsappCampaignDao().getAllCampaigns()
    val allChatMessages: Flow<List<ChatMessage>> = database.chatMessageDao().getAllMessages()
    val allVeoVideos: Flow<List<GeneratedVeoVideo>> = database.veoVideoDao().getAllVideos()

    suspend fun savePost(post: MarketingPost): Long {
        return database.postDao().insertPost(post)
    }

    suspend fun updatePost(post: MarketingPost) {
        database.postDao().updatePost(post)
    }

    suspend fun deletePost(id: Long) {
        database.postDao().deletePost(id)
    }

    suspend fun duplicatePost(original: MarketingPost): Long {
        val copy = original.copy(
            id = 0,
            title = "${original.title} (Copy)",
            status = PostStatus.DRAFT.name,
            scheduledDateMillis = null,
            publishedDateMillis = null,
            createdDateMillis = System.currentTimeMillis(),
            leadsGenerated = 0,
            clicks = 0,
            rejectionReason = null
        )
        return database.postDao().insertPost(copy)
    }

    suspend fun submitForApproval(id: Long) {
        val post = database.postDao().getPostById(id) ?: return
        database.postDao().updatePost(
            post.copy(
                status = PostStatus.PENDING_APPROVAL.name,
                rejectionReason = null
            )
        )
    }

    suspend fun approvePost(id: Long) {
        val post = database.postDao().getPostById(id) ?: return
        database.postDao().updatePost(
            post.copy(
                status = PostStatus.APPROVED.name,
                rejectionReason = null
            )
        )
    }

    suspend fun rejectPost(id: Long, reason: String) {
        val post = database.postDao().getPostById(id) ?: return
        database.postDao().updatePost(
            post.copy(
                status = PostStatus.REJECTED.name,
                rejectionReason = reason
            )
        )
    }

    suspend fun schedulePost(id: Long, timeMillis: Long) {
        val post = database.postDao().getPostById(id) ?: return
        database.postDao().updatePost(
            post.copy(
                status = PostStatus.SCHEDULED.name,
                scheduledDateMillis = timeMillis
            )
        )
    }

    suspend fun markPostPublished(id: Long) {
        val post = database.postDao().getPostById(id) ?: return
        database.postDao().updatePost(
            post.copy(
                status = PostStatus.PUBLISHED.name,
                publishedDateMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveVideoScript(script: VideoScript): Long {
        return database.videoScriptDao().insertVideoScript(script)
    }

    suspend fun updateVideoScript(script: VideoScript) {
        database.videoScriptDao().updateVideoScript(script)
    }

    suspend fun deleteVideoScript(id: Long) {
        database.videoScriptDao().deleteVideoScript(id)
    }

    suspend fun saveCampaign(campaign: WhatsAppCampaign): Long {
        return database.whatsappCampaignDao().insertCampaign(campaign)
    }

    suspend fun updateCampaign(campaign: WhatsAppCampaign) {
        database.whatsappCampaignDao().updateCampaign(campaign)
    }

    suspend fun deleteCampaign(id: Long) {
        database.whatsappCampaignDao().deleteCampaign(id)
    }

    suspend fun saveVeoVideo(video: GeneratedVeoVideo): Long {
        return database.veoVideoDao().insertVideo(video)
    }

    suspend fun deleteVeoVideo(id: Long) {
        database.veoVideoDao().deleteVideo(id)
    }

    suspend fun generateVeoTextToVideo(prompt: String, aspectRatio: String): VeoVideoResult {
        val result = geminiService.generateVeoTextToVideo(prompt, aspectRatio)
        val videoEntity = GeneratedVeoVideo(
            prompt = result.prompt,
            model = result.model,
            aspectRatio = result.aspectRatio,
            isImageToVideo = false,
            videoUrl = result.videoUrl,
            status = if (result.isSuccess) "COMPLETED" else "FAILED"
        )
        database.veoVideoDao().insertVideo(videoEntity)
        return result
    }

    suspend fun generateVeoImageToVideo(
        imageBytes: ByteArray?,
        mimeType: String?,
        prompt: String,
        aspectRatio: String,
        sourceImageUri: String? = null
    ): VeoVideoResult {
        val result = geminiService.generateVeoImageToVideo(
            imageBytes = imageBytes,
            mimeType = mimeType,
            prompt = prompt,
            aspectRatio = aspectRatio,
            sourceImageUri = sourceImageUri
        )
        val videoEntity = GeneratedVeoVideo(
            prompt = result.prompt,
            model = result.model,
            aspectRatio = result.aspectRatio,
            isImageToVideo = true,
            sourceImageUri = sourceImageUri,
            videoUrl = result.videoUrl,
            status = if (result.isSuccess) "COMPLETED" else "FAILED"
        )
        database.veoVideoDao().insertVideo(videoEntity)
        return result
    }

    suspend fun sendChatMessage(
        userText: String,
        modelName: String = "gemini-3.5-flash",
        systemRole: String = "You are the Marketing Assistant for Rohit Veterinary House.",
        enableSearchGrounding: Boolean = false
    ): ChatMessage {
        // Insert user message
        val userMsg = ChatMessage(sender = "USER", text = userText)
        database.chatMessageDao().insertMessage(userMsg)

        // Retrieve recent chat history for context
        val recentMessages = database.chatMessageDao().getAllMessages().first()
        val historyPairs = recentMessages.takeLast(10).map { it.sender to it.text }

        // Query Gemini with selected model, system instruction, and optional Search Grounding
        val aiResult = geminiService.chatWithGeminiModel(
            modelName = modelName,
            systemRole = systemRole,
            conversationHistory = historyPairs,
            userMessage = userText,
            enableSearchGrounding = enableSearchGrounding
        )

        // Serialize search queries and sources
        val queriesJson = if (aiResult.searchQueries.isNotEmpty()) {
            val arr = JSONArray()
            aiResult.searchQueries.forEach { arr.put(it) }
            arr.toString()
        } else null

        val sourcesJson = if (aiResult.sources.isNotEmpty()) {
            val arr = JSONArray()
            aiResult.sources.forEach {
                val obj = JSONObject()
                obj.put("title", it.title)
                obj.put("uri", it.uri)
                arr.put(obj)
            }
            arr.toString()
        } else null

        val assistantMsg = ChatMessage(
            sender = "ASSISTANT",
            text = aiResult.replyText,
            modelUsed = aiResult.modelUsed,
            searchQueries = queriesJson,
            searchSourcesJson = sourcesJson
        )
        database.chatMessageDao().insertMessage(assistantMsg)
        return assistantMsg
    }

    suspend fun insertChatMessage(message: ChatMessage) {
        database.chatMessageDao().insertMessage(message)
    }

    suspend fun clearChatHistory() {
        database.chatMessageDao().clearChat()
        val welcome = ChatMessage(
            sender = "ASSISTANT",
            text = "Namaste! I am your AI Marketing Assistant for Rohit Veterinary House. 🐾🐄\n\nWhat marketing campaign or post would you like to create today? (WhatsApp, Facebook, or Video Script?)"
        )
        database.chatMessageDao().insertMessage(welcome)
    }

    suspend fun generateMarketingContent(
        platform: String,
        goal: String,
        audience: String,
        service: String,
        tone: String,
        language: String,
        cta: String
    ): GeneratedContentBundle {
        return geminiService.generateMarketingContent(platform, goal, audience, service, tone, language, cta)
    }

    suspend fun generateGuidedMarketingContent(
        platform: String,
        audience: String,
        goal: String,
        service: String,
        language: String,
        tone: String,
        cta: String,
        length: String
    ): com.example.data.gemini.GuidedMarketingResult {
        return geminiService.generateGuidedMarketingContent(
            platform, audience, goal, service, language, tone, cta, length
        )
    }

    suspend fun generateVideoScript(
        title: String,
        category: String,
        durationSec: Int,
        aspectRatio: String,
        language: String
    ): GeneratedVideoScriptBundle {
        return geminiService.generateVideoScript(title, category, durationSec, aspectRatio, language)
    }

    suspend fun generateThemeCaptionsAndPostIdeas(
        theme: String,
        audience: String,
        platform: String,
        language: String,
        tone: String
    ): ThemeMarketingResult {
        return geminiService.generateThemeCaptionsAndPostIdeas(
            theme = theme,
            audience = audience,
            platform = platform,
            language = language,
            tone = tone
        )
    }

    fun isGeminiConfigured(): Boolean = geminiService.isApiKeyConfigured()

    suspend fun resetDemoData() {
        database.postDao().clearAllPosts()
        database.videoScriptDao().clearAllVideoScripts()
        database.whatsappCampaignDao().clearAllCampaigns()
        database.chatMessageDao().clearChat()
        database.seedInitialData()
    }
}
