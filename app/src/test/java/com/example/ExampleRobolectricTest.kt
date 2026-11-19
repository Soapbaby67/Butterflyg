package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.resolveRecaptchaSiteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Хроники Бабочки", appName)
  }

  @Test
  fun `recaptcha site key resource resolves statically`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val siteKey = resolveRecaptchaSiteKey(context)
    assertTrue("Expected non-blank recaptcha_site_key", siteKey.length >= 20)
  }
}
