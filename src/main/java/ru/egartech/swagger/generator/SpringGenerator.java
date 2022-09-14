package ru.egartech.swagger.generator;

import com.samskivert.mustache.Mustache;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.AbstractJavaCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.templating.mustache.SplitStringLambda;
import org.openapitools.codegen.templating.mustache.TrimWhitespaceLambda;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;


public class SpringGenerator extends AbstractJavaCodegen implements BeanValidationFeatures {

    private static final String LOMBOK = "useLombok";
    private static final String CONTROLLER = "controller";
    private static final String SERVICE = "service";
    private static final String USE_DTO = "useDto";
    private static final String DATE_TIME_FORMAT = "DateTimeFormat";

    protected String servicePackage = "ru.egartech.service";
    protected boolean useBeanValidation = true;
    protected boolean useDto = false;
    protected boolean useLombokAnnotation = true;

    public SpringGenerator() {
        super();
        outputFolder = "generated-code/javaSpring";
        apiPackage = "ru.egartech.swagger";
        modelPackage = "ru.egartech.swagger.model";
        templateDir = "egartech-java-spring";
        additionalProperties.put("servicePackage", servicePackage);
        additionalProperties.put("serviceName", StringUtils.capitalize(SERVICE));
        additionalProperties.put(CONTROLLER, CONTROLLER);
        additionalProperties.put(SERVICE, SERVICE);
        additionalProperties.put(JACKSON, "true");
        additionalProperties.put(LOMBOK, useLombokAnnotation);
        additionalProperties.put(USE_DTO, false);
        apiNameSuffix = "";
        apiTemplateFiles.put("apiController.mustache", "Controller.java");
        apiTemplateFiles.put("apiService.mustache", "Service.java");
        cliOptions.add(CliOption.newBoolean(USE_DTO, "Use suffix \"Dto\" in model name", useDto));
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        final OperationMap operations = objs.getOperations();
        Optional.ofNullable(operations).ifPresent(o -> {
            final List<CodegenOperation> ops = operations.getOperation();
            for (final CodegenOperation operation : ops) {
                final List<CodegenResponse> responses = operation.responses;
                Optional.ofNullable(responses).ifPresent(r -> {
                    for (final CodegenResponse resp : responses) {
                        resp.code = "0".equals(resp.code) ? String.valueOf(HttpStatus.SC_OK) : resp.code;
                        doDataTypeAssignment(resp.dataType, new DataTypeAssigner() {
                            @Override
                            public void setReturnType(String returnType) {
                                resp.dataType = returnType;
                            }
                            @Override
                            public void setReturnContainer(String returnContainer) {
                                resp.containerType = returnContainer;
                            }
                        });
                    }
                });
                doDataTypeAssignment(operation.returnType, new DataTypeAssigner() {
                    @Override
                    public void setReturnType(String returnType) {
                        operation.returnType = returnType;
                    }
                    @Override
                    public void setReturnContainer(String returnContainer) {
                        operation.returnContainer = returnContainer;
                    }
                });
                handleImplicitHeaders(operation);
            }
            objs.put("tagDescription", ops.get(0).tags.get(0).getDescription());
        });
        return objs;
    }

