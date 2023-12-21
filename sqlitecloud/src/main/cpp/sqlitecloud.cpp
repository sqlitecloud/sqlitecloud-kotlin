#include <jni.h>
#include <string>

#include "sqcloud.h"

SQCloudConnection *getConnection(JNIEnv *env, jobject thiz) {
    auto thisClass = env->GetObjectClass(thiz);
    auto connectionFieldID = env->GetFieldID(thisClass, "connection", "Ljava/nio/ByteBuffer;");
    auto connectionBuffer = env->GetObjectField(thiz, connectionFieldID);
    return static_cast<SQCloudConnection *>(env->GetDirectBufferAddress(connectionBuffer));
}

const char *cString(JNIEnv *env, jstring string) {
    if (!string) {
        return nullptr;
    }
    return env->GetStringUTFChars(string, nullptr);
}

jobject wrapPointer(JNIEnv *env, void *pointer) {
    if (!pointer) {
        return nullptr;
    }
    return env->NewDirectByteBuffer(pointer, sizeof(void *));
}

void *unwrapPointer(JNIEnv *env, jobject wrappedPointer) {
    return env->GetDirectBufferAddress(wrappedPointer);
}

SQCloudResult *unwrapResult(JNIEnv *env, jobject wrappedResult) {
    return static_cast<SQCloudResult *>(unwrapPointer(env, wrappedResult));
}

SQCloudBlob *unwrapBlob(JNIEnv *env, jobject wrappedBlob) {
    return static_cast<SQCloudBlob *>(unwrapPointer(env, wrappedBlob));
}

SQCloudVM *unwrapVM(JNIEnv *env, jobject wrappedVM) {
    return static_cast<SQCloudVM *>(unwrapPointer(env, wrappedVM));
}

struct PubSubData {
    JNIEnv *env;
    jobject thiz;
};

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_doConnect(
        JNIEnv *env,
        jobject thiz,
        jstring hostname,
        jint port,
        jstring username,
        jstring password,
        jstring database,
        jint timeout,
        jint family,
        jboolean compression,
        jboolean sqlite_mode,
        jboolean zero_text,
        jboolean password_hashed,
        jboolean nonlinearizable,
        jboolean db_memory,
        jboolean no_blob,
        jboolean db_create,
        jint max_data,
        jint max_rows,
        jint max_rowset,
        jstring tls_root_certificate,
        jstring tls_certificate,
        jstring tls_certificate_key,
        jboolean insecure
        // TODO: config_cb callback
) {
    SQCloudConfig config = {
            .username = cString(env, username),
            .password = cString(env, password),
            .database = database ? cString(env, database) : nullptr,
            .timeout = timeout,
            .family = family,
            .compression = static_cast<bool>(compression),
            .sqlite_mode = static_cast<bool>(sqlite_mode),
            .zero_text = static_cast<bool>(zero_text),
            .password_hashed = static_cast<bool>(password_hashed),
            .nonlinearizable = static_cast<bool>(nonlinearizable),
            .db_memory = static_cast<bool>(db_memory),
            .no_blob = static_cast<bool>(no_blob),
            .db_create = static_cast<bool>(db_create),
            .max_data = max_data,
            .max_rows = max_rows,
            .max_rowset = max_rowset,
            .tls_root_certificate = tls_root_certificate ? cString(env, tls_root_certificate)
                                                         : nullptr,
            .tls_certificate = tls_certificate ? cString(env, tls_certificate) : nullptr,
            .tls_certificate_key = tls_certificate_key ? cString(env, tls_certificate_key)
                                                       : nullptr,
            .insecure = static_cast<bool>(insecure),
    };

    auto connection = SQCloudConnect(cString(env, hostname), port, &config);
    return wrapPointer(env, connection);
}

