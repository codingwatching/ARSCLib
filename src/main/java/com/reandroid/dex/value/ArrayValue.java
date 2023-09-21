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
package com.reandroid.dex.value;

import com.reandroid.arsc.container.BlockList;
import com.reandroid.dex.item.EncodedArray;
import com.reandroid.dex.writer.SmaliWriter;

import java.io.IOException;
import java.util.Iterator;

public class ArrayValue extends DexValueBlock<EncodedArray>
        implements Iterable<DexValueBlock<?>> {
    public ArrayValue() {
        super(new EncodedArray());
    }

    public DexValueBlock<?> get(int i){
        return getElementBlockList().get(i);
    }
    public int size() {
        return getElementBlockList().size();
    }
    public void addValue(DexValueBlock<?> value){
        getElementBlockList().add(value);
    }
    public BlockList<DexValueBlock<?>> getElementBlockList() {
        return getValue().getElements();
    }
    @Override
    public Iterator<DexValueBlock<?>> iterator() {
        return getElementBlockList().iterator();
    }
    @Override
    public String getTypeName(){
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        Iterator<DexValueBlock<?>> iterator = iterator();
        if(iterator.hasNext()){
            builder.append(iterator.next().getTypeName());
        }
        return builder.toString();
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.append('{');
        writer.indentPlus();
        BlockList<DexValueBlock<?>> elements = getElementBlockList();
        int count = elements.size();
        for(int i = 0; i < count; i++){
            if(i != 0){
                writer.append(',');
            }
            writer.newLine();
            elements.get(i).append(writer);
        }
        writer.indentMinus();
        if(count > 0){
            writer.newLine();
        }
        writer.append('}');
    }
}
