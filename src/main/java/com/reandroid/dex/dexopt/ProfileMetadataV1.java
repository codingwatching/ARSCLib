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
package com.reandroid.dex.dexopt;

import com.reandroid.arsc.item.StringReference;

public class ProfileMetadataV1 extends ProfileMetadata {

    private final ProfileMetadataHeaderV1 header;
    private final ProfileClassList classList;

    public ProfileMetadataV1(ProfileMetadataHeaderV1 header) {
        super(1);
        this.header = header;
        this.classList = new ProfileClassList(header.classSetSize);

        addChild(0, classList);
    }

    @Override
    public StringReference name() {
        return header.name;
    }
    @Override
    public ProfileClassList classList() {
        return classList;
    }
}
