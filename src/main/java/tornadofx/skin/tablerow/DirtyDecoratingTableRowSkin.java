package tornadofx.skin.tablerow;

import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.skin.TableRowSkin;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import tornadofx.TableViewEditModel;
import java.util.ArrayList;
import java.util.List;

// This needs to be a Java class because of a Kotlin Bug: 
// https://youtrack.jetbrains.com/issue/KT-12255

public final class DirtyDecoratingTableRowSkin<T> extends TableRowSkin<T> {

    private static final String KEY = "tornadofx.dirtyStatePolygon";
    private final TableViewEditModel<T> editModel;

    public DirtyDecoratingTableRowSkin(TableRow<T> tableRow, TableViewEditModel<T> editModel) {
        super(tableRow);
        this.editModel = editModel;
    }

    private Polygon getPolygon(TableCell<?, ?> cell) {
        ObservableMap<Object, Object> properties = cell.getProperties();
        return (Polygon) properties.computeIfAbsent(KEY, x -> {
            Polygon polygon = new Polygon(0.0, 0.0, 0.0, 10.0, 10.0, 0.0);
            polygon.setFill(Color.BLUE);
            return polygon;
        });
    }

    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        final var children = getChildren();
        final var sizeMaxRowElement = children.size();
        List<Node> addedChild = null;
        List<Node> removedChild = null;

        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            if (!(child instanceof TableCell)) continue;
            @SuppressWarnings("unchecked")
            TableCell<T, ?> cell = (TableCell<T, ?>) child;
            T item = null;
            if (cell.getIndex() > -1 && cell.getTableView().getItems().size() > cell.getIndex()) {
                item = cell.getTableView().getItems().get(cell.getIndex());
            }
            Polygon polygon = getPolygon(cell);
            boolean isDirty = item != null && editModel.getDirtyState(item).isDirtyColumn(cell.getTableColumn());
            if (isDirty) {
                if (!children.contains(polygon)) {
                    if (addedChild == null) addedChild = new ArrayList<>(sizeMaxRowElement);
                    addedChild.add(polygon);
                }
                polygon.relocate(cell.getLayoutX(), y);
            } else {
                if (removedChild == null) removedChild = new ArrayList<>(sizeMaxRowElement);
                removedChild.add(polygon);
            }
        }


        if (addedChild != null) {
            children.addAll(addedChild);
        }
        if (removedChild != null) {
            children.removeAll(removedChild);
        }
    }
}

