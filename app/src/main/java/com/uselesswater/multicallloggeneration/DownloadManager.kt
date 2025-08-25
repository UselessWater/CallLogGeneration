package com.uselesswater.multicallloggeneration

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * APK下载管理器
 */
class AppDownloadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AppDownloadManager"
        private fun getAuthority(context: Context): String {
            return "${context.packageName}.fileprovider"
        }
    }
    
    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    
    private var downloadId: Long = -1
    
    /**
     * 下载APK文件
     */
    fun downloadApk(downloadUrl: String, fileName: String): Long {
        // 使用应用私有目录，避免权限问题
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "CallLogGeneration")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        
        // 删除旧文件（如果存在）
        val outputFile = File(downloadDir, fileName)
        if (outputFile.exists()) {
            outputFile.delete()
        }
        
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("通话记录生成工具更新")
            setDescription("正在下载新版本")
            // 设置为可见通知，非后台下载
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationUri(Uri.fromFile(outputFile))
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            // 允许扫描媒体文件
            setAllowedOverRoaming(true)
        }
        
        downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "开始下载APK，下载ID: $downloadId，保存路径: ${outputFile.absolutePath}")
        
        // 注册下载完成广播接收器
        registerDownloadReceiver()
        
        return downloadId
    }
    
    /**
     * 注册下载完成广播接收器
     */
    private fun registerDownloadReceiver() {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
    
    /**
     * 取消注册广播接收器
     */
    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(downloadReceiver)
        } catch (e: IllegalArgumentException) {
            // 接收器未注册，忽略
        }
    }
    
    /**
     * 下载完成广播接收器
     */
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Log.d(TAG, "下载完成，ID: $id")
                    installApk(context, id)
                }
            }
        }
    }
    
    /**
     * 安装APK
     */
    private fun installApk(context: Context, downloadId: Long) {
        try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        // 直接获取本地文件路径
                        val localUriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        val apkUri = Uri.parse(localUriString)
                        
                        Log.d(TAG, "下载完成，文件URI: $localUriString")
                        
                        // 使用FileProvider获取安全的URI
                        val apkFile = File(apkUri.path ?: "")
                        if (apkFile.exists()) {
                            val contentUri = FileProvider.getUriForFile(
                                context,
                                getAuthority(context),
                                apkFile
                            )
                            
                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(contentUri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                // 添加额外的标志以确保安装
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            
                            Log.d(TAG, "启动APK安装，文件路径: ${apkFile.absolutePath}")
                            context.startActivity(installIntent)
                        } else {
                            Log.e(TAG, "APK文件不存在: ${apkFile.absolutePath}")
                            // 如果文件不存在，尝试使用DownloadManager提供的URI
                            try {
                                val downloadUri = downloadManager.getUriForDownloadedFile(downloadId)
                                if (downloadUri != null) {
                                    val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(downloadUri, "application/vnd.android.package-archive")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(fallbackIntent)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "备用安装方案也失败", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "安装APK失败", e)
            // 显示安装失败的提示
            android.widget.Toast.makeText(context, "安装失败，请手动安装下载的文件", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 取消下载
     */
    fun cancelDownload() {
        if (downloadId != -1L) {
            downloadManager.remove(downloadId)
            downloadId = -1
            unregisterReceiver()
            Log.d(TAG, "取消下载")
        }
    }

    /**
     * 简单的前台下载方法（不使用DownloadManager）
     */
    fun downloadApkSimple(downloadUrl: String, fileName: String, onProgress: (progress: Int) -> Unit = {}, onComplete: (file: File?) -> Unit) {
        Thread {
            var connection: HttpURLConnection? = null
            var outputStream: FileOutputStream? = null
            var inputStream: java.io.InputStream? = null
            
            try {
                val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "CallLogGeneration")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                
                val outputFile = File(downloadDir, fileName)
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                
                val url = URL(downloadUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connect()
                
                val contentLength = connection.contentLength
                inputStream = connection.inputStream
                outputStream = FileOutputStream(outputFile)
                
                val buffer = ByteArray(4096) // 增大缓冲区以提高下载速度
                var totalBytesRead = 0
                var bytesRead: Int
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    // 更新进度
                    if (contentLength > 0) {
                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                        onProgress(progress)
                    }
                }
                
                outputStream.flush()
                
                // 下载完成，安装APK
                onComplete(outputFile)
                
            } catch (e: Exception) {
                Log.e(TAG, "前台下载失败", e)
                onComplete(null)
            } finally {
                // 确保资源正确关闭
                try {
                    outputStream?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "关闭输出流失败", e)
                }
                
                try {
                    inputStream?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "关闭输入流失败", e)
                }
                
                try {
                    connection?.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "断开连接失败", e)
                }
            }
        }.start()
    }

    /**
     * 安装APK文件（用于前台下载）
     */
    fun installApkFile(apkFile: File) {
        try {
            if (apkFile.exists()) {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    getAuthority(context),
                    apkFile
                )
                
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                
                Log.d(TAG, "启动APK安装，文件路径: ${apkFile.absolutePath}")
                context.startActivity(installIntent)
            } else {
                Log.e(TAG, "APK文件不存在: ${apkFile.absolutePath}")
                android.widget.Toast.makeText(context, "安装文件不存在", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "安装APK失败", e)
            android.widget.Toast.makeText(context, "安装失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 测试下载和安装功能
     */
    fun testDownloadAndInstall() {
        // 这里可以添加一个测试下载链接
        val testUrl = "https://example.com/test.apk" // 替换为实际的测试URL
        val fileName = "test_app.apk"
        
        downloadApkSimple(
            downloadUrl = testUrl,
            fileName = fileName,
            onProgress = { progress ->
                Log.d(TAG, "测试下载进度: $progress%")
            },
            onComplete = { file ->
                if (file != null) {
                    Log.d(TAG, "测试下载完成，开始安装")
                    installApkFile(file)
                } else {
                    Log.e(TAG, "测试下载失败")
                }
            }
        )
    }
    
    /**
     * 获取下载状态
     */
    fun getDownloadStatus(): DownloadStatus {
        if (downloadId == -1L) {
            return DownloadStatus.Idle
        }
        
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        
        return try {
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                
                when (status) {
                    DownloadManager.STATUS_RUNNING -> DownloadStatus.Downloading(bytesDownloaded, totalSize)
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Completed
                    DownloadManager.STATUS_FAILED -> DownloadStatus.Failed
                    else -> DownloadStatus.Idle
                }
            } else {
                DownloadStatus.Idle
            }
        } finally {
            cursor.close()
        }
    }
}

/**
 * 下载状态
 */
sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val bytesDownloaded: Long, val totalSize: Long) : DownloadStatus()
    object Completed : DownloadStatus()
    object Failed : DownloadStatus()
}