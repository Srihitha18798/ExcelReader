package com.example.demoappp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.demoappp.databinding.ActivityMainBinding
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.recycler_view
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.layout_dialog.view.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private val tag: String = "main"
    private val requestCode = 101
    private val PICK_FILE = 100
    private lateinit var binding: ActivityMainBinding
    private var adapter: ListItemAdapter? = null
    private lateinit var viewModel: ExcellReaderViewModel
    private var file: File? = null
    private var fileUri: Uri? = null
    private var url: String? = null
    private val STORAGE_PERMISSION_CODE: Int = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        init()
    }

    private fun init() {
        viewModel = ViewModelProvider(this).get(ExcellReaderViewModel::class.java)
        viewModel.fileDir = File(this.filesDir, AppConstant.doc)
        if (intent.extras?.containsKey("excellPath") == true) {
            val filePath = intent.extras?.getString("excellPath").orEmpty()
            showProgress()
            if (viewModel.isEncrypt(filePath)) {
                Log.e(tag, "Document encrypted")
                openDialog(filePath)
            } else {
                Log.e(tag, "Document not encrypted")
                viewModel.readExcelFileFromAssets(filePath)
            }
        }

        attachObserver()
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLInputFactory",
            "com.fasterxml.aalto.stax.InputFactoryImpl"
        );
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLOutputFactory",
            "com.fasterxml.aalto.stax.OutputFactoryImpl"
        );
        System.setProperty(
            "org.apache.poi.javax.xml.stream.XMLEventFactory",
            "com.fasterxml.aalto.stax.EventFactoryImpl"
        );

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        setupClickListener()

        setUpAdapter()
    }

    private fun setupClickListener() {
        val fab = binding.selectFile
        fab.setOnClickListener(View.OnClickListener() {
            checkForStoragePermission();
        })
        binding.submitButton.setOnClickListener {
            url = binding.editText.text.toString()
            checkForPermissions(url)
        }
        binding.qrCodeScan.setOnClickListener {
            val scanner = IntentIntegrator(this)
            scanner.initiateScan()
        }

        binding.files.setOnClickListener {
            val intent = Intent(this, FilesList::class.java)
            startActivity(intent)
        }
    }

    private fun setUpAdapter() {
        adapter = ListItemAdapter(arrayListOf(), object : ExcelRowClickListener {
            override fun onRowClick(position: Int) {
                viewModel.setPositionCompleted(position)

            }
        })
        binding.recyclerView.adapter = adapter
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.setHasFixedSize(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        val searchMenu = menu.findItem(R.id.search_bar)
        val searchView = searchMenu.actionView as androidx.appcompat.widget.SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                adapter?.getFilter()?.filter(newText)
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.search_bar) {
            Log.e(tag, "Search Option")
            return true
        } else if (item.itemId == R.id.actionFilter) {
            Log.e(tag, "Filter Option")
            showSortingFilters()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSortingFilters() {
        val firstRow: MutableList<String> = arrayListOf()
        viewModel.excellFirstRowData.observe(this, androidx.lifecycle.Observer {
            for (item in it) {
                firstRow.add(item)
            }
        })
        val builder = AlertDialog.Builder(this)
        builder.setSingleChoiceItems(firstRow.toTypedArray(), -1) { dialog, which ->
            val selected = firstRow.get(which)
            viewModel.sortBy(selected)
            dialog.dismiss()
        }
        builder.create().show()
    }



    private fun openDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE && resultCode == Activity.RESULT_OK) {
            var mimeTypeExtension: String? = ""
            data?.data?.also { uri ->
                Log.e(tag, "ApachPOI Selected file Uri : " + uri)
                mimeTypeExtension = uri.getExtention(this)
                Log.e(tag, "ApachPOI Selected file mimeTypeExtension : " + mimeTypeExtension)
                if (mimeTypeExtension != null && mimeTypeExtension?.isNotEmpty() == true) {

                    if (mimeTypeExtension?.contentEquals("xlsx") == true
                        || mimeTypeExtension?.contentEquals("xls") == true
                    ) {
                        Log.e(
                            tag,
                            "ApachPOI Selected file mimeTypeExtension valid : " + mimeTypeExtension
                        )
                    } else {
                        Toast.makeText(this, "invalid file selected", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                copyFileAndExtract(uri, mimeTypeExtension.orEmpty())
            }
        } else {
            val result: IntentResult =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    url = result.getContents()
                    binding.editText.setText(url)
                    Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG)
                        .show();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }

        }
    }

    fun Uri.getExtention(context: Context): String? {
        var extension: String? = ""
        extension = if (this.scheme == ContentResolver.SCHEME_CONTENT) {
            val mime = MimeTypeMap.getSingleton()
            mime.getExtensionFromMimeType(context.getContentResolver().getType(this))
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                MimeTypeMap.getFileExtensionFromUrl(
                    FileProvider.getUriForFile(
                        context,
                        context.packageName + ".provider",
                        File(this.path)
                    )
                        .toString()
                )
            } else {
                MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(this.path)).toString())
            }
        }
        return extension
    }

    private fun attachObserver() {
        viewModel.excelExceptionListData.observe(this, androidx.lifecycle.Observer {
            it.apply {
                checkForNoData()
                hideProgress()
                binding.llNoDataFound.tvNoDataFound.text = this.orEmpty()
            }
        })
        viewModel.excelDataListLiveData.observe(this, androidx.lifecycle.Observer {
            it?.apply {
                adapter?.clear()
                adapter?.setData(this)
                checkForNoData()
                hideProgress()
            }
        })
    }

    private fun checkForNoData() {
        if (adapter?.itemCount ?: 0 == 0) {
            binding.llNoDataFound.tvNoDataFound.isVisible
        } else {
            binding.llNoDataFound.tvNoDataFound.isInvisible
        }
    }

    private fun checkForPermissions(url: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            } else {
                startDownload(url)
            }
        } else {
            startDownload(url)
        }
    }
    private fun checkForStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                requestCode
            )
            openDocument()
        } else {
            openDocument()
        }
    }

    private fun startDownload(url: String?) {
        val fileName = URLUtil.guessFileName(url, null, MimeTypeMap.getFileExtensionFromUrl(url))
        Log.d(tag, fileName)
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setTitle(fileName)
        request.setDescription("The File is Downoading...")
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            uri.lastPathSegment
        )
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }

    private fun copyFileAndExtract(uri: Uri, extension: String) {
        showProgress()
        val dir = File(this.filesDir, "doc")
        dir.mkdirs()
        val fileName = getFileName(uri)
        file = File(dir, fileName)
        file?.createNewFile()
        val fout = FileOutputStream(file)
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                fout.use { output ->
                    inputStream.copyTo(output)
                    output.flush()
                }
            }
            fileUri = FileProvider.getUriForFile(this, packageName + ".provider", file!!)
        } catch (e: Exception) {
            hideProgress()
            fileUri = uri
            e.printStackTrace()
        }
        fileUri?.apply {
            file?.apply {
                Log.e(tag, this.absolutePath)
                if (viewModel.isEncrypt(this.absolutePath)) {
                    Log.e(tag, "Document encrypted")
                    openDialog(this.absolutePath)
                } else {
                    Log.e(tag, "Document not encrypted")
                    viewModel.readExcelFileFromAssets(this.absolutePath)
                }
            }
        }
    }

    private fun openDialog(path: String) {
        Log.e(tag, "Open Dialog")
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Password")
        builder.setMessage("This File is Password Protected. Enter Password to Open")
        builder.setView(mDialogView)
        builder.setPositiveButton("Submit") { dialog, which ->
            val password = mDialogView.edit_password.text.toString()
            if (password.isEmpty()) {
                hideProgress()
                Toast.makeText(this, "Password Field is Empty.", Toast.LENGTH_SHORT).show()
            } else {
                val result: Boolean = viewModel.checkPassword(password, path)
                if (!result) {
                    Log.e(tag, "Password is incorrect")
                    Toast.makeText(this, "Password is incorrect.", Toast.LENGTH_SHORT).show()
                    openDialog(path)
                    //dialog.dismiss()
                    //hideProgress()
                } else {
                    Log.e(tag, "Password is correct")
                    Toast.makeText(this, "Password is Correct.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    viewModel.readExcelFileFromAssets(path, password)
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            Toast.makeText(this, "Clicked Cancel", Toast.LENGTH_SHORT).show()
            hideProgress()
            dialog.dismiss()
        }
        builder.create().show()
    }

    fun getFileName(uri: Uri): String? = when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
        else -> uri.path?.let(::File)?.name
    }

    private fun getContentFileName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                .let(cursor::getString)
        }
    }.getOrNull()

    private var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                intent.extras?.let {
                    val downloadedFileId = it.getLong(DownloadManager.EXTRA_DOWNLOAD_ID)
                    val downloadManager =
                        getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val uri: Uri = downloadManager.getUriForDownloadedFile(downloadedFileId)
                    copyFileAndExtract(uri, MimeTypeMap.getFileExtensionFromUrl(url))
                    showProgress()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    url?.let { startDownload(it) }
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE

    }

    private fun hideProgress() {
        binding.progressBar.visibility = View.GONE

    }

}
