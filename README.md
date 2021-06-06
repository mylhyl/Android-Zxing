### JCenter迁移说明
 `JCenter`服务已经宣布停止维护，对于`旧版本`大家可以使用阿里云镜像引用，以后`新版本`将会发布在`JitPack`
 #### 1、旧版本阿里云镜像引用方式
 在项目根目录下的`build.gradle`文件中加入
```
 allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/jcenter' }
    }
 }
```
 #### 1、新版本JitPack引用方式
 在项目根目录下的`build.gradle`文件中加入
```
 allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
 }
```
 
### 一、功能
 - 支持全屏或局部扫描
 - 定制扫描框颜色
 - 扫描线样式
 - 扫描提示语
 - 创建二维码
 - 识别图片中二维码
  
### 二、效果图
<img src="preview/gif.gif" width="240px"/>

### 三、[下载APK体验](https://www.90pan.com/b2339598)或手机扫描下面二维码

<img src="preview/qrdown.png"/>

### 四、引用

## 1、在线 本库内部已集成`zxing:core:3.4.0`
- 依赖使用 Gradle 构建时添加一下即可

```javascript
compile 'com.mylhyl:zxingscanner:2.2.0'
```

## 2、离线jar，需要手动添加 Zxing 核心库
- [jar包](https://github.com/mylhyl/Android-Zxing/tree/master/preview/lib)
- 也可自己打包jar文件打开终端，切换置项目根据目录，执行命令：gradlew makeJar
- [最新Zxing核心库点击查看](http://jcenter.bintray.com/com/google/zxing/core/)取出aar
- 也可使用Gradle构建时如下:

```javascript
compile 'com.google.zxing:core:3.4.0'
```
 
[gradle makeJar](http://blog.csdn.net/hupei/article/details/51886221) 或者参考下图，在android studio中执行

![这里写图片描述](http://img.blog.csdn.net/20160711135615587)

- [直接下载jar](preview)

### 五、使用

[例子](https://github.com/mylhyl/Android-Zxing/blob/master/sample/src/main/java/com/mylhyl/zxing/scanner/sample/OptionsScannerActivity.java)  

直接在`layout xml`使用`ScannerView`即可

```xml
<com.mylhyl.zxing.scanner.ScannerView
    android:id="@+id/scanner_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

###### 注意生命周期中的使用
重写`onResume`调用`mScannerView.onResume();`

```java
@Override
protected void onResume() {
    mScannerView.onResume();
    super.onResume();
}

@Override
protected void onPause() {
    mScannerView.onPause();
    super.onPause();
}
```

注册扫描成功监听器`mScannerView.setOnScannerCompletionListener`

```java
/**
 * 扫描成功后将调用
 *
 * @param rawResult    扫描结果
 * @param parsedResult 结果类型
 * @param barcode      扫描后的图像
 */
void OnScannerCompletion(Result rawResult, ParsedResult parsedResult, Bitmap barcode);
```

开启闪光灯
```java
mScannerView.toggleLight(true);//开
mScannerView.toggleLight(false);//关

```

调用如下方法获取类型

```java
ParsedResultType type = parsedResult.getType();
```

可根据`type`强转为相应的对象，按项目需求处理。每个项目都有不同的需求，所以此库将最终处理结果丢给你们自己咯，想怎么玩就怎么玩，下面代码是在 sample 中

```java
switch (type) {
    case ADDRESSBOOK:
	AddressBookParsedResult addressBook = (AddressBookParsedResult) parsedResult;
        bundle.putSerializable(Intents.Scan.RESULT, new AddressBookResult(addressBook));
        break;
    case URI:
        URIParsedResult uriParsedResult = (URIParsedResult) parsedResult;
        bundle.putString(Intents.Scan.RESULT, uriParsedResult.getURI());
        break;
    case TEXT:
        bundle.putString(Intents.Scan.RESULT, rawResult.getText());
        break;
}
```

生成二维码使用

```java
//联系人类型
Bitmap bitmap = new QREncode.Builder(this)
        .setParsedResultType(ParsedResultType.ADDRESSBOOK)
        .setAddressBookUri(contactUri).build().encodeAsBitmap();

//文本类型
Bitmap bitmap = new QREncode.Builder(this)
        .setColor(getResources().getColor(R.color.colorPrimary))//二维码颜色
        //.setParsedResultType(ParsedResultType.TEXT)//默认是TEXT类型
        .setContents("我是石头")//二维码内容
        .setLogoBitmap(logoBitmap)//二维码中间logo
        .build().encodeAsBitmap();

```
解析图中二维码

```java
    public static void decodeQR(String picturePath, OnScannerCompletionListener listener);
    public static void decodeQR(Bitmap srcBitmap, final OnScannerCompletionListener listener)
```

### 六、样式设置
说明：`1.6.0`以后版本将废弃`ScannerView`样式设置，使用新增`ScannerOptions`，后续版本只会在`ScannerOptions`中维护。  
具体api请看[ScannerOptions.Builder](https://github.com/mylhyl/Android-Zxing/blob/master/zxingscanner/src/main/java/com/mylhyl/zxing/scanner/ScannerOptions.java)

### 七、注意事项
权限
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

### 八、常见问题
* 对于`setLaserFrameTopMargin`方法，扫描区域偏移的问题[issues-13](https://github.com/mylhyl/Android-Zxing/issues/13)  
* 可以在扫描成功后，调用`restartPreviewAfterDelay`连续扫描  
* 对于加密后的二维码，判断二维码类型可以如下：
```java
	//重新包装`Result`，`decryptText`为解密后的内容
        Result decryptResult = new Result(decryptText, rawResult.getRawBytes(),
                rawResult.getNumBits(), rawResult.getResultPoints(), 
                rawResult.getBarcodeFormat(), rawResult.getTimestamp());
	//转换扫描结果为类型枚举
        ParsedResult decryptParsedResult = Scanner.parseResult(decryptResult);
        final ParsedResultType decryptType = decryptParsedResult.getType();
        switch (decryptType) {
            //类型分支
	    case :
	    	break;
        }
```

### 联系方式
 * 可能会因为工作忙碌原因没有及时回复，大家方便的话可以加我个人微信号：48025211，备注：github
