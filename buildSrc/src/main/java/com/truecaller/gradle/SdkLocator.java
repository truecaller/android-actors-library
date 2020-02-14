/*
 * Copyright (C) 2017 True Software Scandinavia AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.truecaller.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

public class SdkLocator {
    /**
     * Returns the appropriate SDK path.
     */
    public static File findSdkLocation(final File supportRoot) {
        final Properties properties = new Properties();
        final File propertiesFile = new File(supportRoot, "local.properties");
        if (propertiesFile.exists()) {
            try (InputStream is = new FileInputStream(propertiesFile)) {
                properties.load(is);
            } catch (IOException e) {
                throw new SdkLocatorException(
                        "Cannot read properties file "
                                + "due to "
                                + e.getMessage()
                );
            }
        }

        return findSdkLocation(properties, supportRoot);
    }

    private static File findSdkLocation(Properties properties, File rootDir) {
        String sdkDirProp = properties.getProperty("sdk.dir");
        if (sdkDirProp != null) {
            File sdk = new File(sdkDirProp);
            if (!sdk.isAbsolute()) {
                sdk = new File(rootDir, sdkDirProp);
            }
            return sdk;
        }

        sdkDirProp = properties.getProperty("android.dir");
        if (sdkDirProp != null) {
            return new File(rootDir, sdkDirProp);
        }

        String[] otherPlaces = new String[]{
                System.getenv("ANDROID_HOME"),
                System.getProperty("android.home")
        };

        Optional<String> sdkLocation = Arrays.stream(otherPlaces)
                .filter((s) -> s != null)
                .findFirst();

        if (sdkLocation.isPresent()) {
            return new File(sdkLocation.get());
        }

        throw new SdkLocatorException("Android SDK location is not found");
    }

    private static class SdkLocatorException extends RuntimeException {

        public SdkLocatorException(final String message) {
            super(message);
        }
    }
}
