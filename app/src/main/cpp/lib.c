#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <time.h>
#include <stdlib.h>


JNIEXPORT jlong JNICALL
Java_com_example_lexis_utilities_Utils_timerStringToMillis(JNIEnv *env, jobject thiz, jstring timeString) {
    const char *time = (*env)->GetStringUTFChars(env, timeString, 0);
    int hours = 0, minutes = 0, seconds = 0;
    sscanf(time, "%d:%d:%d", &hours, &minutes, &seconds);
    (*env)->ReleaseStringUTFChars(env, timeString, time);

    return (hours * 3600 + minutes * 60 + seconds) * 1000;
}

JNIEXPORT jstring  JNICALL
Java_com_example_lexis_utilities_Utils_getFullLanguage(JNIEnv *env, jobject thiz, jstring code){
    const char *codeStr = (*env) -> GetStringUTFChars(env, code, 0);
    jstring result = NULL;
    if(strcmp(codeStr, "fr")==0){
        result = (*env)->NewStringUTF(env, "French");
    }else if(strcmp(codeStr, "es") == 0) {
        result = (*env)->NewStringUTF(env, "Spanish");
    } else if (strcmp(codeStr, "de") == 0) {
        result = (*env)->NewStringUTF(env, "German");
    } else if (strcmp(codeStr, "tr") == 0) {
        result = (*env)->NewStringUTF(env, "Turkish");
    }
    (*env)->ReleaseStringUTFChars(env, code, codeStr);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_example_lexis_utilities_Utils_getLanguageCode(JNIEnv *env, jobject thiz, jstring spinnerText) {
    const char *textStr = (*env)->GetStringUTFChars(env, spinnerText, 0);
    char *language = NULL;

    char *space = strchr(textStr, ' ');
    if (space != NULL) {
        language = strdup(space + 1);
    }

    jstring result = NULL;

    if (language != NULL) {
        if (strcmp(language, "French") == 0) {
            result = (*env)->NewStringUTF(env, "fr");
        } else if (strcmp(language, "Spanish") == 0) {
            result = (*env)->NewStringUTF(env, "es");
        } else if (strcmp(language, "German") == 0) {
            result = (*env)->NewStringUTF(env, "de");
        } else if (strcmp(language, "Turkish") == 0) {
            result = (*env)->NewStringUTF(env, "tr");
        }
        free(language);
    }

    (*env)->ReleaseStringUTFChars(env, spinnerText, textStr);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_example_lexis_utilities_Utils_getFlagEmoji(JNIEnv *env, jobject thiz, jstring code) {
    const char *codeStr = (*env)->GetStringUTFChars(env, code, 0);
    jstring result = NULL;

    if (strcmp(codeStr, "de") == 0) {
        result = (*env)->NewStringUTF(env, "🇩🇪");
    } else if (strcmp(codeStr, "fr") == 0) {
        result = (*env)->NewStringUTF(env, "🇫🇷");
    } else if (strcmp(codeStr, "tr") == 0) {
        result = (*env)->NewStringUTF(env, "🇹🇷");
    } else if (strcmp(codeStr, "es") == 0) {
        result = (*env)->NewStringUTF(env, "🇪🇸");
    } else if (strcmp(codeStr, "en") == 0) {
        result = (*env)->NewStringUTF(env, "🇺🇸");
    } else {
        result = (*env)->NewStringUTF(env, "");
    }

    (*env)->ReleaseStringUTFChars(env, code, codeStr);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_example_lexis_utilities_Utils_getSpinnerText(JNIEnv *env, jobject thiz, jstring code) {
    jstring flagEmoji = Java_com_example_lexis_utilities_Utils_getFlagEmoji(env, thiz, code);

    jstring fullLanguage = Java_com_example_lexis_utilities_Utils_getFullLanguage(env, thiz, code);

    const char *flagStr = (*env)->GetStringUTFChars(env, flagEmoji, 0);
    const char *languageStr = (*env)->GetStringUTFChars(env, fullLanguage, 0);


    char resultStr[256];
    snprintf(resultStr, sizeof(resultStr), "%s %s", flagStr, languageStr);

    (*env)->ReleaseStringUTFChars(env, flagEmoji, flagStr);
    (*env)->ReleaseStringUTFChars(env, fullLanguage, languageStr);

    jstring result = (*env)->NewStringUTF(env, resultStr);

    (*env)->DeleteLocalRef(env, flagEmoji);
    (*env)->DeleteLocalRef(env, fullLanguage);

    return result;
}