/**
 * Table editor.
 *
 * @requires prototype JavaScript library
 *
 * @author Andrey Naumenko
 */

var TableEditor = Class.create();

TableEditor.Editors = $H();

TableEditor.Constants = {
  ADD_BEFORE : 1,
  ADD_AFTER : 2,
  MOVE : 3,
  REMOVE : 4
};

TableEditor.prototype = {
  tableContainer : null,
  currentElement : null,
  editor : null,
  saveUrl : null,
  baseUrl : null,
  selectionPos : null,
  selectionHistory : [],
  decorator : null,
  edittedCellValue: null,
  rows : 0,
  columns : 0,
  table : null,

/** Constructor */
  initialize : function(id, url, tableid) {
    this.tableid = tableid;
    this.tableContainer = $(document.getElementById(id));
    this.baseUrl = url;
    this.saveUrl = url + "save";
    this.loadData(url + "load?elementID=" + tableid);

    var self = this;
    $(document.body).observe("click", function(e) {
      self.editStop()
    }, false);
    Event.observe(Prototype.Browser.IE ? document.body : window, "keydown", function(e) {
      self.handleKeyDown(e)
    }, false);

    this.tableContainer.observe("click", function(e) {
      self.handleClick(e)
    });
    this.tableContainer.observe("dblclick", function(e) {
      self.handleDoubleClick(e);
      Event.stop(e)
    });
  },

/**
 * @desc: load data from specific url
 * @type: private
 */
  loadData : function(url) {
    var self = this;
    new Ajax.Request(url, {
      method      : "get",
      encoding    : "utf-8",
      contentType : "text/javascript",
      onSuccess   : function(data) {
        self.renderTable(data);
      }
    });
  },

/**
 * @desc: renders table
 * @type: private
 */
  renderTable : function(data) {
    this.decorator = new Decorator();
    if (this.table) {
      Event.stopObserving(this.table, "click", this.tableEventHandlers.click);
      Event.stopObserving(this.table, "dblclick", this.tableEventHandlers.dblclick);
    }

    this.tableContainer.innerHTML = data.responseText;
    this.table = $(Prototype.Browser.IE ? this.tableContainer.childNodes[0] : this.tableContainer.childNodes[1]);

    this.computeTableInfo(this.table);
  },

/**
 * @desc: computes table width in rows, and height in columns (that is sum of all rowSpans in a column
 * and sum of all colSpans in a row).
 * @type: private
 */
  computeTableInfo: function(table) {
    this.rows = 0;
    this.columns = 0;

    var row = table.down("tr");
    if (row) {
      var tdElt = row.down("td");
      while (tdElt) {
        this.columns += tdElt.colSpan ? tdElt.colSpan : 1;
        tdElt = tdElt.next("td");
      }
    }

    while (row) {
      var tdElt = row.down("td")
      this.rows += tdElt.rowSpan ? tdElt.rowSpan : 1;
      row = row.next("tr");
    }
  },

/**
 * @desc: handles mouse double click on table
 * @type: private
 */
  handleDoubleClick: function(e) {
    var cell = Prototype.Browser.IE ? window.event.srcElement : e.target;

    // Save value of current editor and close it
    this.editStop();
    this.editBeginRequest(cell);
  },


  editBeginRequest : function(cell) {
    var self = this;
    this.selectElement(cell);
    new Ajax.Request(this.baseUrl + "getCellType", {
      onSuccess  : function(response) {
        self.editBegin(cell, response.responseText.strip())
      },
      parameters : {
        row : self.selectionPos[0],
        col : self.selectionPos[1],
        elementID : self.tableid
      }
    });
  },

/**
 *  @desc: Create and activate new editor
 */
  editBegin : function(cell, editor) {
    this.editor = new TableEditor.Editors[editor](this, cell);

    this.editor.setValue(cell.innerHTML);

    this.selectElement(cell);

    this.edittedCellValue = cell.innerHTML;
    cell.innerHTML = "";
    cell.appendChild(this.editor.node);
    this.editor.node.focus();
  },

  editStop : function() {
    if (this.editor != null) {
      if (!this.editor.isCancelled()) {
        new Ajax.Request(this.saveUrl + '?id=' + this.currentElement.title + '&value=' + this.editor.getValue(), {
          method    : "get",
          encoding   : "utf-8",
          contentType : "text/javascript",
          onSuccess  : function(data) {
            //alert('saved !');
          }
        });
        var value = this.editor.getValue();
        this.currentElement.innerHTML = value == "" ? "&nbsp;" : value;
      } else {
        this.currentElement.innerHTML = this.edittedCellValue;
      }

      this.editor.destroy();
    }
    this.editor = null;
  },

/**
 * @desc: handles mouse click on the table
 * @type: private
 */
  handleClick: function(e) {
    var elt = Event.element(e);
    if (elt.tagName == "TD") {
      this.editStop();
      this.selectElement(elt);
    }
    Event.stop(e);
  },

  selectElement: function(elt, dir) {
    if (elt && this.currentElement && elt.id == this.currentElement.id) return;
    if (elt && dir) { // save to selection history
      if (this.selectionPos) this.selectionHistory.push([dir, this.selectionPos[0], this.selectionPos[1]]);
      if (this.selectionHistory.length > 10) this.selectionHistory.shift();
    } else {
      if (dir == -1) {
        var lastEntry = this.selectionHistory.pop();
        this.selectionPos[0] = lastEntry[1];
        this.selectionPos[1] = lastEntry[2];
      } else
        this.selectionHistory.clear();
    }

    if (elt) {
      this.selectionPos = this.elementPosition(elt);
    } else {
      elt = this.$cell(this.selectionPos);
    }
    this.decorator.undecorate(this.currentElement);
    this.decorator.decorate(this.currentElement = elt);
  },

  $cell: function(pos) {
    var cell = $("cell-" + pos[0] + ":" + pos[1]);
    if (!cell) return cell;
    if (!cell.rowSpan) cell.rowSpan = 1;
    if (!cell.colSpan) cell.colSpan = 1;
    return cell
  },

/**
 * @desc: handles key presses. Performs table navigation.
 * @type: private
 */
  handleKeyDown: function(event) {
    if (this.editor) return; // do nothing in editor mode

    if (!this.selectionPos || !this.currentElement) {
      this.selectionPos = [1, 1];
      this.selectElement();
      return;
    }

    var sp = this.selectionPos.clone();

    // check history
    if (this.selectionHistory.length > 0 && this.selectionHistory.last()[0] == event.keyCode) {
      this.selectElement(null, -1);
      return;
    }

    var scanUpLeft = function(index, noRestore) {
      var tmp = sp[index];
      while (sp[index] >= 1 && !this.$cell(sp)) --sp[index];
      var res = this.$cell(sp);
      if (!noRestore) sp[index] = tmp;
      return res;
    }

    switch (event.keyCode) {
      case 37: case 38: // LEFT, UP
      var cell = null;
      var theIndex = event.keyCode == 38 ? 0 : 1;
      while (--sp[theIndex] >= 1) {
        cell = scanUpLeft.call(this, 1 - theIndex, true);
        if (cell) {
          if ((sp[0] + cell.rowSpan >= this.selectionPos[0] + theIndex) &&
              (sp[1] + cell.colSpan >= this.selectionPos[1] + 1 - theIndex))
            break;
        }
        sp[1 - theIndex] = this.selectionPos[1 - theIndex];
      }
      if (cell) this.selectElement(cell, event.keyCode + 2);
      break;

      case 39: case 40:  //RIGHT, DOWN
      var theIndex = event.keyCode == 40 ? 0 : 1;

      sp[theIndex] += this.currentElement[["rowSpan", "colSpan"][theIndex]];
      if (sp[theIndex] > this[["rows", "columns"][theIndex]]) break;
      var newCell = scanUpLeft.call(this, 1 - theIndex);
      if (newCell) this.selectElement(newCell, event.keyCode - 2);
      break;
    }
  },

/**
 * @desc: inspect element id and extracts its position in table. Element is expected to be a TD
 * @type: private
 */
  elementPosition: function(e) {
    var id = $(e).id;
    var pos = id.lastIndexOf("-");
    if (pos < 0) return null;
    var splitted = id.substr(pos + 1).split(":", 2);
    splitted[0] = parseInt(splitted[0]);
    splitted[1] = parseInt(splitted[1]);
    return splitted;
  },

  doRowOperation: function(op) {
    this.doColRowOperation(0, op)
  },
  doColOperation: function(op) {
    this.doColRowOperation(1, op)
  },

/**
 *  @type: private
 */
  doColRowOperation: function(index, op) {
    if (!this.selectionPos || !this.currentElement) {
      alert("Nothing is selected");
      return;
    }

    var params = {elementID : this.tableid}
    params[["row", "col"][index]] = (op == TableEditor.Constants.ADD_AFTER ? 1 : 0) + this.selectionPos[index];
    if (op == TableEditor.Constants.MOVE) params.move = true;

    var self = this;
    new Ajax.Request(this.baseUrl + (op == TableEditor.Constants.REMOVE || op == TableEditor.Constants.MOVE ? "removeRowCol" : "addRowColBefore"), {
      onSuccess : function(response) {
        self.renderTable(response);
        self.selectElement();
      },
      parameters : params
    });
  }
}

/**
 *  Responsible for visual display of 'selected' element.
 */
var Decorator = Class.create();

Decorator.prototype = {
/** Holds changed properties of last decorated  element */
  previosState : {},

/** Empty constructor */
  initialize : Prototype.K,

/**
 * @desc changes elememnt style, so it looks 'selected'
 * @type: public
 */
  decorate: function(/* Element */ elt) {
    if (!elt) return;
    this.previosState = {
      color: elt.style.color,
      backgroundColor: elt.style.backgroundColor
    }

    elt.style.color = "white"
    elt.style.backgroundColor = "blue"
  },

/**
 * @desc reverts 'selection' of last decorated element
 * @type: public
 */
  undecorate: function(/* Element */ elt) {
    if (elt) {
      Object.extend(elt.style, this.previosState)
    }
  }
}
