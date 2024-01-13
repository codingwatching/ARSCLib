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
package com.reandroid.dex.smali.model;

import com.reandroid.dex.debug.DebugElementType;
import com.reandroid.dex.debug.DebugPrologue;
import com.reandroid.dex.smali.*;

import java.io.IOException;

public class SmaliDebugPrologue extends SmaliDebug implements SmaliRegion {

    public SmaliDebugPrologue(){
        super();
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        SmaliParseException.expect(reader, getSmaliDirective());
    }
    @Override
    public SmaliDirective getSmaliDirective() {
        return SmaliDirective.PROLOGUE;
    }
    @Override
    public DebugElementType<DebugPrologue> getDebugElementType() {
        return DebugElementType.PROLOGUE;
    }
}