    @Override
    public void processOpts() {
        super.processOpts();
        apiTemplateFiles.remove("api.mustache");
        modelDocTemplateFiles.remove("model_doc.mustache");
        apiDocTemplateFiles.remove("api_doc.mustache");
        apiTestTemplateFiles.remove("api_test.mustache");
        importMapping.put(DATE_TIME_FORMAT, "org.springframework.format.annotation.DateTimeFormat");
        importMapping.put("ApiIgnore", "springfox.documentation.annotations.ApiIgnore");
        setDateLibrary("java8");
        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            this.setUseBeanValidation(convertPropertyToBoolean(USE_BEANVALIDATION));
        }
        if (additionalProperties.containsKey(USE_DTO)) {
            this.setUseDto(convertPropertyToBoolean(USE_DTO));
        }
        writePropertyBack(USE_BEANVALIDATION, useBeanValidation);
        // add lambda for mustache templates
        additionalProperties.put("lambdaRemoveDoubleQuote", (Mustache.Lambda) (fragment, writer) -> writer
                .write(fragment.execute().replaceAll("\"", Matcher.quoteReplacement(""))));
        additionalProperties.put("lambdaEscapeDoubleQuote", (Mustache.Lambda) (fragment, writer) -> writer
                .write(fragment.execute().replaceAll("\"", Matcher.quoteReplacement("\\\""))));
        additionalProperties.put("lambdaRemoveLineBreak",
                (Mustache.Lambda) (fragment, writer) -> writer.write(fragment.execute().replaceAll("\\r|\\n", "")));
        additionalProperties.put("lambdaTrimWhitespace", new TrimWhitespaceLambda());
        additionalProperties.put("lambdaSplitString", new SplitStringLambda());
        additionalProperties.put("lambdaCapitalize", (Mustache.Lambda) (fragment, writer) -> writer.write(fragment.execute().substring(0,1).toUpperCase() +
                fragment.execute().toLowerCase().substring(1)) );
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, servers);
        codegenOperation.allParams.stream().filter(p -> p.isDate || p.isDateTime).findFirst()
                .ifPresent(p -> codegenOperation.imports.add(DATE_TIME_FORMAT));
        return super.fromOperation(path, httpMethod, operation, servers);
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        // add org.springframework.format.annotation.DateTimeFormat when needed
        if (property.isDate || property.isDateTime) {
            model.imports.add(DATE_TIME_FORMAT);
        }
        if ("null".equals(property.example)) {
            property.example = null;
        }
        // Add imports for Jackson
        if (!Boolean.TRUE.equals(model.isEnum)) {
            model.imports.add("JsonProperty");
            if (Boolean.TRUE.equals(model.hasEnums)) {
                model.imports.add("JsonValue");
            }
        } else { // enum class
            // Needed imports for Jackson's JsonCreator
            if (additionalProperties.containsKey(JACKSON)) {
                model.imports.add("JsonCreator");
            }
        }
        // Add imports for java.util.Arrays
        if (property.isByteArray) {
            model.imports.add("Arrays");
        }
        if (model.getVendorExtensions().containsKey("x-jackson-optional-nullable-helpers")) {
            model.imports.add("Arrays");
        }
    }

    @Override
    public String toModelName(String name) {
        super.toModelName(name);
        return useDto ? name + "Dto" : name;
    }

    @Override
    public boolean isEnablePostProcessFile() {
        return true;
    }

    @Override
    public void postProcessFile(File file, String fileType) {
        super.postProcessFile(file, fileType);
        if(fileType.equals("api")){
            String fileName = FilenameUtils.removeExtension(file.toPath().getFileName().toString());
            if(fileName.toLowerCase().endsWith(CONTROLLER)){
                moveFile(file, CONTROLLER);
            }
            if(fileName.toLowerCase().endsWith(SERVICE)){
                moveFile(file, SERVICE);
            }
        }
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "spring-codegen";
    }

    @Override
    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    public void setUseDto(boolean useDto) {
        this.useDto = useDto;
    }

    private void moveFile(File file, String folderName)  {
        String target = String.join(File.separator, file.getParentFile().getPath(), folderName, file.toPath().getFileName().toString());
        try {
            FileUtils.moveFile(file, FileUtils.getFile(target));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void doDataTypeAssignment(String returnType, DataTypeAssigner dataTypeAssigner) {
        final String rt = returnType;
        if (rt == null) {
            dataTypeAssigner.setReturnType("Void");
        } else if (rt.startsWith("List") || rt.startsWith("java.util.List")) {
            final int start = rt.indexOf("<");
            final int end = rt.lastIndexOf(">");
            if (start > 0 && end > 0) {
                dataTypeAssigner.setReturnType(rt.substring(start + 1, end).trim());
                dataTypeAssigner.setReturnContainer("List");
            }
        } else if (rt.startsWith("Map") || rt.startsWith("java.util.Map")) {
            final int start = rt.indexOf("<");
            final int end = rt.lastIndexOf(">");
            if (start > 0 && end > 0) {
                dataTypeAssigner.setReturnType(rt.substring(start + 1, end).split(",", 2)[1].trim());
                dataTypeAssigner.setReturnContainer("Map");
            }
        } else if (rt.startsWith("Set") || rt.startsWith("java.util.Set")) {
            final int start = rt.indexOf("<");
            final int end = rt.lastIndexOf(">");
            if (start > 0 && end > 0) {
                dataTypeAssigner.setReturnType(rt.substring(start + 1, end).trim());
                dataTypeAssigner.setReturnContainer("Set");
            }
        }
    }

    private interface DataTypeAssigner {
        void setReturnType(String returnType);
        void setReturnContainer(String returnContainer);
    }
}
