/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.auth.util

import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_ID
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_NAME
import com.tencent.bkrepo.auth.constant.PROJECT_VIEWER_ID
import com.tencent.bkrepo.auth.constant.PROJECT_VIEWER_NAME
import com.tencent.bkrepo.auth.constant.REPO_MANAGE_ID
import com.tencent.bkrepo.auth.constant.REPO_MANAGE_NAME
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.role.CreateRoleRequest

object RequestUtil {

    /**
     * 构造创建项目管理员请求
     */
    fun buildProjectAdminRequest(projectId: String): CreateRoleRequest {
        return CreateRoleRequest(
            roleId = PROJECT_MANAGE_ID,
            name = PROJECT_MANAGE_NAME,
            type = RoleType.PROJECT,
            projectId = projectId,
            admin = true
        )
    }

    /**
     * 构造创建项目用户请求
     */
    fun buildProjectViewerRequest(projectId: String): CreateRoleRequest {
        return CreateRoleRequest(
            roleId = PROJECT_VIEWER_ID,
            name = PROJECT_VIEWER_NAME,
            type = RoleType.PROJECT,
            projectId = projectId,
            admin = false
        )
    }

    /**
     * 构造仓库管理员请求
     */
    fun buildRepoAdminRequest(projectId: String, repoName: String): CreateRoleRequest {
        return CreateRoleRequest(
            roleId = REPO_MANAGE_ID,
            name = REPO_MANAGE_NAME,
            type = RoleType.REPO,
            projectId = projectId,
            repoName = repoName,
            admin = true
        )
    }
}
