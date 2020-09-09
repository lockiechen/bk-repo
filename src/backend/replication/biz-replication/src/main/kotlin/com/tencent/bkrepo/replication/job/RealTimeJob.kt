package com.tencent.bkrepo.replication.job

import com.tencent.bkrepo.replication.handler.NodeEventConsumer
import com.tencent.bkrepo.replication.model.TOperateLog
import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.pojo.log.ResourceType
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer
import org.springframework.data.mongodb.core.messaging.MessageListener
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer
import org.springframework.data.mongodb.core.messaging.TailableCursorRequest
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * real time job tail from the caped collection
 */
@Service
class RealTimeJob {

    @Autowired
    private lateinit var template: MongoTemplate

    @Autowired
    private lateinit var nodeConsumer: NodeEventConsumer

    private lateinit var container: MessageListenerContainer


    @Scheduled(cron = "00 */30 * * * ?")
    fun ping(){
        if (!container.isRunning) {
            logger.error("container is not running")
        }
    }

    @Scheduled(initialDelay = 1000 * 40, fixedDelay = Long.MAX_VALUE)
    @SchedulerLock(name = "RealTimeJob", lockAtMostFor = "PT1H")
    fun run() {
        var isRunning = false
        while (true) {
            try {
                if (!isRunning) {
                    container = DefaultMessageListenerContainer(template)
                    val request = getTailCursorRequest()
                    container.register(request, TOperateLog::class.java)
                    container.start()
                    logger.info("try to start status :[${container.isRunning}]")
                }
            } catch (ignored: Exception) {
                logger.error("fail to register container [${ignored.message}]")
            } finally {
                logger.info("container running status :[${container.isRunning}]")
                // get container running status an sleep try
                isRunning = container.isRunning
                if (isRunning) return
                Thread.sleep(retryInterVal)
            }
        }
    }

    private fun getTailCursorRequest(): TailableCursorRequest<Any> {
        val query = Query.query(
            Criteria.where(TOperateLog::createdDate.name).gte(LocalDateTime.now()).and(TOperateLog::resourceType.name)
                .`is`(ResourceType.NODE)
        )

        val listener = MessageListener<Document, TOperateLog> {
            val body = it.body
            body?.let {
                when (body.operateType) {
                    OperateType.CREATE -> {
                        nodeConsumer.dealWithNodeCreateEvent(body.description)
                    }
                    OperateType.RENAME -> {
                        nodeConsumer.dealWithNodeRenameEvent(body.description)
                    }
                    OperateType.COPY -> {
                        nodeConsumer.dealWithNodeCopyEvent(body.description)
                    }
                    OperateType.MOVE -> {
                        nodeConsumer.dealWithNodeMoveEvent(body.description)
                    }
                    OperateType.DELETE -> {
                        nodeConsumer.dealWithNodeDeleteEvent(body.description)
                    }
                    OperateType.UPDATE -> {
                        nodeConsumer.dealWithNodeUpdateEvent(body.description)
                    }
                }
            }
        }
        return TailableCursorRequest.builder()
            .collection(collectionName)
            .filter(query)
            .publishTo(listener)
            .build()
    }

    fun getContainerStatus(): Boolean {
        return this.container.isRunning
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RealTimeJob::class.java)
        const val collectionName = "operation_log"
        const val retryInterVal = 30000L
    }
}