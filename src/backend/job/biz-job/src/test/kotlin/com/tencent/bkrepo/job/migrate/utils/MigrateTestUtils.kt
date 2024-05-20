/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.job.migrate.utils

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.mongo.dao.util.sharding.HashShardingUtils
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.job.SHARDING_COUNT
import com.tencent.bkrepo.job.UT_MD5
import com.tencent.bkrepo.job.UT_PROJECT_ID
import com.tencent.bkrepo.job.UT_REPO_NAME
import com.tencent.bkrepo.job.UT_SHA256
import com.tencent.bkrepo.job.UT_STORAGE_CREDENTIALS_KEY
import com.tencent.bkrepo.job.UT_USER
import com.tencent.bkrepo.job.migrate.dao.MigrateFailedNodeDao
import com.tencent.bkrepo.job.migrate.model.TMigrateFailedNode
import com.tencent.bkrepo.job.migrate.pojo.MigrateRepoStorageTask
import com.tencent.bkrepo.job.migrate.pojo.MigrateRepoStorageTaskState
import com.tencent.bkrepo.job.model.TNode
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.time.LocalDateTime

object MigrateTestUtils {
    fun buildTask(now: LocalDateTime = LocalDateTime.now(), startDate: LocalDateTime? = now): MigrateRepoStorageTask {
        return MigrateRepoStorageTask(
            id = "",
            createdBy = UT_USER,
            createdDate = now,
            lastModifiedBy = UT_USER,
            lastModifiedDate = now,
            startDate = startDate,
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            srcStorageKey = "$UT_STORAGE_CREDENTIALS_KEY-src",
            dstStorageKey = "$UT_STORAGE_CREDENTIALS_KEY-dst",
            state = MigrateRepoStorageTaskState.MIGRATING.name,
        )
    }

    fun buildRepo(
        projectId: String = UT_PROJECT_ID,
        repoName: String = UT_REPO_NAME,
        oldCredentialsKey: String? = null,
        storageCredentials: StorageCredentials? = null,
    ) = RepositoryDetail(
        projectId = projectId,
        name = repoName,
        type = RepositoryType.GENERIC,
        category = RepositoryCategory.LOCAL,
        public = false,
        description = "",
        configuration = LocalConfiguration(),
        createdBy = "",
        createdDate = "",
        lastModifiedBy = "",
        lastModifiedDate = "",
        quota = null,
        used = null,
        oldCredentialsKey = oldCredentialsKey,
        storageCredentials = storageCredentials,
    )

    fun MigrateFailedNodeDao.insertFailedNode(fullPath: String = "/a/b/c.txt"): TMigrateFailedNode {
        val now = LocalDateTime.now()
        return insert(
            TMigrateFailedNode(
                id = null,
                createdDate = now,
                lastModifiedDate = now,
                nodeId = "",
                taskId = "",
                projectId = UT_PROJECT_ID,
                repoName = UT_REPO_NAME,
                fullPath = fullPath,
                sha256 = UT_SHA256,
                size = 1000L,
                md5 = UT_MD5,
                retryTimes = 0,
            )
        )
    }

    fun MongoTemplate.createNode(
        repoName: String = UT_REPO_NAME,
        createDate: LocalDateTime = LocalDateTime.now(),
        sha256: String = UT_SHA256,
        fullPath: String = "/a/b/c.txt",
        archived: Boolean = false,
        compressed: Boolean = false,
    ): TNode {
        val node = TNode(
            id = null,
            projectId = UT_PROJECT_ID,
            repoName = repoName,
            fullPath = fullPath,
            size = 100L,
            sha256 = sha256,
            md5 = UT_MD5,
            createdDate = createDate,
            folder = false,
            archived = archived,
            compressed = compressed,
        )
        val sharding = HashShardingUtils.shardingSequenceFor(UT_PROJECT_ID, SHARDING_COUNT)
        return insert(node, "node_$sharding")
    }

    fun MongoTemplate.removeNodes() {
        val sequence = HashShardingUtils.shardingSequenceFor(UT_PROJECT_ID, SHARDING_COUNT)
        remove(Query(), "node_$sequence")
    }
}
