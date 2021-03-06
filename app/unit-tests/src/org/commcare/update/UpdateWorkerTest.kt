package org.commcare.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.commcare.CommCareApplication
import org.commcare.CommCareTestApplication
import org.commcare.android.mocks.ModernHttpRequesterMock
import org.commcare.android.util.TestAppInstaller
import org.commcare.android.util.UpdateUtils
import org.commcare.preferences.ServerUrls.PREFS_APP_SERVER_KEY
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File


@Config(application = CommCareTestApplication::class)
@RunWith(AndroidJUnit4::class)
class UpdateWorkerTest {

    companion object {
        const val REF_BASE_DIR = "jr://resource/commcare-apps/update_tests/"
    }

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        TestAppInstaller.installAppAndLogin(
                UpdateUtils.buildResourceRef(Companion.REF_BASE_DIR, "base_app", "profile.ccpr"),
                "test", "123")
    }

    @Test
    fun testValidUpdate_shouldSucceed() {
        val profileRef = UpdateUtils.buildResourceRef(Companion.REF_BASE_DIR, "valid_update", "profile.ccpr")
        runAndTestUpdateWorker(profileRef, ListenableWorker.Result.success())
    }

    @Test
    fun testNoLocalStorage_shouldFail() {
        // nuke local folder that CommCare uses to stage updates.
        val dir = File(CommCareApplication.instance().androidFsTemp)
        Assert.assertTrue(dir.delete())

        val profileRef = UpdateUtils.buildResourceRef(REF_BASE_DIR, "valid_update", "profile.ccpr")
        runAndTestUpdateWorker(profileRef, ListenableWorker.Result.failure())
    }

    @Test
    fun testInvalidResource_shouldFail() {
        val profileRef = UpdateUtils.buildResourceRef(REF_BASE_DIR, "invalid_update", "profile.ccpr")
        runAndTestUpdateWorker(profileRef, ListenableWorker.Result.failure())
    }

    @Test
    fun testMissingResource_shouldFail() {
        val profileRef = UpdateUtils.buildResourceRef(REF_BASE_DIR, "valid_update_without_multimedia_present", "profile.ccpr")
        runAndTestUpdateWorker(profileRef, ListenableWorker.Result.failure())
    }

    @Test
    fun testIncompatibleVersion_shouldFail() {
        val profileRef = UpdateUtils.buildResourceRef(REF_BASE_DIR, "invalid_version", "profile.ccpr")
        runAndTestUpdateWorker(profileRef, ListenableWorker.Result.failure())
    }

    @Test
    fun testNetworkFailure_shouldRetry() {
        ModernHttpRequesterMock.setRequestPayloads(arrayOf("null", "null", "null", "null")) // should cause an IO Exception
        val profileRef = UpdateUtils.buildResourceRef("https://", "fake_update", "fake.ccpr")
        runAndTestUpdateWorker(profileRef, ListenableWorker.Result.retry())
    }

    private fun runAndTestUpdateWorker(profileRef: String, expectedResult: ListenableWorker.Result) {
        setUpdatePreference(profileRef)
        val worker = TestListenableWorkerBuilder<UpdateWorker>(context).build()
        runBlocking {
            val result = worker.doWork()
            assertThat(result, `is`(expectedResult))
        }
    }

    private fun setUpdatePreference(profileRef: String) {
        (context as CommCareApplication).currentApp.appPreferences
                .edit()
                .putString(PREFS_APP_SERVER_KEY, profileRef)
                .apply()
    }
}