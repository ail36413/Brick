package com.ail.brick.sample.data

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.*
import com.ail.brick.data.BaseDao
import com.ail.brick.data.BrickDatabase
import com.ail.brick.data.dbResultOf
import com.ail.brick.sample.databinding.ActivityDataDemoBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * brick-data 演示页面
 *
 * 展示功能：
 * - Room DSL 构建数据库（内存数据库）
 * - BaseDao 通用 CRUD（insert / query / update / delete）
 * - Upsert（插入或更新）
 * - DbResult 结果包装
 */
class DataDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataDemoBinding
    private lateinit var db: DemoDatabase
    private lateinit var noteDao: NoteDao
    private var noteIdCounter = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 使用 BrickDatabase DSL 构建内存数据库
        db = BrickDatabase.buildInMemory<DemoDatabase>(this) {
            allowMainThreadQueries()
        }
        noteDao = db.noteDao()
        setupButtons()
    }

    private fun setupButtons() {
        // ---- 基本 CRUD ----
        binding.btnInsert.setOnClickListener {
            lifecycleScope.launch {
                noteIdCounter++
                val note = NoteEntity(
                    id = noteIdCounter,
                    title = "笔记 #$noteIdCounter",
                    content = "这是第 $noteIdCounter 条笔记内容",
                    createdAt = System.currentTimeMillis()
                )
                val rowId = noteDao.insert(note)
                showResult("插入成功:\nid=$noteIdCounter, rowId=$rowId\ntitle=\"${note.title}\"")
            }
        }

        binding.btnQueryAll.setOnClickListener {
            lifecycleScope.launch {
                val notes = noteDao.getAll()
                if (notes.isEmpty()) {
                    showResult("表为空，请先插入数据")
                } else {
                    val sb = StringBuilder("查询到 ${notes.size} 条记录:\n\n")
                    notes.forEach { note ->
                        sb.appendLine("  #${note.id} | ${note.title}")
                        sb.appendLine("       ${note.content}")
                    }
                    showResult(sb.toString())
                }
            }
        }

        binding.btnUpdate.setOnClickListener {
            lifecycleScope.launch {
                val notes = noteDao.getAll()
                if (notes.isEmpty()) {
                    showResult("表为空，无法更新")
                    return@launch
                }
                val target = notes.last()
                val updated = target.copy(
                    title = "${target.title}（已更新）",
                    content = "内容已被更新于 ${System.currentTimeMillis()}"
                )
                val count = noteDao.update(updated)
                showResult("更新成功:\n受影响行数: $count\n更新后: ${updated.title}")
            }
        }

        binding.btnDelete.setOnClickListener {
            lifecycleScope.launch {
                val notes = noteDao.getAll()
                if (notes.isEmpty()) {
                    showResult("表为空，无法删除")
                    return@launch
                }
                val target = notes.last()
                val count = noteDao.delete(target)
                showResult("删除成功:\n删除了: ${target.title}\n受影响行数: $count")
            }
        }

        // ---- 高级功能 ----
        binding.btnUpsert.setOnClickListener {
            lifecycleScope.launch {
                noteIdCounter++
                // 先插入
                val note = NoteEntity(
                    id = 999,
                    title = "Upsert 笔记",
                    content = "第 $noteIdCounter 次 upsert",
                    createdAt = System.currentTimeMillis()
                )
                noteDao.upsert(note)
                val result = noteDao.getById(999)
                showResult(
                    "Upsert 执行完成（id=999 固定）:\n" +
                    "title = \"${result?.title}\"\n" +
                    "content = \"${result?.content}\"\n\n" +
                    "→ 首次执行为 insert，再次执行为 update"
                )
            }
        }

        binding.btnDbResult.setOnClickListener {
            lifecycleScope.launch {
                val result = dbResultOf { noteDao.getAll() }
                val text = result
                    .onSuccess { showResult("DbResult.Success:\n共 ${it.size} 条记录") }
                    .onFailure { showResult("DbResult.Failure:\n${it.message}") }
                // 也演示 map
                val countResult = result.map { it.size }
                showResult(
                    "DbResult 包装:\n" +
                    "isSuccess = ${result.isSuccess}\n" +
                    "getOrNull()?.size = ${result.getOrNull()?.size}\n" +
                    "map { size } = ${countResult.getOrNull()}"
                )
            }
        }

        binding.btnDeleteAll.setOnClickListener {
            lifecycleScope.launch {
                noteDao.clearAll()
                noteIdCounter = 0
                showResult("已清空所有数据\n计数器已重置")
            }
        }
    }

    private fun showResult(text: String) {
        binding.tvResult.text = text
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }

    // ==================== Demo 数据库定义 ====================

    @Entity(tableName = "notes")
    data class NoteEntity(
        @PrimaryKey val id: Long,
        @ColumnInfo(name = "title") val title: String,
        @ColumnInfo(name = "content") val content: String,
        @ColumnInfo(name = "created_at") val createdAt: Long
    )

    @Dao
    abstract class NoteDao : BaseDao<NoteEntity>() {
        @Query("SELECT * FROM notes ORDER BY created_at DESC")
        abstract suspend fun getAll(): List<NoteEntity>

        @Query("SELECT * FROM notes WHERE id = :id")
        abstract suspend fun getById(id: Long): NoteEntity?

        @Query("SELECT * FROM notes ORDER BY created_at DESC")
        abstract fun observeAll(): Flow<List<NoteEntity>>

        @Query("DELETE FROM notes")
        abstract suspend fun clearAll()
    }

    @Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
    abstract class DemoDatabase : RoomDatabase() {
        abstract fun noteDao(): NoteDao
    }
}
