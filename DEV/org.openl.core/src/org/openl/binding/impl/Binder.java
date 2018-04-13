/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.*;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;

/**
 * Default implementation of {@link IOpenBinder}.
 * 
 * @author snshor
 */
public class Binder implements IOpenBinder {

    Map<MethodKey, Object> methodCache = new HashMap<>();
    private OpenL openl;
    private INodeBinderFactory nodeBinderFactory;
    private ICastFactory castFactory;
    private INameSpacedVarFactory varFactory;
    private INameSpacedTypeFactory typeFactory;
    private INameSpacedMethodFactory methodFactory;

    public Binder(INodeBinderFactory nodeBinderFactory,
            INameSpacedMethodFactory methodFactory,
            ICastFactory castFactory,
            INameSpacedVarFactory varFactory,
            INameSpacedTypeFactory typeFactory,
            OpenL openl) {

        this.nodeBinderFactory = nodeBinderFactory;
        this.methodFactory = methodFactory;
        this.castFactory = castFactory;
        this.varFactory = varFactory;
        this.typeFactory = typeFactory;
        this.openl = openl;
    }

    public ICastFactory getCastFactory() {
        return castFactory;
    }

    public INameSpacedMethodFactory getMethodFactory() {
        return methodFactory;
    }

    public INodeBinderFactory getNodeBinderFactory() {
        return nodeBinderFactory;
    }

    public INameSpacedTypeFactory getTypeFactory() {
        return typeFactory;
    }

    public INameSpacedVarFactory getVarFactory() {
        return varFactory;
    }

    public IBindingContext makeBindingContext() {
        return new BindingContext(this, JavaOpenClass.VOID, openl);
    }

    public IBoundCode bind(IParsedCode parsedCode) {
        return bind(parsedCode, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
     */
    public IBoundCode bind(IParsedCode parsedCode, IBindingContextDelegator delegator) {

        IBindingContext bindingContext = makeBindingContext();
        bindingContext = BindHelper.delegateContext(bindingContext, delegator);

        ISyntaxNode syntaxNode = parsedCode.getTopNode();

        bindingContext.pushLocalVarContext();
        IBoundNode boundNode = ANodeBinder.bindChildNode(syntaxNode, bindingContext);
        bindingContext.popLocalVarContext();

        return new BoundCode(parsedCode, boundNode, bindingContext.getErrors(), bindingContext.getOpenLMessages(), bindingContext.getLocalVarFrameSize());
    }

}
