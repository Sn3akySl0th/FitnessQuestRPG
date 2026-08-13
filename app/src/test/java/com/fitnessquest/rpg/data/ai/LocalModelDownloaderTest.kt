package com.fitnessquest.rpg.data.ai

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.Proxy

class LocalModelDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var downloader: LocalModelDownloader

    private class FakeContext(private val baseDir: File) : android.content.ContextWrapper(null) {
        override fun getExternalFilesDir(type: String?): File = baseDir
        override fun getSystemService(name: String): Any? = null
        override fun getApplicationContext(): Context = this
    }

    @Before
    fun setUp() {
        context = FakeContext(tempFolder.root)
        downloader = LocalModelDownloader(context)
    }

    @Test
    fun `isModelReady returns false when model file does not exist`() {
        val spec = LocalModelSpec(
            id = "test_nonexistent",
            displayName = "Test Model",
            version = "1.0",
            approxSizeMb = 100,
            downloadUrl = "https://example.com/model.bin"
        )
        assertFalse(downloader.isModelReady(spec))
    }

    @Test
    fun `isModelReady returns true when model file meets size requirement`() {
        val spec = LocalModelSpec(
            id = "test_ready",
            displayName = "Test Model",
            version = "1.0",
            approxSizeMb = 1,
            downloadUrl = "https://example.com/model.bin",
            expectedBytes = 1024 * 1024L
        )
        val file = downloader.getModelFile(spec.id)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(1024 * 1024))

        assertTrue(downloader.isModelReady(spec))
    }

    @Test
    fun `migrateFromPlayAsset copies pad model to target directory`() {
        val padFile = tempFolder.newFile("pad_model.bin")
        padFile.writeBytes(ByteArray(1024))

        val fakePadFile = object : File(padFile.absolutePath) {
            override fun length(): Long = 1_350_000_000L
            override fun exists(): Boolean = true
        }

        val target = downloader.getModelFile("gemma_2b_it")
        target.delete()

        val migrated = downloader.migrateFromPlayAsset(fakePadFile)
        assertTrue(migrated)
        assertTrue(target.exists())
    }

    @Test
    fun `migrateFromPlayAsset returns false when source file is null or empty`() {
        assertFalse(downloader.migrateFromPlayAsset(null))
        val emptyFile = tempFolder.newFile("empty.bin")
        assertFalse(downloader.migrateFromPlayAsset(emptyFile))
    }
}
