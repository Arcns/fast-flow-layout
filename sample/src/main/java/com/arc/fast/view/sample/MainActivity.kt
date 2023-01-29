package com.arc.fast.view.sample

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.arc.fast.flowlayout.sample.R
import com.arc.fast.flowlayout.sample.databinding.ActivityMainBinding
import com.arc.fast.flowlayout.sample.databinding.LayoutTagBinding
import com.arc.fast.view.FastFlowAdapter
import com.arc.fast.view.sample.extension.applyFullScreen
import com.arc.fast.view.sample.extension.setLightSystemBar

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFullScreen()
        setLightSystemBar(true)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val data = arrayListOf<String>()
        for (i in 0..20) {
            data.add("tag_$i")
        }
        binding.flow1.adapter = FastFlowAdapter(
            layoutRes = R.layout.layout_tag,
            data = data,
            convert = { layout, item, position ->
                val tvTag = layout.findViewById<TextView>(R.id.tv_tag)
                tvTag.text = item
            },
            onItemClick = { layout, item, position ->
                Toast.makeText(this@MainActivity, item, Toast.LENGTH_SHORT).show()
            }
        )
        binding.flow2.adapter = FastFlowAdapter(
            data = data,
            onCreateItem = { layoutInflater, parent, item, position ->
                return@FastFlowAdapter LayoutTagBinding.inflate(layoutInflater, null, false).apply {
                    tvTag.text = item
                }.root
            },
            onItemClick = { layout, item, position ->
                Toast.makeText(this@MainActivity, item, Toast.LENGTH_SHORT).show()
            },
            onCreateExpand = { layoutInflater, parent ->
                return@FastFlowAdapter LayoutTagBinding.inflate(layoutInflater, null, false).apply {
                    tvTag.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                    tvTag.text = "展开"
                }.root
            },
            onExpand = { expand, isExpand ->
                (expand as? TextView)?.text = if (isExpand) "收缩" else "展开"
                false
            }
        )
    }

}