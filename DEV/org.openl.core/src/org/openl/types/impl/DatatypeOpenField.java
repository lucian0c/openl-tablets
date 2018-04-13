package org.openl.types.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openl.types.IOpenClass;
import org.openl.util.StringTool;
import org.openl.vm.IRuntimeEnv;

/**
 * Open field for datatypes. Work with generated simple beans.
 *
 * @author DLiauchuk
 */
public class DatatypeOpenField extends AOpenField {

    private IOpenClass declaringClass;
    private String getterMethodName;
    private String setterMethodName;

    public DatatypeOpenField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(name, type);
        this.getterMethodName = StringTool.getGetterName(getName());
        this.setterMethodName = StringTool.getSetterName(getName());
        this.declaringClass = declaringClass;
    }

    public DatatypeOpenField(String name, IOpenClass type) {
        super(name, type);
    }

    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return null;
        }

        Object res = null;
        Class<?> targetClass = target.getClass();
        Method method;
        try {
            method = targetClass.getMethod(getterMethodName);
            res = method.invoke(target);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return res != null ? res : getType().nullObject();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public boolean isWritable() {
        // TODO check final attribute
        return true;
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        Class<?> targetClass = target.getClass();
        Method method;
        try {
            method = targetClass.getMethod(setterMethodName, getType().getInstanceClass());
            method.invoke(target, value);
        } catch (NoSuchMethodException e) {
            String errorMessage = String.format("There is no setter method in class %s for the field %s with type %s",
                targetClass.getSimpleName(),
                getName(),
                getType().getInstanceClass().getSimpleName());
            throw new RuntimeException(errorMessage, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDeclaringClass(IOpenClass declaringClass) {
        this.declaringClass = declaringClass;
    }

}
