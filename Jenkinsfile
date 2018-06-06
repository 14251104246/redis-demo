node {

/*****************************git代码块***********************************/
    // ! git 仓库路径(可改)
    gitUrl = 'http://192.168.0.105:10080/gitnavi/spring-boot-ci-demo'
    // ! git 代码分支(可改)
    gitBranch = 'master'
     // ! git 使用的jenkins证书id
    gitCredential = 'wt93jQzA8yu5a6pfsk3s'

    stage ('git') {
        git branch: gitBranch,
            credentialsId: gitCredential,
            url: gitUrl
    }

/*****************************预处理代码块***********************************/
    // ! maven最终构建项目ArtifactId（项目目录请和ArtifactId一致）(可改)
    mavenArtifactId = 'demo'

    // 文件夹分隔
    DELIMITER = '/'
    // jenkins Docker 容器要挂载在宿主机的所有项目存放路径
    volumeRoot = '/data/docker/ci/jenkins/home/jobs';
    // jenkins Docker 容器中的所有项目存放路径
    jenkinsRoot = '/var/jenkins_home/jobs'
    // 具体项目工作区
    projectWorkspace = 'workspace'

    // jenkins挂载在宿主机的项目地址
    volumeDeployRootPath = volumeRoot + DELIMITER + env.JOB_NAME + DELIMITER + projectWorkspace
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

        if (mavenSingleModule) {
            println 'git项目名称：' + mavenArtifactId + '，无相对路径，为单模块项目'
            volumeDeployPath = volumeDeployRootPath
            jenkinsDeployPath = jenkinsDeployRootPath
        } else {
            println 'git项目名称：' + mavenArtifactId + '，相对路径为: ' + relativePath + '，为多模块项目'
            volumeDeployPath = volumeDeployRootPath + DELIMITER + (relativePath == null || relativePath.size() == 0 ? relativePath : relativePath + DELIMITER) + mavenArtifactId
            jenkinsDeployPath = jenkinsDeployRootPath + DELIMITER + (relativePath == null || relativePath.size() == 0 ? relativePath : relativePath + DELIMITER) + mavenArtifactId
        }
        println 'jenkins挂载在宿主机的项目地址：' + volumeDeployRootPath
        println 'jenkins在容器中的项目路径：' + jenkinsDeployRootPath
        println 'jenkins挂载在宿主机的构建地址：' + volumeDeployPath
        println 'jenkins在容器中的构建路径：' + jenkinsDeployPath
    }

/*******************************maven代码块*********************************/
    // maven地址
    MVN_HOME = '/var/jenkins_home/apache-maven-3.5.3'

    dependencyPath = jenkinsDeployPath + DELIMITER +  'adg-pojo'

    stage ('mvn') {
        echo 'start mvn'
        //暂时使用sh，后边替换为 ArtifactoryMavenBuild
        stage ('mvn-clean') {
            sh """
			cd ${jenkinsDeployPath}
			${MVN_HOME}/bin/mvn clean
			"""
        }

        stage ('mvn安装依赖') {
            sh """
			cd ${dependencyPath}
			${MVN_HOME}/bin/mvn clean install
			"""
        }

        stage ('mvn-package') {
            thisPath = jenkinsDeployRootPath + DELIMITER + (relativePath == null || relativePath.size() == 0 ? relativePath : relativePath + DELIMITER)+ DELIMITER
            if(mavenSingleModule) {
                sh """
                cd ${thisPath}
                ${MVN_HOME}/bin/mvn -B package -am -amd -DskipTests
                """
            } else {
                sh """
                cd ${thisPath}
                ${MVN_HOME}/bin/mvn -B package -pl ${mavenArtifactId} -am -amd -DskipTests
                """
            }
        }
        echo 'stop mvn'
    }



}