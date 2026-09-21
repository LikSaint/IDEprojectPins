package dev.projectpins

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer

class ProjectPinsServiceTest : BasePlatformTestCase() {
    private val service get() = ProjectPinsService.getInstance(project)
    override fun setUp() {
        super.setUp()
        service.loadState(PinsState())
    }
    fun testSerializationRoundTripKeepsOrderAndSettings() {
        val a = service.add(myFixture.addFileToProject("src/a.txt", "a").virtualFile, "A", false)
        val b = service.add(myFixture.addFileToProject("src/b.txt", "b").virtualFile, "B", true)
        service.move(b.id, -1)
        val xml = XmlSerializer.serialize(service.state)
        service.loadState(XmlSerializer.deserialize(xml, PinsState::class.java))
        assertEquals(listOf(b.id, a.id), service.pins().map { it.id })
        assertTrue(service.pins()[0].openInEditor)
        assertFalse(service.pins()[1].openInEditor)
        assertEquals("B", service.pins()[0].name)
        assertNotNull(service.resolve(a.id))
    }
    fun testOrderingAndBoundaryMoves() {
        val a = service.add(myFixture.addFileToProject("a.txt", "").virtualFile, "A", false)
        val b = service.add(myFixture.addFileToProject("b.txt", "").virtualFile, "B", false)
        service.move(a.id, -1)
        service.move(b.id, 1)
        assertEquals(listOf(a.id, b.id), service.pins().map { it.id })
        service.move(a.id, 1)
        assertEquals(listOf(b.id, a.id), service.pins().map { it.id })
        service.remove(b.id)
        assertEquals(listOf(a.id), service.pins().map { it.id })
    }
    fun testSettingsAndDefensiveSnapshots() {
        val file = myFixture.addFileToProject("a.txt", "").virtualFile
        val pin = service.add(file, "A", false)
        service.update(pin.id, "Renamed", true)
        val snapshot = service.pins().single()
        assertEquals("Renamed", snapshot.name)
        assertTrue(snapshot.openInEditor)
        snapshot.name = "outside mutation"
        assertEquals("Renamed", service.pins().single().name)
        val folder = service.add(file.parent, "Folder", true)
        service.update(folder.id, "Directory", true)
        assertFalse(service.pins().last().openInEditor)
    }
    fun testMissingTargetAndRepair() {
        val file = myFixture.addFileToProject("gone.txt", "").virtualFile
        val pin = service.add(file, "Missing", true)
        WriteCommandAction.runWriteCommandAction(project) { file.delete(this) }
        assertNull(service.resolve(pin.id))
        service.loadState(service.state)
        assertNull(service.resolve(pin.id))
        val replacement = myFixture.addFileToProject("replacement.txt", "").virtualFile
        service.update(pin.id, "Repaired", true, service.storeUrl(replacement.url))
        assertEquals(replacement, service.resolve(pin.id))
    }
    fun testPointerTracksRenameAndParentMoveAcrossSerialization() {
        val file = myFixture.addFileToProject("old/a.txt", "").virtualFile
        val destination = myFixture.tempDirFixture.findOrCreateDir("destination")
        val pin = service.add(file, "Custom name", false)
        WriteCommandAction.runWriteCommandAction(project) {
            file.rename(this, "renamed.txt")
            file.parent.move(this, destination)
        }
        assertEquals(file, service.resolve(pin.id))
        assertEquals(service.storeUrl(file.url), service.state.pins.single().targetPath)
        service.loadState(XmlSerializer.deserialize(XmlSerializer.serialize(service.state), PinsState::class.java))
        assertEquals(file, service.resolve(pin.id))
        assertEquals("Custom name", service.pins().single().name)
    }
    fun testProjectRelativePathRoundTripAndExternalFallback() {
        val base = project.basePath!!
        val url = com.intellij.openapi.vfs.VfsUtilCore.pathToUrl("$base/src/a.txt")
        assertEquals("src/a.txt", service.storeUrl(url))
        assertEquals(url, service.expandPath("src/a.txt"))
        assertEquals("file:///outside/a.txt", service.storeUrl("file:///outside/a.txt"))
        assertEquals("file:///outside/a.txt", service.expandPath("file:///outside/a.txt"))
    }
}
