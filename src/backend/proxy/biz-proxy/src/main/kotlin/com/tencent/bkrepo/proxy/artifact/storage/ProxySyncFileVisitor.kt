/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.proxy.artifact.storage

import com.tencent.bkrepo.common.artifact.stream.rateLimit
import com.tencent.bkrepo.common.artifact.util.http.StreamRequestBody
import com.tencent.bkrepo.common.service.proxy.ProxyEnv
import com.tencent.bkrepo.common.service.proxy.ProxyRequestInterceptor
import com.tencent.bkrepo.common.service.util.okhttp.HttpClientBuilderFactory
import com.tencent.bkrepo.common.storage.filesystem.ArtifactFileVisitor
import com.tencent.bkrepo.common.storage.util.delete
import com.tencent.bkrepo.common.storage.util.existReal
import com.tencent.bkrepo.replication.constant.FILE
import com.tencent.bkrepo.replication.constant.SHA256
import com.tencent.bkrepo.replication.constant.SIZE
import com.tencent.bkrepo.replication.constant.STORAGE_KEY
import okhttp3.MultipartBody
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.temporal.ChronoUnit

class ProxySyncFileVisitor(
    private val rate: Long,
    private val cacheExpireDays: Int
) : ArtifactFileVisitor() {

    private val httpClient = HttpClientBuilderFactory.create().addInterceptor(ProxyRequestInterceptor()).build()

    override fun needWalk(): Boolean {
        return true
    }

    override fun visitFile(filePath: Path, attrs: BasicFileAttributes): FileVisitResult {
        try {
            if (filePath.toString().endsWith(".sync")) {
                val file = File(filePath.toString().removeSuffix(".sync"))
                syncFile(filePath, file)
            } else {
                deleteCacheFile(filePath)
            }
        } catch (e: Exception) {
            logger.error("sync file error: ", e)
        }
        return FileVisitResult.CONTINUE
    }

    private fun syncFile(filePath: Path, file: File) {
        val syncFile = filePath.toFile()
        val storageKey = if (syncFile.length() == 0L) null else syncFile.readText()

        val sha256 = file.canonicalPath.split(File.separator).last()
//        val url = "$gateway:25903/proxy/replica/blob/push"
        val url = "$gateway/replication/proxy/replica/blob/push"
        logger.info("start sync file[${file.canonicalPath}]")
        val inputStream = if (rate > 0) {
            file.inputStream().rateLimit(rate)
        } else {
            file.inputStream()
        }
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(FILE, sha256, StreamRequestBody(inputStream, file.length()))
            .addFormDataPart(SIZE, file.length().toString())
            .addFormDataPart(SHA256, sha256)
            .apply { storageKey?.let { addFormDataPart(STORAGE_KEY, storageKey) } }
            .build()
        val request = Request.Builder().url(url).post(requestBody).build()
        httpClient.newCall(request).execute().use {
            if (it.isSuccessful) {
                logger.info("sync file[${file.canonicalPath}] success")
                filePath.delete()
            } else {
                logger.error("sync file[${file.canonicalPath}] error: ${it.code}, ${it.body?.string()}")
            }
        }
    }

    private fun deleteCacheFile(filePath: Path) {
        if (filePath.toFile().isDirectory) {
            return
        }
        val syncFilePath = Paths.get(filePath.toString().plus(".sync"))
        // 还未同步到服务端
        if (syncFilePath.existReal()) {
            return
        }
        val view = Files.getFileAttributeView(filePath, BasicFileAttributeView::class.java)
        val attributes = view.readAttributes()
        val aTime = attributes.lastAccessTime().toInstant()
        val expireTime = Instant.now().minus(cacheExpireDays.toLong(), ChronoUnit.DAYS)
        if (aTime.isBefore(expireTime)) {
            filePath.toFile().deleteOnExit()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProxySyncFileVisitor::class.java)
        private val gateway = ProxyEnv.getGateway()
    }
}
