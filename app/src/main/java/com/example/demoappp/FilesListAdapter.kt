package com.example.demoappp

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.demoappp.databinding.ListViewBinding
import kotlinx.android.synthetic.main.list_view.view.*
import java.io.File

class FilesListAdapter(private val context: Context) :
    RecyclerView.Adapter<FilesListAdapter.MyViewHolder>() {
    private val files: MutableList<ExcellFile> = arrayListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            ListViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return files.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem: ExcellFile = files[position]
        holder.binding.tvTitle.text = currentItem.name


        holder.itemView.setOnClickListener {
            Log.e("File Path", currentItem.path)
            openFile(currentItem.path)

        }
        holder.itemView.ivDelete.setOnClickListener {
            Log.e("delete", "button clicked")
            deleteSelectedFile(currentItem.path)
        }
    }

    private fun deleteSelectedFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
        if (context is FilesList) {
            context.getFiles()
        }
    }

    private fun openFile(path: String) {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("excellPath", path)
        context.startActivity(intent)

    }

    class MyViewHolder(val binding: ListViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val textView1: TextView = itemView.tv_title
    }

    fun View.gone() {
        this.visibility = View.GONE
    }

    fun View.visible() {
        this.visibility = View.VISIBLE
    }

    fun View.invisible() {
        this.visibility = View.INVISIBLE
    }

    fun clear() {
        val oldSize = files.size
        files.clear()
        notifyItemRangeRemoved(0, oldSize)
    }

    fun setData(list: MutableList<ExcellFile>) {
        this.files.addAll(list)
        notifyItemRangeInserted(0, files.size)
    }

}
