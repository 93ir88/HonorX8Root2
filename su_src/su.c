/*
 * Minimal su — compiled arm64 static by CI
 * Bind-mounted over /system/bin/su after exploit
 */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>

int main(int argc, char *argv[]) {
    uid_t uid = 0; gid_t gid = 0;
    const char *shell = "/system/bin/sh";
    const char *cmd   = NULL;

    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "-c") && i+1 < argc) cmd = argv[++i];
        else if (!strcmp(argv[i], "-s") && i+1 < argc) shell = argv[++i];
        else if (argv[i][0] != '-') { uid = gid = (uid_t)atoi(argv[i]); }
    }
    setgroups(0, NULL);
    if (setresgid(gid,gid,gid) || setresuid(uid,uid,uid)) {
        perror("su"); return 1;
    }
    if (cmd) execl(shell, shell, "-c", cmd, NULL);
    else     execl(shell, shell, NULL);
    perror("su: exec"); return 1;
}
