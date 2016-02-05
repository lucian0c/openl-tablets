package org.openl.rules.webstudio.web.trace.node;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.SpreadsheetInvoker;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.dt.ActionInvoker;
import org.openl.rules.dt.DecisionTableInvoker;
import org.openl.rules.method.table.MethodTableInvoker;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.tbasic.AlgorithmInvoker;
import org.openl.types.Invokable;

public class TracedObjectFactory {

    public static ATableTracerNode getTracedObject(Invokable method, Object[] params) {
        if (method instanceof AlgorithmInvoker) {
            return new TBasicAlgorithmTraceObject(((AlgorithmInvoker) method).getInvokableMethod(), params);
        } else if (method instanceof DecisionTableInvoker) {
            return new DecisionTableTraceObject(((DecisionTableInvoker) method).getInvokableMethod(), params);
        } else if (method instanceof MethodTableInvoker) {
            return new MethodTableTraceObject(((MethodTableInvoker) method).getInvokableMethod(), params);
        } else if (method instanceof SpreadsheetInvoker) {
            return new SpreadsheetTraceObject(((SpreadsheetInvoker) method).getInvokableMethod(), params);
        } else if (method instanceof SpreadsheetCell) {
            return new SpreadsheetTracerLeaf((SpreadsheetCell) method);
        } else if (method instanceof ActionInvoker) {
            return new DTRuleTracerLeaf(((ActionInvoker) method).getRule());
        }
        throw new OpenlNotCheckedException(String.format("Can`t create traced object for %s.", method));
    }
}
