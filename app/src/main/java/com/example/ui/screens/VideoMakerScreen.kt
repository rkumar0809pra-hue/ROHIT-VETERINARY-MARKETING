package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.gemini.VeoVideoResult
import com.example.data.model.ContentCategory
import com.example.data.model.ContentLanguage
import com.example.data.model.GeneratedVeoVideo
import com.example.data.model.PostStatus
import com.example.data.model.UserRole
import com.example.ui.theme.VetAmber
import com.example.ui.theme.VetBlue
import com.example.ui.theme.VetTeal
import com.example.ui.theme.VetTealContainer
import com.example.ui.viewmodel.MarketingViewModel
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

data class SceneItem(
    val sceneNum: Int,
    val duration: String,
    val visual: String,
    val onscreen: String,
    val audio: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoMakerScreen(
    viewModel: MarketingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("video_maker_screen")
    ) {
        // Top Tab Navigation Bar
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = VetTeal
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Veo 3 Video", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_veo_text_to_video")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Animate Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_veo_animate_photo")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Script Writer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_storyboard_script")
            )
        }

        when (selectedTab) {
            0 -> VeoTextToVideoTab(viewModel = viewModel)
            1 -> VeoAnimatePhotoTab(viewModel = viewModel)
            2 -> StoryboardScriptTab(viewModel = viewModel)
        }
    }
}

