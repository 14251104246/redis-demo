# 项目介绍

- ci(持续构建测试项目)，数据源使用h2

## 概述

- 以[测试项目](https://github.com/14251104246/spring-boot-ci-demo)为例：https://github.com/14251104246/spring-boot-ci-demo

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-12adc45eab79d4a8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 第一个红框里是加特林（gatling）脚本的存放目录位置与结构

- 第二个红框的Dockerfile也是必须的

- 项目必须为maven项目

- 前提用`docker-compose`构建服务器,步骤如下

    - 创建宿主机挂载目录：`mkdir -p /data/docker/ci/nexus /data/docker/ci/jenkins/lib /data/docker/ci/jenkins/home /data/docker/ci/sonarqube /data/docker/ci/postgresql /data/docker/ci/gatling/results`

    - 赋权（避免挂载的时候，一些程序需要容器中的用户的特定权限使用）：`chmod -R 777 /data/docker/ci/nexus /data/docker/ci/jenkins/lib /data/docker/ci/jenkins/home /data/docker/ci/sonarqube /data/docker/ci/postgresql /data/docker/ci/gatling/results`

    - 使用`docker-compose`执行如下脚本

    ```

    version: '3'

    networks:

      prodnetwork:

        driver: bridge

    services:

      sonardb:

        image: postgres:9.6.6

        restart: always

        ports:

          - "5433:5432"

        networks:

          - prodnetwork

        volumes:

          - /data/docker/ci/postgresql:/var/lib/postgresql

        environment:

          - POSTGRES_USER=sonar

          - POSTGRES_PASSWORD=sonar

      sonar:

        image: sonarqube:6.7.1

        restart: always

        ports:

        - "19000:9000"

        - "19092:9092"

        networks:

          - prodnetwork

        depends_on:

          - sonardb

        volumes:

          - /data/docker/ci/sonarqube/conf:/opt/sonarqube/conf

          - /data/docker/ci/sonarqube/data:/opt/sonarqube/data

          - /data/docker/ci/sonarqube/extension:/opt/sonarqube/extensions

          - /data/docker/ci/sonarqube/bundled-plugins:/opt/sonarqube/lib/bundled-plugins

        environment:

          #- SONARQUBE_JDBC_URL=jdbc:postgresql://sonardb:5433/sonar

          - SONARQUBE_JDBC_URL=jdbc:postgresql://sonardb:5432/sonar

          - SONARQUBE_JDBC_USERNAME=sonar

          - SONARQUBE_JDBC_PASSWORD=sonar

      nexus:

        image: sonatype/nexus3

        restart: always

        ports:

          - "18081:8081"

        networks:

          - prodnetwork

        volumes:

          - /data/docker/ci/nexus:/nexus-data

      jenkins:

        image: wine6823/jenkins:1.1

        restart: always

        ports:

          - "18080:8080"

        networks:

          - prodnetwork

        volumes:

          - /var/run/docker.sock:/var/run/docker.sock

          - /etc/localtime:/etc/localtime:ro

          - $HOME/.ssh:/root/.ssh

          - /data/docker/ci/jenkins/lib:/var/lib/jenkins/

          - /usr/bin/docker:/usr/bin/docker

          - /data/docker/ci/jenkins/home:/var/jenkins_home

        depends_on:

          - nexus

          - sonar

        environment:

          - NEXUS_PORT=8081

          - SONAR_PORT=9000

          - SONAR_DB_PORT=5432

    ```

- 最后用dockers启动一个nginx服务器，命令如下

```

docker run --name some-nginx -v /data/docker/ci/gatling/results:/usr/share/nginx/html:ro -p 8080:80 -d nginx

```

## 步骤一(jenkins)

- 创建jenkins流水线([打开jenkins服务器首页，点击新建任务](http://47.90.83.207:7070/view/all/newJob))

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-47c652aab9b1c5e9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 配置流水线脚本([配置地址](http://47.90.83.207:7070/job/ci-demo/configure))

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-bb458efae99f8bc0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- `Jenkinsfiles`脚本代码如下

```

node {

/*****************************git代码块***********************************/

    // ! git 仓库路径

    gitUrl = 'https://github.com/14251104246/spring-boot-ci-demo.git'

    // ! git 分支

    gitBranch = 'master'

    // ! git 使用的jenkins证书id

    gitCredential = '5b3912a8-0074-4955-b48f-c0af217785ac'

    stage ('git') {

        git branch: gitBranch,

            credentialsId: gitCredential,

            url: gitUrl

    }

/*****************************预处理代码块***********************************/

    // ! maven最终构建项目ArtifactId（项目目录请和ArtifactId一致）(可改)

    mavenArtifactId = 'yisike-parent'

    // 文件夹分隔

    DELIMITER = '/'

    // jenkins挂载在宿主机的所有项目地址

    volumeRoot = '/data/docker/ci/jenkins/home';

    // jenkins在容器中的所有项目地址

    jenkinsRoot = '/var/jenkins_home'

    // 具体项目工作区

    projectWorkspace = 'workspace'

    // jenkins挂载在宿主机的项目地址

    volumeDeployRootPath = volumeRoot + DELIMITER + projectWorkspace + DELIMITER + env.JOB_NAME

    // jenkins在容器中的项目路径

    jenkinsDeployRootPath = env.WORKSPACE

    // jenkins挂载在宿主机的构建地址

    volumeDeployPath = ''

    // jenkins在容器中的构建路径

    jenkinsDeployPath = ''

    //是否只有一个项目

    mavenSingleModule = true

    String relativePath = ''

    stage ('pre') {

        echo '路径预处理'

        volumeDeployPath = volumeDeployRootPath

        jenkinsDeployPath = jenkinsDeployRootPath

        println 'jenkins挂载在宿主机的项目地址：' + volumeDeployRootPath

        println 'jenkins在容器中的项目路径：' + jenkinsDeployRootPath

        println 'jenkins挂载在宿主机的构建地址：' + volumeDeployPath

        println 'jenkins在容器中的构建路径：' + jenkinsDeployPath

    }

  /*******************************maven代码块*********************************/

    // maven地址

    MVN_HOME = 'docker run -t --rm --name my-maven-project -v '+volumeDeployPath+':/usr/src/mymaven -v $HOME/.m2:/root/.m2 -w /usr/src/mymaven maven:3.3-jdk-8 '

    stage ('mvn') {

        echo 'start mvn'

        //暂时使用sh，后边替换为 ArtifactoryMavenBuild

        stage ('mvn-clean') {

            sh """

${MVN_HOME} mvn clean

"""

        }

        stage ('mvn-package') {

            thisPath = jenkinsDeployRootPath + DELIMITER + (relativePath == null || relativePath.size() == 0 ? relativePath : relativePath + DELIMITER)+ DELIMITER

            sh """

            ${MVN_HOME} mvn package  -DskipTests

            """

        }

        echo 'stop mvn'

    }

/**********************************docker代码块************************************************************/

/**********************************docker代码块************************************************************/

    //服务名称（子模块目录名称）

    def srvName = 'demo'

    //仓库地址

    def registryUrl = 'hboverseas.banggood.cn'

    //远程服务器地址（用于部署）

    def devHost = '122.152.225.111'

    def devDockerDaemon = "tcp://${devHost}:2376"

    //项目名称

    def project = 'ad'

    //远程docker容器启动参数

    def dockerParam = "--name ${srvName} -v /etc/hosts:/etc/hosts -v /data/docker/logs/yisike/yisike-registry:/data/docker/logs --restart=always  -p 8090:8090"

    stage ('docker') {

        echo 'start docker'

        stage ('构建镜像')

        {

            sh """

                cd ${jenkinsDeployPath}/

                docker build -t $registryUrl/$project/$srvName:$BUILD_NUMBER ./

              """

        }

        //stage ('推送镜像')

        //{

        //    sh """

        //      docker push $registryUrl/$project/$srvName:$BUILD_NUMBER

        //      docker rmi $registryUrl/$project/$srvName:$BUILD_NUMBER

        //      """

        //}

        //stage ('部署生产环境')

        //{

        // sh """

        //  docker -H ${devDockerDaemon} pull $registryUrl/$project/$srvName:$BUILD_NUMBER

        //  docker -H ${devDockerDaemon} rm -f ${srvName} | true

        //  docker -H ${devDockerDaemon} run -d  ${dockerParam}  $registryUrl/$project/$srvName:$BUILD_NUMBER

        //    """

        //}

        stage ('部署生产环境')

        {

          sh """

            docker rm -f ${srvName} | true

            docker run -d  ${dockerParam}  $registryUrl/$project/$srvName:$BUILD_NUMBER

            """

        }

        echo 'complete docker'

    }

}

```

- 点击保存后进入`ci-demo`流水线，点击立即构建(图中2是构建结果)

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-486b02e3770c8adf.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 步骤二（集成sonar)

- 进入sonarQube服务器创建一个新的需要分析的项目（打开官方引导教程）

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-2d46576f2b87d9f4.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 复制生成的mvn命令

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-abae5fb968363c77.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 配置流水线脚本([配置地址](http://47.90.83.207:7070/job/ci-demo/configure))

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-bb458efae99f8bc0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 把mvn命令加入到`Jenkinsfiles`脚本，如下

```

node {

/*****************************git代码块***********************************/

    // ! git 仓库路径

    gitUrl = 'https://github.com/14251104246/spring-boot-ci-demo.git'

    // ! git 分支

    gitBranch = 'master'

    // ! git 使用的jenkins证书id

    gitCredential = '5b3912a8-0074-4955-b48f-c0af217785ac'

    stage ('git') {

        git branch: gitBranch,

            credentialsId: gitCredential,

            url: gitUrl

    }

/*****************************预处理代码块***********************************/

    // ! maven最终构建项目ArtifactId（项目目录请和ArtifactId一致）(可改)

    mavenArtifactId = 'yisike-parent'

    // 文件夹分隔

    DELIMITER = '/'

    // jenkins挂载在宿主机的所有项目地址

    volumeRoot = '/data/docker/ci/jenkins/home';

    // jenkins在容器中的所有项目地址

    jenkinsRoot = '/var/jenkins_home'

    // 具体项目工作区

    projectWorkspace = 'workspace'

    // jenkins挂载在宿主机的项目地址

    volumeDeployRootPath = volumeRoot + DELIMITER + projectWorkspace + DELIMITER + env.JOB_NAME

    // jenkins在容器中的项目路径

    jenkinsDeployRootPath = env.WORKSPACE

    // jenkins挂载在宿主机的构建地址

    volumeDeployPath = ''

    // jenkins在容器中的构建路径

    jenkinsDeployPath = ''

    //是否只有一个项目

    mavenSingleModule = true

    String relativePath = ''

    stage ('pre') {

        echo '路径预处理'

        volumeDeployPath = volumeDeployRootPath

        jenkinsDeployPath = jenkinsDeployRootPath

        println 'jenkins挂载在宿主机的项目地址：' + volumeDeployRootPath

        println 'jenkins在容器中的项目路径：' + jenkinsDeployRootPath

        println 'jenkins挂载在宿主机的构建地址：' + volumeDeployPath

        println 'jenkins在容器中的构建路径：' + jenkinsDeployPath

    }

  /*******************************maven代码块*********************************/

    // maven地址

    MVN_HOME = 'docker run -t --rm --name my-maven-project -v '+volumeDeployPath+':/usr/src/mymaven -v $HOME/.m2:/root/.m2 -w /usr/src/mymaven maven:3.3-jdk-8 '

    stage ('mvn') {

        echo 'start mvn'

        //暂时使用sh，后边替换为 ArtifactoryMavenBuild

        stage ('mvn-clean') {

            sh """

${MVN_HOME} mvn clean

"""

        }

        stage ('mvn-package') {

            thisPath = jenkinsDeployRootPath + DELIMITER + (relativePath == null || relativePath.size() == 0 ? relativePath : relativePath + DELIMITER)+ DELIMITER

            sh """

            ${MVN_HOME} mvn package  -DskipTests

            """

        }

        stage ('mvn-代码质量') {

            sh """

            ${MVN_HOME} mvn sonar:sonar \

                            -Dsonar.host.url=http://172.17.0.1:19000 \

                            -Dsonar.login=40aee4cc56ecf6d6742af9cb36fd0f00f896e306

            """

        }

        echo 'stop mvn'

    }

/**********************************docker代码块************************************************************/

/**********************************docker代码块************************************************************/

    //服务名称（子模块目录名称）

    def srvName = 'demo'

    //仓库地址

    def registryUrl = 'hboverseas.banggood.cn'

    //远程服务器地址（用于部署）

    def devHost = '122.152.225.111'

    def devDockerDaemon = "tcp://${devHost}:2376"

    //项目名称

    def project = 'ad'

    //远程docker容器启动参数

    def dockerParam = "--name ${srvName} -v /etc/hosts:/etc/hosts -v /data/docker/logs/yisike/yisike-registry:/data/docker/logs --restart=always  -p 8090:8090"

    stage ('docker') {

        echo 'start docker'

        stage ('构建镜像')

        {

            sh """

                cd ${jenkinsDeployPath}/

                docker build -t $registryUrl/$project/$srvName:$BUILD_NUMBER ./

              """

        }

        //stage ('推送镜像')

        //{

        //    sh """

        //      docker push $registryUrl/$project/$srvName:$BUILD_NUMBER

        //      docker rmi $registryUrl/$project/$srvName:$BUILD_NUMBER

        //      """

        //}

        //stage ('部署生产环境')

        //{

        // sh """

        //  docker -H ${devDockerDaemon} pull $registryUrl/$project/$srvName:$BUILD_NUMBER

        //  docker -H ${devDockerDaemon} rm -f ${srvName} | true

        //  docker -H ${devDockerDaemon} run -d  ${dockerParam}  $registryUrl/$project/$srvName:$BUILD_NUMBER

        //    """

        //}

        stage ('部署生产环境')

        {

          sh """

            docker rm -f ${srvName} | true

            docker run -d  ${dockerParam}  $registryUrl/$project/$srvName:$BUILD_NUMBER

            """

        }

        echo 'complete docker'

    }

}

```

- 点击保存后进入`ci-demo`流水线，点击立即构建

## 步骤三（集成gatling）

- 打开jenkins服务器首页，创建gatling的测试

- `Jenkinsfile`脚本如下

```

node {

/*****************************git代码块***********************************/

    // ! git 仓库路径

    gitUrl = 'https://github.com/14251104246/spring-boot-ci-demo.git'

    // ! git 分支

    gitBranch = 'master'

    // ! git 使用的jenkins证书id

    gitCredential = '5b3912a8-0074-4955-b48f-c0af217785ac'

    stage ('git') {

        git branch: gitBranch,

            credentialsId: gitCredential,

            url: gitUrl

    }

/*****************************预处理代码块***********************************/

    // ! maven最终构建项目ArtifactId（项目目录请和ArtifactId一致）(可改)

    mavenArtifactId = 'yisike-parent'

    // 文件夹分隔

    DELIMITER = '/'

    // jenkins挂载在宿主机的所有项目地址

    volumeRoot = '/data/docker/ci/jenkins/home';

    // jenkins在容器中的所有项目地址

    jenkinsRoot = '/var/jenkins_home'

    // 具体项目工作区

    projectWorkspace = 'workspace'

    // jenkins挂载在宿主机的项目地址

    volumeDeployRootPath = volumeRoot + DELIMITER + projectWorkspace + DELIMITER + env.JOB_NAME

    // jenkins在容器中的项目路径

    jenkinsDeployRootPath = env.WORKSPACE

    // jenkins挂载在宿主机的构建地址

    volumeDeployPath = ''

    // jenkins在容器中的构建路径

    jenkinsDeployPath = ''

    //是否只有一个项目

    mavenSingleModule = true

    String relativePath = ''

    stage ('pre') {

        echo '路径预处理'

        volumeDeployPath = volumeDeployRootPath

        jenkinsDeployPath = jenkinsDeployRootPath

        println 'jenkins挂载在宿主机的构建地址：' + volumeDeployPath

        println 'jenkins在容器中的构建路径：' + jenkinsDeployPath

    }

  /*******************************gatling代码块*********************************/

    scalaScriptDir = volumeDeployPath + '/src/test/gatling/user-files'

    resultDir = '/data/docker/ci/gatling/results'

    // 加特林命令行

    COM_DOCKER = 'docker run  -t --rm  -v '+ scalaScriptDir +':/opt/gatling/user-files -v '+ resultDir +':/opt/gatling/results denvazh/gatling --mute'

    stage ('gatling') {

        echo 'start gatling'

        //暂时使用sh，后边替换为 ArtifactoryMavenBuild

        stage ('gatling') {

            sh """

            rm -f out.txt | true

${COM_DOCKER}  1>>out.txt

cat out.txt

echo http://localhost:8080\$(cat out.txt | grep 'Please open the following file' | cut -b 53- )

"""

        }

        echo 'stop gatling'

    }

}

```

- 创建完成后，点击构建，等待构建完成后查看输出

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-feb858da9fa41c18.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 点击上图中2指向的蓝点，查看**最底下**的输出

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-9cb56faf3a5893c2.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 点击链接，查看gatling的测试结果

> ![image.png](https://upload-images.jianshu.io/upload_images/12571020-4a2775752a1ab235.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
