package com.lxh.randompicker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lxh.randompicker.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var original: MutableList<String> = mutableListOf()
    private var pool: MutableList<String> = mutableListOf()
    private var history: MutableList<String> = mutableListOf()
    private var lastInputCanonical: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPick.setOnClickListener { onPick() }
        binding.btnReset.setOnClickListener { onReset() }
        binding.btnCopy.setOnClickListener { onCopy() }

        binding.etInput.hint = "用逗号、空格或换行分隔，例如：\n麻辣烫，寿司，拉面 烤肉\n米线; 汉堡 可乐鸡翅"
        updateUi()
    }

    private fun onPick() {
        syncFromInput()
        if (original.isEmpty()) {
            toast("请输入一些选项（用逗号、空格或换行分隔）")
            return
        }

        val noRepeat = binding.switchNoRepeat.isChecked
        val pickFrom = if (noRepeat) pool else original

        if (pickFrom.isEmpty()) {
            toast("已全部抽完啦，点“重置”再来一轮～")
            return
        }

        val index = Random.nextInt(pickFrom.size)
        val picked = pickFrom[index]
        if (noRepeat) {
            pool.removeAt(index)
        }
        history.add(picked)

        binding.tvResult.text = picked
        updateUi()
        hideKeyboard()
    }

    private fun onReset() {
        syncFromInput(force = true)
        pool = original.toMutableList()
        history.clear()
        binding.tvResult.text = "—"
        updateUi()
    }

    private fun onCopy() {
        val result = binding.tvResult.text?.toString()?.trim().orEmpty()
        if (result.isEmpty() || result == "—") {
            toast("还没有结果可复制")
            return
        }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("随机抽选结果", result))
        toast("已复制：$result")
    }

    private fun syncFromInput(force: Boolean = false) {
        val parsed = parseInput(binding.etInput.text?.toString().orEmpty())
        val canonical = parsed.joinToString("|")
        if (force || canonical != lastInputCanonical) {
            original = parsed.toMutableList()
            pool = original.toMutableList()
            history.clear()
            lastInputCanonical = canonical
        }
    }

    private fun parseInput(raw: String): List<String> {
        // 支持中文/英文逗号、分号、空格、制表符与换行
        val splitter = Regex("[,，;；\\s]+")
        val items = raw.split(splitter)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        // 去重且保序
        val seen = LinkedHashSet<String>()
        val deduped = ArrayList<String>()
        for (s in items) if (seen.add(s)) deduped.add(s)
        return deduped
    }

    private fun updateUi() {
        val total = original.size
        val remain = pool.size
        binding.tvStats.text = "剩余：$remain / 总计：$total"
        val recent = if (history.size <= 20) history else history.takeLast(20)
        binding.tvHistory.text = if (recent.isEmpty()) "（暂无）"
        else recent.joinToString("、")
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etInput.windowToken, 0)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
