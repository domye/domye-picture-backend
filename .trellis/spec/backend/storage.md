# Storage Guidelines

> File storage implementation using S3-compatible services (RustFS, MinIO, etc.)

---

## Overview

This project uses **S3-compatible object storage** for file uploads, replacing the previous Tencent Cloud COS implementation.

---

## Architecture

```
Controller → FileManager → S3Manager → S3Client (AWS SDK)
                ↓
          Thumbnailator (image processing)
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `S3ClientConfig` | `picture-service/.../upload/` | S3 client configuration |
| `S3Manager` | `picture-service/.../upload/` | S3 operations wrapper |
| `FileManager` | `picture-service/.../upload/` | Image upload with processing |
| `Thumbnailator` | Maven dependency | Image compression & thumbnails |

---

## Configuration

### application.yml

```yaml
s3:
  client:
    endpoint: https://your-s3-endpoint.com
    accessKey: your-access-key
    secretKey: your-secret-key
    region: us-east-1
    bucket: your-bucket-name
    host: https://your-s3-endpoint.com/your-bucket-name
    pathStyleAccess: true  # Required for MinIO/RustFS
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `S3_CLIENT_ENDPOINT` | S3 service endpoint URL |
| `S3_CLIENT_ACCESSKEY` | Access key ID |
| `S3_CLIENT_SECRETKEY` | Secret access key |
| `S3_CLIENT_BUCKET` | Bucket name |
| `S3_CLIENT_HOST` | Public access URL |

---

## Usage Patterns

### 1. Basic File Upload

```java
@Service
@RequiredArgsConstructor
public class MyService {
    final S3Manager s3Manager;

    public void uploadFile(String key, File file) {
        s3Manager.putObject(key, file);
    }
}
```

### 2. Image Upload with Processing

```java
@Service
@RequiredArgsConstructor
public class MyService {
    final FileManager fileManager;

    public UploadPictureResult uploadImage(MultipartFile file, String pathPrefix) {
        // Automatically handles:
        // - Image validation
        // - WebP compression (80% quality)
        // - Thumbnail generation (128x128)
        // - S3 upload
        return fileManager.uploadPicture(file, pathPrefix);
    }
}
```

### 3. File Deletion

```java
// Supports both URL and key
s3Manager.deleteObject("https://host/bucket/path/file.webp");
s3Manager.deleteObject("path/file.webp");
```

---

## Image Processing

### Compression

- **Format**: WebP
- **Quality**: 80%
- **Library**: Thumbnailator

### Thumbnail

- **Size**: 128x128 (aspect ratio preserved)
- **Format**: Same as original

### Color Extraction

- **Method**: Center region sampling (5x5 pixels)
- **Output**: Hex color string (e.g., `#FF5733`)

---

## Path Conventions

### Public Gallery

```
/public/{userId}/{date}_{uuid}.{ext}
/public/{userId}/{date}_{uuid}.webp          # compressed
/public/{userId}/{date}_{uuid}_thumbnail.{ext}  # thumbnail
```

### Space Gallery

```
/space/{spaceId}/{date}_{uuid}.{ext}
/space/{spaceId}/{date}_{uuid}.webp
/space/{spaceId}/{date}_{uuid}_thumbnail.{ext}
```

---

## Key Implementation Details

### Multipart Request Handling

**Issue**: `ContentCachingRequestWrapper` doesn't support multipart requests.

**Solution**: Skip wrapping for multipart requests in `ReqRecordFilter`:

```java
boolean isMultipart = isMultipartRequest(request);
if (!isMultipart) {
    request = new ContentCachingRequestWrapper(request);
}
```

### Controller Binding

For multipart requests with both file and DTO:

```java
@PostMapping(value = "/upload", consumes = "multipart/form-data")
public BaseResponse<PictureVO> upload(
    @RequestPart("file") MultipartFile file,
    PictureUploadRequest request) {
    // ...
}
```

### URL to Key Extraction

`S3Manager.deleteObject()` automatically extracts key from full URL:

```java
// Input: https://host/bucket/path/file.webp
// Key: path/file.webp
```

---

## Migration Notes

### From Tencent COS to S3

| COS Feature | S3 Replacement |
|-------------|----------------|
| Built-in image processing | Thumbnailator (application layer) |
| `CosManager` | `S3Manager` |
| `CosClientConfig` | `S3ClientConfig` |
| `cos_api` dependency | `aws-java-sdk-s3` |

### Dependencies

```xml
<!-- Removed -->
<dependency>
    <groupId>com.qcloud</groupId>
    <artifactId>cos_api</artifactId>
</dependency>

<!-- Added -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
</dependency>
```

---

## Common Issues

### 1. "Not a multipart request"

**Cause**: `ContentCachingRequestWrapper` wrapping multipart requests.

**Fix**: Check content-type before wrapping in filters.

### 2. Path Style Access

**Cause**: S3 uses virtual-host style by default.

**Fix**: Set `pathStyleAccess: true` for MinIO/RustFS compatibility.

### 3. Missing Thumbnails

**Cause**: Original format not supported by Thumbnailator.

**Supported formats**: JPEG, JPG, PNG, WebP