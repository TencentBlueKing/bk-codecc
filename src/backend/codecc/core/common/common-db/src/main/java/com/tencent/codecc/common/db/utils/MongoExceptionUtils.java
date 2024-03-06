package com.tencent.codecc.common.db.utils;

import com.mongodb.MongoException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;

public class MongoExceptionUtils {

    private static final int CODE_BSON_OBJECT_TOO_LARGE = 10334;


    public static boolean checkIsBSONObjectTooLargeException(UncategorizedMongoDbException e) {
        Integer code = getUncategorizedMongoDbExceptionCode(e);
        return code != null && code == CODE_BSON_OBJECT_TOO_LARGE;
    }

    private static Integer getUncategorizedMongoDbExceptionCode(UncategorizedMongoDbException e) {
        if (e == null) {
            return null;
        }
        Throwable rootCause = e.getRootCause();
        if (rootCause instanceof MongoException) {
            MongoException mongoException = (MongoException) rootCause;
            return mongoException.getCode();
        }
        return null;
    }

}
