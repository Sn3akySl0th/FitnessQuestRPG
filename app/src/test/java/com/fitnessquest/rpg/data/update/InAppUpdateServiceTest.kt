package com.fitnessquest.rpg.data.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InAppUpdateServiceTest {

    private class FakeAppUpdateClient : AppUpdateClient {
        var updateDataToReturn: Result<AppUpdateData> = Result.success(
            AppUpdateData(isAvailable = false, isFlexibleAllowed = false),
        )
        var flexibleUpdateResult: Result<Unit> = Result.success(Unit)
        var completeUpdateCalled = false
        var completeUpdateResult: Result<Unit> = Result.success(Unit)

        val registeredListeners = mutableListOf<(UpdateInstallState) -> Unit>()

        override suspend fun getAppUpdateInfo(): Result<AppUpdateData> = updateDataToReturn

        override fun startFlexibleUpdate(
            data: AppUpdateData,
            launcher: ActivityResultLauncher<IntentSenderRequest>,
        ): Result<Unit> = flexibleUpdateResult

        override fun registerInstallListener(listener: (UpdateInstallState) -> Unit) {
            registeredListeners.add(listener)
        }

        override fun unregisterInstallListener(listener: (UpdateInstallState) -> Unit) {
            registeredListeners.remove(listener)
        }

        override fun completeUpdate(): Result<Unit> {
            completeUpdateCalled = true
            return completeUpdateResult
        }

        fun emitInstallState(state: UpdateInstallState) {
            registeredListeners.forEach { it(state) }
        }
    }

    private fun createDummyLauncher(): ActivityResultLauncher<IntentSenderRequest> {
        return object : ActivityResultLauncher<IntentSenderRequest>() {
            override fun launch(input: IntentSenderRequest, options: androidx.core.app.ActivityOptionsCompat?) {}
            override fun unregister() {}
            override val contract: androidx.activity.result.contract.ActivityResultContract<IntentSenderRequest, *>
                get() = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
        }
    }

    @Test
    fun `initial status is Idle`() {
        val client = FakeAppUpdateClient()
        val service = InAppUpdateService(client)

        assertEquals(InAppUpdateStatus.Idle, service.updateStatus.value)
    }

    @Test
    fun `checkForUpdate - stays Idle when no update available`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val client = FakeAppUpdateClient()
        client.updateDataToReturn = Result.success(
            AppUpdateData(isAvailable = false, isFlexibleAllowed = false),
        )

        val service = InAppUpdateService(client, testScope)
        val launcher = createDummyLauncher()

        service.checkForUpdate(launcher)
        testScope.advanceUntilIdle()

        assertEquals(InAppUpdateStatus.Idle, service.updateStatus.value)
    }

    @Test
    fun `checkForUpdate - starts flexible update and handles progress states`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val client = FakeAppUpdateClient()
        client.updateDataToReturn = Result.success(
            AppUpdateData(isAvailable = true, isFlexibleAllowed = true),
        )

        val service = InAppUpdateService(client, testScope)
        val launcher = createDummyLauncher()

        service.checkForUpdate(launcher)
        testScope.advanceUntilIdle()

        // Simulate downloading progress
        client.emitInstallState(
            UpdateInstallState(
                status = InstallStatus.DOWNLOADING,
                bytesDownloaded = 500L,
                totalBytesToDownload = 1000L,
            ),
        )

        val downloadingStatus = service.updateStatus.value
        assertTrue(downloadingStatus is InAppUpdateStatus.Downloading)
        val progress = downloadingStatus as InAppUpdateStatus.Downloading
        assertEquals(500L, progress.bytesDownloaded)
        assertEquals(1000L, progress.totalBytes)

        // Simulate downloaded completion
        client.emitInstallState(
            UpdateInstallState(
                status = InstallStatus.DOWNLOADED,
                bytesDownloaded = 1000L,
                totalBytesToDownload = 1000L,
            ),
        )

        assertEquals(InAppUpdateStatus.Downloaded, service.updateStatus.value)
    }

    @Test
    fun `completeUpdate - no-ops when status is not Downloaded`() {
        val client = FakeAppUpdateClient()
        val service = InAppUpdateService(client)

        assertEquals(InAppUpdateStatus.Idle, service.updateStatus.value)
        service.completeUpdate()

        // Should not have invoked client.completeUpdate()
        assertEquals(false, client.completeUpdateCalled)
    }

    @Test
    fun `completeUpdate - invokes client completeUpdate when status is Downloaded`() {
        val client = FakeAppUpdateClient()
        val service = InAppUpdateService(client)

        client.emitInstallState(
            UpdateInstallState(
                status = InstallStatus.DOWNLOADED,
                bytesDownloaded = 1000L,
                totalBytesToDownload = 1000L,
            ),
        )

        assertEquals(InAppUpdateStatus.Downloaded, service.updateStatus.value)
        service.completeUpdate()

        assertEquals(true, client.completeUpdateCalled)
    }

    @Test
    fun `onDestroy - unregisters listener`() {
        val client = FakeAppUpdateClient()
        val service = InAppUpdateService(client)

        assertEquals(1, client.registeredListeners.size)
        service.onDestroy()
        assertEquals(0, client.registeredListeners.size)
    }
}
