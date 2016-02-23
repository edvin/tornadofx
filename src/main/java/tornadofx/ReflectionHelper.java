package tornadofx;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import kotlin.reflect.KFunction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionHelper {
	public static class CellValueFunctionRefCallback<S, T> implements Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>> {
		private final KFunction<ObjectProperty<T>> observableFn;
		private Method method;

		public CellValueFunctionRefCallback(KFunction<ObjectProperty<T>> observableFn) {
			this.observableFn = observableFn;
		}

		public ObservableValue<T> call(TableColumn.CellDataFeatures<S, T> param) {
			S item = param.getValue();
			if (item == null) return null;
			if (method == null) {
				try {
					method = item.getClass().getDeclaredMethod(observableFn.getName());
					if (!method.isAccessible()) method.setAccessible(true);
				} catch (Exception e) {
					throw new RuntimeException("Unable to extract observable function method");
				}
			}
			try {
				//noinspection unchecked
				return (ObservableValue<T>) method.invoke(item);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException("Unable to extract observable value");
			}
		}
	}
}
