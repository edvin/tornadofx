package tornadofx.skin.tablerow;

import javafx.scene.Node;
import javafx.scene.control.TableRow;
import javafx.scene.control.skin.TableRowSkin;
import tornadofx.ExpanderColumn;

// This needs to be a Java class because of a Kotlin Bug: 
// https://youtrack.jetbrains.com/issue/KT-12255

public final class ExpandableTableRowSkin<S> extends TableRowSkin<S> {

    private double tableRowPrefHeight = -1.0D;

    private final TableRow<S> tableRow;
    private final ExpanderColumn<S> expander;

    public ExpandableTableRowSkin(TableRow<S> tableRow, ExpanderColumn<S> expander) {
        super(tableRow);
        this.tableRow = tableRow;
        this.expander = expander;
        this.tableRow.itemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                Node expandedNode = expander.getExpandedNode(oldValue);
                if (expandedNode != null) {
                    getChildren().remove(expandedNode);
                }
            }
        });
    }

    private boolean getExpanded() {
        TableRow<S> tableRow = getSkinnable();
        final S item = tableRow.getItem();
        return (item != null && expander.getCellData(getSkinnable().getIndex()));
    }

    private Node getContent() {
        Node node = expander.getOrCreateExpandedNode(tableRow);
        if (!getChildren().contains(node)) {
            getChildren().add(node);
        }
        return node;
    }

    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        tableRowPrefHeight = super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
        return (getExpanded() && getContent() != null) ? tableRowPrefHeight + getContent().prefHeight(width) : tableRowPrefHeight;
    }

    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        if (getExpanded() && getContent() != null) {
            getContent().resizeRelocate(0.0, tableRowPrefHeight, w, h - tableRowPrefHeight);
        }
    }

}