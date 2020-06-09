# TestReport
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.wazoakarapace/testreport/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.wazoakarapace/testreport/)

Simple Report Extension for JUnit 5

#### Work with JUnit 5 at least.

Maven dependency : 
```
<dependency>
   <groupId>io.github.wazoakarapace</groupId>
   <artifactId>testreport</artifactId>
   <version>1.6</version>
 </dependency>
 ```
 
# Configuration
In your test resources forlder, add the file `report.properties`

You can use maven replace to set project variable in your configuration.
```
projectname=MyZuperProjekt
version=@project.version@
```

The generated PDF will be in the target directory.

# Auto execute in JUnit 5
• Add in your maven-surefire-plugin configuration :
```
<properties>
   <configurationParameters>
       junit.jupiter.extensions.autodetection.enabled = true
   </configurationParameters>
</properties>
```
 
• Add in your `META-INF/services` folder a file named `org.junit.jupiter.api.extension.Extension` with 
`io.github.wazoakarapace.ReportExtension` in it.

It will be automatically loaded by JUnit 5 (while running normally, or maven-ly).

