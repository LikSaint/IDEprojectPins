package dev.projectpins

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.openapi.vfs.pointers.VirtualFilePointer
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.messages.Topic
import java.util.UUID

fun interface PinsListener { fun changed() }

@Service(Service.Level.PROJECT)
@State(name = "ProjectPins", storages = [Storage("projectPins.xml")])
class ProjectPinsService(private val project: Project) : PersistentStateComponent<PinsState>, Disposable {
    private data class Reference(val owner: Disposable, val pointer: VirtualFilePointer)
    private var saved = PinsState()
    private val references = mutableMapOf<String, Reference>()

    init {
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                if (events.any { it is VFileCreateEvent || it is VFileDeleteEvent || it is VFileMoveEvent ||
                        (it is VFilePropertyChangeEvent && it.propertyName == VirtualFile.PROP_NAME) }) changed()
            }
        })
    }

    @Synchronized
    override fun getState(): PinsState = PinsState(saved.pins.map { pin ->
        pin.copy(targetPath = references[pin.id]?.pointer?.let { it.file?.url ?: it.url }?.let(::storeUrl) ?: pin.targetPath)
    }.toMutableList())

    @Synchronized
    override fun loadState(state: PinsState) {
        references.values.forEach { Disposer.dispose(it.owner) }
        references.clear()
        saved = PinsState(state.pins.map { it.copy(openInEditor = it.openInEditor && !it.directory) }.toMutableList())
        saved.pins.forEach { pin ->
            if (pin.id.isBlank() || references.containsKey(pin.id)) pin.id = UUID.randomUUID().toString()
            track(pin)
        }
        changed()
    }

    fun pins(): List<ProjectPin> = state.pins

    @Synchronized
    fun resolve(id: String): VirtualFile? = references[id]?.pointer?.file?.takeIf { it.isValid }

    @Synchronized
    fun add(file: VirtualFile, name: String, openInEditor: Boolean): ProjectPin {
        findByTarget(file)?.let { return it.copy() }
        val pin = ProjectPin(UUID.randomUUID().toString(), name.trim().ifEmpty { file.name },
            storeUrl(file.url), file.isDirectory, openInEditor && !file.isDirectory)
        saved.pins.add(pin)
        track(pin)
        changed()
        return pin.copy()
    }

    @Synchronized
    fun findByTarget(file: VirtualFile): ProjectPin? {
        val url = file.url
        return saved.pins.firstOrNull { pin ->
            val pointerUrl = references[pin.id]?.pointer?.file?.url ?: references[pin.id]?.pointer?.url
            pointerUrl == url || expandPath(pin.targetPath) == url
        }
    }

    @Synchronized
    fun update(id: String, name: String, openInEditor: Boolean, targetPath: String? = null) {
        val index = saved.pins.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = state.pins[index]
        val newPath = targetPath?.trim()?.takeIf { it.isNotEmpty() } ?: old.targetPath
        val target = VirtualFileManager.getInstance().findFileByUrl(expandPath(newPath))
        val directory = target?.isDirectory ?: old.directory
        val pin = old.copy(name = name.trim().ifEmpty { old.name }, targetPath = newPath,
            directory = directory, openInEditor = openInEditor && !directory)
        saved.pins[index] = pin
        if (newPath != old.targetPath) {
            references.remove(id)?.let { Disposer.dispose(it.owner) }
            track(pin)
        }
        changed()
    }

    @Synchronized
    fun remove(id: String) {
        saved.pins.removeAll { it.id == id }
        references.remove(id)?.let { Disposer.dispose(it.owner) }
        changed()
    }

    @Synchronized
    fun move(id: String, offset: Int) {
        val from = saved.pins.indexOfFirst { it.id == id }
        val to = from + offset
        if (from < 0 || to !in saved.pins.indices) return
        saved.pins.add(to, saved.pins.removeAt(from))
        changed()
    }

    private fun track(pin: ProjectPin) {
        val owner = Disposer.newDisposable("Project pin ${pin.id}")
        Disposer.register(this, owner)
        references[pin.id] = Reference(owner, VirtualFilePointerManager.getInstance().create(expandPath(pin.targetPath), owner, null))
    }

    internal fun storeUrl(url: String): String {
        val baseUrl = project.basePath?.let(VfsUtilCore::pathToUrl)?.trimEnd('/') ?: return url
        return when {
            url == baseUrl -> "."
            url.startsWith("$baseUrl/") -> url.removePrefix("$baseUrl/")
            else -> url
        }
    }

    internal fun expandPath(path: String): String {
        if (path.contains("://")) return path
        val base = project.basePath ?: return "file:///__project_pins_missing_base__/$path"
        return VfsUtilCore.pathToUrl(if (path == ".") base else "$base/$path")
    }

    private fun changed() {
        ToolWindowManager.getInstance(project).invokeLater {
            if (!project.isDisposed) project.messageBus.syncPublisher(TOPIC).changed()
        }
    }

    override fun dispose() = Unit

    companion object {
        val TOPIC = Topic.create("Project pins changed", PinsListener::class.java)
        fun getInstance(project: Project): ProjectPinsService = project.service()
    }
}
