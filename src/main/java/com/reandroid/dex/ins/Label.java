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
package com.reandroid.dex.ins;

import com.reandroid.dex.writer.SmaliWriter;

import java.io.IOException;

public interface Label extends ExtraLine{
    int getAddress();
    int getTargetAddress();
    String getLabelName();
    @Override
    default void appendExtra(SmaliWriter writer) throws IOException {
        writer.append(getLabelName());
    }
    @Override
    default boolean isEqualExtraLine(Object obj) {
        if(!(obj instanceof Label)){
            return false;
        }
        Label label = (Label) obj;
        return getLabelName().equals(label.getLabelName());
    }
}