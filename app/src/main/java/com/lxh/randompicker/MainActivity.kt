package com.lxh.randompicker

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lxh.randompicker.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var tablesDao: TablesDao
    private lateinit var itemsDao: ItemsDao
    private lateinit var settings: SettingsStore

    private lateinit var tableSpinner: Spinner
    private lateinit var noRepeatCheck: CheckBox
    private lateinit var inputEditText: EditText
    private lateinit var saveAsNewTableButton: Button
    private lateinit var overwriteTableButton: Button
    private lateinit var setDefaultButton: Button
    private lateinit var resetDrawnButton: Button
    private lateinit var pickButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var drawnListText: TextView

    private var currentTables: List<TableEntity> = emptyList()
    private var currentTableId: Long? = null
    private fun confirmDeleteCurrentTable() {
        val pos = tableSpinner.selectedItemPosition
        val table = currentTables.getOrNull(pos)
            ?: return toast("暂无可删除的表")

        if (currentTables.size <= 1) {
            toast("至少保留一个表，无法删除最后一个")
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除表")
            .setMessage("确认删除表「${table.name}」？该表的所有条目也会被删除。")
            .setPositiveButton("删除") { d, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val defaultId = settings.defaultTableIdFlow.first()
                    tablesDao.delete(table) // 级联删除 items

                    val remain = tablesDao.getAll()
                    if (remain.isNotEmpty()) {
                        if (defaultId == table.id) {
                            settings.setDefaultTableId(remain.first().id)
                        }
                    } else {
                        // 理论上不会走到这里，兜底清空默认
                        settings.clearDefaultTableId()
                    }

                    withContext(Dispatchers.Main) {
                        toast("已删除表：${table.name}")
                        refreshDrawnList()
                    }
                }
                d.dismiss()
            }
            .setNegativeButton("取消") { d, _ -> d.dismiss() }
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.get(this)
        tablesDao = db.tablesDao()
        itemsDao = db.itemsDao()
        settings = SettingsStore(this)

        bindViews()
        bindClicks()

        // 观察表变化，刷新下拉框
        lifecycleScope.launch {
            tablesDao.observeAll().collect { tables ->
                currentTables = tables
                val names = tables.map { it.name }
                tableSpinner.adapter =
                    ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names)

                // 长按 Spinner 删除当前选中的表
                tableSpinner.setOnLongClickListener {
                    confirmDeleteCurrentTable()
                    true
                }

                ensureSelectDefaultIfPossible()
                refreshDrawnList()
            }
        }


        // 加载“不重复抽取”开关
        lifecycleScope.launch {
            noRepeatCheck.isChecked = settings.noRepeatFlow.first()
        }
    }

    private suspend fun ensureSelectDefaultIfPossible() {
        val defaultId = settings.defaultTableIdFlow.first()
        val targetId = defaultId ?: currentTables.firstOrNull()?.id
        currentTableId = targetId
        val idx = currentTables.indexOfFirst { it.id == targetId }
        if (idx >= 0) tableSpinner.setSelection(idx)
    }

    private fun bindViews() {
        tableSpinner = findViewById(R.id.tableSpinner)
        noRepeatCheck = findViewById(R.id.noRepeatCheck)
        inputEditText = findViewById(R.id.inputEditText)
        saveAsNewTableButton = findViewById(R.id.saveAsNewTableButton)
        overwriteTableButton = findViewById(R.id.overwriteTableButton)
        setDefaultButton = findViewById(R.id.setDefaultButton)
        resetDrawnButton = findViewById(R.id.resetDrawnButton)
        pickButton = findViewById(R.id.pickButton)
        resultTextView = findViewById(R.id.resultTextView)
        drawnListText = findViewById(R.id.drawnListText)

        tableSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentTableId = currentTables.getOrNull(position)?.id
                lifecycleScope.launch { refreshDrawnList() }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun bindClicks() {
        noRepeatCheck.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch { settings.setNoRepeat(isChecked) }
        }

        saveAsNewTableButton.setOnClickListener {
            val itemsText = inputEditText.text.toString().trim()
            if (itemsText.isEmpty()) return@setOnClickListener toast("输入表项（用逗号分隔）")

            promptInput("给这个新表起个名字：") { name ->
                if (name.isBlank()) return@promptInput toast("表名不能为空")
                lifecycleScope.launch(Dispatchers.IO) {
                    val tableId = tablesDao.insert(TableEntity(name = name.trim()))
                    val itemEntities = itemsText.split(",")
                        .map { it.trim() }.filter { it.isNotEmpty() }
                        .map { ItemEntity(tableId = tableId, text = it) }
                    if (itemEntities.isNotEmpty()) itemsDao.insert(itemEntities)
                    withContext(Dispatchers.Main) {
                        toast("已保存为新表")
                        currentTableId = tableId
                        val idx = currentTables.indexOfFirst { it.id == tableId }
                        if (idx >= 0) tableSpinner.setSelection(idx)
                        inputEditText.setText("")
                        refreshDrawnList()
                    }
                }
            }
        }

        overwriteTableButton.setOnClickListener {
            val tableId = currentTableId ?: return@setOnClickListener toast("请先选择一个表")
            val itemsText = inputEditText.text.toString().trim()
            if (itemsText.isEmpty()) return@setOnClickListener toast("请输入表项（用逗号分隔）")

            lifecycleScope.launch(Dispatchers.IO) {
                itemsDao.deleteByTable(tableId)
                val itemEntities = itemsText.split(",")
                    .map { it.trim() }.filter { it.isNotEmpty() }
                    .map { ItemEntity(tableId = tableId, text = it) }
                if (itemEntities.isNotEmpty()) itemsDao.insert(itemEntities)
                withContext(Dispatchers.Main) {
                    toast("已覆盖当前表")
                    inputEditText.setText("")
                    refreshDrawnList()
                }
            }
        }

        setDefaultButton.setOnClickListener {
            val tableId = currentTableId ?: return@setOnClickListener toast("请先选择一个表")
            lifecycleScope.launch { settings.setDefaultTableId(tableId); toast("已设为默认表") }
        }

        resetDrawnButton.setOnClickListener {
            val tableId = currentTableId ?: return@setOnClickListener toast("请先选择一个表")
            lifecycleScope.launch(Dispatchers.IO) {
                itemsDao.resetDrawn(tableId)
                withContext(Dispatchers.Main) { toast("已清空已抽到"); refreshDrawnList() }
            }
        }

        pickButton.setOnClickListener {
            val tableId = currentTableId ?: return@setOnClickListener toast("暂无可用表，请先新建或等待预置表加载")
            lifecycleScope.launch(Dispatchers.IO) {
                val noRepeat = settings.noRepeatFlow.first()
                val candidates = if (noRepeat) itemsDao.getUndrawn(tableId) else itemsDao.getByTable(tableId)
                if (candidates.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        toast(if (noRepeat) "本表已抽完，请清空已抽到或添加新项" else "当前表为空")
                    }
                    return@launch
                }
                val pick = candidates[Random().nextInt(candidates.size)]
                if (noRepeat && !pick.isDrawn) itemsDao.markDrawn(pick.id, System.currentTimeMillis())
                withContext(Dispatchers.Main) {
                    resultTextView.text = "抽中了：${pick.text}"
                    refreshDrawnList()
                }
            }
        }
    }

    private suspend fun refreshDrawnList() {
        val tableId = currentTableId ?: return
        val all = withContext(Dispatchers.IO) { itemsDao.getByTable(tableId) }
        val drawn = all.filter { it.isDrawn }.sortedBy { it.drawnAt ?: 0L }
        drawnListText.text = if (drawn.isEmpty()) "（暂无）" else drawn.joinToString("，") { it.text }
    }

    private fun promptInput(title: String, onOk: (String) -> Unit) {
        val edit = EditText(this).apply { hint = "请输入" }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(edit)
            .setPositiveButton("确定") { d, _ -> onOk(edit.text.toString()); d.dismiss() }
            .setNegativeButton("取消") { d, _ -> d.dismiss() }
            .create().show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
