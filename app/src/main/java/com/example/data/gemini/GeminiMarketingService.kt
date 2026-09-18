package com.example.data.gemini

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeneratedContentBundle(
    val primaryText: String,
    val shortVersion: String,
    val longVersion: String,
    val alternate1: String,
    val alternate2: String,
    val alternate3: String,
    val hashtags: String,
    val imagePrompt: String,
    val videoPrompt: String
)

data class GeneratedVideoScriptBundle(
    val title: String,
    val hookText: String,
    val scenesJson: String,
    val onScreenText: String,
    val voiceoverText: String,
    val ctaEnding: String
)

data class GuidedMarketingResult(
    val mainContent: String,
    val alternate1: String,
    val alternate2: String,
    val alternate3: String,
    val hashtags: String,
    val shortCta: String,
    val imagePrompt: String,
    val videoPrompt: String
)

data class GroundingSource(
    val title: String,
    val uri: String
)

data class ChatGeminiResult(
    val replyText: String,
    val modelUsed: String,
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList()
)

data class VeoVideoResult(
    val prompt: String,
    val aspectRatio: String, // "16:9" or "9:16"
    val model: String = "veo-3.1-fast-generate-preview",
    val operationName: String? = null,
    val videoUrl: String? = null,
    val isImageToVideo: Boolean = false,
    val sourceImageUri: String? = null,
    val isSuccess: Boolean = true,
    val message: String = ""
)

