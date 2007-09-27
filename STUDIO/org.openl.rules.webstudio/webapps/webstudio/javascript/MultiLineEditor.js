/**
 * Text editor.
 *
 * @author Andrey Naumenko
 */
var MultiLineEditor = Class.create();

MultiLineEditor.prototype = Object.extend(new BaseEditor(), {
  eventHandler : null,

  initialize: function(tableEditor, cell) {
    this.tableEditor = tableEditor;
    this.cell = cell;
    this.node = document.createElement("div");
    //this.node.setAttribute("type", "text");
    //this.node.style.border = "0px none";
    //this.node.style.height = (this.cell.offsetHeight - (Prototype.Browser.IE ? 6 : 4)) + "px";

    //this.node.style.fontFamily = this.cell.style.fontFamily;
    //this.node.style.fontSize = this.cell.style.fontSize;
    //this.node.style.fontStyle = this.cell.style.fontStyle;
    //this.node.style.fontWeight = this.cell.style.fontWeight;
    //this.node.style.textAlign = this.cell.align;

    //this.node.style.margin = "0px";
    //this.node.style.padding = "0px";
    //this.node.style.width = "100%";
    var ta = document.createElement("textarea");
    ta.cols = 30;
    ta.rows = 3;
    this.node.appendChild(ta);

    this.node.style.position = "absolute";
    this.node.style.left = this.getPosition(cell)[0] + "px";
    this.node.style.top = (this.getPosition(cell)[1] + 10) + "px";
    this.node.zIndex = "10";
    document.body.appendChild(this.node);
    ta.focus();

    //this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
    //Event.observe(this.node, "keyup", this.eventHandler);

    //Event.observe(this.node, "click", function(e) {(e || event).cancelBubble = true});
    //Event.observe(this.node, "mousedown", function(e) {(e || event).cancelBubble = true});
  },

  getPosition : function(node) {
    var left = 0;
    var top = 0;
    while (node.tagName != "BODY") {
      left += node.offsetLeft;
      top += node.offsetTop;
      node = node.offsetParent;
    }
    return new Array(left, top);
  },

  handleKeyPress: function (event) {
    switch (event.keyCode) {
      case 27: this.cancelEdit(); break;
      case 13: this.doneEdit(); break;
    }
  },

  getValue : function() {
    return this.node.firstChild.value;
  },

  setValue : function(/* String */ value) {
    if (this.node != null) {
      this.node.firstChild.value = value.strip();
    }
  },

  destroy: function() {
    document.body.removeChild(this.node);
    Event.stopObserving(this.node, "keyup", this.eventHandler);
  },

  detach: function() {
    this.editorContainer.innerHTML = this.node.value;
  }
});

TableEditor.Editors["multiline"] = MultiLineEditor;
