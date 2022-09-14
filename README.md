# swagger_codegen



## Описание:

Автогенерация REST контроллеров, моделей и интерфейсов сервисов для Spring, на основании протокола 
OpenApi 3.0

## Установка:

1. Склонировать проект из репозитория
2. Выполнить `mvn clean install`.

## Использование:

Описать контракт приложения в соотвествии с OpenAPI 3.0  
[Пример, как это можно сделать](https://editor.swagger.io)  
[Документация протокола OpenApi 3.0](https://swagger.io/specification)


В проекте в котором предполагается использование swagger_codegen выполнить:

1. Добавить следующие зависимости зависимости в pom.xml:

```xml
<dependency>
    <groupId>javax.validation</groupId>
    <artifactId>validation-api</artifactId>
    <version>${validation-api-current-version}</version>
</dependency>

<dependency>
	<groupId>io.swagger</groupId>
	<artifactId>swagger-annotations</artifactId>
	<version>${swagger-annotations-current-version}</version>
</dependency>

<dependency>
	<groupId>org.openapitools</groupId>
	<artifactId>jackson-databind-nullable</artifactId>
	<version>${jackson-databind-nullable-current-version}</version>
</dependency>
```

или при использовании "spring-boot-starter-parent"
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
	<groupId>io.swagger</groupId>
	<artifactId>swagger-annotations</artifactId>
	<version>${swagger-annotations-current-version}</version>
</dependency>

<dependency>
	<groupId>org.openapitools</groupId>
	<artifactId>jackson-databind-nullable</artifactId>
	<version>${jackson-databind-nullable-current-version}</version>
</dependency>
```

2. Добавить plugin в pom.xml:
```xml
<plugin>
	<groupId>org.openapitools</groupId>
	<artifactId>openapi-generator-maven-plugin</artifactId>
	<!-- RELEASE_VERSION -->
	<version>6.0.1</version>
	<!-- /RELEASE_VERSION -->
	<executions>
		<execution>
			<goals>
				<goal>generate</goal>
			</goals>
			<configuration>
				<inputSpec>${path to the yml contract file}</inputSpec>
				<generatorName>spring-codegen</generatorName>
				<apiPackage>ru.egartech.${application package name}</apiPackage>
				<modelPackage>ru.egartech.${application package name}.model</modelPackage>
				<configOptions>
					<useDto>false</useDto>
				</configOptions>
			</configuration>
		</execution>
	</executions>
	<dependencies>
		<dependency>
			<groupId>ru.egartech</groupId>
			<artifactId>swagger-codegen</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>
	</dependencies>
</plugin>
```

Пример:
```xml
    <apiPackage>ru.egartech.aplication</apiPackage>
    <modelPackage>ru.egartech.application.model</modelPackage>
```

Дополнительно можно добавить опцию для добавления суффикса "Dto" к имени модели:
```xml
<configuration>
    <configOptions>
    	<useDto>false</useDto>
    </configOptions>
</configuration>
```
3. Выполнить `mvn clean package` для генерации кода.

Пример структуры сгенерированных файлов
```
generated-sources
├───annotations
└───openapi
    │   .openapi-generator-ignore
    │
    ├───.openapi-generator
    │       FILES
    │       VERSION
    │
    └───src
        └───main
            └───java
                └───ru
                    └───egartech
                        └───petstore
                            ├───controller
                            │       PetController.java
                            │       StoreController.java
                            │       UserController.java
                            │
                            ├───model
                            │       Address.java
                            │       ApiResponse.java
                            │       Category.java
                            │       Customer.java
                            │       Order.java
                            │       Pet.java
                            │       Tag.java
                            │       User.java
                            │
                            └───service
                                    PetService.java
                                    StoreService.java
                                    UserService.java
```