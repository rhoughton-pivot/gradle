/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.externalresource.transport.file;

import org.apache.commons.io.IOUtils;
import org.gradle.api.internal.externalresource.DefaultLocallyAvailableExternalResource;
import org.gradle.api.internal.externalresource.ExternalResource;
import org.gradle.api.internal.externalresource.metadata.ExternalResourceMetaData;
import org.gradle.api.internal.externalresource.transfer.ExternalResourceAccessor;
import org.gradle.api.internal.externalresource.transfer.ExternalResourceLister;
import org.gradle.api.internal.externalresource.transfer.ExternalResourceUploader;
import org.gradle.internal.Factory;
import org.gradle.internal.hash.HashValue;
import org.gradle.internal.resource.local.DefaultLocallyAvailableResource;
import org.gradle.util.GFileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class FileResourceConnector implements ExternalResourceLister, ExternalResourceAccessor, ExternalResourceUploader {
    public List<URI> list(URI parent) throws IOException {
        File dir = getFile(parent);
        if (dir.exists() && dir.isDirectory()) {
            String[] names = dir.list();
            if (names != null) {
                List<URI> ret = new ArrayList<URI>(names.length);
                for (String name : names) {
                    ret.add(parent.resolve(name));
                }
                return ret;
            }
        }
        return null;
    }

    public void upload(Factory<InputStream> source, Long contentLength, URI destination) throws IOException {
        File target = getFile(destination);
        if (!target.canWrite()) {
            target.delete();
        } // if target is writable, the copy will overwrite it without requiring a delete
        GFileUtils.mkdirs(target.getParentFile());
        FileOutputStream fileOutputStream = new FileOutputStream(target);
        try {
            InputStream sourceInputStream = source.create();
            try {
                IOUtils.copyLarge(sourceInputStream, fileOutputStream);
            } finally {
                sourceInputStream.close();
            }
        } finally {
            fileOutputStream.close();
        }
    }

    public ExternalResource getResource(URI uri) throws IOException {
        File localFile = getFile(uri);
        if (!localFile.exists()) {
            return null;
        }
        return new DefaultLocallyAvailableExternalResource(uri, new DefaultLocallyAvailableResource(localFile));
    }

    public ExternalResourceMetaData getMetaData(URI location) throws IOException {
        ExternalResource resource = getResource(location);
        return resource == null ? null : resource.getMetaData();
    }

    public HashValue getResourceSha1(URI location) {
        // TODO Read sha1 from published .sha1 file
        return null;
    }

    private static File getFile(URI uri) {
        return new File(uri);
    }
}
