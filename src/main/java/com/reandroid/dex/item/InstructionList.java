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
package com.reandroid.dex.item;

import com.reandroid.arsc.io.BlockReader;
import com.reandroid.dex.base.DexBlockList;
import com.reandroid.arsc.item.IntegerVisitor;
import com.reandroid.arsc.item.VisitableInteger;
import com.reandroid.dex.debug.DebugElement;
import com.reandroid.dex.ins.*;
import com.reandroid.dex.writer.SmaliFormat;
import com.reandroid.dex.writer.SmaliWriter;
import com.reandroid.utils.collection.CollectionUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

public class InstructionList extends DexBlockList<Ins> implements SmaliFormat, VisitableInteger {
    private final CodeItem codeItem;
    private final RegisterFactory registerFactory;

    public InstructionList(CodeItem codeItem){
        super();
        this.codeItem = codeItem;
        this.registerFactory = new RegisterFactory(codeItem);
    }

    public MethodDef getMethodDef() {
        return getCodeItem().getMethodDef();
    }
    public CodeItem getCodeItem() {
        return codeItem;
    }
    @Override
    public void add(int index, Ins item) {
        reBuildExtraLines();
        super.add(index, item);
        updateAddresses();
        updateLabelAddress();
        reBuildExtraLines();
    }
    @Override
    public boolean remove(Ins item) {
        if(!contains(item)){
            return false;
        }
        reBuildExtraLines();
        if(item.hasExtraLines()){
            Ins next = get(item.getIndex() + 1);
            if(next != null) {
                item.transferExtraLines(next);
            }
        }
        super.remove(item);
        updateAddresses();
        updateLabelAddress();
        return true;
    }
    public void replace(Ins old, Ins item) {
        if(old == item){
            return;
        }
        reBuildExtraLines();
        int index = old.getIndex();
        old.transferExtraLines(item);
        item.setAddress(old.getAddress());
        super.remove(old);
        super.add(index, item);
        updateAddresses();
        updateLabelAddress();
    }
    private void updateLabelAddress() {
        for(Ins ins : this) {
            ins.updateLabelAddress();
        }
    }
    private void updateAddresses() {
        int address = 0;
        for(Ins ins : this) {
            ins.setAddress(address);
            address += ins.getCodeUnits();
        }
    }

    @Override
    public void visitIntegers(IntegerVisitor visitor) {
        List<Ins> insList = CollectionUtil.toList(this.iterator());
        for(Ins ins : insList) {
            if(ins instanceof VisitableInteger){
                ((VisitableInteger)ins).visitIntegers(visitor);
            }
        }
    }

    public RegisterFactory getRegisterFactory() {
        return registerFactory;
    }

    @Override
    protected void onRefreshed() {
        super.onRefreshed();
        this.codeItem.getInstructionCodeUnitsReference().set(getCodeUnits());
    }
    public int getCodeUnits() {
        int result = 0;
        for (Ins ins : this) {
            result += ins.getCodeUnits();
        }
        return result;
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        buildExtraLines();
        for (Ins ins : this) {
            writer.newLine();
            ins.append(writer);
        }
    }
    @Override
    public void onReadBytes(BlockReader reader) throws IOException {

        int position = reader.getPosition() +
                codeItem.getInstructionCodeUnitsReference().get() * 2;

        int zeroPosition = reader.getPosition();

        while (reader.getPosition() < position){
            Opcode<?> opcode = Opcode.read(reader);
            Ins ins = opcode.newInstance();
            ins.setAddress((reader.getPosition() - zeroPosition) / 2);
            add(ins);
            ins.readBytes(reader);
        }
        if(position != reader.getPosition()){
            // should not reach here
            reader.seek(position);
        }
        getDexPositionAlign().readBytes(reader);
    }

    private void clearExtraLines() {
        for (Ins ins : this) {
            ins.clearExtraLines();
        }
    }
    private boolean haveExtraLines() {
        for (Ins ins : this) {
            if(ins.hasExtraLines()){
                return true;
            }
        }
        return false;
    }
    public void buildExtraLines(){
        if(haveExtraLines()){
            return;
        }
        buildLabels();
        buildTryBlock();
        buildDebugInfo();
    }
    public void reBuildExtraLines(){
        clearExtraLines();
        buildExtraLines();
    }
    private Ins getAtAddress(int address){
        for (Ins ins : this) {
            if(ins.getAddress() == address){
                return ins;
            }
        }
        return null;
    }
    private void addLabel(Label label){
        Ins target = getAtAddress(label.getTargetAddress());
        if(target != null){
            target.addExtraLine(label);
        }
    }
    private void addLabels(Iterator<? extends Label> iterator){
        while (iterator.hasNext()){
            addLabel(iterator.next());
        }
    }
    private void buildLabels(){
        for(Ins ins : this){
            if(ins instanceof Label){
                addLabel((Label) ins);
            }else if(ins instanceof LabelList){
                addLabels(((LabelList) ins).getLabels());
            }
            ins.sortExtraLines();
        }
    }
    private void buildTryBlock(){
        TryBlock tryBlock = this.codeItem.getTryBlock();
        if(tryBlock == null || tryBlock.isNull()){
            return;
        }
        addLabels(tryBlock.getLabels());
    }
    private void buildDebugInfo(){
        DebugInfo debugInfo = codeItem.getDebugInfo();
        if(debugInfo == null){
            return;
        }
        Iterator<DebugElement> iterator = debugInfo.getExtraLines();
        while (iterator.hasNext()){
            DebugElement element = iterator.next();
            Ins target = getAtAddress(element.getAddress());
            if(target != null){
                target.addExtraLine(element);
            }
        }
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        SmaliWriter smaliWriter = new SmaliWriter(writer);
        smaliWriter.indentPlus();
        try {
            append(smaliWriter);
            smaliWriter.close();
        } catch (IOException exception) {
            return exception.toString();
        }
        return writer.toString();
    }
}
