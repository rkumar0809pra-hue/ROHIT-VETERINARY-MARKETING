package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Platform(val label: String) {
    FACEBOOK("Facebook"),
    WHATSAPP("WhatsApp"),
    INSTAGRAM("Instagram"),
    VIDEO("Video Reels/Status")
}

enum class ContentCategory(val label: String) {
    PET_CARE("Pet Care Awareness"),
    CATTLE_HEALTH("Cattle & Dairy Health"),
    GOAT_FARMING("Goat Farming Support"),
    POULTRY_CARE("Poultry Care"),
    VACCINATION("Vaccination Reminder"),
    DEWORMING("Deworming Reminder"),
    SEASONAL("Seasonal Disease Prevention"),
    PRODUCT_PROMO("Product Promotion"),
    CLINIC_CONSULT("Clinic Consultation"),
    EMERGENCY("Emergency Help Messaging"),
    FESTIVAL_OFFER("Festival & Local Campaign")
}

enum class AudienceType(val label: String) {
    PET_OWNERS("Pet Owners (Dogs & Cats)"),
    CATTLE_OWNERS("Cattle & Buffalo Dairy"),
    GOAT_FARMERS("Goat & Sheep Farmers"),
    POULTRY_FARMERS("Poultry Farmers"),
    GENERAL("General Animal Parents")
}

enum class ContentTone(val label: String) {
    PROMOTIONAL("Promotional & Engaging"),
    EDUCATIONAL("Educational & Informative"),
    URGENT("Urgent Health Reminder"),
    FRIENDLY("Warm & Friendly"),
    FESTIVE("Festival Celebration")
}

enum class ContentLanguage(val label: String) {
    HINGLISH("Hinglish"),
    HINDI("Hindi (हिंदी)"),
    ENGLISH("English")
}

enum class PostStatus(val label: String) {
    DRAFT("Draft"),
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved"),
    SCHEDULED("Scheduled"),
    PUBLISHED("Published"),
    REJECTED("Rejected")
}

enum class UserRole(val label: String, val description: String) {
    ADMIN("Admin / Owner", "Can approve, reject, schedule, and publish all marketing materials."),
    MARKETING_STAFF("Marketing Staff", "Can create campaigns, schedule approved posts, and track analytics."),
    CONTENT_CREATOR("Content Creator / Editor", "Can generate posts, create video scripts, and submit for approval.")
}

@Entity(tableName = "marketing_posts")
data class MarketingPost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val platform: String,
    val contentType: String = "Post",
    val category: String,
    val audience: String,
    val serviceOrProduct: String,
    val tone: String,
    val language: String,
    val ctaText: String,
    val status: String = PostStatus.DRAFT.name,
    val contentText: String,
    val shortVersion: String = "",
    val longVersion: String = "",
    val alternate1: String = "",
    val alternate2: String = "",
    val alternate3: String = "",
    val hashtags: String = "",
    val imagePrompt: String = "",
    val videoPrompt: String = "",
    val scheduledDateMillis: Long? = null,
    val createdDateMillis: Long = System.currentTimeMillis(),
    val publishedDateMillis: Long? = null,
    val leadsGenerated: Int = 0,
    val clicks: Int = 0,
    val authorRole: String = UserRole.CONTENT_CREATOR.name,
    val rejectionReason: String? = null
)

@Entity(tableName = "video_scripts")
data class VideoScript(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val durationSec: Int = 30,
    val hookText: String,
    val sceneBreakdownJson: String, // JSON array of scenes
    val voiceoverText: String,
    val onScreenText: String,
    val ctaEnding: String,
    val aspectRatio: String = "9:16",
    val status: String = PostStatus.DRAFT.name,
    val createdDateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "whatsapp_campaigns")
data class WhatsAppCampaign(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignName: String,
    val targetGroup: String,
    val templateBody: String,
    val buttonCtaType: String = "Call Now",
    val buttonCtaValue: String = "+91 98765 43210",
    val totalRecipients: Int = 150,
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val responseCount: Int = 0,
    val status: String = PostStatus.DRAFT.name,
    val scheduledDateMillis: Long? = null,
    val createdDateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "ASSISTANT"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val postPayloadJson: String? = null,
    val modelUsed: String? = null,
    val searchQueries: String? = null,
    val searchSourcesJson: String? = null
)

@Entity(tableName = "veo_videos")
data class GeneratedVeoVideo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val model: String = "veo-3.1-fast-generate-preview",
    val aspectRatio: String = "9:16", // "16:9" or "9:16"
    val isImageToVideo: Boolean = false,
    val sourceImageUri: String? = null,
    val videoUrl: String? = null,
    val status: String = "COMPLETED",
    val createdDateMillis: Long = System.currentTimeMillis()
)