/**
 * Tab 0: Veo 3 Text-to-Video Generation
 * Model: veo-3.1-fast-generate-preview
 * Aspect Ratio: 16:9 or 9:16
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VeoTextToVideoTab(
    viewModel: MarketingViewModel
) {
    val context = LocalContext.current
    val prompt by viewModel.veoPrompt.collectAsState()
    val aspectRatio by viewModel.veoAspectRatio.collectAsState()
    val isGenerating by viewModel.isGeneratingVeoVideo.collectAsState()
    val lastResult by viewModel.lastGeneratedVeoVideo.collectAsState()
    val statusMessage by viewModel.veoStatusMessage.collectAsState()
    val allVideos by viewModel.allVeoVideos.collectAsState()

    val promptPresets = listOf(
        "Veterinarian examining a healthy dairy cow in modern Indian barn with morning sunlight and lush background",
        "Veterinary doctor gently checking a golden retriever puppy in clean modern clinic",
        "Village dairy farmer giving chelated mineral mixture to high-yielding buffalo with satisfied smile",
        "Rohit Veterinary House mobile clinic ambulance arriving at rural farm in morning light",
        "Cold chain medicine storage unit showing sterile veterinary vaccines with safety seal"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("veo_text_to_video_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Model Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(VetTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Veo 3 Video Generator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = VetTealContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Model: veo-3.1-fast-generate-preview",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VetTeal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Prompt Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Video Description / Text Prompt",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { viewModel.veoPrompt.value = it },
                        placeholder = { Text("Describe the veterinary scene, camera movement, and lighting...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("veo_prompt_input"),
                        minLines = 3,
                        maxLines = 5
                    )

                    // Presets
                    Text(
                        text = "✨ Quick Veterinary Scene Presets:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        promptPresets.forEach { preset ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { viewModel.veoPrompt.value = preset }
                            ) {
                                Text(
                                    text = preset.take(45) + "...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Aspect Ratio
                    Text(
                        text = "2. Aspect Ratio (veo-3.1-fast-generate-preview)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { viewModel.veoAspectRatio.value = "9:16" },
                            color = if (aspectRatio == "9:16") VetTeal else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ratio_9_16_chip")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📱 9:16 Vertical",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (aspectRatio == "9:16") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Reels, Shorts, Status",
                                    fontSize = 10.sp,
                                    color = if (aspectRatio == "9:16") Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { viewModel.veoAspectRatio.value = "16:9" },
                            color = if (aspectRatio == "16:9") VetTeal else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ratio_16_9_chip")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🖥️ 16:9 Landscape",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (aspectRatio == "16:9") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "YouTube, Facebook Feed",
                                    fontSize = 10.sp,
                                    color = if (aspectRatio == "16:9") Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Generate Button
                    Button(
                        onClick = { viewModel.generateVeoTextToVideo() },
                        enabled = !isGenerating && prompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_veo_video_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Video with Veo 3...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Video with Veo 3 ($aspectRatio)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Status Message Banner
        if (statusMessage != null) {
            item {
                Surface(
                    color = VetTealContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = VetTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = statusMessage ?: "", fontSize = 12.sp, color = VetTeal, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Video Player Preview (if generated or default showcase)
        item {
            InteractiveVeoVideoPlayer(
                videoResult = lastResult ?: VeoVideoResult(
                    prompt = prompt,
                    aspectRatio = aspectRatio,
                    model = "veo-3.1-fast-generate-preview",
                    videoUrl = "https://storage.googleapis.com/rvh-studio-assets/sample_vet_video.mp4",
                    message = "Ready for playback"
                ),
                onCopyPrompt = { copyToClipboard(context, prompt) },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Veo 3 Video Prompt - Rohit Veterinary House")
                        putExtra(Intent.EXTRA_TEXT, "Generated Video via Veo 3 ($aspectRatio):\n\n$prompt\n\nModel: veo-3.1-fast-generate-preview\nRohit Veterinary House")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Video Prompt"))
                }
            )
        }

        // Recent Veo Videos Gallery
        if (allVideos.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Veo Videos Gallery (${allVideos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(allVideos) { video ->
                SavedVeoVideoCard(
                    video = video,
                    onDelete = { viewModel.deleteVeoVideo(video.id) },
                    onCopyPrompt = { copyToClipboard(context, video.prompt) }
                )
            }
        }
    }
}

/**
 * Tab 1: Veo 3 Animate Photo into Video
 * Model: veo-3.1-fast-generate-preview
 * Aspect Ratio: 16:9 or 9:16
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VeoAnimatePhotoTab(
    viewModel: MarketingViewModel
) {
    val context = LocalContext.current
    val selectedImageUri by viewModel.veoSelectedImageUri.collectAsState()
    val motionPrompt by viewModel.veoImagePrompt.collectAsState()
    val aspectRatio by viewModel.veoImageAspectRatio.collectAsState()
    val isGenerating by viewModel.isGeneratingVeoVideo.collectAsState()
    val lastResult by viewModel.lastGeneratedVeoVideo.collectAsState()
    val statusMessage by viewModel.veoStatusMessage.collectAsState()

    // Photo picker launcher (zero-permission, compliant with Android guidelines)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.veoSelectedImageUri.value = uri.toString()
            Toast.makeText(context, "Photo selected successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    val clinicSamplePresets = listOf(
        "🐄 Dairy Cow Pasture" to "https://images.unsplash.com/photo-1546445317-29f4545e9d53?w=800",
        "🐶 Puppy Health Exam" to "https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=800",
        "🐐 Goat Farm Herd" to "https://images.unsplash.com/photo-1524024973431-2ad916746881?w=800",
        "🏥 Clinic Care Room" to "https://images.unsplash.com/photo-1584820927498-cfe5211fd8bf?w=800"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("veo_animate_photo_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Model Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Animate Photos into Video",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0xFFEEF2FF),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Model: veo-3.1-fast-generate-preview",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Photo Upload & Preview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Choose or Upload a Photo to Animate",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Photo Picker Button
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("upload_photo_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedImageUri != null) "Change Selected Photo" else "Select Photo from Device")
                    }

                    // Preset Clinic Sample Photos
                    Text(
                        text = "Or choose a Rohit Veterinary House sample photo:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        clinicSamplePresets.forEach { (name, url) ->
                            val isSelected = selectedImageUri == url
                            Surface(
                                onClick = { viewModel.veoSelectedImageUri.value = url },
                                color = if (isSelected) VetTealContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, VetTeal) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) VetTeal else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    // Preview of selected photo
                    if (selectedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected photo to animate",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Ready to Animate",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Motion Prompt
                    Text(
                        text = "2. Motion & Animation Prompt",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = motionPrompt,
                        onValueChange = { viewModel.veoImagePrompt.value = it },
                        placeholder = { Text("e.g. Slow motion camera dolly push, cow blinking and chewing calmly, soft cinematic breeze...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("veo_motion_prompt_input"),
                        minLines = 2,
                        maxLines = 4
                    )

                    // Aspect Ratio
                    Text(
                        text = "3. Aspect Ratio (veo-3.1-fast-generate-preview)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { viewModel.veoImageAspectRatio.value = "9:16" },
                            color = if (aspectRatio == "9:16") Color(0xFF6366F1) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("image_ratio_9_16_chip")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📱 9:16 Portrait",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (aspectRatio == "9:16") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Reels & Stories",
                                    fontSize = 10.sp,
                                    color = if (aspectRatio == "9:16") Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { viewModel.veoImageAspectRatio.value = "16:9" },
                            color = if (aspectRatio == "16:9") Color(0xFF6366F1) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("image_ratio_16_9_chip")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🖥️ 16:9 Landscape",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (aspectRatio == "16:9") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "YouTube & Feed",
                                    fontSize = 10.sp,
                                    color = if (aspectRatio == "16:9") Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Animate Button
                    Button(
                        onClick = { viewModel.generateVeoImageToVideo(null, null) },
                        enabled = !isGenerating && motionPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("animate_photo_with_veo_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Animating Photo with Veo 3...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Animate Photo with Veo 3 ($aspectRatio)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Status Message Banner
        if (statusMessage != null) {
            item {
                Surface(
                    color = Color(0xFFEEF2FF),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = statusMessage ?: "", fontSize = 12.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Video Player Preview
        item {
            InteractiveVeoVideoPlayer(
                videoResult = lastResult ?: VeoVideoResult(
                    prompt = motionPrompt,
                    aspectRatio = aspectRatio,
                    model = "veo-3.1-fast-generate-preview",
                    isImageToVideo = true,
                    videoUrl = "https://storage.googleapis.com/rvh-studio-assets/sample_animated_video.mp4",
                    message = "Ready for playback"
                ),
                onCopyPrompt = { copyToClipboard(context, motionPrompt) },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Veo 3 Animated Photo - Rohit Veterinary House")
                        putExtra(Intent.EXTRA_TEXT, "Veo 3 Animated Photo ($aspectRatio):\n\nMotion: $motionPrompt\n\nModel: veo-3.1-fast-generate-preview\nRohit Veterinary House")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Animated Video"))
                }
            )
        }
    }
}

/**
 * Tab 2: Storyboard Script Writer (Multi-Scene 15s/30s/60s)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoryboardScriptTab(
    viewModel: MarketingViewModel
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val isGenerating by viewModel.isGeneratingVideo.collectAsState()
    val generatedScript by viewModel.generatedVideoBundle.collectAsState()
    val statusMessage by viewModel.videoStatusMessage.collectAsState()

    val title by viewModel.videoTitle.collectAsState()
    val category by viewModel.videoCategory.collectAsState()
    val durationSec by viewModel.videoDurationSec.collectAsState()
    val aspectRatio by viewModel.videoAspectRatio.collectAsState()
    val language by viewModel.videoLanguage.collectAsState()

    var activeScenePreviewIndex by remember { mutableIntStateOf(0) }

    val scenesList: List<SceneItem> = remember(generatedScript?.scenesJson) {
        val jsonStr = generatedScript?.scenesJson ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SceneItem(
                    sceneNum = obj.optInt("scene", i + 1),
                    duration = obj.optString("duration", ""),
                    visual = obj.optString("visual", ""),
                    onscreen = obj.optString("onscreen", ""),
                    audio = obj.optString("audio", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("storyboard_script_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Storyboard Script Writer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Create structured video scripts with scene-by-scene visuals, hooks, voiceover lines, and ending CTAs tailored for Indian veterinary farmers and pet owners.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Video Topic or Concept",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.videoTitle.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_topic_input"),
                        placeholder = { Text("e.g. Stop Cow Milk Drop in Summer, Puppy Deworming Milestone") },
                        singleLine = true
                    )

                    Text(
                        text = "2. Target Duration",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15 to "15s (Status)", 30 to "30s (Reel)", 60 to "60s (Deep)").forEach { (sec, label) ->
                            FilterChip(
                                selected = durationSec == sec,
                                onClick = { viewModel.videoDurationSec.value = sec },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = "3. Language",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContentLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = language == lang.name,
                                onClick = { viewModel.videoLanguage.value = lang.name },
                                label = { Text(lang.label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.generateVideoScript() },
                        enabled = !isGenerating && title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_video_script_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Script...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Multi-Scene Script", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Script Output Sections
        if (generatedScript != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🪝 First 3-Second Hook",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            IconButton(onClick = { copyToClipboard(context, generatedScript?.hookText ?: "") }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Hook", tint = Color(0xFF92400E), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = generatedScript?.hookText ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }

            // Scene by scene breakdown
            items(scenesList) { scene ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = VetTealContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "Scene ${scene.sceneNum} (${scene.duration})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VetTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "🎬 Visual Direction:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = scene.visual, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "📝 On-Screen Text:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = scene.onscreen, style = MaterialTheme.typography.bodyMedium, color = VetTeal)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "🎙️ Voiceover Line:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "\"${scene.audio}\"", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Save Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.saveCurrentVideoScript(PostStatus.DRAFT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_video_draft_button")
                    ) {
                        Text("Save as Draft")
                    }

                    Button(
                        onClick = {
                            val targetStatus = if (currentRole == UserRole.CONTENT_CREATOR) PostStatus.PENDING_APPROVAL else PostStatus.APPROVED
                            viewModel.saveCurrentVideoScript(targetStatus)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VetTeal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_video_script_button")
                    ) {
                        Text(if (currentRole == UserRole.CONTENT_CREATOR) "Submit Approval" else "Approve Script")
                    }
                }
            }
        }
    }
}

/**
 * Interactive Video Player Component
 * Renders video container according to selected aspect ratio (16:9 or 9:16)
 * with animated playback timeline, play/pause controls, timecode, and export actions.
 */
