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
package com.reandroid.dex.index;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.base.BlockRefresh;
import com.reandroid.arsc.item.IndirectItem;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.base.DexBlockItem;
import com.reandroid.dex.base.StringKeyItem;
import com.reandroid.dex.pool.DexIdPool;
import com.reandroid.dex.sections.SectionType;

public class ItemIndexReference<T extends IndexItemEntry> extends IndirectItem<DexBlockItem>
        implements IntegerReference, BlockRefresh, StringKeyItem {
    private final SectionType<T> sectionType;
    private T item;
    public ItemIndexReference(SectionType<T> sectionType, DexBlockItem blockItem, int offset) {
        super(blockItem, offset);
        this.sectionType = sectionType;
        set(-1);
    }
    @Override
    public String getKey(){
        T item = getItem();
        if(item != null){
            return item.getKey();
        }
        return null;
    }
    public T getItem() {
        int i = get();
        if(item == null){
            item = getBlockItem().get(sectionType, i);
        }
        return item;
    }
    public void setItem(T item) {
        if(item == this.item){
            return;
        }
        int value;
        if(item != null) {
            value = item.getIndex();
        }else {
            value = -1;
        }
        set(value);
        this.item = item;
    }
    public void setItem(String item){
        DexIdPool<T> pool = getBlockItem().getPool(sectionType);
        setItem(pool.getOrCreate(item));
    }

    @Override
    public void set(int value) {
        Block.putInteger(getBytesInternal(), getOffset(), value);
        item = null;
    }
    @Override
    public int get() {
        return Block.getInteger(getBytesInternal(), getOffset());
    }

    @Override
    public void refresh() {
        T item = getItem();
        if(item != null){
            Block.putInteger(getBytesInternal(), getOffset(), item.getIndex());
        }
    }

    @Override
    public String toString() {
        if(item != null){
            return get() + ": " + item.toString();
        }
        return sectionType.getName() + ": " + get();
    }
}
