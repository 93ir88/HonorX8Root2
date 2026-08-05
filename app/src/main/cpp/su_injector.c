#define _GNU_SOURCE
#include <sched.h>
#ifndef CLONE_NEWNS
#define CLONE_NEWNS 0x00020000
#endif
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/mount.h>
#include <sys/stat.h>
#include <sys/sendfile.h>
#include <jni.h>
#include <android/log.h>

#define TAG "hx8root"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG,__VA_ARGS__)

#define SU_STAGE  "/data/local/tmp/.hx8su"
#define SU_TARGET "/system/bin/su"
#define ADB_BASE  "/data/adb"

static int fcopy(const char *src, const char *dst) {
    int s=open(src,O_RDONLY), d=open(dst,O_WRONLY|O_CREAT|O_TRUNC,0755);
    if(s<0||d<0){close(s);close(d);return -1;}
    struct stat st; fstat(s,&st); off_t off=0;
    while(off<st.st_size){
        ssize_t n=sendfile(d,s,&off,st.st_size-off);
        if(n<0&&errno!=EINTR) break;
    }
    close(s); close(d); return 0;
}

static int stage_su(const char *src) {
    unlink(SU_STAGE);
    if(fcopy(src,SU_STAGE)!=0) return -1;
    chmod(SU_STAGE, S_ISUID|S_ISGID|0755);
    lchown(SU_STAGE,0,0);
    LOGI("su staged: %s",SU_STAGE);
    return 0;
}

static int bind_su(void) {
    unshare(CLONE_NEWNS);
    mount(NULL,"/",NULL,MS_REC|MS_PRIVATE,NULL);
    if(access(SU_TARGET,F_OK)!=0){int f=open(SU_TARGET,O_CREAT|O_WRONLY,0755);if(f>=0)close(f);}
    if(mount(SU_STAGE,SU_TARGET,NULL,MS_BIND,NULL)!=0){
        LOGE("bind: %s",strerror(errno)); return -1;
    }
    LOGI("su bind-mounted");
    return 0;
}

static void mk_adb(void) {
    const char *d[]=
        {ADB_BASE,ADB_BASE"/magisk",ADB_BASE"/modules",
         ADB_BASE"/post-fs-data.d",ADB_BASE"/service.d",NULL};
    for(int i=0;d[i];i++){mkdir(d[i],0700);lchown(d[i],0,0);}
    /* minimal magisk db */
    FILE *f=fopen(ADB_BASE"/magisk.db","w");
    if(f){fputs("POLICY=2\nLOGGING=1\n",f);fclose(f);
          lchown(ADB_BASE"/magisk.db",0,0);}
    LOGI("adb dirs ready");
}

JNIEXPORT jint JNICALL
Java_com_zero_honorroot_SuInstaller_nativeInstall(JNIEnv *e, jobject t, jstring jp) {
    (void)t;
    const char *p=(*e)->GetStringUTFChars(e,jp,NULL);
    int r=0;
    if(stage_su(p)!=0){r=-1;goto done;}
    if(bind_su()!=0) {r=-2;goto done;}
    mk_adb();
done:
    (*e)->ReleaseStringUTFChars(e,jp,p);
    return r;
}