@Composable
fun InteractiveVeoVideoPlayer(
    videoResult: VeoVideoResult,
    onCopyPrompt: () -> Unit,
    onShare: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0.45f) }

    val infiniteTransition = rememberInfiniteTransition(label = "playback_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Periodic progress advance when playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(200)
            progress = (progress + 0.02f)
            if (progress > 1f) progress = 0f
        }
    }

    val isPortrait = videoResult.aspectRatio == "9:16"
    val currentTimeSeconds = (progress * 8).toInt()
    val timecodeString = "00:0$currentTimeSeconds / 00:08"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Veo 3 Video Player Preview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${videoResult.aspectRatio} • 720p",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Viewport Container (framed to 16:9 or 9:16)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isPortrait) Modifier.height(340.dp) else Modifier.height(200.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B),
                                Color(0xFF090D16)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background visual texture
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = VetTeal.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (videoResult.isImageToVideo) "Veo 3 Animated Image Motion" else "Veo 3 Generated Scene",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = videoResult.prompt.take(80) + if (videoResult.prompt.length > 80) "..." else "",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // Aspect ratio watermark badge
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "VEO-3 • ${videoResult.aspectRatio}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Play / Pause center toggle overlay
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Bottom Timeline Controls inside Player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = timecodeString,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            color = VetTeal,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "720p HD",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Player Scrubber Slider
            Slider(
                value = progress,
                onValueChange = {
                    progress = it
                    isPlaying = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = VetTeal,
                    activeTrackColor = VetTeal,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )

            // Video Details and Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Model: ${videoResult.model}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rohit Veterinary House Studio",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onCopyPrompt, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Prompt", tint = VetTeal, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = VetTeal, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Saved Veo Video Card in Gallery
 */
@Composable
fun SavedVeoVideoCard(
    video: GeneratedVeoVideo,
    onDelete: () -> Unit,
    onCopyPrompt: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (video.aspectRatio == "9:16") VetTeal else Color(0xFF6366F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (video.isImageToVideo) Icons.Outlined.Image else Icons.Outlined.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = VetTealContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${video.aspectRatio} • ${video.model}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = VetTeal,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.prompt,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onCopyPrompt, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Prompt", tint = VetTeal, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("RVH Studio", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}
