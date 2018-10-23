/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.poet.transform.protocols;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.query.AwsQueryProtocolFactory;
import software.amazon.awssdk.utils.StringUtils;

public class QueryMarshallerSpec implements MarshallerProtocolSpec {

    private final ShapeModel shapeModel;
    private final Metadata metadata;

    public QueryMarshallerSpec(IntermediateModel model, ShapeModel shapeModel) {
        this.metadata = model.getMetadata();
        this.shapeModel = shapeModel;
    }

    @Override
    public ParameterSpec protocolFactoryParameter() {
        return ParameterSpec.builder(AwsQueryProtocolFactory.class, "protocolFactory").build();
    }

    @Override
    public CodeBlock marshalCodeBlock(ClassName requestClassName) {
        String variableName = shapeModel.getVariable().getVariableName();
        return CodeBlock.builder()
                        .addStatement("$T<$T<$T>> protocolMarshaller = protocolFactory.createProtocolMarshaller"
                                      + "(SDK_OPERATION_BINDING, $L)",
                                      ProtocolMarshaller.class, Request.class,
                                      requestClassName, variableName)
                        .addStatement("return protocolMarshaller.marshall($L)", variableName)
                        .build();
    }

    @Override
    public FieldSpec protocolFactory() {
        return FieldSpec.builder(AwsQueryProtocolFactory.class, "protocolFactory")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build();
    }

    @Override
    public Optional<MethodSpec> constructor() {
        return Optional.of(MethodSpec.constructorBuilder()
                                     .addModifiers(Modifier.PUBLIC)
                                     .addParameter(protocolFactoryParameter())
                                     .addStatement("this.protocolFactory = protocolFactory")
                                     .build());
    }

    @Override
    public List<FieldSpec> memberVariables() {
        List<FieldSpec> fields = new ArrayList<>();

        CodeBlock.Builder initializationCodeBlockBuilder = CodeBlock.builder()
                                                                    .add("$T.builder()", OperationInfo.class);
        initializationCodeBlockBuilder.add(".requestUri($S)", shapeModel.getMarshaller().getRequestUri())
                                      .add(".httpMethodName($T.$L)", HttpMethodName.class, shapeModel.getMarshaller().getVerb())
                                      .add(".hasExplicitPayloadMember($L)", shapeModel.isHasPayloadMember() ||
                                                                            shapeModel.getExplicitEventPayloadMember() != null)
                                      .add(".hasPayloadMembers($L)", shapeModel.hasPayloadMembers());

        if (StringUtils.isNotBlank(shapeModel.getMarshaller().getTarget())) {
            initializationCodeBlockBuilder.add(".operationIdentifier($S)", shapeModel.getMarshaller().getTarget())
                                          .add(".apiVersion($S)", metadata.getApiVersion())
                                          .add(".serviceName($S)", metadata.getServiceName());
        }

        if (shapeModel.isHasStreamingMember()) {
            initializationCodeBlockBuilder.add(".hasStreamingInput(true)");
        }

        CodeBlock codeBlock = initializationCodeBlockBuilder.add(".build()").build();

        FieldSpec.Builder instance = FieldSpec.builder(ClassName.get(OperationInfo.class), "SDK_OPERATION_BINDING")
                                              .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                                              .initializer(codeBlock);

        fields.add(instance.build());
        fields.add(protocolFactory());
        return fields;
    }
}
