package com.uselesswater.multicallloggeneration

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * 应用更新检查器
 * 使用Gitee API检查更新
 */
class UpdateChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateChecker"
        
        // Gitee API地址
        private const val GITEE_API_BASE = "https://gitee.com/api/v5/repos"
        private const val REPO_OWNER = "uselesswater"
        private const val REPO_NAME = "CallLogGeneration"
        
        // 更新配置
        var includePreReleases = mutableStateOf(false)
        var updateToLatest = mutableStateOf(true) // true: 更新到最新版, false: 只更新到新版本
    }
    
    private val httpClient = OkHttpClient()
    
    /**
     * 获取当前应用版本信息
     */
    private fun getCurrentVersion(): VersionInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return VersionInfo(
            versionName = packageInfo.versionName ?: "",
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            },
            isPreRelease = (packageInfo.versionName ?: "").contains("-", ignoreCase = true)
        )
    }
    
    /**
     * 检查更新
     */
    fun checkForUpdate(callback: (UpdateResult) -> Unit) {
        val currentVersion = getCurrentVersion()
        
        try {
            val releases = fetchReleases()
            val latestRelease = findSuitableRelease(releases, currentVersion)
            
            if (latestRelease != null) {
                // 如果用户选择"更新到最新版"，直接返回最新版本
                // 如果用户选择"只更新到新版本"，需要检查版本是否更新
                val shouldUpdate = if (updateToLatest.value) {
                    true  // 总是更新到最新版
                } else {
                    isNewerVersion(latestRelease, currentVersion)  // 只更新到新版本
                }
                
                if (shouldUpdate) {
                    callback(UpdateResult.UpdateAvailable(latestRelease))
                } else {
                    callback(UpdateResult.NoUpdateAvailable)
                }
            } else {
                callback(UpdateResult.NoUpdateAvailable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查更新失败", e)
            callback(UpdateResult.Error(e.message ?: "未知错误"))
        }
    }
    
    /**
     * 从Gitee获取发行版列表
     */
    private fun fetchReleases(): List<ReleaseInfo> {
        val url = if (includePreReleases.value) {
            "$GITEE_API_BASE/$REPO_OWNER/$REPO_NAME/releases" // 所有发行版（包含pre-release）
        } else {
            "$GITEE_API_BASE/$REPO_OWNER/$REPO_NAME/releases/latest" // 仅最新正式版
        }
        
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP错误: ${response.code}")
            }
            
            val responseBody = response.body?.string() ?: ""
            return parseReleases(responseBody)
        }
    }
    
    /**
     * 解析Gitee API响应
     */
    private fun parseReleases(json: String): List<ReleaseInfo> {
        val releases = mutableListOf<ReleaseInfo>()
        
        try {
            if (json.trim().startsWith('[')) {
                // 数组格式（多个发行版）
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val releaseJson = jsonArray.getJSONObject(i)
                    releases.add(parseReleaseJson(releaseJson))
                }
            } else {
                // 对象格式（单个最新发行版）
                val releaseJson = JSONObject(json)
                releases.add(parseReleaseJson(releaseJson))
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析发行版数据失败", e)
        }
        
        return releases
    }
    
    /**
     * 解析单个发行版信息
     */
    private fun parseReleaseJson(json: JSONObject): ReleaseInfo {
        return ReleaseInfo(
            tagName = json.optString("tag_name", ""),
            name = json.optString("name", ""),
            body = json.optString("body", ""),
            prerelease = json.optBoolean("prerelease", false),
            publishedAt = json.optString("published_at", ""),
            assets = parseAssets(json.optJSONArray("assets"))
        )
    }
    
    /**
     * 解析资产文件（APK文件）
     */
    private fun parseAssets(assetsArray: JSONArray?): List<AssetInfo> {
        val assets = mutableListOf<AssetInfo>()
        
        assetsArray?.let {
            for (i in 0 until it.length()) {
                val assetJson = it.getJSONObject(i)
                val originalName = assetJson.optString("name", "")
                
                // 检查是否是APK文件（包括可能被重命名为zip的情况）
                if (originalName.endsWith(".apk") || 
                    (originalName.endsWith(".zip") && originalName.contains("apk", ignoreCase = true)) ||
                    assetJson.optString("content_type", "").contains("apk", ignoreCase = true)) {
                    
                    // 确保文件名以.apk结尾
                    val correctedName = if (originalName.endsWith(".apk")) {
                        originalName
                    } else {
                        // 如果是zip或其他格式，但实际上是APK文件，修正扩展名
                        val baseName = originalName.substringBeforeLast('.')
                        "$baseName.apk"
                    }
                    
                    assets.add(
                        AssetInfo(
                            name = correctedName,
                            downloadUrl = assetJson.optString("browser_download_url", ""),
                            size = assetJson.optLong("size", 0)
                        )
                    )
                }
            }
        }
        
        return assets
    }
    
    /**
     * 查找合适的发行版
     * 如果用户选择更新到最新版，则返回版本号最新的可用版本
     */
    private fun findSuitableRelease(releases: List<ReleaseInfo>, currentVersion: VersionInfo): ReleaseInfo? {
        // 过滤出符合条件的发行版（根据pre-release设置）
        val filteredReleases = releases.filter { release ->
            includePreReleases.value || !release.prerelease
        }
        
        // 按版本号排序，获取版本号最新的发行版
        return filteredReleases.maxByOrNull { release ->
            // 使用版本号比较而不是发布时间
            val versionNumber = release.tagName.removePrefix("v")
            parseVersionNumber(versionNumber)
        }
    }
    
    /**
     * 解析版本号为可比较的数字
     */
    internal fun parseVersionNumber(version: String): Long {
        try {
            // 移除前缀v并分割版本号
            val cleanVersion = version.removePrefix("v")
            val parts = cleanVersion.split(".", "-", "_").filter { it.isNotEmpty() }
            
            var result = 0L
            // 格式: major * 1000000 + minor * 10000 + patch * 100 + preRelease
            for (i in parts.indices.take(4)) {
                val part = parts[i]
                // 尝试解析为数字，如果失败则转换为数字代码
                val numericValue = if (part.all { it.isDigit() }) {
                    part.toIntOrNull() ?: 0
                } else {
                    // 对于非数字部分（如beta, alpha），转换为数字
                    when {
                        part.startsWith("alpha", ignoreCase = true) -> -3
                        part.startsWith("beta", ignoreCase = true) -> -2
                        part.startsWith("rc", ignoreCase = true) -> -1
                        else -> {
                            // 将字符串转换为数字代码
                            part.fold(0) { acc, char -> acc * 31 + char.code }
                        }
                    }
                }
                
                when (i) {
                    0 -> result += numericValue.toLong() * 1_000_000  // 主版本
                    1 -> result += numericValue.toLong() * 10_000     // 次版本
                    2 -> result += numericValue.toLong() * 100        // 修订版本
                    3 -> result += numericValue.toLong()              // 预发布版本
                }
            }
            
            return result
        } catch (e: Exception) {
            Log.w(TAG, "版本号解析失败: $version, 返回默认值0", e)
            return 0L
        }
    }
    
    /**
     * 检查版本是否更新
     */
    private fun isNewerVersion(release: ReleaseInfo, currentVersion: VersionInfo): Boolean {
        return try {
            // 只有当发布版本比当前版本新时才返回true
            val releaseVersion = release.tagName.removePrefix("v")
            val currentVersionName = currentVersion.versionName
            
            compareVersions(releaseVersion, currentVersionName) > 0
        } catch (e: Exception) {
            Log.w(TAG, "版本比较失败，假设需要更新", e)
            // 出错时默认需要更新
            true
        }
    }
    
    /**
     * 比较版本号
     */
    internal fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split(".", "-").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".", "-").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val part1 = parts1.getOrElse(i) { 0 }
            val part2 = parts2.getOrElse(i) { 0 }
            
            val comparison = part1.compareTo(part2)
            if (comparison != 0) {
                return comparison
            }
        }
        
        return 0
    }
}

/**
 * 版本信息
 */
data class VersionInfo(
    val versionName: String,
    val versionCode: Int,
    val isPreRelease: Boolean
)

/**
 * 发行版信息
 */
data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val prerelease: Boolean,
    val publishedAt: String,
    val assets: List<AssetInfo>
)

/**
 * 资产文件信息
 */
data class AssetInfo(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

/**
 * 更新检查结果
 */
sealed class UpdateResult {
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateResult()
    object NoUpdateAvailable : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}