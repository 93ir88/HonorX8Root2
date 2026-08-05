#define _GNU_SOURCE
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/mount.h>
#include <sys/stat.h>
#include <errno.h>
#include <jni.h>
#include <android/log.h>

#define TAG "hx8root"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG,__VA_ARGS__)

#define MBASE  "/data/adb/modules"
#define MWORK  "/data/adb/.ovl_work"
#define MUPPER "/data/adb/.ovl_upper"

static int mount_overlay(const char *tgt) {
    mkdir(MWORK,0700); mkdir(MUPPER,0700);
    char ldir[8192]={0};
    DIR *d=opendir(MBASE);
    if(d){
        struct dirent *ent;
        while((ent=readdir(d))){
            if(ent->d_name[0]=='.') continue;
            char dis[512]; snprintf(dis,sizeof(dis),"%s/%s/disable",MBASE,ent->d_name);
            if(access(dis,F_OK)==0) continue;
            char lay[512]; snprintf(lay,sizeof(lay),"%s/%s%s",MBASE,ent->d_name,tgt);
            if(access(lay,F_OK)!=0) continue;
            if(ldir[0]) strncat(ldir,":",sizeof(ldir)-strlen(ldir)-1);
            strncat(ldir,lay,sizeof(ldir)-strlen(ldir)-1);
        }
        closedir(d);
    }
    if(ldir[0]) strncat(ldir,":",sizeof(ldir)-strlen(ldir)-1);
    strncat(ldir,tgt,sizeof(ldir)-strlen(ldir)-1);
    char opts[16384];
    snprintf(opts,sizeof(opts),"lowerdir=%s,upperdir=%s,workdir=%s",ldir,MUPPER,MWORK);
    if(mount("overlay",tgt,"overlay",MS_RDONLY,opts)!=0)
        if(mount("overlay",tgt,"overlay",0,opts)!=0){
            LOGE("overlay %s: %s",tgt,strerror(errno)); return -1;
        }
    LOGI("overlay ok: %s",tgt); return 0;
}

JNIEXPORT jint JNICALL
Java_com_zero_honorroot_OverlayMounter_nativeMountModules(JNIEnv *e, jobject t) {
    (void)e;(void)t;
    int r=0;
    r|=mount_overlay("/system");
    r|=mount_overlay("/vendor");
    return r;
}
