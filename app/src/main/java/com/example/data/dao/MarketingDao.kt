package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChatMessage
import com.example.data.model.GeneratedVeoVideo
import com.example.data.model.MarketingPost
import com.example.data.model.VideoScript
import com.example.data.model.WhatsAppCampaign
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketingPostDao {
    @Query("SELECT * FROM marketing_posts ORDER BY createdDateMillis DESC")
    fun getAllPosts(): Flow<List<MarketingPost>>

    @Query("SELECT * FROM marketing_posts WHERE status = :status ORDER BY createdDateMillis DESC")
    fun getPostsByStatus(status: String): Flow<List<MarketingPost>>

    @Query("SELECT * FROM marketing_posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Long): MarketingPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: MarketingPost): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<MarketingPost>)

    @Update
    suspend fun updatePost(post: MarketingPost)

    @Query("DELETE FROM marketing_posts WHERE id = :id")
    suspend fun deletePost(id: Long)

    @Query("DELETE FROM marketing_posts")
    suspend fun clearAllPosts()
}

@Dao
interface VideoScriptDao {
    @Query("SELECT * FROM video_scripts ORDER BY createdDateMillis DESC")
    fun getAllVideoScripts(): Flow<List<VideoScript>>

    @Query("SELECT * FROM video_scripts WHERE id = :id LIMIT 1")
    suspend fun getVideoScriptById(id: Long): VideoScript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoScript(script: VideoScript): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoScripts(scripts: List<VideoScript>)

    @Update
    suspend fun updateVideoScript(script: VideoScript)

    @Query("DELETE FROM video_scripts WHERE id = :id")
    suspend fun deleteVideoScript(id: Long)

    @Query("DELETE FROM video_scripts")
    suspend fun clearAllVideoScripts()
}

@Dao
interface WhatsAppCampaignDao {
    @Query("SELECT * FROM whatsapp_campaigns ORDER BY createdDateMillis DESC")
    fun getAllCampaigns(): Flow<List<WhatsAppCampaign>>

    @Query("SELECT * FROM whatsapp_campaigns WHERE id = :id LIMIT 1")
    suspend fun getCampaignById(id: Long): WhatsAppCampaign?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: WhatsAppCampaign): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaigns(campaigns: List<WhatsAppCampaign>)

    @Update
    suspend fun updateCampaign(campaign: WhatsAppCampaign)

    @Query("DELETE FROM whatsapp_campaigns WHERE id = :id")
    suspend fun deleteCampaign(id: Long)

    @Query("DELETE FROM whatsapp_campaigns")
    suspend fun clearAllCampaigns()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Dao
interface VeoVideoDao {
    @Query("SELECT * FROM veo_videos ORDER BY createdDateMillis DESC")
    fun getAllVideos(): Flow<List<GeneratedVeoVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: GeneratedVeoVideo): Long

    @Query("DELETE FROM veo_videos WHERE id = :id")
    suspend fun deleteVideo(id: Long)
}

