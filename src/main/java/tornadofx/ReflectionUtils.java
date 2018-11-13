package tornadofx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionUtils {

    public static void callMethod(Object object, String methodName, Object... params) {
        Class<?> clazz = object.getClass();
        callMethod(object, clazz, methodName, params);
    }

    public static void callMethod(Object object, Class<?> clazz, String methodName, Object... params) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, Arrays.stream(params).map(Object::getClass).toArray(Class[]::new));
            method.setAccessible(true);
            method.invoke(object, params);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Cannot call method " + methodName + " on " + object.getClass());
        }
    }
}
