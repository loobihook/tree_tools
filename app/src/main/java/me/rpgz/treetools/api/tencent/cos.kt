package me.rpgz.treetools.api.tencent

import android.content.Context
import com.tencent.cos.xml.CosXmlService
import com.tencent.cos.xml.CosXmlServiceConfig
import com.tencent.cos.xml.model.PresignedUrlRequest
import com.tencent.cos.xml.model.`object`.PutObjectRequest
import com.tencent.cos.xml.transfer.TransferConfig
import com.tencent.cos.xml.transfer.TransferManager
import com.tencent.qcloud.core.auth.QCloudCredentialProvider
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


val secretId = ""
val secretKey = ""
const val bucketName = "tree-tools-1300275204"
val region = "ap-shanghai"

var myCredentialProvider: QCloudCredentialProvider =
    ShortTimeCredentialProvider(secretId, secretKey, 60 * 60 * 24)

// 创建 CosXmlServiceConfig 对象，根据需要修改默认的配置参数
var serviceConfig = CosXmlServiceConfig.Builder()
    .setRegion(region)
    .isHttps(true) // 使用 HTTPS 请求, 默认为 HTTP 请求
    .builder()

/**
 * upload a file and return the url of the file
 */
suspend fun uploadFile(srcPath: String, cosPath: String, context: Context): String {
    return withContext(Dispatchers.IO) {
        val cosXmlService = CosXmlService(
            context,
            serviceConfig, myCredentialProvider
        )
        val transferConfig = TransferConfig.Builder() // 设置启用分块上传的最小对象大小 默认为2M
            .setDivisionForUpload(2097152) // 设置分块上传时的分块大小 默认为1M
            .setSliceSizeForUpload(1048576)
            .setForceSimpleUpload(true) // 设置是否强制使用简单上传, 禁止分块上传
            .build()
        cosXmlService.putObject(PutObjectRequest(
            bucketName, cosPath, srcPath
        ))

        val presignedUrlRequest: PresignedUrlRequest =  PresignedUrlRequest(
            bucketName,
            cosPath
        )
        presignedUrlRequest.setSignKeyTime(60 * 60 * 24);
        cosXmlService.getPresignedURL(presignedUrlRequest)
    }
}
