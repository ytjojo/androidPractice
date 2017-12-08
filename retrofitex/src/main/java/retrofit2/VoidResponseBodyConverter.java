package retrofit2;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import okhttp3.ResponseBody;

public class VoidResponseBodyConverter implements Converter<ResponseBody, Void> {
    static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();
    public static Void DEFAULT_VOID_INSTIANCE;
    @Override
    public Void convert(ResponseBody value) throws IOException {
        value.close();
        if (DEFAULT_VOID_INSTIANCE == null) {
            Constructor<?>[] cons = Void.class.getDeclaredConstructors();
            cons[0].setAccessible(true);
            try {
                DEFAULT_VOID_INSTIANCE = (Void) cons[0].newInstance(new Object[]{});
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return DEFAULT_VOID_INSTIANCE;
    }
}