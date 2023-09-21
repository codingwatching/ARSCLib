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
package com.reandroid.dex.sections;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.base.*;
import com.reandroid.utils.HexUtil;

import java.util.Comparator;

public class MapItem extends DexBlockItem {
    private final IndirectInteger type;
    private final IntegerPair countAndOffset;

    public MapItem() {
        super(SIZE);
        this.type = new IndirectInteger(this, 0);
        this.countAndOffset = new IndirectIntegerPair(this, 4);
    }

    public IntegerPair getCountAndOffset() {
        return countAndOffset;
    }
    public<T1 extends Block> SectionType<T1> getMapType(){
        return SectionType.get(getType().get());
    }

    public<T1 extends Block> Section<T1> createNewSection(){
        SectionType<T1> sectionType = getMapType();
        if(sectionType == null || sectionType.getCreator() == null){
            return null;
        }
        Block parent = getParentInstance(SectionList.class);
        if(parent != null){
            parent = parent.getParent();
        }
        if(parent == null){
            parent = getParentInstance(MapList.class);
        }
        if(parent == null){
            parent = getParentInstance(DexFileBlock.class);
        }
        if(parent == null){
            parent = getParent();
        }
        Section<T1> section = new Section<>(getCountAndOffset(), sectionType);
        section.setParent(parent);
        return section;
    }

    public void setType(SectionType<?> type){
        getType().set(type.getType());
    }
    public IntegerReference getType(){
        return type;
    }
    public IntegerReference getCount(){
        return countAndOffset.getFirst();
    }
    public IntegerReference getOffset(){
        return countAndOffset.getSecond();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        SectionType<?> sectionType = getMapType();
        String name;
        if(sectionType == null){
            name = HexUtil.toHex("UNKNOWN_", getType().get(), 1);
        }else {
            name = sectionType.getName();
        }
        builder.append(name);
        builder.append(' ');
        int fill = 24 - name.length();
        for(int i = 0; i < fill; i++){
            builder.append('-');
        }
        IntegerPair co = getCountAndOffset();
        builder.append("[");
        name = Integer.toString(co.getFirst().get());
        builder.append(name);
        fill = 6 - name.length();
        for(int i = 0; i < fill; i++){
            builder.append(' ');
        }
        builder.append(',');
        name = Integer.toString(co.getSecond().get());
        fill = 8 - name.length();
        for(int i = 0; i < fill; i++){
            builder.append(' ');
        }
        builder.append(name);
        builder.append(']');
        return builder.toString();
    }

    public static final Comparator<MapItem> READ_COMPARATOR = (mapItem1, mapItem2) -> {
        if(mapItem1 == mapItem2){
            return 0;
        }
        if(mapItem1 == null){
            return 1;
        }
        if(mapItem2 == null){
            return -1;
        }
        SectionType<?> sectionType1 = mapItem1.getMapType();
        SectionType<?> sectionType2 = mapItem2.getMapType();
        if(sectionType1 == sectionType2){
            return 0;
        }
        if(sectionType1 == null){
            return 1;
        }
        return sectionType1.compareReadOrder(sectionType2);
    };

    private static final int SIZE = 12;
}
