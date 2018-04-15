package retrofit2;

import com.ytjojo.http.RetrofitClient;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;

import okhttp3.MediaType;
import retrofit2.http.PostJsonAttr;

/**
 * Created by Administrator on 2016/10/27 0027.
 */
public class ExtendedParameterProcessing {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static BodyJsonAttrRequestOpertor sbodyJsonAttrRequestOpertor = new BodyJsonAttrRequestOpertor();
    public static void extendedprocessing(IRequestOperator operator, ArrayList<Annotation> annotations, ArrayList<ExtendParameterHandler<?>> handlers, Object... args) throws IOException {

        if(handlers != null && !handlers.isEmpty() && handlers.get(0).getAnnotation() instanceof PostJsonAttr){
            sbodyJsonAttrRequestOpertor.operate(operator,annotations,handlers,args);
        }
        ParameterRequestOperator Operator = RetrofitClient.getMergeParameterHandler();
        if(Operator !=null){
            Operator.operate(operator,annotations,handlers,args);
        }

    }


}
