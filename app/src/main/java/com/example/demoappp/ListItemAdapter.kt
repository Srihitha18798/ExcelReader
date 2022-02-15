package com.example.demoappp

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import androidx.recyclerview.widget.RecyclerView
import com.example.demoappp.databinding.CellListitemBinding


class ListItemAdapter(
    private val exampleList: ArrayList<ListItems>,
    private val listener: ExcelRowClickListener
) :
    RecyclerView.Adapter<ListItemAdapter.ExampleViewHolder>() {
    var exampleListFull = ArrayList<ListItems>()

    companion object {
        var searchString: String? = null
    }

    init {
        exampleListFull = exampleList
    }

    fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
                searchString = constraint.toString()
                val charSearch = constraint.toString()
                if (charSearch.isEmpty()) {
                    exampleListFull = exampleList
                } else {
                    val resultList = ArrayList<ListItems>()
                    resultList.addAll(exampleList.filter {
                        it.singleRowList.filter {
                            it.value?.contains(charSearch, true) == true
                        }.isNotEmpty()
                    })

                    exampleListFull = resultList
                }
                val filterResults = FilterResults()
                filterResults.values = exampleListFull
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                exampleListFull = results?.values as ArrayList<ListItems>
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleViewHolder {
        return ExampleViewHolder(
            CellListitemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = exampleListFull.size

    override fun onBindViewHolder(holder: ExampleViewHolder, position: Int) {
        val currentItem = exampleListFull[position]
        val adapter = Adapter(currentItem.singleRowList.orEmpty(), object : ExcelRowClickListener {
            override fun onRowClick(position1: Int) {
                listener.onRowClick(position)
                val status = currentItem.singleRowList.last().value
                if (status?.contentEquals(AppConstant.Completed) == true) {
                    currentItem.singleRowList.last().value = AppConstant.Pending
                } else {
                    currentItem.singleRowList.last().value = AppConstant.Completed
                }
                notifyItemChanged(position)
            }
        })
        holder.binding.recyclerView.adapter = adapter
        if (currentItem.singleRowList.last().value.equals(AppConstant.Completed)) {
            holder.binding.oneRowCard.setBackgroundColor(Color.YELLOW)
        }
        if (currentItem.singleRowList.last().value.equals(AppConstant.Pending)) {
            holder.binding.oneRowCard.setBackgroundColor(Color.WHITE)
        }
    }


    inner class ExampleViewHolder(val binding: CellListitemBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }

    fun clear() {
        val oldSize = exampleListFull.size
        exampleListFull.clear()
        notifyItemRangeRemoved(0, oldSize)
    }

    fun setData(list: List<ListItems>) {
        this.exampleListFull.addAll(list)
        notifyItemRangeInserted(0, exampleListFull.size)
    }
}