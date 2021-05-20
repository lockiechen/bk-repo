/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.replication.model

import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeName
import com.tencent.bkrepo.replication.pojo.request.ReplicaType
import com.tencent.bkrepo.replication.pojo.task.ReplicationStatus
import com.tencent.bkrepo.replication.pojo.task.setting.ReplicaSetting
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 同步对象
 */
@Document("replica_object")
data class TReplicaObject(
    var id: String? = null,
    /**
     * 任务唯一key
     */
    @Indexed(unique = true)
    val key: String,
    /**
     * 项目id
     */
    val projectId: String,
    /**
     * 同步类型
     */
    val replicaType: ReplicaType,
    /**
     * 任务设置
     */
    val setting: ReplicaSetting,
    /**
     * 远程集群集合
     */
    val remoteClusterSet: Set<ClusterNodeName>,
    /**
     * 任务描述
     */
    val description: String? = null,
    /**
     * 当前状态
     */
    var status: ReplicationStatus = ReplicationStatus.WAITING,
    /**
     * 下次执行时间
     */
    var nextExecutionTime: LocalDateTime? = null,
    /**
     * 执行次数
     */
    var executionTimes: Long,
    /**
     * 是否启用
     */
    var enabled: Boolean = true,
    /**
     * 审计信息
     */
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime
)
