package com.example.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Campaign
import com.example.data.model.HeroSkills
import com.example.data.model.SkillXpState
import com.example.data.model.UnlockedAchievementRecord
import com.example.data.repository.CampaignExtendedStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CampaignExtendedStoreTest {

    @Test
    fun roundTripsExtendedSkillXpBookmarksAchievementsAndMergesLevelUps() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CampaignExtendedStore(context)
        val campaignId = "test_ext_store_campaign"
        store.deleteCampaign(campaignId)

        val grownSkills = HeroSkills(might = 6L, stealth = 7L, insight = 5L, lore = 5L)
        val xp = SkillXpState(mightXp = 30, stealthXp = 85, insightXp = 12, loreXp = 44)
        val achievement = UnlockedAchievementRecord(
            id = "path_of_mastery",
            unlockedAtTurn = 5L,
            unlockedAtMillis = 1_700_000_000_000L
        )

        store.saveCampaignState(
            campaignId = campaignId,
            skills = grownSkills,
            skillXp = xp,
            skillLevelUpCount = 2,
            unlockedAchievements = listOf(achievement),
            hasForkedChronicle = true
        )

        val added = store.toggleBookmark(campaignId, turnNumber = 4L, excerpt = "  Встреча  на причале  ")
        assertTrue(added)

        val loaded = store.read(campaignId)
        assertNotNull(loaded)
        assertEquals(grownSkills, loaded?.skills)
        assertEquals(xp, loaded?.skillXp)
        assertEquals(2, loaded?.skillLevelUpCount)
        assertEquals(1, loaded?.bookmarks?.size)
        assertEquals("Встреча на причале", loaded?.bookmarks?.first()?.excerpt)
        assertEquals(listOf(achievement), loaded?.unlockedAchievements)
        assertTrue(loaded?.hasForkedChronicle == true)

        // Even if remote Firestore campaign has older custom starting skills, local level-ups win in mergeInto
        val remoteStaleCampaign = Campaign(
            id = campaignId,
            title = "Хроника",
            skills = HeroSkills(might = 4L, stealth = 6L, insight = 5L, lore = 5L)
        )
        val merged = store.mergeInto(remoteStaleCampaign)
        assertEquals(grownSkills, merged.skills)

        // Toggling same bookmark again removes it cleanly
        val removed = store.toggleBookmark(campaignId, turnNumber = 4L, excerpt = "")
        assertFalse(removed)
        assertTrue(store.read(campaignId)?.bookmarks.isNullOrEmpty())

        store.deleteCampaign(campaignId)
    }
}
