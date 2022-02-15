package com.example.demoappp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.demoappp.databinding.ActivityFilesListBinding
import kotlinx.android.synthetic.main.activity_files_list.*
import java.io.File


class FilesList : AppCompatActivity() {
    private lateinit var binding: ActivityFilesListBinding
    private var adapter: FilesListAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    override fun onResume() {
        super.onResume()
        getFiles()
    }

    private fun init() {

        setupClickListener()

        setUpAdapter()
    }

    private fun setupClickListener() {
        binding.excellReader.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }

    private fun setUpAdapter() {
        adapter = FilesListAdapter(this)
        binding.rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)

    }

    fun getFiles() {
        val listOfFiles: MutableList<ExcellFile> = arrayListOf()
        showProgress()
        val fileDir = File(this.filesDir, "doc")
        if (fileDir.exists()) {
            val files = fileDir.listFiles()
            files?.forEach {
                listOfFiles.add(
                    ExcellFile(it.name, it.absolutePath)
                )
            }
        }

        Log.e("files", listOfFiles.toString())
        listOfFiles.reverse()
        hideProgress()
        adapter?.clear()
        adapter?.setData(listOfFiles)
        checkForNoData()
    }

    private fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE

    }

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE

    }

    private fun checkForNoData() {
        if (adapter?.itemCount ?: 0 == 0) {
            binding.llNoDataFound.tvNoDataFound.isVisible
        } else {
            binding.llNoDataFound.tvNoDataFound.isInvisible
        }
    }

}
