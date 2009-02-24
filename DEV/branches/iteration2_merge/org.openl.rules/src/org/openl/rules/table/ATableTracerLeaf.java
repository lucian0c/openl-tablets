package org.openl.rules.table;

import org.openl.vm.ITracerObject;

public abstract class ATableTracerLeaf extends ITracerObject.SimpleTracerObject implements ITableTracerObject {
    public ATableTracerLeaf(){
        super();
    }
    
    public ATableTracerLeaf(Object tracedObject){
        super(tracedObject);
    }
    
    public ITableTracerObject[] getTableTracers() {
        ITracerObject[] tracerObjects = getTracerObjects();

        int size = tracerObjects.length;
        ITableTracerObject[] temp = new ITableTracerObject[size];

        System.arraycopy(tracerObjects, 0, temp, 0, size);

        return temp;
    }
}
