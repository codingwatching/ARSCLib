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
package com.reandroid.dex.model;

import com.reandroid.dex.common.AccessFlag;
import com.reandroid.dex.index.MethodId;
import com.reandroid.dex.index.TypeId;
import com.reandroid.dex.item.MethodDef;
import com.reandroid.utils.collection.ComputeIterator;

import java.util.Iterator;

public class DexMethod {
    private final DexClass dexClass;
    private final MethodDef methodDef;

    public DexMethod(DexClass dexClass, MethodDef methodDef){
        this.dexClass = dexClass;
        this.methodDef = methodDef;
    }

    public String getAccessFlags() {
        return AccessFlag.formatForField(getMethodDef().getAccessFlagsValue());
    }
    public String getName(){
        return getMethodId().getName();
    }
    public void setName(String name){
        getMethodId().setName(name);
    }
    public int getParametersCount() {
        return getMethodId().getParametersCount();
    }
    public String getParameter(int index) {
        TypeId typeId = getMethodId().getParameter(index);
        if(typeId != null){
            return typeId.getName();
        }
        return null;
    }
    public Iterator<String> getParameters() {
        return ComputeIterator.of(getMethodId().getParameters(), TypeId::getName);
    }
    public String getReturnType() {
        TypeId typeId = getMethodId().getReturnTypeId();
        if(typeId != null) {
            return typeId.getName();
        }
        return null;
    }

    public String getKey(){
        return getMethodId().getKey();
    }
    public MethodId getMethodId() {
        return getMethodDef().getMethodId();
    }
    public DexClass getDexClass() {
        return dexClass;
    }
    public MethodDef getMethodDef() {
        return methodDef;
    }
    @Override
    public String toString() {
        return getKey();
    }
}
