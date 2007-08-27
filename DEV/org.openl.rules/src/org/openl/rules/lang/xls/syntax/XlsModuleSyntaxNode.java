/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.lang.xls.syntax;

import org.openl.IOpenSourceCodeModule;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.syntax.impl.NaryNode;

/**
 * @author snshor
 *
 */
public class XlsModuleSyntaxNode extends NaryNode implements ITableNodeTypes
{

	OpenlSyntaxNode openlNode;
	String allImportString;

  /**
   * @param type
   * @param nodes
   * @param module
   * @param allImportString 
   */
  public XlsModuleSyntaxNode(
  		TableSyntaxNode[] nodes,
    IOpenSourceCodeModule module,
	OpenlSyntaxNode openlNode, String allImportString)
  {
    super(XLS_MODULE, null, nodes, null, module);
    
    this.openlNode = openlNode;
    this.allImportString = allImportString;
  }


  /**
   * @return
   */
  public OpenlSyntaxNode getOpenlNode()
  {
    return openlNode;
  }
  
  public TableSyntaxNode[] getXlsTableSyntaxNodes()
  {
  	return (TableSyntaxNode[])nodes;
  }


	public String getAllImportString()
	{
		return allImportString;
	}
  
  

}
