## 编译&运行方法

### 1.编译前准备

**安装MAVEN**

[Maven安装方法](https://www.cnblogs.com/eagle6688/p/7838224.html)

[Maven官网配置教程](http://maven.apache.org/guides/mini/guide-configuring-maven.html)

**下载代码**
```
git clone git@github.com:NaturalBase/NaturalBase.git
```

### 2. 编译
```
mvn compile
```

### 3. 本地运行调试
```
mvn spring-boot:run
```

### 4. 打包
```
mvn package
```
打包后会生成.jar文件，上传到服务器上之后运行`java -jar naturalbase-0.0.1-SNAPSHOT.jar`即可运行。
如果想修改端口号加上`--server.port=[port]`
