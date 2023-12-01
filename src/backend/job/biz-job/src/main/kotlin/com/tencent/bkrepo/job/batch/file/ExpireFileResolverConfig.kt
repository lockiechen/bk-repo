package com.tencent.bkrepo.job.batch.file

import com.tencent.bkrepo.common.storage.filesystem.cleanup.FileExpireResolver
import com.tencent.bkrepo.job.config.properties.ExpiredCacheFileCleanupJobProperties
import com.tencent.bkrepo.repository.api.NodeClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
class ExpireFileResolverConfig {
    @Bean
    fun fileExpireResolver(
        expiredCacheFileCleanupJobProperties: ExpiredCacheFileCleanupJobProperties,
        nodeClient: NodeClient,
        scheduler: ThreadPoolTaskScheduler,
    ): FileExpireResolver {
        return BasedRepositoryFileExpireResolver(
            nodeClient,
            expiredCacheFileCleanupJobProperties.repoConfig,
            scheduler,
        )
    }
}
