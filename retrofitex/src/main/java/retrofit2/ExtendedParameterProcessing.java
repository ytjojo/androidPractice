package retrofit2;

import com.ytjojo.http.RetrofitClient;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;

import okhttp3.MediaType;
import retrofit2.http.BodyJsonAttr;

/**
 * Created by Administrator on 2016/10/27 0027.
 */
public class ExtendedParameterProcessing {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static BodyJsonAttrRequestOpertor sbodyJsonAttrRequestOpertor = new BodyJsonAttrRequestOpertor();
    public static void extendedprocessing(IRequestOperator operator, ArrayList<Annotation> annotations, ArrayList<ExtendParameterHandler<?>> handlers, Object... args) throws IOException {
        if(handlers != null && handlers.get(0).getAnnotation() instanceof BodyJsonAttr){
            sbodyJsonAttrRequestOpertor.operate(operator,annotations,handlers,args);
        }else{
            ParameterRequestOperator handler = RetrofitClient.getMergeParameterHandler();
            if(handler !=null){
                handler.operate(operator,annotations,handlers,args);
            }
        }

    }


}
