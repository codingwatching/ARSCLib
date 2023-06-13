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
package com.reandroid.arsc.chunk;

import com.reandroid.arsc.ApkFile;
import com.reandroid.arsc.BuildInfo;
import com.reandroid.arsc.array.PackageArray;
import com.reandroid.arsc.group.EntryGroup;
import com.reandroid.arsc.group.ResourceEntry;
import com.reandroid.arsc.header.HeaderBlock;
import com.reandroid.arsc.header.InfoHeader;
import com.reandroid.arsc.header.TableHeader;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.pool.TableStringPool;
import com.reandroid.arsc.util.*;
import com.reandroid.arsc.value.*;
import com.reandroid.common.EntryStore;
import com.reandroid.common.ReferenceResolver;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONObject;
import com.reandroid.xml.XMLUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

public class TableBlock extends Chunk<TableHeader>
        implements MainChunk, JSONConvert<JSONObject>, EntryStore {
    private final TableStringPool mTableStringPool;
    private final PackageArray mPackageArray;
    private final List<TableBlock> mFrameWorks;
    private ApkFile mApkFile;
    private ReferenceResolver referenceResolver;

    public TableBlock() {
        super(new TableHeader(), 2);
        TableHeader header = getHeaderBlock();
        this.mTableStringPool = new TableStringPool(true);
        this.mPackageArray = new PackageArray(header.getPackageCount());
        this.mFrameWorks = new ArrayList<>();
        addChild(mTableStringPool);
        addChild(mPackageArray);
    }

    public PackageBlock getPackageBlockByTag(Object tag){
        for(PackageBlock packageBlock : listPackages()){
            if(Objects.equals(tag, packageBlock.getTag())){
                return packageBlock;
            }
        }
        return null;
    }
    public Iterator<ResourceEntry> getResources(){
        return new IterableIterator<PackageBlock, ResourceEntry>(getPackages()) {
            @Override
            public Iterator<ResourceEntry> iterator(PackageBlock element) {
                return element.getResources();
            }
        };
    }
    public ResourceEntry getResource(int resourceId){
        if(resourceId == 0){
            return null;
        }
        Iterator<PackageBlock> iterator = getAllPackages((resourceId >> 24) & 0xff);
        int typeId = (resourceId >> 16) & 0xff;
        int entryId = resourceId & 0xffff;
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(typeId, entryId);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        int staged = resolveStagedAlias(resourceId, 0);
        if(staged == 0 || staged == resourceId){
            return null;
        }
        iterator = getAllPackages((staged >> 24) & 0xff);
        typeId = (staged >> 16) & 0xff;
        entryId = staged & 0xffff;
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(typeId, entryId);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        return null;
    }
    public ResourceEntry getResource(String packageName, String type, String name){
        Iterator<PackageBlock> iterator = getAllPackages(packageName);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(type, name);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        return null;
    }
    public int resolveResourceId(String packageName, String type, String name){
        Iterator<Entry> iterator = getEntries(packageName, type, name);
        if(iterator.hasNext()){
            return iterator.next().getResourceId();
        }
        return 0;
    }
    public Entry getEntry(String packageName, String type, String name){
        Iterator<PackageBlock> iterator = getAllPackages(packageName);
        Entry result = null;
        while (iterator.hasNext()){
            Entry entry = iterator.next().getEntry(type, name);
            if(entry == null){
                continue;
            }
            if(!entry.isNull()){
                return entry;
            }
            if(result == null){
                result = entry;
            }
        }
        return result;
    }
    public Iterator<Entry> getEntries(int resourceId){
        return getEntries(resourceId, true);
    }
    public Iterator<Entry> getEntries(int resourceId, boolean skipNull){

        final int packageId = (resourceId >> 24) & 0xff;
        final int typeId = (resourceId >> 16) & 0xff;
        final int entryId = resourceId & 0xffff;
        return new IterableIterator<PackageBlock, Entry>(getAllPackages(packageId)) {
            @Override
            public Iterator<Entry> iterator(PackageBlock element) {
                if(super.getCount() > 0){
                    super.stop();
                    return null;
                }
                return element.getEntries(typeId, entryId, skipNull);
            }
        };
    }
    public Iterator<Entry> getEntries(String packageName, String type, String name){
        return new IterableIterator<PackageBlock, Entry>(getAllPackages(packageName)) {
            @Override
            public Iterator<Entry> iterator(PackageBlock element) {
                if(super.getCount() > 0){
                    super.stop();
                    return null;
                }
                return element.getEntries(type, name);
            }
        };
    }
    public Iterator<PackageBlock> getPackages(String packageName){
        return new  FilterIterator<PackageBlock>(getPackages()) {
            @Override
            public boolean test(PackageBlock packageBlock){
                if(packageName != null){
                    return packageName.equals(packageBlock.getName());
                }
                return TableBlock.this == packageBlock.getTableBlock();
            }
        };
    }
    public Iterator<PackageBlock> getPackages(int packageId){
        if(packageId == 0){
            return EmptyIterator.of();
        }
        return new FilterIterator<PackageBlock>(getPackages()) {
            @Override
            public boolean test(PackageBlock packageBlock){
                return packageId == packageBlock.getId();
            }
        };
    }
    public Iterator<PackageBlock> getPackages(){
        return getPackageArray().iterator();
    }
    public Iterator<PackageBlock> getAllPackages(){
        return new CombiningIterator<>(getPackages(),
                new IterableIterator<TableBlock, PackageBlock>(getFrameWorks().iterator()) {
                    @Override
                    public Iterator<PackageBlock> iterator(TableBlock element) {
                        return element.getPackages();
                    }
                });
    }
    public Iterator<PackageBlock> getAllPackages(int packageId){
        return new  FilterIterator<PackageBlock>(getAllPackages()) {
            @Override
            public boolean test(PackageBlock packageBlock){
                return packageId == packageBlock.getId();
            }
        };
    }
    public Iterator<PackageBlock> getAllPackages(String packageName){
        return new  FilterIterator<PackageBlock>(getAllPackages()) {
            @Override
            public boolean test(PackageBlock packageBlock){
                if(packageName != null){
                    return packageName.equals(packageBlock.getName());
                }
                return TableBlock.this == packageBlock.getTableBlock();
            }
        };
    }
    public int removeUnusedSpecs(){
        int result = 0;
        for(PackageBlock packageBlock : listPackages()){
            result += packageBlock.removeUnusedSpecs();
        }
        return result;
    }
    public String refreshFull(){
        int sizeOld = getHeaderBlock().getChunkSize();
        StringBuilder message = new StringBuilder();
        boolean appendOnce = false;
        int count = getTableStringPool().removeUnusedStrings().size();
        if(count != 0){
            message.append("Removed unused table strings = ");
            message.append(count);
            appendOnce = true;
        }
        for(PackageBlock packageBlock : listPackages()){
            String packageMessage = packageBlock.refreshFull(false);
            if(packageMessage == null){
                continue;
            }
            if(appendOnce){
                message.append("\n");
            }
            message.append("Package: ");
            message.append(packageBlock.getName());
            message.append("\n  ");
            packageMessage = packageMessage.replaceAll("\n", "\n  ");
            message.append(packageMessage);
            appendOnce = true;
        }
        refresh();
        int sizeNew = getHeaderBlock().getChunkSize();
        if(sizeOld != sizeNew){
            if(appendOnce){
                message.append("\n");
            }
            message.append("Table size changed = ");
            message.append(sizeOld);
            message.append(", ");
            message.append(sizeNew);
            appendOnce = true;
        }
        if(appendOnce){
            return message.toString();
        }
        return null;
    }
    public void linkTableStringsInternal(TableStringPool tableStringPool){
        for(PackageBlock packageBlock : listPackages()){
            packageBlock.linkTableStringsInternal(tableStringPool);
        }
    }
    public List<Entry> resolveReference(int referenceId){
        return resolveReference(referenceId, null);
    }
    public List<Entry> resolveReferenceWithConfig(int referenceId, ResConfig resConfig){
        ReferenceResolver resolver = this.referenceResolver;
        if(resolver == null){
            resolver = new ReferenceResolver(this);
            this.referenceResolver = resolver;
        }
        return resolver.resolveWithConfig(referenceId, resConfig);
    }
    public List<Entry> resolveReference(int referenceId, Predicate<Entry> filter){
        ReferenceResolver resolver = this.referenceResolver;
        if(resolver == null){
            resolver = new ReferenceResolver(this);
            this.referenceResolver = resolver;
        }
        return resolver.resolveAll(referenceId, filter);
    }
    public void destroy(){
        getPackageArray().destroy();
        getStringPool().destroy();
        clearFrameworks();
        refresh();
    }
    public int countPackages(){
        return getPackageArray().childesCount();
    }

    public PackageBlock pickOne(){
        return getPackageArray().pickOne();
    }
    public PackageBlock pickOne(int packageId){
        return getPackageArray().pickOne(packageId);
    }
    public void sortPackages(){
        getPackageArray().sort();
    }
    public Collection<PackageBlock> listPackages(){
        return getPackageArray().listItems();
    }
    @Override
    public TableStringPool getStringPool() {
        return mTableStringPool;
    }
    @Override
    public ApkFile getApkFile(){
        return mApkFile;
    }
    @Override
    public void setApkFile(ApkFile apkFile){
        this.mApkFile = apkFile;
    }
    @Override
    public TableBlock getTableBlock() {
        return this;
    }

    public TableStringPool getTableStringPool(){
        return mTableStringPool;
    }
    public PackageBlock getPackageBlockById(int pkgId){
        return getPackageArray().getPackageBlockById(pkgId);
    }
    public PackageBlock newPackage(int id, String name){
        PackageBlock packageBlock = getPackageArray().createNext();
        packageBlock.setId(id);
        if(name != null){
            packageBlock.setName(name);
        }
        return packageBlock;
    }
    public PackageBlock getOrCreatePackage(int id, String name){
        PackageBlock packageBlockId = getPackageArray()
                .getPackageBlockById(id);
        if(packageBlockId == null){
            return newPackage(id, name);
        }
        if(name == null){
            return packageBlockId;
        }
        PackageBlock packageBlockName = getPackageArray()
                .getPackageBlockByName(name);
        if(packageBlockId == packageBlockName){
            return packageBlockId;
        }
        return newPackage(id, name);
    }
    public PackageArray getPackageArray(){
        return mPackageArray;
    }

    private void refreshPackageCount(){
        int count = getPackageArray().childesCount();
        getHeaderBlock().getPackageCount().set(count);
    }
    @Override
    protected void onChunkRefreshed() {
        refreshPackageCount();
    }
    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        TableHeader tableHeader = getHeaderBlock();
        tableHeader.readBytes(reader);
        if(tableHeader.getChunkType()!=ChunkType.TABLE){
            throw new IOException("Not resource table: "+tableHeader);
        }
        boolean stringPoolLoaded=false;
        InfoHeader infoHeader = reader.readHeaderBlock();
        PackageArray packageArray=mPackageArray;
        packageArray.clearChildes();
        while(infoHeader!=null && reader.isAvailable()){
            ChunkType chunkType=infoHeader.getChunkType();
            if(chunkType==ChunkType.STRING){
                if(!stringPoolLoaded){
                    mTableStringPool.readBytes(reader);
                    stringPoolLoaded=true;
                }
            }else if(chunkType==ChunkType.PACKAGE){
                PackageBlock packageBlock=packageArray.createNext();
                packageBlock.readBytes(reader);
            }else {
                UnknownChunk unknownChunk=new UnknownChunk();
                unknownChunk.readBytes(reader);
                addChild(unknownChunk);
            }
            infoHeader=reader.readHeaderBlock();
        }
        reader.close();
    }

    public void readBytes(File file) throws IOException{
        BlockReader reader=new BlockReader(file);
        super.readBytes(reader);
    }
    public void readBytes(InputStream inputStream) throws IOException{
        BlockReader reader=new BlockReader(inputStream);
        super.readBytes(reader);
    }
    public final int writeBytes(File file) throws IOException{
        if(isNull()){
            throw new IOException("Can NOT save null block");
        }
        File dir=file.getParentFile();
        if(dir!=null && !dir.exists()){
            dir.mkdirs();
        }
        OutputStream outputStream=new FileOutputStream(file);
        int length = super.writeBytes(outputStream);
        outputStream.close();
        return length;
    }

    public Entry getAnyEntry(int resourceId){
        if(resourceId == 0 || ((resourceId >> 16) & 0xff) == 0){
            return null;
        }
        Entry result = null;
        for(PackageBlock packageBlock : listPackages()){
            Entry entry = packageBlock.getAnyEntry(resourceId);
            if(entry == null){
                continue;
            }
            if(!entry.isNull()){
                return entry;
            }
            if(result == null){
                result = entry;
            }
        }
        if(result != null){
            return result;
        }
        for(TableBlock tableBlock : getFrameWorks()){
            Entry entry = tableBlock.getAnyEntry(resourceId);
            if(entry == null){
                continue;
            }
            if(!entry.isNull()){
                return entry;
            }
            if(result == null){
                result = entry;
            }
        }
        return result;
    }
    public EntryGroup search(int resourceId){
        if(resourceId==0){
            return null;
        }
        EntryGroup entryGroup = searchLocal(resourceId);
        if(entryGroup!=null){
            return entryGroup;
        }
        for(TableBlock tableBlock:getFrameWorks()){
            entryGroup = tableBlock.search(resourceId);
            if(entryGroup!=null){
                return entryGroup;
            }
        }
        return null;
    }
    private EntryGroup searchLocal(int resourceId){
        if(resourceId == 0){
            return null;
        }
        int aliasId = searchResourceIdAlias(resourceId);
        Iterator<PackageBlock> iterator = getPackages();
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            EntryGroup entryGroup = packageBlock.getEntryGroup(resourceId);
            if(entryGroup == null){
                entryGroup = packageBlock.getEntryGroup(aliasId);
            }
            if(entryGroup != null){
                return entryGroup;
            }
        }
        return null;
    }
    @Override
    public Collection<EntryGroup> getEntryGroups(int resourceId) {
        List<EntryGroup> results = new ArrayList<>();
        EntryGroup entryGroup = searchLocal(resourceId);
        if(entryGroup!=null){
            results.add(entryGroup);
        }
        for(TableBlock framework:getFrameWorks()){
            results.addAll(framework.getEntryGroups(resourceId));
        }
        return results;
    }
    @Override
    public EntryGroup getEntryGroup(int resourceId) {
        return search(resourceId);
    }
    @Override
    public Collection<PackageBlock> getPackageBlocks(int packageId) {
        List<PackageBlock> results=new ArrayList<>();
        PackageBlock packageBlock = getPackageBlockById(packageId);
        if(packageBlock!=null){
            results.add(packageBlock);
        }
        for(TableBlock tableBlock:getFrameWorks()){
            results.addAll(tableBlock.getPackageBlocks(packageId));
        }
        return results;
    }
    public int searchResourceIdAlias(int resourceId){
        return resolveStagedAlias(resourceId, 0);
    }
    public int resolveStagedAlias(int stagedResId, int def){
        StagedAliasEntry stagedAliasEntry = getStagedAlias(stagedResId);
        if(stagedAliasEntry != null){
            return stagedAliasEntry.getFinalizedResId();
        }
        return def;
    }
    public StagedAliasEntry getStagedAlias(int stagedResId){
        Iterator<PackageBlock> iterator = getAllPackages();
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            StagedAliasEntry stagedAliasEntry =
                    packageBlock.searchByStagedResId(stagedResId);
            if(stagedAliasEntry != null){
                return stagedAliasEntry;
            }
        }
        return null;
    }
    public List<TableBlock> getFrameWorks(){
        return mFrameWorks;
    }
    public boolean isAndroid(){
        PackageBlock packageBlock = pickOne();
        if(packageBlock == null){
            return false;
        }
        return "android".equals(packageBlock.getName())
                && packageBlock.getId() == 0x01;
    }
    public boolean hasFramework(){
        return getFrameWorks().size() != 0;
    }
    public void addFramework(TableBlock tableBlock){
        if(tableBlock==null||tableBlock==this){
            return;
        }
        for(TableBlock frm:tableBlock.getFrameWorks()){
            if(frm==this || frm==tableBlock || tableBlock.equals(frm)){
                return;
            }
        }
        mFrameWorks.add(tableBlock);
    }
    public void removeFramework(TableBlock tableBlock){
        mFrameWorks.remove(tableBlock);
    }
    public void clearFrameworks(){
        mFrameWorks.clear();
    }
    public PackageBlock parsePublicXml(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        int event = XMLUtil.findStartTag(parser);
        int id = PackageBlock.readPackageId(parser, 0);
        while (id == 0 && event == XmlPullParser.START_TAG){
            parser.nextToken();
            event = XMLUtil.findStartTag(parser);
            id = PackageBlock.readPackageId(parser, 0);
        }
        String name = PackageBlock.readPackageName(parser, null);

        PackageBlock packageBlock = getOrCreatePackage(id, name);
        packageBlock.parsePublicXml(parser);
        IOUtil.close(parser);
        return packageBlock;
    }
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject=new JSONObject();

        jsonObject.put(BuildInfo.NAME_arsc_lib_version, BuildInfo.getVersion());

        jsonObject.put(NAME_packages, getPackageArray().toJson());
        JSONArray jsonArray = getStringPool().toJson();
        if(jsonArray!=null){
            jsonObject.put(NAME_styled_strings, jsonArray);
        }
        return jsonObject;
    }
    @Override
    public void fromJson(JSONObject json) {
        getPackageArray().fromJson(json.getJSONArray(NAME_packages));
        refresh();
    }
    public void merge(TableBlock tableBlock){
        if(tableBlock==null||tableBlock==this){
            return;
        }
        if(countPackages()==0 && getStringPool().countStrings()==0){
            getStringPool().merge(tableBlock.getStringPool());
        }
        getPackageArray().merge(tableBlock.getPackageArray());
        refresh();
    }
    @Override
    public String toString(){
        StringBuilder builder=new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(": packages = ");
        builder.append(mPackageArray.childesCount());
        builder.append(", size = ");
        builder.append(getHeaderBlock().getChunkSize());
        builder.append(" bytes");
        return builder.toString();
    }

    @Deprecated
    public static TableBlock loadWithAndroidFramework(InputStream inputStream) throws IOException{
        return load(inputStream);
    }
    public static TableBlock load(File file) throws IOException{
        return load(new FileInputStream(file));
    }
    public static TableBlock load(InputStream inputStream) throws IOException{
        TableBlock tableBlock=new TableBlock();
        tableBlock.readBytes(inputStream);
        return tableBlock;
    }

    public static boolean isResTableBlock(File file){
        if(file==null){
            return false;
        }
        boolean result=false;
        try {
            InputStream inputStream=new FileInputStream(file);
            result=isResTableBlock(inputStream);
            inputStream.close();
        } catch (IOException ignored) {
        }
        return result;
    }
    public static boolean isResTableBlock(InputStream inputStream){
        try {
            HeaderBlock headerBlock= BlockReader.readHeaderBlock(inputStream);
            return isResTableBlock(headerBlock);
        } catch (IOException ignored) {
            return false;
        }
    }
    public static boolean isResTableBlock(BlockReader blockReader){
        if(blockReader==null){
            return false;
        }
        try {
            HeaderBlock headerBlock = blockReader.readHeaderBlock();
            return isResTableBlock(headerBlock);
        } catch (IOException ignored) {
            return false;
        }
    }
    public static boolean isResTableBlock(HeaderBlock headerBlock){
        if(headerBlock==null){
            return false;
        }
        ChunkType chunkType=headerBlock.getChunkType();
        return chunkType==ChunkType.TABLE;
    }
    public static final String FILE_NAME = "resources.arsc";
    public static final String FILE_NAME_JSON = "resources.arsc.json";

    private static final String NAME_packages = "packages";
    public static final String NAME_styled_strings = "styled_strings";

    public static final String JSON_FILE_NAME = "resources.arsc.json";
    public static final String DIRECTORY_NAME = "resources";

    public static final String RES_JSON_DIRECTORY_NAME = "res-json";
    public static final String RES_FILES_DIRECTORY_NAME = "res-files";
}
