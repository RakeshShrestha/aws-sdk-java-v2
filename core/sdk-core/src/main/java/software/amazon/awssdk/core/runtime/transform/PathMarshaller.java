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

package software.amazon.awssdk.core.runtime.transform;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.core.util.StringConversion;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

// TODO move this to protocols-core
@SdkProtectedApi
public abstract class PathMarshaller {

    /**
     * Marshaller for non greedy path labels. Value is URL encoded and then replaced in the request URI.
     */
    public static final PathMarshaller NON_GREEDY = new NonGreedyPathMarshaller();

    /**
     * Marshaller for greedy path labels. Value is not URL encoded and replaced in the request URI.
     */
    public static final PathMarshaller GREEDY = new GreedyPathMarshaller();

    /**
     * Marshaller for members marked with the idempotency trait. If the path value is null, a value will
     * be auto generated by the SDK.
     */
    public static final PathMarshaller IDEMPOTENCY = new IdempotencyPathMarshaller();

    private PathMarshaller() {
    }

    private static String trimLeadingSlash(String value) {
        if (value.startsWith("/")) {
            return value.replaceFirst("/", "");
        }
        return value;
    }

    /**
     * @param resourcePath Current resource path with path param placeholder
     * @param paramName    Name of parameter (i.e. placeholder value {Foo})
     * @param pathValue    String value of path parameter.
     * @return New URI with placeholder replaced with marshalled value.
     */
    public abstract String marshall(String resourcePath, String paramName, String pathValue);

    /**
     * @param resourcePath Current resource path with path param placeholder
     * @param paramName    Name of parameter (i.e. placeholder value {Foo})
     * @param pathValue    Integer value of path parameter.
     * @return New URI with placeholder replaced with marshalled value.
     */
    public abstract String marshall(String resourcePath, String paramName, Integer pathValue);

    /**
     * @param resourcePath Current resource path with path param placeholder
     * @param paramName    Name of parameter (i.e. placeholder value {Foo})
     * @param pathValue    Long value of path parameter.
     * @return New URI with placeholder replaced with marshalled value.
     */
    public abstract String marshall(String resourcePath, String paramName, Long pathValue);

    private static class NonGreedyPathMarshaller extends PathMarshaller {
        @Override
        public String marshall(String resourcePath, String paramName, String pathValue) {
            Validate.notEmpty(pathValue, "%s cannot be empty.", paramName);
            return resourcePath.replace(String.format("{%s}", paramName), SdkHttpUtils.urlEncode(pathValue));
        }

        @Override
        public String marshall(String resourcePath, String paramName, Integer pathValue) {
            Validate.paramNotNull(pathValue, paramName);
            return marshall(resourcePath, paramName, StringConversion.fromInteger(pathValue));
        }

        @Override
        public String marshall(String resourcePath, String paramName, Long pathValue) {
            Validate.paramNotNull(pathValue, paramName);
            return marshall(resourcePath, paramName, StringConversion.fromLong(pathValue));
        }
    }

    private static class GreedyPathMarshaller extends PathMarshaller {

        @Override
        public String marshall(String resourcePath, String paramName, String pathValue) {
            Validate.notEmpty(pathValue, "%s cannot be empty.", paramName);
            return resourcePath.replace(String.format("{%s+}", paramName),
                                        SdkHttpUtils.urlEncodeIgnoreSlashes(trimLeadingSlash(pathValue)));
        }

        @Override
        public String marshall(String resourcePath, String paramName, Integer pathValue) {
            Validate.paramNotNull(pathValue, paramName);
            return marshall(resourcePath, paramName, StringConversion.fromInteger(pathValue));
        }

        @Override
        public String marshall(String resourcePath, String paramName, Long pathValue) {
            Validate.paramNotNull(pathValue, paramName);
            return marshall(resourcePath, paramName, StringConversion.fromLong(pathValue));
        }
    }

    private static class IdempotencyPathMarshaller extends PathMarshaller {
        @Override
        public String marshall(String resourcePath, String paramName, String pathValue) {
            if (pathValue != null && pathValue.isEmpty()) {
                throw new IllegalArgumentException(paramName + " must not be empty. If not set a value will be auto generated");
            }
            return resourcePath.replace(String.format("{%s}", paramName),
                                        SdkHttpUtils.urlEncode(IdempotentUtils.resolveString(pathValue)));
        }

        @Override
        public String marshall(String resourcePath, String paramName, Integer pathValue) {
            throw new UnsupportedOperationException("Integer idempotency tokens not yet supported");
        }

        @Override
        public String marshall(String resourcePath, String paramName, Long pathValue) {
            throw new UnsupportedOperationException("Long idempotency tokens not yet supported");
        }
    }
}
