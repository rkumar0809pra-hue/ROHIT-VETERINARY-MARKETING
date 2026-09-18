package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.model.PostStatus
import com.example.data.model.UserRole
import com.example.ui.components.PlatformBadge
import com.example.ui.components.RolePill
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun branding_components_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Column(modifier = Modifier.padding(16.dp)) {
          StatusBadge(status = PostStatus.APPROVED.name)
          Spacer(modifier = Modifier.height(8.dp))
          PlatformBadge(platform = "WhatsApp")
          Spacer(modifier = Modifier.height(8.dp))
          RolePill(role = UserRole.ADMIN, onClick = {})
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