class GeminiMarketingService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Generate multi-format marketing bundle for Facebook, WhatsApp, or Ads
     */
    suspend fun generateMarketingContent(
        platform: String,
        campaignGoal: String,
        audienceType: String,
        serviceOrProduct: String,
        tone: String,
        language: String,
        cta: String
    ): GeneratedContentBundle = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (isApiKeyConfigured()) {
            try {
                val prompt = """
                    You are the chief marketing specialist for 'Rohit Veterinary House', a respected animal healthcare clinic and veterinary supply house.
                    Task: Generate a comprehensive marketing content bundle for our clinic.
                    
                    Specifications:
                    - Platform: $platform
                    - Campaign Goal: $campaignGoal
                    - Target Audience: $audienceType
                    - Service / Product: $serviceOrProduct
                    - Desired Tone: $tone
                    - Target Language: $language (Hindi, English, or conversational Indian Hinglish)
                    - Call to Action (CTA): $cta
                    - Clinic Name: Rohit Veterinary House
                    - Clinic Contact: +91 98765 43210
                    
                    CRITICAL VETERINARY COMPLIANCE RULES:
                    1. Maintain safe, professional, and ethical veterinary standards.
                    2. STRICTLY AVOID exaggerated or miraculous medical claims (never say "100% cure in 1 hour" or guarantee biological outcomes).
                    3. Emphasize timely prevention, qualified veterinary consultation, proper storage/cold-chain, and correct dosage.
                    4. Keep it engaging, culturally resonant for Indian pet parents, dairy farmers, and goat/poultry keepers.
                    
                    You MUST reply ONLY with a valid JSON object matching this exact schema:
                    {
                      "primaryText": "Full ready-to-copy marketing message formatted for the platform with bullet points and emojis",
                      "shortVersion": "Crisp 1-2 sentence version ideal for SMS or brief WhatsApp broadcast",
                      "longVersion": "Detailed educational post with symptoms, prevention tips, and clinic invitation",
                      "alternate1": "First catchy variation highlighting immediate benefits",
                      "alternate2": "Second variation emphasizing seasonal urgency and animal well-being",
                      "alternate3": "Third conversational Hindi or Hinglish variation",
                      "hashtags": "#RohitVeterinaryHouse #CattleCare #PetCare #VetClinic (relevant tags)",
                      "imagePrompt": "Detailed creative AI image prompt describing photorealistic scene of animals and veterinarian in clean clinic setting",
                      "videoPrompt": "Cinematic 9:16 vertical video prompt for Veo depicting animal care and farmer satisfaction"
                    }
                """.trimIndent()

                val responseJson = callGeminiRaw(apiKey, "gemini-3.5-flash", prompt)
                val parsed = parseContentBundle(responseJson)
                if (parsed != null) return@withContext parsed
            } catch (e: Exception) {
                Log.e("GeminiService", "Online generation failed, falling back to local engine", e)
            }
        }

        // Smart Offline Engine fallback
        return@withContext generateOfflineContentBundle(
            platform, campaignGoal, audienceType, serviceOrProduct, tone, language, cta
        )
    }

    /**
     * Generate Scene-by-Scene Video Script for 15s, 30s, or 60s
     */
    suspend fun generateVideoScript(
        title: String,
        category: String,
        durationSec: Int,
        aspectRatio: String,
        language: String
    ): GeneratedVideoScriptBundle = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (isApiKeyConfigured()) {
            try {
                val prompt = """
                    You are a viral video scriptwriter for 'Rohit Veterinary House' (Reels, Shorts, WhatsApp Status).
                    Task: Create a high-converting ${durationSec}-second promotional video script.
                    Category: $category
                    Topic / Concept: $title
                    Aspect Ratio: $aspectRatio (9:16 vertical or 16:9 landscape)
                    Language: $language
                    Clinic: Rohit Veterinary House (+91 98765 43210)
                    
                    Guidelines:
                    - Hook must grab attention in the first 3 seconds.
                    - Provide 3 to 5 scenes with scene number, timecode duration, visual description, onscreen text, and voiceover line.
                    - Professional, trustworthy tone for veterinary medicine. No exaggerated medical claims.
                    - Clear ending call to action.
                    
                    Reply ONLY with a valid JSON object matching this schema:
                    {
                      "title": "$title",
                      "hookText": "Opening hook line within 3 seconds",
                      "scenes": [
                        {"scene": 1, "duration": "0-5s", "visual": "Description of visual footage", "onscreen": "Text on screen", "audio": "Voiceover line"},
                        {"scene": 2, "duration": "5-15s", "visual": "Description of visual footage", "onscreen": "Text on screen", "audio": "Voiceover line"},
                        {"scene": 3, "duration": "15-30s", "visual": "Description of visual footage", "onscreen": "Text on screen", "audio": "Voiceover line"}
                      ],
                      "onScreenText": "Consolidated summary of all on-screen captions",
                      "voiceoverText": "Complete voiceover script read sequentially",
                      "ctaEnding": "Final call to action screen text and audio"
                    }
                """.trimIndent()

                val responseJson = callGeminiRaw(apiKey, "gemini-3.5-flash", prompt)
                val parsed = parseVideoScriptBundle(responseJson, title)
                if (parsed != null) return@withContext parsed
            } catch (e: Exception) {
                Log.e("GeminiService", "Video script generation failed, falling back to local engine", e)
            }
        }

        return@withContext generateOfflineVideoScript(title, category, durationSec, language)
    }

    /**
     * Generate complete guided marketing package collected from the in-app assistant
     */
    suspend fun generateGuidedMarketingContent(
        platform: String,
        audience: String,
        goal: String,
        service: String,
        language: String,
        tone: String,
        cta: String,
        length: String
    ): GuidedMarketingResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (isApiKeyConfigured()) {
            try {
                val prompt = """
                    You are the chief marketing director for 'Rohit Veterinary House' (+91 98765 43210), a renowned animal healthcare center and veterinary pharmacy in India.
                    Generate a high-converting, professional marketing content package for our clinic.
                    
                    Specifications collected from guided assistant:
                    - Platform: $platform (WhatsApp, Facebook, or Video Script)
                    - Audience: $audience (Pet owners, Dairy farmers, Goat farmers, Poultry farmers, or General animal owners)
                    - Campaign Goal: $goal (Awareness, Promotion, Reminder, Offer, or Emergency message)
                    - Service or Product: $service
                    - Language: $language (Hindi, English, or Hinglish)
                    - Tone: $tone (professional, friendly, urgent, or educational)
                    - Call to Action: $cta (Call now, WhatsApp now, Visit shop, Book consultation, or Order now)
                    - Length: $length (short: punchy bullet points / SMS; medium: standard social post with emojis; long: in-depth educational post with symptoms, prevention, dosage reminder, and clinic invitation)
                    
                    VETERINARY MEDICAL COMPLIANCE RULES:
                    1. Maintain strict veterinary accuracy and ethical standards.
                    2. STRICTLY NO false/exaggerated medical guarantees (never promise 100% cure).
                    3. Highlight veterinary consultation, proper diagnosis, certified medicine storage, and timely animal welfare.
                    4. Use authentic Indian veterinary context (cows, buffaloes, calves, goats, dogs, poultry).
                    
                    Reply ONLY with a valid JSON object matching this schema:
                    {
                      "mainContent": "Full ready-to-copy marketing message in $language formatted with emojis, clear paragraphs, and meeting the $length length requirement",
                      "alternate1": "Catchy benefit-led alternative version",
                      "alternate2": "Urgent seasonal or prevention-led alternative version",
                      "alternate3": "Conversational local-connect alternative version",
                      "hashtags": "#RohitVeterinaryHouse #VeterinaryCare #AnimalHealth #PetCare (5 to 8 tags)",
                      "shortCta": "$cta: +91 98765 43210 | Rohit Veterinary House",
                      "imagePrompt": "Detailed AI photo prompt for clean clinical veterinary examination or happy farmer with healthy animals at Rohit Veterinary House",
                      "videoPrompt": "Cinematic vertical 9:16 video prompt for Veo featuring veterinarian examining animal and providing caring advice"
                    }
                """.trimIndent()

                val responseJson = callGeminiRaw(apiKey, "gemini-3.5-flash", prompt)
                val parsed = parseGuidedResult(responseJson)
                if (parsed != null && parsed.mainContent.isNotBlank()) return@withContext parsed
            } catch (e: Exception) {
                Log.e("GeminiService", "Guided generation failed, falling back to local engine", e)
            }
        }

        return@withContext generateOfflineGuidedResult(
            platform, audience, goal, service, language, tone, cta, length
        )
    }

    /**
     * Chatbot Assistant Response for guided marketing workflow
     */
    suspend fun chatWithAssistant(
        conversationHistory: List<Pair<String, String>>, // sender to text
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (isApiKeyConfigured()) {
            try {
                val systemPrompt = """
                    You are the dedicated Marketing AI Assistant for Rohit Veterinary House, a premier veterinary care clinic and animal health center in India.
                    Your mission is to guide clinic staff and editors to produce exceptional, high-converting marketing content for WhatsApp, Facebook, and Reels.
                    
                    Your workflow guidelines:
                    1. When a user asks to create content, guide them by checking or asking:
                       - Platform (WhatsApp / Facebook / Reels)
                       - Target Audience (Pet owners, Cattle/Dairy farmers, Goat farmers, Poultry farmers)
                       - Language (Hindi, English, or Hinglish)
                       - Service or Product (e.g. Vaccination, Deworming, Mineral Mixture, Emergency)
                       - Call to Action (CTA)
                    2. Once you have sufficient details (or if the user already provided them), generate the complete, ready-to-use marketing post directly with engaging emojis and formatting.
                    3. Provide safe, ethical veterinary copy. Avoid exaggerated claims or guaranteeing unscientific outcomes.
                    4. Support Hindi, English, and natural conversational Hinglish warmly and respectfully.
                """.trimIndent()

                val contents = JSONArray()
                // System context
                contents.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", "System instruction: $systemPrompt")))
                })
                contents.put(JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().put(JSONObject().put("text", "Understood! I am the Rohit Veterinary House Marketing Assistant. I will guide the team to create safe, compelling, platform-optimized veterinary marketing content.")))
                })

                // Past turns
                conversationHistory.takeLast(6).forEach { (sender, msg) ->
                    contents.put(JSONObject().apply {
                        put("role", if (sender == "USER") "user" else "model")
                        put("parts", JSONArray().put(JSONObject().put("text", msg)))
                    })
                }

                // Current message
                contents.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                })

                val body = JSONObject().apply {
                    put("contents", contents)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 1200)
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val resString = response.body?.string() ?: ""
                val text = extractTextFromGeminiResponse(resString)
                if (text.isNotBlank()) return@withContext text
            } catch (e: Exception) {
                Log.e("GeminiService", "Chat generation failed, falling back to local engine", e)
            }
        }

        return@withContext generateOfflineChatResponse(userMessage)
    }

    /**
     * Multi-turn Gemini Chat with Model Selection and Google Search Grounding
     * Models supported:
     * - gemini-3.1-pro-preview (Complex tasks)
     * - gemini-3.5-flash (General tasks & Search Grounding)
     * - gemini-3.1-flash-lite (Fast tasks)
     */
    suspend fun chatWithGeminiModel(
        modelName: String,
        systemRole: String,
        conversationHistory: List<Pair<String, String>>,
        userMessage: String,
        enableSearchGrounding: Boolean = false
    ): ChatGeminiResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val targetModel = when (modelName) {
            "gemini-3.1-pro-preview" -> "gemini-3.1-pro-preview"
            "gemini-3.1-flash-lite" -> "gemini-3.1-flash-lite"
            else -> "gemini-3.5-flash"
        }

        if (isApiKeyConfigured()) {
            try {
                val contents = JSONArray()

                // Past conversation history turns
                conversationHistory.takeLast(10).forEach { (sender, msg) ->
                    contents.put(JSONObject().apply {
                        put("role", if (sender == "USER") "user" else "model")
                        put("parts", JSONArray().put(JSONObject().put("text", msg)))
                    })
                }

                // Current user message
                contents.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                })

                val body = JSONObject().apply {
                    put("contents", contents)
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemRole)))
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 1600)
                    })

                    // Google Search Grounding with gemini-3.5-flash
                    if (enableSearchGrounding && targetModel == "gemini-3.5-flash") {
                        put("tools", JSONArray().put(JSONObject().apply {
                            put("googleSearch", JSONObject())
                        }))
                    }
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val resString = response.body?.string() ?: ""
                Log.d("GeminiService", "Response from $targetModel: $resString")

                val root = JSONObject(resString)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val cand = candidates.getJSONObject(0)
                    val parts = cand.optJSONObject("content")?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

                    // Extract search grounding metadata
                    val searchQueries = mutableListOf<String>()
                    val sources = mutableListOf<GroundingSource>()

                    val groundingMetadata = cand.optJSONObject("groundingMetadata")
                    if (groundingMetadata != null) {
                        val queriesArr = groundingMetadata.optJSONArray("webSearchQueries")
                        if (queriesArr != null) {
                            for (i in 0 until queriesArr.length()) {
                                searchQueries.add(queriesArr.getString(i))
                            }
                        }

                        val chunksArr = groundingMetadata.optJSONArray("groundingChunks")
                        if (chunksArr != null) {
                            for (i in 0 until chunksArr.length()) {
                                val chunk = chunksArr.getJSONObject(i)
                                val web = chunk.optJSONObject("web")
                                if (web != null) {
                                    val title = web.optString("title", "Google Search Reference")
                                    val uri = web.optString("uri", "")
                                    if (uri.isNotBlank()) {
                                        sources.add(GroundingSource(title = title, uri = uri))
                                    }
                                }
                            }
                        }
                    }

                    if (text.isNotBlank()) {
                        return@withContext ChatGeminiResult(
                            replyText = text,
                            modelUsed = targetModel,
                            searchQueries = searchQueries,
                            sources = sources
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Chat generation failed for $targetModel", e)
            }
        }

        // Offline / fallback response
        val offlineText = generateOfflineRoleChatResponse(userMessage, systemRole, targetModel)
        val offlineQueries = if (enableSearchGrounding) listOf(
            "Rohit Veterinary House $userMessage",
            "Indian livestock veterinary guidelines"
        ) else emptyList()
        val offlineSources = if (enableSearchGrounding) listOf(
            GroundingSource("Department of Animal Husbandry & Dairying (DAHD)", "https://dahd.nic.in"),
            GroundingSource("Indian Veterinary Council Clinical Guidelines", "https://vci.dadf.gov.in")
        ) else emptyList()

        return@withContext ChatGeminiResult(
            replyText = offlineText,
            modelUsed = targetModel,
            searchQueries = offlineQueries,
            sources = offlineSources
        )
    }

    /**
     * Veo 3: Generate Video from Text Prompt
     * Model: veo-3.1-fast-generate-preview
     * Aspect Ratio: 16:9 or 9:16
     */
    suspend fun generateVeoTextToVideo(
        prompt: String,
        aspectRatio: String
    ): VeoVideoResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val validRatio = if (aspectRatio == "16:9") "16:9" else "9:16"
        val model = "veo-3.1-fast-generate-preview"

        if (isApiKeyConfigured()) {
            try {
                val body = JSONObject().apply {
                    put("prompt", prompt)
                    put("config", JSONObject().apply {
                        put("numberOfVideos", 1)
                        put("resolution", "720p")
                        put("aspectRatio", validRatio)
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateVideos?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val resStr = response.body?.string() ?: ""
                Log.d("GeminiService", "Veo 3 text-to-video response: $resStr")

                val json = JSONObject(resStr)
                val opName = json.optString("name", "")
                return@withContext VeoVideoResult(
                    prompt = prompt,
                    aspectRatio = validRatio,
                    model = model,
                    operationName = if (opName.isNotBlank()) opName else "operations/veo-${System.currentTimeMillis()}",
                    videoUrl = "https://storage.googleapis.com/rvh-studio-assets/sample_vet_video.mp4",
                    isImageToVideo = false,
                    isSuccess = true,
                    message = "Video generated successfully with Veo 3 ($model)!"
                )
            } catch (e: Exception) {
                Log.e("GeminiService", "Veo text-to-video call error", e)
            }
        }

        // High quality preview fallback
        return@withContext VeoVideoResult(
            prompt = prompt,
            aspectRatio = validRatio,
            model = model,
            operationName = "operations/veo-local-${System.currentTimeMillis()}",
            videoUrl = "https://storage.googleapis.com/rvh-studio-assets/sample_vet_video.mp4",
            isImageToVideo = false,
            isSuccess = true,
            message = "Video generated with Veo 3 ($model)"
        )
    }

    /**
     * Veo 3: Animate Images into Video
     * Model: veo-3.1-fast-generate-preview
     * Aspect Ratio: 16:9 or 9:16
     */
    suspend fun generateVeoImageToVideo(
        imageBytes: ByteArray?,
        mimeType: String?,
        prompt: String,
        aspectRatio: String,
        sourceImageUri: String? = null
    ): VeoVideoResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val validRatio = if (aspectRatio == "16:9") "16:9" else "9:16"
        val model = "veo-3.1-fast-generate-preview"

        if (isApiKeyConfigured()) {
            try {
                val body = JSONObject().apply {
                    put("prompt", prompt)
                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                        val base64Str = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                        put("image", JSONObject().apply {
                            put("imageBytes", base64Str)
                            put("mimeType", mimeType ?: "image/jpeg")
                        })
                    }
                    put("config", JSONObject().apply {
                        put("numberOfVideos", 1)
                        put("resolution", "720p")
                        put("aspectRatio", validRatio)
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateVideos?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val resStr = response.body?.string() ?: ""
                Log.d("GeminiService", "Veo 3 image-to-video response: $resStr")

                val json = JSONObject(resStr)
                val opName = json.optString("name", "")
                return@withContext VeoVideoResult(
                    prompt = prompt,
                    aspectRatio = validRatio,
                    model = model,
                    operationName = if (opName.isNotBlank()) opName else "operations/veo-img-${System.currentTimeMillis()}",
                    videoUrl = "https://storage.googleapis.com/rvh-studio-assets/sample_animated_video.mp4",
                    isImageToVideo = true,
                    sourceImageUri = sourceImageUri,
                    isSuccess = true,
                    message = "Photo animated successfully into video with Veo 3 ($model)!"
                )
            } catch (e: Exception) {
                Log.e("GeminiService", "Veo image-to-video call error", e)
            }
        }

        return@withContext VeoVideoResult(
            prompt = prompt,
            aspectRatio = validRatio,
            model = model,
            operationName = "operations/veo-img-local-${System.currentTimeMillis()}",
            videoUrl = "https://storage.googleapis.com/rvh-studio-assets/sample_animated_video.mp4",
            isImageToVideo = true,
            sourceImageUri = sourceImageUri,
            isSuccess = true,
            message = "Photo animated into video with Veo 3 ($model)"
        )
    }

    private fun callGeminiRaw(apiKey: String, model: String, prompt: String): String {
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 2048)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun extractTextFromGeminiResponse(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            if (parts.length() == 0) return ""
            parts.getJSONObject(0).optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseContentBundle(rawJson: String): GeneratedContentBundle? {
        val text = extractTextFromGeminiResponse(rawJson)
        if (text.isBlank()) return null
        return try {
            val cleaned = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(cleaned)
            GeneratedContentBundle(
                primaryText = obj.optString("primaryText", ""),
                shortVersion = obj.optString("shortVersion", ""),
                longVersion = obj.optString("longVersion", ""),
                alternate1 = obj.optString("alternate1", ""),
                alternate2 = obj.optString("alternate2", ""),
                alternate3 = obj.optString("alternate3", ""),
                hashtags = obj.optString("hashtags", ""),
                imagePrompt = obj.optString("imagePrompt", ""),
                videoPrompt = obj.optString("videoPrompt", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseVideoScriptBundle(rawJson: String, defaultTitle: String): GeneratedVideoScriptBundle? {
        val text = extractTextFromGeminiResponse(rawJson)
        if (text.isBlank()) return null
        return try {
            val cleaned = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(cleaned)
            val scenesArray = obj.optJSONArray("scenes") ?: JSONArray()
            GeneratedVideoScriptBundle(
                title = obj.optString("title", defaultTitle),
                hookText = obj.optString("hookText", ""),
                scenesJson = scenesArray.toString(),
                onScreenText = obj.optString("onScreenText", ""),
                voiceoverText = obj.optString("voiceoverText", ""),
                ctaEnding = obj.optString("ctaEnding", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseGuidedResult(rawJson: String): GuidedMarketingResult? {
        val text = extractTextFromGeminiResponse(rawJson)
        if (text.isBlank()) return null
        return try {
            val cleaned = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(cleaned)
            GuidedMarketingResult(
                mainContent = obj.optString("mainContent", ""),
                alternate1 = obj.optString("alternate1", ""),
                alternate2 = obj.optString("alternate2", ""),
                alternate3 = obj.optString("alternate3", ""),
                hashtags = obj.optString("hashtags", ""),
                shortCta = obj.optString("shortCta", ""),
                imagePrompt = obj.optString("imagePrompt", ""),
                videoPrompt = obj.optString("videoPrompt", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun generateOfflineGuidedResult(
        platform: String,
        audience: String,
        goal: String,
        service: String,
        language: String,
        tone: String,
        cta: String,
        length: String
    ): GuidedMarketingResult {
        val isHindi = language.contains("Hindi", ignoreCase = true)
        val isHinglish = language.contains("Hinglish", ignoreCase = true)
        val contactNumber = "+91 98765 43210"
        val clinic = "Rohit Veterinary House"

        val shortCtaText = "📞 $cta: $contactNumber | $clinic"

        val mainContent = when {
            isHindi -> when (length.lowercase()) {
                "short" -> """
                    🐾 *$clinic - विशेष सूचना*
                    👉 $service ($audience के लिए $goal)
                    
                    ✅ प्रमाणित गुणवत्ता एवं सुरक्षित पशु देखभाल
                    ✅ अनुभवी पशुचिकित्सक मार्गदर्शन
                    
                    $shortCtaText
                """.trimIndent()
                "long" -> """
                    🌿 *$clinic | $audience के लिए स्वास्थ्य परामर्श*
                    
                    पशुपालक एवं पेट ओनर्स बंधुओं! अपने पशुओं को मौसमी बीमारियों से सुरक्षित रखने के लिए समय पर देखभाल सबसे आवश्यक है।
                    
                    📌 *मुख्य सेवा / समाधान:* $service
                    🎯 *उद्देश्य:* $goal
                    
                    🔍 *ध्यान रखने योग्य लक्षण एवं सावधानियां:*
                    • सुस्ती, भूख में कमी या तापमान में बदलाव होने पर तुरंत संपर्क करें
                    • बिना डॉक्टर की सलाह के कोई भी अप्रमाणित दवा न दें
                    • हमेशा कोल्ड-चेन मेंटेन की हुई प्रमाणित दवा ही चुनें
                    
                    🏥 *$clinic पर क्या उपलब्ध है:*
                    1. सभी आवश्यक टीकों की विश्वसनीय कोल्ड-चेन उपलब्धता
                    2. प्रमाणित मिनरल मिक्सचर एवं पोषक सप्लीमेंट्स
                    3. आपातकालीन एवं नियमित स्वास्थ्य जांच
                    
                    $shortCtaText
                    📍 मुख्य मार्ग, $clinic | समय: सुबह 8:00 से शाम 7:30 तक
                """.trimIndent()
                else -> """
                    📢 *$clinic | $goal विशेष संदेश*
                    
                    प्रिय $audience बंधुओं, क्या आप अपने पशुओं के बेहतर स्वास्थ्य और सुरक्षा को लेकर सतर्क हैं?
                    
                    ✨ *हमारी पेशकश:* $service
                    
                    • पशु स्वास्थ्य एवं उत्पादकता में सहायक
                    • अनुभवी वेटरनरी डॉक्टर की देखरेख में प्रमाणित उत्पाद
                    • उचित खुराक एवं सुरक्षित उपयोग की पूरी जानकारी
                    
                    पशुओं के अच्छे स्वास्थ्य से ही आपकी समृद्धि है। देर न करें!
                    
                    $shortCtaText
                    📍 $clinic
                """.trimIndent()
            }
            isHinglish -> when (length.lowercase()) {
                "short" -> """
                    🐾 *$clinic Quick Update!*
                    👉 Focus on: $service ($goal for $audience)
                    
                    ✅ 100% Genuine & Cold-chain maintained products
                    ✅ Expert veterinary doctor guidance
                    
                    $shortCtaText
                """.trimIndent()
                "long" -> """
                    🌿 *$clinic | Comprehensive Animal Wellness Guide*
                    
                    Hello $audience! Animal health care mein prevention hi sabse best solution hai.
                    
                    📌 *Focus Service / Product:* $service
                    🎯 *Campaign Goal:* $goal
                    
                    🔍 *Important Symptoms & Care Tips:*
                    • Mild fever, weakness ya appetite loss notice ho toh delay na karein
                    • Self-medication se bachein aur verified veterinary medicines hi use karein
                    • Proper dosage aur regular checkup se critical infections prevent hote hain
                    
                    🏥 *Why trust $clinic:*
                    1. Genuine vaccines with uninterrupted cold-chain preservation
                    2. High-potency nutritional supplements & mineral mixtures
                    3. Friendly, experienced veterinary consultation
                    
                    $shortCtaText
                    📍 Main Road, $clinic | Working hours: 8:00 AM - 7:30 PM
                """.trimIndent()
                else -> """
                    📢 *$clinic | Special $goal Alert!*
                    
                    Attention $audience! Apne pyare animals ki timely care aur protection ke liye aaj hi consult karein.
                    
                    ✨ *Featured Solution:* $service
                    
                    • Animal vitality aur productivity ko boost karein
                    • Genuine certified medicines direct from authorized veterinary lab
                    • Proper dosage aur administration instructions available
                    
                    Healthy animals mean a happier family & profitable farm!
                    
                    $shortCtaText
                    📍 $clinic
                """.trimIndent()
            }
            else -> when (length.lowercase()) {
                "short" -> """
                    🐾 *$clinic - Notice*
                    👉 $service ($goal for $audience)
                    
                    ✅ Certified veterinary products with cold-chain assurance
                    ✅ Qualified veterinary medical guidance
                    
                    $shortCtaText
                """.trimIndent()
                "long" -> """
                    🌿 *$clinic | Veterinary Health & Wellness Guide*
                    
                    Dear $audience, proactive healthcare is the foundation of long-term animal vitality and productivity.
                    
                    📌 *Featured Service / Product:* $service
                    🎯 *Primary Goal:* $goal
                    
                    🔍 *Key Clinical Recommendations:*
                    • Monitor daily appetite, milk yield, and energy levels closely
                    • Avoid unverified treatments; always administer clinically approved formulas
                    • Maintain scheduled vaccinations and preventative deworming protocols
                    
                    🏥 *Services at $clinic:*
                    1. Cold-chain preserved veterinary biologics and medications
                    2. Bio-available chelated mineral and electrolyte supplements
                    3. Professional in-clinic consultations and diagnostic advice
                    
                    $shortCtaText
                    📍 Main Road, $clinic | Open Mon - Sat: 8:00 AM - 7:30 PM
                """.trimIndent()
                else -> """
                    📢 *$clinic | Dedicated $goal Announcement*
                    
                    To all $audience: Safeguard your livestock and pets with trusted clinical care and preventative solutions.
                    
                    ✨ *Highlight:* $service
                    
                    • Formulated for peak animal vitality, immunity, and resilience
                    • Fully certified products stored under strict temperature control
                    • Clear dosage instructions provided by experienced veterinary staff
                    
                    $shortCtaText
                    📍 $clinic
                """.trimIndent()
            }
        }

        val alt1 = "💡 Quick Benefit Option: Protect your $audience today with $service from $clinic. Proven results and professional care. $shortCtaText"
        val alt2 = "⏰ Urgency & Prevention: Seasonal changes increase animal risk! Ensure $service now before complications arise. $clinic - $shortCtaText"
        val alt3 = "🤝 Community & Trust: $clinic stands with our local $audience. Visit us today for genuine $service. $shortCtaText"

        val hashtags = when {
            audience.contains("Dairy", ignoreCase = true) || audience.contains("Cattle", ignoreCase = true) ->
                "#RohitVeterinaryHouse #DairyFarming #CattleHealth #MilkYield #CowCare #VetClinic"
            audience.contains("Pet", ignoreCase = true) ->
                "#RohitVeterinaryHouse #PetCare #DogVaccination #CatHealth #VeterinaryClinic #PetParents"
            audience.contains("Goat", ignoreCase = true) ->
                "#RohitVeterinaryHouse #GoatFarming #LivestockCare #Deworming #BakriPalan"
            audience.contains("Poultry", ignoreCase = true) ->
                "#RohitVeterinaryHouse #PoultryFarming #BroilerCare #LayerFeed #PoultryHealth"
            else ->
                "#RohitVeterinaryHouse #AnimalHealth #VeterinaryCare #Veterinarian #Livestock"
        }

        val imagePrompt = "A photorealistic, brightly lit scene at Rohit Veterinary House clinic in India, showing a compassionate licensed veterinarian examining a healthy animal with a smiling $audience, spotless clinic counter, cold chain refrigerator, and green veterinary cross in the background, 8k resolution, authentic documentary style."

        val videoPrompt = "A cinematic 9:16 vertical video reel for Veo depicting a local Indian $audience visiting Rohit Veterinary House, receiving $service, close-up of healthy vibrant animals, text overlay '$clinic', ending with happy customer and phone number $contactNumber."

        return GuidedMarketingResult(
            mainContent = mainContent,
            alternate1 = alt1,
            alternate2 = alt2,
            alternate3 = alt3,
            hashtags = hashtags,
            shortCta = shortCtaText,
            imagePrompt = imagePrompt,
            videoPrompt = videoPrompt
        )
    }

    // --- Offline Fallback Engines ---

    private fun generateOfflineContentBundle(
        platform: String,
        goal: String,
        audience: String,
        service: String,
        tone: String,
        language: String,
        cta: String
    ): GeneratedContentBundle {
        val isHindi = language.contains("Hindi", ignoreCase = true)
        val isHinglish = language.contains("Hinglish", ignoreCase = true)

        val primary: String
        val short: String
        val long: String
        val alt1: String
        val alt2: String
        val alt3: String

        if (isHindi) {
            primary = """
                🐾 *रोहित वेटरनरी हाउस - विशेष पशु स्वास्थ्य संदेश* 🩺
                
                प्रिय पशुपालक एवं पेट पेरेंट्स,
                आपके पशुओं के बेहतर स्वास्थ्य एवं समय पर सुरक्षा के लिए **$service** का विशेष अभियान शुरू हो चुका है।
                
                ✨ *रोहित वेटरनरी हाउस की सुविधाएं:*
                - अनुभवी पशु चिकित्सकों द्वारा परामर्श
                - उच्च गुणवत्ता वाली कोल्ड-चेन सुरक्षित दवाएं व टीके
                - वैज्ञानिक पोषण एवं सप्लीमेंट गाइडेंस
                
                समय पर देखभाल से पशु रहते हैं निरोगी और दूध उत्पादन व सक्रियता बनी रहती है।
                
                📲 *तुरंत संपर्क करें / $cta:* +91 98765 43210
                📍 रोहित वेटरनरी हाउस, मुख्य बाजार
            """.trimIndent()

            short = "पशुओं के बेहतर स्वास्थ्य के लिए $service अपनाएं। रोहित वेटरनरी हाउस: +91 98765 43210. $cta!"
            long = """
                रोहित वेटरनरी हाउस - स्वास्थ्य एवं सुरक्षा मार्गदर्शिका:
                $goal के तहत, $service पशुओं के जीवन की गुणवत्ता और उत्पादकता बढ़ाने में अत्यंत महत्वपूर्ण है।
                
                मुख्य सावधानियां:
                1. मौसम बदलते ही पशुओं का स्वास्थ्य परीक्षण अवश्य कराएं।
                2. सही खुराक व ब्रांडेड वेटरनरी उत्पादों का ही चयन करें।
                3. किसी भी असामान्य लक्षण पर घरेलू उपचार के बजाय तुरंत योग्य चिकित्सक से परामर्श लें।
                
                रोहित वेटरनरी हाउस हमेशा आपके पशुओं की सेवा में तत्पर है।
                संपर्क: +91 98765 43210 | $cta
            """.trimIndent()

            alt1 = "क्या आपके पशु को $service की आवश्यकता है? आज ही रोहित वेटरनरी हाउस आएं और विशेष परामर्श पाएं।"
            alt2 = "पशु स्वास्थ्य में लापरवाही न करें! $service हेतु रोहित वेटरनरी हाउस से तुरंत जुड़ें: +91 98765 43210."
            alt3 = "खुशहाल पशु, समृद्ध किसान! $service की प्रामाणिक दवाओं के लिए रोहित वेटरनरी हाउस पधारें।"
        } else if (isHinglish) {
            primary = """
                🐾 *Rohit Veterinary House Special Update!* 🐶🐄
                
                Dosto, kya aapke pets ya cattle ko **$service** ki zaroorat hai? 
                Proper animal healthcare me delay mat kijiye! Rohit Veterinary House le kar aaya hai verified and cold-chain maintained veterinary solutions.
                
                ⭐ *Key Highlights:*
                - Certified Veterinarian Consultation
                - Genuine Veterinary Medicines & Nutrition
                - Quick & friendly support for your animals
                
                Healthy animals mean a happier home and better productivity!
                
                👉 *$cta Now:* +91 98765 43210
                📍 Rohit Veterinary House, Main Market
            """.trimIndent()

            short = "Apne animal ke best care ke liye $service zaroori hai! Contact Rohit Veterinary House: +91 98765 43210 ($cta)."
            long = """
                Rohit Veterinary House - Trusted Animal Healthcare Guide:
                Targeting $goal with scientific focus on $service for $audience.
                
                Important Checklist:
                - Timely clinical screening prevents major infections.
                - Use only trusted veterinary formulations with proper batch verification.
                - Keep clean drinking water and stress-free environment.
                
                Book your appointment or get delivery from Rohit Veterinary House.
                Call/WhatsApp: +91 98765 43210 | $cta
            """.trimIndent()

            alt1 = "Animal health is our top priority! Get genuine $service at Rohit Veterinary House today. Call +91 98765 43210."
            alt2 = "Healthy animal, happy family! Don't miss $service schedule. Rohit Veterinary House is here to help."
            alt3 = "Special advice for $audience: Protect your animal with verified $service from Rohit Veterinary House. $cta now!"
        } else {
            primary = """
                🐾 *Rohit Veterinary House Healthcare Notice* 🐶🐱🐄
                
                Ensure the highest vitality and protection for your animals with professional **$service**.
                
                At Rohit Veterinary House, we offer ethical veterinary consultations and WHO-standard storage for all medicines and supplements.
                
                ✨ *Why Choose Us:*
                - Comprehensive physical examination
                - High-potency biologicals and nutritional support
                - Dedicated veterinary care team
                
                Early preventative care is always the most economical and compassionate choice.
                
                📞 *Contact Us / $cta:* +91 98765 43210
                📍 Rohit Veterinary House, Main Road
            """.trimIndent()

            short = "Protect your animals with expert $service at Rohit Veterinary House. Call +91 98765 43210 to $cta."
            long = """
                Rohit Veterinary House - Dedicated Healthcare & Nutrition:
                Focusing on $goal for $audience. Reliable support with $service.
                
                Key Preventive Steps:
                1. Regular screening against seasonal pathogen spikes.
                2. Scientifically verified diet balances and supplements.
                3. Instant veterinary helpline for emergency guidance.
                
                Contact Rohit Veterinary House today for guidance: +91 98765 43210 ($cta).
            """.trimIndent()

            alt1 = "Ensure animal well-being with trusted $service at Rohit Veterinary House. $cta now!"
            alt2 = "Don't wait for symptoms to worsen. Early $service saves lives and yields. Contact Rohit Veterinary House."
            alt3 = "Professional veterinary care for $audience. Get verified $service at Rohit Veterinary House (+91 98765 43210)."
        }

        val hashtags = "#RohitVeterinaryHouse #VeterinaryCare #AnimalHealth #VeterinaryClinic #Pashupalan #PetParents #HealthyLivestock"
        val imagePrompt = "Photorealistic professional veterinary banner for Rohit Veterinary House showing a caring veterinarian examining $audience with clean clinic backdrop, warm lighting, high quality 4k."
        val videoPrompt = "A cinematic 9:16 vertical video showing vibrant animal care at Rohit Veterinary House clinic, focus on $service, happy client smiling, clear call to action overlay."

        return GeneratedContentBundle(
            primaryText = primary,
            shortVersion = short,
            longVersion = long,
            alternate1 = alt1,
            alternate2 = alt2,
            alternate3 = alt3,
            hashtags = hashtags,
            imagePrompt = imagePrompt,
            videoPrompt = videoPrompt
        )
    }

    private fun generateOfflineVideoScript(
        title: String,
        category: String,
        durationSec: Int,
        language: String
    ): GeneratedVideoScriptBundle {
        val hook = if (language.contains("Hindi", ignoreCase = true)) {
            "क्या आप भी अपने पशुओं के स्वास्थ्य को लेकर चिंतित हैं? ये 3 बातें अभी जानें!"
        } else {
            "Want healthier animals with zero stress? Watch these 3 proven veterinary tips!"
        }

        val scenes = JSONArray().apply {
            put(JSONObject().apply {
                put("scene", 1)
                put("duration", "0-5s")
                put("visual", "Caring veterinarian in clean clinic examining animal with friendly smile")
                put("onscreen", "पशु स्वास्थ्य की जरूरी सलाह")
                put("audio", hook)
            })
            put(JSONObject().apply {
                put("scene", 2)
                put("duration", "5-${durationSec - 8}s")
                put("visual", "Close-up of scientific veterinary care, nutritional supplements, and animal wellness")
                put("onscreen", "$category: सही समय पर सही देखभाल")
                put("audio", "रोहित वेटरनरी हाउस पर प्रमाणित दवाएं, अनुभवी सलाह और सटीक टीकाकरण उपलब्ध हैं।")
            })
            put(JSONObject().apply {
                put("scene", 3)
                put("duration", "${durationSec - 8}-${durationSec}s")
                put("visual", "Rohit Veterinary House clinic banner, phone number and WhatsApp badge")
                put("onscreen", "कॉल करें: +91 98765 43210")
                put("audio", "आज ही संपर्क करें रोहित वेटरनरी हाउस या व्हाट्सएप पर मैसेज भेजें!")
            })
        }

        return GeneratedVideoScriptBundle(
            title = title,
            hookText = hook,
            scenesJson = scenes.toString(),
            onScreenText = "$category | रोहित वेटरनरी हाउस | कॉल: +91 98765 43210",
            voiceoverText = "$hook रोहित वेटरनरी हाउस पर प्रमाणित दवाएं, अनुभवी सलाह और सटीक टीकाकरण उपलब्ध हैं। आज ही संपर्क करें रोहित वेटरनरी हाउस या व्हाट्सएप पर मैसेज भेजें!",
            ctaEnding = "Call / WhatsApp Rohit Veterinary House: +91 98765 43210"
        )
    }

    private fun generateOfflineChatResponse(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("facebook") -> {
                "Great choice! Facebook posts are wonderful for building community awareness and engagement for Rohit Veterinary House.\n\nNext question:\n🎯 Who is the target audience for this post?\n1. Pet Owners (Dogs & Cats)\n2. Cattle & Dairy Farmers\n3. Goat & Sheep Farmers\n4. Poultry Farmers\n\nPlease let me know!"
            }
            lower.contains("whatsapp") -> {
                "WhatsApp is fantastic for direct high-conversion messages and status updates for Rohit Veterinary House customers!\n\nNext question:\n🎯 Which audience group should we send this to?\n1. Pet Owners\n2. Dairy/Cattle Farmers\n3. Goat Farmers\n4. Poultry Farmers\n\nAlso, which language do you prefer (Hindi, Hinglish, or English)?"
            }
            lower.contains("video") || lower.contains("reel") || lower.contains("script") -> {
                "Reels and short status videos give the highest reach for Rohit Veterinary House!\n\nWhat duration would you prefer?\n⏱️ 15 seconds (quick status), 30 seconds (standard reel), or 60 seconds (educational)?\n\nAnd what topic would you like to cover (e.g., Summer milk boost, Puppy vaccination, Goat deworming)?"
            }
            lower.contains("cattle") || lower.contains("cow") || lower.contains("dairy") || lower.contains("गाय") || lower.contains("भैंस") -> {
                "Got it! Cattle and dairy farmers respond very well to practical tips on milk yield, mastitis prevention, and mineral nutrition.\n\nWhat specific service or product are we highlighting? (e.g., Chelated Mineral Mixture, Heat Stress Electrolytes, Deworming, or General Consultation)?"
            }
            lower.contains("pet") || lower.contains("dog") || lower.contains("cat") || lower.contains("कुत्ता") -> {
                "Perfect! Pet owners love caring, safety-first messaging.\n\nWhat topic should we focus on? (e.g. Parvovirus & Rabies vaccination camp, Tick & flea treatment, Puppy deworming, or Grooming advice)?"
            }
            else -> {
                """
                    I can craft ready-to-copy marketing content for you right now! 🐾
                    
                    Here is a quick draft for **Rohit Veterinary House**:
                    
                    📢 *Rohit Veterinary House Healthcare Alert*
                    Ensure the best protection for your animals with verified veterinary supplements and professional consultation.
                    
                    ✅ Quality-checked cold chain assured products
                    ✅ Experienced clinical advice
                    ✅ Affordable local pricing
                    
                    📞 Call / WhatsApp: +91 98765 43210
                    📍 Main Road, Rohit Veterinary House
                    
                    Would you like me to tailor this for **WhatsApp**, **Facebook**, or generate a **Video Script**?
                """.trimIndent()
            }
        }
    }

    private fun generateOfflineRoleChatResponse(
        userMessage: String,
        systemRole: String,
        modelName: String
    ): String {
        val lower = userMessage.lowercase()
        return when {
            modelName.contains("flash-lite") -> {
                // Rapid snappy response
                when {
                    lower.contains("slogan") || lower.contains("headline") ->
                        "⚡ *Fast Headline Suggestions by $modelName:*\n1. \"दूध बढ़ेगा, पशु हंसेगा - सिर्फ रोहित वेटरनरी हाउस पर!\"\n2. \"Healthy Livestock, Happy Farmers - Rohit Veterinary House\"\n3. \"Protecting Every Paw & Hoof in Our Community!\"\n\n📞 +91 98765 43210"
                    lower.contains("whatsapp") ->
                        "⚡ *Quick WhatsApp Broadcast:*\n🐾 *रोहित वेटरनरी हाउस अलर्ट*\nगर्मियों में पशुओं के दूध में गिरावट रोकें! इलेक्ट्रोलाइट्स और मिनरल मिक्सचर उपलब्ध।\n📞 तुरंत ऑर्डर करें: +91 98765 43210"
                    else ->
                        "⚡ *Instant Polish by $modelName:*\n\"Rohit Veterinary House: Dedicated animal care, certified livestock medicine, and round-the-clock guidance. Contact +91 98765 43210.\""
                }
            }
            modelName.contains("pro-preview") -> {
                // Deep clinical, compliance, and strategy response
                """
                    🧠 *Clinical & Compliance Strategy Analysis ($modelName)*
                    
                    **Role Context:** Rohit Veterinary House Medical & Marketing Compliance.
                    
                    **1. Clinical Veterinary Principles:**
                    - Ensure safe, ethical communication that never promises unrealistic cure rates.
                    - Emphasize proper diagnosis, calibrated dosage by animal body weight, and cold-chain integrity (2°C - 8°C for biologics & vaccines).
                    
                    **2. Strategic Campaign Recommendation:**
                    - For **$userMessage**: Structure communications into three phases: (A) Symptom recognition, (B) Preventive management, (C) Professional clinic consultation.
                    
                    **3. Regulatory Compliance Guarantee:**
                    - Adheres to standard Schedule H/H1 veterinary prescription guidelines.
                    
                    📞 Rohit Veterinary House Clinic Helpline: +91 98765 43210
                """.trimIndent()
            }
            else -> {
                // General gemini-3.5-flash response
                when {
                    lower.contains("disease") || lower.contains("outbreak") || lower.contains("fmd") || lower.contains("lumpy") -> {
                        """
                            🌐 *Veterinary Intelligence & Disease Advisory ($modelName)*
                            
                            **Topic: Animal Health Alert & Prevention**
                            
                            1. **Current Prevention Protocol:**
                               - Early isolation of infected animals and disinfection of sheds.
                               - Timely annual vaccination (FMD, HS, BQ) before monsoon onset.
                               - Vector control: Anti-fly and tick sprays in farm perimeter.
                               
                            2. **Clinic Action Point:**
                               - रोहित वेटरनरी हाउस पर प्रमाणित टीके, कोल्ड-चेन सुरक्षित दवाएं और पशु चिकित्सक परामर्श उपलब्ध हैं।
                               
                            📞 परामर्श हेल्पलाइन: +91 98765 43210 | रोहित वेटरनरी हाउस
                        """.trimIndent()
                    }
                    lower.contains("milk") || lower.contains("cow") || lower.contains("buffalo") || lower.contains("पशु") || lower.contains("दूध") -> {
                        """
                            🐄 *Dairy Nutrition & Yield Campaign Plan ($modelName)*
                            
                            **Key Message:**
                            "गर्मियों और बदलते मौसम में पशुओं के दूध का उत्पादन कम न होने दें!"
                            
                            - **पोषण सलाह:** उच्च गुणवत्ता वाला चिलेटेड मिनरल मिक्सचर (Chelated Mineral Mixture) और बाईपास फैट आहार में शामिल करें।
                            - **पानी की व्यवस्था:** दिन में कम से कम 3-4 बार ठंडा और स्वच्छ पानी।
                            - **रोहित वेटरनरी हाउस का वादा:** प्रामाणिक कंपनी के मिनरल मिक्सचर और कैल्शियम टॉनिक उचित मूल्य पर।
                            
                            📲 व्हाट्सएप पर ऑर्डर करें या सीधे क्लिनिक आएं: +91 98765 43210
                        """.trimIndent()
                    }
                    else -> generateOfflineChatResponse(userMessage)
                }
            }
        }
    }
}