extern "C" JNIEXPORT void JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_doDisconnect(JNIEnv *env, jobject thiz) {
    SQCloudDisconnect(getConnection(env, thiz));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_isError(JNIEnv *env, jobject thiz) {
    return SQCloudIsError(getConnection(env, thiz));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_isSQLiteError(JNIEnv *env, jobject thiz) {
    return SQCloudIsSQLiteError(getConnection(env, thiz));
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_errorCode(JNIEnv *env, jobject thiz) {
    auto connection = getConnection(env, thiz);
    if (!SQCloudIsError(connection)) {
        return nullptr;
    }
    return env->NewObject(
            env->FindClass("java/lang/Integer"),
            env->GetMethodID(env->FindClass("java/lang/Integer"), "<init>", "(I)V"),
            SQCloudErrorCode(connection)
    );
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_errorMessage(JNIEnv *env, jobject thiz) {
    auto connection = getConnection(env, thiz);
    if (!SQCloudIsError(connection)) {
        return nullptr;
    }
    return env->NewStringUTF(SQCloudErrorMsg(connection));
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_extendedErrorCode(JNIEnv *env, jobject thiz) {
    auto connection = getConnection(env, thiz);
    if (!SQCloudIsError(connection)) {
        return nullptr;
    }
    return env->NewObject(
            env->FindClass("java/lang/Integer"),
            env->GetMethodID(env->FindClass("java/lang/Integer"), "<init>", "(I)V"),
            SQCloudExtendedErrorCode(connection)
    );
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_errorOffset(JNIEnv *env, jobject thiz) {
    auto connection = getConnection(env, thiz);
    if (!SQCloudIsError(connection)) {
        return nullptr;
    }
    return env->NewObject(
            env->FindClass("java/lang/Integer"),
            env->GetMethodID(env->FindClass("java/lang/Integer"), "<init>", "(I)V"),
            SQCloudErrorOffset(connection)
    );
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmErrorCode(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    if (!vm) {
        return nullptr;
    }
    return env->NewObject(
            env->FindClass("java/lang/Integer"),
            env->GetMethodID(env->FindClass("java/lang/Integer"), "<init>", "(I)V"),
            SQCloudVMErrorCode(vm)
    );
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmErrorMessage(JNIEnv *env, jobject thiz,
                                                      jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    if (!vm) {
        return nullptr;
    }
    return env->NewStringUTF(SQCloudVMErrorMsg(vm));
}

void pubSubCallback(SQCloudConnection *connection, SQCloudResult *result, void *data) {
    auto pubSubData = static_cast<PubSubData *>(data);
    auto env = pubSubData->env;
    auto thiz = pubSubData->thiz;
    auto wrappedResult = wrapPointer(env, result);
    env->CallVoidMethod(thiz, env->GetMethodID(env->GetObjectClass(thiz), "pubSubCallback",
                                               "(Ljava/nio/ByteBuffer;)V"), wrappedResult);
}

extern "C" JNIEXPORT void JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_setPubSubCallback(JNIEnv *env, jobject thiz) {
    auto data = new PubSubData;
    data->env = env;
    data->thiz = thiz;

    SQCloudSetPubSubCallback(getConnection(env, thiz),pubSubCallback, data);
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_setPubSubOnly(JNIEnv *env, jobject thiz) {
    auto result = SQCloudSetPubSubOnly(getConnection(env, thiz));
    return wrapPointer(env, result);
}


extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_getClientUUID(JNIEnv *env, jobject thiz) {
    auto connection = getConnection(env, thiz);
    if (connection == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(SQCloudUUID(connection));
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_executeCommand(
        JNIEnv *env,
        jobject thiz,
        jstring query
) {
    auto connection = getConnection(env, thiz);

    auto result = SQCloudExec(connection, cString(env, query));

    return wrapPointer(env, result);
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_executeArrayCommand(
        JNIEnv *env,
        jobject thiz,
        jstring query,
        jobjectArray params,
        jintArray param_types
) {
    auto connection = getConnection(env, thiz);
    auto command = cString(env, query);
    uint32_t paramCount = env->GetArrayLength(params);
    const char **nativeParams = static_cast<const char **>(calloc(paramCount, sizeof(char *)));
    uint32_t *paramLengths = static_cast<uint32_t *>(calloc(paramCount, sizeof(uint32_t)));
    auto paramTypes = env->GetIntArrayElements(param_types, nullptr);
    for (int i = 0; i < paramCount; i++) {
        auto param = env->GetObjectArrayElement(params, i);
        if (paramTypes[i] == VALUE_BLOB) {
            nativeParams[i] = static_cast<const char *>(env->GetDirectBufferAddress(param));
            paramLengths[i] = env->GetDirectBufferCapacity(param);
        } else {
            auto stringParam = static_cast<jstring>(param);
            nativeParams[i] = cString(env, stringParam);
            paramLengths[i] = env->GetStringLength(stringParam);
        }
    }
    SQCLOUD_VALUE_TYPE *types = reinterpret_cast<SQCLOUD_VALUE_TYPE *>(env->GetIntArrayElements(
            param_types, nullptr));

    auto result = SQCloudExecArray(connection, command, nativeParams, paramLengths, types,
                                   paramCount);

    return wrapPointer(env, result);
}

extern "C" JNIEXPORT void JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_freeResult(JNIEnv *env, jobject thiz,
                                                  jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    SQCloudResultFree(result);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_resultType(JNIEnv *env, jobject thiz,
                                                  jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudResultType(result);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_intResult(JNIEnv *env, jobject thiz, jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudResultInt32(result);
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_longResult(JNIEnv *env, jobject thiz,
                                                  jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudResultInt64(result);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_doubleResult(JNIEnv *env, jobject thiz,
                                                    jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudResultDouble(result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_stringResult(JNIEnv *env, jobject thiz,
                                                    jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    auto bufferResult = SQCloudResultBuffer(result);
    return env->NewStringUTF(bufferResult);
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_bufferResult(JNIEnv *env, jobject thiz,
                                                    jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    auto bufferResult = SQCloudResultBuffer(result);
    return env->NewDirectByteBuffer(bufferResult, SQCloudResultLen(result));
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_arrayResultSize(JNIEnv *env, jobject thiz,
                                                       jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudArrayCount(result);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_arrayResultValueType(JNIEnv *env, jobject thiz,
                                                            jobject wrappedResult, jint index) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudArrayValueType(result, index);
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_arrayResultLongValue(JNIEnv *env, jobject thiz,
                                                            jobject wrappedResult, jint index) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudArrayInt64Value(result, index);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_arrayResultDoubleValue(JNIEnv *env, jobject thiz,
                                                              jobject wrappedResult, jint index) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudArrayDoubleValue(result, index);
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_arrayResultStringValue(JNIEnv *env, jobject thiz,
                                                              jobject wrappedResult, jint index) {
    auto result = unwrapResult(env, wrappedResult);
    uint32_t valueSize;
    return env->NewStringUTF(SQCloudArrayValue(result, index, &valueSize));
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_arrayResultBufferValue(JNIEnv *env, jobject thiz,
                                                              jobject wrappedResult, jint index) {
    auto result = unwrapResult(env, wrappedResult);
    uint32_t valueSize;
    auto value = SQCloudArrayValue(result, index, &valueSize);
    return env->NewDirectByteBuffer(value, valueSize);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_rowsetResultRowCount(JNIEnv *env, jobject thiz,
                                                            jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudRowsetRows(result);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_rowsetResultColumnCount(JNIEnv *env, jobject thiz,
                                                               jobject wrappedResult) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudRowsetCols(result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_rowsetResultColumnName(JNIEnv *env, jobject thiz,
                                                              jobject wrappedResult, jint column) {
    auto result = unwrapResult(env, wrappedResult);
    uint32_t columnNameLength;
    auto columnName = SQCloudRowsetColumnName(result, column, &columnNameLength);
    return env->NewStringUTF(columnName);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_rowsetResultValueType(JNIEnv *env, jobject thiz,
                                                             jobject wrappedResult, jint row,
                                                             jint column) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudRowsetValueType(result, row, column);
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_rowsetResultLongValue(JNIEnv *env, jobject thiz,
                                                             jobject wrappedResult, jint row,
                                                             jint column) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudRowsetInt64Value(result, row, column);
}


extern "C" JNIEXPORT jdouble JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_rowsetResultDoubleValue(JNIEnv *env, jobject thiz,
                                                               jobject wrappedResult, jint row,
                                                               jint column) {
    auto result = unwrapResult(env, wrappedResult);
    return SQCloudRowsetDoubleValue(result, row, column);
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_rowsetResultStringValue(JNIEnv *env, jobject thiz,
                                                               jobject wrappedResult, jint row,
                                                               jint column) {
    auto result = unwrapResult(env, wrappedResult);
    uint32_t valueSize;
    return env->NewStringUTF(SQCloudRowsetValue(result, row, column, &valueSize));
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_rowsetResultBufferValue(JNIEnv *env, jobject thiz,
                                                               jobject wrappedResult, jint row,
                                                               jint column) {
    auto result = unwrapResult(env, wrappedResult);
    uint32_t valueSize;
    auto value = SQCloudRowsetValue(result, row, column, &valueSize);
    return env->NewDirectByteBuffer(value, valueSize);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_uploadDatabase(JNIEnv *env, jobject thiz, jstring name,
                                                      jstring encryption_key, jobject data_handler,
                                                      jlong file_size, jobject callback) {
    return SQCloudUploadDatabase(
            getConnection(env, thiz),
            cString(env, name),
            cString(env, encryption_key),
            data_handler,
            file_size,
            reinterpret_cast<int (*)(void *, void *, uint32_t *, int64_t, int64_t)>(callback)
    );
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_downloadDatabase(JNIEnv *env, jobject thiz, jstring name,
                                                        jobject data_handler, jobject callback) {
    return SQCloudDownloadDatabase(
            getConnection(env, thiz),
            cString(env, name),
            data_handler,
            reinterpret_cast<int (*)(void *, const void *, uint32_t, int64_t, int64_t)>(callback)
    );
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_openBlob(JNIEnv *env, jobject thiz, jstring schema,
                                                jstring table, jstring column, jlong row_id,
                                                jboolean read_write) {
    auto handle = SQCloudBlobOpen(
            getConnection(env, thiz),
            cString(env, schema),
            cString(env, table),
            cString(env, column),
            row_id,
            read_write
    );
    return wrapPointer(env, handle);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_reopenBlob(JNIEnv *env, jobject thiz, jobject handle,
                                                  jlong row_id) {
    return SQCloudBlobReOpen(unwrapBlob(env, handle), row_id);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_closeBlob(JNIEnv *env, jobject thiz, jobject handle) {
    return SQCloudBlobClose(unwrapBlob(env, handle));
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_blobFieldSize(JNIEnv *env, jobject thiz, jobject handle) {
    return SQCloudBlobBytes(unwrapBlob(env, handle));
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_readBlob(JNIEnv *env, jobject thiz, jobject handle,
                                                jobject buffer) {
    return SQCloudBlobRead(
            unwrapBlob(env, handle),
            env->GetDirectBufferAddress(buffer),
            env->GetDirectBufferCapacity(buffer),
            0
    );
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_writeBlob(JNIEnv *env, jobject thiz, jobject handle,
                                                 jobject buffer) {
    auto result = SQCloudBlobWrite(
            unwrapBlob(env, handle),
            env->GetDirectBufferAddress(buffer),
            env->GetDirectBufferCapacity(buffer),
            0
    );
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindInt(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                 jint row_index, jint value) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMBindInt(vm, row_index, value);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindInt64(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                   jint row_index, jlong value) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMBindInt64(vm, row_index, value);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindDouble(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                    jint row_index, jdouble value) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMBindDouble(vm, row_index, value);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindText(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                  jint row_index, jstring value, jint byteSize) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMBindText(vm, row_index, cString(env, value),
                             byteSize);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindBlob(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                  jint row_index, jobject value) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMBindBlob(vm, row_index,
                             unwrapPointer(env, value), env->GetDirectBufferCapacity(value));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindZeroBlob(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                      jint row_index) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMBindZeroBlob(vm, row_index, 0);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindNull(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                  jint row_index) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMBindNull(vm, row_index);
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmCompile(JNIEnv *env, jobject thiz, jstring query) {
    auto result = SQCloudVMCompile(getConnection(env, thiz), cString(env, query), -1,
                                   nullptr);
    return wrapPointer(env, result);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmStep(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    auto result = SQCloudVMStep(vm);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmClose(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMClose(vm);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmColumnCount(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMColumnCount(vm);
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmLastRowID(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMLastRowID(vm);
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmChanges(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMChanges(vm);
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmTotalChanges(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMTotalChanges(vm);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmIsReadOnly(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMIsReadOnly(vm);
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmIsExplain(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMIsExplain(vm);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmIsFinalized(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMIsFinalized(vm);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindParameterCount(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMBindParameterCount(vm);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindParameterIndex(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                            jstring name) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMBindParameterIndex(vm, cString(env, name));
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmBindParameterName(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                           jint index) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return env->NewStringUTF(SQCloudVMBindParameterName(vm, index));
}

extern "C" JNIEXPORT jint JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmColumnType(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                    jint index) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMColumnType(vm, index);
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmResult(JNIEnv *env, jobject thiz, jobject wrappedVM) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    auto result = SQCloudVMResult(vm);
    return wrapPointer(env, result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_rowsetColumnName(JNIEnv *env, jobject thiz, jobject wrappedResult,
                                                        jint index) {
    auto result = unwrapResult(env, wrappedResult);
    uint32_t nameLength;
    auto name = SQCloudRowsetColumnName(result, index, &nameLength);
    return env->NewStringUTF(name);
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmColumnInt64(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                     jint index) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMColumnInt64(vm, index);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmColumnDouble(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                      jint index) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    return SQCloudVMColumnDouble(vm, index);
}

extern "C" JNIEXPORT jstring JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmColumnText(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                    jint index) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    uint32_t textLength;
    auto text = SQCloudVMColumnText(vm, index, &textLength);
    return env->NewStringUTF(text);
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_sqlitecloud_SQLiteCloudBridge_vmColumnBlob(JNIEnv *env, jobject thiz, jobject wrappedVM,
                                                    jint index) {
    SQCloudVM *vm = unwrapVM(env, wrappedVM);
    uint32_t blobLength;
    auto blob = SQCloudVMColumnBlob(vm, index, &blobLength);
    return env->NewDirectByteBuffer(const_cast<void *>(blob), blobLength);
}
