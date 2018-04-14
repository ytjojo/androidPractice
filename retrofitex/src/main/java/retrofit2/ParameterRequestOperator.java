package retrofit2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import static retrofit2.Utils.checkNotNull;

/**
 * Created by Administrator on 2017/10/12 0012.
 */

public interface ParameterRequestOperator {
    MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    Charset UTF_8 = Charset.forName("UTF-8");
    void operate(@NonNull IRequestOperator requestOperator, @Nullable ArrayList<Annotation> annotations, @Nullable ArrayList<ExtendParameterHandler<?>> handlers,@NonNull Object... args) throws IOException;



}
