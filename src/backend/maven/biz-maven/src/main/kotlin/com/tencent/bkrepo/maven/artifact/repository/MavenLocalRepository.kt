package com.tencent.bkrepo.maven.artifact.repository

import com.tencent.bkrepo.common.api.util.readXmlString
import com.tencent.bkrepo.common.api.util.toXmlString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.hash.md5
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.maven.pojo.Basic
import com.tencent.bkrepo.maven.pojo.MavenArtifactVersionData
import com.tencent.bkrepo.maven.pojo.MavenMetadata
import com.tencent.bkrepo.maven.pojo.MavenPom
import com.tencent.bkrepo.maven.util.MavenGAVCUtils.GAVC
import com.tencent.bkrepo.maven.util.StringUtils.formatSeparator
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.StageClient
import com.tencent.bkrepo.repository.pojo.download.service.DownloadStatisticsAddRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

@Component
class MavenLocalRepository : LocalRepository() {

    @Autowired
    lateinit var packageClient: PackageClient

    @Autowired
    lateinit var stageClient: StageClient

    /**
     * 获取MAVEN节点创建请求
     */
    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        val request = super.buildNodeCreateRequest(context)
        return request.copy(overwrite = true)
    }

    override fun onUpload(context: ArtifactUploadContext) {
        with(context.artifactInfo) {
            // 改为解析pom文件数据
            if (getArtifactFullPath().matches(Regex("(.)+-(.)+\\.pom"))) {
                val mavenPom = context.getArtifactFile().getInputStream().readXmlString<MavenPom>()
                // 打包方式为pom时，下载地址为pom文件地址，否则改为jar包地址。
                val artifactFullPath = if (StringUtils.isNotBlank(mavenPom.version) && mavenPom.packaging == "pom") {
                    getArtifactFullPath()
                } else {
                    StringBuilder(getArtifactFullPath().removeSuffix("pom")).append("jar").toString()
                }
                packageClient.createVersion(
                    PackageVersionCreateRequest(
                        projectId,
                        repoName,
                        packageName = mavenPom.artifactId,
                        packageKey = PackageKeys.ofGav(mavenPom.groupId, mavenPom.artifactId),
                        packageType = PackageType.MAVEN,
                        versionName = mavenPom.version,
                        size = context.getArtifactFile().getSize(),
                        artifactPath = artifactFullPath,
                        overwrite = true,
                        createdBy = context.userId
                    )
                )
            }
        }
        super.onUpload(context)
    }

    fun metadataNodeCreateRequest(
        context: ArtifactUploadContext,
        fullPath: String,
        metadataArtifact: ArtifactFile
    ): NodeCreateRequest {
        val request = super.buildNodeCreateRequest(context)
        return request.copy(
            overwrite = true,
            fullPath = fullPath
        )
    }

    fun updateMetadata(fullPath: String, metadataArtifact: ArtifactFile) {
        val uploadContext = ArtifactUploadContext(metadataArtifact)
        val metadataNode = metadataNodeCreateRequest(uploadContext, fullPath, metadataArtifact)
        storageManager.storeArtifactFile(metadataNode, metadataArtifact, uploadContext.storageCredentials)
        logger.info("Success to save $fullPath, size: ${metadataArtifact.getSize()}")
    }

    override fun remove(context: ArtifactRemoveContext) {
        val packageKey = context.request.getParameter("packageKey")
        val version = context.request.getParameter("version")
        val artifactId = packageKey.split(":").last()
        val groupId = packageKey.removePrefix("gav://").split(":")[0]
        with(context.artifactInfo) {
            if (version.isNullOrBlank()) {
                packageClient.deletePackage(
                    projectId,
                    repoName,
                    packageKey
                )
            } else {
                packageClient.deleteVersion(
                    projectId,
                    repoName,
                    packageKey,
                    version
                )
            }
            logger.info("Success to delete $packageKey:$version")
        }
        updateMetadataXml(context, groupId, artifactId, version)
    }

    /**
     * 删除jar包时 对包一级目录下maven-metadata.xml 更新
     */
    fun updateMetadataXml(context: ArtifactRemoveContext, groupId: String, artifactId: String, version: String?) {
        val packageKey = context.request.getParameter("packageKey")
        val artifactPath = StringUtils.join(groupId.split("."), "/") + "/$artifactId"
        if (version.isNullOrBlank()) {
            nodeClient.delete(
                NodeDeleteRequest(
                    context.projectId,
                    context.repoName,
                    artifactPath,
                    ArtifactRemoveContext().userId
                )
            )
            return
        }
        // 加载xml
        with(context.artifactInfo) {
            val nodeList = nodeClient.list(projectId, repoName, "/$artifactPath").data ?: return
            val mavenMetadataNode = nodeList.filter { it.name == "maven-metadata.xml" }[0]
            val artifactInputStream = storageService.load(
                mavenMetadataNode.sha256!!,
                Range.full(mavenMetadataNode.size),
                ArtifactRemoveContext().storageCredentials
            ) ?: return
            val xmlStr = String(artifactInputStream.readBytes()).removePrefix("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            val mavenMetadata = xmlStr.readXmlString<MavenMetadata>()
            mavenMetadata.versioning.versions.version.removeIf { it == version }
            // 当删除当前版本后不存在任一版本则删除整个包。
            if (mavenMetadata.versioning.versions.version.size == 0) {
                nodeClient.delete(
                    NodeDeleteRequest(
                        projectId,
                        repoName,
                        artifactPath,
                        ArtifactRemoveContext().userId
                    )
                )
                packageClient.deletePackage(
                    projectId,
                    repoName,
                    packageKey
                )
                return
            } else {
                nodeClient.delete(
                    NodeDeleteRequest(
                        projectId,
                        repoName,
                        "$artifactPath/$version",
                        ArtifactRemoveContext().userId
                    )
                )
                mavenMetadata.versioning.release = mavenMetadata.versioning.versions.version.last()
                val resultXml = mavenMetadata.toXmlString()
                val resultXmlMd5 = resultXml.md5()
                val resultXmlSha1 = resultXml.sha1()

                val metadataArtifact = ByteArrayInputStream(resultXml.toByteArray()).use {
                    ArtifactFileFactory.build(it)
                }
                val metadataArtifactMd5 = ByteArrayInputStream(resultXmlMd5.toByteArray()).use {
                    ArtifactFileFactory.build(it)
                }
                val metadataArtifactSha1 = ByteArrayInputStream(resultXmlSha1.toByteArray()).use {
                    ArtifactFileFactory.build(it)
                }

                logger.warn("${metadataArtifact.getSize()}")
                updateMetadata("$artifactPath/maven-metadata.xml", metadataArtifact)
                metadataArtifact.delete()
                updateMetadata("$artifactPath/maven-metadata.xml.md5", metadataArtifactMd5)
                metadataArtifactMd5.delete()
                updateMetadata("$artifactPath/maven-metadata.xml.sha1", metadataArtifactSha1)
                metadataArtifactSha1.delete()
            }
        }
    }

    override fun query(context: ArtifactQueryContext): MavenArtifactVersionData? {
        val packageKey = context.request.getParameter("packageKey")
        val version = context.request.getParameter("version")
        val artifactId = packageKey.split(":").last()
        val groupId = packageKey.removePrefix("gav://").split(":")[0]
        val trueVersion = packageClient.findVersionByName(
            context.projectId,
            context.repoName,
            packageKey,
            version
        ).data ?: return null
        with(context.artifactInfo) {
            val jarNode = nodeClient.detail(
                projectId, repoName, trueVersion.contentPath!!
            ).data ?: return null
            val stageTag = stageClient.query(projectId, repoName, packageKey, version).data
            val mavenArtifactMetadata = jarNode.metadata
            val countData = packageDownloadStatisticsClient.query(
                projectId, repoName, jarNode.fullPath,
                null, null, null
            ).data
            val count = countData?.count ?: 0
            val mavenArtifactBasic = Basic(
                groupId,
                artifactId,
                version,
                jarNode.size, jarNode.fullPath, jarNode.lastModifiedBy, jarNode.lastModifiedDate,
                count,
                jarNode.sha256,
                jarNode.md5,
                stageTag,
                null
            )
            return MavenArtifactVersionData(mavenArtifactBasic, mavenArtifactMetadata)
        }
    }

    // maven 客户端下载统计
    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): DownloadStatisticsAddRequest? {
        with(context) {
            val fullPath = context.artifactInfo.getArtifactFullPath()
            val mavenGAVC = fullPath.GAVC()
            val version = mavenGAVC.version
            val artifactId = mavenGAVC.artifactId
            val groupId = mavenGAVC.groupId.formatSeparator("/", ".")
            val packageKey = PackageKeys.ofGav(groupId, artifactId)
            return DownloadStatisticsAddRequest(projectId, repoName, packageKey, artifactId, version)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenLocalRepository::class.java)
    }
}
