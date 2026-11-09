@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements. See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership. The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License. You may obtain a copy of the License at
@REM
@REM https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied. See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET @@MVNW_LAUNCHER=mvnw
@SET MAVEN_PROJECTBASEDIR=%~dp0

@SETLOCAL

@REM Determine the Java home to use
@IF DEFINED JAVA_HOME (
    @SET _JAVA_CMD=%JAVA_HOME%\bin\java.exe
) ELSE (
    @SET _JAVA_CMD=java
)

@REM Determine the wrapper JAR location
@SET MVNW_REPOURL=https://repo.maven.apache.org/maven2
@SET WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@REM If the wrapper jar doesn't exist, download it
@IF NOT EXIST "%WRAPPER_JAR%" (
    @echo Downloading Maven Wrapper...
    @"%_JAVA_CMD%" -classpath "" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
        "-Dmaven.wrapper.forceMavenVersion=3.9.6" ^
        "-Dmaven.wrapper.mavenDistributionUrl=%MVNW_REPOURL%/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip" ^
        org.apache.maven.wrapper.MavenWrapperMain %WRAPPER_LAUNCHER% %* 2>NUL || (
        @echo Unable to download. Using system Maven if available.
    )
)

@REM Download the wrapper jar using PowerShell if it's still missing
@IF NOT EXIST "%WRAPPER_JAR%" (
    @powershell -Command "Invoke-WebRequest -Uri '%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%WRAPPER_JAR%'"
)

@REM Run Maven via the wrapper
@"%_JAVA_CMD%" ^
    -classpath "%WRAPPER_JAR%" ^
    "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
    "%WRAPPER_LAUNCHER%" %*

@ENDLOCAL
