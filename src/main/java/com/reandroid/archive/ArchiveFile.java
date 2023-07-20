/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.archive;

import com.reandroid.archive.io.ArchiveFileEntrySource;
import com.reandroid.archive.io.ZipFileInput;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class ArchiveFile extends Archive<ZipFileInput>{
    public ArchiveFile(ZipFileInput zipInput) throws IOException {
        super(zipInput);
    }
    public ArchiveFile(File file) throws IOException {
        this(new ZipFileInput(file));
    }

    @Override
    InputSource createInputSource(ArchiveEntry entry) {
        return new ArchiveFileEntrySource(getZipInput(), entry);
    }
    void extractStored(File file, ArchiveEntry archiveEntry) throws IOException {
        if(file.isFile()){
            file.delete();
        }
        file.createNewFile();
        StandardOpenOption openOption = StandardOpenOption.WRITE;
        FileChannel outputChannel = FileChannel.open(file.toPath(), openOption);
        FileChannel fileChannel = getZipInput().getFileChannel();
        fileChannel.position(archiveEntry.getFileOffset());
        outputChannel.transferFrom(fileChannel, 0, archiveEntry.getDataSize());
        outputChannel.close();
    }

}
