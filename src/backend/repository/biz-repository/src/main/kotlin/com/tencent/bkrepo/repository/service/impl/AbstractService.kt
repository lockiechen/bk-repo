package com.tencent.bkrepo.repository.service.impl

import com.tencent.bkrepo.auth.api.ServiceRoleResource
import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.security.http.HttpAuthProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * 服务抽象类，封装公共逻辑
 */
abstract class AbstractService {

    @Autowired
    open lateinit var roleResource: ServiceRoleResource

    @Autowired
    open lateinit var userResource: ServiceUserResource

    @Autowired
    open lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var authProperties: HttpAuthProperties

    fun publishEvent(any: Any) {
        eventPublisher.publishEvent(any)
    }

    fun createRepoManager(projectId: String, repoName: String, userId: String) {
        try {
            if (userId != ANONYMOUS_USER) {
                val repoManagerRoleId = roleResource.createRepoManage(projectId, repoName).data!!
                userResource.addUserRole(userId, repoManagerRoleId)
            }
        } catch (ignored: RuntimeException) {
            if (authProperties.enabled) {
                throw ignored
            } else {
                logger.warn("Create repository manager failed, ignore exception due to auth disabled[${ignored.message}].")
            }
        }
    }

    fun createProjectManager(projectId: String, userId: String) {
        try {
            if (userId != ANONYMOUS_USER) {
                val projectManagerRoleId = roleResource.createProjectManage(projectId).data!!
                userResource.addUserRole(userId, projectManagerRoleId)
            }
        } catch (ignored: RuntimeException) {
            if (authProperties.enabled) {
                throw ignored
            } else {
                logger.warn("Create project manager failed, ignore exception due to auth disabled[${ignored.message}].")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractService::class.java)
    }
}