package vn.training.bai02

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val fallback = "This long fallback string exists only in English to demonstrate Android resource fallback."

    @After
    fun resetLanguage() {
        compose.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }

    @Test
    fun switchingLanguagesPreservesUnicodeNameAndLocalizesMessages() {
        val name = "Nguyễn Văn Android 日本語"
        compose.onNodeWithTag("name_input").performTextReplacement(name)

        selectLanguage("en", "Language settings")
        compose.onNodeWithText("Hello, $name!").assertExists()
        listOf("No messages", "1 message", "2 messages", "5 messages").forEach {
            compose.onNodeWithText(it).assertExists()
        }

        selectLanguage("vi", "Cài đặt ngôn ngữ")
        compose.onNodeWithText("Xin chào, $name!").assertExists()
        listOf("Không có tin nhắn", "1 tin nhắn", "2 tin nhắn", "5 tin nhắn").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText(fallback).assertExists()

        selectLanguage("ja", "言語設定")
        compose.onNodeWithText("こんにちは、${name}さん！").assertExists()
        listOf("メッセージはありません", "1件のメッセージ", "2件のメッセージ", "5件のメッセージ").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText(fallback).assertExists()

        compose.onNodeWithTag("language_").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("", AppCompatDelegate.getApplicationLocales().toLanguageTags())
        }
        assertName(name)
    }

    @Test
    fun longNameSurvivesRecreationAndIsPersisted() {
        val name = "Nguyễn 日本語 ".repeat(30)
        compose.onNodeWithTag("name_input").performTextReplacement(name)
        selectLanguage("vi", "Cài đặt ngôn ngữ")
        compose.activityRule.scenario.recreate()
        assertName(name)
        compose.onNodeWithText("Xin chào, $name!").assertExists()
        compose.runOnIdle {
            assertEquals(name, compose.activity.getSharedPreferences("input", Context.MODE_PRIVATE).getString("name", null))
        }
    }

    @Test
    fun emptyNameSurvivesRecreation() {
        compose.onNodeWithTag("name_input").performTextReplacement("")
        compose.activityRule.scenario.recreate()
        assertName("")
        compose.runOnIdle {
            assertEquals("", compose.activity.getSharedPreferences("input", Context.MODE_PRIVATE).getString("name", null))
        }
    }

    private fun assertName(expected: String) {
        compose.onNodeWithTag("name_input").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(expected))
        )
    }

    private fun selectLanguage(tag: String, title: String) {
        compose.onNodeWithTag("language_$tag").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText(title)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
