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
package com.reandroid.dex.refactor;

import com.reandroid.dex.id.StringId;
import com.reandroid.dex.key.KeyPair;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.model.DexClassRepository;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.utils.ObjectsUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RenameTypes extends Rename<TypeKey, TypeKey>{

    private int arrayDepth;
    private boolean renameSignatures;
    private boolean renameSource;
    private boolean noRenameSourceForNoPackageClass;

    public RenameTypes(){
        super();
        this.arrayDepth = DEFAULT_ARRAY_DEPTH;
        this.renameSignatures = true;
        this.renameSource = true;
        this.noRenameSourceForNoPackageClass = true;
    }

    @Override
    public int apply(DexClassRepository classRepository) {
        Map<String, String> map = buildMap();
        Iterator<StringId> iterator = classRepository.getClonedItems(SectionType.STRING_ID);
        int count = 0;
        while (iterator.hasNext()){
            StringId stringId = iterator.next();
            String text = map.get(stringId.getString());
            if(text == null){
                continue;
            }
            stringId.setString(text);
            count ++;
        }
        return count;
    }

    public void setArrayDepth(int arrayDepth) {
        if(arrayDepth < 0){
            arrayDepth = DEFAULT_ARRAY_DEPTH;
        }
        this.arrayDepth = arrayDepth;
    }
    public void setRenameSignatures(boolean renameSignatures) {
        this.renameSignatures = renameSignatures;
    }
    public void setRenameSource(boolean renameSource) {
        this.renameSource = renameSource;
    }
    public void setNoRenameSourceForNoPackageClass(boolean noRenameSourceForNoPackageClass) {
        this.noRenameSourceForNoPackageClass = noRenameSourceForNoPackageClass;
    }

    private Map<String, String> buildMap() {
        List<KeyPair<TypeKey, TypeKey>> list = sortedList();
        boolean renameSignatures = this.renameSignatures;
        boolean renameSource = this.renameSource;
        boolean noRenameSourceForNoPackageClass = this.noRenameSourceForNoPackageClass;

        int estimatedSize = 1;
        if(renameSignatures){
            estimatedSize = estimatedSize + 1;
        }
        if(renameSource){
            estimatedSize = estimatedSize + 1;
        }
        if(arrayDepth > 0){
            estimatedSize = estimatedSize + arrayDepth + 1;
        }
        estimatedSize = list.size() * estimatedSize;

        Map<String, String> map = new HashMap<>(estimatedSize);

        int size = list.size();
        int arrayDepth = this.arrayDepth + 1;

        for(int i = 0; i < size; i++){

            KeyPair<TypeKey, TypeKey> keyPair = list.get(i);
            TypeKey first = keyPair.getFirst();
            TypeKey second = keyPair.getSecond();

            String name1 = first.getTypeName();
            String name2 = second.getTypeName();
            map.put(name1, name2);

            if(renameSignatures){
                name1 = name1.replace(';', '<');
                name2 = name2.replace(';', '<');
                map.put(name1, name2);
            }

            for(int j = 1; j < arrayDepth; j++){
                name1 = first.getArrayType(j);
                name2 = first.getArrayType(j);
                map.put(name1, name2);
                if(renameSignatures && j == 1){
                    name1 = name1.replace(';', '<');
                    name2 = name2.replace(';', '<');
                    map.put(name1, name2);
                }
            }
            if(renameSource){
                name1 = first.getTypeName();
                if(!noRenameSourceForNoPackageClass || name1.indexOf('/') > 0){
                    name1 = first.getSourceName();
                    name2 = second.getSourceName();
                    map.put(name1, name2);
                }
            }
        }
        return map;
    }

    public static final int DEFAULT_ARRAY_DEPTH = ObjectsUtil.of(3);
}
