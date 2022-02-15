package com.example.demoappp

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.demoappp.databinding.CustomLayoutBinding
import kotlinx.android.synthetic.main.custom_layout.view.*
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class Adapter(
    private val exampleList: List<SingleRow>,
    private val listener: ExcelRowClickListener
) :
    RecyclerView.Adapter<Adapter.ExampleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleViewHolder {
        return ExampleViewHolder(
            CustomLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = exampleList.size

    override fun onBindViewHolder(holder: ExampleViewHolder, position: Int) {
        val currentItem = exampleList[position]
        holder.binding.textView1.text = currentItem.name
        holder.binding.textView2.text = currentItem.value

        if (ListItemAdapter.searchString != null && !ListItemAdapter.searchString!!.isEmpty()) {
            val sb = SpannableStringBuilder(currentItem.value)
            val word: Pattern =
                Pattern.compile(ListItemAdapter.searchString!!.toLowerCase(Locale.ROOT))
            val match: Matcher = word.matcher(currentItem.value?.toLowerCase(Locale.ROOT))
            while (match.find()) {
                val fcs = ForegroundColorSpan(Color.WHITE)
                sb.setSpan(fcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                val bcs = BackgroundColorSpan(Color.DKGRAY)
                sb.setSpan(bcs, match.start(), match.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
            holder.binding.textView2.setText(sb)
        } else {
            holder.binding.textView2.text = currentItem.value
        }
    }

    inner class ExampleViewHolder(val binding: CustomLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val textView1: TextView = itemView.text_view1
        val textView2: TextView = itemView.text_view2

        init {
            binding.root.setOnClickListener {
                listener.onRowClick(adapterPosition)
            }
        }
    }

}