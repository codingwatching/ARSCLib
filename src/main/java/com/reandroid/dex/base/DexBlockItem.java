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
package com.reandroid.dex.base;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.item.BlockItem;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.io.ByteReader;
import com.reandroid.dex.io.StreamUtil;
import com.reandroid.dex.pool.DexIdPool;
import com.reandroid.dex.sections.Section;
import com.reandroid.dex.sections.SectionList;
import com.reandroid.dex.sections.SectionType;

import java.io.IOException;
import java.io.InputStream;

public abstract class DexBlockItem extends BlockItem {

    public DexBlockItem(int bytesLength) {
        super(bytesLength);
    }

    public<T1 extends Block> T1 createOffsetItem(SectionType<T1> sectionType) {
        Section<T1> section = getSection(sectionType);
        if(section != null){
            return section.createOffsetItem();
        }
        return null;
    }

    public<T1 extends Block> T1 getAt(SectionType<T1> sectionType, IntegerReference offset){
        if(offset != null){
            return getAt(sectionType, offset.get());
        }
        return null;
    }
    public<T1 extends Block> T1 getAt(SectionType<T1> sectionType, int offset){
        if(offset == 0){
            return null;
        }
        Section<T1> section = getSection(sectionType);
        if(section != null){
            return section.getAt(offset);
        }
        return null;
    }
    public<T1 extends Block> T1[] getAt(SectionType<T1> sectionType, int[] offsets){
        if(offsets == null || offsets.length == 0){
            return null;
        }
        Section<T1> section = getSection(sectionType);
        if(section != null){
            return section.getAt(offsets);
        }
        return null;
    }
    public<T1 extends Block> T1[] get(SectionType<T1> sectionType, int[] indexes){
        if(indexes == null || indexes.length == 0){
            return null;
        }
        Section<T1> section = getSection(sectionType);
        if(section == null){
            return null;
        }
        return section.get(indexes);
    }
    public<T1 extends Block> T1 get(SectionType<T1> sectionType, int i){
        Section<T1> section = getSection(sectionType);
        if(section != null){
            return section.get(i);
        }
        return null;
    }
    public<T1 extends Block> Section<T1> getSection(SectionType<T1> sectionType){
        SectionList sectionList = getParent(SectionList.class);
        if(sectionList != null){
            return sectionList.get(sectionType);
        }
        return null;
    }

    public<T1 extends Block> DexIdPool<T1> getPool(SectionType<T1> sectionType){
        Section<T1> section = getSection(sectionType);
        if(section != null){
            return section.getPool();
        }
        return null;
    }

    public static int writeUleb128(byte[] bytes, int offset, int value) {
        int index = 0;
        while ((value & 0xffffffffL) > 0x7f) {
            bytes[offset + index] = (byte) ((value & 0x7f) | 0x80);
            value >>>= 7;
            index ++;
        }
        bytes[offset + index] = (byte) (value);
        return index + 1;
    }
    public static int readUleb128(InputStream inputStream) throws IOException {
        return readUleb128(StreamUtil.createByteReader(inputStream));
    }
    public static int readUleb128(byte[] bytes, int offset) throws IOException {
        return readUleb128(StreamUtil.createByteReader(bytes, offset));
    }
    public static int readUleb128(ByteReader reader) throws IOException{
        return readUleb128(reader, 4);
    }
    public static int readUleb128Large(ByteReader reader) throws IOException{
        return readUleb128(reader, 5);
    }
    public static int readUleb128(ByteReader reader, int size) throws IOException {
        int result = 0;
        int value = 0x80;
        int count = 0;
        while (value > 0x7f && count < size){
            value = reader.read();
            result |= ((value & 0x7f) << (count * 7));
            count ++;
        }
        if (value > 0x7f) {
            throw new IOException("Invalid uleb128 integer, size = " + size);
        } else if (count == size && ((value & 0xf) > 0x07)) {
            throw new IOException("Encountered valid uleb128 that is out of range, size = " + size);
        }
        return result;
    }
    public static int readSleb128(ByteReader reader) throws IOException {
        int value = 0x80;
        int result = 0;
        int count = 0;

        while (value > 0x7f && count < 5){
            value = reader.read();
            result = result | ((value & 0x7f) << count * 7);
            count ++;
        }
        if(count == 5 && value > 0x7f){
            throw new IOException("Invalid sleb128 integer");
        }
        int shift = 32 - count * 7;
        result = (result << shift) >> shift;
        return result;
    }
    public static int writeSleb128(byte[] bytes, int offset, int value) {
        int index = 0;
        if (value >= 0) {
            while (value > 0x3f) {
                bytes[offset + index] = (byte) ((value & 0x7f) | 0x80);
                index ++;
                value >>>= 7;
            }
            bytes[offset + index] = (byte) (value & 0x7f);
            index ++;
        } else {
            while (value < -0x40) {
                bytes[offset + index] = (byte) ((value & 0x7f) | 0x80);
                index ++;
                value >>= 7;
            }
            bytes[offset + index] = (byte) (value & 0x7f);
            index ++;
        }
        return index;
    }
    protected static long getNumber(byte[] bytes, int offset, int size){
        if((offset + size)>bytes.length){
            return 0;
        }
        long result = 0;
        int index = offset + size - 1;
        while (index>=offset){
            result = result << 8;
            result |= (bytes[index] & 0xff);
            index --;
        }
        return result;
    }
    protected static void putNumber(byte[] bytes, int offset, int size, long value){
        if((offset + size) > bytes.length){
            return;
        }
        int index = offset;
        offset = index + size;
        while (index<offset){
            bytes[index] = (byte) (value & 0xff);
            value = value >>> 8;
            index++;
        }
    }
}
