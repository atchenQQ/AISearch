package com.atchen.AISearch.utils;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-10
 * @Description: minio使用工具
 * @Version: 1.0
 */

@Component
public class MinioUtil {
        @Resource
        private MinioClient minioClient;
        @Value("${minio.bucket-name}")
        private String bucketName;
        @Value("${minio.endpoint}")
        private String miniourl;


        public  String upload(String fileName, InputStream inputStream,String contentType) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
           boolean isExist= minioClient.bucketExists(
                   BucketExistsArgs.builder().bucket(bucketName).build()
           );
           if (!isExist){
               minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
           }
           //上传文件到minio
               minioClient.putObject(PutObjectArgs.builder()
                       .bucket(bucketName)  //桶名
                       .stream(inputStream,-1,10485760)  //文件流
                       .object(fileName)   //文件名
                       .contentType(contentType)  //文件类型
                       .build());
           //获取文件下载地址  方法为预签名URL
//          return  minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
//                   .bucket(bucketName)
//                   .object(fileName)
//                   .method(Method.GET)
//                   .build()
//           );
            return miniourl + "/" + bucketName + "/" + fileName;
        }
}
    