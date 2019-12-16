package com.tencent.bkrepo.auth.service.bk

import com.tencent.bkrepo.auth.pojo.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.Permission
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "bk")
class BkPermissionServiceImpl @Autowired constructor(

) : PermissionService {
    override fun deletePermission(id: String) {
        throw ErrorCodeException(CommonMessageCode.OPERATION_UNSUPPORTED, "not supported")
    }

    override fun listPermission(resourceType: ResourceType?): List<Permission> {
        return listOf()
    }

    override fun createPermission(request: CreatePermissionRequest) {
        throw ErrorCodeException(CommonMessageCode.OPERATION_UNSUPPORTED, "not supported")
    }

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        // todo 对接权限中心
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkPermissionServiceImpl::class.java)
    }
}
