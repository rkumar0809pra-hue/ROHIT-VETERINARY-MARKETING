package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostStatus
import com.example.data.model.UserRole
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusApprovedContainer
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusDraftContainer
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingContainer
import com.example.ui.theme.StatusPublished
import com.example.ui.theme.StatusPublishedContainer
import com.example.ui.theme.StatusRejected
import com.example.ui.theme.StatusRejectedContainer
import com.example.ui.theme.StatusScheduled
import com.example.ui.theme.StatusScheduledContainer
import com.example.ui.theme.VetAmber
import com.example.ui.theme.VetBlue
import com.example.ui.theme.VetTeal
import com.example.ui.theme.VetTealContainer
import com.example.ui.theme.WhatsAppDark
import com.example.ui.theme.WhatsAppGreen

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        PostStatus.APPROVED.name -> Triple(StatusApprovedContainer, StatusApproved, "Approved")
        PostStatus.SCHEDULED.name -> Triple(StatusScheduledContainer, StatusScheduled, "Scheduled")
        PostStatus.PUBLISHED.name -> Triple(StatusPublishedContainer, StatusPublished, "Published")
        PostStatus.PENDING_APPROVAL.name -> Triple(StatusPendingContainer, StatusPending, "Pending Approval")
        PostStatus.REJECTED.name -> Triple(StatusRejectedContainer, StatusRejected, "Rejected")
        else -> Triple(StatusDraftContainer, StatusDraft, "Draft")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.testTag("status_badge_$status")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PlatformBadge(
    platform: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when {
        platform.contains("WhatsApp", ignoreCase = true) -> Triple(Color(0xFFDCFCE7), WhatsAppDark, "WhatsApp")
        platform.contains("Facebook", ignoreCase = true) -> Triple(Color(0xFFDBEAFE), FacebookBlue, "Facebook")
        platform.contains("Instagram", ignoreCase = true) -> Triple(Color(0xFFFCE7F3), Color(0xFFBE185D), "Instagram")
        else -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), "Video Reels")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun RolePill(
    role: UserRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (role) {
        UserRole.ADMIN -> Pair(VetTealContainer, VetTeal)
        UserRole.MARKETING_STAFF -> Pair(Color(0xFFE0F2FE), VetBlue)
        UserRole.CONTENT_CREATOR -> Pair(Color(0xFFFEF3C7), VetAmber)
    }

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.testTag("role_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = role.label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
